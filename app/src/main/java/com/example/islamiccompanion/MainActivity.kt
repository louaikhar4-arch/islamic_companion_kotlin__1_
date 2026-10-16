package com.example.islamiccompanion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.DateComponents
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val Green = Color(0xFF176B52)
private val Cream = Color(0xFFF6F4EF)
private val Ink = Color(0xFF19352D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(23, 107, 82)
        window.navigationBarColor = android.graphics.Color.rgb(246, 244, 239)
        setContent { IslamicApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IslamicApp() {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var dark by remember { mutableStateOf(false) }
    val colors = if (dark) darkColorScheme(primary = Color(0xFF79D5B0), background = Color(0xFF101B17), surface = Color(0xFF1A2822))
                 else lightColorScheme(primary = Green, background = Cream, surface = Color.White)
    MaterialTheme(colorScheme = colors) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { TopAppBar(title = { Text("رفيق المسلم", fontWeight = FontWeight.Bold) }, actions = {
                IconButton(onClick = { dark = !dark }) { Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "تبديل المظهر") }
            }) },
            bottomBar = {
                NavigationBar {
                    val labels = listOf("الرئيسية", "الصلاة", "القرآن", "الأذكار", "القبلة")
                    val icons = listOf(Icons.Default.Home, Icons.Default.AccessTime, Icons.Default.MenuBook, Icons.Default.Favorite, Icons.Default.Explore)
                    labels.forEachIndexed { i, label -> NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(icons[i], label) }, label = { Text(label, fontSize = 10.sp) }) }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    0 -> HomeScreen(onTab = { tab = it })
                    1 -> PrayerScreen()
                    2 -> QuranScreen()
                    3 -> AdhkarScreen(context)
                    else -> QiblaScreen()
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onTab: (Int) -> Unit) {
    val times = remember { prayerTimes(21.4225, 39.8262) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Green), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text("السلام عليكم", color = Color.White.copy(alpha = .85f))
                    Spacer(Modifier.height(6.dp))
                    Text("مواقيت الصلاة اليوم", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("مكة المكرمة · إعداد افتراضي قابل للتغيير", color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    times.take(5).forEach { (name, time) -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, color = Color.White); Text(time, color = Color.White, fontWeight = FontWeight.Bold)
                    } }
                }
            }
        }
        item { Text("الوصول السريع", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink) }
        item { QuickCard("تابع قراءة القرآن", "افتح المصحف واستكمل قراءتك", Icons.Default.MenuBook) { onTab(2) } }
        item { QuickCard("أذكار اليوم", "أذكار الصباح والمساء وغيرها", Icons.Default.Favorite) { onTab(3) } }
        item { QuickCard("اتجاه القبلة", "اعرف الاتجاه مع إرشادات المعايرة", Icons.Default.Explore) { onTab(4) } }
        item { Text("تنبيه: مواقيت الصلاة المعروضة محسوبة لمكة المكرمة بإعداد افتراضي. غيّر الموقع وطريقة الحساب قبل الاعتماد.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun QuickCard(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(32.dp)); Spacer(Modifier.width(14.dp))
            Column { Text(title, fontWeight = FontWeight.Bold); Text(detail, fontSize = 12.sp, color = Color.Gray) }
        }
    }
}

private fun prayerTimes(lat: Double, lon: Double, methodName: String = "أم القرى"): List<Pair<String, String>> = try {
    val now = Calendar.getInstance()
    val coords = Coordinates(lat, lon)
    val method = when (methodName) {
        "رابطة العالم الإسلامي" -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        "ISNA" -> CalculationMethod.NORTH_AMERICA
        "مصر" -> CalculationMethod.EGYPTIAN
        else -> CalculationMethod.UMM_AL_QURA
    }
    val params = method.parameters.copy(madhab = Madhab.SHAFI)
    val date = DateComponents(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH))
    val pt = PrayerTimes(coords, date, params)
    val fmt = SimpleDateFormat("HH:mm", Locale("ar"))
    listOf("الفجر" to pt.fajr, "الشروق" to pt.sunrise, "الظهر" to pt dhuhr, "العصر" to pt.asr, "المغرب" to pt.maghrib, "العشاء" to pt.isha).map { it.first to fmt.format(Date(it.second.toEpochMilliseconds())) }
} catch (_: Exception) { listOf("الفجر" to "—", "الشروق" to "—", "الظهر" to "—", "العصر" to "—", "المغرب" to "—", "العشاء" to "—") }

