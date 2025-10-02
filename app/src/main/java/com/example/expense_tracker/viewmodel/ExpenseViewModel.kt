package com.example.expense_tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expense_tracker.model.Expense
import com.example.expense_tracker.BuildConfig
import com.example.expense_tracker.repository.ExpenseRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {


    // --- Gemini AI Integration ---

    // 1. Initialize the generative model
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY // Securely access the key
    )

    // 2. StateFlow to hold the AI response and loading state
    private val _aiAdvice = MutableStateFlow<String?>(null)
    val aiAdvice: StateFlow<String?> = _aiAdvice.asStateFlow()

    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading.asStateFlow()

    fun getAIAdvice(expenses: List<Expense>, goal: String) {
        _isAILoading.value = true
        _aiAdvice.value = null // Clear previous advice

        viewModelScope.launch {
            try {
                // Prepare the data for the prompt
                val monthlyExpenses = expenses.filter {
                    try {
                        val calendar = Calendar.getInstance()
                        val expenseCal = Calendar.getInstance()
                        val sdf = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        )
                        sdf.parse(it.date)?.let { date ->
                            expenseCal.time = date
                            return@filter expenseCal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                                    expenseCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
                        }
                        false
                    } catch (e: Exception) { false }
                }

                val expenseSummary = if (monthlyExpenses.isNotEmpty()) {
                    monthlyExpenses.groupBy { it.category }
                        .mapValues { it.value.sumOf { exp -> exp.amount } }
                        .entries.joinToString("\n") { "- ${it.key}: RM${"%.2f".format(it.value)}" }
                } else {
                    "No expenses recorded yet for this month."
                }

                // Build the prompt
                val prompt = """
                    You are a friendly financial advisor for a mobile expense tracker app.
                    My current financial goal is: "$goal".

                    Here is my spending summary for the current month:
                    $expenseSummary

                    Based on this, provide a short (2-3 sentences), encouraging, and actionable piece of advice.
                    If there's no spending, just provide general encouragement.
                """.trimIndent()

                // Call the API
                val response = generativeModel.generateContent(prompt)
                _aiAdvice.value = response.text

            } catch (e: Exception) {
                // Handle potential errors (network, API key issues, etc.)
                _aiAdvice.value = "Sorry, I couldn't generate advice right now. Please check your connection or API key."
                e.printStackTrace() // Log the error for debugging
            } finally {
                _isAILoading.value = false
            }
        }
    }



    // Expose expenses as StateFlow so Composables can observe
    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExpense(amount: Double, category: String, date: String) {
        viewModelScope.launch {
            repository.insert(Expense(amount = amount, category = category, date = date))
        }
    }
}
