package kr.slot.hyena

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.edit
import androidx.core.net.toUri
import org.json.JSONArray
import org.json.JSONObject

// --- Data Models ---
data class Machine(
    val name: String,
    val ceiling: Int,
    val unit: String = "G",
    val reward: String = "미검증",
    val classType: String = "미검증",
    val highThreshold: Int,
    val midThreshold: Int,
    val note: String = "",
)

data class FinanceRecord(
    val id: Long = System.currentTimeMillis(),
    val date: String,
    val hallName: String,
    val machineName: String,
    val startMoney: Int,
    val endMoney: Int,
) {
    val profit: Int get() = endMoney - startMoney
}

data class Hall(
    val name: String,
    val dist: String,
    val address: String,
    val url: String,
    val slots: Int,
    val distVal: Double,
)

// --- Theme ---
private val CasinoDarkColors = darkColorScheme(
    primary = Color(0xFFFFD700), // Gold
    onPrimary = Color.Black,
    secondary = Color(0xFF00FF41), // Matrix Green
    onSecondary = Color.Black,
    surface = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
    tertiary = Color(0xFF2196F3) // Sky Blue
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HyenaTheme {
                App(MachineStore(this))
            }
        }
    }
}

@Composable
fun HyenaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CasinoDarkColors,
        typography = Typography(),
        content = content
    )
}

// --- Store ---
class MachineStore(val context: Context) {
    private val p = context.getSharedPreferences("machines_v2", 0)
    private val defaults = listOf(
        Machine("スーパーリオエース2", 750, "G", "BIG", "보너스 유지", 550, 400, "750G 계열"),
        Machine("スマスロ モンキーターンV", 795, "G", "AT", "직접 AT", 550, 400, "400G 이상부터 효율 양호"),
        Machine("Lパチスロ 炎炎ノ消防隊2", 850, "G", "보너스", "보너스 유지", 550, 420, "보너스간 게임수 기준"),
        Machine("Lリコリス・リコイル", 850, "G", "RUSH", "직접 AT", 600, 450, "천장 850G"),
        Machine("スロット ワールドダイスター", 899, "G", "보너스", "보너스 유지", 700, 500, "보너스간 천장 기준"),
        Machine("鉄拳6", 747, "pt", "보너스", "보너스 유지", 650, 450, "포인트(pt) 기준"),
        Machine("スマスロ 甲鉄城のカバネリ 海門決戦", 999, "G", "EP보너스→ST", "직접 AT", 650, 500, "250/450G 존 스루 후 500G부터"),
        Machine("スマスロ モンスターハンターライズ", 999, "G", "보너스", "보너스 유지", 750, 580, "600G 부근부터 기댓값 상승"),
        Machine("スマスロ 攻殻機動隊 SAC", 999, "G", "AT", "직접 AT", 700, 550, "AT간 순수 게임수 기준"),
        Machine("L戦国乙女5 業火を穿つ宿焔の双刃", 999, "G", "AT", "직접 AT", 700, 530, "600G 존 노림수 연계"),
        Machine("スーパーブラックジャック", 999, "G", "BIG→ST", "보너스 유지", 700, 550, "BIG 보너스 확정"),
        Machine("ToLOVEる TRANCE", 999, "G", "AT", "직접 AT", 750, 550, "ST간 게임수 기준"),
        Machine("タクトオーパス", 999, "G", "AT", "직접 AT", 750, 550, "AT 확정 천장"),
        Machine("L邪神ちゃんドロップキック", 999, "G", "AT", "직접 AT", 750, 550, "AT 확정 천장"),
        Machine("スマスロ ストリートファイター6", 999, "G", "AT", "직접 AT", 750, 580, "보너스/AT 확정"),
        Machine("スマスロ とんでもスキルで異世界放浪メシ", 999, "G", "AT", "직접 AT", 750, 580, "AT 확정 천장"),
        Machine("ゴッドイーター リザレクション", 1000, "G", "AT", "직접 AT", 750, 580, "600G 존 연계"),
        Machine("デビルメイクライ5", 1000, "G", "AT", "직접 AT", 750, 580, "표준 1000G 천장"),
        Machine("バイオハザード RE:3", 1000, "G", "AT", "직접 AT", 750, 580, "표준 1000G 천장"),
        Machine("真・一騎当千", 1000, "G", "AT", "직접 AT", 750, 580, "표준 1000G 천장"),
        Machine("スマスロ やじきた道中記参る!", 1000, "G", "보너스", "보너스 유지", 700, 500, "주기 카운트 기준"),
        Machine("かぐや様は告らせたい", 1100, "G", "보너스", "보너스 유지", 800, 620, "BIG 후 1100G 천장 기준"),
        Machine("東京リベンジャーズ", 1190, "G", "AT", "직접 AT", 800, 620, "1190G 천장 기준"),
        Machine("L 東京喰種", 1200, "G", "AT", "직접 AT", 800, 650, "AT간 게임수 기준"),
        Machine("スロット ソードアート・オンラインII", 1200, "G", "AT", "직접 AT", 850, 680, "AT간 1200G 컷라인 충족"),
        Machine("スマスロ マギアレコード", 999, "G", "AT", "직접 AT", 750, 580, "보너스/AT 확정"),
        Machine("Lパチスロ 喰霊-零-Re", 999, "G", "AT", "직접 AT", 750, 580, "AT 확정 천장"),
        Machine("戦国コレクション6", 1200, "G", "AT", "직접 AT", 800, 650, "AT간 1200G 컷라인 충족"),
    )

