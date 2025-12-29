package com.pmob.expensestracker.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Transaction(
    val id: String = "",
    val type: String = "",
    val category: String = "",
    val amount: Long = 0,
    val date: String = "",
    val timestamp: Long = 0
) : Parcelable