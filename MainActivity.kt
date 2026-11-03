package com.adnan.passwordvault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val PASS = "123456"
val Ink = Color(0xFF111114)
val Muted = Color(0xFF6E7381)
val Soft = Color(0xFF9AA0AE)
val Rule = Color(0xFFE4E7EE)
val Danger = Color(0xFFD32F2F)
val DarkBrush = Brush.verticalGradient(listOf(Color(0xFF2B2D36), Color(0xFF0F1014)))
val LocalFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

data class Entry(val id: Long, val type: String, val account: String, val pass: String, val note: String)

fun toJson(l: List<Entry>): String {
    val a = JSONArray()
    l.forEach { e ->
        a.put(
            JSONObject().put("id", e.id).put("type", e.type).put("account", e.account)
                .put("pass", e.pass).put("note", e.note)
        )
    }
    return a.toString()
}

fun fromJson(s: String): List<Entry> {
    val a = JSONArray(s)
    return (0 until a.length()).map { i ->
        val o = a.getJSONObject(i)
        Entry(
            o.optLong("id", System.nanoTime() + i), o.optString("type"), o.optString("account"),
            o.optString("pass"), o.optString("note")
        )
    }
}

fun copyText(ctx: Context, t: String, msg: String = "কপি হয়েছে") {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("vault", t))
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalTextApi::class)
fun loadFamily(ctx: Context): FontFamily {
    val have = runCatching { ctx.assets.list("fonts")?.toSet() }.getOrNull() ?: emptySet()
    val fonts = listOf(
        "HindSiliguri-Regular.ttf" to FontWeight.Normal,
        "HindSiliguri-Medium.ttf" to FontWeight.Medium,
        "HindSiliguri-SemiBold.ttf" to FontWeight.SemiBold,
        "HindSiliguri-Bold.ttf" to FontWeight.Bold
    ).filter { p -> p.first in have }
        .map { p -> Font(assetManager = ctx.assets, path = "fonts/${p.first}", weight = p.second) }
    return if (fonts.isEmpty()) FontFamily.Default else FontFamily(fonts)
}

fun Typography.withFont(f: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bar = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
        setContent { App() }
    }
}

// ---------- Look & feel helpers ----------

fun Modifier.soft(shape: Shape, elevation: Dp = 14.dp): Modifier = this
    .shadow(elevation, shape, ambientColor = Color(0x1F1B2335), spotColor = Color(0x381B2335))
    .background(Brush.verticalGradient(listOf(Color(0xFFFCFCFD), Color(0xFFEFF1F5))), shape)
    .border(2.dp, Color.White, shape)

