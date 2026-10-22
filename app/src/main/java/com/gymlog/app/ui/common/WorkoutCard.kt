package com.gymlog.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gymlog.app.data.model.ExerciseSet
import com.gymlog.app.data.model.Workout
import com.gymlog.app.data.model.WorkoutExercise
import com.gymlog.app.util.DateFormatter
import com.gymlog.app.util.WeightFormatter

@Composable
fun WorkoutCard(
    workout: Workout,
    weightUnit: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Image

                Column() {
                    // Username
                    Text(
                        text = workout.username,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(4.dp))

                    // Date - Hour
                    Text(
                        text = DateFormatter.formatDateTime(workout.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }


            }

            Spacer(Modifier.height(6.dp))
            HorizontalDivider()

            Spacer(Modifier.height(6.dp))

            // Title
            Text(
                text = workout.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Data row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(20.dp)
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${workout.durationMinutes} min")
                }

                Spacer(Modifier.width(8.dp))
                VerticalDivider()
                Spacer(Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(WeightFormatter.format(workout.totalVolume, weightUnit))
                }

                Spacer(Modifier.width(8.dp))
                VerticalDivider()
                Spacer(Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${workout.totalSets} series")
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(2.dp))

            // Exercises
            workout.exercises.take(3).forEach { exercise ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image

                    Text(
                        text = "${exercise.sets.size} series",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WorkoutCardPreview() {
    WorkoutCard(
        workout = Workout(
            id = "1",
            username = "juanfit",
            title = "PECHO",
            date = System.currentTimeMillis(),
            durationMinutes = 45,
            totalVolume = 4200.0,
            totalSets = 12,
            exercises = listOf(
                WorkoutExercise(
                    exerciseName = "Press plano en máquina",
                    sets = listOf(
                        ExerciseSet(reps = 8, weight = 60.0),
                        ExerciseSet(reps = 8, weight = 65.0),
                        ExerciseSet(reps = 8, weight = 70.0)
                    )
                ),
                WorkoutExercise(
                    exerciseName = "Press inclinado con mancuernas",
                    sets = listOf(
                        ExerciseSet(reps = 10, weight = 50.0),
                        ExerciseSet(reps = 10, weight = 55.0)
                    )
                ),
                WorkoutExercise(
                    exerciseName = "Fondos",
                    sets = listOf(
                        ExerciseSet(reps = 12, weight = 0.0),
                        ExerciseSet(reps = 12, weight = 0.0)
                    )
                )
            )
        ),
        weightUnit = "kg"
    )
}