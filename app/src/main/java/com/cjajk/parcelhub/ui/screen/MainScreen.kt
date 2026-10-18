package com.cjajk.parcelhub.ui.screen

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cjajk.parcelhub.data.PackageRecord
import com.cjajk.parcelhub.data.SmsParser
import com.cjajk.parcelhub.service.SmsReceiver
import com.cjajk.parcelhub.ui.viewmodel.MainViewModel
import com.cjajk.parcelhub.util.COM_LIST
import com.cjajk.parcelhub.util.DeepLinks
import com.cjajk.parcelhub.util.ExpressQuery
import com.cjajk.parcelhub.util.ExpressQueryException
import com.cjajk.parcelhub.util.ExpressResult
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
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

data class Platform(
    val id: String,
    val packages: List<String>,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: (Context) -> Unit
)

private fun defaultPlatforms(context: Context) = listOf(
    Platform("taobao", listOf("com.taobao.taobao"), "淘宝", Icons.Default.ShoppingBag) { DeepLinks.openTaobaoIdentity(it) },
    Platform("pdd", listOf("com.xunmeng.pinduoduo"), "拼多多", Icons.Default.ShoppingCart) { DeepLinks.openPddPickup(it) },
    Platform("cainiao", listOf("com.eg.android.AlipayGphone"), "菜鸟", Icons.Default.LocalShipping) { DeepLinks.openAlipayCainiao(it) },
    Platform("sf", listOf("com.sf.activity", "com.sfexpress.activity"), "顺丰", Icons.Outlined.MailOutline) { DeepLinks.openSf(it) },
    Platform("jd", listOf("com.jingdong.app.mall"), "京东", Icons.Default.Store) { DeepLinks.openJd(it) }
)

