package com.gymlog.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gymlog.app.data.model.ExerciseSet
import com.gymlog.app.data.model.WorkoutExercise
import com.gymlog.app.ui.theme.LocalWorkoutColors
import com.gymlog.app.util.DateFormatter
import com.gymlog.app.util.NumberFormatter
import com.gymlog.app.util.WeightFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: String,
    onBack: (Boolean) -> Unit,
    viewModel: WorkoutDetailViewModel = viewModel(key = workoutId)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }

    LaunchedEffect(state.deleteSuccess) {
        if (state.deleteSuccess) {
            onBack(true)
            viewModel.onDeleteHandled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detalles del entreno",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = { onBack(false) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar entrenamiento",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            state.workout != null -> {
                val workout = state.workout!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = workout.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (workout.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = workout.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = DateFormatter.formatDateTime(workout.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Timer,
                                label = "Duración",
                                value = "${workout.durationMinutes} min"
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.FitnessCenter,
                                label = "Volumen",
                                value = WeightFormatter.format(workout.totalVolume, "kg")
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.List,
                                label = "Series",
                                value = "${workout.totalSets}"
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.TrendingUp,
                                label = "Progresos",
                                value = "${workout.progressCount}"
                            )
                        }
                    }

                    items(workout.exercises) { exercise ->
                        ReadOnlyWorkoutExerciseCard(exercise = exercise)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar entrenamiento") },
            text = { Text("¿Estás seguro de que quieres eliminar este entrenamiento? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteWorkout(workoutId)
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
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
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ReadOnlyWorkoutExerciseCard(exercise: WorkoutExercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = exercise.exercisePhotoUrl,
                    contentDescription = exercise.exerciseName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = exercise.muscleGroup.ifEmpty { "Exercise" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCell("SERIE", Modifier.width(44.dp))
                Spacer(modifier = Modifier.width(6.dp))
                HeaderCell("KG", Modifier.weight(1f), icon = Icons.Default.FitnessCenter)
                Spacer(modifier = Modifier.width(6.dp))
                HeaderCell("REPS", Modifier.weight(1f), icon = Icons.Default.Repeat)
                Spacer(modifier = Modifier.width(6.dp))
                HeaderCell("PROG.", Modifier.weight(1.2f), icon = Icons.Default.TrendingUp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                exercise.sets.forEachIndexed { index, set ->
                    ReadOnlySetRow(label = "${index + 1}", set = set)
                }
            }
        }
    }
}

@Composable
private fun ReadOnlySetRow(label: String, set: ExerciseSet) {
    val workoutColors = LocalWorkoutColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        // Set number / Check icon
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            set.isWarmup -> workoutColors.warmup.copy(alpha = 0.2f)
                            set.isCompleted -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (set.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (set.isWarmup) workoutColors.warmup else workoutColors.up,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (set.isWarmup) workoutColors.warmup else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Weight
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = NumberFormatter.toDisplay(set.weight),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (set.isWarmup) workoutColors.warmup else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Reps
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "× ${set.reps}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (set.isWarmup) workoutColors.warmup else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Progress badge
        Box(
            modifier = Modifier.weight(1.2f),
            contentAlignment = Alignment.Center
        ) {
            val weightDiff = set.progressDiff ?: 0.0
            val repsDiff = set.repsProgressDiff ?: 0

            if (weightDiff != 0.0 || repsDiff != 0) {
                // Mixed signal: weight down, reps up
                val isMixedSignal = weightDiff < -0.001 && repsDiff > 0

                val progressText = when {
                    isMixedSignal -> "${NumberFormatter.toDisplay(weightDiff)} / +$repsDiff"
                    weightDiff > 0.001 -> "+${NumberFormatter.toDisplay(weightDiff)} kg"
                    weightDiff < -0.001 -> "${NumberFormatter.toDisplay(weightDiff)} kg"
                    repsDiff > 0 -> if (repsDiff == 1) "+1 rep" else "+$repsDiff reps"
                    repsDiff < 0 -> {
                        val absReps = kotlin.math.abs(repsDiff)
                        if (absReps == 1) "-1 rep" else "-$absReps reps"
                    }
                    else -> "-"
                }

                val progressTextColor = when {
                    weightDiff > 0.001 -> workoutColors.up
                    weightDiff == 0.0 && repsDiff > 0 -> workoutColors.up
                    weightDiff < -0.001 && repsDiff > 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                    weightDiff < -0.001 -> workoutColors.down
                    weightDiff == 0.0 && repsDiff < 0 -> workoutColors.down
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }

                val progressBackground = when {
                    weightDiff > 0.001 -> workoutColors.upBackground
                    weightDiff == 0.0 && repsDiff > 0 -> workoutColors.upBackground
                    weightDiff < -0.001 && repsDiff > 0 -> MaterialTheme.colorScheme.surfaceVariant
                    weightDiff < -0.001 -> workoutColors.downBackground
                    weightDiff == 0.0 && repsDiff < 0 -> workoutColors.downBackground
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(progressBackground)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMixedSignal) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = progressTextColor
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    } else {
                        Icon(
                            imageVector = if (weightDiff > 0.001 || repsDiff > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = progressTextColor
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressTextColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}