package com.docforge.feature.converter

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

enum class AudioOutputFormat {
    M4A,
    MP3
}

data class AudioTrackInfo(
    val mimeType: String,
    val durationMs: Long?,
    val sampleRateHz: Int?,
    val channelCount: Int?
)

data class VideoAudioExtractionResult(
    val outputFile: File,
    val outputSizeBytes: Long,
    val outputFormat: AudioOutputFormat,
    val sourceMimeType: String,
    val durationMs: Long?
)

class VideoAudioExtractor(
    private val context: Context
) {

    suspend fun inspectAudioTrack(inputUri: Uri): AudioTrackInfo = withContext(Dispatchers.IO) {
        withAudioTrack(inputUri) { _, _, trackFormat ->
            AudioTrackInfo(
                mimeType = trackFormat.getString(MediaFormat.KEY_MIME).orEmpty(),
                durationMs = readDurationMs(trackFormat),
                sampleRateHz = readOptionalInt(trackFormat, MediaFormat.KEY_SAMPLE_RATE),
                channelCount = readOptionalInt(trackFormat, MediaFormat.KEY_CHANNEL_COUNT)
            )
        }
    }

    suspend fun extractAudio(
        inputUri: Uri,
        outputBaseName: String,
        outputFormat: AudioOutputFormat
    ): VideoAudioExtractionResult = withContext(Dispatchers.IO) {
        val checkCancelled = { coroutineContext.ensureActive() }
        val outputFile = createOutputFile(outputBaseName, outputFormat)
        when (outputFormat) {
            AudioOutputFormat.M4A -> extractToM4a(inputUri, outputFile, checkCancelled)
            AudioOutputFormat.MP3 -> extractToMp3Passthrough(inputUri, outputFile, checkCancelled)
        }
    }

    private fun extractToM4a(
        inputUri: Uri,
        outputFile: File,
        checkCancelled: () -> Unit
    ): VideoAudioExtractionResult {
        return withAudioTrack(inputUri) { extractor, trackIndex, trackFormat ->
            extractor.selectTrack(trackIndex)

            var muxer: MediaMuxer? = null
            try {
                muxer = MediaMuxer(
                    outputFile.absolutePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )
                val outputTrackIndex = muxer.addTrack(trackFormat)
                muxer.start()

                val buffer = ByteBuffer.allocate(selectBufferSize(trackFormat))
                val info = MediaCodec.BufferInfo()

                while (true) {
                    checkCancelled()
                    buffer.clear()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    info.offset = 0
                    info.size = sampleSize
                    info.presentationTimeUs = extractor.sampleTime
                    info.flags = extractor.sampleFlags
                    muxer.writeSampleData(outputTrackIndex, buffer, info)
                    extractor.advance()
                }

                muxer.stop()
            } finally {
                muxer?.release()
            }

            VideoAudioExtractionResult(
                outputFile = outputFile,
                outputSizeBytes = outputFile.length(),
                outputFormat = AudioOutputFormat.M4A,
                sourceMimeType = trackFormat.getString(MediaFormat.KEY_MIME).orEmpty(),
                durationMs = readDurationMs(trackFormat)
            )
        }
    }

    private fun extractToMp3Passthrough(
        inputUri: Uri,
        outputFile: File,
        checkCancelled: () -> Unit
    ): VideoAudioExtractionResult {
        return withAudioTrack(inputUri) { extractor, trackIndex, trackFormat ->
            val mimeType = trackFormat.getString(MediaFormat.KEY_MIME).orEmpty()
            require(mimeType == MediaFormat.MIMETYPE_AUDIO_MPEG || mimeType == "audio/mpeg") {
                "Source audio track is '$mimeType'. MP3 output only supports MP3 passthrough without re-encode. Choose M4A for this file."
            }

            extractor.selectTrack(trackIndex)

            val buffer = ByteBuffer.allocate(selectBufferSize(trackFormat))
            outputFile.outputStream().channel.use { outputChannel ->
                while (true) {
                    checkCancelled()
                    buffer.clear()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    buffer.position(0)
                    buffer.limit(sampleSize)
                    while (buffer.hasRemaining()) {
                        outputChannel.write(buffer)
                    }
                    extractor.advance()
                }
            }

            VideoAudioExtractionResult(
                outputFile = outputFile,
                outputSizeBytes = outputFile.length(),
                outputFormat = AudioOutputFormat.MP3,
                sourceMimeType = mimeType,
                durationMs = readDurationMs(trackFormat)
            )
        }
    }

    private fun createOutputFile(outputBaseName: String, outputFormat: AudioOutputFormat): File {
        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.AUDIO
        )

        val base = outputBaseName.ifBlank { "audio_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val extension = when (outputFormat) {
            AudioOutputFormat.M4A -> "m4a"
            AudioOutputFormat.MP3 -> "mp3"
        }

        return File(outputDir, "$base.$extension").also { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun selectBufferSize(trackFormat: MediaFormat): Int {
        val fallback = 256 * 1024
        return if (trackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(fallback)
        } else {
            fallback
        }
    }

    private inline fun <T> withAudioTrack(
        inputUri: Uri,
        block: (extractor: MediaExtractor, trackIndex: Int, trackFormat: MediaFormat) -> T
    ): T {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, inputUri, null)
            val trackIndex = findAudioTrack(extractor)
            require(trackIndex >= 0) { "No audio track found in selected file." }
            val trackFormat = extractor.getTrackFormat(trackIndex)
            return block(extractor, trackIndex, trackFormat)
        } finally {
            extractor.release()
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            val mimeType = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mimeType.startsWith("audio/")) {
                return index
            }
        }
        return -1
    }

    private fun readDurationMs(trackFormat: MediaFormat): Long? {
        return if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
            trackFormat.getLong(MediaFormat.KEY_DURATION) / 1000L
        } else {
            null
        }
    }

    private fun readOptionalInt(trackFormat: MediaFormat, key: String): Int? {
        return if (trackFormat.containsKey(key)) {
            trackFormat.getInteger(key)
        } else {
            null
        }
    }
}
