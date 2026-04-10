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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.inglada.battleship.ui.navigation.AppScreens

@Composable
fun MainMenuScreen(navController: NavController) {
    // We need the Activity context to be able to close the app
    val activity = LocalContext.current as? Activity

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Game Title
        Text(
            text = "BATTLESHIP",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Start Game Button
        Button(
            onClick = { navController.navigate(AppScreens.Configuration.route) },
            modifier = Modifier
                .fillMaxWidth(0.6f) // Takes 60% of the screen width
                .padding(bottom = 16.dp)
        ) {
            Text(text = "Start Game")
        }

        // Help Button
        Button(
            onClick = { navController.navigate(AppScreens.Help.route) },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(bottom = 16.dp)
        ) {
            Text(text = "Help")
        }

        // Exit Button
        Button(
            onClick = { activity?.finish() },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(text = "Exit")
        }
    }
}