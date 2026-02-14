package com.example.offlinetts.ml

import android.content.Context
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
import java.nio.FloatBuffer

class OnnxInference(private val context: Context) {

    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null

    fun loadModel(modelPath: String = "kokoro-quant.onnx") {
        try {
            env = OrtEnvironment.getEnvironment()
            // In a real scenario, you need to copy the asset to a file path accessible by native C++
            // Or read bytes. ORT Android supports reading from assets directly in newer versions or via file.
            // For now assuming the user puts the file in app compatible storage or we load from assets.
            
            // To load from assets, we usually copy to cache dir first
            val modelFile = java.io.File(context.filesDir, modelPath)
           
            // Check if file exists, if not, try copying from assets (if user bundled it)
            if (!modelFile.exists()) {
                 try {
                     context.assets.open(modelPath).use { input ->
                         java.io.FileOutputStream(modelFile).use { output ->
                             input.copyTo(output)
                         }
                     }
                 } catch (e: Exception) {
                     // If not in assets, assumes basic path
                 }
            }
            
            if (modelFile.exists()) {
                session = env?.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
            } else {
                 throw RuntimeException("Model file not found at ${modelFile.absolutePath}. Please add $modelPath to assets or storage.")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Failed to load ONNX model", e)
        }
    }

    fun runInference(inputIds: LongArray): FloatArray {
        val s = session ?: throw IllegalStateException("Model not loaded")
        val e = env ?: throw IllegalStateException("Environment not created")

        // Prepare Inputs
        // Kokoro inputs: 'input_ids', 'style' (optional usually), etc.
        // Need to know exact input names from model. Assuming 'input_ids' and 'style_vector'
        
        // This is highly dependent on the specific ONNX export of Kokoro
        val inputName = s.inputNames.iterator().next() // Guessing first input is IDs
        
        val shape = longArrayOf(1, inputIds.size.toLong())
        val tensor = OnnxTensor.createTensor(e, LongBuffer.wrap(inputIds), shape)
        
        val result = s.run(mapOf(inputName to tensor))
        
        // Output
        // Assuming output is audio float array
        val outputTensor = result.get(0) as OnnxTensor
        val floatBuffer = outputTensor.floatBuffer
        val floatArray = FloatArray(floatBuffer.remaining())
        floatBuffer.get(floatArray)
        
        result.close()
        return floatArray
    }
    
    fun close() {
        session?.close()
        env?.close()
    }
}

// Helper for LongBuffer if not imported
import java.nio.LongBuffer
