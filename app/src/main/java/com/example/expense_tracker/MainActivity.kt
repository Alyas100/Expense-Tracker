package com.example.expense_tracker

import com.example.expense_tracker.viewmodel.ExpenseViewModel
import com.example.expense_tracker.viewmodel.ExpenseViewModelFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expense_tracker.data.ExpenseDatabase
import com.example.expense_tracker.repository.ExpenseRepository
import com.example.expense_tracker.ui.theme.ExpenseTrackerTheme
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {

                        ExpenseTrackerApp()
                    }
                }
            }
        }


@Composable
fun ExpenseTrackerApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val db = ExpenseDatabase.getDatabase(context)
    val repository = ExpenseRepository(db.expenseDao())
    val factory = remember { ExpenseViewModelFactory(repository) }
    val sharedViewModel: ExpenseViewModel = viewModel(factory = factory)

    val items = listOf(
        BottomNavItem("Add", Icons.Default.Add, "add"),
        BottomNavItem("History", Icons.Default.List, "history"),
        BottomNavItem("Dashboard", Icons.Default.PlayArrow, "dashboard")
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryFlow.collectAsState(initial = null).value?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "add",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("add") { AddExpenseScreen() }
            composable("history") { HistoryScreen(sharedViewModel) }
            composable("dashboard") { DashboardScreen(sharedViewModel) }
        }
    }
}

data class BottomNavItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String)

@Composable
fun AddExpenseScreen() {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Food", "Transport", "Shopping", "Other")

    // choosing between the older and new version of showing date
    // for device that not support api 26, it will show
    fun getCurrentDateString(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now().toString()
        } else {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.format(Calendar.getInstance().time)
        }
    }

    var date by remember {
        // Call the function to get the initial value
        mutableStateOf(getCurrentDateString())
    }
    val focusManager = LocalFocusManager.current // Used to remove focus when menu opens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Amount input
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Category dropdown (STABLE VERSION)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.TopStart)
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown Arrow",
                        modifier = Modifier.clickable { expanded = true } // Icon also opens the menu
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { expanded = true })
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            focusManager.clearFocus()
                        }
                    }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            category = option
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Date: $date")

        Spacer(Modifier.height(16.dp))

        val context = LocalContext.current
        val db = ExpenseDatabase.getDatabase(context)
        val repository = ExpenseRepository(db.expenseDao())
        val viewModel = remember { ExpenseViewModel(repository) }


        // Save button
        Button(
            onClick = {
                // Convert string input to Double
                val amt = amount.toDoubleOrNull() ?: 0.0

                // call func to save
                viewModel.addExpense(amt, category, date)

                Log.d("Expense", "Saved: $amount, $category, $date")


                // clear input after save
                amount = ""
                category = "Food"



            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Expense")
        }
    }
}


@Composable
fun HistoryScreen(viewModel: ExpenseViewModel) {
    val expenses = viewModel.allExpenses.collectAsState()

    if (expenses.value.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No expenses yet")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(expenses.value) { expense ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Amount: ${expense.amount}")
                        Text("Category: ${expense.category}")
                        Text("Date: ${expense.date}")
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: ExpenseViewModel) {
    val expenses = viewModel.allExpenses.collectAsState()

    val total = expenses.value.sumOf { it.amount }
    val groupedByCategory = expenses.value.groupBy { it.category }
    val categoryTotals = groupedByCategory.mapValues { entry ->
        entry.value.sumOf { it.amount }
    }

    // Group by date for bar chart
    val groupedByDate = expenses.value.groupBy { it.date }
    val dateTotals = groupedByDate.mapValues { entry ->
        entry.value.sumOf { it.amount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)

        // Show total
        Text("Total Expenses: RM$total")

        Spacer(Modifier.height(16.dp))

        // ---- PIE CHART (Category Breakdown) ----
        Text("By Category", style = MaterialTheme.typography.titleMedium)
        AndroidView(
            factory = { context ->
                PieChart(context).apply {
                    val entries = categoryTotals.map { (category, sum) ->
                        PieEntry(sum.toFloat(), category)
                    }
                    val dataSet = PieDataSet(entries, "Categories").apply {
                        colors = ColorTemplate.MATERIAL_COLORS.toList()
                        valueTextSize = 14f
                    }
                    data = PieData(dataSet)
                    description.isEnabled = false
                    legend.isEnabled = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )

        Spacer(Modifier.height(16.dp))

        // ---- BAR CHART (Daily Breakdown) ----
        Text("By Date", style = MaterialTheme.typography.titleMedium)
        AndroidView(
            factory = { context ->
                BarChart(context).apply {
                    val entries = dateTotals.entries.mapIndexed { index, entry ->
                        BarEntry(index.toFloat(), entry.value.toFloat())
                    }
                    val dataSet = BarDataSet(entries, "Expenses by Day").apply {
                        colors = ColorTemplate.MATERIAL_COLORS.toList()
                        valueTextSize = 12f
                    }
                    data = BarData(dataSet)
                    xAxis.valueFormatter = IndexAxisValueFormatter(dateTotals.keys.toList())
                    xAxis.granularity = 1f
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    axisRight.isEnabled = false
                    description.isEnabled = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )
    }
}


// PREVIEW--
@Preview(showBackground = true)
@Composable
fun PreviewExpenseTrackerApp() {
    ExpenseTrackerTheme {
        ExpenseTrackerApp()
    }
}

