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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
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

private val DashHeaderBlue        = Color(0xFF1964A3)
private val DashBackgroundDark    = Color(0xFF121212)
private val DashSurfaceDark       = Color(0xFF2A2A2A)
private val DashSurfaceDarkBorder = Color(0xFF444444)
private val DashTextWhite         = Color(0xFFFFFFFF)
private val DashTextGrey          = Color(0xFF888888)

@Composable
fun MenuSehatScreen(
    onNavigateToAgeGroup: (String) -> Unit, // Navigasi ke List berdasarkan Umur
    onNavigateToDetail: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToTicket: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { MenuRepository(context) }
    var recommendedList by remember { mutableStateOf<List<MenuSehat>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        repository.syncMenuData()
        recommendedList = repository.getRecommendedMenu()
        isLoading = false
    }

    Scaffold(
        backgroundColor = DashBackgroundDark,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(DashHeaderBlue).padding(16.dp)) {
                Text(text = "MyPosyandu", color = Color.White, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Menu Sehat", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Resep", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        },
        bottomBar = {
            BottomNavBarCommon(activeTab = "food", onHomeClick = onNavigateToHome, onTicketClick = onNavigateToTicket, onFoodClick = {}, onProfileClick = onNavigateToProfile)
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                // ✅ Kotak Tetap Sesuai Gambar (4 Pilihan Umur)
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DashSurfaceDark).padding(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AgeBox(Modifier.weight(1f), "6-12 Bulan", Icons.Outlined.ChildCare, onNavigateToAgeGroup)
                            AgeBox(Modifier.weight(1f), "1-3 Tahun", Icons.Outlined.ChildFriendly, onNavigateToAgeGroup)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AgeBox(Modifier.weight(1f), "4-5 Tahun", Icons.Outlined.SetMeal, onNavigateToAgeGroup)
                            AgeBox(Modifier.weight(1f), "> 5 Tahun", Icons.Outlined.SoupKitchen, onNavigateToAgeGroup)
                        }
                    }
                }
            }

            item { Text(text = "Menu Rekomendasi", color = DashTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold) }

            if (isLoading && recommendedList.isEmpty()) {
                item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DashHeaderBlue) } }
            } else {
                items(recommendedList) { menu ->
                    MenuListItem(menu = menu, onClick = { onNavigateToDetail(menu.id) })
                }
            }
        }
    }
}

@Composable
private fun AgeBox(modifier: Modifier, title: String, icon: ImageVector, onClick: (String) -> Unit) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFBBD9F8)).clickable { onClick(title) }.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = DashHeaderBlue, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = DashHeaderBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Screen 2: Menu List ──────────────────────────────────────────────────────

@Composable
fun MenuListScreen(
    ageGroup: String, // Menggunakan AgeGroup bukan KategoriID
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToTicket: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { MenuRepository(context) }
    var menuList by remember { mutableStateOf<List<MenuSehat>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(ageGroup) {
        isLoading = true
        menuList = repository.getMenuListByAgeGroup(ageGroup)
        isLoading = false
    }

    Scaffold(
        backgroundColor = DashBackgroundDark,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(DashHeaderBlue).padding(16.dp)) {
                Button(onClick = onNavigateBack, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent, contentColor = Color.White), modifier = Modifier.border(1.dp, Color.White, RoundedCornerShape(4.dp)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kembali", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = ageGroup, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Resep", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        },
        bottomBar = {
            BottomNavBarCommon(activeTab = "food", onHomeClick = onNavigateToHome, onTicketClick = onNavigateToTicket, onFoodClick = {}, onProfileClick = onNavigateToProfile)
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DashHeaderBlue) }
        } else if (menuList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Belum ada resep untuk usia ini.", color = DashTextGrey) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(menuList) { menu -> MenuListItem(menu = menu, onClick = { onNavigateToDetail(menu.id) }) }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Screen 3: Menu Detail ─────────────────────────────────────────────────────

@Composable
fun MenuDetailScreen(
    menuId: String,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToTicket: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { MenuRepository(context) }
    var menu by remember { mutableStateOf<MenuSehat?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(menuId) {
        isLoading = true
        menu = repository.getMenuDetail(menuId)
        isLoading = false
    }

    Scaffold(
        backgroundColor = DashBackgroundDark,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(DashHeaderBlue).padding(16.dp)) {
                Button(onClick = onNavigateBack, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent, contentColor = Color.White), modifier = Modifier.border(1.dp, Color.White, RoundedCornerShape(4.dp)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kembali", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = menu?.judul ?: "Detail Menu", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Detail Resep", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        },
        bottomBar = {
            BottomNavBarCommon(activeTab = "food", onHomeClick = onNavigateToHome, onTicketClick = onNavigateToTicket, onFoodClick = {}, onProfileClick = onNavigateToProfile)
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DashHeaderBlue) }
        } else if (menu != null) {
            val m = menu!!
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { MenuListItem(menu = m, onClick = {}) }
                item {
                    SectionCard(title = "Bahan-bahan") {
                        val bahanList = m.bahan.split(",").filter { it.isNotBlank() }
                        Column {
                            bahanList.forEach { line ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("•", color = Color.White, modifier = Modifier.padding(end = 8.dp))
                                    Text(line.trim(), color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                item { SectionCard(title = "Cara memasak") { Text(m.caraMembuat, color = Color.White, fontSize = 14.sp) } }
                if (m.kandunganGizi.isNotBlank() && m.kandunganGizi != "null") {
                    item { SectionCard(title = "Kandungan Gizi") {
                        val cleanGizi = m.kandunganGizi.replace("[", "").replace("]", "").replace("\"", "")
                        Text(cleanGizi, color = Color.White, fontSize = 14.sp)
                    } }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MenuListItem(menu: MenuSehat, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DashSurfaceDark).border(1.dp, DashSurfaceDarkBorder, RoundedCornerShape(12.dp)).clickable(onClick = onClick)) {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color(0xFFD9D9D9)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.SoupKitchen, null, tint = DashHeaderBlue, modifier = Modifier.size(48.dp)) }
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF444444)).padding(12.dp)) {
            Text(menu.judul, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = "Cocok ${menu.rangeUsia} - ${menu.durasiMenit} Menit", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DashSurfaceDark).border(1.dp, DashSurfaceDarkBorder, RoundedCornerShape(12.dp)).padding(16.dp)) {
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun BottomNavBarCommon(activeTab: String, onHomeClick: () -> Unit, onTicketClick: () -> Unit, onFoodClick: () -> Unit, onProfileClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(DashSurfaceDark).navigationBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            NavIcon(Icons.Outlined.Home, isActive = activeTab == "home", onClick = onHomeClick)
            NavIcon(Icons.Outlined.ConfirmationNumber, isActive = activeTab == "ticket", onClick = onTicketClick)
            NavIcon(Icons.Outlined.Restaurant, isActive = activeTab == "food", onClick = onFoodClick)
            NavIcon(Icons.Outlined.Person, isActive = activeTab == "profile", onClick = onProfileClick)
        }
    }
}

@Composable
private fun NavIcon(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    Icon(imageVector = icon, contentDescription = null, tint = if (isActive) DashHeaderBlue else DashTextWhite.copy(alpha = 0.5f), modifier = Modifier.size(32.dp).clickable(onClick = onClick))
}
