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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cjajk.parcelhub.data.PackageRecord
import com.cjajk.parcelhub.service.SmsReceiver
import com.cjajk.parcelhub.ui.viewmodel.MainViewModel
import com.cjajk.parcelhub.util.DeepLinks
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val packages by vm.packages.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

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
        containerColor = Color(0xFFF2F3F7),
        bottomBar = { GlassBottomBar() }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                "快递聚合",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
            )
            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                val waiting = packages.count { !it.pickedUp }
                val done = packages.count { it.pickedUp }
                Tab(selected = tab == 0, onClick = { tab = 0 },
                    text = { Text("待取 $waiting", fontWeight = FontWeight.Medium) })
                Tab(selected = tab == 1, onClick = { tab = 1 },
                    text = { Text("已取 $done", fontWeight = FontWeight.Medium) })
            }
            val list = packages.filter { if (tab == 0) !it.pickedUp else it.pickedUp }
            if (list.isEmpty()) {
                EmptyHint(tab)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 110.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it.id }) { item ->
                        PackageCard(item, onToggle = { vm.togglePickedUp(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassBottomBar() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.75f),
                            Color.White.copy(alpha = 0.55f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlatformBtn("淘", Color(0xFFFF6A00)) { DeepLinks.openTaobaoIdentity(context) }
            PlatformBtn("拼", Color(0xFFE2231A)) { DeepLinks.openPddPickup(context) }
            PlatformBtn("菜", Color(0xFF1E88E5)) { DeepLinks.openAlipayCainiao(context) }
            PlatformBtn("顺", Color(0xFF1A1A1A)) { DeepLinks.openSf(context) }
            PlatformBtn("JD", Color(0xFFE1251B)) { DeepLinks.openJd(context) }
        }
    }
}

@Composable
private fun PlatformBtn(label: String, bg: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (label.length > 1) 13.sp else 17.sp
            )
        }
    }
}

@Composable
private fun PackageCard(item: PackageRecord, onToggle: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                    color = Color(0xFF1B5E20)
                )
                if (item.station.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.station, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.carrier.isNotBlank()) {
                        Text(item.carrier, style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF888888))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(formatTime(item.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA))
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Inbox, contentDescription = null,
                modifier = Modifier.size(64.dp), tint = Color(0xFFCCCCCC))
            Spacer(Modifier.height(12.dp))
            Text(
                if (tab == 0) "没有待取的快递\n收到取件短信后会自动出现"
                else "没有已取记录",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF999999)
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
