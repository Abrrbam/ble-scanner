package com.goodeva.blescannertracker.ui.scanner

import com.goodeva.blescannertracker.domain.DeviceFilter
import com.goodeva.blescannertracker.domain.filterAndSort
import com.goodeva.blescannertracker.domain.model.BleDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceFilterTest {

    private fun device(address: String, name: String?, rssi: Int) =
        BleDevice(address = address, name = name, rssi = rssi, lastSeen = 0L)

    private val watch = device("AA:BB:CC:DD:EE:01", "Watch", -70)
    private val buds = device("AA:BB:CC:DD:EE:02", "Buds", -50)
    private val unknown = device("11:22:33:44:55:66", null, -85)
    private val all = listOf(watch, buds, unknown)

    @Test
    fun `sorted by strongest signal first`() {
        assertEquals(listOf(buds, watch, unknown), all.filterAndSort(DeviceFilter()))
    }

    @Test
    fun `same rssi is ordered by address`() {
        val b = device("B", null, -60)
        val a = device("A", null, -60)
        assertEquals(listOf(a, b), listOf(b, a).filterAndSort(DeviceFilter()))
    }

    @Test
    fun `minimum rssi removes weaker devices`() {
        val result = all.filterAndSort(DeviceFilter(minRssi = -80))
        assertEquals(listOf(buds, watch), result)
    }

    @Test
    fun `search by name ignores case`() {
        assertEquals(listOf(watch), all.filterAndSort(DeviceFilter(query = "wat")))
    }

    @Test
    fun `search by mac works with and without colons`() {
        assertEquals(listOf(buds), all.filterAndSort(DeviceFilter(query = "ee:02")))
        assertEquals(listOf(buds), all.filterAndSort(DeviceFilter(query = "EE02")))
        assertEquals(listOf(unknown), all.filterAndSort(DeviceFilter(query = "1122")))
    }

    @Test
    fun `query is trimmed`() {
        assertEquals(listOf(buds), all.filterAndSort(DeviceFilter(query = " buds ")))
    }

    @Test
    fun `lone colon does not match everything`() {
        assertEquals(emptyList<BleDevice>(), all.filterAndSort(DeviceFilter(query = ":")))
    }

    @Test
    fun `query and minimum rssi combine`() {
        val result = all.filterAndSort(DeviceFilter(query = "AABB", minRssi = -60))
        assertEquals(listOf(buds), result)
    }
}