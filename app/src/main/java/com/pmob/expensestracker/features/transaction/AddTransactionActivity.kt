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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var dbRef: DatabaseReference
    private var transactionToEdit: Transaction? = null

    private val categoriesExpense = arrayOf(
        "Makanan & Minuman", "Uang Kos/Kontrakan", "Transportasi/Bensin",
        "Tugas/Alat Tulis/Print", "Pulsa/Data/Netflix", "Hiburan/Self Reward", "Lainnya"
    )
    private val categoriesIncome = arrayOf(
        "Kiriman Orang Tua", "Gaji Part-time", "Beasiswa",
        "Project/Freelance", "Tabungan", "Lainnya"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFirebase()
        checkIntentData()
        setupListeners()
    }

    private fun initFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseUrl = "https://expence-tra-4c82c-default-rtdb.asia-southeast1.firebasedatabase.app"
        dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("transactions").child(userId)
    }

    private fun checkIntentData() {
        transactionToEdit = intent.getParcelableExtra("TRANSACTION_DATA")
        val isReadOnly = intent.getBooleanExtra("IS_READ_ONLY", false)

        if (transactionToEdit != null) {
            setupEditMode(transactionToEdit!!, isReadOnly)
        } else {
            setupAddMode()
        }
    }

    private fun setupListeners() {
        // Klik Panah Kembali
        binding.ivBack.setOnClickListener { finish() }

        binding.rgType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_income) setupSpinner(categoriesIncome)
            else setupSpinner(categoriesExpense)
        }

        binding.btnSave.setOnClickListener {
            if (transactionToEdit == null) performAddTransaction()
            else performEditTransaction()
        }

        binding.btnDelete.setOnClickListener { showDeleteConfirmation() }
    }

    private fun setupAddMode() {
        binding.tvHeaderTitle.text = "Tambah Transaksi"
        binding.btnSave.text = "Simpan Transaksi"
        binding.btnDelete.visibility = View.GONE
        setupSpinner(categoriesExpense)
    }

    private fun setupEditMode(trx: Transaction, isReadOnly: Boolean) {
        binding.etAmount.setText(trx.amount.toString())

        if (trx.type == "Income") {
            binding.rbIncome.isChecked = true
            setupSpinner(categoriesIncome)
            binding.spinnerCategory.setSelection(categoriesIncome.indexOf(trx.category))
        } else {
            binding.rbExpense.isChecked = true
            setupSpinner(categoriesExpense)
            binding.spinnerCategory.setSelection(categoriesExpense.indexOf(trx.category))
        }

        if (isReadOnly) {
            // MODE BACA (DARI SEE ALL)
            binding.tvHeaderTitle.text = "Detail Transaksi"
            binding.btnSave.visibility = View.GONE
            binding.btnDelete.visibility = View.GONE

            // Tampilkan tanggal di paling bawah
            binding.lineDate.visibility = View.VISIBLE
            binding.tvLabelDate.visibility = View.VISIBLE
            binding.tvTransactionDate.visibility = View.VISIBLE
            binding.tvTransactionDate.text = trx.date

            // Kunci input
            binding.etAmount.isEnabled = false
            binding.spinnerCategory.isEnabled = false
            binding.rbIncome.isEnabled = false
            binding.rbExpense.isEnabled = false
        } else {
            // MODE EDIT (DARI HOME)
            binding.tvHeaderTitle.text = "Edit Transaksi"
            binding.btnSave.text = "Perbarui Transaksi"
            binding.btnDelete.visibility = View.VISIBLE
        }
    }

    private fun performAddTransaction() {
        val data = collectInputData() ?: return
        val newId = dbRef.push().key ?: return
        dbRef.child(newId).setValue(data.copy(id = newId)).addOnSuccessListener {
            Toast.makeText(this, "Berhasil disimpan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun performEditTransaction() {
        val data = collectInputData() ?: return
        val id = transactionToEdit?.id ?: return
        val updated = data.copy(id = id, date = transactionToEdit?.date ?: data.date, timestamp = transactionToEdit?.timestamp ?: data.timestamp)

        dbRef.child(id).setValue(updated).addOnSuccessListener {
            Toast.makeText(this, "Berhasil diperbarui", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun performDeleteTransaction() {
        transactionToEdit?.id?.let {
            dbRef.child(it).removeValue().addOnSuccessListener { finish() }
        }
    }

    private fun collectInputData(): Transaction? {
        val amount = binding.etAmount.text.toString()
        if (amount.isEmpty()) return null

        return Transaction(
            id = "",
            type = if (binding.rbIncome.isChecked) "Income" else "Expense" ,
            category = binding.spinnerCategory.selectedItem.toString(),
            amount = amount.toLong(),
            date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Hapus")
            .setMessage("Yakin ingin menghapus?")
            .setPositiveButton("Hapus") { _, _ -> performDeleteTransaction() }
            .setNegativeButton("Batal", null).show()
    }

    private fun setupSpinner(list: Array<String>) {
        binding.spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
    }
}