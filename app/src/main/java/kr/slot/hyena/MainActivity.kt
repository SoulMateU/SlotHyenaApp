package kr.slot.hyena

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
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
import kotlin.math.roundToInt

// --- Data Models ---
data class Machine(
    val name: String,
    val ceiling: Int,
    val unit: String = "G",
    val highThreshold: Int,
    val midThreshold: Int,
    val quitRule: String = "",
    val exceptionRule: String = "",
) {
    val note: String
        get() = if (exceptionRule.isNotBlank()) "$quitRule | $exceptionRule" else quitRule
}

data class FinanceRecord(
    val id: Long = System.currentTimeMillis(),
    val date: String,
    val hallName: String,
    val machineName: String,
    val startMoney: Int,
    val endMedals: Int,
    val exchangeMedalsPer1000: Int = 0,
) {
    val effectiveExchangeMedalsPer1000: Int
        get() = exchangeMedalsPer1000.takeIf { it > 0 } ?: exchangeMedalsPer1000(hallName)

    val revenue: Int
        get() = if (effectiveExchangeMedalsPer1000 > 0) {
            (endMedals * 1000.0 / effectiveExchangeMedalsPer1000).roundToInt()
        } else 0

    val profit: Int get() = revenue - startMoney
}

fun exchangeMedalsPer1000(hallName: String): Int =
    hallDefinitions.find { it.name == hallName }?.exchangeMedalsPer1000 ?: 0

data class Hall(
    val name: String,
    val dist: String,
    val address: String,
    val url: String,
    val slots: Int,
    val distVal: Double,
    val info: String = "",
    val rentalMedalsPer1000: Int,
    val exchangeMedalsPer1000: Int,
)

