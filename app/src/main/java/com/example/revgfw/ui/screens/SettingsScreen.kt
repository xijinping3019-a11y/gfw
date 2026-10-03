package com.example.revgfw.ui.screens

import android.provider.Settings
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.revgfw.exec.ShizukuExec
import com.example.revgfw.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authorized = ShizukuExec.isAuthorized()
    val installed = ShizukuExec.isInstalled()

    Scaffold(topBar = { TopAppBar(title = { Text("设置") }) }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Shizuku 状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("已安装：${if (installed) "是" else "否"}")
                    Text(
                        "已授权：${if (authorized) "是" else "否"}",
                        color = if (authorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Button(
                        onClick = { ShizukuExec.requestPermission() },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    ) { Text("申请 / 重新授权") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val r = withContext(Dispatchers.IO) { ShizukuExec.run("id") }
                                vm.notify("测试执行：${r.pretty()}")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("测试执行（id）") }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("系统权限", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("流量统计需要「使用情况访问权限」", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("打开使用情况访问设置") }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                })
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("打开本应用系统设置") }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("关于", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("ReverseGFW · 自用隐私审计工具", fontWeight = FontWeight.SemiBold)
                    Text(
                        "本工具用于审计本机第三方应用的权限与后台流量，帮助用户自主管理隐私与资源占用。" +
                            "所有操作均作用于本机、由用户主动触发。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}