package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data Class sederhana untuk list anak
data class Anak(
    val nama: String,
    val status: String,
    val tb: Int,
    val bb: Int,
    val umur: Int,
    val tanggal: String
)

@Composable
fun DataAnakScreen(onNavigateBack: () -> Unit = {}, onNavigateToDetail: () -> Unit = {}) {
    // Background utama layar
    val backgroundColor = Color(0xFF121212) 
    
    // State untuk Search Bar
    var searchQuery by remember { mutableStateOf("") }

    // Dummy data untuk List
    val listAnak = List(5) {
        Anak("Michael Kwok", "Gizi Kurang", 100, 40, 36, "Apr 2025")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 1. Header Hijau
        DataAnakHeader(onBack = onNavigateBack)

        // 2. Search Bar
        SearchBarSection(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        // 3. List Data Anak (Bisa di-scroll)
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(listAnak.size) { index ->
                AnakListItem(
                    anak = listAnak[index],
                    onClick = onNavigateToDetail
                )
            }
        }

        // 4. Tombol Tambah Anak (Nempel di bawah)
        TambahAnakButton()
    }
}

@Composable
fun DataAnakHeader(onBack: () -> Unit) {
    val headerColor = Color(0xFF2E9E7B) // Hijau Tosca
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerColor)
            .padding(top = 40.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Text(
                text = "MyPosyandu",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.width(24.dp)) // To balance the back icon
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Data Anak",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Text(
            text = "5 Anak Terdaftar",
            color = Color(0xCCFFFFFF), // Light transparent white
            fontSize = 14.sp
        )
    }
}

@Composable
fun SearchBarSection(query: String, onQueryChange: (String) -> Unit) {
    val searchBoxBg = Color(0xFF2A2A2A)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(searchBoxBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(text = "Cari nama balita..", color = Color.Gray, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun AnakListItem(anak: Anak, onClick: () -> Unit) {
    val avatarColor = Color(0xFF98E6C8) // Mint
    val dividerColor = Color(0xFF333333)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Lingkaran
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarColor)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info Anak (Tengah)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = anak.nama, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = anak.status, color = Color.LightGray, fontSize = 12.sp)
                Text(
                    text = "TB: ${anak.tb}  BB: ${anak.bb}  Umur: ${anak.umur} Bulan",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            // Tanggal (Kanan)
            Text(
                text = anak.tanggal,
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Bottom)
            )
        }
        Divider(color = dividerColor, thickness = 1.dp)
    }
}

@Composable
fun TambahAnakButton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color.White, RoundedCornerShape(8.dp))
            .clickable { /* Aksi tambah anak */ }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Tambah",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Tambah Anak Baru", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
