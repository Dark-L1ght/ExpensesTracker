package com.pmob.expensestracker.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pmob.expensestracker.R
import com.pmob.expensestracker.databinding.ItemTransactionBinding
import com.pmob.expensestracker.model.Transaction

/**
 * TransactionAdapter
 *
 * Adapter RecyclerView yang bertugas untuk menampilkan daftar transaksi keuangan.
 * Adapter ini menangani pengikatan data (binding) ke layout item_transaction
 * serta menangani logika tampilan visual berdasarkan kategori dan tipe transaksi.
 *
 * @property transactionList Daftar data transaksi yang akan ditampilkan.
 * @property onItemClick Fungsi callback untuk menangani interaksi klik pada item.
 */
class TransactionAdapter(
    private val transactionList: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    /**
     * ViewHolder untuk memegang referensi view menggunakan ViewBinding.
     */
    class TransactionViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactionList[position]

        with(holder.binding) {
            // Set data dasar
            tvCategory.text = transaction.category
            tvDate.text = transaction.date

            // Format uang (Contoh: 50000 -> 50.000)
            val formattedAmount = String.format("%,d", transaction.amount).replace(',', '.')

            // Panggil fungsi pembantu untuk mengatur ikon dan gaya visual
            setupTransactionIcon(this, transaction.category)
            setupTransactionStyle(this, transaction.type, formattedAmount)
        }

        // Listener untuk interaksi klik
        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }
    }

    override fun getItemCount(): Int = transactionList.size

    /**
     * Mengatur ikon kategori berdasarkan jenis kategori transaksi.
     */
    private fun setupTransactionIcon(binding: ItemTransactionBinding, category: String) {
        val iconRes = when (category) {
            "Makanan & Minuman" -> R.drawable.ic_food
            "Uang Kos/Kontrakan" -> R.drawable.ic_home
            "Transportasi/Bensin" -> R.drawable.ic_transport
            "Tugas/Alat Tulis/Print" -> R.drawable.ic_task
            "Pulsa/Data/Netflix" -> R.drawable.ic_dataa
            "Hiburan/Self Reward" -> R.drawable.ic_fun
            "Kiriman Orang Tua",
            "Gaji Part-time",
            "Beasiswa",
            "Project/Freelance",
            "Tabungan" -> R.drawable.ic_income
            else -> R.drawable.ic_others
        }
        binding.ivCategoryIcon.setImageResource(iconRes)
    }

    /**
     * Mengatur warna teks dan indikator visual berdasarkan tipe transaksi (Expense vs Income).
     */
    private fun setupTransactionStyle(
        binding: ItemTransactionBinding,
        type: String,
        amount: String
    ) {
        if (type.equals("Expense", ignoreCase = true)) {
            // Gaya untuk Pengeluaran (Merah)
            binding.tvAmount.text = "- Rp $amount"
            binding.tvAmount.setTextColor(Color.parseColor("#E53935"))
            binding.viewTypeIndicator.setBackgroundColor(Color.parseColor("#E53935"))
        } else {
            // Gaya untuk Pemasukan (Hijau)
            binding.tvAmount.text = "+ Rp $amount"
            binding.tvAmount.setTextColor(Color.parseColor("#2E7D32"))
            binding.viewTypeIndicator.setBackgroundColor(Color.parseColor("#2E7D32"))
        }
    }
}