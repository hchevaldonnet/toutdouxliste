package com.example.toutdouxliste

import android.R.attr.checked
import java.time.Instant
import java.time.ZoneId
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.toutdouxliste.data.TaskDao
import com.example.toutdouxliste.ui.theme.ToutDouxListeTheme
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.example.toutdouxliste.data.TaskDatabase
import com.example.toutdouxliste.data.Task
import com.example.toutdouxliste.data.Etat
import kotlinx.coroutines.launch
import java.time.LocalDateTime

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
            HomeScreen(navController = navController, taskDao)
        }
        composable("new") {
            NewScreen(navController = navController, taskDao)
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewScreen(navController: NavHostController, taskDao: TaskDao) {

    var titre by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val datePickerState = rememberDatePickerState(
        initialDisplayMode = DisplayMode.Picker
    )

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle tâche") }
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

            Text(
                text = "Date limite",
                style = MaterialTheme.typography.titleMedium
            )

            DatePicker(
                state = datePickerState
            )

            Button(
                onClick = {

                    val selectedDate = datePickerState.selectedDateMillis

                    if (selectedDate != null) {

                        val dateLimite = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(selectedDate),
                            ZoneId.systemDefault()
                        )

                        val task = Task(
                            titre = titre,
                            description = description,
                            etat = Etat.A_FAIRE,
                            dateLimite = dateLimite
                        )

                        scope.launch {
                            taskDao.insert(task)
                        }

                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Créer la tâche")
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController, taskDao: TaskDao) {

    val tasks by taskDao.getAllTasks().collectAsState(initial = emptyList())

    Scaffold(

        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("new") }
            ) {
                Text("+")
            }
        }

    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            items(tasks) { task ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = task.titre,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(task.description)

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Etat : ${task.etat}")

                        Text("Date limite : ${task.dateLimite}")
                    }
                }
            }
        }
    }
}