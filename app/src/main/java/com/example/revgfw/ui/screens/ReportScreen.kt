package com.example.revgfw.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.revgfw.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(vm: MainViewModel) {
    val summary by vm.summary.collectAsState()
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("报告") }) }) { pad ->
        val s = summary
        if (s == null) {
            Text("暂无报告，请先扫描", Modifier.padding(pad).padding(16.dp))
            return@Scaffold
        }

        val text = buildReport(s.timestamp, s.highRiskCount, s.mediumRiskCount, s.lowRiskCount, s.totalBytes,
            s.apps.take(20).map { Triple(it.appLabel, it.packageName, it.score) })

        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    text,
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Button(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(send, "分享报告"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("分享 / 导出报告") }
        }
    }
}

private fun buildReport(
    ts: Long,
    high: Int, medium: Int, low: Int,
    totalBytes: Long,
    top: List<Triple<String, String, Int>>,
): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return buildString {
        appendLine("===== ReverseGFW 应用审计报告 =====")
        appendLine("生成时间：${fmt.format(Date(ts))}")
        appendLine()
        appendLine("[概览]")
        appendLine("高危应用：$high")
        appendLine("中危应用：$medium")
        appendLine("低危应用：$low")
        appendLine("后台流量合计：${formatBytes(totalBytes)}")
        appendLine()
        appendLine("[风险 Top${top.size}]")
        top.forEachIndexed { i, (label, pkg, score) ->
            appendLine("${i + 1}. $label（$pkg）评分 $score")
        }
        appendLine()
        appendLine("说明：评分仅基于「包枚举权限 / 敏感权限 / 后台流量」三类可解释信号，")
        appendLine("供自用隐私审计参考，不代表应用存在恶意行为。")
    }
}