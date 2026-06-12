package com.example.myapplication

import kotlin.math.roundToInt

// ════════════════════════════════════════════════════════════
//  TABEL WHO LENGKAP PER BULAN (0-60)
//  Sumber: WHO Child Growth Standards 2006
// ════════════════════════════════════════════════════════════

// ── TB/U Laki-laki ───────────────────────────────────────────
val tabelTBU_LakiLaki = mapOf(
    0 to Pair(49.9, 1.89),  1 to Pair(54.7, 2.04),  2 to Pair(58.4, 2.15),
    3 to Pair(61.4, 2.21),  4 to Pair(63.9, 2.23),  5 to Pair(65.9, 2.24),
    6 to Pair(67.6, 2.25),  7 to Pair(69.2, 2.28),  8 to Pair(70.6, 2.32),
    9 to Pair(72.0, 2.37), 10 to Pair(73.3, 2.42), 11 to Pair(74.5, 2.46),
    12 to Pair(75.7, 2.50), 13 to Pair(76.9, 2.53), 14 to Pair(78.0, 2.57),
    15 to Pair(79.1, 2.61), 16 to Pair(80.2, 2.64), 17 to Pair(81.2, 2.68),
    18 to Pair(82.3, 2.71), 19 to Pair(83.2, 2.75), 20 to Pair(84.2, 2.78),
    21 to Pair(85.1, 2.82), 22 to Pair(86.0, 2.85), 23 to Pair(86.9, 2.88),
    24 to Pair(87.8, 2.92), 25 to Pair(88.7, 2.97), 26 to Pair(89.6, 3.01),
    27 to Pair(90.4, 3.06), 28 to Pair(91.2, 3.10), 29 to Pair(92.1, 3.14),
    30 to Pair(92.9, 3.18), 31 to Pair(93.7, 3.22), 32 to Pair(94.4, 3.26),
    33 to Pair(95.2, 3.29), 34 to Pair(95.9, 3.33), 35 to Pair(96.7, 3.36),
    36 to Pair(97.4, 3.40), 37 to Pair(98.1, 3.43), 38 to Pair(98.7, 3.47),
    39 to Pair(99.4, 3.50), 40 to Pair(100.1,3.53), 41 to Pair(100.7,3.56),
    42 to Pair(101.3,3.59), 43 to Pair(102.0,3.62), 44 to Pair(102.6,3.65),
    45 to Pair(103.2,3.68), 46 to Pair(103.8,3.71), 47 to Pair(104.4,3.74),
    48 to Pair(105.0,3.77), 49 to Pair(105.6,3.80), 50 to Pair(106.1,3.82),
    51 to Pair(106.7,3.85), 52 to Pair(107.2,3.88), 53 to Pair(107.8,3.90),
    54 to Pair(108.3,3.93), 55 to Pair(108.9,3.96), 56 to Pair(109.4,3.98),
    57 to Pair(109.9,4.01), 58 to Pair(110.4,4.03), 59 to Pair(111.0,4.06),
    60 to Pair(111.5,4.08)
)

