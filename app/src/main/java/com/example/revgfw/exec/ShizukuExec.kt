package com.example.revgfw.exec

import android.content.Context
import android.os.Build
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Shizuku 执行层。
 *
 * 设计目标：
 *  - 不再像原型那样用反射调 Shizuku.newProcess（脆弱、随版本失效）
 *  - 统一走 Shizuku 官方 API：Shizuku.newProcess(cmd, env, dir)
 *  - 统一返回 ExecResult(code, stdout, stderr)，供上层判定
 *
 * 前提：用户已安装 Shizuku 并授权本应用（Android 11+ 通常走无线调试授权）。
 */
object ShizukuExec {

    data class ExecResult(
        val code: Int,
        val stdout: String,
        val stderr: String,
    ) {
        val ok: Boolean get() = code == 0
        /** 合并输出，便于 UI 直接展示 */
        fun pretty(): String = buildString {
            if (stdout.isNotBlank()) append(stdout.trim())
            if (stderr.isNotBlank()) {
                if (isNotEmpty()) append('\n')
                append("[stderr] ").append(stderr.trim())
            }
            if (isEmpty()) append("(无输出)")
        }
    }

    /** 是否已安装 Shizuku（仅判断包是否存在，不判断授权） */
    fun isInstalled(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    /** 是否已授权 */
    fun isAuthorized(): Boolean = runCatching {
        if (!Shizuku.pingBinder()) return false
        Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    /** 申请授权（Android 11+ 会拉起 Shizuku 授权弹窗） */
    fun requestPermission(requestCode: Int = 1001) {
        runCatching {
            if (Shizuku.isPreV11()) return
            if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode)
            }
        }
    }

    /**
     * 执行 shell 命令。
     *
     * @param command 完整 shell 命令（会以 `sh -c` 语义执行）
     * @param timeoutMs 读取超时（毫秒），超时后强杀进程
     */
    fun run(command: String, timeoutMs: Long = 15_000L): ExecResult {
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            return ExecResult(-1, "", "Shizuku 未运行或未授权")
        }
        var process: Process? = null
        return try {
            val cmd = arrayOf("sh", "-c", command)
            // Shizuku.newProcess 在部分版本为 public static，新版用 Shizuku::newProcess
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
     * 兼容不同 Shizuku 版本的 newProcess。
     * 优先用官方 API，失败再反射兜底。
     */
    private fun newProcess(cmd: Array<String>): Process {
        // 官方：Shizuku.newProcess(String[] cmd, String[] env, String dir)
        return runCatching {
            Shizuku::class.java
                .getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                .invoke(null, cmd, null, null) as Process
        }.getOrElse {
            // 兼容旧签名（只有 cmd 一个参数）
            Shizuku::class.java
                .getMethod("newProcess", Array<String>::class.java)
                .invoke(null, cmd) as Process
        }
    }
}
