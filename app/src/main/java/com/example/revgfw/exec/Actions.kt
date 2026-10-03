package com.example.revgfw.exec

/**
 * 具体的系统动作封装（修正版）。
 *
 * 全部基于 Shizuku（免 root，Android 11+ 需无线调试授权）。
 *
 * ⚠️ 合规声明：这些动作等价于系统自带的"应用管理/网络控制/应用挂起"，
 * 仅用于用户对自己设备上的应用做隐私审计与自我管理。
 *
 * 权限边界说明（重要）：
 *   Shizuku 提供的是 adb shell(uid 2000) 权限。
 *   - 能做的：am force-stop / cmd appops / pm disable-user / pm enable
 *   - 做不到的：per-app 断网（改网络策略需系统签名）、pm suspend（需 device owner）
 */
object Actions {

    // ---------------- 自检 ----------------

    /** 自检：执行 id，确认 Shizuku 执行链路是否打通（uid 应是 2000 shell 或类似） */
    fun selfTest(): ShizukuExec.ExecResult = ShizukuExec.run("id")

    // ---------------- 强制停止 ----------------

    /** 强制停止应用（shell 权限可做） */
    fun forceStop(pkg: String): ShizukuExec.ExecResult =
        ShizukuExec.run("am force-stop $pkg")

    // ---------------- 后台限制 ----------------

    /**
     * 限制后台（RUN_ANY_IN_BACKGROUND = ignore）。
     * 注：旧版 Android 用 RUN_IN_BACKGROUND，新版为 RUN_ANY_IN_BACKGROUND，
     * 我们先试新名，失败再试旧名。
     */
    fun restrictBackground(pkg: String): ShizukuExec.ExecResult {
        val r = ShizukuExec.run("cmd appops set $pkg RUN_ANY_IN_BACKGROUND ignore")
        if (r.ok) return r
        return ShizukuExec.run("cmd appops set $pkg RUN_IN_BACKGROUND ignore")
    }

    /** 解除后台限制 */
    fun unrestrictBackground(pkg: String): ShizukuExec.ExecResult {
        val r = ShizukuExec.run("cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow")
        if (r.ok) return r
        return ShizukuExec.run("cmd appops set $pkg RUN_IN_BACKGROUND allow")
    }

    // ---------------- 权限撤销 ----------------

    /** 撤销某个运行时权限（如回收后台定位、读取短信等） */
    fun revokePermission(pkg: String, permission: String): ShizukuExec.ExecResult =
        ShizukuExec.run("pm revoke --user 0 $pkg $permission")

    // ---------------- 冻结 / 停止 ----------------

    /**
     * 冻结应用（挂起）。
     * 优先用 pm suspend（部分 ROM 需更高权限），失败则降级为
     * pm disable-user（禁用应用，等效冻结，shell 权限通常可做）。
     */
    fun freeze(pkg: String): ShizukuExec.ExecResult {
        ShizukuExec.run("am force-stop $pkg")
        val s = ShizukuExec.run("pm suspend $pkg")
        if (s.ok) return s
        // 降级：禁用应用
        return ShizukuExec.run("pm disable-user --user 0 $pkg")
    }

    /** 解冻应用（同时尝试恢复挂起与启用，兼容两种冻结方式） */
    fun unfreeze(pkg: String): ShizukuExec.ExecResult {
        val u = ShizukuExec.run("pm unsuspend $pkg")
        ShizukuExec.run("pm enable --user 0 $pkg")
        return u
    }

    // ---------------- 网络控制（诚实说明） ----------------

    /**
     * 断网：Android 没有可通过 shell 直接操作的 per-app 网络开关。
     * 真断网需自建 VpnService（本地防火墙）。此处返回明确说明，
     * 避免误导用户以为已断网。
     */
    fun blockNetwork(pkg: String): ShizukuExec.ExecResult =
        ShizukuExec.ExecResult(
            -1, "",
            "Android 不支持通过 Shizuku 直接禁用某应用联网。\n" +
            "彻底断网需启用 VPN 模式（开发中）。\n" +
            "当前可先使用「限制后台」减少其后台流量。"
        )

    /** 恢复联网（占位：与断网对称，当前无 shell 可用的恢复动作） */
    fun unblockNetwork(pkg: String): ShizukuExec.ExecResult =
        ShizukuExec.ExecResult(
            -1, "",
            "无需恢复：应用联网从未被本机修改。\n" +
            "如需彻底管控联网，请使用 VPN 模式（开发中）。"
        )

    // ---------------- 查询类 ----------------

    /** 查询某包当前状态（进程/包信息摘要） */
    fun packageInfo(pkg: String): ShizukuExec.ExecResult =
        ShizukuExec.run("dumpsys package $pkg | grep -E 'enabled|stopped|suspended' | head -10")

    /** 获取第三方应用包名列表 */
    fun listPackages(): ShizukuExec.ExecResult =
        ShizukuExec.run("pm list packages -3")
}
