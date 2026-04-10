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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.inglada.battleship.ui.navigation.AppScreens
import com.inglada.battleship.R
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
    val resultMessage = if (didWin) stringResource(id = R.string.log_won) else stringResource(id = R.string.log_lost)
    val initialLog = stringResource(
        id = R.string.log_format,
        playerName, // %1$s
        gridSize,   // %2$d
        gridSize,   // %3$d
        timeSpent,  // %4$d
        resultMessage // %5$s
    )

    // States for the text fields (so the user can edit them if they want)
    var dateTimeText by remember { mutableStateOf(currentDateTime) }
    var logText by remember { mutableStateOf(initialLog) }

    // Read the string resource out here in the Composable scope
    val defaultEmail = stringResource(id = R.string.results_default_email)
    // Pass the resolved string into the initial state
    var emailText by remember { mutableStateOf(defaultEmail) }

    val emailSubject = stringResource(id = R.string.email_subject_format, dateTimeText)
    val chooserTitle = stringResource(id = R.string.results_email_chooser)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.results_title),
            style = MaterialTheme.typography.headlineMedium
        )

        // 1. Date and Time TextField
        OutlinedTextField(
            value = dateTimeText,
            onValueChange = { dateTimeText = it },
            label = { Text(text = stringResource(id = R.string.results_date_time)) },
            modifier = Modifier.fillMaxWidth()
        )

        // 2. Log Data TextField
        OutlinedTextField(
            value = logText,
            onValueChange = { logText = it },
            label = { Text(text = stringResource(id = R.string.results_log_data)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )

        // 3. Recipient Email TextField
        OutlinedTextField(
            value = emailText,
            onValueChange = { emailText = it },
            label = { Text(text = stringResource(id = R.string.results_email_recipient)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // Actions
        Button(
            onClick = { sendEmail(context, emailText, emailSubject, logText, chooserTitle) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.results_btn_send))
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
            Text(text = stringResource(id = R.string.results_btn_new_game))
        }
    }
}

// Helper function to trigger the Android Email Intent
private fun sendEmail(
    context: Context,
    email: String,
    subject: String,
    logContent: String,
    chooserTitle: String
) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, logContent)
    }

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}