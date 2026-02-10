package com.drivetheory.cbt.domain.model

data class ExamConfig(
    val name: String = "Standard",
    val questionCount: Int = 10,
    val timeLimitSeconds: Int = 600,
    val passMarkPercent: Int = 80,
)