@Composable
private fun PrayerScreen() {
    var latitude by remember { mutableStateOf("21.4225") }
    var longitude by remember { mutableStateOf("39.8262") }
    var method by remember { mutableStateOf("أم القرى") }
    var calculated by remember { mutableStateOf(prayerTimes(21.4225, 39.8262, method)) }
    val options = listOf("أم القرى", "رابطة العالم الإسلامي", "ISNA", "مصر")
    var expanded by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("مواقيت الصلاة", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        item { Text("أدخل إحداثيات مدينتك. الموقع لا يُرسل إلى أي خادم.", fontSize = 13.sp) }
        item { OutlinedTextField(latitude, { latitude = it }, label = { Text("خط العرض") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(longitude, { longitude = it }, label = { Text("خط الطول") }, modifier = Modifier.fillMaxWidth()) }
        item {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(method, {}, readOnly = true, label = { Text("طريقة الحساب") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { options.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { method = it; expanded = false }) } }
            }
        }
        item { Button(onClick = { calculated = prayerTimes(latitude.toDoubleOrNull() ?: 21.4225, longitude.toDoubleOrNull() ?: 39.8262, method) }, modifier = Modifier.fillMaxWidth()) { Text("حساب المواقيت") } }
        items(calculated) { (name, time) -> Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, fontWeight = FontWeight.SemiBold); Text(time, color = Green, fontWeight = FontWeight.Bold) } } }
        item { Text("الأوقات محسوبة محليًا. قد تختلف دقائق عن تقويم الجهة الرسمية؛ راجع إعداد المذهب والتعديلات المحلية.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private data class Surah(val number: Int, val name: String, val english: String, val ayahs: Int)

@Composable
private fun QuranScreen() {
    var surahs by remember { mutableStateOf<List<Surah>>(emptyList()) }
    var selected by remember { mutableStateOf<Surah?>(null) }
    var text by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("جارٍ تحميل فهرس السور…") }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try {
            val body = withContext(Dispatchers.IO) { URL("https://api.alquran.cloud/v1/surah").readText() }
            val arr = JSONObject(body).getJSONArray("data")
            surahs = (0 until arr.length()).map { i -> arr.getJSONObject(i).let { Surah(it.getInt("number"), it.getString("name"), it.getString("englishName"), it.getInt("numberOfAyahs")) } }
            status = "اختر سورة لعرض نصها بالرسم العثماني"
        } catch (_: Exception) { status = "تعذر الاتصال. تحقق من الإنترنت ثم أعد فتح الشاشة." }
    }
    LaunchedEffect(selected) {
        selected?.let { s ->
            status = "جارٍ تحميل ${s.name}…"
            try {
                val body = withContext(Dispatchers.IO) { URL("https://api.alquran.cloud/v1/surah/${s.number}/quran-uthmani").readText() }
                val arr = JSONObject(body).getJSONObject("data").getJSONArray("ayahs")
                text = (0 until arr.length()).joinToString("  ۞  ") { i -> arr.getJSONObject(i).getString("text") + " ﴿${arr.getJSONObject(i).getInt("numberInSurah")}﴾" }
                status = "${s.name} · ${s.ayahs} آية · المصدر: AlQuran Cloud"
            } catch (_: Exception) { text = ""; status = "تعذر تحميل السورة. جرّب الاتصال مجددًا." }
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("القرآن الكريم", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        selected?.let { s -> TextButton(onClick = { selected = null; text = "" }) { Text("← الفهرس") }; Text(status, fontSize = 12.sp); Spacer(Modifier.height(10.dp)); Text(text, fontSize = 23.sp, lineHeight = 44.sp, textAlign = TextAlign.Justify, modifier = Modifier.weight(1f).padding(8.dp)) }
            ?: run {
                OutlinedTextField(query, { query = it }, label = { Text("بحث باسم السورة") }, modifier = Modifier.fillMaxWidth())
                Text(status, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn { items(surahs.filter { it.name.contains(query) || it.english.contains(query, true) }) { s ->
                    ListItem(headlineContent = { Text(s.name, fontSize = 19.sp) }, supportingContent = { Text("${s.number} · ${s.ayahs} آية · ${s.english}") }, leadingContent = { Icon(Icons.Default.MenuBook, null, tint = Green) }, modifier = Modifier.fillMaxWidth().clickable { selected = s }, trailingContent = { Text("فتح") })
                    HorizontalDivider()
                } }
            }
    }
}

private val adhkar = listOf(
    "سبحان الله وبحمده" to 100,
    "أستغفر الله وأتوب إليه" to 100,
    "سبحان الله، والحمد لله، ولا إله إلا الله، والله أكبر" to 33,
    "اللهم أعني على ذكرك وشكرك وحسن عبادتك" to 1
)

@Composable
private fun AdhkarScreen(context: Context) {
    val prefs = remember { context.getSharedPreferences("adhkar", Context.MODE_PRIVATE) }
    var count by remember { mutableIntStateOf(prefs.getInt("count", 0)) }
    var index by remember { mutableIntStateOf(prefs.getInt("index", 0).coerceIn(0, adhkar.lastIndex)) }
    val (phrase, target) = adhkar[index]
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("الأذكار", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        item { Text("أذكار مختارة · اختر الذكر واضغط للعدّ", color = Color.Gray) }
        item { Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(phrase, fontSize = 25.sp, lineHeight = 42.sp, textAlign = TextAlign.Center, color = Ink)
            Spacer(Modifier.height(14.dp)); Text("$count / $target", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Green)
            Spacer(Modifier.height(12.dp)); Button(onClick = { count = if (count >= target) 0 else count + 1; prefs.edit().putInt("count", count).apply() }, modifier = Modifier.size(86.dp), shape = RoundedCornerShape(50)) { Text("اذكر", fontSize = 16.sp) }
            TextButton(onClick = { count = 0; prefs.edit().putInt("count", 0).apply() }) { Text("إعادة العداد") }
        } } }
        item { Text("اختر ذكرًا", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth()) }
        items(adhkar.indices.toList()) { i -> Card(onClick = { index = i; count = 0; prefs.edit().putInt("index", i).putInt("count", 0).apply() }, modifier = Modifier.fillMaxWidth()) { Text(adhkar[i].first, Modifier.padding(16.dp), fontSize = 17.sp) } }
        item { Text("ملاحظة: هذه مجموعة أولية؛ راجع تخريج الأذكار واعتمادها علميًا قبل النشر.", fontSize = 12.sp, color = Color.Gray) }
    }
}

@Composable
private fun QiblaScreen() {
    val context = LocalContext.current
    var latitude by remember { mutableStateOf(21.4225) }
    var longitude by remember { mutableStateOf(39.8262) }
    var azimuth by remember { mutableFloatStateOf(Float.NaN) }
    var hasSensor by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        hasSensor = sensor != null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val matrix = FloatArray(9); val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, orientation)
                azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat().let { (it + 360) % 360 }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    val qibla = remember(latitude, longitude) { qiblaBearing(latitude, longitude) }
    val relative = if (azimuth.isNaN()) qibla else (qibla - azimuth + 360) % 360
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("اتجاه القبلة", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("${qibla.roundToInt()}° من الشمال الحقيقي", color = Green, fontSize = 17.sp)
        Spacer(Modifier.height(16.dp))
        Text(if (azimuth.isNaN()) "🕋" else "➤", fontSize = 100.sp, color = Green)
        Text(if (azimuth.isNaN()) "الزاوية محسوبة من الموقع الافتراضي" else "وجّه السهم نحو القبلة · ${relative.roundToInt()}°", fontSize = 17.sp, textAlign = TextAlign.Center)
        Text(if (hasSensor) "حساس الاتجاه متاح. حرّك الهاتف على شكل رقم 8 عند عدم استقرار البوصلة." else "لم يُعثر على حساس اتجاه؛ اعتمد على زاوية القبلة الظاهرة.", fontSize = 13.sp, textAlign = TextAlign.Center, color = Color.Gray)
        Text("الموقع الحالي في النموذج: مكة المكرمة (إحداثيات افتراضية). حدّث الإحداثيات في شاشة الصلاة.", fontSize = 12.sp, textAlign = TextAlign.Center)
        Text("تتأثر البوصلة بالمغناطيس والمعادن. لا تستخدم الخريطة offline في هذه النسخة.", fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

private fun qiblaBearing(lat: Double, lon: Double): Double {
    val kaabaLat = Math.toRadians(21.4225); val kaabaLon = Math.toRadians(39.8262)
    val userLat = Math.toRadians(lat); val deltaLon = kaabaLon - Math.toRadians(lon)
    val y = kotlin.math.sin(deltaLon)
    val x = kotlin.math.cos(userLat) * kotlin.math.tan(kaabaLat) - kotlin.math.sin(userLat) * kotlin.math.cos(deltaLon)
    return (Math.toDegrees(kotlin.math.atan2(y, x)) + 360) % 360
}
