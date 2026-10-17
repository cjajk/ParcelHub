package com.cjajk.parcelhub.ui.screen

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToPx
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.cjajk.parcelhub.data.PackageRecord
import com.cjajk.parcelhub.service.SmsReceiver
import com.cjajk.parcelhub.ui.viewmodel.MainViewModel
import com.cjajk.parcelhub.util.DeepLinks
import com.cjajk.parcelhub.util.ExpressQuery
import com.cjajk.parcelhub.util.ExpressResult
import com.cjajk.parcelhub.util.COM_LIST
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

// 平台配置（id 用于排序持久化）
data class Platform(
    val id: String,
    val packages: List<String>,
    val label: String,
    val fallbackIcon: String,
    val fallbackBg: Color,
    val onClick: (Context) -> Unit
)

private fun defaultPlatforms(context: Context) = listOf(
    Platform("taobao", listOf("com.taobao.taobao"), "淘宝", "淘", Color(0xFFFF6A00)) { DeepLinks.openTaobaoIdentity(it) },
    Platform("pdd", listOf("com.xunmeng.pinduoduo"), "拼多多", "拼", Color(0xFFE2231A)) { DeepLinks.openPddPickup(it) },
    Platform("cainiao", listOf("com.eg.android.AlipayGphone"), "菜鸟", "菜", Color(0xFF1E88E5)) { DeepLinks.openAlipayCainiao(it) },
    Platform("sf", listOf("com.sf.activity", "com.sfexpress.activity"), "顺丰", "顺", Color(0xFF8E8E93)) { DeepLinks.openSf(it) },
    Platform("jd", listOf("com.jingdong.app.mall"), "京东", "JD", Color(0xFFE1251B)) { DeepLinks.openJd(it) }
)

