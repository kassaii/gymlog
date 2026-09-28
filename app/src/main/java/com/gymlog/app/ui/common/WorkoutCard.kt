package com.gymlog.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
    // Uso de card permite mejor estructura. Añade separación entre tarjetas.
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Imagen

                Column() {
                    // Nombre de usuario
                    Text(
                        text = workout.username,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(4.dp))

                    // Fecha y hora
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

            // Título del entrenamiento
            Text(
                text = workout.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Row con datos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(20.dp)
            ) {

                Text("⏲\uFE0F ${workout.durationMinutes} min")
                Spacer(Modifier.width(8.dp))
                VerticalDivider()
                Spacer(Modifier.width(8.dp))
                Text("\uD83C\uDFCB\uFE0F ${WeightFormatter.format(workout.totalVolume, weightUnit)}")
                Spacer(Modifier.width(8.dp))
                VerticalDivider()
                Spacer(Modifier.width(8.dp))
                Text("\uD83C\uDFAF ${workout.totalSets} series")
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(2.dp))

            // Ejercicios
            workout.exercises.take(3).forEach { exercise ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Imagen del ejercicio

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