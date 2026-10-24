package com.spendwise.android.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.domain.model.SourceType
import com.spendwise.android.domain.model.Transaction
import com.spendwise.android.domain.model.TransactionCategory
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.ui.common.EmptyStateView
import com.spendwise.android.ui.theme.CharcoalBorder
import com.spendwise.android.ui.theme.CharcoalBorderSubtle
import com.spendwise.android.ui.theme.CharcoalDark
import com.spendwise.android.ui.theme.CreditGreen
import com.spendwise.android.ui.theme.CreditGreenSubtle
import com.spendwise.android.ui.theme.DebitRed
import com.spendwise.android.ui.theme.DebitRedSubtle
import com.spendwise.android.ui.theme.LimeAccent
import com.spendwise.android.ui.theme.LimeAccentSubtle
import com.spendwise.android.ui.theme.SlateTextMuted
import com.spendwise.android.ui.theme.SlateTextSecondary
import com.spendwise.android.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionsScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel? = null
) {
    val uiState = if (viewModel != null) {
        val state by viewModel.uiState.collectAsState()
        state
    } else {
        TransactionsUiState(
            transactions = listOf(
                Transaction(
                    id = "preview_1",
                    amountPaise = 34900L,
                    direction = TransactionDirection.DEBIT,
                    merchant = "Swiggy",
                    note = "Dinner order",
                    category = TransactionCategory.FOOD,
                    occurredAtEpochMillis = System.currentTimeMillis() - 3600000L,
                    sourceType = SourceType.MANUAL
                ),
                Transaction(
                    id = "preview_2",
                    amountPaise = 52000L,
                    direction = TransactionDirection.DEBIT,
                    merchant = "Uber",
                    note = "Ride to office",
                    category = TransactionCategory.TRANSPORT,
                    occurredAtEpochMillis = System.currentTimeMillis() - 86400000L,
                    sourceType = SourceType.NOTIFICATION
                )
            ),
            isLoading = false
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.recentlyDeletedTransaction) {
        val deleted = uiState.recentlyDeletedTransaction
        if (deleted != null && viewModel != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Deleted \"${deleted.merchant}\"",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            } else {
                viewModel.clearRecentlyDeleted()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = LimeAccent,
                contentColor = CharcoalDark,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add transaction"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Complete ledger history",
                style = MaterialTheme.typography.bodySmall,
                color = SlateTextSecondary,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel?.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                placeholder = { Text("Search merchants, categories...", color = SlateTextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SlateTextMuted
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel?.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = SlateTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = LimeAccent,
                    unfocusedIndicatorColor = CharcoalBorderSubtle
                )
            )

            // Filter Chips Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedDirection == null && uiState.selectedCategory == null && uiState.selectedSourceType == null,
                    onClick = { viewModel?.clearFilters() },
                    label = { Text("All") },
                    border = BorderStroke(1.dp, CharcoalBorderSubtle),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LimeAccent,
                        selectedLabelColor = CharcoalDark
                    )
                )
                FilterChip(
                    selected = uiState.selectedDirection == TransactionDirection.DEBIT,
                    onClick = { viewModel?.setDirectionFilter(TransactionDirection.DEBIT) },
                    label = { Text("Debits") },
                    border = BorderStroke(1.dp, CharcoalBorderSubtle),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LimeAccent,
                        selectedLabelColor = CharcoalDark
                    )
                )
                FilterChip(
                    selected = uiState.selectedDirection == TransactionDirection.CREDIT,
                    onClick = { viewModel?.setDirectionFilter(TransactionDirection.CREDIT) },
                    label = { Text("Credits") },
                    border = BorderStroke(1.dp, CharcoalBorderSubtle),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LimeAccent,
                        selectedLabelColor = CharcoalDark
                    )
                )
                FilterChip(
                    selected = uiState.selectedSourceType == SourceType.MANUAL,
                    onClick = { viewModel?.setSourceFilter(SourceType.MANUAL) },
                    label = { Text("Manual Only") },
                    border = BorderStroke(1.dp, CharcoalBorderSubtle),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LimeAccent,
                        selectedLabelColor = CharcoalDark
                    )
                )
                FilterChip(
                    selected = uiState.selectedSourceType == SourceType.NOTIFICATION,
                    onClick = { viewModel?.setSourceFilter(SourceType.NOTIFICATION) },
                    label = { Text("From Alerts") },
                    border = BorderStroke(1.dp, CharcoalBorderSubtle),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LimeAccent,
                        selectedLabelColor = CharcoalDark
                    )
                )

                // Category Chips
                TransactionCategory.values().forEach { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel?.setCategoryFilter(category) },
                        label = { Text(category.displayName) },
                        border = BorderStroke(1.dp, CharcoalBorderSubtle),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LimeAccent,
                            selectedLabelColor = CharcoalDark
                        )
                    )
                }
            }

            // List of Transactions
            if (uiState.transactions.isEmpty()) {
                EmptyStateView(
                    title = "No Transactions Found",
                    message = if (uiState.searchQuery.isNotBlank() || uiState.selectedDirection != null || uiState.selectedCategory != null)
                        "No transactions match your current search query or active filter."
                    else
                        "No transactions have been recorded yet. Tap + to add one manually.",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    actionLabel = if (uiState.searchQuery.isNotBlank() || uiState.selectedDirection != null) "Clear Filters" else "Add Expense",
                    onActionClick = {
                        if (uiState.searchQuery.isNotBlank() || uiState.selectedDirection != null) {
                            viewModel?.clearFilters()
                        } else {
                            onNavigateToAddTransaction()
                        }
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.transactions, key = { it.id }) { tx ->
                        TransactionRowItem(
                            transaction = tx,
                            onClick = { onNavigateToEditTransaction(tx.id) },
                            onDelete = { viewModel?.deleteTransaction(tx) }
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

@Composable
private fun TransactionRowItem(
    transaction: Transaction,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(transaction.occurredAtEpochMillis) {
        dateFormat.format(Date(transaction.occurredAtEpochMillis))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorderSubtle, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (transaction.direction == TransactionDirection.DEBIT)
                                DebitRedSubtle
                            else
                                CreditGreenSubtle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (transaction.direction == TransactionDirection.DEBIT)
                            Icons.Default.ArrowUpward
                        else
                            Icons.Default.ArrowDownward,
                        contentDescription = transaction.direction.name,
                        tint = if (transaction.direction == TransactionDirection.DEBIT) DebitRed else CreditGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${transaction.category.displayName} • $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary
                    )
                    if (!transaction.note.isNullOrBlank()) {
                        Text(
                            text = transaction.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextMuted,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    val sign = if (transaction.direction == TransactionDirection.DEBIT) "−" else "+"
                    Text(
                        text = "$sign${CurrencyFormatter.formatPaise(transaction.amountPaise)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.direction == TransactionDirection.DEBIT)
                            MaterialTheme.colorScheme.onSurface
                        else
                            CreditGreen
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (transaction.sourceType == SourceType.NOTIFICATION) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LimeAccentSubtle)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Alert",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LimeAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Text(
                            text = if (transaction.direction == TransactionDirection.DEBIT) "Debit" else "Credit",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete transaction",
                        tint = SlateTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
