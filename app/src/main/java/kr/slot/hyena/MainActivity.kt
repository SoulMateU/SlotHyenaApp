package kr.slot.hyena

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import org.json.JSONArray
import org.json.JSONObject

data class Machine(
    val name: String,
    val ceiling: Int,
    val unit: String = "G",
    val reward: String = "미검증",
    val classType: String = "미검증",
    val highThreshold: Int,
    val midThreshold: Int,
    val note: String = ""
)

class MainActivity:ComponentActivity(){
    override fun onCreate(b:Bundle?){
        super.onCreate(b)
        setContent{ App(MachineStore(this)) }
    }
}

class MachineStore(val context: Context){
    private val p=context.getSharedPreferences("machines_v2",0)
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
        Machine("戦国コレクション6", 1200, "G", "AT", "직접 AT", 800, 650, "AT간 1200G 컷라인 충족")
    )
    fun load(): List<Machine> {
        val s = p.getString("data", null) ?: return defaults
        return try {
            val a = JSONArray(s); List(a.length()) { i ->
                val o = a.getJSONObject(i)
                Machine(
                    o.getString("name"), o.getInt("ceiling"), o.optString("unit"),
                    o.optString("reward"), o.optString("classType"),
                    o.optInt("highThreshold"), o.optInt("midThreshold"), o.optString("note")
                )
            }
        } catch (_: Exception) { defaults }
    }

    fun save(ms: List<Machine>) {
        val a = JSONArray(); ms.forEach { m ->
            a.put(JSONObject().apply {
                put("name", m.name); put("ceiling", m.ceiling); put("unit", m.unit)
                put("reward", m.reward); put("classType", m.classType)
                put("highThreshold", m.highThreshold); put("midThreshold", m.midThreshold)
                put("note", m.note)
            })
        }; p.edit().putString("data", a.toString()).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(store:MachineStore){
    var ms by remember{mutableStateOf(store.load())}
    var tab by remember{mutableIntStateOf(0)}
    MaterialTheme{
        Scaffold(topBar={TopAppBar(title={Text("🎰 천장 하이에나")})},
            bottomBar={NavigationBar{
                NavigationBarItem(selected = tab==0, onClick = {tab=0}, label = {Text("기종")}, icon = {Icon(Icons.AutoMirrored.Filled.List, null)})
                NavigationBarItem(selected = tab==1, onClick = {tab=1}, label = {Text("계산")}, icon = {Icon(Icons.Filled.Calculate, null)})
                NavigationBarItem(selected = tab==2, onClick = {tab=2}, label = {Text("업장")}, icon = {Icon(Icons.Filled.Place, null)})
                NavigationBarItem(selected = tab==3, onClick = {tab=3}, label = {Text("관리")}, icon = {Icon(Icons.Filled.Settings, null)})
            }}){pad->Box(Modifier.padding(pad).fillMaxSize()){
            when(tab){
                0->ListScreen(ms)
                1->CalcScreen(ms)
                2->StoreScreen(store.context)
                3->ManageScreen(ms = ms, onSave = { updatedMs -> ms = updatedMs; store.save(updatedMs) })
            }
        }}
    }
}

@Composable fun ListScreen(ms:List<Machine>){
    var filter by remember{mutableStateOf("전체")}
    val filtered=when(filter){
        "AT 유지"->ms.filter{it.classType=="직접 AT"}
        "보너스 유지"->ms.filter{it.classType=="보너스 유지"}
        "PASS"->ms.filter{it.classType=="PASS"}
        else->ms
    }
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{Text("후보 ${filtered.size}개 / 전체 ${ms.size}개",style=MaterialTheme.typography.headlineSmall)}
        item{Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("전체","AT 유지","보너스 유지","PASS").forEach{x->
            FilterChip(selected=filter==x,onClick={filter=x},label={Text(x)})
        }}}
        items(filtered){m->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
            Text(m.name, style = MaterialTheme.typography.titleMedium)
            Text("천장: ${m.ceiling}${m.unit}  / 보상: ${m.reward}")
            Text("분류: ${m.classType}")
            if (m.note.isNotBlank()) Text(m.note)
        }}}
    }
}