    fun load(): List<Machine> {
        val s = p.getString("data", null) ?: return defaults
        return try {
            val a = JSONArray(s)
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                Machine(
                    o.getString("name"), o.getInt("ceiling"), o.optString("unit"),
                    o.optString("reward"), o.optString("classType"),
                    o.optInt("highThreshold"), o.optInt("midThreshold"), o.optString("note"),
                )
            }
        } catch (_: Exception) {
            defaults
        }
    }

    fun save(ms: List<Machine>) {
        val a = JSONArray()
        ms.forEach { m ->
            a.put(
                JSONObject().apply {
                    put("name", m.name)
                    put("ceiling", m.ceiling)
                    put("unit", m.unit)
                    put("reward", m.reward)
                    put("classType", m.classType)
                    put("highThreshold", m.highThreshold)
                    put("midThreshold", m.midThreshold)
                    put("note", m.note)
                },
            )
        }
        p.edit { putString("data", a.toString()) }
    }

    fun saveFinance(total: Int, invested: Int, revenue: Int) {
        p.edit {
            putInt("total", total)
            putInt("invested", invested)
            putInt("revenue", revenue)
        }
    }

    fun loadFinance(): Triple<Int, Int, Int> {
        return Triple(p.getInt("total", 0), p.getInt("invested", 0), p.getInt("revenue", 0))
    }

    fun saveRecords(records: List<FinanceRecord>) {
        val a = JSONArray()
        records.forEach { r ->
            a.put(JSONObject().apply {
                put("id", r.id)
                put("date", r.date)
                put("hallName", r.hallName)
                put("machineName", r.machineName)
                put("startMoney", r.startMoney)
                put("endMoney", r.endMoney)
            })
        }
        p.edit { putString("records", a.toString()) }
    }

    fun loadRecords(): List<FinanceRecord> {
        val s = p.getString("records", null) ?: return emptyList()
        return try {
            val a = JSONArray(s)
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                FinanceRecord(
                    o.getLong("id"), o.getString("date"), o.getString("hallName"),
                    o.getString("machineName"), o.getInt("startMoney"), o.getInt("endMoney")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

// --- Main App Shell ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(store: MachineStore) {
    var ms by remember { mutableStateOf(store.load()) }
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("SLOT HYENA", fontWeight = FontWeight.ExtraBold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    Triple(0, "업장", Icons.Default.Place),
                    Triple(1, "계산", Icons.Default.Calculate),
                    Triple(2, "기종", Icons.AutoMirrored.Filled.List),
                    Triple(3, "자금", Icons.Default.AccountBalanceWallet)
                )
                items.forEach { (index, label, icon) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        label = { Text(label) },
                        icon = { Icon(icon, null) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (tab) {
                0 -> StoreScreen(store.context)
                1 -> CalcScreen(ms)
                2 -> ListScreen(ms)
                3 -> FinanceScreen(ms, store)
            }
        }
    }
}

// --- Screens ---

@Composable
fun ListScreen(ms: List<Machine>) {
    var filter by remember { mutableStateOf("전체") }
    val filtered = when (filter) {
        "AT 유지" -> ms.filter { it.classType == "직접 AT" }
        "보너스 유지" -> ms.filter { it.classType == "보너스 유지" }
        "PASS" -> ms.filter { it.classType == "PASS" }
        else -> ms
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column {
                Text(
                    text = "Machine Database",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "총 ${ms.size}개 기종 정보 탑재",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("전체", "AT 유지", "보너스 유지", "PASS").forEach { x ->
                    FilterChip(
                        selected = filter == x,
                        onClick = { filter = x },
                        label = { Text(x) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }
        items(filtered) { m ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(m.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${m.ceiling}${m.unit}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(onClick = {}, label = { Text(m.reward) }, icon = { Icon(Icons.Default.Stars, null, Modifier.size(16.dp)) })
                        SuggestionChip(onClick = {}, label = { Text(m.classType) })
                    }
                    if (m.note.isNotBlank()) {
                        Text(
                            text = m.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalcScreen(ms: List<Machine>) {
    var idx by remember { mutableStateOf<Int?>(null) }
    var g by remember { mutableStateOf("") }
    val m = idx?.let { ms.getOrNull(it) }
    val cur = g.toIntOrNull()

    val (verdict, icon, color) = when {
        (m == null || cur == null) -> Triple("데이터를 입력하세요", Icons.Default.Info, Color.Gray)
        cur == m.ceiling -> Triple("천장 도달 (당첨 확정)", Icons.Default.Stars, MaterialTheme.colorScheme.tertiary)
        cur > m.ceiling -> Triple("천장 초과 NG (${cur - m.ceiling}${m.unit})", Icons.Default.Report, Color.Red)
        (m.classType == "보너스 유지" || m.classType == "직접 AT") -> {
            if (cur >= m.highThreshold) Triple("HIGH (강력 추천)", Icons.Default.ThumbUp, MaterialTheme.colorScheme.secondary)
            else if (cur >= m.midThreshold) Triple("MID (진입 가능)", Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary)
            else Triple("LOW (진입 불가)", Icons.Default.Warning, Color(0xFFFF5252))
        }
        else -> Triple("PASS (효율 낮음)", Icons.Default.Block, Color.Gray)
    }

    Column(
        Modifier
            .padding(20.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()), 
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Hyena Calculator", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        var open by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                onClick = { open = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(m?.name ?: "기종을 선택하세요", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                ms.forEachIndexed { i, x ->
                    DropdownMenuItem(text = { Text(x.name) }, onClick = { idx = i; g = ""; open = false })
                }
            }
        }

        OutlinedTextField(
            value = g,
            onValueChange = { g = it.filter(Char::isDigit) },
            label = { Text("현재 게임수 (${m?.unit ?: "G"})") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.Speed, null) }
        )

        if (m != null && cur != null) {
            val remainText = when {
                cur < m.ceiling -> "천장까지 ${m.ceiling - cur}${m.unit}"
                cur == m.ceiling -> "당첨 확정!"
                else -> "천장 ${cur - m.ceiling}${m.unit} 초과"
            }
            
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = color.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(64.dp))
                    Text(verdict, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Text(remainText, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    HorizontalDivider(Modifier.padding(vertical = 16.dp), color = color.copy(alpha = 0.3f))
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        InfoColumn("천장", "${m.ceiling}${m.unit}")
                        InfoColumn("HIGH", "${m.highThreshold}${m.unit}")
                        InfoColumn("보상", m.reward)
                    }
                }
            }
            Spacer(Modifier.height(32.dp)) // 스크롤 여유 공간 확보
        }
    }
}

@Composable
fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StoreScreen(ctx: Context) {
    val halls = listOf(
        Hall("HINODE 大野城店", "약 2.0km", "福岡県大野城市瓦田4-12-5", "https://66100.p-world.jp", 360, 2.0),
        Hall("玉屋409雑餉隈", "약 2.2km", "福岡県福岡市博多区南本町2-1-1", "https://92039.p-world.jp", 336, 2.2),
        Hall("BEAM by HIKARI", "약 2.5km", "福岡県大野城市御笠川1-13-3", "https://80684.p-world.jp", 378, 2.5),
        Hall("ワンダーランド南ヶ丘店", "약 3.8km", "福岡県大野城市紫台19-10", "https://42071.p-world.jp", 256, 3.8),
        Hall("MJアリーナ井尻店", "약 4.0km", "福岡県春日市桜ヶ丘4-14", "https://mjijiri.p-world.jp", 273, 4.0),
        Hall("Aパーク春日店", "약 4.1km", "福岡県春日市日の出町5-24", "https://www.p-world.co.jp/fukuoka/a-parkkasuga.htm", 324, 4.1),
        Hall("つかさ月隈店", "약 4.5km", "福岡県福岡市博多区西月隈1-1-43", "https://20814.p-world.jp", 534, 4.5),
        Hall("プラザ本店II", "약 4.8km", "福岡県福岡市博多区西月隈3-5-32", "https://43342.p-world.jp", 488, 4.8)
    ).sortedBy { it.distVal }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Visit Candidates", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        items(halls) { h ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(h.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(" 숙소에서 ${h.dist}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(16.dp))
                        Icon(Icons.Default.ConfirmationNumber, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Text(" 슬롯 ${h.slots}대", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("🏠 ${h.address}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    
                    Row(
                        modifier = Modifier.padding(top = 16.dp), 
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, h.url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { 
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("P-WORLD") 
                        }
                        OutlinedButton(
                            onClick = {
                                val uri = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(h.name + " " + h.address)}".toUri()
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Map, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("지도 열기")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinanceScreen(ms: List<Machine>, store: MachineStore) {
    val (savedTotal, _, _) = store.loadFinance()
    var totalFunds by remember { mutableIntStateOf(savedTotal) }
    var records by remember { mutableStateOf(store.loadRecords()) }

    val currentDate = SimpleDateFormat("M/d", Locale.getDefault()).format(Date())
    val todayRecords = records.filter { it.date == currentDate }
    val todayInvested = todayRecords.sumOf { it.startMoney }
    val todayRevenue = todayRecords.sumOf { it.endMoney }

    fun updateFinance(total: Int, updatedRecords: List<FinanceRecord>) {
        totalFunds = total
        records = updatedRecords
        store.saveFinance(total, 0, 0)
        store.saveRecords(updatedRecords)
    }
    var showDialog by remember { mutableStateOf(false) }
    var showEditFunds by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<FinanceRecord?>(null) }
    var deletingRecord by remember { mutableStateOf<FinanceRecord?>(null) }

    val todayProfit = (todayRevenue - todayInvested).coerceAtLeast(0)
    val todayLoss = (todayInvested - todayRevenue).coerceAtLeast(0)
    val totalProfit = records.sumOf { it.profit }
    val currentBalance = totalFunds + totalProfit
    val yield = if (todayInvested > 0) ((todayRevenue - todayInvested).toDouble() / todayInvested * 100) else 0.0



    if (showDialog || editingRecord != null) {
        var hall by remember(editingRecord) { mutableStateOf(editingRecord?.hallName ?: "업장 선택") }
        var machine by remember(editingRecord) { mutableStateOf(editingRecord?.machineName ?: "기종 선택") }
        var start by remember(editingRecord) { mutableStateOf(editingRecord?.startMoney?.toString() ?: "") }
        var end by remember(editingRecord) { mutableStateOf(editingRecord?.endMoney?.toString() ?: "") }
        var hallExpanded by remember { mutableStateOf(false) }
        var machExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { 
                showDialog = false 
                editingRecord = null 
            },
            title = { Text(if (editingRecord != null) "기록 수정" else "기록 추가") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 업장 선택
                    Box {
                        OutlinedButton(onClick = { hallExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("업장: $hall") }
                        DropdownMenu(expanded = hallExpanded, onDismissRequest = { hallExpanded = false }) {
                            listOf(
                                "HINODE 大野城店", "玉屋409雑餉隈", "BEAM by HIKARI",
                                "ワンダーランド南ヶ丘店", "MJアリーナ井尻店", "Aパーク春日店",
                                "つかさ月隈店", "プラザ本店II"
                            ).forEach { name ->
                                DropdownMenuItem(text = { Text(name) }, onClick = { hall = name; hallExpanded = false })
                            }
                        }
                    }
                    // 기종 선택
                    Box {
                        OutlinedButton(onClick = { machExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("기종: $machine") }
                        DropdownMenu(expanded = machExpanded, onDismissRequest = { machExpanded = false }) {
                            ms.forEach { m ->
                                DropdownMenuItem(text = { Text(m.name) }, onClick = { machine = m.name; machExpanded = false })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = start, 
                        onValueChange = { if (it.all(Char::isDigit)) start = it }, 
                        label = { Text("투자 금액") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = end, 
                        onValueChange = { if (it.all(Char::isDigit)) end = it }, 
                        label = { Text("회수 금액") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val s = start.toIntOrNull()
                    val e = end.toIntOrNull()
                    if (hall == "업장 선택" || machine == "기종 선택" || s == null || e == null) {
                        return@TextButton
                    }
                    if (editingRecord != null) {
                        val r = editingRecord!!
                        val newRecords = records.map { if (it.id == r.id) it.copy(hallName = hall, machineName = machine, startMoney = s, endMoney = e) else it }
                        updateFinance(totalFunds, newRecords)
                    } else {
                        val currentDate = SimpleDateFormat("M/d", Locale.getDefault()).format(Date())
                        val newRecords = records + FinanceRecord(date = currentDate, hallName = hall, machineName = machine, startMoney = s, endMoney = e)
                        updateFinance(totalFunds, newRecords)
                    }
                    editingRecord = null
                    showDialog = false
                }) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { editingRecord = null; showDialog = false }) { Text("취소") } }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { pad ->
        if (showEditFunds) {
        var input by remember { mutableStateOf("$totalFunds") }
        AlertDialog(
            onDismissRequest = { showEditFunds = false },
            title = { Text("총 자금 수정") },
            text = {
                OutlinedTextField(
                    value = input, 
                    onValueChange = { if (it.all(Char::isDigit)) input = it }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    totalFunds = input.toIntOrNull() ?: totalFunds
                    updateFinance(totalFunds, records)
                    showEditFunds = false 
                }) { Text("저장") }
            }
        )
    }

    LazyColumn(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("자금 관리", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("💰 총 자금", "$totalFunds", Modifier.weight(1f), onClick = { showEditFunds = true })
                    StatCard("🎰 오늘 투입", "$todayInvested", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("💵 현재 잔액", "$currentBalance", Modifier.weight(1f))
                    StatCard("📊 수익률", "${String.format("%.1f", yield)}%", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("🎯 오늘 결과", "${todayRevenue - todayInvested}", Modifier.weight(1f), if(todayRevenue >= todayInvested) Color(0xFF00FF41) else Color(0xFFFF5252))
                    StatCard("📈 누적 결과", "${records.sumOf { it.profit }}", Modifier.weight(1f))
                }
                }
            }
            item { Text("최근 기록", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp)) }
        items(records.sortedByDescending { it.id }) { r ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth().padding(end = 56.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { 
                        Text("${r.date} | ${r.hallName} | ${r.machineName}", style = MaterialTheme.typography.bodyMedium)
                        Text("${if (r.profit >= 0) "+" else ""}${r.profit}엔", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) 
                    }
                    Row {
                        IconButton(onClick = {
                            editingRecord = r
                            showDialog = true
                        }) {
                            Icon(Icons.Default.Edit, null, tint = Color.Gray)
                        }
                        IconButton(onClick = {
                            val newRecords = records.filter { it.id != r.id }
                            updateFinance(totalFunds, newRecords)
                        }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Gray)
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}
