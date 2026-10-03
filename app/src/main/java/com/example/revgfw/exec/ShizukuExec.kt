package com.example.revgfw.exec

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Shizuku 执行层（修正版）。
 *
 * 关键修复：旧代码用反射调 Shizuku.newProcess(String[]) 找不到方法
 * （NoSuchMethodException: Shizuku.newProcess [class [Ljava.lang.String;]），
 * 导致所有命令都 code=-1。现改为按不同 Shizuku 版本逐一探测真实签名。
 */
object ShizukuExec {

    data class ExecResult(
        val code: Int,
        val stdout: String,
        val stderr: String,
    ) {
        val ok: Boolean get() = code == 0
        fun pretty(): String = buildString {
            if (stdout.isNotBlank()) append(stdout.trim())
            if (stderr.isNotBlank()) {
                if (isNotEmpty()) append('\n')
                append("[stderr] ").append(stderr.trim())
            }
            if (isEmpty()) append("(无输出)")
        }
    }

    fun isInstalled(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun isAuthorized(): Boolean = runCatching {
        if (!Shizuku.pingBinder()) return false
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun requestPermission(requestCode: Int = 1001) {
        runCatching {
            if (Shizuku.isPreV11()) return
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode)
            }
        }
    }

    fun run(command: String, timeoutMs: Long = 15_000L): ExecResult {
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            return ExecResult(-1, "", "Shizuku 未运行或未授权")
        }
        var process: Process? = null
        return try {
            val cmd = arrayOf("sh", "-c", command)
            process = newProcess(cmd)

            val outReader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            val stdout = StringBuilder()
            val stderr = StringBuilder()

            val tOut = Thread { outReader.forEachLine { stdout.append(it).append('\n') } }
            val tErr = Thread { errReader.forEachLine { stderr.append(it).append('\n') } }
            tOut.start(); tErr.start()

            val finished = waitFor(process, timeoutMs)
            if (!finished) {
                process.destroy()
                stderr.append("\n[超时 ${timeoutMs}ms，已终止]")
            }
            tOut.join(500); tErr.join(500)

            val code = runCatching { process.exitValue() }.getOrDefault(-1)
            ExecResult(code, stdout.toString(), stderr.toString())
        } catch (t: Throwable) {
            ExecResult(-1, "", t.message ?: "执行异常")
        } finally {
            runCatching { process?.destroy() }
        }
    }

    private fun waitFor(p: Process, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!p.isAlive) return true
            Thread.sleep(50)
        }
        return !p.isAlive
    }

    /**
     * 兼容 Shizuku 各版本的 newProcess 签名。
     * 依次尝试：
     *   1) newProcess(String[] cmd, String[] env, String dir)   ← 新版标准
     *   2) newProcess(String[] cmd)                             ← 旧版
     * 失败则抛出带版本号的明确异常。
     */
    private fun newProcess(cmd: Array<String>): Process {
        val errors = StringBuilder()

        // 尝试 1：三参数
        runCatching {
            val m = Shizuku::class.java.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            m.isAccessible = true
            return m.invoke(null, cmd, null, null) as Process
        }.onFailure { errors.append("3-arg: ").append(it.message).append('\n') }

        // 尝试 2：单参数
        runCatching {
            val m = Shizuku::class.java.getMethod("newProcess", Array<String>::class.java)
            m.isAccessible = true
            return m.invoke(null, cmd) as Process
        }.onFailure { errors.append("1-arg: ").append(it.message).append('\n') }

        // 尝试 3：newProcess(String[] cmd, String[] env, String[] dir) 变体兜底
        runCatching {
            val m = Shizuku::class.java.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                Array<String>::class.java
            )
            m.isAccessible = true
            return m.invoke(null, cmd, null, null) as Process
        }.onFailure { errors.append("3-arg-arr: ").append(it.message).append('\n') }

        val ver = runCatching { Shizuku::class.java.getMethod("getVersion").invoke(null) }.getOrNull()
        throw IllegalStateException(
            "Shizuku.newProcess 不可用（版本 $ver）。已尝试所有签名：\n$errors"
        )
    }
}
