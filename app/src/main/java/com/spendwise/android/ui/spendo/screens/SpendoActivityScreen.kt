package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.model.TransactionType
import com.spendwise.android.ui.spendo.theme.SpendoColor

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
    val totalExpensePaise = uiState.transactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amountPaise }
    val totalIncomePaise = uiState.transactions
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amountPaise }

    val filteredTransactions = uiState.transactions.filter { tx ->
        val matchesQuery = uiState.searchQuery.isBlank() ||
                tx.merchant.contains(uiState.searchQuery, ignoreCase = true) ||
                tx.category.contains(uiState.searchQuery, ignoreCase = true) ||
                tx.bankAccountName.contains(uiState.searchQuery, ignoreCase = true)

        val matchesType = uiState.selectedFilterType == null || tx.type == uiState.selectedFilterType
        val matchesCategory = uiState.selectedCategory == null || tx.category == uiState.selectedCategory

        matchesQuery && matchesType && matchesCategory
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SpendoColor.Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = SpendoColor.SurfaceDark,
                contentColor = SpendoColor.TextInverse,
                shape = CircleShape
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
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    DotMatrixText(
                        text = "Ledger",
                        fontSize = 11.sp,
                        color = SpendoColor.TextSecondary
                    )
                    Text(
                        text = "Activity",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = SpendoColor.TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpendoColor.Surface)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${filteredTransactions.size} ENTRIES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpendoColor.TextSecondary
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
                        DotMatrixText(text = "Total Spent", fontSize = 10.sp, color = SpendoColor.TextSecondary)
                        Text(
                            text = "₹%,.2f".format(totalExpensePaise / 100.0),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = SpendoColor.TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 1.dp, height = 30.dp)
                            .background(SpendoColor.Divider)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.End) {
                        DotMatrixText(text = "Total Income", fontSize = 10.sp, color = SpendoColor.TextSecondary)
                        Text(
                            text = "₹%,.2f".format(totalIncomePaise / 100.0),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = SpendoColor.TextPrimary
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
                        color = SpendoColor.TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SpendoColor.TextSecondary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = SpendoColor.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SpendoColor.Surface,
                    unfocusedContainerColor = SpendoColor.Surface,
                    focusedIndicatorColor = SpendoColor.TextPrimary,
                    unfocusedIndicatorColor = SpendoColor.Divider
                )
            )

            // Filter Chips Scrollable Row (All, Expenses, Income + Categories)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedFilterType == null && uiState.selectedCategory == null,
                    onClick = {
                        onFilterTypeChange(null)
                        onCategoryFilterChange(null)
                    },
                    label = { Text("All", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SpendoColor.Divider),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpendoColor.SurfaceDark,
                        selectedLabelColor = SpendoColor.TextInverse,
                        containerColor = SpendoColor.Surface,
                        labelColor = SpendoColor.TextSecondary
                    )
                )

                FilterChip(
                    selected = uiState.selectedFilterType == TransactionType.EXPENSE,
                    onClick = { onFilterTypeChange(TransactionType.EXPENSE) },
                    label = { Text("Expenses", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SpendoColor.Divider),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpendoColor.SurfaceDark,
                        selectedLabelColor = SpendoColor.TextInverse,
                        containerColor = SpendoColor.Surface,
                        labelColor = SpendoColor.TextSecondary
                    )
                )

                FilterChip(
                    selected = uiState.selectedFilterType == TransactionType.INCOME,
                    onClick = { onFilterTypeChange(TransactionType.INCOME) },
                    label = { Text("Income", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SpendoColor.Divider),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpendoColor.SurfaceDark,
                        selectedLabelColor = SpendoColor.TextInverse,
                        containerColor = SpendoColor.Surface,
                        labelColor = SpendoColor.TextSecondary
                    )
                )

                val categories = listOf("Food & Drink", "Transit", "Groceries", "Subscriptions", "Salary", "Freelance")
                categories.forEach { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { onCategoryFilterChange(category) },
                        label = { Text(category, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, SpendoColor.Divider),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpendoColor.SurfaceDark,
                            selectedLabelColor = SpendoColor.TextInverse,
                            containerColor = SpendoColor.Surface,
                            labelColor = SpendoColor.TextSecondary
                        )
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        HomeTransactionRow(
                            transaction = tx,
                            onClick = { onNavigateToTransactionDetail(tx.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}
