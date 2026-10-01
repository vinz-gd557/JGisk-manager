package com.topjohnwu.magisk.ui.home

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.topjohnwu.magisk.core.BuildConfig
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.download.DownloadEngine
import com.topjohnwu.magisk.core.model.module.OnlineModule
import com.topjohnwu.magisk.ui.JgTheme
import com.topjohnwu.magisk.ui.MainActivity
import com.topjohnwu.magisk.ui.module.OnlineModuleSubject
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val REPO_URL = "https://raw.githubusercontent.com/vinz-gd557/JGisk/main/modules.json"
private const val GITHUB_URL = "https://github.com/vinz-gd557/JGisk"
private const val MAGISK_URL = "https://github.com/topjohnwu/Magisk"

@Composable
private fun JgPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun JgLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp
    )
}

// ---------------------------------------------------------------- Tes root

private fun runRootTest(): Pair<Boolean, String> = try {
    val r = Shell.cmd("id").exec()
    val out = r.out.joinToString(" ").trim()
    if (r.isSuccess && out.contains("uid=0")) {
        true to "LULUS - akses root aktif.\n$out"
    } else {
        false to "GAGAL - belum ada akses root.\n" + out.ifEmpty { "(tidak ada output)" }
    }
} catch (e: Exception) {
    false to "GAGAL - ${e.message}"
}

@Composable
fun JgRootTest() {
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    JgPanel {
        JgLabel("TES ROOT")
        Spacer(Modifier.height(8.dp))
        val r = result
        Text(
            text = r?.second ?: "Cek apakah JGisk benar-benar punya akses root (menjalankan perintah id).",
            color = when {
                r == null -> MaterialTheme.colorScheme.onSurfaceVariant
                r.first -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            },
            fontFamily = if (r != null) FontFamily.Monospace else FontFamily.Default,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = !running,
            onClick = {
                running = true
                scope.launch {
                    result = withContext(Dispatchers.IO) { runRootTest() }
                    running = false
                }
            }
        ) { Text(if (running) "Menguji..." else "Tes sekarang") }
    }
}

// ---------------------------------------------------------------- Info perangkat

@Composable
fun JgDeviceCard() {
    val rows = listOf(
        "Perangkat" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Kernel" to (System.getProperty("os.version") ?: "-"),
        "Arsitektur" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "-"),
        "Partisi A/B" to if (Info.isAB) "Ya (slot ${Info.slot.ifEmpty { "?" }})" else "Tidak",
        "Root" to if (Info.isRooted) "Aktif" else "Belum aktif",
    )
    JgPanel {
        JgLabel("INFO PERANGKAT")
        Spacer(Modifier.height(8.dp))
        rows.forEach { (k, v) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(
                    text = k,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.38f)
                )
                Text(
                    text = v,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.62f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Backup boot stok otomatis aktif: tersimpan di folder Download saat kamu menekan Pasang.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// ---------------------------------------------------------------- Tentang + tema

private val accentChoices = listOf(
    "Ungu" to Color(0xFFB388FF),
    "Hijau" to Color(0xFF00E5A8),
    "Biru" to Color(0xFF4FC3F7),
    "Merah" to Color(0xFFFF6B6B),
    "Kuning" to Color(0xFFFFD54F),
    "Pink" to Color(0xFFFF80AB),
)

@Composable
fun JgAboutDialog(onDismiss: () -> Unit) {
    val uri = LocalUriHandler.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tentang JGisk",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("Tutup") }
                }
                JgPanel {
                    JgLabel("VERSI")
                    Spacer(Modifier.height(6.dp))
                    Text(BuildConfig.APP_VERSION_NAME, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "JGisk adalah fork dari Magisk karya topjohnwu dan kontributor, berlisensi GPLv3. Mesin root berasal dari Magisk; tampilan dan fitur tambahan dibuat oleh pembuat JGisk.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                JgPanel {
                    JgLabel("WARNA TEMA")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        accentChoices.forEach { (_, c) ->
                            val selected = !JgTheme.useMonet && JgTheme.accent == c
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .then(
                                        if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { JgTheme.setAccent(c) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(onClick = { JgTheme.setMonet(!JgTheme.useMonet) }) {
                        Text(if (JgTheme.useMonet) "Warna wallpaper: AKTIF" else "Pakai warna wallpaper (Monet)")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { uri.openUri(GITHUB_URL) }, modifier = Modifier.weight(1f)) {
                        Text("GitHub JGisk")
                    }
                    FilledTonalButton(onClick = { uri.openUri(MAGISK_URL) }, modifier = Modifier.weight(1f)) {
                        Text("Magisk (sumber)")
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Repo module

private data class RepoItem(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Int,
    val author: String,
    val description: String,
    val zipUrl: String,
)

private fun JSONObject.str(k: String) = if (isNull(k)) "" else getString(k)

private fun parseRepo(text: String): List<RepoItem> {
    val arr = JSONArray(text)
    val list = ArrayList<RepoItem>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val zip = o.str("zipUrl")
        if (zip.isEmpty()) continue
        list.add(
            RepoItem(
                id = o.str("id").ifEmpty { o.str("name") },
                name = o.str("name"),
                version = o.str("version"),
                versionCode = o.optInt("versionCode", 0),
                author = o.str("author"),
                description = o.str("description"),
                zipUrl = zip,
            )
        )
    }
    return list
}

@Composable
fun JgModuleRepoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<RepoItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val text = withContext(Dispatchers.IO) { ServiceLocator.networkService.fetchString(REPO_URL) }
            items = parseRepo(text)
        } catch (e: Exception) {
            error = "Gagal memuat repo: ${e.message ?: "tidak ada koneksi"}"
        }
        loading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Repo Module JGisk",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("Tutup") }
                }
                if (!Info.env.isActive) {
                    Text(
                        text = "Memasang module butuh root aktif. Daftar tetap bisa dilihat.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                when {
                    loading -> Text("Memuat...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    error != null -> Text(errormkdir -p app/apk/src/main/java/com/topjohnwu/magisk/ui/home, color = MaterialTheme.colorScheme.error)
                    items.isEmpty() -> Text("Belum ada module di repo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> items.forEach { m ->
                        JgPanel {
                            Text(
                                text = "${m.name}  ${m.version}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (m.author.isNotEmpty()) {
                                Text(
                                    text = "oleh ${m.author}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (m.description.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = m.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                enabled = Info.env.isActive,
                                onClick = {
                                    (context as? MainActivity)?.let { activity ->
                                        val module = OnlineModule(
                                            id = m.id,
                                            name = m.name,
                                            version = m.version,
                                            versionCode = m.versionCode,
                                            zipUrl = m.zipUrl,
                                            changelog = "",
                                        )
                                        DownloadEngine.startWithActivity(
                                            activity,
                                            OnlineModuleSubject(module, true)
                                        )
                                        onDismiss()
                                    }
                                }
                            ) { Text(if (Info.env.isActive) "Unduh & pasang" else "Perlu root") }
                        }
                    }
                }
            }
        }
    }
}
