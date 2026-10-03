package com.example.revgfw.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.revgfw.data.AppAudit
import com.example.revgfw.data.Risk
import com.example.revgfw.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(vm: MainViewModel, onOpenDetail: () -> Unit) {
    val summary by vm.summary.collectAsState()
    val scanning by vm.scanning.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("应用审计") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.scan() },
                icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                text = { Text("扫描") },
            )
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (scanning) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text("正在扫描应用…", Modifier.padding(top = 12.dp))
                }
            } else if (summary == null) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("尚未扫描", style = MaterialTheme.typography.titleLarge)
                    Text("点击右下角「扫描」开始审计", Modifier.padding(top = 8.dp))
                }
            } else {
                val s = summary!!
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { SummaryCard(s.high, s.medium, s.low, s.apps.size) }
                    items(s.apps, key = { it.pkg }) { app ->
                        AppRow(app) {
                            vm.select(app)
                            onOpenDetail()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(high: Int, medium: Int, low: Int, total: Int) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    )) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("扫描概览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("共 $total 个第三方应用")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("高危 $high", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Text("中危 $medium")
                Text("低危 $low")
            }
        }
    }
}

@Composable
private fun AppRow(app: AppAudit, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(10.dp).clip(CircleShape).background(app.risk.color()),
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(app.label, fontWeight = FontWeight.SemiBold)
                Text(
                    app.pkg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { app.score / 100f },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    color = app.risk.color(),
                )
            }
            Text(
                "${app.score}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = app.risk.color(),
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

internal fun Risk.color() = when (this) {
    Risk.HIGH -> androidx.compose.ui.graphics.Color(0xFFE53935)
    Risk.MEDIUM -> androidx.compose.ui.graphics.Color(0xFFFB8C00)
    Risk.LOW -> androidx.compose.ui.graphics.Color(0xFF43A047)
}