package com.example.offlinetts.ml

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset

object VoiceManager {

    private val voiceCache = mutableMapOf<String, FloatArray>()
    
    fun loadVoice(context: Context, voiceName: String = "af_bella"): FloatArray? {
        if (voiceCache.containsKey(voiceName)) return voiceCache[voiceName]
        
        try {
            // Try loading from assets
            val jsonString = context.assets.open("voices.json").use { 
                it.readBytes().toString(Charset.defaultCharset())
            }
            
            val json = JSONObject(jsonString)
            if (json.has(voiceName)) {
                val styleArray = json.getJSONArray(voiceName)
                // Flatten if it's 2D or just take first row? Kokoro usually has [1, 256] or similar
                // Assuming it's a flat array or array of arrays in JSON
                // If it's [[...]], take first.
                
                val firstItem = styleArray.get(0)
                val floats: FloatArray
                
                if (firstItem is Number) {
                    // It's a 1D array
                    floats = FloatArray(styleArray.length())
                    for (i in 0 until styleArray.length()) {
                         floats[i] = styleArray.getDouble(i).toFloat()
                    }
                } else if (firstItem is org.json.JSONArray) {
                    // It's 2D
                    val inner = firstItem as org.json.JSONArray
                    floats = FloatArray(inner.length())
                    for (i in 0 until inner.length()) {
                        floats[i] = inner.getDouble(i).toFloat()
                    }
                } else {
                    return null
                }
                
                voiceCache[voiceName] = floats
                return floats
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
