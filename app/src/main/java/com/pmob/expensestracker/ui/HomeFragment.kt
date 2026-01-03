package com.pmob.expensestracker.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pmob.expensestracker.adapter.TransactionAdapter
import com.pmob.expensestracker.databinding.FragmentHomeBinding
import com.pmob.expensestracker.features.transaction.AddTransactionActivity
import com.pmob.expensestracker.model.Transaction

/**
 * HomeFragment
 *
 * Fragment utama yang berfungsi sebagai Dashboard Keuangan.
 * Menampilkan ringkasan saldo, total pemasukan, pengeluaran,
 * serta daftar transaksi terbaru secara real-time.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    // Properti binding valid hanya di antara onCreateView dan onDestroyView
    private val binding get() = _binding!!

    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()

    // Konstanta ditempatkan di companion object agar bersifat statis dan hemat memori
    companion object {
        private const val DATABASE_URL = "https://pmobakhir-1279e-default-rtdb.asia-southeast1.firebasedatabase.app"
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUserProfile()
        setupClickListeners()
        setupRecyclerView()
        observeTransactionData()
    }

    /**
     * Menampilkan sapaan berdasarkan nama user yang sedang login.
     */
    private fun setupUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userName = currentUser?.displayName ?: "User"
        binding.tvGreeting.text = "Halo, $userName!"
    }

    /**
     * Mengatur semua event klik tombol navigasi.
     */
    private fun setupClickListeners() {
        // Tombol Tambah Transaksi (FAB)
        binding.fabAdd.setOnClickListener {
            navigateToActivity(AddTransactionActivity::class.java)
        }
    }

    /**
     * Inisialisasi RecyclerView dan Adapter.
     */
    private fun setupRecyclerView() {
        // Lambda function menangani klik item di RecyclerView (Edit Transaksi)
        transactionAdapter = TransactionAdapter(transactionList) { trx ->
            val intent = Intent(requireContext(), AddTransactionActivity::class.java)
            intent.putExtra("TRANSACTION_DATA", trx) // Mengirim objek Transaksi (Parcelable)
            startActivity(intent)
        }

        binding.rvRecentTransaction.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }
    }

    /**
     * Mengamati perubahan data transaksi dari Firebase Realtime Database.
     */
    private fun observeTransactionData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Menggunakan konstanta DATABASE_URL dari companion object
        val dbRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference("transactions").child(userId)

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Mencegah update UI jika fragment tidak lagi aktif
                if (!isAdded) return

                processTransactions(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database Error: ${error.message}")
            }
        })
    }

    /**
     * Memproses data snapshot dari Firebase:
     * 1. Menghitung total Income/Expense.
     * 2. Memperbarui List Transaksi.
     * 3. Mengatur visibilitas Empty State.
     */
    private fun processTransactions(snapshot: DataSnapshot) {
        transactionList.clear()
        var totalIncome = 0L
        var totalExpense = 0L

        if (snapshot.exists() && snapshot.childrenCount > 0) {
            // Tampilkan RecyclerView, Sembunyikan Layout Kosong
            toggleEmptyState(isEmpty = false)

            for (data in snapshot.children) {
                val trx = data.getValue(Transaction::class.java)
                trx?.let {
                    transactionList.add(it)
                    // Kalkulasi Saldo
                    if (it.type == "Income") {
                        totalIncome += it.amount
                    } else {
                        totalExpense += it.amount
                    }
                }
            }
            // Urutkan berdasarkan waktu (Terbaru di atas)
            transactionList.sortByDescending { it.timestamp }
        } else {
            // Tampilkan Layout Kosong, Sembunyikan RecyclerView
            toggleEmptyState(isEmpty = true)
        }

        // Update UI Dashboard & Adapter
        updateDashboardUI(totalIncome, totalExpense)
        transactionAdapter.notifyDataSetChanged()
    }

    /**
     * Mengatur visibilitas tampilan saat data kosong vs ada data.
     */
    private fun toggleEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvRecentTransaction.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvRecentTransaction.visibility = View.VISIBLE
        }
    }

    /**
     * Memperbarui teks ringkasan saldo pada Dashboard.
     */
    private fun updateDashboardUI(income: Long, expense: Long) {
        val currentBalance = income - expense
        binding.tvTotalBalance.text = "Rp ${formatCurrency(currentBalance)}"
        binding.tvIncome.text = "Rp ${formatCurrency(income)}"
        binding.tvExpense.text = "Rp ${formatCurrency(expense)}"
    }

    /**
     * Fungsi utilitas untuk navigasi activity sederhana.
     */
    private fun navigateToActivity(targetActivity: Class<*>) {
        startActivity(Intent(requireContext(), targetActivity))
    }

    /**
     * Format angka ke format mata uang (Ribuan dipisah titik).
     */
    private fun formatCurrency(number: Long): String {
        return String.format("%,d", number).replace(',', '.')
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}