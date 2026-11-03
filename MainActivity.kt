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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray
import org.json.JSONObject

const val PASS = "123456"
val Bg = Color(0xFFE8ECF2)
val Ink = Color(0xFF0F0F10)
val Muted = Color(0xFF6B7280)
val Rule = Color(0xFFE1E5EB)

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bar = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
        setContent { App() }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("vault", Context.MODE_PRIVATE) }
    val list = remember {
        mutableStateListOf<Entry>().apply {
            runCatching { addAll(fromJson(prefs.getString("data", "[]") ?: "[]")) }
        }
    }
    var open by remember { mutableStateOf(false) }
    val save = { prefs.edit().putString("data", toJson(list)).apply() }

    MaterialTheme(colorScheme = lightColorScheme(primary = Ink, onPrimary = Color.White)) {
        Box(Modifier.fillMaxSize().background(Bg)) {
            if (open) Home(list, save) else Lock { open = true }
        }
    }
}

@Composable
fun VCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(34.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFEEF0F5))), shape)
            .padding(24.dp),
        content = content
    )
}

@Composable
fun Credit() {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(buildAnnotatedString {
            withStyle(SpanStyle(color = Muted, fontSize = 12.sp, letterSpacing = 1.2.sp)) { append("powered by ") }
            withStyle(SpanStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)) {
                append("Adnan Sami")
            }
        })
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(34.dp).height(2.dp).background(Color(0xFFC3CAD6), CircleShape))
        Spacer(Modifier.height(8.dp))
        Text("© 2026 Love Forever ❤️", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun VChip(t: String, on: Boolean, click: () -> Unit) {
    Text(
        t, color = if (on) Color.White else Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(CircleShape)
            .background(if (on) Ink else Color(0xFFDDE2EA))
            .clickable(onClick = click)
            .padding(horizontal = 16.dp, vertical = 7.dp)
    )
}

@Composable
fun VField(label: String, v: String, single: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = v, onValueChange = onChange, label = { Text(label) },
        singleLine = single, minLines = if (single) 1 else 3,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink, focusedLabelColor = Ink, cursorColor = Ink,
            unfocusedBorderColor = Color(0xFFC3CAD6),
            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
        )
    )
}

@Composable
fun Lock(onOk: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
            VCard {
                Text("Password Vault", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("পাস কি দিয়ে আনলক করো", color = Muted, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { v ->
                        pin = v.filter { c -> c.isDigit() }.take(6)
                        err = false
                        if (pin == PASS) onOk()
                    },
                    label = { Text("পাস কি") },
                    singleLine = true,
                    isError = err,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Ink, focusedLabelColor = Ink, cursorColor = Ink,
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                    )
                )
                if (err) Text("ভুল পাস কি", color = Color(0xFFD32F2F), fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { if (pin == PASS) onOk() else err = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)
                ) { Text("আনলক", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Credit()
    }
}

@Composable
fun VLine(label: String, value: String, canCopy: Boolean) {
    val ctx = LocalContext.current
    HorizontalDivider(color = Rule)
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 16.sp, modifier = Modifier.width(96.dp))
        Text(value, color = Ink, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (canCopy) {
            Spacer(Modifier.width(8.dp))
            Text(
                "কপি", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clip(CircleShape)
                    .background(Color(0xFFE0E5ED))
                    .clickable {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("vault", value))
                        Toast.makeText(ctx, "কপি হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun EntryCard(e: Entry, onEdit: () -> Unit, onDel: () -> Unit) {
    VCard {
        Text(e.type, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Spacer(Modifier.height(12.dp))
        VLine("অ্যাকাউন্ট", e.account, true)
        VLine("পাসওয়ার্ড", e.pass, true)
        if (e.note.isNotBlank()) VLine("বিবরণ", e.note, false)
        HorizontalDivider(color = Rule)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onEdit) { Text("এডিট", color = Ink, fontWeight = FontWeight.Bold) }
            TextButton(onDel) { Text("ডিলিট", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun EntryForm(e: Entry?, onDone: (Entry) -> Unit, onCancel: () -> Unit) {
    var type by remember { mutableStateOf(e?.type ?: "") }
    var acc by remember { mutableStateOf(e?.account ?: "") }
    var pass by remember { mutableStateOf(e?.pass ?: "") }
    var note by remember { mutableStateOf(e?.note ?: "") }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        VCard(Modifier.padding(16.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    if (e == null) "নতুন পাসওয়ার্ড" else "এডিট করো",
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("FB", "Gmail", "Website", "Other").forEach { t ->
                        VChip(t, type.equals(t, true)) { type = t }
                    }
                }
                VField("কিসের পাসওয়ার্ড (FB / Gmail / Website...)", type) { type = it }
                VField("নম্বর / ইমেইল", acc) { acc = it }
                VField("পাসওয়ার্ড", pass) { pass = it }
                VField("বিবরণ", note, single = false) { note = it }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onCancel) { Text("বাতিল", color = Muted, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDone(
                                Entry(
                                    e?.id ?: System.currentTimeMillis(),
                                    type.trim().ifBlank { "Other" }, acc.trim(), pass, note.trim()
                                )
                            )
                        },
                        enabled = acc.isNotBlank() || pass.isNotBlank(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Ink)
                    ) { Text("সেভ", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun Home(list: SnapshotStateList<Entry>, save: () -> Unit) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var form by remember { mutableStateOf<Entry?>(null) }
    var del by remember { mutableStateOf<Entry?>(null) }

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
            contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column {
                    Text("Password Vault", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text("মোট ${list.size}টি সেভ করা আছে", color = Muted, fontSize = 15.sp)
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VChip("এক্সপোর্ট", false) { exp.launch("password-vault-backup.json") }
                        VChip("ইমপোর্ট", false) { imp.launch(arrayOf("*/*")) }
                    }
                    VField("সার্চ", q) { q = it }
                }
            }
            items(shown, key = { x -> x.id }) { e ->
                EntryCard(e, { form = e }, { del = e })
            }
            if (shown.isEmpty()) item {
                Text(
                    "কিছু নেই। নিচের + চাপো।", color = Muted, fontSize = 16.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)
                )
            }
            item { Credit() }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(24.dp).size(64.dp)
                .shadow(12.dp, CircleShape).background(Ink, CircleShape).clickable { adding = true },
            contentAlignment = Alignment.Center
        ) { Text("+", color = Color.White, fontSize = 34.sp) }
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
                TextButton({ list.remove(e); save(); del = null }) { Text("ডিলিট", color = Color(0xFFD32F2F)) }
            },
            dismissButton = { TextButton({ del = null }) { Text("না", color = Ink) } },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }
}
