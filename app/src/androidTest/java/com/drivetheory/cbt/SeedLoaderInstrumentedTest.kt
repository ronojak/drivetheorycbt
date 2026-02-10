package com.drivetheory.cbt

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drivetheory.cbt.data.seed.QuestionSeedLoader
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeedLoaderInstrumentedTest {
    @Test
    fun loads_seed_questions_from_assets() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = QuestionSeedLoader(context)
        val questions = loader.loadFromAssets()
        assertTrue("Expected seed questions to be loaded", questions.isNotEmpty())
    }
}

