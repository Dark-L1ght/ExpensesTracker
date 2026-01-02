package com.pmob.expensestracker.features.transaction

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pmob.expensestracker.R
import com.pmob.expensestracker.adapter.TransactionAdapter
import com.pmob.expensestracker.databinding.ActivityAllTransactionsBinding
import com.pmob.expensestracker.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AllTransactionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllTransactionsBinding
    private lateinit var adapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Semua Transaksi"

        setupRecyclerView()
        loadAllData()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
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
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvAllTransactions.visibility = View.VISIBLE

                    for (data in snapshot.children) {
                        data.getValue(Transaction::class.java)?.let { transactionList.add(it) }
                    }
                    transactionList.sortByDescending { it.timestamp }
                } else {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvAllTransactions.visibility = View.GONE
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_print_pdf -> {
                if (transactionList.isNotEmpty()) {
                    generatePdf(transactionList)
                } else {
                    Toast.makeText(this, "No transactions to print", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun generatePdf(transactions: List<Transaction>) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paints
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.GRAY
            textSize = 12f
        }
        val headerTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isFakeBoldText = true
        }
        val bodyTextPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }
        val amountTextPaint = Paint(bodyTextPaint).apply {
            textAlign = Paint.Align.RIGHT
        }
        val rowPaint = Paint().apply {
            style = Paint.Style.FILL
        }
        val linePaint = Paint().apply {
            strokeWidth = 1f
            color = Color.LTGRAY
        }

        // Dimensions
        val pageWidth = pageInfo.pageWidth
        val margin = 40f
        val topMargin = 50f
        val rowHeight = 25f
        val textPadding = 10f

        // Column positions
        val dateColX = margin
        val categoryColX = margin + 100
        val typeColX = margin + 300
        val amountColX = pageWidth - margin

        // --- Title & Subtitle ---
        canvas.drawText("Transaction Report", margin, topMargin, titlePaint)
        val reportDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $reportDate", margin, topMargin + 20, subtitlePaint)
        var yPosition = topMargin + 60

        // --- Table Header ---
        canvas.drawText("Date", dateColX + textPadding, yPosition, headerTextPaint)
        canvas.drawText("Category", categoryColX + textPadding, yPosition, headerTextPaint)
        canvas.drawText("Type", typeColX + textPadding, yPosition, headerTextPaint)
        headerTextPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Amount", amountColX - textPadding, yPosition, headerTextPaint)
        headerTextPaint.textAlign = Paint.Align.LEFT // reset align

        yPosition += 10
        canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint)
        yPosition += rowHeight

        // --- Table Body ---
        var totalIncome = 0L
        var totalExpense = 0L
        transactions.forEachIndexed { index, transaction ->
            // Zebra striping for rows
            if (index % 2 == 0) {
                rowPaint.color = Color.WHITE
            } else {
                rowPaint.color = Color.rgb(240, 240, 240) // Light gray
            }
            canvas.drawRect(margin, yPosition - rowHeight + (textPadding / 2), pageWidth - margin, yPosition + (textPadding / 2), rowPaint)

            val bodyTextY = yPosition - (rowHeight / 2) + (bodyTextPaint.textSize / 2)

            // Draw content
            canvas.drawText(transaction.date, dateColX + textPadding, bodyTextY, bodyTextPaint)
            canvas.drawText(transaction.category, categoryColX + textPadding, bodyTextY, bodyTextPaint)
            canvas.drawText(transaction.type, typeColX + textPadding, bodyTextY, bodyTextPaint)

            val formattedAmount = "%,d".format(transaction.amount)
            val amountColor = if(transaction.type == "Income") Color.rgb(2,100,56) else Color.RED
            amountTextPaint.color = amountColor
            canvas.drawText("Rp $formattedAmount", amountColX - textPadding, bodyTextY, amountTextPaint)

            if (transaction.type == "Income") {
                totalIncome += transaction.amount
            } else {
                totalExpense += transaction.amount
            }

            yPosition += rowHeight
        }
        canvas.drawLine(margin, yPosition - (rowHeight/2), pageWidth - margin, yPosition - (rowHeight/2), linePaint)

        // --- Summary ---
        yPosition += 40f
        val summaryLabelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            textAlign = Paint.Align.RIGHT
        }
        val summaryValuePaint = Paint(summaryLabelPaint).apply {
            isFakeBoldText = true
        }
        val summaryCurrencyPaint = Paint(summaryValuePaint).apply {
            textAlign = Paint.Align.LEFT
        }

        val summaryLabelX = pageWidth - margin - 200
        val summaryValueX = pageWidth - margin - textPadding
        val summaryCurrencyX = summaryLabelX + 15

        canvas.drawText("Total Income:", summaryLabelX, yPosition, summaryLabelPaint)
        summaryCurrencyPaint.color = Color.rgb(2,100,56)
        summaryValuePaint.color = Color.rgb(2,100,56)
        canvas.drawText("Rp", summaryCurrencyX, yPosition, summaryCurrencyPaint)
        canvas.drawText("%,d".format(totalIncome), summaryValueX, yPosition, summaryValuePaint)
        yPosition += 25f

        canvas.drawText("Total Expense:", summaryLabelX, yPosition, summaryLabelPaint)
        summaryCurrencyPaint.color = Color.RED
        summaryValuePaint.color = Color.RED
        canvas.drawText("Rp", summaryCurrencyX, yPosition, summaryCurrencyPaint)
        canvas.drawText("%,d".format(totalExpense), summaryValueX, yPosition, summaryValuePaint)
        yPosition += 10f
        canvas.drawLine(summaryLabelX - 20, yPosition, summaryValueX + textPadding, yPosition, linePaint)
        yPosition += 15f

        val balance = totalIncome - totalExpense
        val balanceValuePaint = Paint(summaryValuePaint)
        val balanceCurrencyPaint = Paint(summaryCurrencyPaint)
        balanceValuePaint.color = if (balance >= 0) Color.BLACK else Color.RED
        balanceCurrencyPaint.color = if (balance >= 0) Color.BLACK else Color.RED

        canvas.drawText("Final Balance:", summaryLabelX, yPosition, summaryLabelPaint)
        canvas.drawText("Rp", summaryCurrencyX, yPosition, balanceCurrencyPaint)
        canvas.drawText("%,d".format(balance), summaryValueX, yPosition, balanceValuePaint)

        // --- Finish ---
        document.finishPage(page)

        val filePath = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "transactions_report.pdf")
        try {
            FileOutputStream(filePath).use { document.writeTo(it) }
            Toast.makeText(this, "PDF report created successfully", Toast.LENGTH_SHORT).show()
            openPdf(filePath)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error creating PDF", Toast.LENGTH_SHORT).show()
        } finally {
            document.close()
        }
    }

    private fun openPdf(file: File) {
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No application available to view PDF", Toast.LENGTH_SHORT).show()
        }
    }
}