// ── TB/U Perempuan ───────────────────────────────────────────
val tabelTBU_Perempuan = mapOf(
    0 to Pair(49.1, 1.86),  1 to Pair(53.7, 2.01),  2 to Pair(57.1, 2.10),
    3 to Pair(59.8, 2.16),  4 to Pair(62.1, 2.19),  5 to Pair(64.0, 2.20),
    6 to Pair(65.7, 2.22),  7 to Pair(67.3, 2.26),  8 to Pair(68.7, 2.31),
    9 to Pair(70.1, 2.37), 10 to Pair(71.5, 2.43), 11 to Pair(72.8, 2.49),
    12 to Pair(74.0, 2.55), 13 to Pair(75.2, 2.60), 14 to Pair(76.4, 2.64),
    15 to Pair(77.5, 2.68), 16 to Pair(78.6, 2.72), 17 to Pair(79.7, 2.75),
    18 to Pair(80.7, 2.79), 19 to Pair(81.7, 2.82), 20 to Pair(82.7, 2.86),
    21 to Pair(83.7, 2.89), 22 to Pair(84.6, 2.92), 23 to Pair(85.5, 2.95),
    24 to Pair(86.4, 2.98), 25 to Pair(87.3, 3.02), 26 to Pair(88.1, 3.06),
    27 to Pair(88.9, 3.10), 28 to Pair(89.7, 3.13), 29 to Pair(90.5, 3.16),
    30 to Pair(91.3, 3.19), 31 to Pair(92.1, 3.22), 32 to Pair(92.9, 3.25),
    33 to Pair(93.6, 3.28), 34 to Pair(94.3, 3.31), 35 to Pair(95.0, 3.34),
    36 to Pair(95.7, 3.37), 37 to Pair(96.4, 3.40), 38 to Pair(97.1, 3.43),
    39 to Pair(97.7, 3.46), 40 to Pair(98.4, 3.48), 41 to Pair(99.0, 3.51),
    42 to Pair(99.7, 3.54), 43 to Pair(100.3,3.56), 44 to Pair(100.9,3.59),
    45 to Pair(101.5,3.62), 46 to Pair(102.1,3.64), 47 to Pair(102.7,3.67),
    48 to Pair(103.3,3.69), 49 to Pair(103.9,3.72), 50 to Pair(104.4,3.74),
    51 to Pair(105.0,3.77), 52 to Pair(105.6,3.79), 53 to Pair(106.1,3.82),
    54 to Pair(106.7,3.84), 55 to Pair(107.2,3.86), 56 to Pair(107.8,3.89),
    57 to Pair(108.3,3.91), 58 to Pair(108.8,3.93), 59 to Pair(109.4,3.96),
    60 to Pair(109.9,3.98)
)

// ── BB/U Laki-laki ───────────────────────────────────────────
val tabelBBU_LakiLaki = mapOf(
    0 to Pair(3.35, 0.43),  1 to Pair(4.47, 0.52),  2 to Pair(5.57, 0.60),
    3 to Pair(6.37, 0.65),  4 to Pair(7.00, 0.68),  5 to Pair(7.51, 0.71),
    6 to Pair(7.93, 0.74),  7 to Pair(8.30, 0.77),  8 to Pair(8.62, 0.79),
    9 to Pair(8.90, 0.82), 10 to Pair(9.16, 0.84), 11 to Pair(9.41, 0.86),
    12 to Pair(9.65, 0.89), 13 to Pair(9.88, 0.91), 14 to Pair(10.09,0.93),
    15 to Pair(10.30,0.95), 16 to Pair(10.49,0.97), 17 to Pair(10.69,0.99),
    18 to Pair(10.89,1.01), 19 to Pair(11.08,1.03), 20 to Pair(11.27,1.05),
    21 to Pair(11.46,1.07), 22 to Pair(11.65,1.09), 23 to Pair(11.83,1.11),
    24 to Pair(12.01,1.13), 25 to Pair(12.19,1.15), 26 to Pair(12.37,1.17),
    27 to Pair(12.55,1.19), 28 to Pair(12.73,1.21), 29 to Pair(12.90,1.23),
    30 to Pair(13.07,1.25), 31 to Pair(13.24,1.27), 32 to Pair(13.41,1.29),
    33 to Pair(13.58,1.31), 34 to Pair(13.74,1.33), 35 to Pair(13.90,1.35),
    36 to Pair(14.06,1.37), 37 to Pair(14.22,1.39), 38 to Pair(14.38,1.41),
    39 to Pair(14.54,1.43), 40 to Pair(14.69,1.45), 41 to Pair(14.84,1.47),
    42 to Pair(14.99,1.49), 43 to Pair(15.14,1.51), 44 to Pair(15.29,1.53),
    45 to Pair(15.44,1.55), 46 to Pair(15.58,1.57), 47 to Pair(15.73,1.59),
    48 to Pair(15.87,1.61), 49 to Pair(16.01,1.63), 50 to Pair(16.15,1.65),
    51 to Pair(16.29,1.67), 52 to Pair(16.43,1.69), 53 to Pair(16.57,1.71),
    54 to Pair(16.71,1.73), 55 to Pair(16.85,1.75), 56 to Pair(16.98,1.77),
    57 to Pair(17.12,1.79), 58 to Pair(17.25,1.81), 59 to Pair(17.38,1.83),
    60 to Pair(17.52,1.85)
)

