package kr.slot.hyena

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
                    Triple(3, "관리", Icons.Default.Settings)
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
                3 -> ManageScreen(ms = ms) { updated -> ms = updated; store.save(updated) }
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
    var idx by remember { mutableIntStateOf(0) }
    var g by remember { mutableStateOf("") }
    val m = ms.getOrNull(idx)
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
                Text(m?.name ?: "기종 선택", style = MaterialTheme.typography.bodyLarge)
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
        Hall("ワンダーランド南ヶ丘店", "약 3.8km", "福岡県大野城市紫台19-10", "https://42071.p-world.jp", 200, 3.8),
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
fun ManageScreen(ms: List<Machine>, @Suppress("UNUSED_PARAMETER") onSave: (List<Machine>) -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Construction, null, Modifier.size(80.dp), tint = Color.Gray)
            Spacer(Modifier.height(16.dp))
            Text("기종 관리 기능 준비 중", style = MaterialTheme.typography.headlineSmall, color = Color.Gray)
            Text("현재 ${ms.size}개 기종의 기본 데이터가 보호되고 있습니다.", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
        }
    }
}
