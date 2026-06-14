package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

private val MdHeader = Color(0xFF1964A3)
private val MdBg     = Color(0xFF121212)
private val MdSurf   = Color(0xFF2A2A2A)
private val MdTextW  = Color(0xFFFFFFFF)
private val MdTextG  = Color(0xFF888888)

@Composable
fun MenuDetailScreen(
    menuId            : String = "",
    onNavigateBack    : () -> Unit = {},
    onNavigateToHome  : () -> Unit = {},
    onNavigateToTicket: () -> Unit = {},
    onNavigateToFood  : () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val repo    = remember { MenuRepository(context) }

    var menu      by remember { mutableStateOf<MenuSehat?>(null) }
    var katNama   by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(menuId) {
        scope.launch {
            menu = repo.getMenuDetail(menuId)
            menu?.let { m ->
                katNama = repo.getKategoriList().find { it.id == m.kategoriId }?.nama ?: ""
            }
            isLoading = false
        }
    }

    Scaffold(
        backgroundColor = MdBg,
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(MdHeader)
                    .statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onNavigateBack)
                ) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = MdTextW, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(menu?.judul ?: "Detail Resep", color = MdTextW, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Detail Resep", color = MdTextW.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        },
        bottomBar = {
            MenuBottomBar(
                currentTab     = "food",
                onHomeClick    = onNavigateToHome,
                onTicketClick  = onNavigateToTicket,
                onFoodClick    = onNavigateToFood,
                onProfileClick = onNavigateToProfile
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MdHeader)
            }
            return@Scaffold
        }

        val m = menu
        if (m == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Menu tidak ditemukan", color = MdTextG)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card gambar + judul
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp)).background(MdSurf)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF1E2A3A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(usiaToEmoji(m.rangeUsia), fontSize = 64.sp)
                }
                Column(Modifier.padding(12.dp)) {
                    Text(m.judul, color = MdTextW, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    val sub = buildString {
                        if (katNama.isNotBlank()) append("Cocok $katNama")
                        if (m.durasiMenit > 0) { if (isNotEmpty()) append(" - "); append("${m.durasiMenit} Menit") }
                    }
                    if (sub.isNotBlank()) Text(sub, color = MdTextG, fontSize = 12.sp)
                }
            }

            // Bahan
            if (m.bahan.isNotBlank()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp)).background(MdSurf).padding(16.dp)
                ) {
                    Text("Bahan-bahan", color = MdTextW, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    m.bahan.split(",", "\n").filter { it.isNotBlank() }.forEach { bahan ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("• ", color = MdTextW, fontSize = 14.sp)
                            Text(bahan.trim(), color = MdTextW, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Cara membuat
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp)).background(MdSurf).padding(16.dp)
            ) {
                Text("Cara memasak", color = MdTextW, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (m.caraMembuat.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    m.caraMembuat.split(".", "\n").filter { it.isNotBlank() }
                        .forEachIndexed { idx, step ->
                            Row(Modifier.padding(vertical = 3.dp)) {
                                Text("${idx + 1}. ", color = MdTextG, fontSize = 14.sp)
                                Text(step.trim().trimEnd('.'), color = MdTextW, fontSize = 14.sp)
                            }
                        }
                }
            }

            // Kandungan gizi
            if (m.kandunganGizi.isNotBlank()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp)).background(MdSurf).padding(16.dp)
                ) {
                    Text("Kandungan Gizi", color = MdTextW, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(m.kandunganGizi, color = MdTextG, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}