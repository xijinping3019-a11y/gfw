package com.example.revgfw.data

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
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
 * 职责：从系统收集原始数据（包信息 / 权限 / 流量），
 * 交给 domain.Scorer 打分，返回 AppAudit 列表。
 *
 * 与原型 Audit.kt 的区别：
 *  - 异步（coroutines），不阻塞 UI
 *  - 评分逻辑抽到 domain.Scorer，可单测
 *  - 返回结构化模型，而不是散装变量
 */
class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    /** 扫描全部第三方应用并评分。 */
    suspend fun scanAll(): ScanSummary = withContext(Dispatchers.IO) {
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // 只扫第三方
        val audits = ArrayList<AppAudit>(apps.size)
        var totalBytes = 0L

        for (info in apps) {
            val pkg = info.packageName
            val perms = runCatching {
                pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS).requestedPermissions?.toList() ?: emptyList()
            }.getOrDefault(emptyList())

            val bgBytes = queryBackgroundBytes(pkg)
            totalBytes += bgBytes

            val audit = Scorer.score(
                packageName = pkg,
                appLabel = runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(pkg),
                requestedPermissions = perms,
                backgroundBytes24h = bgBytes,
                isSystem = false,
            )
            audits.add(audit)
        }

        val sorted = audits.sortedByDescending { it.score }
        ScanSummary(
            apps = sorted,
            highRiskCount = sorted.count { it.risk == Risk.HIGH },
            mediumRiskCount = sorted.count { it.risk == Risk.MEDIUM },
            lowRiskCount = sorted.count { it.risk == Risk.LOW },
            totalBytes = totalBytes,
            timestamp = System.currentTimeMillis(),
        )
    }

    /** 查询某包最近 24h 的后台（WiFi + 蜂窝）流量字节数。 */
    private fun queryBackgroundBytes(pkg: String): Long {
        return runCatching {
            val nsm = context.getSystemService<NetworkStatsManager>() ?: return 0L
            val end = System.currentTimeMillis()
            val start = end - 24 * 60 * 60 * 1000L
            var sum = 0L

            // WiFi
            runCatching {
                val s = nsm.querySummaryForUid(
                    ConnectivityManager.TYPE_WIFI, null, start, end, pkg.hashCode()
                )
                sum += readStats(s)
            }
            // 蜂窝
            runCatching {
                val s = nsm.querySummaryForUid(
                    ConnectivityManager.TYPE_MOBILE, null, start, end, pkg.hashCode()
                )
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
            // rx 是后台接收，tx 是后台发送（前台计数在字段名里带 foreground）
            total += bucket.rxBytes + bucket.txBytes
        }
        runCatching { stats.close() }
        return total
    }

    /** 检查是否有 PACKAGE_USAGE_STATS 权限（查流量需要）。 */
    fun hasUsageStatsPermission(): Boolean = runCatching {
        val appOps = context.getSystemService<AppOpsManager>() ?: return false
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