// 업장 정보 화면과 자금 기록 화면이 함께 사용하는 업장 목록
private val hallDefinitions = listOf(
    Hall("HINODE 大野城店", "약 2.0km", "福岡県大野城市瓦田4-12-5", "https://66100.p-world.jp", 360, 2.0, "슬롯 재플레이 ○ | 무료 / 한도 미확인", 50, 55),
    Hall("玉屋409雑餉隈", "약 2.2km", "福岡県福岡市博多区南本町2-1-1", "https://92039.p-world.jp", 336, 2.2, "슬롯 재플레이 ○ | 한도 약 460枚", 50, 51),
    Hall("BEAM by HIKARI", "약 2.5km", "福岡県大野城市御笠川1-13-3", "https://80684.p-world.jp", 378, 2.5, "슬롯 재플레이 ○ | 수수료·한도 미확인", 50, 55),
    Hall("ワンダーランド南ヶ丘店", "약 3.8km", "福岡県大野城市紫台19-10", "https://42071.p-world.jp", 256, 3.8, "슬롯 재플레이 ○ | 수수료·한도 미확인", 50, 51),
    Hall("MJアリーナ井尻店", "약 4.0km", "福岡県春日市桜ヶ丘4-14", "https://mjijiri.p-world.jp", 273, 4.0, "슬롯 재플레이 ○ | 무료 / 무제한", 50, 50),
    Hall("Aパーク春日店", "약 4.1km", "福岡県春日市日の出町5-24", "https://www.p-world.co.jp/fukuoka/a-parkkasuga.htm", 324, 4.1, "슬롯 재플레이 ○ | 6% 수수료 / 한도 미확인", 50, 51),
    Hall("つかさ月隈店", "약 4.5km", "福岡県福岡市博多区西月隈1-1-43", "https://20814.p-world.jp", 534, 4.5, "슬롯 재플레이 ○ | 무제한", 50, 55),
    Hall("プラザ本店II", "약 4.8km", "福岡県福岡市博多区西月隈3-5-32", "https://43342.p-world.jp", 488, 4.8, "슬롯 재플레이 ○ | 무료 / 무제한", 50, 51),
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
        Machine("スーパーリオエース2 (슈퍼 리오 에이스 2)", 750, "G", 550, 400, "50G+α", "에이스모드·보너스 스루 등 강한 상태면 계속"),
        Machine("スマスロ モンキーターンV (스마슬로 몽키턴 V)", 795, "G", 550, 400, "1G + 전조 확인", "헬멧/로고 등 천국·우대 시 1~2주기"),
        Machine("Lパチスロ 炎炎ノ消防隊2 (L 염염의 소방대 2)", 850, "G", 550, 420, "①보너스 후 88G+α ②염염격투 후 최대28G", "염염루프·스루 狀況 지속"),
        Machine("Lリコリス・リコイル (L 리코리스 리코일)", 850, "G", 600, 450, "최소 100G", "상위AT 후 100G+α, AT 완주 후 단축천장 확인"),
        Machine("スロット ワールドダイスター (슬롯 월드 다이스타)", 899, "G", 700, 500, "일반ST 즉시", "상위ST 100G+CZ | 종료 정보 확인"),
        Machine("鉄拳6 (철권 6)", 747, "pt", 650, 450, "AT후 99pt+α / 보너스후 철권ZONE", "고CZ·고포인트·강한 종료화면"),
        Machine("スマスロ 甲鉄城のカバネリ 海門決戦 (갑철성의 카바네리 해문결전)", 999, "G", 650, 500, "ST종료 후 1G", "찬스目 카운터 발광 → CZ까지"),
        Machine("スマスロ モンスターハンターライズ (스마슬로 몬스터 헌터 라이즈)", 999, "G", 750, 580, "1G", "1G째 레어역·특수상태 등"),
        Machine("スマスロ 攻殻機動隊 SAC (스마슬로 공각기동대 SAC)", 999, "G", 700, 550, "종료 후 전조 확인", "모드/고確示唆 시 계속"),
        Machine("L戦国乙女5 業火を穿つ宿焔の双刃 (L 전국을녀5)", 999, "G", 700, 530, "백화요란 5G + 引き戻し 확인", "1주기 확인 고려"),
        Machine("スーパーブラックジャック (슈퍼 블랙잭)", 999, "G", 700, 550, "최소 30G", "RC 고확 지속 시 계속"),
        Machine("ToLOVEる TRANCE (투러브 트란스)", 999, "G", 750, 550, "ST 종료 후 즉시", "200G 이내 전개·도키도키 포인트 우대 시 계속"),
        Machine("タクトオーパス (택트 오퍼스)", 999, "G", 750, 550, "100G+α", "오르페 루프 후 약 70% 引き戻し → 반드시 확인"),
        Machine("L邪神ちゃんドロップキック (L 사신짱 드롭킥)", 999, "G", 750, 550, "전조 확인 후 기본 즉시", "99G+α 천국 가능성 확인"),
        Machine("スマスロ ストリートファイター6 (스마슬로 스트리트 파이터 6)", 999, "G", 750, 580, "기본 ST 종료 후", "천국/고모드 의심 → 200G+α"),
        Machine("スマスロ とんでもスキルで異世界放浪メシ (이세계 방랑 메시)", 999, "G", 750, 580, "100G+α", "女神の加護 발생 시 계속"),
        Machine("ゴッドイーター リザレクション (갓이터 리자레クション)", 1000, "G", 750, 580, "기본 즉시", "바캉스→100G, 전원집합→32G, 역린실패→AT까지"),
        Machine("デビルメイクライ5 (デビル メイ ク라이 5)", 1000, "G", 750, 580, "1G + 종료화면/아이캐치", "천국 확인 → 100G"),
        Machine("バイオハザード RE:3 (바이오해저드 RE:3)", 1000, "G", 750, 580, "스테이지 + NE포인트 확인 후", "지하창고/NE 고포인트면 계속"),
        Machine("真・一騎当千 (진 일기당천)", 1000, "G", 750, 580, "勾玉の導き 종료 후", "포인트 강하면 100G+α 천국까지 고려"),
        Machine("スマスロ やじきた道中記参る! (야지키타 도중기)", 1000, "G", 700, 500, "마일/전조 확인 후", "関所チャレンジ 스루 상황에 따라 계속"),
        Machine("かぐや様は告らせたい (카구야 님은 고백받고 싶어)", 1100, "G", 800, 620, "최소 100G (실전 약 130G)", "REG후·5연이후·상성모드 등 계속"),
        Machine("東京リベンジャーズ (도쿄 리벤저스)", 1190, "G", 800, 620, "종료 후 전조 확인", "모드/주기 강한 경우 계속"),
        Machine("L 東京喰種 (L 도쿄 구울)", 1200, "G", 800, 650, "14~16G", "東京上空 이동 시 계속 / 엔딩 후 별도"),
        Machine("スロット ソードアート・オンラインII (소드 아트 온라인 II)", 1200, "G", 850, 680, "50G", "50G 내 引き戻し 추첨 확인"),
        Machine("スマスロ マギアレコード (마기아 레코드)", 999, "G", 750, 580, "종료 후 전조 확인", "모드·CZ 관련 강한 상태면 계속"),
        Machine("Lパチスロ 喰霊-零-Re (가령-제로-Re)", 999, "G", 750, 580, "종료 후 전조 확인", "모드/스루 상황에 따라 계속"),
        Machine("戦国コレクション6 (전국 컬렉션 6)", 1200, "G", 800, 650, "AT후 기본 즉시", "획득120枚 이하 즉시야메 금지, 무장가챠 티켓 보유시 계속"),
    )

    fun load(): List<Machine> {
        val s = p.getString("data", null) ?: return defaults
        return try {
            val a = JSONArray(s)
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                Machine(
                    o.getString("name"), 
                    o.getInt("ceiling"), 
                    o.optString("unit", "G"),
                    o.optInt("highThreshold"), 
                    o.optInt("midThreshold"), 
                    o.optString("note")
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
                put("endMedals", r.endMedals)
                put("exchangeMedalsPer1000", r.exchangeMedalsPer1000)
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
                    o.getString("machineName"), o.getInt("startMoney"), 
                    if (o.has("endMedals")) o.getInt("endMedals") else o.optInt("endMoney", 0),
                    o.optInt("exchangeMedalsPer1000", 0)
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
    var searchQuery by remember { mutableStateOf("") }
    val filtered = ms.filter { 
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.note.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("기종 검색") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    singleLine = true
                )
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
                    if (m.quitRule.isNotBlank()) {
                        Text(
                            text = "야메: ${m.quitRule}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                    }
                    if (m.exceptionRule.isNotBlank()) {
                        Text(
                            text = "예외: ${m.exceptionRule}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 2.dp)
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
        cur >= m.highThreshold -> Triple("HIGH (강력 추천)", Icons.Default.ThumbUp, MaterialTheme.colorScheme.secondary)
        cur >= m.midThreshold -> Triple("MID (진입 가능)", Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary)
        else -> Triple("LOW (진입 불가 / PASS)", Icons.Default.Warning, Color(0xFFFF5252))
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
                        InfoColumn("MID", "${m.midThreshold}${m.unit}")
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
    val halls = hallDefinitions
    val prefs = remember(ctx) { ctx.getSharedPreferences("machines_v2", 0) }
    var sortType by remember {
        mutableStateOf(prefs.getString("hall_sort_type", "거리순") ?: "거리순")
    }
    val sortedHalls = when (sortType) {
        "설치 대수순" -> halls.sortedWith(compareByDescending<Hall> { it.slots }.thenBy { it.distVal })
        "환전율순" -> halls.sortedWith(compareBy<Hall> { it.exchangeMedalsPer1000 }.thenBy { it.distVal })
        else -> halls.sortedBy { it.distVal }
    }
    var sortExpanded by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Visit Candidates", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Box {
                OutlinedButton(onClick = { sortExpanded = true }) {
                    Text("정렬: $sortType")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    listOf("거리순", "설치 대수순", "환전율순").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                sortType = option
                                prefs.edit { putString("hall_sort_type", option) }
                                sortExpanded = false
                            }
                        )
                    }
                }
            }
        }
        items(sortedHalls) { h ->
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
                    if (h.info.isNotBlank()) {
                        Text(
                            text = h.info,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Text(
                            "대여 ${h.rentalMedalsPer1000}枚/1,000円 · 환전 ${h.exchangeMedalsPer1000}枚/1,000円",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    
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
    val maxFinanceInput = 10_000_000
    val (savedTotal, _, _) = store.loadFinance()
    var totalFunds by remember { mutableIntStateOf(savedTotal) }
    var records by remember { mutableStateOf(store.loadRecords()) }

    val currentDate = SimpleDateFormat("M/d", Locale.getDefault()).format(Date())
    val todayRecords = records.filter { it.date == currentDate }
    val todayInvested = todayRecords.sumOf { it.startMoney }
    val todayRevenue = todayRecords.sumOf { it.revenue }

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
    var dateFilter by remember { mutableStateOf("전체 날짜") }
    var hallFilter by remember { mutableStateOf("전체 업장") }
    var resultFilter by remember { mutableStateOf("전체 결과") }
    var statsPeriod by remember { mutableStateOf("오늘") }

    val todayProfit = (todayRevenue - todayInvested).coerceAtLeast(0)
    val todayLoss = (todayInvested - todayRevenue).coerceAtLeast(0)
    val totalProfit = records.sumOf { it.profit }
    val currentBalance = totalFunds + totalProfit
    val yield = if (todayInvested > 0) ((todayRevenue - todayInvested).toDouble() / todayInvested * 100) else 0.0
    val filteredRecords = records.filter { record ->
        (dateFilter == "전체 날짜" || record.date == currentDate) &&
            (hallFilter == "전체 업장" || record.hallName == hallFilter) &&
            (resultFilter == "전체 결과" ||
                (resultFilter == "수익" && record.profit > 0) ||
                (resultFilter == "손실" && record.profit < 0) ||
                (resultFilter == "보합" && record.profit == 0))
    }
    val statsRecords = records.filter { record ->
        if (statsPeriod == "전체") true else {
            val recordDate = runCatching { SimpleDateFormat("M/d", Locale.getDefault()).parse(record.date) }.getOrNull()
            if (recordDate == null) false else {
                val now = Calendar.getInstance()
                val date = Calendar.getInstance().apply {
                    time = recordDate
                    set(Calendar.YEAR, now.get(Calendar.YEAR))
                }
                when (statsPeriod) {
                    "오늘" -> date.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                    "이번 주" -> date.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
                        && date.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                    "이번 달" -> date.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                        && date.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                    else -> true
                }
            }
        }
    }
    val statsInvested = statsRecords.sumOf { it.startMoney }
    val statsRevenue = statsRecords.sumOf { it.revenue }
    val statsProfit = statsRevenue - statsInvested



    if (showDialog || editingRecord != null) {
        var hall by remember(editingRecord) { mutableStateOf(editingRecord?.hallName ?: "업장 선택") }
        var machine by remember(editingRecord) { mutableStateOf(editingRecord?.machineName ?: "기종 선택") }
        var start by remember(editingRecord) { mutableStateOf(editingRecord?.startMoney?.toString() ?: "") }
        var endMedalsStr by remember(editingRecord) { mutableStateOf(editingRecord?.endMedals?.toString() ?: "") }
        var hallExpanded by remember { mutableStateOf(false) }
        var machExpanded by remember { mutableStateOf(false) }
        var validationError by remember(editingRecord) { mutableStateOf<String?>(null) }

        val currentHallObj = hallDefinitions.find { it.name == hall }

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
                    if (currentHallObj != null) {
                        val exchange = exchangeMedalsPer1000(currentHallObj.name)
                        Text(
                            if (exchange > 0) "환전 기준: ${exchange}枚 / 1,000円" else "환전 기준: 미확인",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
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
                        onValueChange = { if (it.all(Char::isDigit)) { start = it; validationError = null } }, 
                        label = { Text("투자 금액 (円)") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endMedalsStr, 
                        onValueChange = { if (it.all(Char::isDigit)) { endMedalsStr = it; validationError = null } }, 
                        label = { Text("회수 메달 (枚)") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    validationError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    val tempMedals = endMedalsStr.toIntOrNull()
                    if (tempMedals != null && currentHallObj != null) {
                        val tempRecord = FinanceRecord(date = "", hallName = hall, machineName = machine, startMoney = 0, endMedals = tempMedals, exchangeMedalsPer1000 = exchangeMedalsPer1000(hall))
                        if (tempRecord.effectiveExchangeMedalsPer1000 > 0) {
                            Text("예상 회수금액: ${tempRecord.revenue}엔", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val s = start.toIntOrNull()
                    val em = endMedalsStr.toIntOrNull()
                    validationError = when {
                        hall == "업장 선택" -> "업장을 선택해 주세요."
                        machine == "기종 선택" -> "기종을 선택해 주세요."
                        s == null || s < 0 -> "투자 금액을 입력해 주세요."
                        s > maxFinanceInput -> "투자 금액은 ${maxFinanceInput}円 이하로 입력해 주세요."
                        em == null || em < 0 -> "회수 메달을 입력해 주세요."
                        em > maxFinanceInput -> "회수 메달은 ${maxFinanceInput}枚 이하로 입력해 주세요."
                        hallDefinitions.none { it.name == hall && it.exchangeMedalsPer1000 > 0 } -> "환전 기준을 확인할 수 없는 업장입니다."
                        else -> null
                    }
                    if (validationError != null) {
                        return@TextButton
                    }
                    val validStartMoney = s ?: return@TextButton
                    val validEndMedals = em ?: return@TextButton
                    if (editingRecord != null) {
                        val r = editingRecord!!
                        val newRecords = records.map { if (it.id == r.id) it.copy(hallName = hall, machineName = machine, startMoney = validStartMoney, endMedals = validEndMedals, exchangeMedalsPer1000 = exchangeMedalsPer1000(hall)) else it }
                        updateFinance(totalFunds, newRecords)
                    } else {
                        val currentDate = SimpleDateFormat("M/d", Locale.getDefault()).format(Date())
                        val newRecords = records + FinanceRecord(date = currentDate, hallName = hall, machineName = machine, startMoney = validStartMoney, endMedals = validEndMedals, exchangeMedalsPer1000 = exchangeMedalsPer1000(hall))
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
                var dateExpanded by remember { mutableStateOf(false) }
                var hallExpanded by remember { mutableStateOf(false) }
                var resultExpanded by remember { mutableStateOf(false) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterMenu(dateFilter, listOf("전체 날짜", currentDate), { dateFilter = it }, dateExpanded, { dateExpanded = it }, Modifier.weight(1f))
                    FilterMenu(hallFilter, listOf("전체 업장") + hallDefinitions.map { it.name }, { hallFilter = it }, hallExpanded, { hallExpanded = it }, Modifier.weight(1f))
                    FilterMenu(resultFilter, listOf("전체 결과", "수익", "손실", "보합"), { resultFilter = it }, resultExpanded, { resultExpanded = it }, Modifier.weight(1f))
                }
            }
            item {
                var periodExpanded by remember { mutableStateOf(false) }
                FilterMenu(statsPeriod, listOf("오늘", "이번 주", "이번 달", "전체"), { statsPeriod = it }, periodExpanded, { periodExpanded = it }, Modifier.fillMaxWidth())
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("💰 총 자금", "$totalFunds", Modifier.weight(1f), onClick = { showEditFunds = true })
                    StatCard("🎰 ${statsPeriod} 투입", "$statsInvested", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("💵 현재 잔액", "$currentBalance", Modifier.weight(1f))
                    StatCard("📊 수익률", "${String.format("%.1f", yield)}%", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("🎯 ${statsPeriod} 결과", "$statsProfit", Modifier.weight(1f), if(statsProfit >= 0) Color(0xFF00FF41) else Color(0xFFFF5252))
                    StatCard("📈 누적 결과", "${records.sumOf { it.profit }}", Modifier.weight(1f))
                }
                }
            }
            item { Text("최근 기록", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp)) }
        items(filteredRecords.sortedByDescending { it.id }) { r ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth().padding(end = 56.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { 
                        Text("${r.date} | ${r.hallName} | ${r.machineName}", style = MaterialTheme.typography.bodyMedium)
                        Text("투자 ${r.startMoney}円 · 회수 ${r.endMedals}枚", style = MaterialTheme.typography.bodyMedium)
                        Text("회수금 ${r.revenue}円 · 기준 ${r.effectiveExchangeMedalsPer1000}枚/1,000円", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("${if (r.profit >= 0) "+" else ""}${r.profit}円", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
fun FilterMenu(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        OutlinedButton(onClick = { onExpandedChange(true) }, modifier = Modifier.fillMaxWidth()) {
            Text(selected, maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        onExpandedChange(false)
                    }
                )
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
