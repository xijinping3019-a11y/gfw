package com.example.revgfw.exec

import android.content.Context
import android.os.RemoteException
import android.util.Log
import androidx.annotation.Keep
import com.example.revgfw.IExecService
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 运行在 shell(uid=2000) 身份下的 UserService。
 * 通过 Shizuku.bindUserService 拉起，内部用 Runtime.exec 跑命令。
 */
class ExecUserService : IExecService.Stub {

    constructor() {
        Log.i("ExecUserService", "constructor()")
    }

    @Keep
    constructor(context: Context) {
        Log.i("ExecUserService", "constructor(Context)")
    }

    override fun destroy() {
        Log.i("ExecUserService", "destroy")
        System.exit(0)
    }

    override fun getUid(): Int = android.os.Process.myUid()

    override fun exec(cmd: String): String {
        return try {
            val pb = ProcessBuilder("sh", "-c", cmd)
            pb.redirectErrorStream(true)
            val p = pb.start()
            val out = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }
            val code = p.waitFor()
            val trimmed = out.trim()
            if (code == 0) trimmed else "[exit=$code] $trimmed"
        } catch (e: Exception) {
            "[Exception] ${e.message}"
        }
    }
}
