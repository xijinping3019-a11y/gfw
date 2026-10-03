package com.example.revgfw.exec

/**
 * 具体的系统动作封装。
 *
 * 全部基于 Shizuku（免 root，Android 11+ 需无线调试授权）。
 * 每个动作都返回 ExecResult，便于 UI 显示成功/失败。
 *
 * ⚠️ 合规声明：这些动作等价于系统自带的"应用管理/网络控制/应用挂起"，
 * 仅用于用户对自己设备上的应用做隐私审计与自我管理。
 */
object Actions {

    // ---------------- 网络控制 ----------------

    /** 断网（禁止该包使用网络）。等价：设置 → 应用 → 流量 → 禁用网络 */
    fun blockNetwork(pkg: String): ShizukuExec.ExecResult =
        ShizukuExec.run("cmd connectivity set-package-networking-enabled false $pkg")

    /** 恢复联网 */
    fun unblockNetwork(pkg: String): ShizukuExec.ExecResult =
        ShizukuExec.run("cmd connectivity set-package-networking-enabled true $pkg")

    // ---------------- 后台限制 ----------------

    /** 限制后台（RUN_IN_BACKGROUND = ignore） */
    fun restrictBackground(pkg: String): ShizukuExec.ExecResult =
        ShizukuExec.run("cmd appops set $pkg RUN_IN_BACKGROUND ignore")

    /** 解除后台限制 */
    fun unrestrictBackground(pkg: String): ShizukuExec.ExecResult =
        ShizukuExec.run("cmd appops set $pkg RUN_IN_BACKGROUND allow")

    // ---------------- 权限撤销 ----------------

    /** 撤销某个运行时权限（如回收后台定位、读取短信等） */
    fun revokePermission(pkg: String, permission: String): ShizukuExec.ExecResult =
        ShizukuExec.run("pm revoke $pkg $permission")

    // ---------------- 冻结 / 停止 ----------------

    /** 强制停止应用 */
    fun forceStop(pkg: String): ShizukuExec.ExecResult =
        ShizukuExec.run("am force-stop $pkg")

    /**
     * 冻结应用（挂起）。挂起后系统会阻止其后台运行，效果类似"冻结"。
     * 恢复：unfreeze
     */
    fun freeze(pkg: String): ShizukuExec.ExecResult {
        ShizukuExec.run("am force-stop $pkg")
        return ShizukuExec.run("pm suspend $pkg")
    }

    /** 解冻应用 */
    fun unfreeze(pkg: String): ShizukuExec.ExecResult =
        ShizukuExec.run("pm unsuspend $pkg")

    // ---------------- 查询类 ----------------

    /** 查询某包当前是否被限制网络（读取 appops 或网络策略较复杂，这里用 dumpsys 简查） */
    fun isNetworkBlocked(pkg: String): ShizukuExec.ExecResult =
        ShizukuExec.run("dumpsys connectivity | grep -A3 '$pkg' | head -20")

    /** 获取应用包名列表（备用，正常扫描走 PackageManager） */
    fun listPackages(): ShizukuExec.ExecResult =
        ShizukuExec.run("pm list packages -3")
}
