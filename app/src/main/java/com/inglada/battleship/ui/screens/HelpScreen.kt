package com.inglada.battleship.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.inglada.battleship.ui.navigation.AppScreens

@Composable
fun HelpScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HOW TO PLAY",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp, top = 32.dp)
        )

        // Scrollable column for the instructions
        Column(
            modifier = Modifier
                .weight(1f) // Takes all available space leaving room for the button below
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Welcome to BATTLESHIP!\n\n" +
                        "The goal of the game is to find and sink the entire hidden enemy fleet before the time runs out (if time control is enabled).\n\n" +
                        "1. The grid hides 5 ships of different sizes (5, 4, 3, 3, and 2 squares).\n" +
                        "2. Tap any square on the grid to guess.\n" +
                        "3. A BLUE square means you hit water (MISS).\n" +
                        "4. A RED square means you hit a ship (HIT).\n" +
                        "5. Find all parts of all ships to win the game!\n\n" +
                        "Good luck, Commander!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Justify
            )
        }

        // Return Button
        Button(
            onClick = {
                navController.navigate(AppScreens.MainMenu.route) {
                    popUpTo(AppScreens.MainMenu.route) { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Back to Menu")
        }
    }
}