package com.example.toutdouxliste.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: Task)

    @Query("SELECT * FROM Task")
    fun getAllTasks(): Flow<List<Task>>
}