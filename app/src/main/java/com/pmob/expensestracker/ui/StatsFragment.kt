package com.pmob.expensestracker.ui

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pmob.expensestracker.R
import com.pmob.expensestracker.adapter.TransactionAdapter
import com.pmob.expensestracker.databinding.FragmentStatsBinding
import com.pmob.expensestracker.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val fullTransactionList = mutableListOf<Transaction>()
    private var activeStartDate: Calendar? = null
    private var activeEndDate: Calendar? = null

    // Custom Color Palettes with better contrast
    private val expenseColors = listOf(
        Color.parseColor("#EF5350"), // Red
        Color.parseColor("#FF7043"), // Deep Orange
        Color.parseColor("#78909C"), // Blue Grey
        Color.parseColor("#AB47BC"), // Purple
        Color.parseColor("#8D6E63"), // Brown
        Color.parseColor("#26A69A")  // Teal
    )
    private val incomeColors = listOf(
        Color.parseColor("#66BB6A"), // Green
        Color.parseColor("#29B6F6"), // Light Blue
        Color.parseColor("#FFCA28"), // Amber
        Color.parseColor("#26C6DA"), // Cyan
        Color.parseColor("#7986CB")  // Indigo
    )

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
        setupTabs()
        setupChartToggle()
        setupDateFilter()
        loadTransactionData()
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Pengeluaran"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Pemasukan"))
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { updateVisuals() }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupChartToggle() {
        binding.toggleChartType.check(R.id.btn_pie_chart)
        binding.toggleChartType.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if(isChecked) {
                updateVisuals()
            }
        }
    }

    private fun setupDateFilter() {
        binding.tvDateRangeFilter.setOnClickListener { showDateFilterDialog() }
    }

    private fun showDateFilterDialog() {
        val options = arrayOf("Hari ini", "7 hari terakhir", "Pilih bulan", "Pilih rentang tanggal", "Tampilkan Semua")
        AlertDialog.Builder(requireContext())
            .setTitle("Filter Transaksi")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> filterToday()
                    1 -> filterLast7Days()
                    2 -> showMonthYearPickerDialog()
                    3 -> showDateRangePickerDialog()
                    4 -> applyFilter(null, null, "Semua Transaksi")
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun filterToday() {
        val calendar = Calendar.getInstance()
        applyFilter(calendar, calendar, "Hari ini")
    }

    private fun filterLast7Days() {
        val calendar = Calendar.getInstance()
        val end = calendar.clone() as Calendar
        val start = calendar.clone() as Calendar
        start.add(Calendar.DAY_OF_YEAR, -6)
        applyFilter(start, end, "7 hari terakhir")
    }

    private fun showMonthYearPickerDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_year_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.picker_month)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.picker_year)
        val calendar = Calendar.getInstance()

        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = calendar.get(Calendar.MONTH) + 1
        monthPicker.displayedValues = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

        val currentYear = calendar.get(Calendar.YEAR)
        yearPicker.minValue = currentYear - 10
        yearPicker.maxValue = currentYear
        yearPicker.value = currentYear

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Bulan dan Tahun")
            .setView(dialogView)
            .setPositiveButton("Pilih") { _, _ ->
                val month = monthPicker.value
                val year = yearPicker.value
                val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
                val start = cal.clone() as Calendar
                val end = cal.clone() as Calendar
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                applyFilter(start, end, "${monthPicker.displayedValues[month - 1]} $year")
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDateRangePickerDialog() {
        val calendar = Calendar.getInstance()
        val startDatePickerDialog = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            val startDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            val endDatePickerDialog = DatePickerDialog(requireContext(), { _, endYear, endMonth, endDayOfMonth ->
                val endDate = Calendar.getInstance().apply { set(endYear, endMonth, endDayOfMonth) }
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                applyFilter(startDate, endDate, "${sdf.format(startDate.time)} - ${sdf.format(endDate.time)}")
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            endDatePickerDialog.datePicker.minDate = startDate.timeInMillis
            endDatePickerDialog.setTitle("Pilih Tanggal Selesai")
            endDatePickerDialog.show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        startDatePickerDialog.setTitle("Pilih Tanggal Mulai")
        startDatePickerDialog.show()
    }

    private fun applyFilter(startDate: Calendar?, endDate: Calendar?, filterLabel: String) {
        binding.tvDateRangeFilter.text = filterLabel
        activeStartDate = startDate
        activeEndDate = endDate
        updateVisuals()
    }

    private fun loadTransactionData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseUrl = "https://pmobakhir-1279e-default-rtdb.asia-southeast1.firebasedatabase.app"
        val dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("transactions").child(userId)

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                fullTransactionList.clear()
                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        data.getValue(Transaction::class.java)?.let { fullTransactionList.add(it) }
                    }
                    fullTransactionList.sortByDescending { it.timestamp }
                }
                updateVisuals()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateVisuals() {
        val filteredList = if (activeStartDate != null && activeEndDate != null) {
            val startDate = activeStartDate!!.clone() as Calendar
            startDate.set(Calendar.HOUR_OF_DAY, 0)
            startDate.set(Calendar.MINUTE, 0)
            startDate.set(Calendar.SECOND, 0)
            startDate.set(Calendar.MILLISECOND, 0)

            val endDate = activeEndDate!!.clone() as Calendar
            endDate.set(Calendar.HOUR_OF_DAY, 0)
            endDate.set(Calendar.MINUTE, 0)
            endDate.set(Calendar.SECOND, 0)
            endDate.set(Calendar.MILLISECOND, 0)
            endDate.add(Calendar.DAY_OF_YEAR, 1)

            fullTransactionList.filter { 
                val trxDate = it.date.toDate()
                !trxDate.before(startDate.time) && trxDate.before(endDate.time)
             }
        } else {
            fullTransactionList
        }

        val expenseMap = filteredList.filter { it.type == "Expense" }.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
        val incomeMap = filteredList.filter { it.type == "Income" }.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }

        val isExpenseTab = binding.tabLayout.selectedTabPosition == 0
        val dataMap = if (isExpenseTab) expenseMap else incomeMap
        val title = if (isExpenseTab) "Pengeluaran" else "Pemasukan"
        
        val listToShow = if (isExpenseTab) {
            expenseMap.toList().sortedByDescending { it.second }.map { Transaction(category = it.first, amount = it.second, type = "Expense") } 
        } else {
            incomeMap.toList().sortedByDescending { it.second }.map { Transaction(category = it.first, amount = it.second, type = "Income") }
        }

        if (binding.toggleChartType.checkedButtonId == R.id.btn_pie_chart) {
            binding.pieChart.visibility = View.VISIBLE
            binding.barChart.visibility = View.GONE
            setupPieChart(binding.pieChart, dataMap, title)
        } else {
            binding.pieChart.visibility = View.GONE
            binding.barChart.visibility = View.VISIBLE
            setupStackedBarChart(binding.barChart, dataMap, title)
        }
        setupRecyclerView(listToShow)
    }

    private fun setupRecyclerView(list: List<Transaction>) {
        binding.rvTransactionsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = TransactionAdapter(list) {}
            isClickable = false
        }
    }

    private fun setupPieChart(pieChart: PieChart, dataMap: Map<String, Long>, title: String) {
        if (dataMap.isEmpty()) {
            pieChart.visibility = View.GONE
            binding.tvEmptyChart.visibility = View.VISIBLE
            return
        }
        pieChart.visibility = View.VISIBLE
        binding.tvEmptyChart.visibility = View.GONE

        val entries = ArrayList<PieEntry>()
        dataMap.forEach { (category, amount) -> entries.add(PieEntry(amount.toFloat(), category)) }

        val dataSet = PieDataSet(entries, "").apply {
            colors = if (title == "Pengeluaran") expenseColors else incomeColors
            setDrawValues(true)
        }

        pieChart.setUsePercentValues(true)

        val pieData = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChart))
            setValueTextSize(12f)
            setValueTextColor(Color.WHITE)
        }

        pieChart.apply {
            data = pieData
            setDrawEntryLabels(false)
            description.isEnabled = false

            val totalAmount = dataMap.values.sum()
            val formattedTotal = "Rp ${String.format("%,d", totalAmount).replace(',', '.')}"
            centerText = SpannableString("$title\n$formattedTotal").apply { setSpan(RelativeSizeSpan(1.4f), 0, title.length, 0) }
            setCenterTextColor(if (title == "Pengeluaran") Color.RED else Color.rgb(2, 100, 56))

            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.isWordWrapEnabled = true

            invalidate()
        }
    }

    private fun setupStackedBarChart(barChart: BarChart, dataMap: Map<String, Long>, title: String) {
        if (dataMap.isEmpty()) {
            barChart.visibility = View.GONE
            binding.tvEmptyChart.visibility = View.VISIBLE
            return
        }
        barChart.visibility = View.VISIBLE
        binding.tvEmptyChart.visibility = View.GONE

        val categories = dataMap.keys.toList()
        val amounts = dataMap.values.map { it.toFloat() }.toFloatArray()

        val entries = listOf(BarEntry(0f, amounts))

        val dataSet = BarDataSet(entries, "").apply {
            stackLabels = categories.toTypedArray()
            colors = if (title == "Pengeluaran") expenseColors else incomeColors
            setDrawValues(false)
        }

        barChart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            setDrawGridBackground(false)

            xAxis.setDrawGridLines(false)
            xAxis.setDrawLabels(false)

            axisLeft.setDrawGridLines(false)
            axisLeft.axisMinimum = 0f

            axisRight.isEnabled = false

            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)

            setFitBars(true)
            animateY(1000)
            invalidate()
        }
    }

    private fun String.toDate(): Date {
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(this) ?: Date()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}