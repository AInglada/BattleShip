package com.inglada.battleship.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import kotlin.math.roundToInt
import com.inglada.battleship.R

/**
 * Composable that represents the game configuration screen.
 *
 * Allows the user to customize player alias, grid size, time limits, and AI difficulty.
 *
 * @param onBackClicked Callback executed when the back navigation is triggered.
 * @param onStartGameClicked Callback executed to initiate the game with the selected parameters.
 */
@Composable
fun ConfigScreen(
    onBackClicked: () -> Unit,
    onStartGameClicked: (playerName: String, gridSize: Int, isTimeEnabled: Boolean, timeLimit: Int, isHardMode: Boolean) -> Unit
) {
    val defaultPlayerName = stringResource(id = R.string.config_default_player)
    var playerName by rememberSaveable { mutableStateOf(defaultPlayerName) }
    var gridSize by rememberSaveable { mutableFloatStateOf(8f) }
    var isTimeEnabled by rememberSaveable { mutableStateOf(false) }
    var timeLimit by rememberSaveable { mutableFloatStateOf(15f) }
    var isHardMode by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            IconButton(onClick = { onBackClicked() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        Text(
            text = stringResource(id = R.string.config_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = playerName,
            onValueChange = { playerName = it },
            label = { Text(stringResource(id = R.string.config_player_alias)) },
            isError = playerName.isBlank(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    id = R.string.config_grid_size,
                    gridSize.roundToInt(),
                    gridSize.roundToInt()
                )
            )
            Slider(
                value = gridSize,
                onValueChange = { gridSize = it },
                valueRange = 8f..12f,
                steps = 3
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isTimeEnabled,
                onCheckedChange = { isTimeEnabled = it }
            )
            Text(text = stringResource(id = R.string.config_enable_time))
        }

        if (isTimeEnabled) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        id = R.string.config_time_limit,
                        timeLimit.roundToInt()
                    )
                )
                Slider(
                    value = timeLimit,
                    onValueChange = { timeLimit = it },
                    valueRange = 5f..30f,
                    steps = 4
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val difficultyText = if (isHardMode) {
                stringResource(id = R.string.config_ai_hard)
            } else {
                stringResource(id = R.string.config_ai_easy)
            }

            Text(text = stringResource(id = R.string.config_ai_difficulty, difficultyText))

            Switch(
                checked = isHardMode,
                onCheckedChange = { isHardMode = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onStartGameClicked(
                    playerName,
                    gridSize.roundToInt(),
                    isTimeEnabled,
                    timeLimit.roundToInt(),
                    isHardMode
                )
            },
            enabled = playerName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.config_btn_start))
        }
    }
}