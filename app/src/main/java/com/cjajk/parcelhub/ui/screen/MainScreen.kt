package com.cjajk.parcelhub.ui.screen

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cjajk.parcelhub.R
import com.cjajk.parcelhub.data.PackageRecord
import com.cjajk.parcelhub.service.SmsReceiver
import com.cjajk.parcelhub.ui.viewmodel.MainViewModel
import com.cjajk.parcelhub.util.DeepLinks
import com.cjajk.parcelhub.util.ExpressQuery
import com.cjajk.parcelhub.util.ExpressQueryException
import com.cjajk.parcelhub.util.ExpressResult
import com.cjajk.parcelhub.util.COM_LIST
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
    val iconRes: Int,
    val ringColor: Color,
    val onClick: (Context) -> Unit
)

private fun defaultPlatforms(context: Context) = listOf(
    Platform("taobao", listOf("com.taobao.taobao"), "淘宝", R.drawable.ic_platform_taobao, Color(0xFFFF6A00)) { DeepLinks.openTaobaoIdentity(it) },
    Platform("pdd", listOf("com.xunmeng.pinduoduo"), "拼多多", R.drawable.ic_platform_pdd, Color(0xFFE62E2E)) { DeepLinks.openPddPickup(it) },
    Platform("cainiao", listOf("com.eg.android.AlipayGphone"), "菜鸟", R.drawable.ic_platform_cainiao, Color(0xFF3C8CF0)) { DeepLinks.openAlipayCainiao(it) },
    Platform("sf", listOf("com.sf.activity", "com.sfexpress.activity"), "顺丰", R.drawable.ic_platform_sf, Color(0xFFB8BCC4)) { DeepLinks.openSf(it) },
    Platform("jd", listOf("com.jingdong.app.mall"), "京东", R.drawable.ic_platform_jd, Color(0xFFC8452E)) { DeepLinks.openJd(it) }
)

private const val SP_ORDER = "platform_order"
private const val SP_SIDE_HEIGHT = "side_bar_height"

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

    var verticalBar by remember { mutableStateOf(false) }
    val sp = remember { context.getSharedPreferences("parcel_hub", Context.MODE_PRIVATE) }
    var sideHeightPx by remember { mutableIntStateOf(sp.getInt(SP_SIDE_HEIGHT, 0)) }

    val bottomPad by animateDpAsState(
        targetValue = if (verticalBar) 0.dp else 100.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bottomPad"
    )
    val endPad by animateDpAsState(
        targetValue = if (verticalBar) 68.dp else 0.dp,
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
                        onDelete = { vm.delete(item) })
                }
            }
        }
        }

        AnimatedContent(
            targetState = verticalBar,
            transitionSpec = {
                val enter = if (targetState) slideInHorizontally { it } + fadeIn()
                            else slideInVertically { it } + fadeIn()
                val exit = if (targetState) slideOutVertically { -it } + fadeOut()
                           else slideOutHorizontally { -it } + fadeOut()
                (enter togetherWith exit) using SizeTransform(clip = false) { _, _ ->
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                }
            },
            modifier = Modifier.align(if (verticalBar) Alignment.CenterEnd else Alignment.BottomCenter)
        ) { isVertical ->
            if (isVertical) {
                VerticalBarStandalone(
                    onToggleVertical = { verticalBar = false },
                    heightPx = sideHeightPx,
                    onHeightChange = { px ->
                        sideHeightPx = px
                        sp.edit().putInt(SP_SIDE_HEIGHT, px).apply()
                    }
                )
            } else {
                GlassBottomBar(onToggleVertical = { verticalBar = true })
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
    val animOff by animateFloatAsState(
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
                animatedOffset = if (dragIdx == index) animOff else 0f,
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
            currentHeightPx = 0,
            minPx = 0,
            maxPx = 0,
            onToggle = onToggleVertical,
            onHeightChange = {}
        )
    }
}

@Composable
private fun VerticalBarStandalone(
    onToggleVertical: () -> Unit,
    heightPx: Int,
    onHeightChange: (Int) -> Unit
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
    val animOff by animateFloatAsState(
        targetValue = if (dragIdx >= 0) dragOff else 0f,
        animationSpec = spring(stiffness = 2000f, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "d"
    )

    BoxWithConstraints(modifier = Modifier) {
        val maxPx = with(density) { maxHeight.roundToPx() }
        val minPx = with(density) { 220.dp.roundToPx() }
        val effectiveHeight = heightPx.takeIf { it in minPx..maxPx } ?: (maxPx / 2)

        Column(
            modifier = Modifier
                .width(64.dp)
                .height(with(density) { effectiveHeight.toDp() })
                .padding(vertical = 12.dp, horizontal = 8.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(BarDark.copy(alpha = 0.9f))
                .border(1.dp, BorderGray, RoundedCornerShape(32.dp))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            order.forEachIndexed { index, id ->
                val platform = allPlatforms.first { it.id == id }
                ReorderableIcon(
                    platform = platform,
                    shaking = dragIdx >= 0 && dragIdx != index,
                    dragging = dragIdx == index,
                    animatedOffset = if (dragIdx == index) animOff else 0f,
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
            TransposeKey(
                vertical = true,
                currentHeightPx = effectiveHeight,
                minPx = minPx,
                maxPx = maxPx,
                onToggle = onToggleVertical,
                onHeightChange = onHeightChange
            )
        }
    }
}

@Composable
private fun TransposeKey(
    vertical: Boolean,
    currentHeightPx: Int,
    minPx: Int,
    maxPx: Int,
    onToggle: () -> Unit,
    onHeightChange: (Int) -> Unit
) {
    var dragActive by remember { mutableStateOf(false) }
    var startHeight by remember { mutableIntStateOf(currentHeightPx) }
    var acc by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(BorderGray)
            .clickable { onToggle() }
            .then(
                if (vertical) Modifier.pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            startHeight = currentHeightPx
                            acc = 0f
                            dragActive = true
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            acc += amount.y
                            val new = (startHeight + acc).roundToInt().coerceIn(minPx, maxPx)
                            onHeightChange(new)
                        },
                        onDragEnd = { dragActive = false },
                        onDragCancel = { dragActive = false }
                    )
                } else Modifier
            )
            .graphicsLayer {
                scaleX = if (dragActive) 1.18f else 1f
                scaleY = if (dragActive) 1.18f else 1f
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
    animatedOffset: Float,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    onLongPress: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val infinite = rememberInfiniteTransition(label = "shake")
    val shakeRot by infinite.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(80), RepeatMode.Reverse),
        label = "rot"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (dragging) 1.2f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    val iconAlpha by animateFloatAsState(
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
                translationX = if (vertical) 0f else animatedOffset
                translationY = if (vertical) animatedOffset else 0f
                rotationZ = if (shaking) shakeRot else 0f
                scaleX = iconScale
                scaleY = iconScale
                alpha = iconAlpha
                shadowElevation = if (dragging) 14f else 0f
            }
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(BorderGray)
                .border(1.5.dp, platform.ringColor.copy(alpha = if (dragging) 0.95f else 0.55f), CircleShape)
        ) {
            Image(
                painter = painterResource(platform.iconRes),
                contentDescription = platform.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(platform.label, fontSize = 10.sp, color = TextSecondary)
    }
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
                Text(item.code, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = CodeGreen,
                    lineHeight = 40.sp)
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
