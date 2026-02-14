package com.example.offlinetts

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.offlinetts.ml.OnnxInference
import com.example.offlinetts.ml.Phonemizer
import com.example.offlinetts.utils.AudioStitcher
import com.example.offlinetts.utils.TextNormalizer
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RenderingWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val inputText = inputData.getString("INPUT_TEXT") ?: return Result.failure()
        
        setProgressAsync(workDataOf("PROGRESS" to 0, "STATUS" to "Initializing..."))

        try {
            // 1. Normalize and Split
            val normalized = TextNormalizer.normalize(inputText)
            val sentences = TextNormalizer.splitIntoSentences(normalized)
            
            if (sentences.isEmpty()) return Result.failure()

            // 2. Initialize Engines
            val phonemizer = Phonemizer(applicationContext)
            phonemizer.init()
            
            val inference = OnnxInference(applicationContext)
            inference.loadModel() // user must have placed the model

            val tempFiles = mutableListOf<File>()
            val cacheDir = applicationContext.cacheDir

            // 3. Process Loop
            sentences.forEachIndexed { index, sentence ->
                setProgressAsync(workDataOf(
                    "PROGRESS" to (index.toFloat() / sentences.size * 100).toInt(),
                    "STATUS" to "Processing sentence ${index + 1}/${sentences.size}"
                ))

                // A. Phonemize
                val inputIds = phonemizer.textToIds(sentence)
                
                // B. Inference
                val audioFloats = inference.runInference(inputIds)
                
                // C. Save WAV Chunk
                val chunkFile = File(cacheDir, "chunk_$index.wav")
                saveWav(chunkFile, audioFloats, 24000) // Assuming 24khz, change as per model
                tempFiles.add(chunkFile)
                
                // Garbage Collection hint
                System.gc()
            }

            // 4. Stitch
            setProgressAsync(workDataOf("PROGRESS" to 95, "STATUS" to "Stitching audio..."))
            
            val outputDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
            val finalFile = File(outputDir, "OfflineTTS_${System.currentTimeMillis()}.mp3")
            
            val success = AudioStitcher.mergeAudioFiles(tempFiles, finalFile)
            
            // Clean up temp
            tempFiles.forEach { it.delete() }
            inference.close()

            if (success) {
                return Result.success(workDataOf("OUTPUT_PATH" to finalFile.absolutePath))
            } else {
                return Result.failure()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }

    private fun saveWav(file: File, data: FloatArray, sampleRate: Int) {
        // Simple WAV header writer + PCM data
        // For float array, we usually convert to 16-bit PCM for WAV
        val fos = FileOutputStream(file)
        
        // WAV Header (Simplistic)
        // ... (Header writing logic omitted for brevity, assuming standard WAV write)
        // Writing raw PCM for now to save space in this snippet, but FFmpeg needs proper headers or raw arguments
        
        // Convert float to 16-bit short PCM
        val buffer = ByteBuffer.allocate(data.size * 2)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        for (f in data) {
            var s = (f * 32767).toInt()
            if (s > 32767) s = 32767
            if (s < -32768) s = -32768
            buffer.putShort(s.toShort())
        }
        
        // Write generic WAV header based on buffer size
        writeWavHeader(fos, buffer.capacity(), sampleRate)
        fos.write(buffer.array())
        fos.close()
    }
    
    private fun writeWavHeader(out: FileOutputStream, totalAudioLen: Int, longSampleRate: Int) {
        // Minimal WAV Header
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = longSampleRate * channels * 2
        
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = ((longSampleRate shr 8) and 0xff).toByte()
        header[26] = ((longSampleRate shr 16) and 0xff).toByte()
        header[27] = ((longSampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2 * 1 // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
        out.write(header, 0, 44)
    }
}
