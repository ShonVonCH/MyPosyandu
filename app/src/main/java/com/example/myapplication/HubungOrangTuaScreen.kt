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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardBg           = Color(0xFF2A2A2A)
private val CardBorder       = Color(0xFF444444)
private val OptionBorder     = Color(0xFF555555)
private val SearchBg         = Color(0xFF1A1A1A)
private val AvatarGrey       = Color(0xFF6B6B6B)
private val ConfirmCardBg    = Color(0xFFB8EDD8)
private val ConfirmInfoBoxBg = Color(0xFF8FDDBC)
private val ConfirmTextDark  = Color(0xFF1E6B4E)
private val RadioActiveColor = Color(0xFF2E9B6E)
private val SubtextColor     = Color(0xFFAAAAAA)
private val PageLabelColor   = Color(0xFF666666)

@Composable
fun HubungOrangTuaScreen(
    viewModel     : FormDataViewModel = FormDataViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateNext: () -> Unit = {}
) {
    var selectedMethod  by remember { mutableStateOf("existing") }
    var searchQuery     by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf<OrangTuaAccount?>(null) }

    var namaOrtu by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var noHp     by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val formBaruValid = namaOrtu.isNotBlank() && username.isNotBlank() &&
            noHp.isNotBlank() && password.isNotBlank()

    val showLanjut = if (selectedMethod == "existing") selectedAccount != null
    else formBaruValid

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
            color    = PageLabelColor,
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
                akunList        = viewModel.akunOrangTuaList,
                searchQuery     = searchQuery,
                onSearchChange  = { searchQuery = it },
                selectedAccount = selectedAccount,
                onSelectAccount = {
                    selectedAccount = it
                    viewModel.formOrangTua = FormOrangTuaData(
                        nama     = it.nama,
                        username = it.username
                    )
                },
                onClearAccount  = { selectedAccount = null },
                modifier        = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            FormAkunBaruCard(
                namaOrtu      = namaOrtu,
                onNamaChange  = { namaOrtu = it },
                username      = username,
                onUserChange  = { username = it },
                noHp          = noHp,
                onHpChange    = { noHp = it },
                password      = password,
                onPassChange  = { password = it },
                modifier      = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (showLanjut) {
            Spacer(modifier = Modifier.height(16.dp))
            LanjutButton(
                onClick = {
                    if (selectedMethod == "new") {
                        viewModel.tambahAkunOrangTua(namaOrtu, username, noHp, password)
                    }
                    onNavigateNext()
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun HubungHeader(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector        = Icons.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint               = TextWhite,
                    modifier           = Modifier.size(16.dp)
                )
                Text(text = "Kembali", color = TextWhite, fontSize = 13.sp)
            }

            Text(
                text       = "Hubungkan Ke Akun Orang Tua",
                color      = TextWhite,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PilihCaraCard(
    selectedMethod: String,
    onMethodSelect: (String) -> Unit,
    modifier      : Modifier = Modifier
) {
    CardContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text       = "Pilih cara hubungkan",
                color      = TextWhite,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )
            MethodOptionItem(
                label      = "Akun sudah ada",
                subLabel   = "Cari akun orang tua yang sudah terdaftar",
                isSelected = selectedMethod == "existing",
                onClick    = { onMethodSelect("existing") }
            )
            MethodOptionItem(
                label      = "Buat akun baru",
                subLabel   = "Orang tua belum punya akun",
                isSelected = selectedMethod == "new",
                onClick    = { onMethodSelect("new") }
            )
        }
    }
}

@Composable
private fun MethodOptionItem(
    label     : String,
    subLabel  : String,
    isSelected: Boolean,
    onClick   : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = if (isSelected) RadioActiveColor else OptionBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isSelected) RadioActiveColor else Color.Transparent)
                .border(2.dp, if (isSelected) RadioActiveColor else OptionBorder, CircleShape)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TextWhite)
                        .align(Alignment.Center)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = label,    color = TextWhite,    fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = subLabel, color = SubtextColor, fontSize = 11.sp)
        }
    }
}

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

    CardContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text       = "Cari akun orang tua",
                color      = TextWhite,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )

            BasicTextField(
                value         = searchQuery,
                onValueChange = onSearchChange,
                singleLine    = true,
                cursorBrush   = SolidColor(AccentGreen),
                textStyle     = TextStyle(color = TextWhite, fontSize = 13.sp),
                modifier      = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SearchBg)
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Search,
                            contentDescription = "Cari",
                            tint               = SubtextColor,
                            modifier           = Modifier.size(18.dp)
                        )
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text     = "Nama, Username, Atau no. HP.",
                                    color    = SubtextColor,
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )

            if (selectedAccount == null) {
                Column {
                    filteredList.forEach { account ->
                        AkunListItem(account = account, onClick = { onSelectAccount(account) })
                        if (account != filteredList.last()) {
                            Divider(color = CardBorder, thickness = 0.8.dp)
                        }
                    }
                }
            } else {
                AkunKonfirmasiCard(
                    account      = selectedAccount,
                    onGantiClick = onClearAccount
                )
            }
        }
    }
}

