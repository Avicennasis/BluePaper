package com.avicennasis.bluepaper.ble.serial

import com.avicennasis.bluepaper.ble.DeviceTransport
import com.avicennasis.bluepaper.ble.ScannedDevice
import com.avicennasis.bluepaper.config.DeviceRegistry
import com.avicennasis.bluepaper.protocol.CommandBuilder
import com.avicennasis.bluepaper.protocol.InfoEnum
import com.avicennasis.bluepaper.protocol.PacketStreamReader

/**
 * Finds printers on USB serial ports. Only ports whose USB id is a known printer chip are
 * probed — writing a probe to an arbitrary serial device (a modem, a microcontroller) isn't
 * safe. A probe asks for the model id and names the device after the matching model.
 */
class UsbPrinterFinder(
    private val listPorts: () -> List<SerialPortInfo> = JSerialCommLink::listPorts,
    private val openLink: (String) -> SerialLink = JSerialCommLink::open,
    private val probeTimeoutMs: Int = 1_500,
) {
    companion object {
        /** 3513:0001 "YICHIP FS USB" — a D110_M (fw 4.30) as a CDC-ACM serial port. */
        val KNOWN_USB_IDS: Set<Pair<Int, Int>> = setOf(0x3513 to 0x0001)
    }

    fun find(): List<ScannedDevice> =
        listPorts().filter { (it.vendorId to it.productId) in KNOWN_USB_IDS }.mapNotNull { probe(it) }

    private fun probe(port: SerialPortInfo): ScannedDevice? {
        val link = try {
            openLink(port.path)
        } catch (e: Exception) {
            println("[UsbPrinterFinder] Skipping ${port.path}: ${e.message}")
            return null
        }
        try {
            link.write(CommandBuilder.getInfo(InfoEnum.DEVICE_TYPE).toBytes())
            val reader = PacketStreamReader()
            val buffer = ByteArray(256)
            val modelIdReply = 0x40 + InfoEnum.DEVICE_TYPE.key
            val deadline = System.currentTimeMillis() + probeTimeoutMs
            while (System.currentTimeMillis() < deadline) {
                val count = link.read(buffer, 100)
                if (count < 0) break
                val reply = reader.feed(buffer.copyOf(count)).firstOrNull { it.type == modelIdReply } ?: continue
                val model = DeviceRegistry.modelIdFromReply(reply.data)?.let(DeviceRegistry::modelForModelId)
                if (model == null) {
                    println("[UsbPrinterFinder] ${port.path}: unknown model id")
                    return null
                }
                return ScannedDevice(
                    name = "${model.uppercase()} (USB)",
                    address = port.path,
                    rssi = 0,
                    serviceUuids = emptyList(),
                    transport = DeviceTransport.USB,
                )
            }
            println("[UsbPrinterFinder] ${port.path}: no reply to model query")
            return null
        } catch (e: Exception) {
            println("[UsbPrinterFinder] ${port.path}: probe failed: ${e.message}")
            return null
        } finally {
            runCatching { link.close() }
        }
    }
}
