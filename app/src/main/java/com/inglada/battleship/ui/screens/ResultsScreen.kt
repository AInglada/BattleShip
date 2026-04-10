package com.inglada.battleship.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.inglada.battleship.ui.navigation.AppScreens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultsScreen(
    navController: androidx.navigation.NavController,
    playerName: String,
    gridSize: Int,
    didWin: Boolean,
    timeSpent: Int
) {
    val context = LocalContext.current

    // Generate current Date and Time
    val currentDateTime = remember {
        SimpleDateFormat("MMM dd, yyyy h:mm:ss a", Locale.getDefault()).format(Date())
    }

    // Generate the Log String based on the practice requirements
    val resultMessage = if (didWin) "You won!" else "Time finished!"
    val initialLog = "Alias: $playerName | Grid Size: $gridSize\n" +
            "Total time: $timeSpent seconds.\n" +
            "$resultMessage"

    // States for the text fields (so the user can edit them if they want)
    var dateTimeText by remember { mutableStateOf(currentDateTime) }
    var logText by remember { mutableStateOf(initialLog) }
    var emailText by remember { mutableStateOf("example@example.com") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "GAME RESULTS",
            style = MaterialTheme.typography.headlineMedium
        )

        // 1. Date and Time TextField
        OutlinedTextField(
            value = dateTimeText,
            onValueChange = { dateTimeText = it },
            label = { Text("Date and time") },
            modifier = Modifier.fillMaxWidth()
        )

        // 2. Log Data TextField
        OutlinedTextField(
            value = logText,
            onValueChange = { logText = it },
            label = { Text("Log Data") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )

        // 3. Recipient Email TextField
        OutlinedTextField(
            value = emailText,
            onValueChange = { emailText = it },
            label = { Text("Email receiver") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // Actions
        Button(
            onClick = { sendEmail(context, emailText, dateTimeText, logText) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send email")
        }

        Button(
            onClick = {
                // Navigate back to Main Menu and clear the backstack
                navController.navigate(AppScreens.MainMenu.route) {
                    popUpTo(0) // Clears the whole stack so we don't return to the finished game
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("New Game / Exit")
        }
    }
}

// Helper function to trigger the Android Email Intent
private fun sendEmail(context: Context, email: String, dateTime: String, logContent: String) {
    val subject = "Log - $dateTime"

    // ACTION_SENDTO Intent ensures only email apps handle this
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, logContent)
    }

    // Verify that the user has an email app installed before trying to start it
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // Fallback if no email app is found
        context.startActivity(Intent.createChooser(intent, "Choose an Email client"))
    }
}