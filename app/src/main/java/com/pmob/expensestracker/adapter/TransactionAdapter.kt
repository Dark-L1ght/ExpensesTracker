package com.pmob.expensestracker.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pmob.expensestracker.R
import com.pmob.expensestracker.databinding.ItemTransactionBinding
import com.pmob.expensestracker.model.Transaction

class TransactionAdapter(
    private val transactionList: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit // TAMBAHKAN INI: Fungsi untuk menangani klik
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

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
        val trx = transactionList[position]

        // 1. Set Data ke UI
        holder.binding.tvCategory.text = trx.category
        holder.binding.tvDate.text = trx.date

        val formattedAmount = String.format("%,d", trx.amount).replace(',', '.')

        // 2. Set Icon berdasarkan kategori
        val iconRes = when (trx.category) {
            "Makanan & Minuman" -> R.drawable.ic_food
            "Uang Kos/Kontrakan" -> R.drawable.ic_home
            "Transportasi/Bensin" -> R.drawable.ic_transport
            "Tugas/Alat Tulis/Print" -> R.drawable.ic_task
            "Pulsa/Data/Netflix" -> R.drawable.ic_dataa
            "Hiburan/Self Reward" -> R.drawable.ic_fun
            "Kiriman Orang Tua", "Gaji Part-time", "Beasiswa", "Project/Freelance", "Tabungan" -> R.drawable.ic_income
            else -> R.drawable.ic_others
        }
        holder.binding.ivCategoryIcon.setImageResource(iconRes)

        // 3. Set Warna & Indikator (Expense/Income)
        if (trx.type == "Expense") {
            holder.binding.tvAmount.text = "- Rp $formattedAmount"
            holder.binding.tvAmount.setTextColor(Color.parseColor("#E53935"))
            holder.binding.viewTypeIndicator.setBackgroundColor(Color.parseColor("#E53935"))
        } else {
            holder.binding.tvAmount.text = "+ Rp $formattedAmount"
            holder.binding.tvAmount.setTextColor(Color.parseColor("#2E7D32"))
            holder.binding.viewTypeIndicator.setBackgroundColor(Color.parseColor("#2E7D32"))
        }

        // 4. LOGIKA KLIK: Mengirim data transaksi ke Activity tujuan
        holder.itemView.setOnClickListener {
            onItemClick(trx)
        }
    }

    override fun getItemCount(): Int = transactionList.size
}