@Composable
private fun AkunListItem(account: OrangTuaAccount, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(AvatarGrey)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = account.nama,     color = TextWhite,    fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = "${account.username} · ${account.jumlahAnak} anak terdaftar", color = SubtextColor, fontSize = 12.sp)
        }
        Icon(
            imageVector        = Icons.Filled.ChevronRight,
            contentDescription = "Pilih",
            tint               = SubtextColor,
            modifier           = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AkunKonfirmasiCard(account: OrangTuaAccount, onGantiClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ConfirmCardBg)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Filled.CheckCircle,
                        contentDescription = "Ditemukan",
                        tint               = ConfirmTextDark,
                        modifier           = Modifier.size(22.dp)
                    )
                    Text(
                        text       = "Akun ditemukan",
                        color      = ConfirmTextDark,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text       = "Ganti",
                    color      = ConfirmTextDark,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.clickable(onClick = onGantiClick)
                )
            }

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CircleMint)
                )
                Column {
                    Text(text = account.nama,     color = ConfirmTextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${account.username} · ${account.jumlahAnak} anak terdaftar", color = ConfirmTextDark.copy(alpha = 0.75f), fontSize = 11.sp)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ConfirmInfoBoxBg)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = "Setelah didaftarkan, anak ini akan otomatis muncul di halaman orang tua saat login.",
                    color     = ConfirmTextDark,
                    fontSize  = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LanjutButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundDark)
            .border(1.dp, TextWhite, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Lanjut - Konfirmasi →", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CardContainer(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun FormAkunBaruCard(
    namaOrtu    : String,
    onNamaChange: (String) -> Unit,
    username    : String,
    onUserChange: (String) -> Unit,
    noHp        : String,
    onHpChange  : (String) -> Unit,
    password    : String,
    onPassChange: (String) -> Unit,
    modifier    : Modifier = Modifier
) {
    val fieldBg     = Color(0xFF3A3A3A)
    val fieldBorder = Color(0xFF555555)
    val labelColor  = Color(0xFFAAAAAA)
    val infoBoxBg   = Color(0xFFBBCFEF)
    val infoBoxText = Color(0xFF1A3A6E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2A2A2A))
            .border(1.dp, Color(0xFF444444), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text       = "Buat akun orang tua baru",
                color      = TextWhite,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold
            )

            AkunBaruInputField(
                label         = "Nama Orang Tua",
                value         = namaOrtu,
                onValueChange = onNamaChange,
                placeholder   = "Nama Orang Tua",
                fieldBg       = fieldBg,
                fieldBorder   = fieldBorder,
                labelColor    = labelColor
            )
            AkunBaruInputField(
                label         = "Username Orang Tua",
                value         = username,
                onValueChange = onUserChange,
                placeholder   = "Untuk login orang tua",
                fieldBg       = fieldBg,
                fieldBorder   = fieldBorder,
                labelColor    = labelColor
            )
            AkunBaruInputField(
                label         = "No. HP",
                value         = noHp,
                onValueChange = onHpChange,
                placeholder   = "No. Hp Orang Tua",
                fieldBg       = fieldBg,
                fieldBorder   = fieldBorder,
                labelColor    = labelColor,
                keyboardType  = KeyboardType.Phone
            )
            AkunBaruInputField(
                label         = "Password Sementara",
                value         = password,
                onValueChange = onPassChange,
                placeholder   = "Min. 6 karakter",
                fieldBg       = fieldBg,
                fieldBorder   = fieldBorder,
                labelColor    = labelColor,
                keyboardType  = KeyboardType.Password,
                isPassword    = true
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(infoBoxBg)
                    .padding(12.dp)
            ) {
                Text(
                    text       = "Bagikan username & password ini ke orang tua anak. Mereka bisa ganti password setelah login pertama.",
                    color      = infoBoxText,
                    fontSize   = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun AkunBaruInputField(
    label        : String,
    value        : String,
    onValueChange: (String) -> Unit,
    placeholder  : String,
    fieldBg      : Color,
    fieldBorder  : Color,
    labelColor   : Color,
    keyboardType : KeyboardType = KeyboardType.Text,
    isPassword   : Boolean      = false,
    modifier     : Modifier     = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = labelColor, fontSize = 12.sp)
        OutlinedTextField(
            value                = value,
            onValueChange        = onValueChange,
            modifier             = Modifier
                .fillMaxWidth()
                .height(50.dp),
            placeholder          = { Text(text = placeholder, color = Color(0xFF6B6B6B), fontSize = 14.sp) },
            singleLine           = true,
            textStyle            = TextStyle(color = TextWhite, fontSize = 14.sp),
            keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            shape                = RoundedCornerShape(8.dp),
            colors               = TextFieldDefaults.outlinedTextFieldColors(
                textColor            = TextWhite,
                cursorColor          = AccentGreen,
                focusedBorderColor   = AccentGreen,
                unfocusedBorderColor = fieldBorder,
                backgroundColor      = fieldBg,
                placeholderColor     = Color(0xFF6B6B6B)
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun HubungOrangTuaScreenPreview() {
    HubungOrangTuaScreen()
}