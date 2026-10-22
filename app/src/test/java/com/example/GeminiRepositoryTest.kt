package com.example

import com.example.data.repository.GeminiRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiRepositoryTest {

    @Test
    fun repository_initializesCorrectly() {
        val repository = GeminiRepository()
        assertEquals("gemini-3.5-flash", repository.modelName)
    }

    @Test
    fun generativeModel_createsSuccessfully() {
        val repository = GeminiRepository()
        val model = repository.createGenerativeModel(apiKey = "TEST_API_KEY")
        assertNotNull(model)
        assertEquals("models/gemini-3.5-flash", model.modelName)
    }
}
