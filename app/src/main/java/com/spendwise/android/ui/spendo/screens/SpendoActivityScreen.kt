package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoEmptyState
import com.spendwise.android.ui.spendo.model.SpendoTransaction
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.model.TransactionType
import com.spendwise.android.ui.spendo.theme.SpendoTheme

enum class ActivityFilterTab {
    ALL, EXPENSES, INCOME, TRANSFERS
}

@Composable
fun SpendoActivityScreen(
    uiState: SpendoUiState,
    onSearchChange: (String) -> Unit,
    onFilterTypeChange: (TransactionType?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SpendoTheme.colors

    // Derive active tab
    val currentTab = when {
        uiState.selectedCategory.equals("TRANSFER", ignoreCase = true) ||
                uiState.selectedCategory.equals("TRANSFERS", ignoreCase = true) -> ActivityFilterTab.TRANSFERS
        uiState.selectedFilterType == TransactionType.EXPENSE -> ActivityFilterTab.EXPENSES
        uiState.selectedFilterType == TransactionType.INCOME -> ActivityFilterTab.INCOME
        else -> ActivityFilterTab.ALL
    }

    // Accurate Transaction Filtering
    val filteredTransactions = uiState.transactions.filter { tx ->
        val matchesQuery = uiState.searchQuery.isBlank() ||
                tx.merchant.contains(uiState.searchQuery, ignoreCase = true) ||
                tx.category.contains(uiState.searchQuery, ignoreCase = true) ||
                tx.bankAccountName.contains(uiState.searchQuery, ignoreCase = true) ||
                (tx.note?.contains(uiState.searchQuery, ignoreCase = true) == true)

        val isTransfer = tx.category.equals("TRANSFER", ignoreCase = true) ||
                tx.category.equals("TRANSFERS", ignoreCase = true)

        val matchesTab = when (currentTab) {
            ActivityFilterTab.ALL -> true
            ActivityFilterTab.EXPENSES -> tx.type == TransactionType.EXPENSE && !isTransfer
            ActivityFilterTab.INCOME -> tx.type == TransactionType.INCOME && !isTransfer
            ActivityFilterTab.TRANSFERS -> isTransfer
        }

        val matchesCategory = if (currentTab == ActivityFilterTab.TRANSFERS) {
            true
        } else {
            uiState.selectedCategory == null || tx.category.equals(uiState.selectedCategory, ignoreCase = true)
        }

        matchesQuery && matchesTab && matchesCategory
    }

    // Accurate Counts
    val allCount = uiState.transactions.size
    val expenseCount = uiState.transactions.count {
        it.type == TransactionType.EXPENSE && !it.category.equals("TRANSFER", ignoreCase = true)
    }
    val incomeCount = uiState.transactions.count {
        it.type == TransactionType.INCOME && !it.category.equals("TRANSFER", ignoreCase = true)
    }
    val transferCount = uiState.transactions.count {
        it.category.equals("TRANSFER", ignoreCase = true) || it.category.equals("TRANSFERS", ignoreCase = true)
    }

    // Baseline Totals (excluding internal transfers)
    val totalExpensePaise = uiState.transactions
        .filter { it.type == TransactionType.EXPENSE && !it.category.equals("TRANSFER", ignoreCase = true) }
        .sumOf { it.amountPaise }
    val totalIncomePaise = uiState.transactions
        .filter { it.type == TransactionType.INCOME && !it.category.equals("TRANSFER", ignoreCase = true) }
        .sumOf { it.amountPaise }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = if (colors.isDark) colors.surfaceDark else colors.surfaceDark,
                contentColor = if (colors.isDark) colors.textPrimary else colors.textInverse,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Screen Header with Nothing OS Dot Matrix Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    DotMatrixText(
                        text = "LEDGER",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "Activity",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${filteredTransactions.size} ENTRIES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                }
            }

            // Summary Strip: Total Spent vs Total Income
            SpendoCard(contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        DotMatrixText(text = "Total Spent", fontSize = 10.sp, color = colors.textSecondary)
                        Text(
                            text = "₹%,.2f".format(totalExpensePaise / 100.0),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = colors.textPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 1.dp, height = 30.dp)
                            .background(colors.divider)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.End) {
                        DotMatrixText(text = "Total Income", fontSize = 10.sp, color = colors.textSecondary)
                        Text(
                            text = "₹%,.2f".format(totalIncomePaise / 100.0),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = colors.textPrimary
                        )
                    }
                }
            }

            // Search Bar with Clear Icon
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search payees, categories, banks...",
                        fontSize = 13.sp,
                        color = colors.textMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.textSecondary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedIndicatorColor = colors.textPrimary,
                    unfocusedIndicatorColor = colors.divider,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            // Responsive Horizontal Scroll Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tab: ALL
                ActivityFilterPill(
                    label = "All ($allCount)",
                    selected = currentTab == ActivityFilterTab.ALL && uiState.selectedCategory == null,
                    onClick = {
                        onFilterTypeChange(null)
                        onCategoryFilterChange(null)
                    }
                )

                // Tab: EXPENSES
                ActivityFilterPill(
                    label = "Expenses ($expenseCount)",
                    selected = currentTab == ActivityFilterTab.EXPENSES && uiState.selectedCategory == null,
                    onClick = {
                        onCategoryFilterChange(null)
                        onFilterTypeChange(TransactionType.EXPENSE)
                    }
                )

                // Tab: INCOME
                ActivityFilterPill(
                    label = "Income ($incomeCount)",
                    selected = currentTab == ActivityFilterTab.INCOME && uiState.selectedCategory == null,
                    onClick = {
                        onCategoryFilterChange(null)
                        onFilterTypeChange(TransactionType.INCOME)
                    }
                )

                // Tab: TRANSFERS
                ActivityFilterPill(
                    label = "Transfers ($transferCount)",
                    selected = currentTab == ActivityFilterTab.TRANSFERS,
                    onClick = {
                        onFilterTypeChange(null)
                        onCategoryFilterChange("TRANSFER")
                    }
                )

                // Specific Category Filter Chips
                val categories = listOf("Food & Drink", "Transit", "Groceries", "Subscriptions", "Salary", "Freelance")
                categories.forEach { category ->
                    val catCount = uiState.transactions.count { it.category.equals(category, ignoreCase = true) }
                    ActivityFilterPill(
                        label = "$category ($catCount)",
                        selected = uiState.selectedCategory.equals(category, ignoreCase = true),
                        onClick = {
                            if (uiState.selectedCategory.equals(category, ignoreCase = true)) {
                                onCategoryFilterChange(null)
                            } else {
                                onCategoryFilterChange(category)
                            }
                        }
                    )
                }
            }

            // Transaction List or Empty State
            if (filteredTransactions.isEmpty()) {
                SpendoEmptyState(
                    title = "No Matches Found",
                    message = "No transactions matched your current search and active filters.",
                    actionLabel = "Clear Filters",
                    onActionClick = {
                        onSearchChange("")
                        onFilterTypeChange(null)
                        onCategoryFilterChange(null)
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        HomeTransactionRow(
                            transaction = tx,
                            onClick = { onNavigateToTransactionDetail(tx.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = SpendoTheme.colors
    val backgroundColor = if (selected) {
        if (colors.isDark) colors.surfaceDark else colors.surfaceDark
    } else {
        colors.surface
    }

    val contentColor = if (selected) {
        if (colors.isDark) colors.textPrimary else colors.textInverse
    } else {
        colors.textSecondary
    }

    val borderColor = if (selected) {
        if (colors.isDark) colors.accentRed else colors.borderDark
    } else {
        colors.borderSubtle
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(colors.accentRed)
                )
            }
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
