package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val MkHeader  = Color(0xFF1964A3)
private val MkBg      = Color(0xFF121212)
private val MkTextW   = Color(0xFFFFFFFF)
private val MkTextG   = Color(0xFF888888)

@Composable
fun MenuKategoriScreen(
    kategoriId        : String = "",
    kategoriNama      : String = "",
    onNavigateBack    : () -> Unit = {},
    onNavigateToHome  : () -> Unit = {},
    onNavigateToTicket: () -> Unit = {},
    onNavigateToFood  : () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToLogout : () -> Unit = {},
    onNavigateToDetail : (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val repo    = remember { MenuRepository(context) }

    var allKategori   by remember { mutableStateOf<List<MenuKategori>>(emptyList()) }
    var selectedKatId by remember { mutableStateOf(kategoriId) }
    var menuList      by remember { mutableStateOf<List<MenuSehat>>(emptyList()) }
    var isLoading     by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            backgroundColor = Color(0xFF2A2A2A),
            title = {
                Text("Logout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text("Yakin ingin keluar dari akun?", color = Color(0xFF888888), fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    try {
                        val db = DatabaseHelper(context).writableDatabase
                        db.execSQL("DELETE FROM ${DatabaseHelper.TABLE_USERS}")
                        db.close()
                    } catch (e: Exception) { }
                    onNavigateToLogout()
                }) {
                    Text("Logout", color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = Color(0xFF888888))
                }
            }
        )
    }

    fun loadMenu(katId: String) {
        scope.launch {
            isLoading = true
            menuList = if (katId.startsWith("__filter__")) {
                // Filter berdasarkan range_usia label (fallback kalau tidak ada match di DB)
                val label = katId.removePrefix("__filter__")
                val keywords = when {
                    label.contains("6-12")  -> listOf("6-8", "9-11", "6-12", "bulan")
                    label.contains("1-3")   -> listOf("12-24", "1-3", "1 tahun", "2 tahun", "3 tahun")
                    label.contains("4-5")   -> listOf("4-5", "4 tahun", "5 tahun", "2-5")
                    label.contains(">5")    -> listOf("semua", "all", "> 5", ">5")
                    else                    -> listOf(label.lowercase())
                }
                repo.getMenuByRangeUsia(keywords)
            } else {
                repo.getMenuByKategori(katId)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        allKategori = repo.getKategoriList()
        loadMenu(kategoriId)
    }

    Scaffold(
        backgroundColor = MkBg,
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(MkHeader)
                    .statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onNavigateBack)
                ) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = MkTextW, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = allKategori.find { it.id == selectedKatId }?.nama ?: kategoriNama,
                    color = MkTextW, fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                Text("Resep", color = MkTextW.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        },
        bottomBar = {
            MenuBottomBar(
                currentTab     = "food",
                onHomeClick    = onNavigateToHome,
                onTicketClick  = onNavigateToTicket,
                onFoodClick    = onNavigateToFood,
                onProfileClick = { showLogoutDialog = true }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Tab kategori
            if (allKategori.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().background(MkHeader)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allKategori) { kat ->
                        val active = kat.id == selectedKatId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (active) MkTextW else Color(0xFF2A2A2A))
                                .border(1.dp, if (active) MkTextW else MkTextG, RoundedCornerShape(20.dp))
                                .clickable { selectedKatId = kat.id; loadMenu(kat.id) }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = kat.nama,
                                color = if (active) MkHeader else MkTextG,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // List menu
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MkHeader)
                }
                menuList.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada menu untuk kategori ini", color = MkTextG, fontSize = 14.sp)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(menuList) { menu ->
                        MenuCardItem(menu = menu, onClick = { onNavigateToDetail(menu.id) })
                    }
                }
            }
        }
    }
}