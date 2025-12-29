package com.pmob.expensestracker.ui

import com.pmob.expensestracker.model.Transaction
import android.content.Intent
import android.os.Bundle
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
import com.pmob.expensestracker.features.transaction.AllTransactionsActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        binding.tvGreeting.text = "Halo, ${currentUser?.displayName ?: "User"}!"

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(context, AddTransactionActivity::class.java))
        }

        binding.tvSeeAll.setOnClickListener {
            startActivity(Intent(requireContext(), AllTransactionsActivity::class.java))
        }

        setupRecyclerView()
        loadDataFromFirebase()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(transactionList) { trx ->
            val intent = Intent(requireContext(), AddTransactionActivity::class.java)
            intent.putExtra("TRANSACTION_DATA", trx)
            startActivity(intent)
        }

        binding.rvRecentTransaction.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }
    }

    private fun loadDataFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseUrl = "https://expence-tra-4c82c-default-rtdb.asia-southeast1.firebasedatabase.app"
        val dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("transactions").child(userId)

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transactionList.clear()
                var income = 0L
                var expense = 0L

                // LOGIKA: Periksa apakah ada data di snapshot
                if (snapshot.exists() && snapshot.childrenCount > 0) {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvRecentTransaction.visibility = View.VISIBLE

                    for (data in snapshot.children) {
                        val trx = data.getValue(Transaction::class.java)
                        trx?.let {
                            transactionList.add(it)
                            if (it.type == "Income") income += it.amount
                            else expense += it.amount
                        }
                    }
                    // Urutkan transaksi terbaru di atas
                    transactionList.sortByDescending { it.timestamp }
                } else {
                    // Tampilkan pesan "No Entry Data" jika database kosong
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvRecentTransaction.visibility = View.GONE
                }

                // Update UI Ringkasan Saldo
                binding.tvTotalBalance.text = "Rp ${formatRupiah(income - expense)}"
                binding.tvIncome.text = "Rp ${formatRupiah(income)}"
                binding.tvExpense.text = "Rp ${formatRupiah(expense)}"

                transactionAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Fungsi pembantu untuk format ribuan
    private fun formatRupiah(number: Long): String {
        return String.format("%,d", number).replace(',', '.')
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}