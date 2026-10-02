package com.goodeva.blescannertracker.domain

import com.goodeva.blescannertracker.domain.model.BleDevice

const val RSSI_FILTER_OFF = -100

data class DeviceFilter(
    val query: String = "",
    val minRssi: Int = RSSI_FILTER_OFF
)

//Filter berdasarkan [nama, MAC, RSSI Minimum]
//Order by DESC/ sinyal terkuat
fun List<BleDevice>.filterAndSort(filter: DeviceFilter): List<BleDevice> {
    val q = filter.query.trim()
    val qMac =q.replace(":", "")
    return this
        .filter { it.rssi >= filter.minRssi }
        .filter { device ->
            q.isEmpty() ||
                    device.name?.contains(q, ignoreCase = true) == true ||
                    (qMac.isNotEmpty() &&
                        device.address.replace(":", "").contains(qMac, ignoreCase = true))
        }
        .sortedWith(compareByDescending<BleDevice> { it.rssi }.thenBy { it.address })
}