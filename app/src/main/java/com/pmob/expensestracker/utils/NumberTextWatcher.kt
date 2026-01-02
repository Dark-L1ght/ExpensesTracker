package com.pmob.expensestracker.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * NumberTextWatcher
 *
 * Kelas utilitas (Helper Class) yang mengimplementasikan interface [TextWatcher].
 * Kelas ini berfungsi untuk memformat input angka pada EditText secara real-time
 * dengan menambahkan pemisah ribuan (titik) sesuai format mata uang Indonesia.
 *
 * Contoh hasil: Input "50000" akan otomatis berubah menjadi "50.000".
 *
 * @property editText Komponen EditText yang akan dipantau perubahannya.
 */
class NumberTextWatcher(private val editText: EditText) : TextWatcher {

    private var current = ""

    // Fungsi ini wajib ada karena implementasi interface, namun tidak digunakan.
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    // Fungsi ini wajib ada karena implementasi interface, namun tidak digunakan.
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    /**
     * Dipanggil setelah teks pada EditText berubah.
     * Di sini logika pemformatan angka dijalankan.
     */
    override fun afterTextChanged(s: Editable?) {
        // Cek apakah teks berubah dari sebelumnya untuk mencegah Infinite Loop
        if (s.toString() != current) {

            // 1. Hentikan listener sementara agar perubahan format tidak memicu event ini lagi secara rekursif
            editText.removeTextChangedListener(this)

            try {
                // 2. Bersihkan string dari karakter non-digit (hapus titik yang lama)
                val cleanString = s.toString().replace("[^\\d]".toRegex(), "")

                if (cleanString.isNotEmpty()) {
                    // 3. Parsing string angka bersih ke tipe Double
                    val parsed = cleanString.toDouble()

                    // 4. Format angka ke format Indonesia (menggunakan titik sebagai pemisah ribuan)
                    val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))
                    val formatted = formatter.format(parsed)

                    // 5. Update teks di EditText dengan string yang sudah diformat
                    current = formatted
                    editText.setText(formatted)

                    // 6. Pindahkan kursor ke posisi paling akhir agar user bisa lanjut mengetik
                    editText.setSelection(formatted.length)
                } else {
                    current = ""
                    // Handle jika user menghapus semua teks
                }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }

            // 7. Pasang kembali listener untuk mendeteksi input selanjutnya
            editText.addTextChangedListener(this)
        }
    }
}