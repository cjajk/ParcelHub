package com.cjajk.parcelhub.ui.screen

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cjajk.parcelhub.data.PackageRecord
import com.cjajk.parcelhub.service.SmsReceiver
import com.cjajk.parcelhub.ui.viewmodel.MainViewModel
import com.cjajk.parcelhub.util.DeepLinks
import com.cjajk.parcelhub.util.ExpressQuery
import com.cjajk.parcelhub.util.ExpressResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val BgBlack = Color(0xFF0F0F0F)
val CardDark = Color(0xFF1C1C1E)
val BarDark = Color(0xFF1C1C1E)
val BorderGray = Color(0xFF2C2C2E)
val TextPrimary = Color(0xFFF2F2F7)
val TextSecondary = Color(0xFF98989F)
val CodeGreen = Color(0xFF30D158)
val AccentBlue = Color(0xFF0A84FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val packages by vm.packages.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var queryResult by remember { mutableStateOf<ExpressResult?>(null) }
    var queryLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val filter = IntentFilter(SmsReceiver.ACTION_PACKAGE_ARRIVED)
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { vm.refresh() }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    Scaffold(
        containerColor = BgBlack,
        bottomBar = { GlassBottomBar() }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "快递聚合",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showSearch = true }) {
                    Icon(Icons.Default.Search, contentDescription = "查询快递",
                        tint = TextPrimary, modifier = Modifier.size(28.dp))
                }
            }

            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                val waiting = packages.count { !it.pickedUp }
                val done = packages.count { it.pickedUp }
                Tab(selected = tab == 0, onClick = { tab = 0 },
                    text = { Text("待取 $waiting", fontWeight = FontWeight.Medium, color = TextPrimary) })
                Tab(selected = tab == 1, onClick = { tab = 1 },
                    text = { Text("已取 $done", fontWeight = FontWeight.Medium, color = TextSecondary) })
            }

            val list = packages.filter { if (tab == 0) !it.pickedUp else it.pickedUp }
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (queryResult != null) {
                    item(key = "express_query") {
                        ExpressQueryCard(queryResult!!, onDismiss = { queryResult = null })
                    }
                }
                if (list.isEmpty() && queryResult == null) {
                    item { EmptyHint(tab) }
                }
                items(list, key = { it.id }) { item ->
                    PackageCard(item, onToggle = { vm.togglePickedUp(item) })
                }
            }
        }
    }

    if (showSearch) {
        SearchDialog(
            loading = queryLoading,
            onDismiss = { showSearch = false },
            onQuery = { nu ->
                showSearch = false
                queryLoading = true
                queryResult = null
                scope.launch {
                    val result = ExpressQuery.query(nu)
                    queryLoading = false
                    queryResult = result
                    if (result == null) {
                        Toast.makeText(context, "查询失败，请检查单号或稍后再试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun SearchDialog(loading: Boolean, onDismiss: () -> Unit, onQuery: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("查询快递", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("输入快递单号") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderGray,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = AccentBlue
                    )
                )
                if (loading) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("查询中...", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onQuery(text.trim()) },
                enabled = text.isNotBlank() && !loading
            ) { Text("查询", color = AccentBlue, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}

@Composable
private fun ExpressQueryCard(result: ExpressResult, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    result.com,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (result.state) {
                                3 -> Color(0xFF30D158).copy(alpha = 0.15f)
                                5 -> Color(0xFFFF9F0A).copy(alpha = 0.15f)
                                else -> Color(0xFF0A84FF).copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        result.stateText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (result.state) {
                            3 -> CodeGreen
                            5 -> Color(0xFFFF9F0A)
                            else -> AccentBlue
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(result.nu, fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(12.dp))
            Text(result.latestContext, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(result.latestTime, fontSize = 12.sp, color = TextSecondary)

            if (result.estimatedDays != null && result.state != 3) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (result.estimatedDays <= 0) "预计今天送达"
                        else if (result.estimatedDays == 1) "预计明天送达"
                        else "预计 ${result.estimatedDays} 天后送达",
                        fontSize = 14.sp,
                        color = AccentBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss) { Text("关闭", color = TextSecondary, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun GlassBottomBar() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(BarDark)
                .border(1.dp, BorderGray, RoundedCornerShape(32.dp))
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlatformBtn("淘", "淘宝", Color(0xFFFF6A00)) { DeepLinks.openTaobaoIdentity(context) }
            PlatformBtn("拼", "拼多多", Color(0xFFE2231A)) { DeepLinks.openPddPickup(context) }
            PlatformBtn("菜", "菜鸟", Color(0xFF1E88E5)) { DeepLinks.openAlipayCainiao(context) }
            PlatformBtn("顺", "顺丰", Color(0xFF8E8E93)) { DeepLinks.openSf(context) }
            PlatformBtn("JD", "京东", Color(0xFFE1251B)) { DeepLinks.openJd(context) }
        }
    }
}

@Composable
private fun PlatformBtn(icon: String, label: String, bg: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                icon,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (icon.length > 1) 13.sp else 16.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun PackageCard(item: PackageRecord, onToggle: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("取件码", item.code))
            Toast.makeText(context, "已复制 ${item.code}", Toast.LENGTH_SHORT).show()
        }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.code,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = CodeGreen
                )
                if (item.station.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.station, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.carrier.isNotBlank()) {
                        Text(item.carrier, style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(formatTime(item.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }
            }
            FilledTonalButton(onClick = onToggle, shape = RoundedCornerShape(14.dp)) {
                Text(if (item.pickedUp) "撤销" else "已取")
            }
        }
    }
}

@Composable
private fun EmptyHint(tab: Int) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Inbox, contentDescription = null,
                modifier = Modifier.size(64.dp), tint = Color(0xFF48484A))
            Spacer(Modifier.height(12.dp))
            Text(
                if (tab == 0) "没有待取的快递\n收到取件短信后会自动出现"
                else "没有已取记录",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    }
}

private fun formatTime(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000} 分钟前"
        diff < 86400_000 -> "${diff / 3600_000} 小时前"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
    }
}
