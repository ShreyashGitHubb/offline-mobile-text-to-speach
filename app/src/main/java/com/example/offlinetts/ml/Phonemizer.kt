package com.example.offlinetts.ml

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.nio.charset.Charset

class Phonemizer(private val context: Context) {

    private val vocab = mutableMapOf<String, Long>()
    private var espeakAvailable = false

    fun init() {
        Log.d("Phonemizer", "Initializing...")
        loadVocab()
        
        // Check for Espeak
        try {
            // Attempt to load the library class if available
            // val clazz = Class.forName("com.github.crushingtides.libespeakng.Espeak")
            // espeakAvailable = true
            // helping user out:
        } catch (e: Exception) {
            Log.w("Phonemizer", "Espeak library not found. Using simple char mapping fallback.")
        }
    }

    private fun loadVocab() {
        try {
            val jsonString = context.assets.open("config.json").use { 
                it.readBytes().toString(Charset.defaultCharset())
            }
            val json = JSONObject(jsonString)
            val vocabObj = json.getJSONObject("vocab")
            
            vocabObj.keys().forEach { key ->
                vocab[key] = vocabObj.getLong(key)
            }
            Log.d("Phonemizer", "Loaded vocab with ${vocab.size} entries")
        } catch (e: Exception) {
            Log.e("Phonemizer", "Failed to load config.json", e)
        }
    }

    fun textToPhonemes(text: String): String {
        // TODO: Call actual Espeak library
        // if (espeakAvailable) { return Espeak.textToPhonemes(text) }
        
        // Fallback: Just return lowercased text which maps to 'a'-'z' in vocab
        // Kokoro is robust enough to handle raw text somewhat or if phonemes match chars
        return text.lowercase()
    }
    
    fun textToIds(text: String): LongArray {
        val phonemes = textToPhonemes(text)
        val ids = mutableListOf<Long>()
        
        // Kokoro expects a start token? Usually 0 is pad.
        // Let's just map characters.
        
        // 0 pad
        ids.add(0L) 
        
        var i = 0
        while (i < phonemes.length) {
            // Try to match longest key first? 
            // Vocab has keys like 't' (62) and 'th' maybe? No, usually single chars in IPA but some might be combined.
            // In the config we saw only single chars mostly.
            
            val charStr = phonemes[i].toString()
            if (vocab.containsKey(charStr)) {
                ids.add(vocab[charStr]!!)
            } else {
                // Unknown char, skip or map to space?
                Log.w("Phonemizer", "Unknown char: $charStr")
            }
            i++
        }
        
        // End token?
        ids.add(0L)
        
        return ids.toLongArray()
    }
}
