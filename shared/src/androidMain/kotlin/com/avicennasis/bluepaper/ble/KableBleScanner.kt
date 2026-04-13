package com.avicennasis.bluepaper.ble

import com.juul.kable.Filter
import com.juul.kable.Scanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class KableBleScanner(
    private val advertisementCache: AdvertisementCache,
) : BleScanner {

    override fun scan(namePrefix: String): Flow<ScannedDevice> {
        val scanner = Scanner {
            filters {
                match {
                    name = Filter.Name.Prefix(namePrefix)
                }
            }
        }

        return scanner.advertisements.map { advertisement ->
            advertisementCache.cache(advertisement.identifier.toString(), advertisement)
            ScannedDevice(
                name = advertisement.name ?: "",
                address = advertisement.identifier.toString(),
                rssi = advertisement.rssi,
                serviceUuids = advertisement.uuids.map { it.toString() },
            )
        }
    }
}
