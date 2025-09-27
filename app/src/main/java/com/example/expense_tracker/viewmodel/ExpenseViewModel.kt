package com.example.expense_tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expense_tracker.model.Expense
import com.example.expense_tracker.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // Expose expenses as StateFlow so Composables can observe
    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExpense(amount: Double, category: String, date: String) {
        viewModelScope.launch {
            repository.insert(Expense(amount = amount, category = category, date = date))
        }
    }
}
