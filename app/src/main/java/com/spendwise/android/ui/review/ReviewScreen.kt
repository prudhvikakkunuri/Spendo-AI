package com.spendwise.android.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.domain.model.Transaction
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.ui.common.EmptyStateView
import com.spendwise.android.ui.theme.CharcoalBorder
import com.spendwise.android.ui.theme.CharcoalBorderSubtle
import com.spendwise.android.ui.theme.CharcoalDark
import com.spendwise.android.ui.theme.CreditGreen
import com.spendwise.android.ui.theme.DebitRed
import com.spendwise.android.ui.theme.LimeAccent
import com.spendwise.android.ui.theme.LimeAccentSubtle
import com.spendwise.android.ui.theme.ReviewOrange
import com.spendwise.android.ui.theme.ReviewOrangeSubtle
import com.spendwise.android.ui.theme.SlateTextMuted
import com.spendwise.android.ui.theme.SlateTextSecondary
import com.spendwise.android.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel? = null,
    onNavigateToEditCandidate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val reviewTransactions = if (viewModel != null) {
        val list by viewModel.reviewTransactions.collectAsState()
        list
    } else {
        null
    }

    val fallbackList = remember {
        mutableStateListOf(
            Transaction(
                id = "demo_rev_1",
                amountPaise = 45000L,
                currencyCode = "INR",
                direction = TransactionDirection.DEBIT,
                merchant = "Unknown Payee",
                note = "Ambiguous merchant in alert",
                category = com.spendwise.android.domain.model.TransactionCategory.OTHER,
                occurredAtEpochMillis = System.currentTimeMillis() - 3600000L,
                sourceType = com.spendwise.android.domain.model.SourceType.NOTIFICATION,
                sourcePackage = "com.google.android.apps.nbu.paisa.user",
                confidence = 0.75f,
                reviewStatus = com.spendwise.android.domain.model.ReviewStatus.NEEDS_REVIEW
            ),
            Transaction(
                id = "demo_rev_2",
                amountPaise = 120000L,
                currencyCode = "INR",
                direction = TransactionDirection.CREDIT,
                merchant = "Amazon",
                note = "Suspected refund or reversal",
                category = com.spendwise.android.domain.model.TransactionCategory.SHOPPING,
                occurredAtEpochMillis = System.currentTimeMillis() - 7200000L,
                sourceType = com.spendwise.android.domain.model.SourceType.NOTIFICATION,
                sourcePackage = "com.phonepe.app",
                confidence = 0.80f,
                reviewStatus = com.spendwise.android.domain.model.ReviewStatus.NEEDS_REVIEW
            )
        )
    }

    val activeList = reviewTransactions ?: fallbackList

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Text(
            text = "Review Queue",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Verify uncertain transaction alerts before adding to confirmed ledger",
            style = MaterialTheme.typography.bodySmall,
            color = SlateTextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (activeList.isEmpty()) {
            EmptyStateView(
                title = "Review Queue Clean",
                message = "All detected notification candidates have been confirmed or dismissed. New uncertain alerts will appear here.",
                icon = Icons.Default.RateReview
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(activeList, key = { it.id }) { candidate ->
                    ReviewCandidateCard(
                        transaction = candidate,
                        onConfirm = {
                            if (viewModel != null) {
                                viewModel.confirmTransaction(candidate.id)
                            } else {
                                fallbackList.remove(candidate)
                            }
                        },
                        onIgnore = {
                            if (viewModel != null) {
                                viewModel.ignoreTransaction(candidate.id)
                            } else {
                                fallbackList.remove(candidate)
                            }
                        },
                        onDelete = {
                            if (viewModel != null) {
                                viewModel.deleteTransaction(candidate.id)
                            } else {
                                fallbackList.remove(candidate)
                            }
                        },
                        onEdit = { onNavigateToEditCandidate(candidate.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ReviewCandidateCard(
    transaction: Transaction,
    onConfirm: () -> Unit,
    onIgnore: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(transaction.occurredAtEpochMillis) {
        dateFormat.format(Date(transaction.occurredAtEpochMillis))
    }

    val sourceLabel = transaction.sourcePackage?.substringAfterLast('.')
        ?.replaceFirstChar { it.uppercase() } ?: "Alert"

    val confidencePercent = transaction.confidence?.let { (it * 100).toInt() } ?: 70

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorderSubtle, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Source & Confidence Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ReviewOrange)
                    )
                    Text(
                        text = "$sourceLabel • $formattedTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Confidence: $confidencePercent%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (confidencePercent >= 80) LimeAccent else ReviewOrange
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete candidate",
                            tint = SlateTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount & Suggested Merchant
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${transaction.direction.name} • ${transaction.category.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary
                    )
                }

                Text(
                    text = CurrencyFormatter.formatPaise(transaction.amountPaise),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.direction == TransactionDirection.DEBIT) DebitRed else CreditGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Uncertainty Reason Callout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ReviewOrangeSubtle)
                    .border(1.dp, ReviewOrange.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Uncertainty Reason",
                        tint = ReviewOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = transaction.note ?: "Uncertain notification layout or merchant",
                        style = MaterialTheme.typography.bodySmall,
                        color = ReviewOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions: Confirm, Edit, Ignore
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeAccent,
                        contentColor = CharcoalDark
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Confirm", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = LimeAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Edit", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onIgnore,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Ignore",
                        tint = SlateTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Ignore", color = SlateTextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}
