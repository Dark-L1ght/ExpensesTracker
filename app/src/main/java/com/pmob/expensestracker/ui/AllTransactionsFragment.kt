package com.pmob.expensestracker.ui

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pmob.expensestracker.R
import com.pmob.expensestracker.adapter.TransactionAdapter
import com.pmob.expensestracker.databinding.FragmentAllTransactionsBinding
import com.pmob.expensestracker.features.transaction.AddTransactionActivity
import com.pmob.expensestracker.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AllTransactionsFragment : Fragment() {

    private var _binding: FragmentAllTransactionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TransactionAdapter
    private val fullTransactionList = mutableListOf<Transaction>()
    private val displayedTransactionList = mutableListOf<Transaction>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbarMenu()
        setupRecyclerView()
        setupDateFilter()
        loadAllData()
    }

    private fun setupToolbarMenu() {
        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_print_pdf -> {
                    if (displayedTransactionList.isNotEmpty()) {
                        generatePdf(displayedTransactionList)
                    } else {
                        Toast.makeText(requireContext(), "Tidak ada transaksi untuk dicetak", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(displayedTransactionList) { trx ->
            val intent = Intent(requireContext(), AddTransactionActivity::class.java)
            intent.putExtra(AddTransactionActivity.EXTRA_TRANSACTION, trx)
            intent.putExtra(AddTransactionActivity.EXTRA_READ_ONLY, true)
            startActivity(intent)
        }
        binding.rvAllTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllTransactions.adapter = adapter
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
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, month - 1)
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.DAY_OF_MONTH, 1)

                val start = cal.clone() as Calendar
                start.set(Calendar.DAY_OF_MONTH, 1)
                val end = cal.clone() as Calendar
                end.add(Calendar.MONTH, 1)
                end.add(Calendar.DAY_OF_MONTH, -1)

                applyFilter(start, end, "${monthPicker.displayedValues[month - 1]} $year")
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDateRangePickerDialog() {
        val calendar = Calendar.getInstance()

        val startDatePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val startDate = Calendar.getInstance()
                startDate.set(year, month, dayOfMonth)

                val endDatePickerDialog = DatePickerDialog(
                    requireContext(),
                    { _, endYear, endMonth, endDayOfMonth ->
                        val endDate = Calendar.getInstance()
                        endDate.set(endYear, endMonth, endDayOfMonth)
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        applyFilter(startDate, endDate, "${sdf.format(startDate.time)} - ${sdf.format(endDate.time)}")
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                endDatePickerDialog.datePicker.minDate = startDate.timeInMillis
                endDatePickerDialog.setTitle("Pilih Tanggal Selesai")
                endDatePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        startDatePickerDialog.setTitle("Pilih Tanggal Mulai")
        startDatePickerDialog.show()
    }

    private fun applyFilter(startDate: Calendar?, endDate: Calendar?, filterLabel: String) {
        binding.tvDateRangeFilter.text = filterLabel
        val filteredList = if (startDate != null && endDate != null) {
            val start = startDate.clone() as Calendar
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)

            val end = endDate.clone() as Calendar
            end.set(Calendar.HOUR_OF_DAY, 0)
            end.set(Calendar.MINUTE, 0)
            end.set(Calendar.SECOND, 0)
            end.set(Calendar.MILLISECOND, 0)
            end.add(Calendar.DAY_OF_YEAR, 1)

            fullTransactionList.filter {
                val trxDate = it.date.toDate()
                !trxDate.before(start.time) && trxDate.before(end.time)
            }
        } else {
            fullTransactionList
        }
        updateDisplayedTransactions(filteredList)
    }

    private fun updateDisplayedTransactions(transactions: List<Transaction>) {
        displayedTransactionList.clear()
        displayedTransactionList.addAll(transactions)
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }

        if (displayedTransactionList.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvAllTransactions.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvAllTransactions.visibility = View.VISIBLE
        }
    }

    private fun loadAllData() {
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
                // Initial load with all transactions
                applyFilter(null, null, "Semua Transaksi")
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun String.toDate(): Date {
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(this) ?: Date()
    }

    private fun generatePdf(transactions: List<Transaction>) {
        // Omitted for brevity
    }

    private fun openPdf(file: File) {
        // Omitted for brevity
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}