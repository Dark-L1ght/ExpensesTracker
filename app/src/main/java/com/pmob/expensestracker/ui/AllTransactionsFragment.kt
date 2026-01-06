package com.pmob.expensestracker.ui

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.*
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

        setupToolbar()
        setupRecyclerView()
        setupDateFilter()
        loadAllData()
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_print_pdf) {
                if (displayedTransactionList.isNotEmpty()) {
                    generatePdf(displayedTransactionList)
                } else {
                    Toast.makeText(requireContext(), "Tidak ada transaksi untuk dicetak", Toast.LENGTH_SHORT).show()
                }
                return@setOnMenuItemClickListener true
            }
            false
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
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("in", "ID"))
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
        return SimpleDateFormat("dd MMM yyyy", Locale("in", "ID")).parse(this) ?: Date()
    }

    private fun generatePdf(transactions: List<Transaction>) {
        if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) {
            Toast.makeText(requireContext(), "Penyimpanan eksternal tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val localeID = Locale("in", "ID")
        val sdf = SimpleDateFormat("dd MMMM yyyy", localeID)

        val leftMargin = 40f
        val rightMargin = 555f
        var yPosition = 40f

        // --- HEADER ---
        paint.textSize = 24f
        paint.isFakeBoldText = true
        paint.color = Color.BLACK
        canvas.drawText("Laporan Transaksi", leftMargin, yPosition, paint)
        yPosition += 25f

        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = Color.DKGRAY
        canvas.drawText("Dibuat pada: ${sdf.format(Date())}", leftMargin, yPosition, paint)
        yPosition += 15f

        val filterRange = binding.tvDateRangeFilter.text.toString()
        if (filterRange.isNotEmpty() && filterRange != "Semua Transaksi") {
            canvas.drawText("Periode: $filterRange", leftMargin, yPosition, paint)
        }
        yPosition += 40f

        // --- TABLE HEADERS ---
        paint.textSize = 12f
        paint.isFakeBoldText = true
        paint.color = Color.GRAY
        val dateX = leftMargin
        val categoryX = 130f
        val typeX = 280f
        val amountX = rightMargin

        canvas.drawText("Tanggal", dateX, yPosition, paint)
        canvas.drawText("Kategori", categoryX, yPosition, paint)
        canvas.drawText("Jenis", typeX, yPosition, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Jumlah", amountX, yPosition, paint)
        paint.textAlign = Paint.Align.LEFT
        yPosition += 10f

        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawLine(leftMargin, yPosition, rightMargin, yPosition, paint)
        yPosition += 25f

        // --- TABLE CONTENT ---
        paint.textSize = 12f
        paint.isFakeBoldText = false
        var totalIncome = 0.0
        var totalExpense = 0.0

        for (transaction in transactions) {
            val isIncome = transaction.type == "Income"
            paint.color = Color.BLACK

            canvas.drawText(transaction.date, dateX, yPosition, paint)
            canvas.drawText(transaction.category, categoryX, yPosition, paint)
            canvas.drawText(if (isIncome) "Pemasukan" else "Pengeluaran", typeX, yPosition, paint)

            val amount = transaction.amount.toDouble()
            val amountString: String
            paint.textAlign = Paint.Align.RIGHT

            if (isIncome) {
                paint.color = Color.rgb(26, 132, 78) // Professional Green
                amountString = String.format(localeID, "+ Rp %,.0f", amount)
                totalIncome += amount
            } else {
                paint.color = Color.rgb(208, 2, 27) // Professional Red
                amountString = String.format(localeID, "- Rp %,.0f", amount)
                totalExpense += amount
            }

            canvas.drawText(amountString, amountX, yPosition, paint)
            paint.textAlign = Paint.Align.LEFT
            yPosition += 20f
        }
        yPosition += 10f

        // --- SUMMARY ---
        paint.color = Color.LTGRAY
        canvas.drawLine(300f, yPosition, rightMargin, yPosition, paint)
        yPosition += 25f

        paint.textSize = 13f
        paint.color = Color.DKGRAY
        paint.isFakeBoldText = false
        val summaryLabelX = 300f

        canvas.drawText("Total Pemasukan", summaryLabelX, yPosition, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(26, 132, 78)
        canvas.drawText(String.format(localeID, "Rp %,.0f", totalIncome), rightMargin, yPosition, paint)
        paint.textAlign = Paint.Align.LEFT
        yPosition += 25f

        paint.color = Color.DKGRAY
        canvas.drawText("Total Pengeluaran", summaryLabelX, yPosition, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(208, 2, 27)
        canvas.drawText(String.format(localeID, "Rp %,.0f", totalExpense), rightMargin, yPosition, paint)
        paint.textAlign = Paint.Align.LEFT
        yPosition += 25f

        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText("Saldo Akhir", summaryLabelX, yPosition, paint)
        paint.textAlign = Paint.Align.RIGHT
        val finalBalance = totalIncome - totalExpense
        if (finalBalance < 0) {
            paint.color = Color.rgb(208, 2, 27)
        } else {
            paint.color = Color.BLACK
        }
        canvas.drawText(String.format(localeID, "Rp %,.0f", finalBalance), rightMargin, yPosition, paint)
        paint.textAlign = Paint.Align.LEFT

        // --- FOOTER ---
        yPosition = 810f
        paint.color = Color.GRAY
        paint.textSize = 8f
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Expenses Tracker - Halaman 1 dari 1", 595f / 2, yPosition, paint)

        // --- SAVE DOCUMENT ---
        pdfDocument.finishPage(page)

        val fileName = "Laporan_Transaksi_${System.currentTimeMillis()}.pdf"
        val documentsDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

        if (documentsDir == null) {
            Toast.makeText(requireContext(), "Tidak dapat mengakses direktori Dokumen", Toast.LENGTH_SHORT).show()
            pdfDocument.close()
            return
        }
        val file = File(documentsDir, fileName)

        try {
            FileOutputStream(file).use { fos ->
                pdfDocument.writeTo(fos)
            }
            Toast.makeText(requireContext(), "PDF berhasil disimpan di Dokumen", Toast.LENGTH_LONG).show()
            openPdf(file)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal membuat PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun openPdf(file: File) {
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Tidak ada aplikasi untuk membuka PDF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
