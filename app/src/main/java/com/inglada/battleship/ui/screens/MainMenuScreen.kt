package com.inglada.battleship.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inglada.battleship.R

/**
 * Composable that represents the main menu screen of the application.
 *
 * Provides options to start a new game, view instructions, or exit the application.
 *
 * @param onStartGame Callback to navigate to the configuration screen.
 * @param onHelp Callback to navigate to the help screen.
 * @param onExit Callback to close the application.
 */
@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onHelp: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.menu_title),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Button(
            onClick = onStartGame,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(bottom = 16.dp)
        ) {
            Text(text = stringResource(id = R.string.menu_btn_start))
        }

        Button(
            onClick = onHelp,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(bottom = 16.dp)
        ) {
            Text(text = stringResource(id = R.string.menu_btn_help))
        }

        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(text = stringResource(id = R.string.menu_btn_exit))
        }
    }
}
