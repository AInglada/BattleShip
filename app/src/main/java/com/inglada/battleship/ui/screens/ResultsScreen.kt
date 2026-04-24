package com.inglada.battleship.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    isTimeOut: Boolean,
    timeSpent: Int,
    isHardMode: Boolean
) {
    val context = LocalContext.current

    // Generate current Date and Time
    val currentDateTime = remember {
        SimpleDateFormat("MMM dd, yyyy h:mm:ss a", Locale.getDefault()).format(Date())
    }

    // Generate the Log String
    // Determine the exact reason for the outcome
    val resultMessage = when {
        didWin -> stringResource(id = R.string.log_outcome_win)
        isTimeOut -> stringResource(id = R.string.log_outcome_loss_time)
        else -> stringResource(id = R.string.log_outcome_loss_ai)
    }
    val diffMessage = if (isHardMode) stringResource(id = R.string.results_diff_hard) else stringResource(id = R.string.results_diff_easy)
    val initialLog = stringResource(
        id = R.string.log_format,
        playerName, // %1$s
        gridSize,   // %2$d
        gridSize,   // %3$d
        diffMessage,// %4$s (Easy o Hard)
        timeSpent,  // %5$d (Time spent)
        resultMessage // %6$s (Win or Lose)
    )

    // States for the text fields (so the user can edit them if they want)
    var dateTimeText by rememberSaveable { mutableStateOf(currentDateTime) }
    var logText by rememberSaveable { mutableStateOf(initialLog) }

    // Read the string resource out here in the Composable scope
    val defaultEmail = stringResource(id = R.string.results_default_email)
    // Pass the resolved string into the initial state
    var emailText by rememberSaveable { mutableStateOf(defaultEmail) }

    val emailSubject = stringResource(id = R.string.email_subject_format, dateTimeText)
    val chooserTitle = stringResource(id = R.string.results_email_chooser)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
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

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        Button(
            onClick = { sendEmail(context, emailText, emailSubject, logText, chooserTitle) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.results_btn_send))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Play Again -> Goes to Config Screen and clears the back stack
            Button(
                onClick = {
                    navController.navigate(AppScreens.Configuration.route) {
                        popUpTo(AppScreens.MainMenu.route)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = R.string.results_btn_play_again))
            }

            // Exit -> Closes the entire application
            OutlinedButton(
                onClick = {
                    (context as? Activity)?.finish()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = R.string.menu_btn_exit))
            }
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