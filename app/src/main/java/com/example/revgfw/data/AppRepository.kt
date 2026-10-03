package com.example.revgfw.data

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.content.getSystemService
import com.example.revgfw.domain.Scorer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用审计仓储层。
 *
 * 说明：querySummaryForUid 属 @SystemApi，对第三方 app 不可见（编译期报
 * Unresolved reference），改用公开 API querySummaryForDevice 获取整机流量
 * 作为近似指标，足以支撑自用隐私审计的粗粒度判断。
 */
class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    /** 扫描全部第三方应用并评分。 */
    suspend fun scanAll(): ScanSummary = withContext(Dispatchers.IO) {
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
        val audits = ArrayList<AppAudit>(apps.size)
        var totalBytes = 0L

        // 整机近 24h 流量（WiFi + 蜂窝），作为所有 app 的共享近似值
        val deviceBytes = queryDeviceBytes()

        for (info in apps) {
            val pkg = info.packageName
            val label = runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(pkg)

            val pkgInfo: PackageInfo? = runCatching {
                pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
            }.getOrNull()
            val perms = pkgInfo?.requestedPermissions?.toList() ?: emptyList()
            val queryAll = perms.contains("android.permission.QUERY_ALL_PACKAGES")

            val bgBytes = deviceBytes
            totalBytes += bgBytes

            val output = Scorer.score(
                Scorer.Input(
                    queryAll = queryAll,
                    grantedDangerous = perms,
                    bgMb = bgBytes / 1048576.0,
                )
            )

            audits.add(
                AppAudit(
                    pkg = pkg,
                    label = label,
                    uid = info.uid,
                    isSystem = false,
                    bgBytes = bgBytes,
                    fgBytes = 0L,
                    firstInstall = pkgInfo?.firstInstallTime ?: 0L,
                    lastUpdate = pkgInfo?.lastUpdateTime ?: 0L,
                    queryAll = queryAll,
                    grantedDangerous = perms,
                    score = output.score,
                    reasons = output.reasons,
                )
            )
        }

        val sorted = audits.sortedByDescending { it.score }
        ScanSummary(
            total = sorted.size,
            high = sorted.count { it.risk == Risk.HIGH },
            medium = sorted.count { it.risk == Risk.MEDIUM },
            low = sorted.count { it.risk == Risk.LOW },
            totalBytes = totalBytes,
            timestamp = System.currentTimeMillis(),
            apps = sorted,
        )
    }

    /** 整机近 24h 流量（WiFi + 蜂窝），公开 API，第三方 app 可用。 */
    private fun queryDeviceBytes(): Long {
        return runCatching {
            val nsm: NetworkStatsManager = context.getSystemService() ?: return 0L
            val end = System.currentTimeMillis()
            val start = end - 24 * 60 * 60 * 1000L
            var sum = 0L
            val types = intArrayOf(
                ConnectivityManager.TYPE_WIFI,
                ConnectivityManager.TYPE_MOBILE,
            )
            for (type in types) {
                val s: NetworkStats? = runCatching {
                    nsm.querySummaryForDevice(type, null, start, end)
                }.getOrNull()
                sum += readStats(s)
            }
            sum
        }.getOrDefault(0L)
    }

    private fun readStats(stats: NetworkStats?): Long {
        if (stats == null) return 0L
        var total = 0L
        val bucket = NetworkStats.Bucket()
        while (stats.hasNextBucket()) {
            stats.getNextBucket(bucket)
            total += bucket.rxBytes + bucket.txBytes
        }
        runCatching { stats.close() }
        return total
    }

    /** 检查是否有 PACKAGE_USAGE_STATS 权限。 */
    fun hasUsageStatsPermission(): Boolean = runCatching {
        val appOps: AppOpsManager = context.getSystemService() ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)
}
