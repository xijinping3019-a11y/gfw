package com.example.revgfw.domain

import com.example.revgfw.data.Category
import com.example.revgfw.data.Reason

/** 评分引擎：把"证据"折算成 0~100 的可疑度，并给出可读原因 */
object Scorer {

    data class Input(
        val queryAll: Boolean,
        val grantedDangerous: List<String>,
        val bgMb: Double,
    )

    data class Output(val score: Int, val reasons: List<Reason>)

    fun score(input: Input): Output {
        val reasons = mutableListOf<Reason>()
        var total = 0

        // 1) 包枚举
        if (input.queryAll) {
            total += Rules.QUERY_ALL_WEIGHT
            reasons += Reason(
                title = "可枚举设备上全部应用（QUERY_ALL_PACKAGES）",
                weight = Rules.QUERY_ALL_WEIGHT,
                category = Category.ENUMERATION,
            )
        }

        // 2) 敏感权限（逐个列名 + 权重）
        var permScore = 0
        input.grantedDangerous.forEach { perm ->
            val (w, cat) = Rules.sensitiveWeight(perm) ?: return@forEach
            permScore += w
            reasons += Reason(
                title = "已授予敏感权限：${prettyPerm(perm)}",
                weight = w,
                category = cat,
            )
        }
        val permCapped = permScore.coerceAtMost(Rules.SENSITIVE_PERM_CAP)
        total += permCapped

        // 3) 后台流量
        val bgW = Rules.bgTrafficWeight(input.bgMb)
        if (bgW > 0) {
            total += bgW
            reasons += Reason(
                title = "24 小时后台流量 %.1f MB".format(input.bgMb),
                weight = bgW,
                category = Category.TRAFFIC,
            )
        }

        return Output(total.coerceIn(0, 100), reasons.sortedByDescending { it.weight })
    }

    private fun prettyPerm(p: String): String =
        p.substringAfterLast('.').lowercase()
            .replace('_', ' ')
}