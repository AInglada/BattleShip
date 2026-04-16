package com.inglada.battleship.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import kotlin.math.roundToInt
import com.inglada.battleship.R

@Composable
fun ConfigScreen(
    // Callback to navigate to the Game screen passing the selected data
    onStartGameClicked: (playerName: String, gridSize: Int, isTimeEnabled: Boolean, timeLimit: Int) -> Unit
) {
    // UI States using 'remember' to survive recompositions
    // Read the default name from resources outside the remember block
    val defaultPlayerName = stringResource(id = R.string.config_default_player)
    // Use it as the initial state
    var playerName by rememberSaveable { mutableStateOf(defaultPlayerName) }
    var gridSize by rememberSaveable { mutableFloatStateOf(8f) } // 8x8 by default
    var isTimeEnabled by rememberSaveable { mutableStateOf(false) }
    var timeLimit by rememberSaveable { mutableFloatStateOf(60f) } // 60 seconds by default

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // Adds space between elements
    ) {
        Text(
            text = stringResource(id = R.string.config_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Player Alias Input
        OutlinedTextField(
            value = playerName,
            onValueChange = { playerName = it },
            label = { Text(stringResource(id = R.string.config_player_alias)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // 2. Grid Size Selection
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
                valueRange = 6f..12f, // Min 6x6, Max 12x12
                steps = 5 // Allows specific stops (7, 8, 9, 10, 11)
            )
        }

        // 3. Time Control Checkbox
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

        // 4. Time Limit Selection (Visible only if time control is enabled)
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
                    valueRange = 30f..120f,
                    steps = 8
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start Game Button
        Button(
            onClick = {
                onStartGameClicked(
                    playerName,
                    gridSize.roundToInt(),
                    isTimeEnabled,
                    timeLimit.roundToInt()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.config_btn_start))
        }
    }
}