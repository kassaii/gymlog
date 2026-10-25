package com.gymlog.app.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gymlog.app.data.model.ExerciseSet
import com.gymlog.app.data.model.WorkoutExercise
import com.gymlog.app.ui.theme.LocalWorkoutColors
import com.gymlog.app.util.NumberFormatter

private val SetBoxSize = 40.dp
private val ColumnGap = 6.dp

private data class SetRowInfo(
    val label: String,
    val previous: ExerciseSet?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExerciseCard(
    exercise: WorkoutExercise,
    isFirst: Boolean,
    isLast: Boolean,
    expandedSetIndex: Int?,
    onToggleSetExpansion: (Int) -> Unit,
    onRemoveExercise: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onSetChange: (Int, Int, Double) -> Unit,
    onToggleWarmup: (Int) -> Unit,
    onToggleCheck: (Int) -> Unit,
    previousSets: List<ExerciseSet> = emptyList()
) {
    val workoutColors = LocalWorkoutColors.current

    val rows = remember(exercise.sets, previousSets) {
        val prevWarmups = previousSets.filter { it.isWarmup }
        val prevWorking = previousSets.filter { !it.isWarmup }
        var warmupCount = 0
        var workingCount = 0
        exercise.sets.map { set ->
            if (set.isWarmup) {
                val info = SetRowInfo("C", prevWarmups.getOrNull(warmupCount))
                warmupCount++
                info
            } else {
                val info = SetRowInfo("${workingCount + 1}", prevWorking.getOrNull(workingCount))
                workingCount++
                info
            }
        }
    }

    val subtitle = exercise.muscleGroup.ifEmpty { "Ejercicio" }

    var showExerciseSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    error = painterResource(id = android.R.drawable.ic_menu_gallery),
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
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { showExerciseSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones del ejercicio",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCell("SERIE", Modifier.width(SetBoxSize))
                Spacer(modifier = Modifier.width(ColumnGap))
                HeaderCell("KG", Modifier.weight(1f), icon = Icons.Default.FitnessCenter)
                Spacer(modifier = Modifier.width(ColumnGap))
                HeaderCell("REPS", Modifier.weight(1f), icon = Icons.Default.Repeat)
                Spacer(modifier = Modifier.width(ColumnGap))
                HeaderCell("PROG.", Modifier.weight(1.2f), icon = Icons.Default.TrendingUp)
                Spacer(modifier = Modifier.width(ColumnGap))
                Spacer(modifier = Modifier.width(SetBoxSize))
            }

            Spacer(modifier = Modifier.height(4.dp))

            exercise.sets.forEachIndexed { setIndex, set ->
                val info = rows[setIndex]
                SetRow(
                    label = info.label,
                    set = set,
                    previous = info.previous,
                    isExpanded = expandedSetIndex == setIndex,
                    workoutColors = workoutColors,
                    onExpand = { onToggleSetExpansion(setIndex) },
                    onSetChange = { reps, weight -> onSetChange(setIndex, reps, weight) },
                    onToggleWarmup = { onToggleWarmup(setIndex) },
                    onToggleCheck = { onToggleCheck(setIndex) },
                    onRemoveSet = { onRemoveSet(setIndex) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddSet,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir serie")
            }
        }
    }

    if (showExerciseSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExerciseSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Opciones de ${exercise.exerciseName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ListItem(
                    headlineContent = { Text("Mover arriba") },
                    leadingContent = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                    modifier = Modifier.clickable(enabled = !isFirst) {
                        showExerciseSheet = false
                        onMoveUp()
                    }
                )

                ListItem(
                    headlineContent = { Text("Mover abajo") },
                    leadingContent = {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable(enabled = !isLast) {
                        showExerciseSheet = false
                        onMoveDown()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ListItem(
                    headlineContent = {
                        Text(
                            "Eliminar ejercicio",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable {
                        showExerciseSheet = false
                        onRemoveExercise()
                    }
                )
            }
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier,
    icon: ImageVector? = null
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SetRow(
    label: String,
    set: ExerciseSet,
    previous: ExerciseSet?,
    isExpanded: Boolean,
    workoutColors: com.gymlog.app.ui.theme.WorkoutColors,
    onExpand: () -> Unit,
    onSetChange: (Int, Double) -> Unit,
    onToggleWarmup: () -> Unit,
    onToggleCheck: () -> Unit,
    onRemoveSet: () -> Unit
) {
    val displayWeight = if (set.weight > 0.0) set.weight else (previous?.weight ?: 0.0)
    val displayReps = if (set.reps > 0) set.reps else (previous?.reps ?: 0)

    val summaryColor = if (set.isWarmup) {
        workoutColors.warmup
    } else {
        when {
            set.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            set.weight == 0.0 && set.reps == 0 && previous != null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.onSurface
        }
    }

    val neutralGray = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    var showSetOptionsSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 1.dp,
                    shape = RoundedCornerShape(12.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(12.dp))
                .background(
                    color = when {
                        set.isWarmup && set.isCompleted -> workoutColors.warmupCompletedBackground
                        set.isWarmup -> workoutColors.warmupBackground
                        set.isCompleted -> workoutColors.completedSet
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
                .combinedClickable(
                    onClick = { onExpand() },
                    onLongClick = { showSetOptionsSheet = true }
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier.width(SetBoxSize),
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

            Spacer(modifier = Modifier.width(ColumnGap))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = NumberFormatter.toDisplay(displayWeight),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = summaryColor
                )
            }

            Spacer(modifier = Modifier.width(ColumnGap))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "× $displayReps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = summaryColor
                )
            }

            Spacer(modifier = Modifier.width(ColumnGap))

            Box(
                modifier = Modifier.weight(1.2f),
                contentAlignment = Alignment.Center
            ) {
                val weightDiff = if (previous != null && !set.isWarmup) displayWeight - previous.weight else 0.0
                val repsDiff = if (previous != null && !set.isWarmup) displayReps - previous.reps else 0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                set.isWarmup -> workoutColors.warmup.copy(alpha = 0.15f)
                                set.isCompleted && (weightDiff > 0.001 || repsDiff > 0) -> MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                weightDiff > 0.001 || repsDiff > 0 -> workoutColors.upBackground
                                weightDiff < -0.001 || repsDiff < 0 -> workoutColors.downBackground
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            }
                        )
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val text = when {
                        set.isWarmup -> "-"
                        weightDiff > 0.001 -> "▲ +${NumberFormatter.toDisplay(weightDiff)} kg"
                        weightDiff < -0.001 -> "▼ ${NumberFormatter.toDisplay(weightDiff)} kg"
                        repsDiff > 0 -> if (repsDiff == 1) "▲ +1 rep" else "▲ +$repsDiff reps"
                        repsDiff < 0 -> {
                            val absReps = kotlin.math.abs(repsDiff)
                            if (absReps == 1) "▼ -1 rep" else "▼ -$absReps reps"
                        }
                        else -> "-"
                    }

                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            set.isWarmup -> workoutColors.warmup.copy(alpha = 0.7f)
                            weightDiff > 0.001 || repsDiff > 0 -> workoutColors.up
                            weightDiff < -0.001 || repsDiff < 0 -> workoutColors.down
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.width(ColumnGap))

            Box(
                modifier = Modifier.width(SetBoxSize),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                UnifiedStepInput(
                    value = if (set.weight > 0) NumberFormatter.toInputText(set.weight) else "",
                    hint = previous?.weight?.let { NumberFormatter.toInputText(it) } ?: "0",
                    onValueChange = { v ->
                        val parsed = v.toDoubleOrNull() ?: 0.0
                        val currentReps = if (set.reps > 0) set.reps else (previous?.reps ?: 0)
                        onSetChange(currentReps, parsed)
                    },
                    onMinusClick = {
                        val baseWeight = if (set.weight > 0) set.weight else (previous?.weight ?: 0.0)
                        val newWeight = (baseWeight - 2.5).coerceAtLeast(0.0)
                        val currentReps = if (set.reps > 0) set.reps else (previous?.reps ?: 0)
                        onSetChange(currentReps, newWeight)
                    },
                    onPlusClick = {
                        val baseWeight = if (set.weight > 0) set.weight else (previous?.weight ?: 0.0)
                        val newWeight = baseWeight + 2.5
                        val currentReps = if (set.reps > 0) set.reps else (previous?.reps ?: 0)
                        onSetChange(currentReps, newWeight)
                    },
                    keyboardType = KeyboardType.Decimal,
                    label = "KG",
                    workoutColors = workoutColors,
                    trailingContent = {
                        if (previous != null && !set.isWarmup) {
                            val weightDiff = displayWeight - previous.weight
                            if (weightDiff > 0.001) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(workoutColors.upBackground)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "▲+${NumberFormatter.toDisplay(weightDiff)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = workoutColors.up,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            } else if (weightDiff < -0.001) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(workoutColors.downBackground)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "▼${NumberFormatter.toDisplay(weightDiff)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = workoutColors.down,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                UnifiedStepInput(
                    value = if (set.reps > 0) set.reps.toString() else "",
                    hint = previous?.reps?.toString() ?: "0",
                    onValueChange = { v ->
                        val parsed = v.toIntOrNull() ?: 0
                        val currentWeight = if (set.weight > 0) set.weight else (previous?.weight ?: 0.0)
                        onSetChange(parsed, currentWeight)
                    },
                    onMinusClick = {
                        val baseReps = if (set.reps > 0) set.reps else (previous?.reps ?: 0)
                        val newReps = (baseReps - 1).coerceAtLeast(0)
                        val currentWeight = if (set.weight > 0) set.weight else (previous?.weight ?: 0.0)
                        onSetChange(newReps, currentWeight)
                    },
                    onPlusClick = {
                        val baseReps = if (set.reps > 0) set.reps else (previous?.reps ?: 0)
                        val newReps = baseReps + 1
                        val currentWeight = if (set.weight > 0) set.weight else (previous?.weight ?: 0.0)
                        onSetChange(newReps, currentWeight)
                    },
                    keyboardType = KeyboardType.Number,
                    label = "REPS",
                    workoutColors = workoutColors,
                    trailingContent = {
                        if (previous != null && !set.isWarmup) {
                            val weightDiff = displayWeight - previous.weight
                            val repsDiff = displayReps - previous.reps

                            if (repsDiff > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(workoutColors.upBackground)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "▲+$repsDiff",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = workoutColors.up,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            } else if (repsDiff < 0) {
                                val isWeightUp = weightDiff > 0.001
                                val bgColor = if (isWeightUp) MaterialTheme.colorScheme.surfaceVariant else workoutColors.downBackground
                                val textColor = if (isWeightUp) neutralGray else workoutColors.down

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bgColor)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "▼$repsDiff",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                )

                if (previous != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Anterior",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${NumberFormatter.toDisplay(previous.weight)} kg × ${previous.reps}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val oneRepMax = if (displayReps > 0 && displayWeight > 0) {
                        NumberFormatter.toDisplay(displayWeight * (1 + (displayReps / 30.0)))
                    } else {
                        "-"
                    }

                    Column(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(
                            text = "1RM ESTIMADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$oneRepMax kg",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val completedGreen = Color(0xFF529B56)

                    Button(
                        onClick = onToggleCheck,
                        modifier = Modifier
                            .padding(end = 12.dp, top = 14.dp, bottom = 14.dp)
                            .fillMaxHeight(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (set.isCompleted) completedGreen else Color.Transparent,
                            contentColor = if (set.isCompleted) Color.White else MaterialTheme.colorScheme.primary
                        ),
                        border = if (!set.isCompleted) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        } else null
                    ) {
                        Text(
                            if (set.isCompleted) "Completada" else "Completar",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

    if (showSetOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSetOptionsSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Opciones de la serie $label",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ListItem(
                    headlineContent = { Text(if (set.isWarmup) "Convertir en efectiva" else "Convertir en calentamiento") },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (set.isWarmup) MaterialTheme.colorScheme.primary else workoutColors.warmup)
                        )
                    },
                    modifier = Modifier.clickable {
                        showSetOptionsSheet = false
                        onToggleWarmup()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ListItem(
                    headlineContent = { Text("Eliminar serie", color = MaterialTheme.colorScheme.error) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable {
                        showSetOptionsSheet = false
                        onRemoveSet()
                    }
                )
            }
        }
    }
}

@Composable
private fun SquareStepButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun UnifiedStepInput(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    onMinusClick: () -> Unit,
    onPlusClick: () -> Unit,
    keyboardType: KeyboardType,
    label: String,
    workoutColors: com.gymlog.app.ui.theme.WorkoutColors,
    trailingContent: (@Composable () -> Unit)? = null
) {
    var text by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (text != value) text = value
    }

    val textStyle = MaterialTheme.typography.titleLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SquareStepButton(
            text = "-",
            onClick = onMinusClick
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                BasicTextField(
                    value = text,
                    onValueChange = { raw ->
                        val v = if (keyboardType == KeyboardType.Decimal) raw.replace(',', '.') else raw
                        if (v.length <= 6 && (v.all { it.isDigit() || it == '.' } || keyboardType == KeyboardType.Number)) {
                            if (keyboardType == KeyboardType.Decimal && v.count { it == '.' } > 1) return@BasicTextField
                            text = v
                            onValueChange(v)
                        }
                    },
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box {
                            if (text.isEmpty()) {
                                Text(
                                    text = hint,
                                    style = textStyle.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            if (trailingContent != null) {
                Box(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    trailingContent()
                }
            }
        }

        SquareStepButton(
            text = "+",
            onClick = onPlusClick
        )
    }
}