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
 * querySummaryForDevice 返回单个 NetworkStats.Bucket（汇总值），
 * 直接读 rxBytes/txBytes 即可，无需遍历。
 */
class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    /** 扫描全部第三方应用并评分。 */
    suspend fun scanAll(): ScanSummary = withContext(Dispatchers.IO) {
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // 只扫第三方
        val audits = ArrayList<AppAudit>(apps.size)
        var totalBytes = 0L

        // 整机近 24h 流量（WiFi + 蜂窝），作为共享近似值
        val deviceBytes = queryDeviceBytes()

        for (info in apps) {
            val pkg = info.packageName
            val label = runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(pkg)

            val pkgInfo: PackageInfo? = runCatching {
                pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
            }.getOrNull()
            val perms = pkgInfo?.requestedPermissions?.toList() ?: emptyList()
            val queryAll = perms.contains("android.permission.QUERY_ALL_PACKAGES")

            totalBytes += deviceBytes

            val output = Scorer.score(
                Scorer.Input(
                    queryAll = queryAll,
                    grantedDangerous = perms,
                    bgMb = deviceBytes / 1048576.0,
                )
            )

            audits.add(
                AppAudit(
                    pkg = pkg,
                    label = label,
                    uid = info.uid,
                    isSystem = false,
                    bgBytes = deviceBytes,
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

    /** 整机近 24h 流量（WiFi + 蜂窝）。querySummaryForDevice 返回单个 Bucket。 */
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
                val bucket: NetworkStats.Bucket? = runCatching {
                    nsm.querySummaryForDevice(type, null, start, end)
                }.getOrNull()
                if (bucket != null) {
                    sum += bucket.rxBytes + bucket.txBytes
                }
            }
            sum
        }.getOrDefault(0L)
    }

    /** 检查是否有 PACKAGE_USAGE_STATS 权限（查流量需要）。 */
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
