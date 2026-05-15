package com.inglada.battleship.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.inglada.battleship.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Composable that represents the results summary screen.
 *
 * Displays the outcome of the match and allows the user to export the game log via email,
 * play again, return to the main menu, or exit the application.
 *
 * @param playerName Alias of the player.
 * @param gridSize Dimensions of the game grid.
 * @param didWin Whether the player won the match.
 * @param isTimeOut Whether the game ended due to a timeout.
 * @param timeSpent Total time spent in the match in seconds.
 * @param isHardMode Whether the game was played in hard difficulty mode.
 * @param onPlayAgain Callback to navigate back to the game screen for a new match.
 * @param onBackToMenu Callback to safely navigate back to the main menu.
 * @param onExit Callback to close the application.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    playerName: String,
    gridSize: Int,
    didWin: Boolean,
    isTimeOut: Boolean,
    timeSpent: Int,
    isHardMode: Boolean,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current

    val currentDateTime = remember {
        SimpleDateFormat("MMM dd, yyyy h:mm:ss a", Locale.getDefault()).format(Date())
    }

    val resultMessage = when {
        didWin -> stringResource(id = R.string.log_outcome_win)
        isTimeOut -> stringResource(id = R.string.log_outcome_loss_time)
        else -> stringResource(id = R.string.log_outcome_loss_ai)
    }

    val diffMessage = if (isHardMode) {
        stringResource(id = R.string.results_diff_hard)
    } else {
        stringResource(id = R.string.results_diff_easy)
    }

    val initialLog = stringResource(
        id = R.string.log_format,
        playerName,
        gridSize,
        gridSize,
        diffMessage,
        timeSpent,
        resultMessage
    )

    var dateTimeText by rememberSaveable { mutableStateOf(currentDateTime) }
    var logText by rememberSaveable { mutableStateOf(initialLog) }

    val defaultEmail = stringResource(id = R.string.results_default_email)
    var emailText by rememberSaveable { mutableStateOf(defaultEmail) }

    val emailSubject = stringResource(id = R.string.email_subject_format, dateTimeText)
    val chooserTitle = stringResource(id = R.string.results_email_chooser)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.results_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackToMenu) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(id = R.string.cd_back_to_menu)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = dateTimeText,
                onValueChange = { dateTimeText = it },
                label = { Text(text = stringResource(id = R.string.results_date_time)) },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = logText,
                onValueChange = { logText = it },
                label = { Text(text = stringResource(id = R.string.results_log_data)) },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            OutlinedTextField(
                value = emailText,
                onValueChange = { emailText = it },
                label = { Text(text = stringResource(id = R.string.results_email_recipient)) },
                isError = emailText.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { sendEmail(context, emailText, emailSubject, logText, chooserTitle) },
                enabled = emailText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.results_btn_send))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(id = R.string.results_btn_play_again))
                }

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(id = R.string.menu_btn_exit))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Triggers an Android Intent to send an email with the match log.
 *
 * @param context The current context.
 * @param email Recipient email address.
 * @param subject Subject of the email.
 * @param logContent The body of the email containing the match log.
 * @param chooserTitle The title for the application chooser.
 */
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

    try {
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    } catch (e: Exception) {
        // Handle case where no email app is available
    }
}