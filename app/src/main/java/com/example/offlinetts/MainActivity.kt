package com.example.offlinetts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.offlinetts.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission Granted
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRender.setOnClickListener {
            if (checkPermissions()) {
                val text = binding.etInput.text.toString()
                if (text.isNotBlank()) {
                    startRendering(text)
                } else {
                    binding.etInput.error = "Please enter text"
                }
            } else {
                requestPermissions()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (Scoped Storage) - We can write to public directories like Music without permission
            // if we accept using MediaStore, but FFmpeg often needs file paths.
            // For simplicity in this demo, we assume < Q or scoped storage approach which might not need strict permissions
            // if using app-specific dirs, but we are using public dir in RenderingWorker.
            // Let's simplified check:
            return true 
        } else {
            return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            // For Android 11+ we might need logic for MANAGE_EXTERNAL_STORAGE if strictly required,
            // but we try to avoid it.
            Toast.makeText(this, "Ensure app has storage access", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startRendering(text: String) {
        binding.tvStatus.text = "Starting Render..."
        binding.btnRender.isEnabled = false
        binding.progressBar.progress = 0

        val inputData = workDataOf("INPUT_TEXT" to text)
        
        val renderRequest = OneTimeWorkRequestBuilder<RenderingWorker>()
            .setInputData(inputData)
            .build()
            
        WorkManager.getInstance(this).enqueue(renderRequest)
        
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(renderRequest.id)
            .observe(this) { workInfo ->
                if (workInfo != null) {
                    val progress = workInfo.progress
                    val value = progress.getInt("PROGRESS", 0)
                    val status = progress.getString("STATUS") ?: ""
                    
                    binding.progressBar.progress = value
                    binding.tvStatus.text = status
                    
                    if (workInfo.state.isFinished) {
                        binding.btnRender.isEnabled = true
                        if (workInfo.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                            val path = workInfo.outputData.getString("OUTPUT_PATH")
                            binding.tvStatus.text = "Success! Saved to: $path"
                            Toast.makeText(this, "Audio Saved!", Toast.LENGTH_LONG).show()
                        } else {
                            binding.tvStatus.text = "Failed"
                        }
                    }
                }
            }
    }
}
