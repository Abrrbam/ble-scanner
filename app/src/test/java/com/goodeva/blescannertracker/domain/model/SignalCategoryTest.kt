package com.goodeva.blescannertracker.domain.model

import com.goodeva.blescannertracker.domain.model.SignalCategory.*
import org.junit.Assert.assertEquals
import org.junit.Test

class SignalCategoryTest {

    @Test
    fun `very strong from -30 and above`() {
        assertEquals(VERY_STRONG, SignalCategory.fromRssi(-5))
        assertEquals(VERY_STRONG, SignalCategory.fromRssi(-30))
    }

    @Test
    fun `boundaries follow the requirement table`() {
        assertEquals(STRONG, SignalCategory.fromRssi(-31))
        assertEquals(STRONG, SignalCategory.fromRssi(-50))
        assertEquals(FAIR, SignalCategory.fromRssi(-51))
        assertEquals(FAIR, SignalCategory.fromRssi(-70))
        assertEquals(WEAK, SignalCategory.fromRssi(-71))
        assertEquals(WEAK, SignalCategory.fromRssi(-80))
        assertEquals(VERY_WEAK, SignalCategory.fromRssi(-81))
        assertEquals(VERY_WEAK, SignalCategory.fromRssi(-90))
    }

    @Test
    fun `below -90 is lost`() {
        assertEquals(LOST, SignalCategory.fromRssi(-91))
        assertEquals(LOST, SignalCategory.fromRssi(-120))
    }
}