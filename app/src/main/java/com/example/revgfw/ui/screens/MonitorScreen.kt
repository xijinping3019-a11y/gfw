package com.example.revgfw.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.revgfw.ui.MainViewModel

/**
 * 监控页：基于最近一次扫描结果，实时呈现"值得关注的应用"。
 *
 * 说明：真正的后台常驻监控需要前台服务（已在 Manifest 预留权限），
 * 这里先做"按需刷新 + 重点列表"。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(vm: MainViewModel) {
    val summary by vm.summary.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("监控") }) }) { pad ->
        val s = summary
        if (s == null) {
            Column(
                Modifier.fillMaxSize().padding(pad),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("暂无数据，请先在「扫描」页执行一次", Modifier.padding(16.dp))
            }
            return@Scaffold
        }

        val watch = s.apps.filter { it.score >= 30 }
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("关注列表", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("评分 ≥ 30 的应用共 ${watch.size} 个")
                        Text(
                            "后台流量合计：${formatBytes(s.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (watch.isEmpty()) {
                item { Text("一切正常，没有需要关注的应用 👍") }
            } else {
                items(watch, key = { it.pkg }) { app ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(app.label, fontWeight = FontWeight.SemiBold)
                                Text(
                                    app.reasons.firstOrNull()?.title ?: "—",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "${app.score}",
                                fontWeight = FontWeight.Bold,
                                color = app.risk.color(),
                            )
                        }
                    }
                }
            }
        }
    }
}