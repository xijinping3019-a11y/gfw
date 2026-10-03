package com.example.revgfw.domain

import com.example.revgfw.data.Category

/**
 * 审计规则集：集中定义"什么行为值得警惕"以及对应权重。
 * 修改评分策略只需改这里，扫描逻辑无需变动。
 */
object Rules {

    /** 敏感权限 -> 权重 */
    val SENSITIVE_PERMS: Map<String, Int> = mapOf(
        "android.permission.READ_PHONE_STATE" to 4,
        "android.permission.ACCESS_FINE_LOCATION" to 6,
        "android.permission.ACCESS_COARSE_LOCATION" to 5,
        "android.permission.ACCESS_BACKGROUND_LOCATION" to 10,
        "android.permission.READ_CONTACTS" to 6,
        "android.permission.READ_SMS" to 8,
        "android.permission.RECEIVE_SMS" to 8,
        "android.permission.RECORD_AUDIO" to 7,
        "android.permission.CAMERA" to 5,
        "android.permission.READ_CALL_LOG" to 8,
        "android.permission.CALL_PHONE" to 4,
        "android.permission.READ_EXTERNAL_STORAGE" to 3,
    )

    /** 敏感权限对普通应用的叠加风险（授予越多越可疑，但有上限） */
    const val SENSITIVE_PERM_CAP = 30

    /** QUERY_ALL_PACKAGES：可枚举你装的全部应用 */
    const val QUERY_ALL_WEIGHT = 25

    /** 后台流量阈值与权重（MB） */
    const val BG_TRAFFIC_THRESHOLD_MB = 1.0
    const val BG_TRAFFIC_CAP = 35

    /** 陌生安装来源（非应用商店）轻微加分 */
    const val SYSTEM_APP_LISTED = false // 系统应用默认不列出

    fun sensitiveWeight(perm: String): Pair<Int, Category>? =
        SENSITIVE_PERMS[perm]?.let { it to Category.PERMISSION }

    /** 后台流量 -> 权重 */
    fun bgTrafficWeight(bgMb: Double): Int {
        if (bgMb <= BG_TRAFFIC_THRESHOLD_MB) return 0
        val raw = (bgMb / 5.0).toInt() + 5
        return raw.coerceAtMost(BG_TRAFFIC_CAP)
    }
}