package com.example.revgfw.exec

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.example.revgfw.IExecService
import rikka.shizuku.Shizuku

object ShizukuExec {

    private const val TAG = "ShizukuExec"
    private const val REQ_PERM = 4213

    /**
     * 命令执行结果。
     * code: 退出码（-1 表示未执行/连接失败）
     * ok:   code == 0
     */
    data class ExecResult(
        val code: Int,
        val stdout: String,
        val stderr: String = ""
    ) {
        val ok: Boolean get() = code == 0

        /** UI 层用 r.pretty() 展示，兼顾成功/失败 */
        fun pretty(): String = if (ok) {
            if (stdout.isBlank()) "(无输出)" else stdout
        } else {
            "[失败 code=$code] " + (stderr.ifBlank { stdout.ifBlank { "(无输出)" } })
        }

        override fun toString(): String = pretty()
    }

    @Volatile
    private var service: IExecService? = null

    private val lock = Object()

    private val userServiceArgs: Shizuku.UserServiceArgs
        get() = Shizuku.UserServiceArgs(
            ComponentName("com.example.revgfw", ExecUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("exec")
            .debuggable(false)
            .version(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.i(TAG, "onServiceConnected")
            service = IExecService.Stub.asInterface(binder)
            synchronized(lock) { lock.notifyAll() }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "onServiceDisconnected")
            service = null
        }
    }

    private fun ensureBound(): Boolean {
        val s = service
        if (s != null && s.asBinder().pingBinder()) return true

        if (Shizuku.isPreV11()) return false
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) return false
        if (!Shizuku.pingBinder()) return false

        synchronized(lock) {
            try {
                Shizuku.bindUserService(userServiceArgs, connection)
                lock.wait(8000)
            } catch (e: Throwable) {
                Log.e(TAG, "bindUserService failed", e)
                return false
            }
        }
        return service != null
    }

    fun run(cmd: String): ExecResult {
        if (!ensureBound()) {
            val msg = "无法连接 Shizuku UserService。请确认：\n" +
                    "1) Shizuku 服务正在运行\n" +
                    "2) 已授权本应用\n" +
                    "3) Shizuku 版本 >= 13（本机: ${safeVersion()}）"
            return ExecResult(-1, "", msg)
        }
        return try {
            val code = service!!.execCode(cmd)
            val out = service!!.execOut(cmd)
            ExecResult(code, if (code == 0) out else "", if (code == 0) "" else out)
        } catch (e: Throwable) {
            ExecResult(-1, "", "执行异常: ${e.message}")
        }
    }

    fun runText(cmd: String): String = run(cmd).let { if (it.ok) it.stdout else it.stderr }

    fun uid(): Int {
        if (!ensureBound()) return -1
        return try { service!!.uid() } catch (e: Throwable) { -1 }
    }

    // ================== UI 层用到的辅助方法 ==================

    /** Shizuku 是否已安装（Binder 是否可用） */
    fun isInstalled(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Throwable) {
        false
    }

    /** Shizuku 是否已授权本应用 */
    fun isAuthorized(): Boolean = try {
        if (Shizuku.isPreV11()) false
        else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) {
        false
    }

    /** 申请授权 */
    fun requestPermission() {
        try {
            if (!Shizuku.isPreV11()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(REQ_PERM)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "requestPermission failed", e)
        }
    }

    private fun safeVersion(): String = try {
        Shizuku.getVersion().toString()
    } catch (e: Throwable) {
        "unknown"
    }
}