@Composable
fun Ico(kind: String, color: Color = Ink, dp: Dp = 24.dp, bg: Color = Color.White) {
    Canvas(Modifier.size(dp)) {
        val u = size.minDimension / 24f
        fun st(w: Float) = Stroke(width = w * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun ln(a: Float, b: Float, c: Float, d: Float, w: Float, col: Color = color) =
            drawLine(col, Offset(a * u, b * u), Offset(c * u, d * u), strokeWidth = w * u, cap = StrokeCap.Round)
        when (kind) {
            "search" -> {
                drawCircle(color, 7f * u, Offset(10.5f * u, 10.5f * u), style = st(2.2f))
                ln(15.8f, 15.8f, 20.5f, 20.5f, 2.4f)
            }
            "dots" -> listOf(5f, 12f, 19f).forEach { x -> drawCircle(color, 2.1f * u, Offset(x * u, 12f * u)) }
            "plus" -> {
                ln(12f, 5f, 12f, 19f, 2.6f)
                ln(5f, 12f, 19f, 12f, 2.6f)
            }
            "lock" -> {
                drawArc(color, 180f, 180f, false, Offset(8.2f * u, 3.4f * u), Size(7.6f * u, 7.6f * u), style = st(2.2f))
                ln(8.2f, 7.2f, 8.2f, 11f, 2.2f)
                ln(15.8f, 7.2f, 15.8f, 11f, 2.2f)
                drawRoundRect(color, Offset(5f * u, 10.2f * u), Size(14f * u, 10.5f * u), CornerRadius(3.2f * u))
                drawCircle(bg, 1.7f * u, Offset(12f * u, 14.6f * u))
                ln(12f, 14.8f, 12f, 17.4f, 1.8f, bg)
            }
            "copy" -> {
                drawRoundRect(color, Offset(9f * u, 9f * u), Size(11f * u, 11.5f * u), CornerRadius(3f * u), style = st(1.9f))
                drawRoundRect(bg, Offset(4f * u, 3.5f * u), Size(11f * u, 11.5f * u), CornerRadius(3f * u))
                drawRoundRect(color, Offset(4f * u, 3.5f * u), Size(11f * u, 11.5f * u), CornerRadius(3f * u), style = st(1.9f))
            }
            "edit" -> {
                ln(6f, 17.5f, 17.5f, 6f, 3.4f)
                ln(4.5f, 21f, 19.5f, 21f, 1.8f)
            }
            "trash" -> {
                ln(4.5f, 7f, 19.5f, 7f, 2f)
                ln(9.5f, 4f, 14.5f, 4f, 2f)
                drawRoundRect(color, Offset(6.5f * u, 9f * u), Size(11f * u, 11.5f * u), CornerRadius(2.5f * u), style = st(1.9f))
            }
            else -> {}
        }
    }
}

@Composable
fun IconBtn(kind: String, dp: Dp = 56.dp, ic: Dp = 24.dp, onClick: (() -> Unit)? = null, tint: Color = Ink) {
    val sh = RoundedCornerShape(dp * 0.36f)
    Box(
        Modifier.size(dp).soft(sh, 10.dp).clip(sh)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) { Ico(kind, tint, ic, Color(0xFFF7F8FA)) }
}

@Composable
fun Field(
    v: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier,
    single: Boolean = true, icon: String? = null, numeric: Boolean = false,
    hide: Boolean = false, center: Boolean = false
) {
    val sh = RoundedCornerShape(24.dp)
    val font = LocalFont.current
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).soft(sh, 5.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Ico(icon, Soft, 24.dp)
            Spacer(Modifier.width(12.dp))
        }
        Box(Modifier.weight(1f)) {
            if (v.isEmpty()) Text(
                hint, color = Soft, fontSize = 17.sp,
                modifier = if (center) Modifier.fillMaxWidth() else Modifier,
                textAlign = if (center) TextAlign.Center else TextAlign.Start
            )
            BasicTextField(
                value = v, onValueChange = onChange, singleLine = single, minLines = if (single) 1 else 3,
                textStyle = TextStyle(
                    fontSize = 17.sp, color = Ink, fontFamily = font,
                    textAlign = if (center) TextAlign.Center else TextAlign.Start
                ),
                cursorBrush = SolidColor(Ink),
                visualTransformation = if (hide) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Text),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BigBtn(text: String, modifier: Modifier = Modifier, dark: Boolean = true, enabled: Boolean = true, onClick: () -> Unit) {
    val sh = RoundedCornerShape(22.dp)
    Box(
        modifier.alpha(if (enabled) 1f else 0.4f).height(56.dp).clip(sh)
            .background(
                if (dark) DarkBrush else Brush.verticalGradient(listOf(Color(0xFFF4F5F8), Color(0xFFE5E8EE))), sh
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (dark) Color.White else Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun VChip(t: String, on: Boolean, click: () -> Unit) {
    Text(
        t, color = if (on) Color.White else Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(CircleShape)
            .background(if (on) Ink else Color(0xFFE6E9EF))
            .clickable(onClick = click)
            .padding(horizontal = 18.dp, vertical = 9.dp)
    )
}

@Composable
fun ActionPill(kind: String, text: String, c: Color, onClick: () -> Unit) {
    val bg = Color(0xFFE9ECF2)
    Row(
        Modifier.clip(CircleShape).background(bg).clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Ico(kind, c, 18.dp, bg)
        Spacer(Modifier.width(8.dp))
        Text(text, color = c, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Credit(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("powered by", color = Soft, fontSize = 15.sp, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(2.dp))
        Text("Adnan Sami", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("© 2026 Love Forever ❤️", color = Soft, fontSize = 13.sp, letterSpacing = 0.4.sp)
    }
}

// ---------- Screens ----------

@Composable
fun App() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("vault", Context.MODE_PRIVATE) }
    val font = remember { loadFamily(ctx) }
    val list = remember {
        mutableStateListOf<Entry>().apply {
            runCatching { addAll(fromJson(prefs.getString("data", "[]") ?: "[]")) }
        }
    }
    var open by remember { mutableStateOf(false) }
    val save = { prefs.edit().putString("data", toJson(list)).apply() }

    CompositionLocalProvider(LocalFont provides font) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = Ink, onPrimary = Color.White, surface = Color.White, surfaceContainer = Color.White
            ),
            typography = Typography().withFont(font),
            shapes = Shapes(extraSmall = RoundedCornerShape(18.dp))
        ) {
            ProvideTextStyle(TextStyle(fontFamily = font, color = Ink)) {
                Box(
                    Modifier.fillMaxSize().drawBehind {
                        drawRect(Brush.verticalGradient(listOf(Color(0xFFF0F1F5), Color.White, Color.White)))
                        drawRect(
                            Brush.radialGradient(
                                listOf(Color(0x66A9B0C2), Color.Transparent),
                                center = Offset(size.width * 1.05f, size.height * 0.52f),
                                radius = size.width * 0.75f
                            )
                        )
                    }
                ) {
                    if (open) Home(list, save) { open = false } else Lock { open = true }
                }
            }
        }
    }
}

@Composable
fun Lock(onOk: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBtn("lock", 92.dp, 42.dp)
            Spacer(Modifier.height(24.dp))
            Text("Password Vault", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(4.dp))
            Text("পাস কি দিয়ে আনলক করো", color = Muted, fontSize = 16.sp)
            Spacer(Modifier.height(30.dp))
            Field(
                pin, { v ->
                    pin = v.filter { c -> c.isDigit() }.take(6)
                    err = false
                    if (pin == PASS) onOk()
                }, "Passcode", numeric = true, hide = true, center = true
            )
            if (err) Text("ভুল পাস কি", color = Danger, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(18.dp))
            BigBtn("আনলক", Modifier.fillMaxWidth()) { if (pin == PASS) onOk() else err = true }
        }
        Credit()
    }
}

@Composable
fun VLine(label: String, value: String, canCopy: Boolean) {
    val ctx = LocalContext.current
    HorizontalDivider(color = Rule)
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Soft, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(value.ifBlank { "—" }, color = Ink, fontSize = 18.sp)
        }
        if (canCopy) IconBtn("copy", 42.dp, 19.dp, { copyText(ctx, value) })
    }
}

