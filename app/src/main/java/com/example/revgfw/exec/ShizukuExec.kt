package com.example.revgfw.exec

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.example.revgfw.IExecService
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 通过 Shizuku UserService（shell uid=2000）执行 shell 命令。
 * 替代已被 Shizuku v13 移除的 Shizuku.newProcess。
 */
object ShizukuExec {

    private const val TAG = "ShizukuExec"
    private const val TAG_SERVICE = "revgfw_exec_service"

    @Volatile
    private var service: IExecService? = null

    private var bound = false
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
            bound = false
        }
    }

    private fun ensureBound(): Boolean {
        val s = service
        if (s != null && s.asBinder().pingBinder()) return true

        if (Shizuku.isPreV11()) return false
        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return false
        }
        if (!Shizuku.pingBinder()) return false

        synchronized(lock) {
            try {
                Shizuku.bindUserService(userServiceArgs, connection)
                bound = true
                lock.wait(8000)
            } catch (e: Throwable) {
                Log.e(TAG, "bindUserService failed", e)
                return false
            }
        }
        return service != null
    }

    /** 执行命令，返回输出文本。失败返回带 [错误] 前缀的说明。 */
    fun run(cmd: String): String {
        if (!ensureBound()) {
            return "[错误] 无法连接 Shizuku UserService。请确认：\n" +
                    "1) Shizuku 服务正在运行\n" +
                    "2) 已授权本应用\n" +
                    "3) Shizuku 版本 >= 13（本机检测: ${safeVersion()}）"
        }
        return try {
            service!!.exec(cmd)
        } catch (e: Throwable) {
            "[错误] 执行异常: ${e.message}"
        }
    }

    /** 返回 uid（验证用），失败返回 -1 */
    fun uid(): Int {
        if (!ensureBound()) return -1
        return try { service!!.uid } catch (e: Throwable) { -1 }
    }

    private fun safeVersion(): String = try {
        Shizuku.getVersion().toString()
    } catch (e: Throwable) {
        "unknown"
    }
}
