package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val MsHeaderBlue   = Color(0xFF1964A3)
private val MsBg           = Color(0xFF121212)
private val MsSurface      = Color(0xFF2A2A2A)
private val MsTextWhite    = Color(0xFFFFFFFF)
private val MsTextGrey     = Color(0xFF888888)
private val MsKatBg        = Color(0xFF1A3A6E)
private val MsKatBorder    = Color(0xFF2D6FC4)
private val MsKatText      = Color(0xFF5B9BF0)

@Composable
fun MenuSehatScreen(
    onNavigateToHome       : () -> Unit = {},
    onNavigateToTicket     : () -> Unit = {},
    onNavigateToFood       : () -> Unit = {},
    onNavigateToProfile    : () -> Unit = {},
    onNavigateToLogout     : () -> Unit = {},
    onNavigateToKategori   : (String, String) -> Unit = { _, _ -> },
    onNavigateToDetail     : (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val repo    = remember { MenuRepository(context) }

    var kategoriList    by remember { mutableStateOf<List<MenuKategori>>(emptyList()) }
    var rekomendasiList by remember { mutableStateOf<List<MenuSehat>>(emptyList()) }
    var isLoading       by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            backgroundColor = MsSurface,
            title = {
                Text("Logout", color = MsTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text("Yakin ingin keluar dari akun?", color = MsTextGrey, fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    val db = DatabaseHelper(context).writableDatabase
                    db.execSQL("DELETE FROM ${DatabaseHelper.TABLE_USERS}")
                    db.close()
                    onNavigateToLogout()
                }) {
                    Text("Logout", color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = MsTextGrey)
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        scope.launch {
            repo.syncMenuData()
            kategoriList    = repo.getKategoriList()
            rekomendasiList = repo.getMenuRekomendasi(5)
            isLoading       = false
        }
    }

    Scaffold(
        backgroundColor = MsBg,
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth().background(MsHeaderBlue).statusBarsPadding().padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("MyPosyandu", color = MsTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        bottomBar = {
            MenuBottomBar(
                currentTab     = "food",
                onHomeClick    = onNavigateToHome,
                onTicketClick  = onNavigateToTicket,
                onFoodClick    = {}, // Sudah di halaman ini
                onProfileClick = { showLogoutDialog = true }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MsHeaderBlue)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)) {
                    Text("Menu Sehat", color = MsTextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Resep", color = MsTextGrey, fontSize = 13.sp)
                }
            }

            // Grid kategori — 4 tombol fixed
            item {
                val fixedKategori = listOf(
                    Triple("6-12 Bulan",  "🍼", listOf("6-8", "9-11", "6-12", "bulan")),
                    Triple("1-3 Tahun",   "🍌", listOf("12-24", "1-3", "1 tahun", "2 tahun", "3 tahun")),
                    Triple("4-5 Tahun",   "🐟", listOf("4-5", "4 tahun", "5 tahun", "2-5")),
                    Triple(">5 Tahun",    "🥗", listOf("semua", "all", "5 tahun", "> 5", ">5"))
                )

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp)).background(MsSurface).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fixedKategori.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (label, emoji, keywords) ->
                                // Cari kategori dari DB yang range_usianya cocok keyword
                                val matchedKat = kategoriList.firstOrNull { kat ->
                                    val r = kat.rangeUsia.lowercase()
                                    keywords.any { kw -> r.contains(kw.lowercase()) }
                                }
                                KategoriCard(
                                    label    = label,
                                    emoji    = emoji,
                                    modifier = Modifier.weight(1f),
                                    onClick  = {
                                        if (matchedKat != null) {
                                            onNavigateToKategori(matchedKat.id, label)
                                        } else {
                                            // fallback: kirim keyword sebagai filter
                                            onNavigateToKategori("__filter__$label", label)
                                        }
                                    }
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text("Menu Rekomendasi", color = MsTextWhite, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            if (rekomendasiList.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada menu tersedia", color = MsTextGrey, fontSize = 14.sp)
                    }
                }
            } else {
                items(rekomendasiList) { menu ->
                    MenuCardItem(menu = menu, onClick = { onNavigateToDetail(menu.id) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun KategoriCard(label: String, emoji: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MsKatBg)
            .border(1.dp, MsKatBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 30.sp)
        Spacer(Modifier.height(8.dp))
        Text(label, color = MsKatText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Card menu — dipakai juga di MenuKategoriScreen ────────────────────────
@Composable
fun MenuCardItem(menu: MenuSehat, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF2A2A2A))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(110.dp).background(Color(0xFF1E2A3A)),
            contentAlignment = Alignment.Center
        ) {
            Text(usiaToEmoji(menu.rangeUsia), fontSize = 48.sp)
        }
        Column(Modifier.padding(12.dp)) {
            Text(menu.judul, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            val sub = buildString {
                if (menu.rangeUsia.isNotBlank()) append("Cocok ${menu.rangeUsia}")
                if (menu.durasiMenit > 0) { if (isNotEmpty()) append(" - "); append("${menu.durasiMenit} Menit") }
            }
            if (sub.isNotBlank()) Text(sub, color = Color(0xFF888888), fontSize = 12.sp)
        }
    }
}

fun usiaToEmoji(range: String): String {
    val r = range.lowercase()
    return when {
        r.contains("bulan")  -> "🍼"
        r.contains("camilan") || r.contains("semua") -> "🍎"
        r.contains("2-5") || r.contains("makanan")   -> "🥗"
        r.contains("1") || r.contains("12-24")       -> "🥣"
        else -> "🍽️"
    }
}

// ── Bottom Nav ────────────────────────────────────────────────────────────
@Composable
fun MenuBottomBar(
    currentTab    : String = "",
    onHomeClick   : () -> Unit,
    onTicketClick : () -> Unit,
    onFoodClick   : () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1C1E)).navigationBarsPadding().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        data class NavEntry(val icon: ImageVector, val label: String, val tab: String, val action: () -> Unit)
        listOf(
            NavEntry(Icons.Outlined.Home,              "Home",    "home",    onHomeClick),
            NavEntry(Icons.Outlined.ConfirmationNumber,"Antrian", "ticket",  onTicketClick),
            NavEntry(Icons.Outlined.Restaurant,        "Menu",    "food",    onFoodClick),
            NavEntry(Icons.Outlined.PowerSettingsNew,  "Logout",  "profile", onProfileClick)
        ).forEach { entry ->
            val tint = if (currentTab == entry.tab) MsHeaderBlue else Color.White.copy(alpha = 0.45f)
            Column(
                modifier = Modifier.clickable(onClick = entry.action).padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(entry.icon, contentDescription = entry.label, tint = tint, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(2.dp))
                Text(entry.label, color = tint, fontSize = 10.sp,
                    fontWeight = if (currentTab == entry.tab) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}