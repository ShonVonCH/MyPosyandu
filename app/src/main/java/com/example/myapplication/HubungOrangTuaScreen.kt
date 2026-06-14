package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HOCardBg           = Color(0xFF2A2A2A)
private val HOCardBorder       = Color(0xFF444444)
private val HOOptionBorder     = Color(0xFF555555)
private val HOSearchBg         = Color(0xFF1A1A1A)
private val HOAvatarGrey       = Color(0xFF6B6B6B)
private val HOConfirmCardBg    = Color(0xFFB8EDD8)
private val HOConfirmInfoBoxBg = Color(0xFF8FDDBC)
private val HOConfirmTextDark  = Color(0xFF1E6B4E)
private val HORadioActive      = Color(0xFF2E9B6E)
private val HOSubtext          = Color(0xFFAAAAAA)
private val HOPageLabel        = Color(0xFF666666)

@Composable
fun HubungOrangTuaScreen(
    viewModel     : FormDataViewModel = FormDataViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateNext: () -> Unit = {}
) {
    val context = LocalContext.current

    // ── Muat daftar ortu dari SQLite setiap kali screen ini dibuka ──
    var akunListFromDb by remember { mutableStateOf<List<OrangTuaAccount>>(emptyList()) }
    LaunchedEffect(Unit) {
        val repo = OrangTuaRepository(context)
        val summaries = repo.getAllOrtu()
        // Sync ke ViewModel agar konsisten
        viewModel.syncAkunOrangTuaFromDb(summaries)
        akunListFromDb = summaries.map { ortu ->
            OrangTuaAccount(
                nama       = ortu.namaOrtu,
                username   = ortu.usernameOrtu,
                noHp       = ortu.noHpOrtu,
                password   = ortu.passOrtu,
                jumlahAnak = ortu.jumlahAnak
            )
        }
    }

    var selectedMethod  by remember { mutableStateOf("existing") }
    var searchQuery     by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf<OrangTuaAccount?>(null) }

    var namaOrtu by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var noHp     by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var errorNama by remember { mutableStateOf(false) }
    var errorUser by remember { mutableStateOf(false) }
    var errorHp   by remember { mutableStateOf(false) }
    var errorPass by remember { mutableStateOf(false) }

    val showLanjut = if (selectedMethod == "existing") selectedAccount != null
    else true // Selalu tampil agar bisa divalidasi saat diklik

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text     = "Hubungkan Ke Ortu",
            color    = HOPageLabel,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )

        HubungHeader(onNavigateBack = onNavigateBack)

        Spacer(modifier = Modifier.height(16.dp))

        PilihCaraCard(
            selectedMethod = selectedMethod,
            onMethodSelect = {
                selectedMethod  = it
                selectedAccount = null
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedMethod == "existing") {
            CariAkunCard(
                // Pakai data dari SQLite (bukan in-memory saja)
                akunList        = akunListFromDb,
                searchQuery     = searchQuery,
                onSearchChange  = { searchQuery = it },
                selectedAccount = selectedAccount,
                onSelectAccount = { acc ->
                    selectedAccount = acc
                    viewModel.formOrangTua = FormOrangTuaData(
                        nama     = acc.nama,
                        username = acc.username,
                        noHp     = acc.noHp,
                        password = acc.password
                    )
                },
                onClearAccount  = { selectedAccount = null },
                modifier        = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            FormAkunBaruCard(
                namaOrtu      = namaOrtu,
                onNamaChange  = { namaOrtu = it; errorNama = false },
                username      = username,
                onUserChange  = { username = it; errorUser = false },
                noHp          = noHp,
                onHpChange    = { noHp = it; errorHp = false },
                password      = password,
                onPassChange  = { password = it; errorPass = false },
                errorNama     = errorNama,
                errorUser     = errorUser,
                errorHp       = errorHp,
                errorPass     = errorPass,
                modifier      = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (showLanjut) {
            Spacer(modifier = Modifier.height(16.dp))
            LanjutButton(
                onClick = {
                    if (selectedMethod == "existing") {
                        if (selectedAccount != null) {
                            onNavigateNext()
                        }
                    } else {
                        var hasError = false
                        if (namaOrtu.isBlank()) { errorNama = true; hasError = true }
                        if (username.isBlank()) { errorUser = true; hasError = true }
                        if (noHp.isBlank()) { errorHp = true; hasError = true }
                        if (password.isBlank()) { errorPass = true; hasError = true }

                        if (!hasError) {
                            // Simpan data ortu baru ke ViewModel (belum ke DB — DB diisi saat Simpan & Daftarkan)
                            viewModel.tambahAkunOrangTua(namaOrtu, username, noHp, password)
                            onNavigateNext()
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ── Header ──────────────────────────────────────────────────────────────────

@Composable
fun HubungHeader(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, TextWhite, RoundedCornerShape(8.dp))
                    .clickable(onClick = onNavigateBack)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali",
                    tint = TextWhite, modifier = Modifier.size(16.dp))
            }
            Text(
                text = "Hubungkan Ke Akun Orang Tua",
                color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── PilihCaraCard ────────────────────────────────────────────────────────────

@Composable
fun PilihCaraCard(
    selectedMethod: String,
    onMethodSelect: (String) -> Unit,
    modifier      : Modifier = Modifier
) {
    HOCardContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pilih cara hubungkan", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            HOMethodOptionItem(
                label      = "Akun sudah ada",
                subLabel   = "Cari akun orang tua yang sudah terdaftar",
                isSelected = selectedMethod == "existing",
                onClick    = { onMethodSelect("existing") }
            )
            HOMethodOptionItem(
                label      = "Buat akun baru",
                subLabel   = "Orang tua belum punya akun",
                isSelected = selectedMethod == "new",
                onClick    = { onMethodSelect("new") }
            )
        }
    }
}

@Composable
private fun HOMethodOptionItem(
    label: String, subLabel: String, isSelected: Boolean, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (isSelected) HORadioActive else HOOptionBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp).clip(CircleShape)
                .background(if (isSelected) HORadioActive else Color.Transparent)
                .border(2.dp, if (isSelected) HORadioActive else HOOptionBorder, CircleShape)
        ) {
            if (isSelected) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                    .background(TextWhite).align(Alignment.Center))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label,    color = TextWhite,  fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subLabel, color = HOSubtext,  fontSize = 11.sp)
        }
    }
}

