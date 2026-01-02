package com.pmob.expensestracker.features.transaction

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.pmob.expensestracker.R
import com.pmob.expensestracker.databinding.ActivityAddTransactionBinding
import com.pmob.expensestracker.model.Transaction
import com.pmob.expensestracker.utils.NumberTextWatcher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AddTransactionActivity
 *
 * Activity ini menangani tiga fungsi utama (CRUD):
 * 1. Menambahkan Transaksi Baru (Create).
 * 2. Mengedit Transaksi yang Ada (Update).
 * 3. Menampilkan Detail Transaksi (Read) dan Menghapus (Delete).
 *
 * Activity ini bersifat dinamis, tampilan dan logika tombol akan berubah
 * tergantung data yang diterima melalui Intent.
 */
class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var dbRef: DatabaseReference
    private var transactionToEdit: Transaction? = null

    // Konstanta statis untuk manajemen key dan data tetap
    companion object {
        const val EXTRA_TRANSACTION = "TRANSACTION_DATA"
        const val EXTRA_READ_ONLY = "IS_READ_ONLY"
        private const val DATABASE_URL = "https://pmobakhir-1279e-default-rtdb.asia-southeast1.firebasedatabase.app"

        private val CATEGORIES_EXPENSE = arrayOf(
            "Makanan & Minuman", "Uang Kos/Kontrakan", "Transportasi/Bensin",
            "Tugas/Alat Tulis/Print", "Pulsa/Data/Netflix", "Hiburan/Self Reward", "Lainnya"
        )
        private val CATEGORIES_INCOME = arrayOf(
            "Kiriman Orang Tua", "Gaji", "Beasiswa",
            "Project/Freelance", "Tabungan", "Investasi", "Lainnya"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi komponen dan logika
        initFirebase()
        setupInputFormatting()
        checkIntentData()
        setupListeners()
    }

    /**
     * Menginisialisasi koneksi ke Firebase Realtime Database.
     */
    private fun initFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        dbRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("transactions").child(userId)
    }

    /**
     * Menambahkan TextWatcher untuk format ribuan otomatis pada input nominal.
     */
    private fun setupInputFormatting() {
        binding.etAmount.addTextChangedListener(NumberTextWatcher(binding.etAmount))
    }

    /**
     * Memeriksa apakah Activity dibuka untuk "Tambah Baru", "Edit", atau "Lihat Detail".
     */
    private fun checkIntentData() {
        transactionToEdit = intent.getParcelableExtra(EXTRA_TRANSACTION)
        val isReadOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, false)

        if (transactionToEdit != null) {
            setupEditMode(transactionToEdit!!, isReadOnly)
        } else {
            setupAddMode()
        }
    }

    /**
     * Mengatur listener untuk interaksi tombol dan radio button.
     */
    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }

        // Ubah isi Spinner Kategori berdasarkan pilihan Income/Expense
        binding.rgType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_income) setupSpinner(CATEGORIES_INCOME)
            else setupSpinner(CATEGORIES_EXPENSE)
        }

        binding.btnSave.setOnClickListener {
            if (transactionToEdit == null) performAddTransaction()
            else performEditTransaction()
        }

        binding.btnDelete.setOnClickListener { showDeleteConfirmation() }
    }

    /**
     * Mengkonfigurasi UI untuk Mode Tambah Baru.
     */
    private fun setupAddMode() {
        binding.tvHeaderTitle.text = "Tambah Transaksi"
        binding.btnSave.text = "Simpan Transaksi"
        binding.btnDelete.visibility = View.GONE
        setupSpinner(CATEGORIES_EXPENSE) // Default ke pengeluaran
    }

    /**
     * Mengkonfigurasi UI untuk Mode Edit atau Mode Baca Saja (Read Only).
     */
    private fun setupEditMode(trx: Transaction, isReadOnly: Boolean) {
        binding.etAmount.setText(trx.amount.toString())

        // Set Radio Button & Spinner sesuai data lama
        if (trx.type == "Income") {
            binding.rbIncome.isChecked = true
            setupSpinner(CATEGORIES_INCOME)
            binding.spinnerCategory.setSelection(CATEGORIES_INCOME.indexOf(trx.category))
        } else {
            binding.rbExpense.isChecked = true
            setupSpinner(CATEGORIES_EXPENSE)
            binding.spinnerCategory.setSelection(CATEGORIES_EXPENSE.indexOf(trx.category))
        }

        if (isReadOnly) {
            applyReadOnlyUI(trx)
        } else {
            binding.tvHeaderTitle.text = "Edit Transaksi"
            binding.btnSave.text = "Perbarui Transaksi"
            binding.btnDelete.visibility = View.VISIBLE
        }
    }

    /**
     * Mengunci UI khusus untuk mode Baca Saja (Detail Transaksi).
     */
    private fun applyReadOnlyUI(trx: Transaction) {
        binding.tvHeaderTitle.text = "Detail Transaksi"
        binding.btnSave.visibility = View.GONE
        binding.btnDelete.visibility = View.GONE

        // Tampilkan informasi tanggal
        binding.lineDate.visibility = View.VISIBLE
        binding.tvLabelDate.visibility = View.VISIBLE
        binding.tvTransactionDate.visibility = View.VISIBLE
        binding.tvTransactionDate.text = trx.date

        // Non-aktifkan input
        binding.etAmount.isEnabled = false
        binding.spinnerCategory.isEnabled = false
        binding.rbIncome.isEnabled = false
        binding.rbExpense.isEnabled = false
    }

    /**
     * Logika menyimpan transaksi baru ke Firebase.
     */
    private fun performAddTransaction() {
        val data = collectInputData() ?: return
        val newId = dbRef.push().key ?: return

        dbRef.child(newId).setValue(data.copy(id = newId)).addOnSuccessListener {
            Toast.makeText(this, "Berhasil disimpan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Logika memperbarui data transaksi yang sudah ada.
     */
    private fun performEditTransaction() {
        val newData = collectInputData() ?: return
        val oldData = transactionToEdit ?: return

        // Gabungkan data baru dengan ID & Timestamp lama
        val updatedTransaction = newData.copy(
            id = oldData.id,
            date = oldData.date,
            timestamp = oldData.timestamp
        )

        dbRef.child(oldData.id).setValue(updatedTransaction).addOnSuccessListener {
            Toast.makeText(this, "Berhasil diperbarui", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Menghapus transaksi dari Firebase.
     */
    private fun performDeleteTransaction() {
        transactionToEdit?.id?.let { id ->
            dbRef.child(id).removeValue().addOnSuccessListener {
                Toast.makeText(this, "Transaksi dihapus", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Mengambil dan memvalidasi input dari user.
     * @return Objek Transaction atau null jika input tidak valid.
     */
    private fun collectInputData(): Transaction? {
        val rawAmount = binding.etAmount.text.toString()
        // Hapus titik format ribuan agar bisa diparsing ke Long
        val cleanAmountStr = rawAmount.replace(".", "")

        if (cleanAmountStr.isEmpty()) {
            binding.etAmount.error = "Mohon isi nominal"
            return null
        }

        val amountLong = cleanAmountStr.toLongOrNull() ?: 0L
        val type = if (binding.rbIncome.isChecked) "Income" else "Expense"
        val category = binding.spinnerCategory.selectedItem.toString()
        val currentDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        return Transaction(
            id = "", // ID akan diisi saat push()
            type = type,
            category = category,
            amount = amountLong,
            date = currentDate,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Transaksi")
            .setMessage("Apakah Anda yakin ingin menghapus data ini?")
            .setPositiveButton("Hapus") { _, _ -> performDeleteTransaction() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupSpinner(list: Array<String>) {
        binding.spinnerCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            list
        )
    }
}