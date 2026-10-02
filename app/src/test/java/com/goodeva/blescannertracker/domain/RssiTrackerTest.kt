package com.goodeva.blescannertracker.domain

import org.junit.Assert
import org.junit.Test

class RssiTrackerTest {

    @Test
    fun `empty tracker has no data`() {
        val s = RssiTracker().snapshot(nowMs = 0)
        Assert.assertNull(s.raw)
        Assert.assertNull(s.smoothed)
        Assert.assertEquals(0, s.packetCount)
    }

    @Test
    fun `first sample becomes the smoothed value`() {
        val t = RssiTracker()
        t.add(-60, nowMs = 0)
        Assert.assertEquals(-60f, t.snapshot(0).smoothed!!, 0.001f)
    }

    @Test
    fun `second sample is blended with alpha`() {
        val t = RssiTracker(alpha = 0.25f)
        t.add(-60, nowMs = 0)
        t.add(-80, nowMs = 1_000)
        // 0.25 * -80 + 0.75 * -60 = -65
        Assert.assertEquals(-65f, t.snapshot(1_000).smoothed!!, 0.001f)
    }

    @Test
    fun `long gap restarts smoothing from the new value`() {
        val t = RssiTracker()
        t.add(-60, nowMs = 0)
        t.add(-90, nowMs = 20_000)
        Assert.assertEquals(-90f, t.snapshot(20_000).smoothed!!, 0.001f)
    }

    @Test
    fun `old samples are dropped from the window`() {
        val t = RssiTracker(windowMs = 10_000)
        t.add(-60, nowMs = 0)
        t.add(-70, nowMs = 5_000)
        val s = t.snapshot(nowMs = 12_000)
        Assert.assertEquals(1, s.packetCount)
        Assert.assertEquals(-70, s.min)
        Assert.assertEquals(-70, s.max)
    }

    @Test
    fun `standard deviation is computed over the window`() {
        val t = RssiTracker()
        t.add(-60, nowMs = 0)
        t.add(-70, nowMs = 1_000)
        // mean -65, variance 25, std dev 5
        Assert.assertEquals(5f, t.snapshot(1_000).stdDev, 0.001f)
    }
}