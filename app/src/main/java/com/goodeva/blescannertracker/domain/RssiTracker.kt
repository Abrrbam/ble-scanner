package com.goodeva.blescannertracker.domain

import kotlin.math.sqrt

data class  RssiSnapshot(
    val raw: Int?,
    val smoothed: Float?,
    val min: Int?,
    val max: Int?,
    val packetCount: Int,
    val stdDev: Float,
    val lastPacketAtMs: Long?,
)

class RssiTracker(
    private val alpha: Float = 0.25f,
    private val windowMs: Long = 10_000L,
) {
    private class Sample(val rssi: Int, val timeMs: Long)

    private val samples = ArrayDeque<Sample>()
    private var smoothed: Float? = null
    private var raw: Int? = null
    private var lastAt: Long? = null

    fun add(rssi: Int, nowMs: Long) {
        val previous = smoothed
        val last = lastAt

        //Setelah jeda panjang, restart dari nilai baru (bukan dicampur dgn data lama)
        smoothed = if (previous == null || (last != null && nowMs - last > windowMs)) {
            rssi.toFloat()
        } else {
            alpha * rssi + (1 - alpha) * previous
        }
        raw = rssi
        lastAt = nowMs
        samples.addLast(Sample(rssi, nowMs))
    }

    fun snapshot(nowMs: Long): RssiSnapshot {
        while (samples.isNotEmpty() && nowMs - samples.first().timeMs > windowMs) {
            samples.removeFirst()
        }
        val values = samples.map { it.rssi }
        val mean = if (values.isEmpty()) 0f else values.average().toFloat()
        val variance = if (values.isEmpty()) 0f
        else values.map { (it - mean) * (it - mean) }.average().toFloat()

        return RssiSnapshot(
            raw = raw,
            smoothed = smoothed,
            min = values.minOrNull(),
            max = values.maxOrNull(),
            packetCount = values.size,
            stdDev = sqrt(variance),
            lastPacketAtMs = lastAt
        )
    }

}