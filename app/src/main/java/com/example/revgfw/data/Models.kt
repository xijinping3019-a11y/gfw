package com.example.revgfw.data

/** 风险等级 */
enum class Risk(val label: String) {
    HIGH("高危"),
    MEDIUM("可疑"),
    LOW("正常");

    companion object {
        fun of(score: Int) = when {
            score >= 60 -> HIGH
            score >= 30 -> MEDIUM
            else -> LOW
        }
    }
}

/** 单条风险原因（可解释评分） */
data class Reason(
    val text: String,
    val weight: Int,
    val category: Category,
)

enum class Category(val label: String) {
    PERMISSION("权限"),
    TRAFFIC("流量"),
    ENUMERATION("包枚举"),
    BACKGROUND("后台行为"),
    SYSTEM("系统"),
}

/** 一个应用的完整审计结果 */
data class AppAudit(
    val pkg: String,
    val label: String,
    val uid: Int,
    val isSystem: Boolean,
    val bgBytes: Long,
    val fgBytes: Long,
    val firstInstall: Long,
    val lastUpdate: Long,
    val queryAll: Boolean,
    val grantedDangerous: List<String>,
    val score: Int,
    val reasons: List<Reason>,
) {
    val risk: Risk get() = Risk.of(score)
    val bgMb: Double get() = bgBytes / 1048576.0
    val fgMb: Double get() = fgBytes / 1048576.0
    val totalMb: Double get() = (bgBytes + fgBytes) / 1048576.0
}

/** 扫描汇总 */
data class ScanSummary(
    val total: Int,
    val high: Int,
    val medium: Int,
    val low: Int,
    val totalBytes: Long,
    val timestamp: Long,
)