package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.VialType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

class SoundSynthesizer(private val scope: CoroutineScope) {

  private val sampleRate = 22050
  private var isEnabled = true

  fun toggleAudio(): Boolean {
    isEnabled = !isEnabled
    return isEnabled
  }

  fun isAudioEnabled(): Boolean = isEnabled

  private fun playPcm(samples: ShortArray) {
    if (!isEnabled || samples.isEmpty()) return
    scope.launch(Dispatchers.Default) {
      try {
        val audioTrack = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(samples.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        // Wait till finished then release
        val durationMs = (samples.size * 1000L) / sampleRate
        kotlinx.coroutines.delay(durationMs + 50)
        audioTrack.stop()
        audioTrack.release()
      } catch (_: Exception) {
        // Gracefully ignore audio hardware limits
      }
    }
  }

  fun playHammerSwing() {
    val duration = 0.12f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    for (i in 0 until totalSamples) {
      val t = i.toFloat() / sampleRate
      val progress = i.toFloat() / totalSamples
      val freq = 320f - progress * 180f
      val env = (1f - progress) * (1f - progress)
      val noise = (Random.nextFloat() * 2f - 1f) * 0.4f
      val tone = sin(2.0 * Math.PI * freq * t).toFloat() * 0.6f
      buffer[i] = ((tone + noise) * env * Short.MAX_VALUE * 0.45f).toInt().toShort()
    }
    playPcm(buffer)
  }

  fun playHammerHit() {
    val duration = 0.22f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    for (i in 0 until totalSamples) {
      val t = i.toFloat() / sampleRate
      val progress = i.toFloat() / totalSamples
      val env = Math.exp((-progress * 14.0)).toFloat()
      val metal1 = sin(2.0 * Math.PI * 540.0 * t).toFloat()
      val metal2 = sin(2.0 * Math.PI * 1120.0 * t).toFloat() * 0.6f
      val sub = sin(2.0 * Math.PI * 95.0 * t).toFloat() * 0.8f
      val noise = (Random.nextFloat() * 2f - 1f) * 0.35f * Math.exp((-progress * 25.0)).toFloat()
      val sample = (metal1 + metal2 + sub + noise) * env
      buffer[i] = (sample * Short.MAX_VALUE * 0.65f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    playPcm(buffer)
  }

  fun playSteamDash() {
    val duration = 0.25f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    for (i in 0 until totalSamples) {
      val progress = i.toFloat() / totalSamples
      // High pressure hiss with attack-decay envelope
      val env = if (progress < 0.1f) progress / 0.1f else (1f - progress) * 1.1f
      val noise = (Random.nextFloat() * 2f - 1f)
      buffer[i] = (noise * env * Short.MAX_VALUE * 0.35f).toInt().toShort()
    }
    playPcm(buffer)
  }

  fun playVialShatter(type: VialType) {
    val duration = 0.30f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    val baseFreq = when (type) {
      VialType.CORROSIVE_VITRIOL -> 440.0
      VialType.PYRETIC_PHLOGISTON -> 180.0
      VialType.CRYO_QUENCH -> 880.0
    }
    for (i in 0 until totalSamples) {
      val t = i.toFloat() / sampleRate
      val progress = i.toFloat() / totalSamples
      val env = Math.exp((-progress * 10.0)).toFloat()
      val glass = sin(2.0 * Math.PI * (baseFreq + 800.0 * (1f - progress)) * t).toFloat() * 0.5f
      val sizzle = (Random.nextFloat() * 2f - 1f) * 0.6f
      buffer[i] = ((glass + sizzle) * env * Short.MAX_VALUE * 0.45f).toInt().toShort()
    }
    playPcm(buffer)
  }

  fun playValveTurn() {
    val duration = 0.45f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    for (i in 0 until totalSamples) {
      val t = i.toFloat() / sampleRate
      val progress = i.toFloat() / totalSamples
      // Multiple click ratchet pulses
      val clickPhase = (t * 18.0) % 1.0
      val click = if (clickPhase < 0.15) 1.0f else 0.0f
      val metalTone = sin(2.0 * Math.PI * 420.0 * t).toFloat() * 0.4f
      val hiss = (Random.nextFloat() * 2f - 1f) * 0.25f * progress
      buffer[i] = ((click * 0.7f + metalTone + hiss) * (1f - progress * 0.3f) * Short.MAX_VALUE * 0.5f).toInt().toShort()
    }
    playPcm(buffer)
  }

  fun playEnemyHurt() {
    val duration = 0.10f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    for (i in 0 until totalSamples) {
      val t = i.toFloat() / sampleRate
      val progress = i.toFloat() / totalSamples
      val tone = sin(2.0 * Math.PI * (260.0 - progress * 120.0) * t).toFloat()
      val crunch = (Random.nextFloat() * 2f - 1f) * 0.5f
      buffer[i] = ((tone + crunch) * (1f - progress) * Short.MAX_VALUE * 0.4f).toInt().toShort()
    }
    playPcm(buffer)
  }

  fun playPlayerHurt() {
    val duration = 0.18f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    for (i in 0 until totalSamples) {
      val t = i.toFloat() / sampleRate
      val progress = i.toFloat() / totalSamples
      val sub = sin(2.0 * Math.PI * (110.0 - progress * 50.0) * t).toFloat()
      val noise = (Random.nextFloat() * 2f - 1f) * 0.4f
      buffer[i] = ((sub + noise) * (1f - progress) * Short.MAX_VALUE * 0.5f).toInt().toShort()
    }
    playPcm(buffer)
  }

  fun playBossPhaseShift() {
    val duration = 0.8f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    for (i in 0 until totalSamples) {
      val t = i.toFloat() / sampleRate
      val progress = i.toFloat() / totalSamples
      val pitch = 90.0 + sin(progress * Math.PI * 4.0) * 40.0
      val horn = sin(2.0 * Math.PI * pitch * t).toFloat() * 0.8f
      val steam = (Random.nextFloat() * 2f - 1f) * 0.3f
      val env = (sin(progress * Math.PI)).toFloat()
      buffer[i] = ((horn + steam) * env * Short.MAX_VALUE * 0.6f).toInt().toShort()
    }
    playPcm(buffer)
  }

  fun playLevelUp() {
    val duration = 0.6f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    val notes = doubleArrayOf(330.0, 440.0, 554.37, 659.25)
    for (i in 0 until totalSamples) {
      val progress = i.toFloat() / totalSamples
      val t = i.toFloat() / sampleRate
      val noteIndex = (progress * notes.size).toInt().coerceIn(0, notes.size - 1)
      val noteFreq = notes[noteIndex]
      val subProgress = (progress * notes.size) - noteIndex
      val noteEnv = 1f - subProgress * 0.5f
      val tone = sin(2.0 * Math.PI * noteFreq * t).toFloat()
      buffer[i] = (tone * noteEnv * Short.MAX_VALUE * 0.4f).toInt().toShort()
    }
    playPcm(buffer)
  }
}
