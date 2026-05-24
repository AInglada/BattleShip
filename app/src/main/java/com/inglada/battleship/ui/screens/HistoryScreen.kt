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
import com.inglada.battleship.model.MoveLog
import com.inglada.battleship.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adaptive screen that displays the history of played matches.
 * Utilizes Material 3 Adaptive Layouts to provide a mono-panel design on smartphones
 * and a bi-panel (list-detail) design on tablets.
 * Includes full sequential move logging.
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

    val navigator = rememberListDetailPaneScaffoldNavigator<GameMatchEntity>()

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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(id = R.string.cd_back)
                        )
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.history_empty_detail),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
 *
 * @param matches The list of game matches to display.
 * @param onMatchSelected Callback triggered when a match is clicked.
 */
@Composable
private fun MatchListPane(
    matches: List<GameMatchEntity>,
    onMatchSelected: (GameMatchEntity) -> Unit
) {
    if (matches.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(id = R.string.history_empty_list),
                style = MaterialTheme.typography.bodyLarge
            )
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
                        val isVictory = match.outcome == "Victory"
                        Text(
                            text = "${match.outcome} - ${match.playerName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isVictory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val dateText = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(match.timestamp))
                        Text(
                            text = dateText, 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable representing the detailed view of a selected match, including its complete move log.
 *
 * @param match The entity containing all information about the selected match.
 */
@Composable
private fun MatchDetailPane(match: GameMatchEntity) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Match Summary Section
            item {
                Text(
                    text = stringResource(id = R.string.history_detail_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow(
                    label = stringResource(id = R.string.history_detail_commander),
                    value = match.playerName
                )
                DetailRow(
                    label = stringResource(id = R.string.history_detail_date),
                    value = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(match.timestamp))
                )
                DetailRow(
                    label = stringResource(id = R.string.history_detail_outcome),
                    value = match.outcome
                )
                DetailRow(
                    label = stringResource(id = R.string.history_detail_grid),
                    value = stringResource(id = R.string.history_detail_grid_format, match.gridSize, match.gridSize)
                )
                DetailRow(
                    label = stringResource(id = R.string.history_detail_time),
                    value = stringResource(id = R.string.history_detail_time_format, match.timeSpent)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = R.string.history_log_title, match.moveLogs.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Logs List Section
            if (match.moveLogs.isEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.history_log_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(match.moveLogs) { log ->
                    MoveLogItem(log = log)
                }
            }
        }
    }
}

/**
 * A simple row displaying a label and a value for match summary.
 *
 * @param label The descriptive label.
 * @param value The value to display.
 */
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

/**
 * Visual representation of a single move in the log history.
 *
 * @param log The move data to display.
 */
@Composable
private fun MoveLogItem(log: MoveLog) {
    val actorPlayer = stringResource(id = R.string.history_log_actor_player)
    val actorAI = stringResource(id = R.string.history_log_actor_ai)
    val actor = if (log.isPlayer) actorPlayer else actorAI

    val isHit = log.result == "Hit"
    val resultColor = if (isHit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.history_log_action, actor, log.row, log.col),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (log.timeRemaining != null) {
                Text(
                    text = stringResource(id = R.string.history_log_time_remaining, log.timeRemaining),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = log.result.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = resultColor
        )
    }
}