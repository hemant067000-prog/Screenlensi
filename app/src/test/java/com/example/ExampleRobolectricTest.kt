package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.GlossaryEntryEntity
import com.example.domain.OfflineTranslationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name and verify offline translation engine`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ScreenLens", appName)

        // Verify Japanese translation
        val jaResult = OfflineTranslationEngine.translate(
            "諦めないで！夜明け前に必ず君を見つけ出すから！",
            emptyList()
        )
        assertEquals("JA", jaResult.detectedLangCode)
        assertTrue(jaResult.translatedText.contains("Don't give up"))

        // Verify 0.1ms Custom Glossary Priority Override
        val glossary = listOf(
            GlossaryEntryEntity(
                sourcePhrase = "領域展開",
                englishTranslation = "Domain Expansion [Custom Override]",
                languageCode = "JA"
            )
        )
        val overrideResult = OfflineTranslationEngine.translate("領域展開", glossary)
        assertTrue(overrideResult.usedGlossaryOverride)
        assertEquals("Domain Expansion [Custom Override]", overrideResult.translatedText)
    }
}
