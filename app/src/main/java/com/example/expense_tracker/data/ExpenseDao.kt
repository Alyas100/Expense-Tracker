package com.example.expense_tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.expense_tracker.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>
}
