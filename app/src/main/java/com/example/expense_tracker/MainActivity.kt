package com.example.expense_tracker

import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import com.example.expense_tracker.viewmodel.ExpenseViewModel
import com.example.expense_tracker.viewmodel.ExpenseViewModelFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
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
            composable("dashboard") { DashboardScreen(viewModel = sharedViewModel, navController = navController) }
            composable("gamification") { GamificationScreen(viewModel = sharedViewModel) }
            composable("insight_dashboard") { InsightDashboardScreen(viewModel = sharedViewModel) }
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
fun GamificationScreen(viewModel: ExpenseViewModel) {
    val expenses = viewModel.allExpenses.collectAsState(initial = emptyList()).value

    // --- Gamification Settings ---
    val dailyBudget = 50.0
    val weeklySavingsGoal = 50.0
    val weeklyFoodBudgetGoal = 75.0

    // --- Gamification Logic ---
    val today: LocalDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LocalDate.now()
    } else {
        LocalDate.parse(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time))
    }

    val expensesByDate = expenses.groupBy { it.date }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    var streak = 0
    var checkDate = today
    val todaysTotal = expensesByDate[checkDate.toString()] ?: 0.0
    if (todaysTotal in 0.01..dailyBudget) {
        streak++
        checkDate = checkDate.minusDays(1)
    } else {
        checkDate = checkDate.minusDays(1)
    }

    while (true) {
        val dailyTotal = expensesByDate[checkDate.toString()] ?: 0.0
        if (dailyTotal in 0.01..dailyBudget) {
            streak++
            checkDate = checkDate.minusDays(1)
        } else {
            break
        }
    }

    val calendar = Calendar.getInstance()
    val currentWeekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
    val currentYear = calendar.get(Calendar.YEAR)

    val weeklyExpenses = expenses.filter {
        try {
            val expenseCal = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.parse(it.date)?.let { date ->
                expenseCal.time = date
                return@filter expenseCal.get(Calendar.WEEK_OF_YEAR) == currentWeekNumber &&
                        expenseCal.get(Calendar.YEAR) == currentYear
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    val weeklyBudget = dailyBudget * 7
    val weeklySavings = weeklyBudget - weeklyExpenses.sumOf { it.amount }
    val saved50ThisWeek = weeklySavings >= weeklySavingsGoal
    val has3DayStreak = streak >= 3
    val weeklyFoodSpending = weeklyExpenses
        .filter { it.category.equals("Food", ignoreCase = true) }
        .sumOf { it.amount }
    val isFrugalFoodie = weeklyFoodSpending > 0 && weeklyFoodSpending <= weeklyFoodBudgetGoal

    // --- Gamification UI ---
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("My Badges & Streaks", style = MaterialTheme.typography.headlineMedium)
        }

        // --- Gamification Streak Card ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Daily Budget Streak", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Streak Icon",
                        modifier = Modifier.size(48.dp),
                        tint = if (streak > 0) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Text(
                        text = "$streak Days",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (streak > 0) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Text(
                        text = when {
                            streak > 1 -> "Great job! Keep it up!"
                            streak == 1 -> "You're on the board! One day down."
                            else -> "Stay under your budget to start a new streak!"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Today's spending: RM${"%.2f".format(todaysTotal)} / RM${"%.2f".format(dailyBudget)}")
                }
            }
        }

        // --- Gamification Achievements Card ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your Achievements", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    BadgeCard(
                        title = "Saver of the Week",
                        description = "Save at least RM${"%.2f".format(weeklySavingsGoal)} in a week.",
                        earned = saved50ThisWeek
                    )
                    BadgeCard(
                        title = "3-Day Streak",
                        description = "Stay under budget for 3 consecutive days.",
                        earned = has3DayStreak
                    )
                    BadgeCard(
                        title = "Frugal Foodie",
                        description = "Keep weekly food spending under RM${"%.2f".format(weeklyFoodBudgetGoal)}.",
                        earned = isFrugalFoodie
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: ExpenseViewModel, navController: NavController) {// Make sure you have an initial value for the state
    val expenses = viewModel.allExpenses.collectAsState(initial = emptyList()).value

    // Basic calculations for the free dashboard
    val totalExpenses = expenses.sumOf { it.amount }
    val categoryTotals = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)

        // --- Total Expenses Card ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Expenses", style = MaterialTheme.typography.titleLarge)
                Text(
                    "RM${"%.2f".format(totalExpenses)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- Basic Category Breakdown Card ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Category Breakdown", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                if (categoryTotals.isEmpty()) {
                    Text("No expenses yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    categoryTotals.forEach { (category, sum) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(category, style = MaterialTheme.typography.bodyLarge)
                            Text("RM${"%.2f".format(sum)}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f)) // Pushes the buttons to the bottom

        // --- "See My Badges" Button ---
        Button(
            onClick = { navController.navigate("gamification") },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = "Badges")
            Spacer(Modifier.width(8.dp))
            Text("See My Badges")
        }

        // --- Unlock Insights Button ---
        Button(
            onClick = { navController.navigate("insight_dashboard") },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.Filled.Lock, contentDescription = "Unlock")
            Spacer(Modifier.width(8.dp))
            Text("Unlock Insight Dashboard")
        }
    }
}



@Composable
fun InsightDashboardScreen(viewModel: ExpenseViewModel) {
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())
    val aiAdvice by viewModel.aiAdvice.collectAsState()
    val isAILoading by viewModel.isAILoading.collectAsState()

    val categoryTotals = expenses.groupBy { it.category }.mapValues { entry ->
        entry.value.sumOf { it.amount }
    }
    val dateTotals = expenses.groupBy { it.date }.mapValues { entry ->
        entry.value.sumOf { it.amount }
    }

    var selectedGoal by remember { mutableStateOf("General") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Insight Dashboard", style = MaterialTheme.typography.headlineMedium)
        }

        // --- Pie Chart Card (Updated) ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("By Category", style = MaterialTheme.typography.titleMedium)
                    AndroidView(
                        factory = { context ->
                            PieChart(context).apply {
                                this.isHighlightPerTapEnabled = true
                                this.description.isEnabled = false
                                this.setDrawEntryLabels(false)
                                this.legend.isEnabled = true
                                this.isDrawHoleEnabled = true
                                this.holeRadius = 58f
                                this.transparentCircleRadius = 61f
                            }
                        },
                        update = { chart ->
                            val entries = categoryTotals.map { (category, sum) ->
                                PieEntry(sum.toFloat(), category)
                            }

                            val dataSet = PieDataSet(entries, "").apply {
                                colors = ColorTemplate.MATERIAL_COLORS.toList()
                                this.setDrawValues(true)
                                valueTextSize = 12f
                                setValueTextColor(android.graphics.Color.BLACK)

                                // --- MODIFICATION: Format the value as a whole number ---
                                this.valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        // Convert the float to an integer and then to a string
                                        return value.toInt().toString()
                                    }
                                }
                            }

                            chart.data = PieData(dataSet)

                            val listener = object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                                    val pieEntry = e as? PieEntry
                                    pieEntry?.label?.let { label ->
                                        chart.centerText = label
                                        chart.setCenterTextSize(24f)
                                        chart.setCenterTextColor(android.graphics.Color.BLACK)
                                    }
                                }

                                override fun onNothingSelected() {
                                    chart.centerText = ""
                                }
                            }
                            chart.setOnChartValueSelectedListener(listener)

                            chart.invalidate()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }
            }
        }
        // --- Bar Chart Card ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("By Date", style = MaterialTheme.typography.titleMedium)
                    AndroidView(
                        factory = { context -> BarChart(context).apply { /* Initial setup */ } },
                        update = { chart ->
                            val entries = dateTotals.entries.mapIndexed { index, entry ->
                                BarEntry(index.toFloat(), entry.value.toFloat())
                            }
                            val dataSet = BarDataSet(entries, "Expenses by Day").apply {
                                colors = ColorTemplate.MATERIAL_COLORS.toList()
                                valueTextSize = 12f
                            }
                            chart.data = BarData(dataSet)
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dateTotals.keys.toList())
                            chart.xAxis.granularity = 1f
                            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                            chart.axisRight.isEnabled = false
                            chart.description.isEnabled = false
                            chart.invalidate()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("AI-Powered Advice", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))

                    // Goal selection buttons
                    Text(
                        "Select your focus:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { selectedGoal = "General" },
                            colors = if (selectedGoal == "General") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                        ) { Text("General Advice") }

                        Button(
                            onClick = { selectedGoal = "Saving" },
                            colors = if (selectedGoal == "Saving") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                        ) { Text("Focus on Saving") }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Button to trigger the AI call
                    Button(
                        onClick = { viewModel.getAIAdvice(expenses, selectedGoal) },
                        enabled = !isAILoading, // Disable button while loading
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Get AI Advice")
                    }

                    Spacer(Modifier.height(16.dp))

                    // Display loading indicator or the AI's advice
                    if (isAILoading) {
                        CircularProgressIndicator()
                    } else if (aiAdvice != null) {
                        Text(
                            text = aiAdvice!!,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun BadgeCard(title: String, description: String, earned: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (earned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (earned) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                contentDescription = if (earned) "Earned" else "Not Earned",
                tint = if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (earned) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (earned) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
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

