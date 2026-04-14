#!/usr/bin/env python3
"""
BLE Bridge for BluePaper - uses Bleak for reliable BLE communication.
Called as subprocess from Kotlin.

Usage:
  python3 ble_bridge.py scan <prefix>
  python3 ble_bridge.py connect <address>
  python3 ble_bridge.py write <hex_data>
  python3 ble_bridge.py read
  python3 ble_bridge.py disconnect
"""

import asyncio
import sys
import json
from typing import Optional

try:
    from bleak import BleakClient, BleakScanner
    from bleak.exc import BleakError
except ImportError:
    print(json.dumps({"error": "bleak not installed. Run: pip install bleak"}))
    sys.exit(1)

# Niimbot characteristic UUID
CHAR_UUID = "bef8d6c9-9c21-4c9e-b632-bd58c1009f9f"

client: Optional[BleakClient] = None
response_data: Optional[bytes] = None
response_event = asyncio.Event()


def notification_handler(sender, data: bytearray):
    global response_data
    response_data = bytes(data)
    response_event.set()


async def scan(prefix: str):
    devices = await BleakScanner.discover(timeout=5.0, return_adv=True)
    results = []
    for device, adv_data in devices.values():
        if device.name and device.name.lower().startswith(prefix.lower()):
            results.append({
                "name": device.name,
                "address": device.address,
                "rssi": adv_data.rssi if adv_data else -100,
                "uuids": len(adv_data.service_uuids) if adv_data else 0,
            })
    # Prefer device with fewer UUIDs for D110
    if prefix.lower().startswith("d110"):
        results.sort(key=lambda x: x["uuids"])
    print(json.dumps({"devices": results}))


async def connect(address: str):
    global client
    try:
        client = BleakClient(address)
        await client.connect(timeout=10.0)
        await client.start_notify(CHAR_UUID, notification_handler)
        print(json.dumps({"connected": True, "address": address}))
    except BleakError as e:
        print(json.dumps({"error": str(e)}))
        client = None


async def write(hex_data: str):
    global response_data
    if not client or not client.is_connected:
        print(json.dumps({"error": "not connected"}))
        return

    try:
        data = bytes.fromhex(hex_data.replace(" ", ""))
        response_event.clear()
        response_data = None

        await client.write_gatt_char(CHAR_UUID, data, response=False)

        # Wait for response
        try:
            await asyncio.wait_for(response_event.wait(), timeout=5.0)
            if response_data:
                print(json.dumps({"response": response_data.hex()}))
            else:
                print(json.dumps({"error": "no response"}))
        except asyncio.TimeoutError:
            print(json.dumps({"error": "timeout"}))
    except BleakError as e:
        print(json.dumps({"error": str(e)}))


async def disconnect():
    global client
    if client:
        try:
            await client.disconnect()
        except:
            pass
        client = None
    print(json.dumps({"disconnected": True}))


async def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "usage: ble_bridge.py <command> [args]"}))
        return

    cmd = sys.argv[1]

    if cmd == "scan":
        prefix = sys.argv[2] if len(sys.argv) > 2 else "D11"
        await scan(prefix)
    elif cmd == "connect":
        if len(sys.argv) < 3:
            print(json.dumps({"error": "address required"}))
            return
        await connect(sys.argv[2])
    elif cmd == "write":
        if len(sys.argv) < 3:
            print(json.dumps({"error": "hex data required"}))
            return
        await write(sys.argv[2])
    elif cmd == "disconnect":
        await disconnect()
    else:
        print(json.dumps({"error": f"unknown command: {cmd}"}))


if __name__ == "__main__":
    asyncio.run(main())
