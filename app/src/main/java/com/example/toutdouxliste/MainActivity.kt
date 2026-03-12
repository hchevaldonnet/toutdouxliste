package com.example.toutdouxliste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.toutdouxliste.data.Etat
import com.example.toutdouxliste.data.Task
import com.example.toutdouxliste.data.TaskDao
import com.example.toutdouxliste.data.TaskDatabase
import com.example.toutdouxliste.ui.theme.ToutDouxListeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.core.models.Shape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import com.example.toutdouxliste.data.Periodicite
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.ActivityNavigatorExtras
import coil.compose.rememberAsyncImagePainter
import android.content.Context
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import java.io.FileOutputStream
import java.io.File
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private lateinit var database: TaskDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = TaskDatabase.getDatabase(this)

        setContent {
            ToutDouxListeTheme {
                MaterialTheme {
                    AppNavigation(database.taskDao())
                }
            }
        }
    }
}

fun saveImageToInternalStorage(context: Context, uri: Uri): String {

    val inputStream = context.contentResolver.openInputStream(uri)

    val fileName = "task_${System.currentTimeMillis()}.jpg"

    val file = File(context.filesDir, fileName)

    val outputStream = FileOutputStream(file)

    inputStream?.copyTo(outputStream)

    inputStream?.close()
    outputStream.close()

    return file.absolutePath
}

