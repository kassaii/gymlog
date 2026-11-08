package com.gymlog.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gymlog.app.data.model.ExerciseSet
import com.gymlog.app.data.model.Workout
import com.gymlog.app.data.model.WorkoutExercise
import com.gymlog.app.ui.theme.LocalWorkoutColors
import com.gymlog.app.ui.theme.WorkoutColors
import com.gymlog.app.util.DateFormatter
import com.gymlog.app.util.WeightFormatter

private const val MAX_VISIBLE_EXERCISES = 3

@Composable
fun WorkoutCard(
    workout: Workout,
    weightUnit: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val workoutColors = LocalWorkoutColors.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // User header with photo with ring, username and date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (workout.userPhotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = workout.userPhotoUrl,
                            contentDescription = "Profile picture of ${workout.username}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = DateFormatter.formatSocialDateTime(workout.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title and description
            Text(
                text = workout.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (workout.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = workout.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    value = "${workout.durationMinutes} min",
                    label = "Duración",
                    modifier = Modifier.weight(1f)
                )
                StatDivider()
                StatItem(
                    value = WeightFormatter.formatCompact(workout.totalVolume, weightUnit),
                    label = "Volumen",
                    modifier = Modifier.weight(1f)
                )
                StatDivider()
                StatItem(
                    value = "${workout.totalSets}",
                    label = "Series",
                    modifier = Modifier.weight(1f)
                )
                if (workout.progressCount > 0) {
                    StatDivider()
                    ProgressStat(
                        count = workout.progressCount,
                        workoutColors = workoutColors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Exercises
            if (workout.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                workout.exercises.take(MAX_VISIBLE_EXERCISES).forEach { exercise ->
                    ExerciseRow(exercise = exercise, weightUnit = weightUnit)
                }

                val remaining = workout.exercises.size - MAX_VISIBLE_EXERCISES
                if (remaining > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (remaining == 1) "+1 ejercicio más" else "+$remaining ejercicios más",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: WorkoutExercise,
    weightUnit: String
) {
    val workingSets = exercise.sets.filter { !it.isWarmup }.ifEmpty { exercise.sets }
    val best = workingSets.maxWithOrNull(compareBy<ExerciseSet>({ it.weight }, { it.reps }))

    val seriesText = if (workingSets.size == 1) "1 serie" else "${workingSets.size} series"
    val detail = when {
        best == null -> seriesText
        best.weight > 0.0 -> "$seriesText · ${
            WeightFormatter.format(
                best.weight,
                weightUnit
            )
        } × ${best.reps}"

        else -> "$seriesText · ${best.reps} reps"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (exercise.exercisePhotoUrl.isNotBlank()) {
                AsyncImage(
                    model = exercise.exercisePhotoUrl,
                    contentDescription = exercise.exerciseName,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressStat(
    count: Int,
    workoutColors: WorkoutColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(workoutColors.upBackground)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = workoutColors.up
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "+$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = workoutColors.up
            )
        }
        Text(
            text = "Progresos",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}