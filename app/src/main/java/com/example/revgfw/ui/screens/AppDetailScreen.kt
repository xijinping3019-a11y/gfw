package com.example.revgfw.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.revgfw.exec.Actions
import com.example.revgfw.ui.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(vm: MainViewModel, onBack: () -> Unit) {
    val app by vm.selected.collectAsState()
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    var lastResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { pad ->
        val a = app
        if (a == null) {
            Text("未选择应用", Modifier.padding(pad).padding(16.dp))
            return@Scaffold
        }

        fun exec(label: String, block: () -> com.example.revgfw.exec.ShizukuExec.ExecResult) {
            scope.launch {
                val r = withContext(Dispatchers.IO) { block() }
                lastResult = "[$label] ${if (r.ok) "成功" else "失败(code=${r.code})"}\n${r.pretty()}"
                vm.notify("$label：${if (r.ok) "成功" else "失败"}")
            }
        }

        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(a.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(a.pkg, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "风险评分：${a.score} / 100（${a.risk.label}）",
                        color = a.risk.color(),
                        fontWeight = FontWeight.Bold,
                    )
                    Text("后台流量(24h)：${formatBytes(a.bgBytes)}")
                }
            }

            Text("评分依据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (a.reasons.isEmpty()) {
                Text("无明显风险项")
            } else {
                a.reasons.forEach { r ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(r.title, fontWeight = FontWeight.SemiBold)
                            Text(r.detail, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Text("操作", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exec("断网") { Actions.blockNetwork(a.pkg) } }) { Text("断网") }
                OutlinedButton(onClick = { exec("恢复联网") { Actions.unblockNetwork(a.pkg) } }) { Text("恢复联网") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exec("限制后台") { Actions.restrictBackground(a.pkg) } }) { Text("限制后台") }
                OutlinedButton(onClick = { exec("解除限制") { Actions.unrestrictBackground(a.pkg) } }) { Text("解除限制") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exec("冻结") { Actions.freeze(a.pkg) } }) { Text("冻结") }
                OutlinedButton(onClick = { exec("解冻") { Actions.unfreeze(a.pkg) } }) { Text("解冻") }
            }
            OutlinedButton(
                onClick = { exec("强制停止") { Actions.forceStop(a.pkg) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("强制停止") }

            lastResult?.let {
                Text("执行结果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(Modifier.fillMaxWidth()) {
                    Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

internal fun formatBytes(b: Long): String {
    if (b < 1024) return "$b B"
    val kb = b / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}