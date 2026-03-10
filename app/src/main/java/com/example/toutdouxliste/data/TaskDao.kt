package com.example.toutdouxliste.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: Task)

    @Query("SELECT * FROM Task")
    fun getAllTasks(): Flow<List<Task>>

    @Update
    suspend fun update(task: Task)

    @Query("SELECT * FROM Task WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task

    @Delete
    suspend fun delete(task: Task)
}