package com.pmob.expensestracker.features.transaction

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class NumberTextWatcher(private val editText: EditText) : TextWatcher {

    private var current = ""

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // Not used
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // Not used
    }

    override fun afterTextChanged(s: Editable?) {
        if (s.toString() != current) {
            editText.removeTextChangedListener(this)

            // 1. Strip off non-digits (remove existing dots)
            val cleanString = s.toString().replace("[^\\d]".toRegex(), "")

            if (cleanString.isNotEmpty()) {
                // 2. Parse the string to double
                val parsed = cleanString.toDouble()

                // 3. Format with Indonesian locale (uses dot as separator)
                val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))
                val formatted = formatter.format(parsed)

                current = formatted
                editText.setText(formatted)
                editText.setSelection(formatted.length) // Move cursor to end
            } else {
                current = ""
                editText.setText("") // Handle empty state
            }

            editText.addTextChangedListener(this)
        }
    }
}
