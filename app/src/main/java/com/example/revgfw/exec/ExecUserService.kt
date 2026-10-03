package com.example.revgfw.exec

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.example.revgfw.IExecService
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 运行在 shell(uid=2000) 身份下的 UserService。
 * 通过 Shizuku.bindUserService 拉起，内部用 ProcessBuilder 跑命令。
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

    override fun uid(): Int = android.os.Process.myUid()

    override fun execCode(cmd: String): Int {
        return try {
            val p = ProcessBuilder("sh", "-c", cmd)
                .redirectErrorStream(true).start()
            readAll(p).length  // 读掉输出避免阻塞
            p.waitFor()
        } catch (e: Exception) {
            -1
        }
    }

    override fun execOut(cmd: String): String {
        return try {
            val p = ProcessBuilder("sh", "-c", cmd)
                .redirectErrorStream(true).start()
            val out = readAll(p)
            p.waitFor()
            out
        } catch (e: Exception) {
            "[Exception] ${e.message}"
        }
    }

    private fun readAll(p: Process): String =
        BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }.trim()
}
