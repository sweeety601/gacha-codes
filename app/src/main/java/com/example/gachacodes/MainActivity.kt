package com.example.gachacodes

import android.Manifest
import android.app.*
import android.content.*
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

data class Code(val game: String, val code: String, val source: String, val seen: String)

private val GenshinPurple = Color(0xFF7C5CFF)
private val WuWaBlue = Color(0xFF3C9DFF)
private val DarkBg = Color(0xFF101116)
private val CardBg = Color(0xFF191B22)

class MainActivity : ComponentActivity() {
    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createChannel()

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "code_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<CodeWorker>(15, TimeUnit.MINUTES).build()
        )

        setContent { App() }
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    "codes", "Новые промокоды",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun App() {
        var selected by remember { mutableStateOf("Genshin Impact") }
        var codes by remember { mutableStateOf<List<Code>>(emptyList()) }
        var loading by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            loading = true
            codes = loadCodes()
            loading = false
        }

        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = if (selected == "Genshin Impact") GenshinPurple else WuWaBlue,
                background = DarkBg,
                surface = CardBg
            )
        ) {
            Scaffold(
                containerColor = DarkBg,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Gacha Codes", fontWeight = FontWeight.Bold)
                                Text(
                                    "Свежие промокоды",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    loading = true
                                    WorkManager.getInstance(this@MainActivity)
                                        .enqueue(OneTimeWorkRequestBuilder<CodeWorker>().build())
                                }
                            ) {
                                Icon(Icons.Default.Refresh, "Обновить")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = DarkBg
                        )
                    )
                }
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    GameSelector(selected) { selected = it }

                    Spacer(Modifier.height(14.dp))

                    val filtered = codes.filter { it.game == selected }

                    if (loading) {
                        LinearProgressIndicator(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Text(
                        if (filtered.isEmpty()) "Промокоды пока не найдены"
                        else "${filtered.size} код${if (filtered.size == 1) "" else "а"}",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filtered, key = { it.game + it.code }) { code ->
                            CodeCard(code)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GameSelector(selected: String, onSelect: (String) -> Unit) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GameChip(
                "Genshin Impact", "Genshin", selected == "Genshin Impact",
                GenshinPurple, Modifier.weight(1f)
            ) { onSelect("Genshin Impact") }

            GameChip(
                "Wuthering Waves", "WuWa", selected == "Wuthering Waves",
                WuWaBlue, Modifier.weight(1f)
            ) { onSelect("Wuthering Waves") }
        }
    }

    @Composable
    private fun GameChip(
        fullName: String,
        label: String,
        selected: Boolean,
        accent: Color,
        modifier: Modifier,
        onClick: () -> Unit
    ) {
        Surface(
            modifier = modifier.clickable { onClick() },
            shape = RoundedCornerShape(18.dp),
            color = if (selected) accent.copy(alpha = .22f) else CardBg,
            border = if (selected) ButtonDefaults.outlinedButtonBorder else null
        ) {
            Column(
                Modifier.padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(label, fontWeight = FontWeight.Bold)
                Text(
                    fullName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }
        }
    }

    @Composable
    private fun CodeCard(code: Code) {
        var copied by remember { mutableStateOf(false) }
        val accent = if (code.game == "Genshin Impact") GenshinPurple else WuWaBlue

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            code.code,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "Источник: ${code.source}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.LightGray
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            val clipboard =
                                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                android.content.ClipData.newPlainText("Промокод", code.code)
                            )
                            copied = true
                            Toast.makeText(
                                this@MainActivity,
                                "Код скопирован",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = accent.copy(alpha = .20f),
                            contentColor = accent
                        )
                    ) {
                        Icon(Icons.Default.ContentCopy, "Скопировать")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accent.copy(alpha = .20f), Color.Transparent)
                            )
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        if (copied) "✓ Скопировано" else "Нажми кнопку справа, чтобы скопировать",
                        color = if (copied) accent else Color.LightGray
                    )
                }
            }
        }
    }

    private suspend fun loadCodes(): List<Code> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(FEED_URL).build()
            val body = client.newCall(request).execute().body?.string() ?: "[]"
            val arr = JSONArray(body)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Code(
                    o.getString("game"),
                    o.getString("code"),
                    o.getString("source"),
                    o.getString("seen")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        // ЗАМЕНИ на адрес своего GitHub Pages.
        const val FEED_URL =
            "https://sweeety601.github.io/gacha-codes/codes.json"
    }
}

class CodeWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("seen", Context.MODE_PRIVATE)
            val client = OkHttpClient()
            val request = Request.Builder().url(MainActivity.FEED_URL).build()
            val body = client.newCall(request).execute().body?.string() ?: "[]"
            val arr = JSONArray(body)
            val editor = prefs.edit()
            var newCount = 0

            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val key = o.getString("game") + ":" + o.getString("code")
                if (!prefs.getBoolean(key, false)) {
                    editor.putBoolean(key, true)
                    newCount++
                }
            }
            editor.apply()

            if (newCount > 0) {
                val nm = applicationContext
                    .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                nm.notify(
                    1001,
                    NotificationCompat.Builder(applicationContext, "codes")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("🔥 Новые промокоды")
                        .setContentText("Найдено новых кодов: $newCount")
                        .setAutoCancel(true)
                        .build()
                )
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
