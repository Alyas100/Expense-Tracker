package com.example.expense_tracker.repository

import com.example.expense_tracker.data.ExpenseDao
import com.example.expense_tracker.model.Expense
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val dao: ExpenseDao) {
    val allExpenses: Flow<List<Expense>> = dao.getAllExpenses()

    suspend fun insert(expense: Expense) {
        dao.insertExpense(expense)
    }
}
