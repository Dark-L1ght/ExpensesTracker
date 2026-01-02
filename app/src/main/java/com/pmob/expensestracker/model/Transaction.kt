package com.pmob.expensestracker.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Transaction Model
 *
 * Data class yang merepresentasikan entitas sebuah Transaksi Keuangan.
 * Kelas ini memetakan struktur data JSON dari Firebase Realtime Database
 * menjadi objek Kotlin yang dapat dimanipulasi dalam aplikasi.
 *
 * Anotasi @Parcelize digunakan untuk mempermudah pengiriman objek ini
 * antar komponen (Activity/Fragment) melalui Intent/Bundle.
 *
 * @property id Unique ID yang digenerate oleh Firebase (key).
 * @property type Jenis transaksi, bernilai "Income" atau "Expense".
 * @property category Kategori transaksi (contoh: "Makanan", "Gaji").
 * @property amount Nominal transaksi dalam satuan Long untuk akurasi perhitungan.
 * @property date Tanggal transaksi dalam format string yang mudah dibaca (dd MMM yyyy).
 * @property timestamp Waktu dalam milidetik (epoch) untuk keperluan pengurutan (sorting).
 */
@Parcelize
data class Transaction(
    val id: String = "",
    val type: String = "",
    val category: String = "",
    val amount: Long = 0L,
    val date: String = "",
    val timestamp: Long = 0L
) : Parcelable