// ── CariAkunCard ─────────────────────────────────────────────────────────────

@Composable
fun CariAkunCard(
    akunList       : List<OrangTuaAccount>,
    searchQuery    : String,
    onSearchChange : (String) -> Unit,
    selectedAccount: OrangTuaAccount?,
    onSelectAccount: (OrangTuaAccount) -> Unit,
    onClearAccount : () -> Unit,
    modifier       : Modifier = Modifier
) {
    val filteredList = remember(searchQuery, akunList.size) {
        if (searchQuery.isBlank()) akunList
        else akunList.filter {
            it.nama.contains(searchQuery, ignoreCase = true) ||
                    it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    HOCardContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Cari akun orang tua", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            // Search field
            BasicTextField(
                value = searchQuery, onValueChange = onSearchChange,
                singleLine = true, cursorBrush = SolidColor(AccentGreen),
                textStyle = TextStyle(color = TextWhite, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(HOSearchBg).padding(horizontal = 12.dp, vertical = 11.dp),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Search, "Cari", tint = HOSubtext,
                            modifier = Modifier.size(18.dp))
                        Box {
                            if (searchQuery.isEmpty())
                                Text("Nama, Username, Atau no. HP.", color = HOSubtext, fontSize = 13.sp)
                            inner()
                        }
                    }
                }
            )

            if (selectedAccount == null) {
                if (akunList.isEmpty()) {
                    // Belum ada akun ortu di database
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada akun orang tua terdaftar.",
                            color = HOSubtext, fontSize = 13.sp, textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column {
                        filteredList.forEach { account ->
                            HOAkunListItem(account = account, onClick = { onSelectAccount(account) })
                            if (account != filteredList.last()) {
                                Divider(color = HOCardBorder, thickness = 0.8.dp)
                            }
                        }
                    }
                }
            } else {
                HOAkunKonfirmasiCard(account = selectedAccount, onGantiClick = onClearAccount)
            }
        }
    }
}