private const val SP_ORDER = "platform_order"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val packages by vm.packages.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
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
                    .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "快递聚合",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, contentDescription = "手动添加", tint = TextPrimary,
                        modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { showSearch = true }) {
                    Icon(Icons.Default.Search, contentDescription = "查询快递", tint = TextPrimary,
                        modifier = Modifier.size(28.dp))
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
                        ExpressQueryCard(queryResult!!, onDismiss = { queryResult = null },
                            onSave = {
                                vm.addManual(queryResult!!.nu, queryResult!!.com, "")
                                Toast.makeText(context, "已保存到列表", Toast.LENGTH_SHORT).show()
                                queryResult = null
                            })
                    }
                }
                if (list.isEmpty() && queryResult == null) {
                    item { EmptyHint(tab) }
                }
                items(list, key = { it.id }) { item ->
                    PackageCard(item, onToggle = { vm.togglePickedUp(item) },
                        onDelete = { vm.delete(item) })
                }
            }
        }
    }

    if (showSearch) {
        SearchDialog(
            loading = queryLoading,
            onDismiss = { showSearch = false },
            onQuery = { nu, com ->
                showSearch = false
                queryLoading = true
                queryResult = null
                scope.launch {
                    val result = ExpressQuery.query(nu, com)
                    queryLoading = false
                    queryResult = result
                    if (result == null) {
                        Toast.makeText(context, "查询失败，请检查单号或手动选择快递公司", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showAdd) {
        AddManualDialog(
            onDismiss = { showAdd = false },
            onAdd = { code, station, carrier ->
                showAdd = false
                vm.addManual(code, station, carrier)
                Toast.makeText(context, "已添加", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddManualDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var station by remember { mutableStateOf("") }
    var carrier by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("手动添加取件码", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = code, onValueChange = { code = it },
                    label = { Text("取件码（如 3-2-501）") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = darkFieldColors())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = station, onValueChange = { station = it },
                    label = { Text("取件地址（驿站/柜机名）") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = darkFieldColors())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = carrier, onValueChange = { carrier = it },
                    label = { Text("快递公司（可选）") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = darkFieldColors())
            }
        },
        confirmButton = {
            TextButton(onClick = { if (code.isNotBlank()) onAdd(code.trim(), station.trim(), carrier.trim()) },
                enabled = code.isNotBlank()) {
                Text("添加", color = AccentBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchDialog(loading: Boolean, onDismiss: () -> Unit, onQuery: (String, String?) -> Unit) {
    var text by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedCom by remember { mutableStateOf<Pair<String, String>?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("查询快递", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it },
                    label = { Text("输入快递单号") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = darkFieldColors())
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedCom?.second ?: "自动识别（可选）",
                        onValueChange = {}, readOnly = true,
                        label = { Text("快递公司") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), colors = darkFieldColors())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                        containerColor = CardDark) {
                        COM_LIST.forEach { item ->
                            DropdownMenuItem(text = { Text(item.second, color = TextPrimary) },
                                onClick = { selectedCom = item; expanded = false })
                        }
                    }
                }
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
            TextButton(onClick = { if (text.isNotBlank()) onQuery(text.trim(), selectedCom?.first) },
                enabled = text.isNotBlank() && !loading) {
                Text("查询", color = AccentBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) } }
    )
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedBorderColor = AccentBlue, unfocusedBorderColor = BorderGray,
    focusedLabelColor = AccentBlue, unfocusedLabelColor = TextSecondary,
    cursorColor = AccentBlue
)

@Composable
private fun ExpressQueryCard(result: ExpressResult, onDismiss: () -> Unit, onSave: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(result.com, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                    modifier = Modifier.weight(1f))
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(when (result.state) {
                        3 -> CodeGreen.copy(alpha = 0.15f)
                        5 -> Color(0xFFFF9F0A).copy(alpha = 0.15f)
                        else -> AccentBlue.copy(alpha = 0.15f)
                    }).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(result.stateText, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = when (result.state) { 3 -> CodeGreen; 5 -> Color(0xFFFF9F0A); else -> AccentBlue })
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
                Row(modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    .background(AccentBlue.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(if (result.estimatedDays <= 0) "预计今天送达"
                        else if (result.estimatedDays == 1) "预计明天送达"
                        else "预计 ${result.estimatedDays} 天后送达",
                        fontSize = 14.sp, color = AccentBlue, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = onSave) { Text("保存到列表", color = AccentBlue, fontSize = 13.sp) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("关闭", color = TextSecondary, fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun GlassBottomBar() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val allPlatforms = defaultPlatforms(context)

    // 读取保存的顺序
    val sp = remember { context.getSharedPreferences("parcel_hub", Context.MODE_PRIVATE) }
    var order by remember {
        mutableStateOf(
            sp.getString(SP_ORDER, null)?.split(",")?.filter { id -> allPlatforms.any { it.id == id } }
                ?: allPlatforms.map { it.id }
        )
    }
    val orderedPlatforms = order.map { id -> allPlatforms.first { it.id == id } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(BarDark.copy(alpha = 0.9f))
                .border(1.dp, BorderGray, RoundedCornerShape(32.dp))
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            orderedPlatforms.forEach { platform ->
                DraggablePlatformBtn(platform = platform,
                    onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onSwap = { from, to ->
                        order = order.toMutableList().apply {
                            val t = removeAt(from)
                            add(to, t)
                        }
                        sp.edit().putString(SP_ORDER, order.joinToString(",")).apply()
                    })
            }
        }
    }
}

@Composable
private fun DraggablePlatformBtn(
    platform: Platform,
    onDragStart: () -> Unit,
    onSwap: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val iconSizePx = with(density) { 44.dp.roundToPx() }
    val appIcon: Drawable? = remember(platform.packages) { loadAppIcon(context, platform.packages) }
    val iconBitmap = remember(appIcon, iconSizePx) {
        appIcon?.let { runCatching { it.toBitmap(iconSizePx, iconSizePx) }.getOrNull() }
    }
    var dragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .pointerInput(platform.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart(); dragging = true },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragX += dragAmount.x
                    },
                    onDragEnd = { dragging = false; dragX = 0f },
                    onDragCancel = { dragging = false; dragX = 0f }
                )
            }
            .clickable { platform.onClick(context) }
            .graphicsLayer {
                translationX = if (dragging) dragX else 0f
                scaleX = if (dragging) 1.15f else 1f
                scaleY = if (dragging) 1.15f else 1f
                alpha = if (dragging) 0.9f else 1f
            }
    ) {
        Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            if (iconBitmap != null) {
                Image(bitmap = iconBitmap.asImageBitmap(), contentDescription = platform.label,
                    modifier = Modifier.size(44.dp))
            } else {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(platform.fallbackBg),
                    contentAlignment = Alignment.Center) {
                    Text(platform.fallbackIcon, color = Color.White, fontWeight = FontWeight.Bold,
                        fontSize = if (platform.fallbackIcon.length > 1) 13.sp else 16.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(platform.label, fontSize = 11.sp, color = TextSecondary)
    }
}

private fun loadAppIcon(context: Context, packages: List<String>): Drawable? {
    for (pkg in packages) {
        try { return context.packageManager.getApplicationIcon(pkg) }
        catch (_: PackageManager.NameNotFoundException) {}
    }
    return null
}

@Composable
private fun PackageCard(item: PackageRecord, onToggle: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .pointerInput(item.id) {
                    detectTapGestures(
                        onTap = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("取件码", item.code))
                            Toast.makeText(context, "已复制 ${item.code}", Toast.LENGTH_SHORT).show()
                        },
                        onLongPress = { showDeleteDialog = true }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.code, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = CodeGreen)
                if (item.station.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.station, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.carrier.isNotBlank()) {
                        Text(item.carrier, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(formatTime(item.timestamp), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            FilledTonalButton(onClick = onToggle, shape = RoundedCornerShape(14.dp)) {
                Text(if (item.pickedUp) "撤销" else "已取")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = CardDark,
            title = { Text("删除这条记录？", color = TextPrimary) },
            text = { Text("取件码 ${item.code} 将被永久删除", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("删除", color = Color(0xFFFF453A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消", color = TextSecondary) }
            }
        )
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
                style = MaterialTheme.typography.bodyLarge, color = TextSecondary
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
