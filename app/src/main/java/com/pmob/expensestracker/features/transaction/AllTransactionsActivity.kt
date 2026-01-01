package com.pmob.expensestracker.features.transaction

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pmob.expensestracker.adapter.TransactionAdapter
import com.pmob.expensestracker.databinding.ActivityAllTransactionsBinding
import com.pmob.expensestracker.model.Transaction

class AllTransactionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllTransactionsBinding
    private lateinit var adapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadAllData()
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(transactionList) { trx ->
            val intent = Intent(this, AddTransactionActivity::class.java)
            intent.putExtra("TRANSACTION_DATA", trx)
            intent.putExtra("IS_READ_ONLY", true)
            startActivity(intent)
        }
        binding.rvAllTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvAllTransactions.adapter = adapter
    }

    private fun loadAllData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseUrl =
            "https://pmobakhir-1279e-default-rtdb.asia-southeast1.firebasedatabase.app"
        val dbRef =
            FirebaseDatabase.getInstance(databaseUrl).getReference("transactions").child(userId)

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transactionList.clear()

                if (snapshot.exists() && snapshot.childrenCount > 0) {
                    // Data tersedia
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvAllTransactions.visibility = View.VISIBLE

                    for (data in snapshot.children) {
                        data.getValue(Transaction::class.java)?.let { transactionList.add(it) }
                    }
                    transactionList.sortByDescending { it.timestamp }
                } else {
                    // Data kosong
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvAllTransactions.visibility = View.GONE
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}