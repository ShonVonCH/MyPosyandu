package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

@Composable
fun ExportDatabaseButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var statusMsg by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E3A2F))
                .border(1.dp, Color(0xFF2E9B6E), RoundedCornerShape(12.dp))
                .clickable {
                    val result = exportDatabase(context)
                    statusMsg = result.message
                    isSuccess = result.success
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "⬇", fontSize = 16.sp)
                Text(
                    text       = "Export Database (.db)",
                    color      = Color(0xFF2E9B6E),
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (statusMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSuccess) Color(0xFF1A3A2A) else Color(0xFF3A1A1A)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text      = statusMsg,
                    color     = if (isSuccess) Color(0xFF6FDDAA) else Color(0xFFDD6F6F),
                    fontSize  = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

data class ExportResult(val success: Boolean, val message: String)

fun exportDatabase(context: Context): ExportResult {
    val dbName    = DatabaseHelper.DATABASE_NAME
    val dbFile    = context.getDatabasePath(dbName)
    val outName   = "posyandu_export_${System.currentTimeMillis()}.db"

    if (!dbFile.exists()) {
        return ExportResult(false, "❌ File database tidak ditemukan.\nBelum ada data yang tersimpan.")
    }

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values   = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, outName)
                put(MediaStore.Downloads.MIME_TYPE,    "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return ExportResult(false, "❌ Gagal membuat file di Downloads.")

            resolver.openOutputStream(uri)?.use { out ->
                copyDbToStream(dbFile, out)
            }

            ExportResult(
                true,
                "✅ Berhasil disimpan ke:\nDownloads/$outName\n\n" +
                        "Buka dengan DB Browser for SQLite atau SQLiteOnline.com"
            )
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            downloadsDir.mkdirs()
            val outFile = File(downloadsDir, outName)
            copyDbToStream(dbFile, outFile.outputStream())

            ExportResult(
                true,
                "✅ Berhasil disimpan ke:\nDownloads/$outName\n\n" +
                        "Buka dengan DB Browser for SQLite atau SQLiteOnline.com"
            )
        }
    } catch (e: Exception) {
        ExportResult(false, "❌ Export gagal: ${e.message}")
    }
}

private fun copyDbToStream(dbFile: File, out: OutputStream) {
    val walFile = File(dbFile.path + "-wal")
    val shmFile = File(dbFile.path + "-shm")

    FileInputStream(dbFile).use { input ->
        out.use { output ->
            input.copyTo(output, bufferSize = 8 * 1024)
        }
    }
}