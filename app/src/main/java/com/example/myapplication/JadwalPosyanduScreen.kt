package com.example.myapplication

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JadwalPosyanduScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { JadwalRepository(context) }
    val posyanduId = remember { repository.getCurrentPosyanduId() ?: "" }
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    var jadwalList by remember { mutableStateOf(repository.getJadwalByPosyandu(posyanduId)) }
    var showDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Jadwal Posyandu", color = Color.White) },
                backgroundColor = HeaderGreen,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                elevation = 0.dp
            )
        },
        backgroundColor = BackgroundDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!isSubmitting) showDialog = true },
                backgroundColor = AccentGreen,
                modifier = Modifier.padding(bottom = 16.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Jadwal", tint = Color.White)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (jadwalList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada jadwal", color = TextGrey)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(jadwalList) { jadwal ->
                        JadwalItem(jadwal)
                    }
                }
            }
        }

        if (showDialog) {
            AddJadwalDialog(
                isSubmitting = isSubmitting,
                onDismiss = { if (!isSubmitting) showDialog = false },
                onConfirm = { date, start, end, location ->
                    val newJadwal = JadwalData(
                        id         = 0,  // auto-increment, tidak dipakai saat insert
                        posyanduId = posyanduId,
                        tanggal    = date,
                        jamMulai   = start,
                        jamSelesai = end,
                        lokasi     = location,
                        status     = "Terjadwal"
                    )
                    scope.launch {
                        isSubmitting = true
                        val result = repository.addJadwal(newJadwal)
                        isSubmitting = false
                        if (result.isSuccess) {
                            jadwalList = repository.getJadwalByPosyandu(posyanduId)
                            showDialog = false
                        } else {
                            scaffoldState.snackbarHostState.showSnackbar(
                                message = "Gagal menyimpan jadwal. Coba lagi."
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun JadwalItem(jadwal: JadwalData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = SurfaceDark,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = formatDate(jadwal.tanggal),
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${jadwal.jamMulai} - ${jadwal.jamSelesai}",
                color = TextGreenLight,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = jadwal.lokasi,
                color = TextGrey,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AddJadwalDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (date: String, start: String, end: String, location: String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var tanggal by remember { mutableStateOf("") }
    var jamMulai by remember { mutableStateOf("") }
    var jamSelesai by remember { mutableStateOf("") }
    var namaPosyandu by remember { mutableStateOf("") }

    var errorTanggal by remember { mutableStateOf(false) }
    var errorMulai by remember { mutableStateOf(false) }
    var errorSelesai by remember { mutableStateOf(false) }
    var errorLokasi by remember { mutableStateOf(false) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            tanggal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            errorTanggal = false
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerMulai = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            jamMulai = String.format("%02d:%02d", hourOfDay, minute)
            errorMulai = false
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    val timePickerSelesai = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            jamSelesai = String.format("%02d:%02d", hourOfDay, minute)
            errorSelesai = false
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Tambah Jadwal Posyandu",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                FakeTextField(
                    label = "Tanggal",
                    value = tanggal,
                    placeholder = "Pilih Tanggal",
                    error = errorTanggal,
                    errorMessage = "Tanggal harus diisi",
                    onClick = { datePickerDialog.show() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                FakeTextField(
                    label = "Jam Mulai",
                    value = jamMulai,
                    placeholder = "Pilih Jam Mulai",
                    error = errorMulai,
                    errorMessage = "Jam mulai harus diisi",
                    onClick = { timePickerMulai.show() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                FakeTextField(
                    label = "Jam Selesai",
                    value = jamSelesai,
                    placeholder = "Pilih Jam Selesai",
                    error = errorSelesai,
                    errorMessage = "Jam selesai harus diisi",
                    onClick = { timePickerSelesai.show() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Text("Nama Posyandu / Lokasi", color = TextGrey, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = namaPosyandu,
                        onValueChange = {
                            namaPosyandu = it
                            errorLokasi = false
                        },
                        placeholder = { Text("Masukkan Nama Posyandu / Lokasi", color = TextGrey.copy(0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = TextWhite,
                            cursorColor = AccentGreen,
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = SurfaceDarkBorder,
                            backgroundColor = LaporanBg
                        )
                    )
                    if (errorLokasi) {
                        Text("Lokasi harus diisi", color = Color.Red, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
                ) {
                    Button(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Batal", color = Color.White)
                    }
                    Button(
                        onClick = {
                            var hasError = false
                            if (tanggal.isEmpty())      { errorTanggal = true; hasError = true }
                            if (jamMulai.isEmpty())     { errorMulai   = true; hasError = true }
                            if (jamSelesai.isEmpty())   { errorSelesai = true; hasError = true }
                            if (namaPosyandu.isEmpty()) { errorLokasi  = true; hasError = true }
                            if (!hasError) onConfirm(tanggal, jamMulai, jamSelesai, namaPosyandu)
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Selesai", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FakeTextField(
    label: String,
    value: String,
    placeholder: String,
    error: Boolean,
    errorMessage: String,
    onClick: () -> Unit
) {
    Column {
        Text(label, color = TextGrey, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, if (error) Color.Red else SurfaceDarkBorder, RoundedCornerShape(4.dp))
                .background(LaporanBg)
                .clickable { onClick() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = if (value.isEmpty()) placeholder else value,
                color = if (value.isEmpty()) TextGrey.copy(0.5f) else TextWhite,
                fontSize = 16.sp
            )
        }
        if (error) {
            Text(errorMessage, color = Color.Red, fontSize = 12.sp)
        }
    }
}

private fun formatDate(date: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outSdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        val d = sdf.parse(date)
        d?.let { outSdf.format(it) } ?: date
    } catch (e: Exception) {
        date
    }
}

private val LaporanBg = Color(0xFF1C1C1E)