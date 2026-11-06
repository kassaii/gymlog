package com.gymlog.app.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymlog.app.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkoutScreen(
    viewModel: CreateWorkoutViewModel,
    onWorkoutSaved: () -> Unit,
    onMinimize: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    // Start timer on first composition
    LaunchedEffect(Unit) {
        viewModel.startWorkoutIfNeeded()
    }

    // Navigate back when workout is saved
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            onWorkoutSaved()
            viewModel.onSaveHandled()
        }
    }

    // Navigate back when workout is discarded
    LaunchedEffect(state.discardSuccess) {
        if (state.discardSuccess) {
            onMinimize()
            viewModel.onDiscardHandled()
        }
    }

    // Reload exercises if they are empty
    LaunchedEffect(Unit) {
        viewModel.reloadExercisesIfNeeded()
    }

    val totalVolume = state.exercises
        .flatMap { it.sets }
        .filter { !it.isWarmup }
        .sumOf { it.weight * it.reps }

    val completedSets = state.exercises
        .flatMap { it.sets }
        .filter { !it.isWarmup && it.isCompleted }
        .size

    val canFinish = !state.isLoading && state.exercises.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear entrenamiento") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onMinimize) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Minimize"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showFinishDialog = true },
                        enabled = canFinish
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Finalizar",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { viewModel.onShowExerciseDialog(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Añadir ejercicio",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showDiscardDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Descartar entrenamiento",
                            color = MaterialTheme.colorScheme.error
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
                .padding(horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.title.isEmpty()) {
                            Text(
                                text = "Título del entrenamiento",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                        BasicTextField(
                            value = state.title,
                            onValueChange = viewModel::onTitleChange,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        if (state.description.isEmpty()) {
                            Text(
                                text = "Describe cómo te ha ido el entreno...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        BasicTextField(
                            value = state.description,
                            onValueChange = viewModel::onDescriptionChange,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = if (state.isTimerRunning) Icons.Default.Timer else Icons.Default.PlayArrow,
                        label = "Duración",
                        value = TimeFormatter.formatDuration(state.elapsedSeconds),
                        onClick = {
                            if (state.isTimerRunning) viewModel.onPauseTimer()
                            else viewModel.onResumeTimer()
                        }
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FitnessCenter,
                        label = "Volumen",
                        value = "${totalVolume.toInt()} kg",
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Check,
                        label = "Series",
                        value = "$completedSets",
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (state.exercises.isEmpty()) {
                item {
                    EmptyWorkoutState(onAddExercise = { viewModel.onShowExerciseDialog(true) })
                }
            }

            itemsIndexed(state.exercises) { index, exercise ->
                val isThisExpanded = state.expandedSet?.first == index
                val expandedSetIndex = if (isThisExpanded) state.expandedSet?.second else null

                // Convert the map of setIndex -> set to an ordered list of sets.
                // If no previous data exists for this exercise, pass an empty list.
                val previousSets = state.previousSetsByExercise[exercise.exerciseName]
                    ?.toSortedMap()
                    ?.values
                    ?.toList()
                    ?: emptyList()

                WorkoutExerciseCard(
                    exercise = exercise,
                    isFirst = index == 0,
                    isLast = index == state.exercises.size - 1,
                    expandedSetIndex = expandedSetIndex,
                    onToggleSetExpansion = { setIndex -> viewModel.onToggleSetExpansion(index, setIndex) },
                    onRemoveExercise = { viewModel.onRemoveExercise(index) },
                    onMoveUp = { viewModel.onMoveExercise(index, index - 1) },
                    onMoveDown = { viewModel.onMoveExercise(index, index + 1) },
                    onAddSet = { viewModel.onAddSet(index) },
                    onRemoveSet = { setIndex -> viewModel.onRemoveSet(index, setIndex) },
                    onSetChange = { setIndex, reps, weight -> viewModel.onSetChange(index, setIndex, reps, weight) },
                    onToggleWarmup = { setIndex -> viewModel.onToggleWarmup(index, setIndex) },
                    onToggleCheck = { setIndex -> viewModel.onToggleCheck(index, setIndex) },
                    previousSets = previousSets
                )
            }
        }

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

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finalizar entrenamiento") },
            text = { Text("¿Estás seguro de que quieres finalizar el entrenamiento? Se guardará en tu perfil.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFinishDialog = false
                        viewModel.onFinishWorkout()
                    }
                ) {
                    Text("Finalizar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Descartar entrenamiento") },
            text = { Text("¿Estás seguro de que quieres descartar el entrenamiento? Se perderán todos los datos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.onDiscardWorkout()
                    }
                ) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = { onClick?.invoke() },
        enabled = onClick != null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyWorkoutState(onAddExercise: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No hay ejercicios todavía",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Añade tu primer ejercicio para empezar el entreno",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}