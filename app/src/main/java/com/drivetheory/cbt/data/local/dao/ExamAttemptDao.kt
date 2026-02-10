package com.drivetheory.cbt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.drivetheory.cbt.data.local.entities.ExamAttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamAttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExamAttemptEntity)

    @Query("SELECT * FROM exam_attempts ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ExamAttemptEntity>>
}

