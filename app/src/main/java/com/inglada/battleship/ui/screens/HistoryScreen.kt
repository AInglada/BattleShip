package com.inglada.battleship.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inglada.battleship.R
import com.inglada.battleship.data.GameMatchEntity
import com.inglada.battleship.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adaptive screen that displays the history of played matches.
 * Utilizes Material 3 Adaptive Layouts to provide a mono-panel design on smartphones
 * and a bi-panel (list-detail) design on tablets.
 *
 * @param viewModel The ViewModel providing the match history state.
 * @param onBackClicked Callback to return to the main menu.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBackClicked: () -> Unit
) {
    val matches by viewModel.matches.collectAsState()

    // The navigator handles the adaptive magic (split screen vs single screen)
    val navigator = rememberListDetailPaneScaffoldNavigator<GameMatchEntity>()

    // Intercept hardware back button to navigate back from detail to list on smartphones
    BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(stringResource(id = R.string.menu_btn_history)) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            listPane = {
                AnimatedPane {
                    MatchListPane(
                        matches = matches,
                        onMatchSelected = { match ->
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, match)
                        }
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    val currentMatch = navigator.currentDestination?.content
                    if (currentMatch != null) {
                        MatchDetailPane(match = currentMatch)
                    } else {
                        // Empty state when nothing is selected on a tablet
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select a match to view details",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        )
    }
}

/**
 * Composable representing the list pane of the adaptive layout.
 */
@Composable
private fun MatchListPane(
    matches: List<GameMatchEntity>,
    onMatchSelected: (GameMatchEntity) -> Unit
) {
    if (matches.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No matches played yet.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(matches) { match ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMatchSelected(match) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${match.outcome} - ${match.playerName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (match.outcome == "Victory") Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val dateText = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(match.timestamp))
                        Text(text = dateText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

/**
 * Composable representing the detailed view of a selected match.
 */
@Composable
private fun MatchDetailPane(match: GameMatchEntity) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Match Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Divider()
            DetailRow(label = "Commander", value = match.playerName)
            DetailRow(
                label = "Date",
                value = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(match.timestamp))
            )
            DetailRow(label = "Outcome", value = match.outcome)
            DetailRow(label = "Grid Size", value = "${match.gridSize} x ${match.gridSize}")
            DetailRow(label = "Time Spent", value = "${match.timeSpent} seconds")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}