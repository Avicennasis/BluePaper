#!/usr/bin/env python3
"""
BLE Bridge for BluePaper - uses Bleak for reliable BLE communication.
Runs as persistent process, reads commands from stdin, writes responses to stdout.

Commands (one per line, JSON):
  {"cmd": "connect", "address": "XX:XX:XX:XX:XX:XX"}
  {"cmd": "write", "data": "hex_string"}
  {"cmd": "disconnect"}
  {"cmd": "quit"}
"""

import asyncio
import sys
import json
from typing import Optional

try:
    from bleak import BleakClient, BleakScanner
    from bleak.exc import BleakError
except ImportError:
    print(json.dumps({"error": "bleak not installed. Run: pip install bleak"}), flush=True)
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


def respond(obj: dict):
    print(json.dumps(obj), flush=True)


async def do_connect(address: str):
    global client
    try:
        if client:
            try:
                await client.disconnect()
            except:
                pass
        client = BleakClient(address)
        await client.connect(timeout=10.0)
        await client.start_notify(CHAR_UUID, notification_handler)
        respond({"connected": True, "address": address})
    except BleakError as e:
        respond({"error": str(e)})
        client = None
    except Exception as e:
        respond({"error": str(e)})
        client = None


async def do_write(hex_data: str, wait_response: bool = True):
    global response_data
    if not client or not client.is_connected:
        respond({"error": "not connected"})
        return

    try:
        data = bytes.fromhex(hex_data.replace(" ", ""))

        if wait_response:
            response_event.clear()
            response_data = None

        await client.write_gatt_char(CHAR_UUID, data, response=False)

        if not wait_response:
            respond({"ok": True})
            return

        # Wait for response
        try:
            await asyncio.wait_for(response_event.wait(), timeout=5.0)
            if response_data:
                respond({"response": response_data.hex()})
            else:
                respond({"error": "no response"})
        except asyncio.TimeoutError:
            respond({"error": "timeout"})
    except BleakError as e:
        respond({"error": str(e)})
    except Exception as e:
        respond({"error": str(e)})


async def do_disconnect():
    global client
    if client:
        try:
            await client.disconnect()
        except:
            pass
        client = None
    respond({"disconnected": True})


async def process_command(line: str):
    try:
        cmd = json.loads(line)
    except json.JSONDecodeError:
        respond({"error": "invalid JSON"})
        return True

    action = cmd.get("cmd", "")

    if action == "connect":
        address = cmd.get("address")
        if not address:
            respond({"error": "address required"})
        else:
            await do_connect(address)
    elif action == "write":
        data = cmd.get("data")
        if not data:
            respond({"error": "data required"})
        else:
            wait = cmd.get("wait", True)
            await do_write(data, wait_response=wait)
    elif action == "disconnect":
        await do_disconnect()
    elif action == "quit":
        await do_disconnect()
        return False
    elif action == "ping":
        respond({"pong": True})
    else:
        respond({"error": f"unknown command: {action}"})

    return True


async def main():
    respond({"ready": True})

    loop = asyncio.get_event_loop()
    reader = asyncio.StreamReader()
    protocol = asyncio.StreamReaderProtocol(reader)
    await loop.connect_read_pipe(lambda: protocol, sys.stdin)

    while True:
        try:
            line = await reader.readline()
            if not line:
                break
            line = line.decode().strip()
            if not line:
                continue
            if not await process_command(line):
                break
        except Exception as e:
            respond({"error": str(e)})
            break

    await do_disconnect()


if __name__ == "__main__":
    asyncio.run(main())