private const val SP_ORDER = "platform_order"
private const val SP_SIDE_OFFSET = "side_bar_offset"

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val packages by vm.packages.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showPasteImport by remember { mutableStateOf(false) }
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

    var verticalBar by remember { mutableStateOf(false) }
    val sp = remember { context.getSharedPreferences("parcel_hub", Context.MODE_PRIVATE) }
    var sideOffsetPx by remember { mutableIntStateOf(sp.getInt(SP_SIDE_OFFSET, 0)) }

    val barAngle by animateFloatAsState(
        targetValue = if (verticalBar) 90f else 0f,
        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
        label = "barAngle"
    )
    val angleSettled = barAngle <= 0.5f || barAngle >= 89.5f

    val bottomPad by animateDpAsState(
        targetValue = if (verticalBar) 0.dp else 84.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bottomPad"
    )
    val endPad by animateDpAsState(
        targetValue = if (verticalBar) 64.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "endPad"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(containerColor = BgBlack) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(bottom = bottomPad, end = endPad)
                    .fillMaxSize()
            ) {
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
                    IconButton(onClick = { showPasteImport = true }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "粘贴导入", tint = TextPrimary,
                            modifier = Modifier.size(28.dp))
                    }
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
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (queryResult != null) {
                        item(key = "express_query") {
                            ExpressQueryCard(queryResult!!, onDismiss = { queryResult = null },
                                onSave = {
                                    vm.addManual(queryResult!!.nu, "", queryResult!!.com)
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
                            onDelete = { vm.delete(item) },
                            modifier = Modifier.animateItem())
                    }
                }
            }
        }

        if (!verticalBar || !angleSettled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        rotationZ = barAngle
                        alpha = (1f - barAngle / 90f).coerceIn(0f, 1f)
                    }
            ) {
                GlassBottomBar(onToggleVertical = { verticalBar = true })
            }
        }

        if (verticalBar || !angleSettled) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer {
                        rotationZ = barAngle - 90f
                        alpha = (barAngle / 90f).coerceIn(0f, 1f)
                    }
            ) {
                VerticalBarStandalone(
                    onToggleVertical = { verticalBar = false },
                    offsetPx = sideOffsetPx,
                    onOffsetChange = { sideOffsetPx = it },
                    onOffsetCommit = { px ->
                        sideOffsetPx = px
                        sp.edit().putInt(SP_SIDE_OFFSET, px).apply()
                    }
                )
            }
        }
    }

    if (showSearch) {
        SearchDialog(
            loading = queryLoading,
            onDismiss = { showSearch = false },
            onQuery = { nu, com, phone ->
                showSearch = false
                queryLoading = true
                queryResult = null
                scope.launch {
                    try {
                        val result = ExpressQuery.query(nu, com, phone.ifBlank { null })
                        queryResult = result
                    } catch (e: ExpressQueryException) {
                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    }
                    queryLoading = false
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

    if (showPasteImport) {
        PasteImportDialog(
            onDismiss = { showPasteImport = false },
            onImport = { raw ->
                showPasteImport = false
                // 按换行分割成多条短信，逐条解析
                val blocks = raw.split(Regex("\\n\\s*\\n|\\n"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                var count = 0
                blocks.forEach { block ->
                    val record = SmsParser.parse(null, block)
                    if (record != null) {
                        vm.addManual(record.code, record.station, record.carrier)
                        count++
                    }
                }
                Toast.makeText(context, "已导入 $count 条", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasteImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    val context = LocalContext.current
    var text by remember {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        mutableStateOf(cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("粘贴短信导入", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("把收到的取件短信粘贴在下面，支持多条一起粘贴",
                    fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("【京东快递】取件码C5-1227...", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = darkFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onImport(text) },
                enabled = text.isNotBlank()) {
                Text("解析并导入", color = AccentBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) } }
    )
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
private fun SearchDialog(loading: Boolean, onDismiss: () -> Unit, onQuery: (String, String?, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedCom by remember { mutableStateOf<Pair<String, String>?>(null) }
    var phone by remember { mutableStateOf("") }
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
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { if (it.length <= 4) phone = it },
                    label = { Text("手机后4位（顺丰/京东必填）") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = darkFieldColors())
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
            TextButton(onClick = { if (text.isNotBlank()) onQuery(text.trim(), selectedCom?.first, phone) },
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
            result.traces.take(5).forEachIndexed { i, trace ->
                Column {
                    Text(trace.context, fontSize = 14.sp,
                        color = if (i == 0) TextPrimary else TextSecondary)
                    Text(trace.time, fontSize = 11.sp, color = TextSecondary)
                    if (i < result.traces.take(5).size - 1) Spacer(Modifier.height(8.dp))
                }
            }
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
private fun GlassBottomBar(onToggleVertical: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val allPlatforms = defaultPlatforms(context)
    val sp = remember { context.getSharedPreferences("parcel_hub", Context.MODE_PRIVATE) }
    val order = remember {
        mutableStateListOf<String>().apply {
            addAll(sp.getString(SP_ORDER, null)?.split(",")?.filter { id -> allPlatforms.any { it.id == id } }
                ?: allPlatforms.map { it.id })
        }
    }
    val density = LocalDensity.current
    val itemW = 56.dp
    val itemWpx = with(density) { itemW.toPx() }
    var dragIdx by remember { mutableStateOf(-1) }
    var dragOff by remember { mutableFloatStateOf(0f) }
    val animOff = animateFloatAsState(
        targetValue = if (dragIdx >= 0) dragOff else 0f,
        animationSpec = spring(stiffness = 2000f, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "d"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(BarDark.copy(alpha = 0.9f))
            .border(1.dp, BorderGray, RoundedCornerShape(32.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        order.forEachIndexed { index, id ->
            val platform = allPlatforms.first { it.id == id }
            ReorderableIcon(
                platform = platform,
                shaking = dragIdx >= 0 && dragIdx != index,
                dragging = dragIdx == index,
                offsetState = animOff,
                onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); dragIdx = index; dragOff = 0f },
                onDrag = { dx -> dragOff += dx },
                onDragEnd = {
                    val shift = (dragOff / itemWpx).roundToInt()
                    val target = (dragIdx + shift).coerceIn(0, order.lastIndex)
                    if (target != dragIdx) {
                        val t = order.removeAt(dragIdx)
                        order.add(target, t)
                        sp.edit().putString(SP_ORDER, order.joinToString(",")).apply()
                    }
                    dragIdx = -1
                    dragOff = 0f
                },
                onClick = { platform.onClick(context) }
            )
        }
        TransposeKey(
            vertical = false,
            offsetPx = 0,
            minOffsetPx = 0,
            maxOffsetPx = 0,
            onToggle = onToggleVertical,
            onOffsetChange = {},
            onOffsetCommit = {}
        )
    }
}

@Composable
private fun VerticalBarStandalone(
    onToggleVertical: () -> Unit,
    offsetPx: Int,
    onOffsetChange: (Int) -> Unit,
    onOffsetCommit: (Int) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val allPlatforms = defaultPlatforms(context)
    val sp = remember { context.getSharedPreferences("parcel_hub", Context.MODE_PRIVATE) }
    val order = remember {
        mutableStateListOf<String>().apply {
            addAll(sp.getString(SP_ORDER, null)?.split(",")?.filter { id -> allPlatforms.any { it.id == id } }
                ?: allPlatforms.map { it.id })
        }
    }
    val density = LocalDensity.current
    val itemH = 56.dp
    val itemHpx = with(density) { itemH.toPx() }
    var dragIdx by remember { mutableStateOf(-1) }
    var dragOff by remember { mutableFloatStateOf(0f) }
    val animOff = animateFloatAsState(
        targetValue = if (dragIdx >= 0) dragOff else 0f,
        animationSpec = spring(stiffness = 2000f, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "d"
    )

    BoxWithConstraints {
        val maxPx = with(density) { maxHeight.roundToPx() }
        val fixedHeightPx = minOf(
            with(density) { (itemH * order.size + 36.dp + 20.dp).roundToPx() },
            maxPx
        )
        val limitPx = ((maxPx - fixedHeightPx) / 2).coerceAtLeast(0)
        val clampedOffset = offsetPx.coerceIn(-limitPx, limitPx)

        Column(
            modifier = Modifier
                .offset { IntOffset(0, clampedOffset) }
                .width(64.dp)
                .height(with(density) { fixedHeightPx.toDp() })
                .clip(RoundedCornerShape(32.dp))
                .background(BarDark.copy(alpha = 0.9f))
                .border(1.dp, BorderGray, RoundedCornerShape(32.dp))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    order.forEachIndexed { index, id ->
                        val platform = allPlatforms.first { it.id == id }
                        ReorderableIcon(
                            platform = platform,
                            shaking = dragIdx >= 0 && dragIdx != index,
                            dragging = dragIdx == index,
                            offsetState = animOff,
                            vertical = true,
                            onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); dragIdx = index; dragOff = 0f },
                            onDrag = { dy -> dragOff += dy },
                            onDragEnd = {
                                val shift = (dragOff / itemHpx).roundToInt()
                                val target = (dragIdx + shift).coerceIn(0, order.lastIndex)
                                if (target != dragIdx) {
                                    val t = order.removeAt(dragIdx)
                                    order.add(target, t)
                                    sp.edit().putString(SP_ORDER, order.joinToString(",")).apply()
                                }
                                dragIdx = -1
                                dragOff = 0f
                            },
                            onClick = { platform.onClick(context) }
                        )
                    }
                }
            }
            TransposeKey(
                vertical = true,
                offsetPx = clampedOffset,
                minOffsetPx = -limitPx,
                maxOffsetPx = limitPx,
                onToggle = onToggleVertical,
                onOffsetChange = onOffsetChange,
                onOffsetCommit = onOffsetCommit
            )
        }
    }
}

@Composable
private fun TransposeKey(
    vertical: Boolean,
    offsetPx: Int,
    minOffsetPx: Int,
    maxOffsetPx: Int,
    onToggle: () -> Unit,
    onOffsetChange: (Int) -> Unit,
    onOffsetCommit: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val curOffset by rememberUpdatedState(offsetPx)
    val curMin by rememberUpdatedState(minOffsetPx)
    val curMax by rememberUpdatedState(maxOffsetPx)
    val curOnChange by rememberUpdatedState(onOffsetChange)
    val curOnCommit by rememberUpdatedState(onOffsetCommit)

    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragActive by remember { mutableStateOf(false) }
    var startOffset by remember { mutableIntStateOf(0) }
    var startRootY by remember { mutableFloatStateOf(0f) }
    var lastOffset by remember { mutableIntStateOf(0) }

    val keyScale by animateFloatAsState(
        targetValue = if (dragActive) 1.2f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "keyScale"
    )

    fun rootY(local: Offset): Float = (coords?.positionInRoot()?.y ?: 0f) + local.y

    Box(
        modifier = Modifier
            .size(36.dp)
            .onGloballyPositioned { coords = it }
            .clip(CircleShape)
            .background(BorderGray)
            .clickable { onToggle() }
            .then(
                if (vertical) Modifier.pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { pos ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            startOffset = curOffset
                            lastOffset = curOffset
                            startRootY = rootY(pos)
                            dragActive = true
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val new = (startOffset + (rootY(change.position) - startRootY))
                                .roundToInt()
                                .coerceIn(curMin, curMax)
                            lastOffset = new
                            curOnChange(new)
                        },
                        onDragEnd = {
                            dragActive = false
                            curOnCommit(lastOffset)
                        },
                        onDragCancel = {
                            dragActive = false
                            curOnCommit(lastOffset)
                        }
                    )
                } else Modifier
            )
            .graphicsLayer {
                scaleX = keyScale
                scaleY = keyScale
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (vertical) Icons.Default.SwapVert else Icons.Default.SwapHoriz,
            contentDescription = if (vertical) "转回底部" else "转至侧边",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ReorderableIcon(
    platform: Platform,
    shaking: Boolean,
    dragging: Boolean,
    offsetState: State<Float>,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    onLongPress: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val shakeRotState = if (shaking) {
        rememberInfiniteTransition(label = "shake").animateFloat(
            initialValue = -3f, targetValue = 3f,
            animationSpec = infiniteRepeatable(tween(80), RepeatMode.Reverse),
            label = "rot"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val iconScale = animateFloatAsState(
        targetValue = if (dragging) 1.2f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    val iconAlpha = animateFloatAsState(
        targetValue = if (dragging) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .pointerInput(platform.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress() },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(if (vertical) amount.y else amount.x)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .clickable { onClick() }
            .graphicsLayer {
                val off = if (dragging) offsetState.value else 0f
                translationX = if (vertical) 0f else off
                translationY = if (vertical) off else 0f
                rotationZ = if (shaking) shakeRotState.value else 0f
                scaleX = iconScale.value
                scaleY = iconScale.value
                alpha = iconAlpha.value
                shadowElevation = if (dragging) 14f else 0f
            }
    ) {
        Icon(
            imageVector = platform.icon,
            contentDescription = platform.label,
            tint = TextPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(platform.label, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun PackageCard(item: PackageRecord, onToggle: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
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
                Text(item.code, fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    color = if (item.pickedUp) TextSecondary else CodeGreen,
                    lineHeight = 40.sp,
                    textDecoration = if (item.pickedUp) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
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