@Composable
fun EntryCard(e: Entry, onEdit: () -> Unit, onDel: () -> Unit) {
    val ctx = LocalContext.current
    var open by remember(e.id) { mutableStateOf(false) }
    val sh = RoundedCornerShape(32.dp)
    Column(
        Modifier.fillMaxWidth().animateContentSize().soft(sh, 16.dp).clip(sh)
            .clickable { open = !open }.padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.type, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(
                    e.account.ifBlank { "—" }, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    maxLines = if (open) 3 else 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (!open) {
                Spacer(Modifier.width(12.dp))
                IconBtn("copy", 44.dp, 20.dp, { copyText(ctx, e.pass, "পাসওয়ার্ড কপি হয়েছে") })
            }
        }
        if (open) {
            Spacer(Modifier.height(14.dp))
            VLine("অ্যাকাউন্ট", e.account, true)
            VLine("পাসওয়ার্ড", e.pass, true)
            if (e.note.isNotBlank()) VLine("বিবরণ", e.note, false)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPill("edit", "এডিট", Ink, onEdit)
                ActionPill("trash", "ডিলিট", Danger, onDel)
            }
        }
    }
}

@Composable
fun EntryForm(e: Entry?, onDone: (Entry) -> Unit, onCancel: () -> Unit) {
    var type by remember { mutableStateOf(e?.type ?: "") }
    var acc by remember { mutableStateOf(e?.account ?: "") }
    var pass by remember { mutableStateOf(e?.pass ?: "") }
    var note by remember { mutableStateOf(e?.note ?: "") }
    val sh = RoundedCornerShape(32.dp)
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth().soft(sh, 20.dp).padding(22.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                if (e == null) "নতুন পাসওয়ার্ড" else "এডিট করো",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("FB", "Gmail", "Website", "Other").forEach { t ->
                    VChip(t, type.equals(t, true)) { type = t }
                }
            }
            FLabel("কিসের পাসওয়ার্ড")
            Field(type, { type = it }, "FB / Gmail / Website / অন্য কিছু")
            FLabel("নম্বর / ইমেইল")
            Field(acc, { acc = it }, "01XXXXXXXXX বা example@mail.com")
            FLabel("পাসওয়ার্ড")
            Field(pass, { pass = it }, "পাসওয়ার্ড")
            FLabel("বিবরণ")
            Field(note, { note = it }, "যা মনে রাখতে চাও", single = false)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigBtn("বাতিল", Modifier.weight(1f), dark = false) { onCancel() }
                BigBtn("সেভ", Modifier.weight(1f), enabled = acc.isNotBlank() || pass.isNotBlank()) {
                    onDone(
                        Entry(
                            e?.id ?: System.currentTimeMillis(),
                            type.trim().ifBlank { "Other" }, acc.trim(), pass, note.trim()
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun FLabel(t: String) {
    Text(
        t, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
fun Home(list: SnapshotStateList<Entry>, save: () -> Unit, onLock: () -> Unit) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var form by remember { mutableStateOf<Entry?>(null) }
    var del by remember { mutableStateOf<Entry?>(null) }
    val today = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH).format(Date()) }

    val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u: Uri? ->
        u?.let { uri ->
            runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { o -> o.write(toJson(list).toByteArray()) }
                Toast.makeText(ctx, "ব্যাকআপ সেভ হয়েছে", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u: Uri? ->
        u?.let { uri ->
            runCatching {
                val s = ctx.contentResolver.openInputStream(uri)!!.bufferedReader().use { r -> r.readText() }
                val ids = list.map { x -> x.id }.toSet()
                val add = fromJson(s).filter { x -> x.id !in ids }
                list.addAll(add)
                save()
                Toast.makeText(ctx, "${add.size}টি যোগ হয়েছে", Toast.LENGTH_SHORT).show()
            }.onFailure { Toast.makeText(ctx, "ফাইল পড়া যায়নি", Toast.LENGTH_SHORT).show() }
        }
    }

    val shown = list.reversed().filter { x ->
        q.isBlank() || listOf(x.type, x.account, x.note).any { s -> s.contains(q, true) }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 130.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBtn("lock", 54.dp, 25.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Password Vault", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink,
                            maxLines = 1, softWrap = false, letterSpacing = (-0.3).sp
                        )
                        Text(today, color = Muted, fontSize = 15.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Box {
                        IconBtn("dots", 54.dp, 24.dp, { menu = true })
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("এক্সপোর্ট ব্যাকআপ") },
                                onClick = { menu = false; exp.launch("password-vault-backup.json") }
                            )
                            DropdownMenuItem(
                                text = { Text("ইমপোর্ট ব্যাকআপ") },
                                onClick = { menu = false; imp.launch(arrayOf("*/*")) }
                            )
                            DropdownMenuItem(
                                text = { Text("এখনই লক করো") },
                                onClick = { menu = false; onLock() }
                            )
                        }
                    }
                }
            }
            item { Field(q, { q = it }, "Search", icon = "search") }
            items(shown, key = { x -> x.id }) { e ->
                EntryCard(e, { form = e }, { del = e })
            }
            if (shown.isEmpty()) item {
                Text(
                    if (list.isEmpty()) "এখনো কিছু নেই। নিচের + চাপো।" else "কিছু পাওয়া যায়নি।",
                    color = Muted, fontSize = 16.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                )
            }
            item { Credit() }
        }

        val fab = RoundedCornerShape(24.dp)
        Box(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(24.dp).size(66.dp)
                .shadow(18.dp, fab, ambientColor = Color(0x33000000), spotColor = Color(0x66000000))
                .clip(fab).background(DarkBrush, fab).clickable { adding = true },
            contentAlignment = Alignment.Center
        ) { Ico("plus", Color.White, 28.dp) }
    }

    if (adding) EntryForm(null, { n -> list.add(n); save(); adding = false }, { adding = false })
    form?.let { e ->
        EntryForm(e, { n ->
            val i = list.indexOfFirst { x -> x.id == e.id }
            if (i >= 0) list[i] = n
            save(); form = null
        }, { form = null })
    }
    del?.let { e ->
        AlertDialog(
            onDismissRequest = { del = null },
            title = { Text("ডিলিট করবে?", fontWeight = FontWeight.Bold) },
            text = { Text("${e.type} — ${e.account}") },
            confirmButton = {
                TextButton({ list.remove(e); save(); del = null }) { Text("ডিলিট", color = Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton({ del = null }) { Text("না", color = Ink, fontWeight = FontWeight.Bold) } },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }
}