@Composable fun CalcScreen(ms:List<Machine>){
    var idx by remember{mutableIntStateOf(0)}
    var g by remember{mutableStateOf("")}
    val m=ms.getOrNull(idx); val cur=g.toIntOrNull()
    val remain=if(m?.ceiling!=null&&cur!=null)(m.ceiling!!-cur).coerceAtLeast(0)else null
    val verdict = when {
        m == null -> ""
        cur != null && cur >= m.ceiling -> "🔵 천장 도달/초과"
        m.classType == "보너스 유지" || m.classType == "직접 AT" -> {
            if (cur != null && cur >= m.highThreshold) "🟢 HIGH (강력 추천)"
            else if (cur != null && cur >= m.midThreshold) "🟡 MID (추천)"
            else "🔴 LOW (진입 불가)"
        }
        else -> "🔴 PASS"
    }
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("공략 계산",style=MaterialTheme.typography.headlineSmall)
        var open by remember{mutableStateOf(false)}
        Button({open=true},Modifier.fillMaxWidth()){Text(m?.name?:"기종 선택")}
        DropdownMenu(expanded = open, onDismissRequest = {open=false}){ms.forEachIndexed{i,x->DropdownMenuItem(text={Text(x.name)},onClick={idx=i;g="";open=false})}}
        OutlinedTextField(
            value = g,
            onValueChange = {g=it.filter(Char::isDigit)},
            label={Text("현재 ${m?.unit?:"G"}")},
            modifier=Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        if (m != null && cur != null) {
            Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text("🎰 ${m.name}", style = MaterialTheme.typography.titleLarge)
                Text("현재: $cur${m.unit} / 천장: ${m.ceiling}${m.unit}")
                Text("남은: ${remain}${m.unit}", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("HIGH 기준: ${m.highThreshold}${m.unit}")
                Text("MID 기준: ${m.midThreshold}${m.unit}")
                Text("천장 보상: ${m.reward}")
                Spacer(Modifier.height(8.dp))
                Text(verdict, style = MaterialTheme.typography.headlineMedium)
            }}
        }
    }
}

@Composable fun ManageScreen(ms: List<Machine>, onSave: (List<Machine>) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text("기종 관리 (준비 중)", style = MaterialTheme.typography.headlineSmall)
    }
}


@Composable fun StoreScreen(ctx: Context){
    val halls = listOf(
        Hall("HINODE 大野城店", "약 2km (福岡県大野城市瓦田4-12-5)", "https://66100.p-world.jp", 360, 2.0),
        Hall("玉屋409雑餉隈", "약 2.2km (福岡県福岡市博多区南本町2-1-1)", "https://92039.p-world.jp", 400, 2.2),
        Hall("BEAM by HIKARI", "약 2.5km (福岡県大野城市御笠川1-13-3)", "https://80684.p-world.jp", 450, 2.5),
        Hall("ワンダーランド南ヶ丘店", "약 3.8km (福岡県大野城市紫台19-10)", "https://42071.p-world.jp", 500, 3.8),
        Hall("MJアリーナ井尻店", "약 4km (福岡県春日市桜ヶ丘4-14)", "https://mjijiri.p-world.jp", 300, 4.0),
        Hall("Aパーク春日店", "약 4.1km (福岡県春日市日の出町5-24)", "https://www.p-world.co.jp/fukuoka/a-parkkasuga.htm", 250, 4.1),
        Hall("つかさ月隈店", "약 4.5km (福岡県福岡市博多区西月隈1-1-43)", "https://20814.p-world.jp", 350, 4.5),
        Hall("プラザ本店II", "약 4.8km (福岡県福岡市博多区西月隈3-5-32)", "https://43342.p-world.jp", 600, 4.8)
    ).sortedBy { it.distVal }

    LazyColumn(contentPadding=PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)){
        items(halls){ h ->
            Card(Modifier.fillMaxWidth()){
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)){
                    Text(h.name, style=MaterialTheme.typography.titleLarge)
                    Text("📍 숙소에서 ${h.dist}")
                    Text("🎰 슬롯 ${h.slots}대")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)){
                        Button(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(h.url))
                            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }) { Text("P-WORLD") }
                        OutlinedButton(onClick = {
                            val mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(h.name.substringBefore(" (") + " " + h.dist.substringAfter("( ").substringBefore(")"))}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                            ctx.startActivity(mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }) { Text("지도 열기") }
                    }
                }
            }
        }
    }
}
data class Hall(val name:String, val dist:String, val url:String, val slots:Int, val distVal: Double)
