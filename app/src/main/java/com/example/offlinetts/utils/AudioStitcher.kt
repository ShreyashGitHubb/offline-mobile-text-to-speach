import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File

object AudioStitcher {

    fun mergeAudioFiles(inputs: List<File>, outputFile: File): Boolean {
        if (inputs.isEmpty()) return false

        // Create a text file listing all inputs for ffmpeg concat demuxer
        // file '/path/to/file1.wav'
        // file '/path/to/file2.wav'
        
        val listFile = File(outputFile.parent, "input_list.txt")
        val writer = listFile.bufferedWriter()
        
        try {
            inputs.forEach { file ->
                writer.write("file '${file.absolutePath}'\n")
            }
            writer.flush()
        } finally {
            writer.close()
        }

        // FFmpeg command
        // -f concat -safe 0 -i list.txt -c copy output.wav (if generic wav)
        // Or re-encode to mp3: -c:a libmp3lame -q:a 2
        
        val cmd = "-f concat -safe 0 -i \"${listFile.absolutePath}\" -c:a libmp3lame -q:a 2 \"${outputFile.absolutePath}\""
        
        val session = FFmpegKit.execute(cmd)
        val rc = session.returnCode
        
        // Clean up list file
        listFile.delete()
        
        return rc.isValueSuccess // true if success
    }
}
