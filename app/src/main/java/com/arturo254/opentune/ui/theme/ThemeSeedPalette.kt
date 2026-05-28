package com.arturo254.opentune.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONObject

data class ThemeSeedPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val neutral: Color
)

object ThemeSeedPaletteCodec {
    fun decodeFromPreference(value: String): ThemeSeedPalette? {
        if (!value.startsWith("{")) return null
        return decodeFromJson(value)
    }

    fun decodeFromJson(jsonString: String): ThemeSeedPalette? {
        return try {
            val json = JSONObject(jsonString)
            ThemeSeedPalette(
                primary = Color(json.getInt("primary")),
                secondary = Color(json.getInt("secondary")),
                tertiary = Color(json.getInt("tertiary")),
                neutral = Color(json.getInt("neutral"))
            )
        } catch (e: Exception) {
            null
        }
    }

    fun extractNameFromJsonOrNull(jsonString: String): String? {
        return try {
            val json = JSONObject(jsonString)
            if (json.has("name")) json.getString("name") else null
        } catch (e: Exception) {
            null
        }
    }

    fun encodeForPreference(palette: ThemeSeedPalette, name: String?): String {
        val json = JSONObject()
        json.put("primary", palette.primary.toArgb())
        json.put("secondary", palette.secondary.toArgb())
        json.put("tertiary", palette.tertiary.toArgb())
        json.put("neutral", palette.neutral.toArgb())
        name?.let { json.put("name", it) }
        return json.toString()
    }
}
