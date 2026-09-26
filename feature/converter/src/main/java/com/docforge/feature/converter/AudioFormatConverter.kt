package com.docforge.feature.converter

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.docforge.core.domain.io.ActiveTempFileRegistry
import com.docforge.core.domain.settings.DocForgeOutputBucket
import com.docforge.core.domain.settings.DocForgeSettingsStore
import com.docforge.core.pdf.resolveNonConflictingFile
import com.docforge.core.pdf.withStagedOutputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.coroutines.coroutineContext
import kotlin.math.max

enum class AudioConvertOutputFormat {
    M4A_AAC,
    WAV,
    MP3,
    FLAC
}

data class AudioConvertTrackInfo(
    val mimeType: String,
    val durationMs: Long?,
    val sampleRateHz: Int?,
    val channelCount: Int?
)

data class AudioFormatConversionResult(
    val outputFile: File,
    val outputSizeBytes: Long,
    val outputFormat: AudioConvertOutputFormat,
    val sourceMimeType: String,
    val durationMs: Long?
)

private data class DecodedPcmSummary(
    val sourceMimeType: String,
    val durationMs: Long?,
    val sampleRateHz: Int,
    val channelCount: Int
)

class AudioFormatConverter(
    private val context: Context
) {

    suspend fun inspectAudioTrack(inputUri: Uri): AudioConvertTrackInfo = withContext(Dispatchers.IO) {
        withAudioTrack(inputUri) { _, _, trackFormat ->
            AudioConvertTrackInfo(
                mimeType = trackFormat.getString(MediaFormat.KEY_MIME).orEmpty(),
                durationMs = readDurationMs(trackFormat),
                sampleRateHz = readOptionalInt(trackFormat, MediaFormat.KEY_SAMPLE_RATE),
                channelCount = readOptionalInt(trackFormat, MediaFormat.KEY_CHANNEL_COUNT)
            )
        }
    }

    suspend fun convert(
        inputUri: Uri,
        outputBaseName: String,
        outputFormat: AudioConvertOutputFormat
    ): AudioFormatConversionResult = withContext(Dispatchers.IO) {
        val checkCancelled = { coroutineContext.ensureActive() }
        when (outputFormat) {
            AudioConvertOutputFormat.M4A_AAC -> convertToAacM4a(inputUri, outputBaseName, checkCancelled)
            AudioConvertOutputFormat.WAV -> convertToWav(inputUri, outputBaseName, checkCancelled)
            AudioConvertOutputFormat.MP3 -> convertToCompressed(
                inputUri = inputUri,
                outputBaseName = outputBaseName,
                outputFormat = outputFormat,
                targetMime = MediaFormat.MIMETYPE_AUDIO_MPEG,
                checkCancelled = checkCancelled
            )
            AudioConvertOutputFormat.FLAC -> convertToCompressed(
                inputUri = inputUri,
                outputBaseName = outputBaseName,
                outputFormat = outputFormat,
                targetMime = MediaFormat.MIMETYPE_AUDIO_FLAC,
                checkCancelled = checkCancelled
            )
        }
    }

    private suspend fun convertToAacM4a(
        inputUri: Uri,
        outputBaseName: String,
        checkCancelled: () -> Unit
    ): AudioFormatConversionResult {
        require(hasEncoderForMime(MediaFormat.MIMETYPE_AUDIO_AAC)) {
            "AAC encoder not available on this device."
        }

        val outputTarget = createOutputFile(outputBaseName, AudioConvertOutputFormat.M4A_AAC)
        val pcmFile = createTempPcmFile()

        return try {
            val decoded = decodeToPcmFile(inputUri, pcmFile, checkCancelled)
            val staged = withStagedOutputFile(
                directory = requireNotNull(outputTarget.parentFile) { "Output directory unavailable." },
                baseName = outputTarget.nameWithoutExtension,
                extension = outputTarget.extension
            ) { stagedFile ->
                encodePcmToAacM4a(
                    pcmFile = pcmFile,
                    outputFile = stagedFile,
                    sampleRateHz = decoded.sampleRateHz,
                    channelCount = decoded.channelCount,
                    checkCancelled = checkCancelled
                )
            }
            val outputFile = staged.outputFile
            AudioFormatConversionResult(
                outputFile = outputFile,
                outputSizeBytes = outputFile.length(),
                outputFormat = AudioConvertOutputFormat.M4A_AAC,
                sourceMimeType = decoded.sourceMimeType,
                durationMs = decoded.durationMs
            )
        } finally {
            deleteTempPcmFile(pcmFile)
        }
    }

    private suspend fun convertToWav(
        inputUri: Uri,
        outputBaseName: String,
        checkCancelled: () -> Unit
    ): AudioFormatConversionResult {
        val outputTarget = createOutputFile(outputBaseName, AudioConvertOutputFormat.WAV)
        val pcmFile = createTempPcmFile()

        return try {
            val decoded = decodeToPcmFile(inputUri, pcmFile, checkCancelled)
            val staged = withStagedOutputFile(
                directory = requireNotNull(outputTarget.parentFile) { "Output directory unavailable." },
                baseName = outputTarget.nameWithoutExtension,
                extension = outputTarget.extension
            ) { stagedFile ->
                writeWav(
                    pcmFile = pcmFile,
                    outputFile = stagedFile,
                    sampleRateHz = decoded.sampleRateHz,
                    channelCount = decoded.channelCount,
                    checkCancelled = checkCancelled
                )
            }
            val outputFile = staged.outputFile
            AudioFormatConversionResult(
                outputFile = outputFile,
                outputSizeBytes = outputFile.length(),
                outputFormat = AudioConvertOutputFormat.WAV,
                sourceMimeType = decoded.sourceMimeType,
                durationMs = decoded.durationMs
            )
        } finally {
            deleteTempPcmFile(pcmFile)
        }
    }

    private suspend fun convertToCompressed(
        inputUri: Uri,
        outputBaseName: String,
        outputFormat: AudioConvertOutputFormat,
        targetMime: String,
        checkCancelled: () -> Unit
    ): AudioFormatConversionResult {
        val outputTarget = createOutputFile(outputBaseName, outputFormat)
        val sourceTrack = withAudioTrack(inputUri) { _, _, trackFormat ->
            Pair(
                trackFormat.getString(MediaFormat.KEY_MIME).orEmpty(),
                readDurationMs(trackFormat)
            )
        }
        val sourceMimeType = sourceTrack.first
        val durationMs = sourceTrack.second

        if (normalizeMime(sourceMimeType) == normalizeMime(targetMime)) {
            val staged = withStagedOutputFile(
                directory = requireNotNull(outputTarget.parentFile) { "Output directory unavailable." },
                baseName = outputTarget.nameWithoutExtension,
                extension = outputTarget.extension
            ) { stagedFile ->
                withAudioTrack(inputUri) { extractor, trackIndex, trackFormat ->
                    extractor.selectTrack(trackIndex)
                    copyExtractorSamplesToFile(extractor, trackFormat, stagedFile, checkCancelled)
                }
            }
            val outputFile = staged.outputFile
            return AudioFormatConversionResult(
                outputFile = outputFile,
                outputSizeBytes = outputFile.length(),
                outputFormat = outputFormat,
                sourceMimeType = sourceMimeType,
                durationMs = durationMs
            )
        }

        require(hasEncoderForMime(targetMime)) {
            "${outputFormat.name} encoder not available on this device. Source can only be converted if the device exposes a ${outputFormat.name} encoder."
        }

        val pcmFile = createTempPcmFile()

        return try {
            val decoded = decodeToPcmFile(inputUri, pcmFile, checkCancelled)
            val staged = withStagedOutputFile(
                directory = requireNotNull(outputTarget.parentFile) { "Output directory unavailable." },
                baseName = outputTarget.nameWithoutExtension,
                extension = outputTarget.extension
            ) { stagedFile ->
                encodePcmToRawCodec(
                    pcmFile = pcmFile,
                    outputFile = stagedFile,
                    sampleRateHz = decoded.sampleRateHz,
                    channelCount = decoded.channelCount,
                    targetMime = targetMime,
                    checkCancelled = checkCancelled
                )
            }
            val outputFile = staged.outputFile
            AudioFormatConversionResult(
                outputFile = outputFile,
                outputSizeBytes = outputFile.length(),
                outputFormat = outputFormat,
                sourceMimeType = sourceMimeType,
                durationMs = durationMs
            )
        } finally {
            deleteTempPcmFile(pcmFile)
        }
    }

    private fun decodeToPcmFile(
        inputUri: Uri,
        pcmFile: File,
        checkCancelled: () -> Unit
    ): DecodedPcmSummary {
        return withAudioTrack(inputUri) { extractor, trackIndex, trackFormat ->
            val sourceMime = trackFormat.getString(MediaFormat.KEY_MIME).orEmpty()
            extractor.selectTrack(trackIndex)

            val decoder = MediaCodec.createDecoderByType(sourceMime)
            try {
                decoder.configure(trackFormat, null, null, 0)
                decoder.start()

                var sampleRateHz = readOptionalInt(trackFormat, MediaFormat.KEY_SAMPLE_RATE) ?: 44100
                var channelCount = readOptionalInt(trackFormat, MediaFormat.KEY_CHANNEL_COUNT) ?: 2
                var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

                val bufferInfo = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false

                FileOutputStream(pcmFile).use { outputStream ->
                    while (!outputDone) {
                        checkCancelled()
                        if (!inputDone) {
                            val inputIndex = decoder.dequeueInputBuffer(CODEC_TIMEOUT_US)
                            if (inputIndex >= 0) {
                                val inputBuffer = decoder.getInputBuffer(inputIndex)
                                    ?: error("Decoder input buffer unavailable")
                                inputBuffer.clear()

                                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                                if (sampleSize < 0) {
                                    decoder.queueInputBuffer(
                                        inputIndex,
                                        0,
                                        0,
                                        0L,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                    inputDone = true
                                } else {
                                    decoder.queueInputBuffer(
                                        inputIndex,
                                        0,
                                        sampleSize,
                                        extractor.sampleTime,
                                        extractor.sampleFlags
                                    )
                                    extractor.advance()
                                }
                            }
                        }

                        val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)
                        when (outputIndex) {
                            MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                val outputFormat = decoder.outputFormat
                                sampleRateHz = readOptionalInt(outputFormat, MediaFormat.KEY_SAMPLE_RATE)
                                    ?: sampleRateHz
                                channelCount = readOptionalInt(outputFormat, MediaFormat.KEY_CHANNEL_COUNT)
                                    ?: channelCount
                                if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                                    pcmEncoding = outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                                }
                            }

                            else -> {
                                if (outputIndex >= 0) {
                                    val outputBuffer = decoder.getOutputBuffer(outputIndex)
                                    if (bufferInfo.size > 0 && outputBuffer != null) {
                                        outputBuffer.position(bufferInfo.offset)
                                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                        val bytes = ByteArray(bufferInfo.size)
                                        outputBuffer.get(bytes)
                                        outputStream.write(bytes)
                                    }
                                    decoder.releaseOutputBuffer(outputIndex, false)
                                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                        outputDone = true
                                    }
                                }
                            }
                        }
                    }
                }

                require(pcmEncoding == AudioFormat.ENCODING_PCM_16BIT) {
                    "Unsupported decoded PCM encoding: $pcmEncoding. Only PCM 16-bit is supported."
                }

                DecodedPcmSummary(
                    sourceMimeType = sourceMime,
                    durationMs = readDurationMs(trackFormat),
                    sampleRateHz = sampleRateHz,
                    channelCount = channelCount
                )
            } finally {
                decoder.stopSafely()
                decoder.release()
            }
        }
    }

    private fun encodePcmToAacM4a(
        pcmFile: File,
        outputFile: File,
        sampleRateHz: Int,
        channelCount: Int,
        checkCancelled: () -> Unit
    ) {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRateHz,
            channelCount
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, recommendedBitrate(sampleRateHz, channelCount))
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, DEFAULT_BUFFER_SIZE)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        var muxer: MediaMuxer? = null
        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()
            var trackIndex = -1
            var muxerStarted = false

            FileInputStream(pcmFile).channel.use { inputChannel ->
                val mappedInput = inputChannel.map(FileChannel.MapMode.READ_ONLY, 0L, inputChannel.size())
                var inputDone = false
                var outputDone = false
                var totalInputBytes = 0L

                while (!outputDone) {
                    checkCancelled()
                    if (!inputDone) {
                        val inputIndex = encoder.dequeueInputBuffer(CODEC_TIMEOUT_US)
                        if (inputIndex >= 0) {
                            val inputBuffer = encoder.getInputBuffer(inputIndex)
                                ?: error("Encoder input buffer unavailable")
                            inputBuffer.clear()

                            val read = minOf(DEFAULT_BUFFER_SIZE, minOf(inputBuffer.remaining(), mappedInput.remaining()))
                            if (read <= 0) {
                                val pts = bytesToPresentationTimeUs(totalInputBytes, sampleRateHz, channelCount)
                                encoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    pts,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                val start = mappedInput.position()
                                val slice = mappedInput.duplicate().apply {
                                    position(start)
                                    limit(start + read)
                                }
                                inputBuffer.put(slice)
                                mappedInput.position(start + read)
                                val pts = bytesToPresentationTimeUs(totalInputBytes, sampleRateHz, channelCount)
                                encoder.queueInputBuffer(inputIndex, 0, read, pts, 0)
                                totalInputBytes += read
                            }
                        }
                    }

                    val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)
                    when (outputIndex) {
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            check(!muxerStarted) { "Encoder output format changed twice." }
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }

                        else -> {
                            if (outputIndex >= 0) {
                                val outputBuffer = encoder.getOutputBuffer(outputIndex)
                                    ?: error("Encoder output buffer unavailable")
                                if (bufferInfo.size > 0) {
                                    check(muxerStarted) { "Muxer not started before encoded data arrived." }
                                    outputBuffer.position(bufferInfo.offset)
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                                }
                                encoder.releaseOutputBuffer(outputIndex, false)
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    outputDone = true
                                }
                            }
                        }
                    }
                }
            }

            if (muxerStarted) {
                muxer.stop()
            }
        } finally {
            encoder.stopSafely()
            encoder.release()
            muxer?.release()
        }
    }

    private fun encodePcmToRawCodec(
        pcmFile: File,
        outputFile: File,
        sampleRateHz: Int,
        channelCount: Int,
        targetMime: String,
        checkCancelled: () -> Unit
    ) {
        val format = MediaFormat.createAudioFormat(targetMime, sampleRateHz, channelCount).apply {
            if (targetMime == MediaFormat.MIMETYPE_AUDIO_MPEG) {
                setInteger(MediaFormat.KEY_BIT_RATE, recommendedBitrate(sampleRateHz, channelCount))
            }
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, DEFAULT_BUFFER_SIZE)
        }

        val encoder = MediaCodec.createEncoderByType(targetMime)
        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            FileInputStream(pcmFile).channel.use { inputChannel ->
                val mappedInput = inputChannel.map(FileChannel.MapMode.READ_ONLY, 0L, inputChannel.size())
                FileOutputStream(outputFile).use { outputStream ->
                    var inputDone = false
                    var outputDone = false
                    var totalInputBytes = 0L

                    while (!outputDone) {
                        checkCancelled()
                        if (!inputDone) {
                            val inputIndex = encoder.dequeueInputBuffer(CODEC_TIMEOUT_US)
                            if (inputIndex >= 0) {
                                val inputBuffer = encoder.getInputBuffer(inputIndex)
                                    ?: error("Encoder input buffer unavailable")
                                inputBuffer.clear()

                                val read = minOf(DEFAULT_BUFFER_SIZE, minOf(inputBuffer.remaining(), mappedInput.remaining()))
                                if (read <= 0) {
                                    val pts = bytesToPresentationTimeUs(totalInputBytes, sampleRateHz, channelCount)
                                    encoder.queueInputBuffer(
                                        inputIndex,
                                        0,
                                        0,
                                        pts,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                    inputDone = true
                                } else {
                                    val start = mappedInput.position()
                                    val slice = mappedInput.duplicate().apply {
                                        position(start)
                                        limit(start + read)
                                    }
                                    inputBuffer.put(slice)
                                    mappedInput.position(start + read)
                                    val pts = bytesToPresentationTimeUs(totalInputBytes, sampleRateHz, channelCount)
                                    encoder.queueInputBuffer(inputIndex, 0, read, pts, 0)
                                    totalInputBytes += read
                                }
                            }
                        }

                        val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)
                        when (outputIndex) {
                            MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                            else -> {
                                if (outputIndex >= 0) {
                                    val outputBuffer = encoder.getOutputBuffer(outputIndex)
                                        ?: error("Encoder output buffer unavailable")
                                    if (bufferInfo.size > 0) {
                                        outputBuffer.position(bufferInfo.offset)
                                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                        val bytes = ByteArray(bufferInfo.size)
                                        outputBuffer.get(bytes)
                                        outputStream.write(bytes)
                                    }
                                    encoder.releaseOutputBuffer(outputIndex, false)
                                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                        outputDone = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            encoder.stopSafely()
            encoder.release()
        }
    }

    private fun writeWav(
        pcmFile: File,
        outputFile: File,
        sampleRateHz: Int,
        channelCount: Int,
        checkCancelled: () -> Unit
    ) {
        val pcmSize = pcmFile.length()
        val bitsPerSample = 16
        val byteRate = sampleRateHz * channelCount * bitsPerSample / 8
        val blockAlign = channelCount * bitsPerSample / 8

        DataOutputStream(FileOutputStream(outputFile)).use { output ->
            output.writeAscii("RIFF")
            output.writeIntLE((36L + pcmSize).toInt())
            output.writeAscii("WAVE")
            output.writeAscii("fmt ")
            output.writeIntLE(16)
            output.writeShortLE(1)
            output.writeShortLE(channelCount.toShort())
            output.writeIntLE(sampleRateHz)
            output.writeIntLE(byteRate)
            output.writeShortLE(blockAlign.toShort())
            output.writeShortLE(bitsPerSample.toShort())
            output.writeAscii("data")
            output.writeIntLE(pcmSize.toInt())
        }

        FileInputStream(pcmFile).channel.use { inputChannel ->
            FileOutputStream(outputFile, true).channel.use { outputChannel ->
                val size = inputChannel.size()
                var position = 0L
                while (position < size) {
                    checkCancelled()
                    val transferred = inputChannel.transferTo(position, size - position, outputChannel)
                    if (transferred <= 0L) break
                    position += transferred
                }
            }
        }
    }

    private fun copyExtractorSamplesToFile(
        extractor: MediaExtractor,
        trackFormat: MediaFormat,
        outputFile: File,
        checkCancelled: () -> Unit
    ) {
        val buffer = ByteBuffer.allocate(selectBufferSize(trackFormat))
        FileOutputStream(outputFile).channel.use { outputChannel ->
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
    }

    private fun createOutputFile(outputBaseName: String, outputFormat: AudioConvertOutputFormat): File {
        val outputDir = DocForgeSettingsStore.resolveOutputDirectory(
            context = context,
            bucket = DocForgeOutputBucket.AUDIO
        )

        val base = outputBaseName.ifBlank { "audio_${System.currentTimeMillis()}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")

        val extension = when (outputFormat) {
            AudioConvertOutputFormat.M4A_AAC -> "m4a"
            AudioConvertOutputFormat.WAV -> "wav"
            AudioConvertOutputFormat.MP3 -> "mp3"
            AudioConvertOutputFormat.FLAC -> "flac"
        }

        return resolveNonConflictingFile(outputDir, base, extension)
    }

    private fun createTempPcmFile(): File {
        val tempDir = context.cacheDir
        return File.createTempFile("docforge_audio_", ".pcm", tempDir).also {
            ActiveTempFileRegistry.register(it)
        }
    }

    private fun deleteTempPcmFile(file: File) {
        ActiveTempFileRegistry.unregister(file)
        file.delete()
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
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) {
                return index
            }
        }
        return -1
    }

    private fun hasEncoderForMime(targetMime: String): Boolean {
        return MediaCodecList(MediaCodecList.ALL_CODECS)
            .codecInfos
            .any { info ->
                info.isEncoder && info.supportedTypes.any { type ->
                    normalizeMime(type) == normalizeMime(targetMime)
                }
            }
    }

    private fun normalizeMime(mime: String): String {
        return mime.lowercase()
    }

    private fun selectBufferSize(trackFormat: MediaFormat): Int {
        val fallback = DEFAULT_BUFFER_SIZE
        return if (trackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            max(trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE), fallback)
        } else {
            fallback
        }
    }

    private fun recommendedBitrate(sampleRateHz: Int, channelCount: Int): Int {
        val channels = channelCount.coerceIn(1, 2)
        val kbpsPerChannel = if (sampleRateHz >= 44100) 96_000 else 64_000
        return kbpsPerChannel * channels
    }

    private fun bytesToPresentationTimeUs(totalPcmBytes: Long, sampleRateHz: Int, channelCount: Int): Long {
        val bytesPerFrame = (channelCount * 2).coerceAtLeast(2)
        val frames = totalPcmBytes / bytesPerFrame
        return (frames * 1_000_000L) / sampleRateHz.coerceAtLeast(1)
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

    private fun MediaCodec.stopSafely() {
        runCatching { stop() }
    }

    companion object {
        private const val CODEC_TIMEOUT_US = 10_000L
        private const val DEFAULT_BUFFER_SIZE = 64 * 1024
    }
}

private fun DataOutputStream.writeAscii(text: String) {
    writeBytes(text)
}

private fun DataOutputStream.writeIntLE(value: Int) {
    writeByte(value and 0xFF)
    writeByte((value shr 8) and 0xFF)
    writeByte((value shr 16) and 0xFF)
    writeByte((value shr 24) and 0xFF)
}

private fun DataOutputStream.writeShortLE(value: Short) {
    writeByte(value.toInt() and 0xFF)
    writeByte((value.toInt() shr 8) and 0xFF)
}
