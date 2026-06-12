package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private val RoleSelectedBg   = Color(0xFFB8EDD8)
private val RoleSelectedText = Color(0xFF1E6B4E)
private val RoleIdleText     = TextWhite
private val InputOutline     = Color(0xFF555555)
private val InputLabelColor  = Color(0xFFAAAAAA)
private val KaderIconTeal    = Color(0xFF3DB89C)
private val OrangTuaIconBlue = Color(0xFF4A90D9)
private val LogoBoxBg        = Color(0xFF7ECFB0)

@Composable
fun LoginScreen(
    formViewModel            : FormDataViewModel = viewModel(),
    onNavigateToDashboard    : (String) -> Unit  = {},
    onNavigateToDashboardOrtu: (String) -> Unit  = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var selectedRole by remember { mutableStateOf("kader") }
    var username     by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var loginError   by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(top = 24.dp)
    ) {
        LoginHeader()
        Spacer(modifier = Modifier.height(36.dp))
        RoleSelection(
            selectedRole   = selectedRole,
            onRoleSelected = { selectedRole = it; loginError = "" },
            modifier       = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(28.dp))
        LoginForm(
            username         = username,
            onUsernameChange = { username = it; loginError = "" },
            password         = password,
            onPasswordChange = { password = it; loginError = "" },
            modifier         = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(28.dp))
        LoginButton(
            isLoading = isLoading,
            onClick   = {
                if (isLoading) return@LoginButton

                val usernameInput = username.trim()
                val passwordInput = password.trim()
                val roleInput     = if (selectedRole == "kader") "kader" else "orangtua"

                if (usernameInput.isBlank() || passwordInput.isBlank()) {
                    loginError = "Username dan password tidak boleh kosong"
                    return@LoginButton
                }

                isLoading  = true
                loginError = ""

                scope.launch {
                    try {
                        // context dikirim ke fetchLoginFromApi untuk simpan user ke SQLite
                        val user = fetchLoginFromApi(usernameInput, passwordInput, roleInput, context)

                        if (user != null) {
                            // Sync data posyandu, jadwal, vaksin referensi ke SQLite lokal
                            try {
                                SyncPosyandu.syncAll(context)
                                android.util.Log.d("SYNC", "Sync berhasil")
                            } catch (e: Exception) {
                                android.util.Log.e("SYNC", "Gagal sync: ${e.message}", e)
                            }

                            if (selectedRole == "kader") {
                                formViewModel.loggedInKaderId = user.id
                                onNavigateToDashboard(user.username)
                            } else {
                                formViewModel.loggedInOrangTuaUsername = user.username
                                onNavigateToDashboardOrtu(user.username)
                            }

                        } else {
                            loginError = "Username, password, atau role salah"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("LOGIN_DEBUG", "Error: ${e.message}", e)
                        loginError = "Gagal terhubung ke server"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        if (loginError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text     = loginError,
                color    = Color(0xFFFF6B6B),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
fun LoginHeader() {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(LogoBoxBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.MedicalServices,
                contentDescription = "Logo",
                tint               = Color(0xFF1E6B4E),
                modifier           = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text       = "MyPosyandu",
            color      = TextWhite,
            fontSize   = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// ── Role Selection ────────────────────────────────────────────────────────────

@Composable
fun RoleSelection(
    selectedRole  : String,
    onRoleSelected: (String) -> Unit,
    modifier      : Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Masuk Sebagai", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoleCard(
                roleKey      = "kader",
                label        = "Kader",
                subLabel     = "Petugas posyandu",
                icon         = Icons.Default.MedicalServices,
                iconTintIdle = KaderIconTeal,
                isSelected   = selectedRole == "kader",
                onSelected   = onRoleSelected,
                modifier     = Modifier.weight(1f)
            )
            RoleCard(
                roleKey      = "orangtua",
                label        = "Orang Tua",
                subLabel     = "Lihat data anak",
                icon         = Icons.Default.People,
                iconTintIdle = OrangTuaIconBlue,
                isSelected   = selectedRole == "orangtua",
                onSelected   = onRoleSelected,
                modifier     = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RoleCard(
    roleKey     : String,
    label       : String,
    subLabel    : String,
    icon        : ImageVector,
    iconTintIdle: Color,
    isSelected  : Boolean,
    onSelected  : (String) -> Unit,
    modifier    : Modifier = Modifier
) {
    val cardBg    = if (isSelected) RoleSelectedBg else SurfaceDark
    val textColor = if (isSelected) RoleSelectedText else RoleIdleText
    val subColor  = if (isSelected) RoleSelectedText.copy(alpha = 0.7f) else TextGrey
    val iconTint  = if (isSelected) RoleSelectedText else iconTintIdle

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(
                width = 1.dp,
                color = if (isSelected) MenuMintBorder else SurfaceDarkBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelected(roleKey) }
            .padding(vertical = 18.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, label, tint = iconTint, modifier = Modifier.size(30.dp))
            Text(label,    color = textColor, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(subLabel, color = subColor,  fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

// ── Login Form ────────────────────────────────────────────────────────────────

@Composable
fun LoginForm(
    username        : String,
    onUsernameChange: (String) -> Unit,
    password        : String,
    onPasswordChange: (String) -> Unit,
    modifier        : Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LoginInputField(
            label         = "Username",
            value         = username,
            onValueChange = onUsernameChange,
            placeholder   = "Username..",
            leadingIcon   = Icons.Default.Person,
            isPassword    = false
        )
        LoginInputField(
            label         = "Password",
            value         = password,
            onValueChange = onPasswordChange,
            placeholder   = "Password..",
            leadingIcon   = Icons.Default.Lock,
            isPassword    = true
        )
    }
}

@Composable
private fun LoginInputField(
    label        : String,
    value        : String,
    onValueChange: (String) -> Unit,
    placeholder  : String,
    leadingIcon  : ImageVector,
    isPassword   : Boolean,
    modifier     : Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = InputLabelColor, fontSize = 13.sp)
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text(placeholder, color = InputLabelColor, fontSize = 14.sp) },
            leadingIcon   = {
                Icon(leadingIcon, null, tint = InputLabelColor, modifier = Modifier.size(20.dp))
            },
            singleLine           = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation()
            else VisualTransformation.None,
            keyboardOptions      = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text
            ),
            shape  = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor            = TextWhite,
                cursorColor          = AccentGreen,
                focusedBorderColor   = InputOutline,
                unfocusedBorderColor = InputOutline,
                backgroundColor      = BackgroundDark,
                placeholderColor     = InputLabelColor,
                leadingIconColor     = InputLabelColor
            )
        )
    }
}

// ── Login Button ──────────────────────────────────────────────────────────────

@Composable
fun LoginButton(
    onClick  : () -> Unit,
    isLoading: Boolean  = false,
    modifier : Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isLoading) Color(0xFF333333) else BackgroundDark)
            .border(1.dp, if (isLoading) Color(0xFF555555) else TextWhite, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color       = AccentGreen,
                modifier    = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text       = "Masuk",
                color      = TextWhite,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun LoginScreenPreview() { LoginScreen() }