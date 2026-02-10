package com.drivetheory.cbt.presentation.navigation

object Routes {
    const val HOME = "home"
    const val EXAM_SETUP = "exam_setup?vehicle={vehicle}"
    const val EXAM = "exam?vehicle={vehicle}&count={count}&all={all}"
    const val RESULTS = "results/{score}/{total}"
    const val HISTORY = "history"
    const val REVIEW = "review"

    fun results(score: Int, total: Int) = "results/$score/$total"
    fun examSetup(vehicle: String) = "exam_setup?vehicle=$vehicle"
    fun exam(vehicle: String, count: Int?, all: Boolean): String {
        val c = count ?: -1
        return "exam?vehicle=$vehicle&count=$c&all=$all"
    }
}
