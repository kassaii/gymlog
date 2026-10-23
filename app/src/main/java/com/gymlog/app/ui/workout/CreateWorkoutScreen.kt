package com.gymlog.app.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
                    IconButton(onClick = { viewModel.onDiscardWorkout() }) {
                        Icon(Icons.Default.Close, contentDescription = "Descartar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onFinishWorkout() },
                        enabled = !state.isLoading
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Finalizar")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = { viewModel.onFinishWorkout() },
                    enabled = !state.isLoading && state.exercises.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp)
                        .imePadding(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Finalizar entrenamiento",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
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
                    onClick = { viewModel.onShowExerciseDialog(true) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Añadir ejercicio")
                }
            }

            // Workout Exercises List
            itemsIndexed(state.exercises) { index, exercise ->
                WorkoutExerciseCard(
                    exercise = exercise,
                    exerciseIndex = index,
                    onRemoveExercise = { viewModel.onRemoveExercise(index) },
                    onAddSet = { viewModel.onAddSet(index) },
                    onRemoveSet = { setIndex -> viewModel.onRemoveSet(index, setIndex) },
                    onSetChange = { setIndex, reps, weight ->
                        viewModel.onSetChange(index, setIndex, reps, weight)
                    }
                )
            }
        }

        // Exercise selection dialog
        if (state.isExerciseDialogVisible) {
            ExerciseSelectionDialog(
                exercises = state.availableExercises,
                selectedIds = state.selectedExerciseIds,
                searchQuery = state.searchQuery,
                selectedMuscleGroup = state.selectedMuscleGroup,
                isLoading = state.isLoadingExercises,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onMuscleGroupSelect = viewModel::onMuscleGroupFilterChange,
                onToggleSelection = viewModel::onToggleExerciseSelection,
                onConfirm = viewModel::onConfirmAddExercises,
                onDismiss = { viewModel.onShowExerciseDialog(false) }
            )
        }
    }
}