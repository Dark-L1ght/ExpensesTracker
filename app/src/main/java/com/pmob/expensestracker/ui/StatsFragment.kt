package com.pmob.expensestracker.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pmob.expensestracker.adapter.TransactionAdapter
import com.pmob.expensestracker.databinding.FragmentStatsBinding
import com.pmob.expensestracker.model.Transaction

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTransactionData()
    }

    private fun loadTransactionData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseUrl =
            "https://expence-tra-4c82c-default-rtdb.asia-southeast1.firebasedatabase.app"

        val dbRef = FirebaseDatabase
            .getInstance(databaseUrl)
            .getReference("transactions")
            .child(userId)

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val expenseMap = mutableMapOf<String, Long>()
                val allExpenseList = mutableListOf<Transaction>()

                for (data in snapshot.children) {
                    val trx = data.getValue(Transaction::class.java)
                    if (trx?.type == "Expense") {
                        val category = trx.category ?: "Lainnya"
                        expenseMap[category] =
                            (expenseMap[category] ?: 0L) + trx.amount
                        allExpenseList.add(trx)
                    }
                }

                if (isAdded) {
                    setupPieChart(expenseMap)
                    allExpenseList.sortByDescending { it.timestamp }
                    setupRecyclerView(allExpenseList)
                    updateTopSpendingUI(expenseMap)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupRecyclerView(list: List<Transaction>) {
        binding.rvTopSpending.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = TransactionAdapter(list) {}
            isClickable = false
        }
    }

    private fun setupPieChart(expenseMap: Map<String, Long>) {

        val entries = ArrayList<PieEntry>()
        expenseMap.forEach { (category, amount) ->
            entries.add(PieEntry(amount.toFloat(), category))
        }

        val dataSet = PieDataSet(entries, "")

        val colors = ArrayList<Int>()
        ColorTemplate.MATERIAL_COLORS.forEach { colors.add(it) }
        ColorTemplate.VORDIPLOM_COLORS.forEach { colors.add(it) }
        dataSet.colors = colors

        // LABEL & NOMINAL DI LUAR PIE
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

        dataSet.valueLinePart1OffsetPercentage = 85f
        dataSet.valueLinePart1Length = 0.4f
        dataSet.valueLinePart2Length = 0.5f
        dataSet.valueLineColor = Color.GRAY

        dataSet.valueTextSize = 10f
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTypeface = android.graphics.Typeface.DEFAULT_BOLD

        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "Rp ${String.format("%,.0f", value).replace(',', '.')}"
            }
        }

        val data = PieData(dataSet)

        binding.pieChart.apply {
            this.data = data

            // NAMA KATEGORI
            setDrawEntryLabels(true)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)

            description.isEnabled = false

            // HILANGKAN LEGEND (KETERANGAN WARNA & KATEGORI)
            legend.isEnabled = false

            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 55f
            transparentCircleRadius = 60f

            centerText = "Pengeluaran"
            setCenterTextSize(16f)
            setCenterTextColor(Color.parseColor("#3F51B5"))

            setExtraOffsets(10f, 10f, 10f, 20f)

            animateY(1000)
            invalidate()
        }
    }

    private fun updateTopSpendingUI(expenseMap: Map<String, Long>) {
        if (expenseMap.isNotEmpty()) {
            val topEntry = expenseMap.maxByOrNull { it.value }
            binding.tvTopCategory.text = topEntry?.key
            binding.tvTopAmount.text =
                "Rp ${String.format("%,d", topEntry?.value).replace(',', '.')}"
        } else {
            binding.tvTopCategory.text = "Belum ada data"
            binding.tvTopAmount.text = "Rp 0"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
