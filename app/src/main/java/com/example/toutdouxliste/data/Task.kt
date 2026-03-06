package com.example.toutdouxliste.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val titre: String,

    val description: String,

    val etat: Etat,

    val dateLimite: LocalDateTime
)