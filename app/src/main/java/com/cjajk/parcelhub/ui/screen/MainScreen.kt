package com.cjajk.parcelhub.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Key
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
import androidx.compose.ui.graphics.ImageBitmap
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
import com.cjajk.parcelhub.reminder.ReminderScheduler
import com.cjajk.parcelhub.service.SmsReceiver
import com.cjajk.parcelhub.ui.viewmodel.MainViewModel
import com.cjajk.parcelhub.util.COM_LIST
import com.cjajk.parcelhub.util.DeepLinks
import com.cjajk.parcelhub.util.ExpressKeyStore
import com.cjajk.parcelhub.util.ExpressQuery
import com.cjajk.parcelhub.util.ExpressQueryException
import com.cjajk.parcelhub.util.ExpressResult
import com.cjajk.parcelhub.util.MascotIcons
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
val WarnOrange = Color(0xFFFF9F0A)

val TaobaoOrange = Color(0xFFFF6600)
val PddRed = Color(0xFFE02020)
val CainiaoBlue = Color(0xFF1677FF)
val SfGray = Color(0xFF8C8C8C)
val JdBrown = Color(0xFFB45309)

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
private const val SP_VERTICAL = "vertical_bar"

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val packages by vm.packages.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showPasteImport by remember { mutableStateOf(false) }
    var showKeyConfig by remember { mutableStateOf(false) }
    var showDailyReminder by remember { mutableStateOf(false) }
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

    val sp = remember { context.getSharedPreferences("parcel_hub", Context.MODE_PRIVATE) }
    var verticalBar by remember { mutableStateOf(sp.getBoolean(SP_VERTICAL, false)) }
    var sideOffsetPx by remember { mutableIntStateOf(sp.getInt(SP_SIDE_OFFSET, 0)) }

    val barAngle by animateFloatAsState(
        targetValue = if (verticalBar) 90f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "barAngle"
    )
    val angleSettled = barAngle <= 0.5f || barAngle >= 89.5f

    val bottomPad by animateDpAsState(
        targetValue = if (verticalBar) 0.dp else 84.dp,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "bottomPad"
    )
    val endPad by animateDpAsState(
        targetValue = if (verticalBar) 64.dp else 0.dp,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
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
                    IconButton(onClick = { showDailyReminder = true }) {
                        Icon(Icons.Default.Alarm, contentDescription = "每日提醒", tint = WarnOrange,
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
                    Tab(selected = tab == 2, onClick = { tab = 2 },
                        text = { Text("快捷取件", fontWeight = FontWeight.Medium, color = TextSecondary) })
                }

                when (tab) {
                    0, 1 -> {
                        val list = packages.filter { if (tab == 0) !it.pickedUp else it.pickedUp }
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (queryResult != null) {
                                item(key = "express_query") {
                                    ExpressQueryCard(
                                        result = queryResult!!,
                                        onDismiss = { queryResult = null },
                                        onSave = { customCode, customStation ->
                                            vm.addManual(customCode, customStation, queryResult!!.com)
                                            Toast.makeText(context, "已保存到列表", Toast.LENGTH_SHORT).show()
                                            queryResult = null
                                        }
                                    )
                                }
                            }
                            if (list.isEmpty() && queryResult == null) {
                                item { EmptyHint(tab) }
                            }
                            items(list, key = { it.id }) { item ->
                                PackageCard(
                                    item = item,
                                    onToggle = { vm.togglePickedUp(item) },
                                    onDelete = { vm.delete(item) },
                                    onSetRemind = { ms -> vm.setRemindAt(item, ms) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                    2 -> QuickAccessPage()
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
                GlassBottomBar(onToggleVertical = {
                    verticalBar = true
                    sp.edit().putBoolean(SP_VERTICAL, true).apply()
                })
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
                    onToggleVertical = {
                        verticalBar = false
                        sp.edit().putBoolean(SP_VERTICAL, false).apply()
                    },
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
            onConfigKey = { showKeyConfig = true },
            onQuery = { nu, com, phone ->
                showSearch = false
                queryLoading = true
                queryResult = null
                scope.launch {
                    try {
                        val customer = ExpressKeyStore.getCustomer(context)
                        val key = ExpressKeyStore.getKey(context)
                        val result = ExpressQuery.query(nu, com, phone.ifBlank { null }, customer, key)
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

    if (showKeyConfig) {
        KeyConfigDialog(onDismiss = { showKeyConfig = false })
    }

    if (showDailyReminder) {
        DailyReminderDialog(onDismiss = { showDailyReminder = false })
    }
}

@Composable
private fun QuickAccessPage() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("身份码", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("打开各平台取件身份码，扫码即可取件", fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(12.dp))

        QuickEntryCard("菜鸟驿站取件", "优先打开身份码，失败时进入菜鸟 App", MascotIcons.cainiao) {
            DeepLinks.openAlipayCainiao(context)
        }
        Spacer(Modifier.height(10.dp))
        QuickEntryCard("淘宝取件", "打开淘宝末端取件身份码", MascotIcons.taobao) {
            DeepLinks.openTaobaoIdentity(context)
        }
        Spacer(Modifier.height(10.dp))
        QuickEntryCard("拼多多取件", "打开多多买菜取件身份码", MascotIcons.pdd) {
            DeepLinks.openPddPickup(context)
        }
        Spacer(Modifier.height(10.dp))
        QuickEntryCard("顺丰取件", "打开顺丰 App，未安装时跳转微信", MascotIcons.sf) {
            DeepLinks.openSf(context)
        }

        Spacer(Modifier.height(24.dp))
        Text("查看待取", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("先确认有没有包裹，再前往驿站取件", fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(12.dp))

        QuickEntryCard("淘宝待取快递", "打开淘宝末端驿站待取列表", MascotIcons.taobao) {
            DeepLinks.openTaobaoWaiting(context)
        }
        Spacer(Modifier.height(10.dp))
        QuickEntryCard("拼多多待取快递", "打开拼多多包裹待取列表", MascotIcons.pdd) {
            DeepLinks.openPddWaiting(context)
        }
        Spacer(Modifier.height(10.dp))
        QuickEntryCard("京东待取快递", "打开京东订单列表", MascotIcons.jd) {
            DeepLinks.openJd(context)
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun QuickEntryCard(
    title: String,
    subtitle: String,
    mascot: ImageBitmap,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = mascot,
                contentDescription = title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
            Icon(
                Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun DailyReminderDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val current = remember { ReminderScheduler.getDailyTime(context) }
    var hour by remember { mutableIntStateOf(current?.first ?: 18) }
    var minute by remember { mutableIntStateOf(current?.second ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("每日统一提醒", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("每天到点提醒所有待取快递", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("每天", color = TextPrimary, fontSize = 16.sp)
                    Spacer(Modifier.width(12.dp))
                    TimePickerField(hour, minute) { h, m -> hour = h; minute = m }
                }
                if (current != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("当前已设置：每天 ${String.format("%02d:%02d", current.first, current.second)}",
                        fontSize = 12.sp, color = CodeGreen)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                ReminderScheduler.setDailyTime(context, hour, minute)
                Toast.makeText(context, "已设置每日 ${String.format("%02d:%02d", hour, minute)} 提醒", Toast.LENGTH_SHORT).show()
                onDismiss()
            }) {
                Text("保存", color = AccentBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = {
                        ReminderScheduler.clearDailyTime(context)
                        Toast.makeText(context, "已关闭每日提醒", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }) { Text("关闭", color = Color(0xFFFF453A)) }
                }
                TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
            }
        }
    )
}

@Composable
private fun TimePickerField(hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    val context = LocalContext.current
    TextButton(
        onClick = {
            TimePickerDialog(
                context,
                { _, h, m -> onChange(h, m) },
                hour, minute, true
            ).show()
        }
    ) {
        Text(String.format("%02d:%02d", hour, minute), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchDialog(
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfigKey: () -> Unit,
    onQuery: (String, String?, String) -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedCom by remember { mutableStateOf<Pair<String, String>?>(null) }
    var phone by remember { mutableStateOf("") }
    val configured = remember { ExpressKeyStore.isConfigured(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("查询快递", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (configured) CodeGreen.copy(alpha = 0.1f) else WarnOrange.copy(alpha = 0.1f))
                        .clickable { onConfigKey() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = if (configured) CodeGreen else WarnOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (configured) "快递100密钥已配置 ✓" else "未配置快递100密钥，点此配置",
                        fontSize = 12.sp,
                        color = if (configured) CodeGreen else WarnOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
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
            TextButton(
                onClick = { if (text.isNotBlank()) onQuery(text.trim(), selectedCom?.first, phone) },
                enabled = text.isNotBlank() && !loading && configured
            ) {
                Text("查询", color = AccentBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyConfigDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var customer by remember { mutableStateOf(ExpressKeyStore.getCustomer(context)) }
    var key by remember { mutableStateOf(ExpressKeyStore.getKey(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = { Text("配置快递100密钥", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = customer, onValueChange = { customer = it },
                    label = { Text("customer（授权码）") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = darkFieldColors()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = key, onValueChange = { key = it },
                    label = { Text("key（密钥）") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = darkFieldColors()
                )
                Spacer(Modifier.height(16.dp))
                Text("获取流程", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                val steps = listOf(
                    "1. 打开快递100开放平台：https://api.kuaidi100.com/",
                    "2. 注册账号并登录（个人即可，免费额度每天 100 单足够用）",
                    "3. 进入「管理中心」→「产品管理」→ 找到「实时查询」（企业版），点「申请开通」",
                    "4. 开通后进入「我的账号」页面，找到「customer」和「key」两串字符",
                    "5. 复制粘贴到上面两个输入框，点保存"
                )
                steps.forEach { step ->
                    Text(step, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    ExpressKeyStore.save(context, customer, key)
                    Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                enabled = customer.isNotBlank() && key.isNotBlank()
            ) {
                Text("保存", color = AccentBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) } }
    )
}

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

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedBorderColor = AccentBlue, unfocusedBorderColor = BorderGray,
    focusedLabelColor = AccentBlue, unfocusedLabelColor = TextSecondary,
    cursorColor = AccentBlue
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressQueryCard(
    result: ExpressResult,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val latestTrace = result.traces.firstOrNull()?.context.orEmpty()
    val parsed = remember(result.nu, latestTrace) { SmsParser.parse(null, latestTrace) }
    var code by remember(result.nu) { mutableStateOf(parsed?.code ?: result.nu) }
    var station by remember(result.nu) { mutableStateOf(parsed?.station ?: extractStationFromTrace(latestTrace, result.com)) }

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
                        5 -> WarnOrange.copy(alpha = 0.15f)
                        else -> AccentBlue.copy(alpha = 0.15f)
                    }).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(result.stateText, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = when (result.state) { 3 -> CodeGreen; 5 -> WarnOrange; else -> AccentBlue })
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
            Spacer(Modifier.height(16.dp))
            Text("保存为：", fontSize = 12.sp, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = code, onValueChange = { code = it },
                label = { Text("取件码") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), colors = darkFieldColors()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = station, onValueChange = { station = it },
                label = { Text("驿站/取件地址") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), colors = darkFieldColors()
            )
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = {
                    if (code.isNotBlank()) onSave(code.trim(), station.trim())
                }) { Text("保存到列表", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("关闭", color = TextSecondary, fontSize = 13.sp) }
            }
        }
    }
}

private fun extractStationFromTrace(trace: String, fallback: String): String {
    val bracketRegex = Regex("【([^】]+)】")
    val matches = bracketRegex.findAll(trace).map { it.groupValues[1] }.toList()
    val keywords = listOf("驿站", "代收", "服务点", "快递柜", "学院", "食堂", "北侧", "南侧", "校区", "邮政")
    for (m in matches) {
        if (keywords.any { m.contains(it) }) return m
    }
    return matches.lastOrNull() ?: fallback
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
    val sp = remember { context.getSharedPreferences("parcel_hub", Context.MODEL_PRIVATE) }
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
private fun PackageCard(
    item: PackageRecord,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onSetRemind: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
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
                if (item.remindAt != null && !item.pickedUp) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, contentDescription = null,
                            tint = WarnOrange, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("提醒 ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(item.remindAt))}",
                            fontSize = 11.sp, color = WarnOrange)
                    }
                }
            }
            IconButton(onClick = {
                if (item.remindAt != null) {
                    onSetRemind(null)
                    Toast.makeText(context, "已取消提醒", Toast.LENGTH_SHORT).show()
                } else {
                    val now = Calendar.getInstance()
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = now.timeInMillis
                        add(Calendar.HOUR_OF_DAY, 1)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            cal.set(Calendar.YEAR, y)
                            cal.set(Calendar.MONTH, m)
                            cal.set(Calendar.DAY_OF_MONTH, d)
                            TimePickerDialog(
                                context,
                                { _, h, minute ->
                                    cal.set(Calendar.HOUR_OF_DAY, h)
                                    cal.set(Calendar.MINUTE, minute)
                                    onSetRemind(cal.timeInMillis)
                                    val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                                    Toast.makeText(context, "已设提醒 ${fmt.format(cal.time)}", Toast.LENGTH_SHORT).show()
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).apply {
                        datePicker.minDate = now.timeInMillis
                    }.show()
                }
            }) {
                Icon(
                    Icons.Default.Alarm,
                    contentDescription = "设置提醒",
                    tint = if (item.remindAt != null) WarnOrange else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
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