// ── BB/U Perempuan ───────────────────────────────────────────
val tabelBBU_Perempuan = mapOf(
    0 to Pair(3.23, 0.41),  1 to Pair(4.18, 0.49),  2 to Pair(5.12, 0.56),
    3 to Pair(5.84, 0.61),  4 to Pair(6.42, 0.64),  5 to Pair(6.90, 0.67),
    6 to Pair(7.30, 0.70),  7 to Pair(7.64, 0.72),  8 to Pair(7.95, 0.75),
    9 to Pair(8.23, 0.77), 10 to Pair(8.49, 0.79), 11 to Pair(8.74, 0.82),
    12 to Pair(8.95, 0.84), 13 to Pair(9.18, 0.86), 14 to Pair(9.39, 0.88),
    15 to Pair(9.60, 0.90), 16 to Pair(9.81, 0.92), 17 to Pair(10.01,0.94),
    18 to Pair(10.20,0.96), 19 to Pair(10.39,0.98), 20 to Pair(10.58,1.00),
    21 to Pair(10.77,1.02), 22 to Pair(10.95,1.04), 23 to Pair(11.13,1.06),
    24 to Pair(11.30,1.08), 25 to Pair(11.48,1.10), 26 to Pair(11.65,1.12),
    27 to Pair(11.82,1.14), 28 to Pair(11.99,1.16), 29 to Pair(12.15,1.18),
    30 to Pair(12.31,1.20), 31 to Pair(12.47,1.22), 32 to Pair(12.63,1.24),
    33 to Pair(12.79,1.26), 34 to Pair(12.94,1.28), 35 to Pair(13.09,1.30),
    36 to Pair(13.24,1.32), 37 to Pair(13.39,1.34), 38 to Pair(13.53,1.36),
    39 to Pair(13.68,1.38), 40 to Pair(13.82,1.40), 41 to Pair(13.96,1.42),
    42 to Pair(14.10,1.44), 43 to Pair(14.23,1.46), 44 to Pair(14.37,1.48),
    45 to Pair(14.50,1.50), 46 to Pair(14.63,1.52), 47 to Pair(14.76,1.54),
    48 to Pair(14.89,1.56), 49 to Pair(15.01,1.58), 50 to Pair(15.14,1.60),
    51 to Pair(15.26,1.62), 52 to Pair(15.38,1.64), 53 to Pair(15.50,1.66),
    54 to Pair(15.62,1.68), 55 to Pair(15.74,1.70), 56 to Pair(15.85,1.72),
    57 to Pair(15.97,1.74), 58 to Pair(16.08,1.76), 59 to Pair(16.19,1.78),
    60 to Pair(16.30,1.80)
)

// ════════════════════════════════════════════════════════════
//  FUNGSI KALKULASI — dipakai oleh PemeriksaanScreen
//  dan DetailAnakRepository
// ════════════════════════════════════════════════════════════

/**
 * Interpolasi linear antar dua titik tabel.
 * Karena tabel sudah lengkap per bulan (0-60),
 * interpolasi hanya terjadi jika umur > 60 (di-clamp ke 60).
 */
