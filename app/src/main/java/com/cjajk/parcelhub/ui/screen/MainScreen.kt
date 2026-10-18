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
    Platform("cainiao", listOf("com.cainiao.wireless"), "菜鸟", Icons.Default.LocalShipping) { DeepLinks.openAlipayCainiao(it) },
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

        QuickEntryCard("菜鸟驿站取件", "优先打开身份码，失败时进入菜鸟 App", "菜", CainiaoBlue) {
            DeepLinks.openAlipayCainiao(context)
        }
        Spacer(Modifier.height(10.dp))
        QuickEntryCard("淘宝取件", "打开淘宝末端取件身份码", "淘", TaobaoOrange) {
            DeepLinks.openTaobaoIdentity(context)
        }
        Spacer(Modifier.height(10.dp))
        QuickEntryCard("拼多多取件", "打开多多买菜取件身份码", "拼", PddRed) {
            DeepLinks.openPddPickup(context)
        }
        Spacer(Modifier.height(10.dp))
        QuickEntryCard("顺丰取件", "打开顺丰 App，未安装时跳转微信", "顺", SfGray) {
            DeepLinks.openSf(context)
        }

        Spacer(Modifier.height(24.dp))
        Text("查看待取", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("先确认有没有包裹，再前往驿站取件", fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(12.dp))

        QuickEntryCard("淘宝待取快递", "打开淘宝末端驿站待取列表", "淘", TaobaoOrange) {
            DeepLinks.openTaobaoWaiting(context)
        }
        Spacer(Modifier.height(10.dp))
        QuickEntryCard("拼多多待取快递", "打开拼多多包裹待取列表", "拼", PddRed) {
            DeepLinks.openPddWaiting(context)
        }
        Spacer(Modifier.height(10.dp))
        QuickEntryCard("京东待取快递", "打开京东订单列表", "京", JdBrown) {
            DeepLinks.openJd(context)
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun QuickEntryCard(
    title: String,
    subtitle: String,
    mark: String,
    markColor: Color,
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(markColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = markColor
                )
            }
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