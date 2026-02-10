package com.drivetheory.cbt.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_attempts")
data class ExamAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val score: Int,
    val totalQuestions: Int,
    val timestamp: Long,
)

