package com.inglada.battleship.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.inglada.battleship.ui.navigation.AppScreens

@Composable
fun MainMenuScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Battleship: Main Menu")

        // Navigate to Configuration screen when clicking "Play"
        Button(onClick = { navController.navigate(AppScreens.Configuration.route) }) {
            Text(text = "Start Game")
        }

        // Navigate to Help screen
        Button(onClick = { navController.navigate(AppScreens.Help.route) }) {
            Text(text = "Help")
        }
    }
}