@Composable
private fun HOAkunListItem(account: OrangTuaAccount, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(HOAvatarGrey))
        Column(modifier = Modifier.weight(1f)) {
            Text(account.nama, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${account.username} · ${account.jumlahAnak} anak terdaftar",
                color = HOSubtext, fontSize = 12.sp)
        }
        Icon(Icons.Filled.ChevronRight, "Pilih", tint = HOSubtext, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun HOAkunKonfirmasiCard(account: OrangTuaAccount, onGantiClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(HOConfirmCardBg).padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CheckCircle, "Ditemukan",
                        tint = HOConfirmTextDark, modifier = Modifier.size(22.dp))
                    Text("Akun ditemukan", color = HOConfirmTextDark,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("Ganti", color = HOConfirmTextDark, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onGantiClick))
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(CircleMint))
                Column {
                    Text(account.nama, color = HOConfirmTextDark,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${account.username} · ${account.jumlahAnak} anak terdaftar",
                        color = HOConfirmTextDark.copy(alpha = 0.75f), fontSize = 11.sp)
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(HOConfirmInfoBoxBg).padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Setelah didaftarkan, anak ini akan otomatis muncul di halaman orang tua saat login.",
                    color = HOConfirmTextDark, fontSize = 11.sp, textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── LanjutButton ─────────────────────────────────────────────────────────────

@Composable
fun LanjutButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(BackgroundDark).border(1.dp, TextWhite, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Lanjut - Konfirmasi →", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// ── HOCardContainer ───────────────────────────────────────────────────────────

@Composable
private fun HOCardContainer(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(HOCardBg).border(1.dp, HOCardBorder, RoundedCornerShape(16.dp)).padding(14.dp)
    ) { Column(content = content) }
}

// ── FormAkunBaruCard ──────────────────────────────────────────────────────────

@Composable
fun FormAkunBaruCard(
    namaOrtu    : String, onNamaChange: (String) -> Unit,
    username    : String, onUserChange: (String) -> Unit,
    noHp        : String, onHpChange  : (String) -> Unit,
    password    : String, onPassChange: (String) -> Unit,
    errorNama   : Boolean = false,
    errorUser   : Boolean = false,
    errorHp     : Boolean = false,
    errorPass   : Boolean = false,
    modifier    : Modifier = Modifier
) {
    val fieldBg     = Color(0xFF3A3A3A)
    val fieldBorder = Color(0xFF555555)
    val labelColor  = Color(0xFFAAAAAA)
    val infoBoxBg   = Color(0xFFBBCFEF)
    val infoBoxText = Color(0xFF1A3A6E)

    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2A2A2A)).border(1.dp, Color(0xFF444444), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Buat akun orang tua baru", color = TextWhite,
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
            HOInputField("Nama Orang Tua",     namaOrtu, onNamaChange, "Nama Orang Tua",         fieldBg, fieldBorder, labelColor, isError = errorNama)
            HOInputField("Username Orang Tua", username, onUserChange, "Untuk login orang tua",  fieldBg, fieldBorder, labelColor, isError = errorUser)
            HOInputField("No. HP",             noHp,     onHpChange,   "No. Hp Orang Tua",       fieldBg, fieldBorder, labelColor, KeyboardType.Phone, isError = errorHp)
            HOInputField("Password Sementara", password, onPassChange, "Min. 6 karakter",        fieldBg, fieldBorder, labelColor, KeyboardType.Password, true, isError = errorPass)
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(infoBoxBg).padding(12.dp)
            ) {
                Text(
                    "Bagikan username & password ini ke orang tua anak. Mereka bisa ganti password setelah login pertama.",
                    color = infoBoxText, fontSize = 13.sp, lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun HOInputField(
    label        : String, value: String, onValueChange: (String) -> Unit,
    placeholder  : String, fieldBg: Color, fieldBorder: Color, labelColor: Color,
    keyboardType : KeyboardType = KeyboardType.Text, isPassword: Boolean = false,
    isError      : Boolean = false,
    modifier     : Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = labelColor, fontSize = 12.sp)
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            placeholder = { Text(placeholder, color = Color(0xFF6B6B6B), fontSize = 14.sp) },
            singleLine = true,
            isError = isError,
            textStyle = TextStyle(color = TextWhite, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor            = TextWhite,
                cursorColor          = AccentGreen,
                focusedBorderColor   = AccentGreen,
                unfocusedBorderColor = if (isError) Color.Red else fieldBorder,
                backgroundColor      = fieldBg,
                placeholderColor     = Color(0xFF6B6B6B),
                errorBorderColor     = Color.Red
            )
        )
        if (isError) {
            Text("Harus diisi", color = Color.Red, fontSize = 11.sp)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun HubungOrangTuaScreenPreview() { HubungOrangTuaScreen() }