package com.drivetheory.cbt.domain.model

data class Question(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val category: String? = null,
    val difficulty: Int? = null,
)