fun getThemeLevel(tasksDone: Int): Int {

    return when {
        tasksDone >= 100 -> 4
        tasksDone >= 50 -> 3
        tasksDone >= 25 -> 2
        tasksDone >= 10 -> 1
        else -> 0
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ToutDouxListeTheme {
        Greeting("Android")
    }
}

@Composable
fun AppNavigation(taskDao : TaskDao) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(navController, taskDao)
        }

        composable("new") {
            NewScreen(navController, taskDao, null)
        }

        composable("edit/{taskId}") { backStackEntry ->

            val taskId = backStackEntry.arguments?.getString("taskId")!!.toInt()

            NewScreen(navController, taskDao, taskId)
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewScreen(navController: NavHostController, taskDao: TaskDao, taskId: Int?) {

    val scope = rememberCoroutineScope()

    var titre by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var etat by remember { mutableStateOf(Etat.A_FAIRE) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(
        initialHour = 12,
        initialMinute = 0
    )

    var periodicite by remember { mutableStateOf(Periodicite.AUCUNE) }

    var important by remember { mutableStateOf(false) }

    var imageDesc by remember { mutableStateOf<Uri?>(null) }
    var imagePath by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        uri -> if (uri != null) {
            val savedPath = saveImageToInternalStorage(context, uri)

            imageDesc = Uri.fromFile(File(savedPath))
            imagePath = savedPath
    }
    }

    LaunchedEffect(taskId) {

        if (taskId != null) {

            val task = taskDao.getTaskById(taskId)

            titre = task.titre
            description = task.description
            etat = task.etat
            important = task.important
            periodicite = task.periodicite
            imagePath = task.imageDesc

            if (task.imageDesc != null) {
                imageDesc = Uri.fromFile(File(task.imageDesc))
            }

            datePickerState.selectedDateMillis =
                task.dateLimite
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

            timePickerState.hour = task.dateLimite.hour
            timePickerState.minute = task.dateLimite.minute
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "Nouvelle tâche" else "Modifier la tâche") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),

            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            TextField(
                value = titre,
                onValueChange = { titre = it },
                label = { Text("Titre") },
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { imagePicker.launch("image/*") }
            ) {
                Text("Ajouter une photo")
            }

            imageDesc?.let { uri ->

                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Image tâche",
                    modifier = Modifier.fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Text(
                text = "Date limite",
                style = MaterialTheme.typography.titleMedium
            )

            DatePicker(
                state = datePickerState
            )

            Text(
                text = "Heure limite",
                style = MaterialTheme.typography.titleMedium
            )

            TimePicker(
                state = timePickerState
            )

            Text(
                text = "Périodicité",
                style = MaterialTheme.typography.titleMedium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                item {
                    Button(onClick = { periodicite = Periodicite.AUCUNE }) {
                        Text("Aucune")
                    }
                }

                item {
                    Button(onClick = { periodicite = Periodicite.QUOTIDIENNE }) {
                        Text("Quotidienne")
                    }
                }

                item {
                    Button(onClick = { periodicite = Periodicite.HEBDOMADAIRE }) {
                        Text("Hebdomadaire")
                    }
                }

                item {
                    Button(onClick = { periodicite = Periodicite.MENSUELLE }) {
                        Text("Mensuelle")
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = important,
                    onCheckedChange = { important = it }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("Tâche importante")
            }

            Button(
                onClick = {

                    val selectedDate = datePickerState.selectedDateMillis

                    if (selectedDate != null) {

                        val localDate = Instant.ofEpochMilli(selectedDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()

                        val dateLimite = LocalDateTime.of(
                            localDate.year,
                            localDate.month,
                            localDate.dayOfMonth,
                            timePickerState.hour,
                            timePickerState.minute
                        )

                        val task = Task(
                            id = taskId ?: 0,
                            titre = titre,
                            description = description,
                            etat = etat,
                            dateLimite = dateLimite,
                            periodicite = periodicite,
                            important = important,
                            imageDesc = imagePath
                        )

                        scope.launch {

                            if (taskId == null) {
                                taskDao.insert(task)
                            } else {
                                taskDao.update(task)
                            }
                        }

                        val delay =
                            Duration.between(LocalDateTime.now(), dateLimite).toMillis()

                        val data = workDataOf(
                            "titre" to titre,
                            "description" to description
                        )

                        val request =
                            OneTimeWorkRequestBuilder<TaskNotificationWorker>()
                                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                .setInputData(data)
                                .build()

                        WorkManager.getInstance(navController.context)
                            .enqueue(request)

                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (taskId == null) "Créer la tâche" else "Modifier la tâche")
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController, taskDao: TaskDao) {

    val context = navController.context
    val prefs = context.getSharedPreferences("stats", android.content.Context.MODE_PRIVATE)

    var totalTasksDone by remember {
        mutableStateOf(prefs.getInt("tasks_done", 0))
    }

    val tasks by taskDao.getAllTasks().collectAsState(initial = emptyList())
    var filtreEtat by remember { mutableStateOf<Etat?>(null) }

    val themeLevel = getThemeLevel(totalTasksDone)

    val ButtonColor = when(themeLevel) {
        1 -> Color(0xFFB2FF59)
        2 -> Color(0xFFD1C4E9)
        3 -> Color(0xFFFFD54F)
        4 -> Color(0xFF90CAF9)
        else -> MaterialTheme.colorScheme.surface
    }

    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            kotlinx.coroutines.delay(2000)
            showConfetti = false
        }
    }

    Scaffold(

        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("new") },
                containerColor = ButtonColor
            ) {
                Text("+")
            }
        }

    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                Text(
                    text = "Vous avez accompli $totalTasksDone tâche(s).",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                Text(
                    text = when(themeLevel) {
                        4 -> "Thème Diamant débloqué"
                        3 -> "Thème Doré débloqué"
                        2 -> "Thème Améthyste débloqué"
                        1 -> "Thème Émeraude débloqué"
                        else -> "Continuez pour débloquer des apparences !"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                Button(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    onClick = {
                        totalTasksDone = 0
                        prefs.edit().putInt("tasks_done",0).apply()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor
                    )
                ) {
                    Text("Réinitialiser les statistiques")
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {

                    item {
                        Button(onClick = { filtreEtat = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonColor
                            )) {
                            Text("Toutes")
                        }
                    }

                    item {
                        Button(onClick = { filtreEtat = Etat.A_FAIRE },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonColor
                            )) {
                            Text("À faire")
                        }
                    }

                    item {
                        Button(onClick = { filtreEtat = Etat.EN_RETARD },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonColor
                            )) {
                            Text("En retard")
                        }
                    }

                    item {
                        Button(onClick = { filtreEtat = Etat.REALISE },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonColor
                            )) {
                            Text("Réalisées")
                        }
                    }
                }

                val tasksWithRealState = tasks.map { task ->

                    if (
                        task.etat != Etat.REALISE && task.dateLimite.isBefore(LocalDateTime.now())
                    ) {
                        task.copy(etat = Etat.EN_RETARD)
                    } else {
                        task
                    }
                }

                val filteredTasks =
                    if (filtreEtat == null) {
                        tasksWithRealState
                    } else {
                        tasksWithRealState.filter { it.etat == filtreEtat }
                    }.sortedByDescending { it.important }

                val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm")

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {

                    items(filteredTasks) { task ->

                        val etatAffiche =
                            if (task.etat != Etat.REALISE && task.dateLimite.isBefore(LocalDateTime.now())) {
                                Etat.EN_RETARD
                            } else {
                                task.etat
                            }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    navController.navigate("edit/${task.id}")
                                }
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),

                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Checkbox(
                                    checked = etatAffiche == Etat.REALISE,
                                    onCheckedChange = { checked ->

                                        val wasRealised = task.etat == Etat.REALISE

                                        val updatedTask = task.copy(
                                            etat = if (checked) Etat.REALISE else Etat.A_FAIRE
                                        )

                                        CoroutineScope(Dispatchers.IO).launch {
                                            taskDao.update(updatedTask)
                                        }

                                        if (checked && !wasRealised) {

                                            showConfetti = true

                                            val newCount = totalTasksDone + 1
                                            totalTasksDone = newCount
                                            prefs.edit().putInt("tasks_done", newCount).apply()

                                        } else if (!checked && wasRealised) {

                                            val newCount = (totalTasksDone - 1).coerceAtLeast(0)
                                            totalTasksDone = newCount
                                            prefs.edit().putInt("tasks_done", newCount).apply()
                                        }

                                        if (checked && task.periodicite != Periodicite.AUCUNE) {

                                            val nouvelleDate = when(task.periodicite) {

                                                Periodicite.QUOTIDIENNE ->
                                                    task.dateLimite.plusDays(1)

                                                Periodicite.HEBDOMADAIRE ->
                                                    task.dateLimite.plusWeeks(1)

                                                Periodicite.MENSUELLE ->
                                                    task.dateLimite.plusMonths(1)

                                                else -> task.dateLimite
                                            }

                                            val newTask = task.copy(
                                                id = 0,
                                                etat = Etat.A_FAIRE,
                                                dateLimite = nouvelleDate
                                            )

                                            CoroutineScope(Dispatchers.IO).launch {
                                                taskDao.insert(newTask)
                                            }
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column (
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = task.titre,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(task.description)

                                    Text("Date limite : ${task.dateLimite.format(dateFormatter)}")

                                    Text("Etat : $etatAffiche")

                                    Text("Périodicité : ${task.periodicite}")

                                    if (task.important) {
                                        Text("⚠ Important")
                                    }

                                    task.imageDesc?.let {
                                        Image(
                                            painter = rememberAsyncImagePainter(File(it)),
                                            contentDescription = "Image tâche",
                                            modifier = Modifier.fillMaxWidth().height(150.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            taskDao.delete(task)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer la tâche"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showConfetti) {

                val party = Party(
                    emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(40),
                    spread = 360,
                    position = Position.Relative(0.5, 0.0),
                    size = listOf(Size.SMALL, Size.MEDIUM),
                    shapes = listOf(Shape.Circle, Shape.Square)
                )

                KonfettiView(
                    modifier = Modifier.fillMaxSize(),
                    parties = listOf(party)
                )
            }
        }
    }
}