package com.gymlog.app.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymlog.app.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkoutScreen(
    viewModel: CreateWorkoutViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear entrenamiento") },
                navigationIcon = {
                    // Dismiss button
                    IconButton(onClick = { viewModel.onDiscardWorkout() }) {
                        Icon(Icons.Default.Close, contentDescription = "Descartar")
                    }
                },
                actions = {
                    // Finish button
                    IconButton(
                        onClick = { viewModel.onFinishWorkout() },
                        enabled = !state.isLoading
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Finalizar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Chronometer
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = TimeFormatter.formatDuration(state.elapsedSeconds),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(onClick = {
                        if (state.isTimerRunning) viewModel.onPauseTimer()
                        else viewModel.onResumeTimer()
                    }) {
                        Icon(
                            imageVector = if (state.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isTimerRunning) "Pausar" else "Reanudar"
                        )
                    }
                }
            }

            // Add exercise button
            item {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Añadir ejercicio")
                }
            }
        }
    }
}