fun interpolasi(umur: Int, tabel: Map<Int, Pair<Double, Double>>): Pair<Double, Double> {
    val bulan = umur.coerceIn(0, 60)
    // Tabel lengkap per bulan — langsung ambil nilai tepat
    tabel[bulan]?.let { return it }
    // Fallback interpolasi jika ada gap (seharusnya tidak terjadi)
    val keys  = tabel.keys.sorted()
    val lower = keys.lastOrNull { it <= bulan } ?: keys.first()
    val upper = keys.firstOrNull { it >= bulan } ?: keys.last()
    if (lower == upper) return tabel[lower]!!
    val (medLow, sdLow) = tabel[lower]!!
    val (medUp,  sdUp)  = tabel[upper]!!
    val ratio = (bulan - lower).toDouble() / (upper - lower)
    return Pair(medLow + ratio * (medUp - medLow), sdLow + ratio * (sdUp - sdLow))
}

fun hitungZScore(nilai: Double, median: Double, sd: Double): Double =
    (nilai - median) / sd

fun analisisWHO(
    tinggiBadan : Double,
    beratBadan  : Double,
    umurBulan   : Int,
    jenisKelamin: String
): HasilAnalisis {
    val isLaki = jenisKelamin.contains("Laki", ignoreCase = true)

    val (medTB, sdTB) = interpolasi(umurBulan, if (isLaki) tabelTBU_LakiLaki else tabelTBU_Perempuan)
    val (medBB, sdBB) = interpolasi(umurBulan, if (isLaki) tabelBBU_LakiLaki else tabelBBU_Perempuan)

    val zTBU = hitungZScore(tinggiBadan, medTB, sdTB)
    val zBBU = hitungZScore(beratBadan,  medBB, sdBB)

    val (statusTBU, warnaTBU, saranTBU) = when {
        zTBU < -3.0 -> Triple(
            "Sangat Pendek",
            StatusWarna.DANGER,
            "Anak mengalami stunting berat. Segera rujuk ke tenaga kesehatan dan tingkatkan asupan gizi."
        )
        zTBU < -2.0 -> Triple(
            "Pendek (Stunting)",
            StatusWarna.WARN,
            "Anak berisiko stunting. Pantau pertumbuhan rutin dan perbaiki pola makan bergizi seimbang."
        )
        zTBU > 3.0  -> Triple(
            "Sangat Tinggi",
            StatusWarna.WARN,
            "Tinggi badan di atas rata-rata. Pantau kondisi kesehatan secara berkala."
        )
        else -> Triple(
            "Normal",
            StatusWarna.NORMAL,
            "Tinggi badan sesuai usia. Pertahankan pola makan dan stimulasi tumbuh kembang."
        )
    }

    val (statusBBU, warnaBBU, saranBBU) = when {
        zBBU < -3.0 -> Triple(
            "Gizi Buruk",
            StatusWarna.DANGER,
            "Anak mengalami gizi buruk. Segera rujuk ke puskesmas untuk tata laksana gizi buruk."
        )
        zBBU < -2.0 -> Triple(
            "Gizi Kurang",
            StatusWarna.WARN,
            "Berat badan di bawah normal. Tingkatkan asupan kalori dan protein, pantau setiap bulan."
        )
        zBBU > 3.0  -> Triple(
            "Obesitas",
            StatusWarna.DANGER,
            "Anak mengalami obesitas. Konsultasikan ke dokter untuk penanganan lebih lanjut."
        )
        zBBU > 2.0  -> Triple(
            "Gizi Lebih",
            StatusWarna.WARN,
            "Berat badan di atas normal. Perhatikan pola makan dan aktivitas fisik anak."
        )
        else -> Triple(
            "Gizi Baik",
            StatusWarna.NORMAL,
            "Berat badan sesuai usia. Pertahankan pola makan bergizi dan aktivitas fisik."
        )
    }

    return HasilAnalisis(
        zScoreTBU = (zTBU * 100).roundToInt() / 100.0,
        zScoreBBU = (zBBU * 100).roundToInt() / 100.0,
        statusTBU = statusTBU,
        statusBBU = statusBBU,
        warnasTBU = warnaTBU,
        warnasBBU = warnaBBU,
        saranTBU  = saranTBU,
        saranBBU  = saranBBU
    )
}