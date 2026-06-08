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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────
//  Local colours
// ─────────────────────────────────────────────────────────────
private val RoleSelectedBg   = Color(0xFFB8EDD8)   // mint – active role card bg
private val RoleSelectedText = Color(0xFF1E6B4E)   // dark green – active role text
private val RoleIdleText     = TextWhite
private val InputOutline     = Color(0xFF555555)
private val InputLabelColor  = Color(0xFFAAAAAA)
private val KaderIconTeal    = Color(0xFF3DB89C)   // teal icon on Kader card
private val OrangTuaIconBlue = Color(0xFF4A90D9)   // blue icon on Orang Tua card
private val LogoBoxBg        = Color(0xFF7ECFB0)   // mint rounded-square logo bg

// ════════════════════════════════════════════════════════════
//  SCREEN ENTRY POINT
// ════════════════════════════════════════════════════════════

@Composable
fun LoginScreen(onNavigateToDashboard: () -> Unit = {}) {

    // ── State ────────────────────────────────────────────────
    var selectedRole by remember { mutableStateOf("kader") }
    var username     by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    // ────────────────────────────────────────────────────────

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Logo / branding block ────────────────────────────
        LoginHeader()

        Spacer(modifier = Modifier.height(36.dp))

        // ── Role selection ────────────────────────────────────
        RoleSelection(
            selectedRole   = selectedRole,
            onRoleSelected = { selectedRole = it },
            modifier       = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Input form ────────────────────────────────────────
        LoginForm(
            username        = username,
            onUsernameChange = { username = it },
            password        = password,
            onPasswordChange = { password = it },
            modifier        = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── CTA button ────────────────────────────────────────
        LoginButton(
            onClick  = onNavigateToDashboard,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ════════════════════════════════════════════════════════════
//  1. HEADER  —  thin green strip + rounded-square logo + title
// ════════════════════════════════════════════════════════════

@Composable
fun LoginHeader() {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rounded-square mint logo box with stethoscope icon
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(LogoBoxBg),
            contentAlignment = Alignment.Center
        ) {
            // ── ICON SLOT ─────────────────────────────────────
            // Replace with your actual stethoscope drawable:
            //   Icon(
            //       painter = painterResource(R.drawable.ic_stethoscope),
            //       contentDescription = "Logo",
            //       tint = Color(0xFF1E6B4E),
            //       modifier = Modifier.size(42.dp)
            //   )
            Icon(
                imageVector        = Icons.Default.MedicalServices,
                contentDescription = "Logo",
                tint               = Color(0xFF1E6B4E),
                modifier           = Modifier.size(40.dp)
            )
            // ── END ICON SLOT ─────────────────────────────────
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App title
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

// ════════════════════════════════════════════════════════════
//  2. ROLE SELECTION
// ════════════════════════════════════════════════════════════

@Composable
fun RoleSelection(
    selectedRole  : String,
    onRoleSelected: (String) -> Unit,
    modifier      : Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text       = "Masuk Sebagai",
            color      = TextWhite,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold
        )

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
                width  = 1.dp,
                color  = if (isSelected) MenuMintBorder else SurfaceDarkBorder,
                shape  = RoundedCornerShape(14.dp)
            )
            .clickable { onSelected(roleKey) }
            .padding(vertical = 18.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = iconTint,
                modifier           = Modifier.size(30.dp)
            )
            Text(
                text       = label,
                color      = textColor,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )
            Text(
                text      = subLabel,
                color     = subColor,
                fontSize  = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  3. LOGIN FORM
// ════════════════════════════════════════════════════════════

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
            label        = "Username",
            value        = username,
            onValueChange = onUsernameChange,
            placeholder  = "Username..",
            leadingIcon  = Icons.Default.Person,
            isPassword   = false
        )
        LoginInputField(
            label        = "Password",
            value        = password,
            onValueChange = onPasswordChange,
            placeholder  = "Password..",
            leadingIcon  = Icons.Default.Lock,
            isPassword   = true
        )
    }
}

@Composable
private fun LoginInputField(
    label        : String,
    value        : String,
    onValueChange: (String) -> Unit,
    placeholder  : String,
    leadingIcon  : ImageVector,   // kept in signature for backward-compat, not rendered
    isPassword   : Boolean,
    modifier     : Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text     = label,
            color    = InputLabelColor,
            fontSize = 13.sp
        )
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = {
                Text(text = placeholder, color = InputLabelColor, fontSize = 14.sp)
            },
            singleLine            = true,
            visualTransformation  = if (isPassword) PasswordVisualTransformation()
                                    else VisualTransformation.None,
            keyboardOptions       = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text
            ),
            shape  = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor              = TextWhite,
                cursorColor            = AccentGreen,
                focusedBorderColor     = InputOutline,
                unfocusedBorderColor   = InputOutline,
                backgroundColor        = BackgroundDark,
                placeholderColor       = InputLabelColor,
                focusedLabelColor      = AccentGreen,
                unfocusedLabelColor    = InputLabelColor
            )
        )
    }
}

// ════════════════════════════════════════════════════════════
//  4. LOGIN BUTTON
// ════════════════════════════════════════════════════════════

@Composable
fun LoginButton(
    onClick : () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BackgroundDark)
            .border(
                width  = 1.dp,
                color  = TextWhite,
                shape  = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "Masuk",
            color      = TextWhite,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )
    }
}

// ════════════════════════════════════════════════════════════
//  PREVIEW
// ════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true, backgroundColor = 0xFF121212)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}
