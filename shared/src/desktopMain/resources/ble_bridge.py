#!/usr/bin/env python3
"""
BLE Bridge for BluePaper - uses Bleak for reliable BLE communication.
Runs as persistent process, reads commands from stdin, writes responses to stdout.

Commands (one per line, JSON):
  {"cmd": "connect", "address": "XX:XX:XX:XX:XX:XX"}
  {"cmd": "write", "data": "hex_string", "expect": [reply types], "wait": true}
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

# Selected write target. A printer profile (e.g. Phomemo D30) overrides these at connect
# time; Niimbot uses its own characteristic with write-without-response.
write_uuid = CHAR_UUID
write_response = False
notify_uuid = CHAR_UUID

# Packet types (the byte after the 0x55 0x55 header) the printer sends on its own,
# not in reply to a command. 0xD3 "PrinterCheckLine" reports rows printed so far
# during a page. Taking one as a reply shifts every later reply by one: a D110_M
# sends two per page, so getPrintStatus received endPagePrint's 0xE4 ack and failed.
UNSOLICITED_TYPES = {0xD3}
# The printer's error report; it can arrive in place of any reply.
PRINT_ERROR_TYPE = 0xDB

client: Optional[BleakClient] = None
notifications: "asyncio.Queue[bytes]" = asyncio.Queue()


def notification_handler(sender, data: bytearray):
    notifications.put_nowait(bytes(data))


def is_reply(packet: bytes, expect=None) -> bool:
    """With `expect` (reply types sent by the caller), only those types (or an error
    report) count; without it, anything except unsolicited notices does."""
    if len(packet) < 3:
        return False
    packet_type = packet[2]
    if packet_type == PRINT_ERROR_TYPE:
        return True
    if expect:
        return packet_type in expect
    return packet_type not in UNSOLICITED_TYPES


def drop_pending_notifications():
    while not notifications.empty():
        notifications.get_nowait()


async def next_reply(timeout: float, expect=None) -> bytes:
    """Next notification that is a reply to a command; raises asyncio.TimeoutError."""
    loop = asyncio.get_running_loop()
    deadline = loop.time() + timeout
    while True:
        remaining = deadline - loop.time()
        if remaining <= 0:
            raise asyncio.TimeoutError
        packet = await asyncio.wait_for(notifications.get(), remaining)
        if is_reply(packet, expect):
            return packet


def respond(obj: dict):
    print(json.dumps(obj), flush=True)


async def find_known_device(address: str):
    """Look the device up in BlueZ's object tree (paired/known devices).

    BleakClient(address) first scans for an advertisement, but a printer that is
    already connected to the PC stops advertising, so that scan reports "not found".
    Handing bleak the existing BlueZ object skips the scan, and bleak then also skips
    Connect() if the device is already connected.

    Returns (BLEDevice or None, lookup error or None). Never prints: stdout (with
    stderr merged into it by the Kotlin side) is the JSON protocol channel.
    """
    try:
        from bleak.backends.device import BLEDevice
        from dbus_fast import BusType, Message, MessageType
        from dbus_fast.aio import MessageBus

        bus = await MessageBus(bus_type=BusType.SYSTEM).connect()
        try:
            reply = await bus.call(Message(
                destination="org.bluez",
                path="/",
                interface="org.freedesktop.DBus.ObjectManager",
                member="GetManagedObjects",
            ))
        finally:
            bus.disconnect()
        if reply.message_type == MessageType.ERROR:
            return None, f"GetManagedObjects failed: {reply.error_name}"

        for path, interfaces in reply.body[0].items():
            props = interfaces.get("org.bluez.Device1")
            if props and str(props["Address"].value).upper() == address.upper():
                plain = {key: variant.value for key, variant in props.items()}
                return BLEDevice(address, plain.get("Alias"), {"path": path, "props": plain}), None
        return None, None
    except Exception as e:
        return None, f"BlueZ lookup failed: {e}"


async def do_connect(address: str, service=None, char=None, notify=None, response=False):
    global client, write_uuid, write_response, notify_uuid
    if char:
        write_uuid = char
        write_response = bool(response)
        notify_uuid = notify
    else:
        write_uuid = CHAR_UUID
        write_response = False
        notify_uuid = CHAR_UUID
    lookup_error = None
    try:
        if client:
            try:
                await client.disconnect()
            except:
                pass
        known, lookup_error = await find_known_device(address)
        # Some cheap BLE printers (Phomemo D30, some Niimbot) advertise classic BR/EDR bits
        # but are LE-only. BlueZ can pick the non-existent classic bearer, failing with
        # br-connection-not-supported. If BlueZ auto-connects the LE link (device bonded and
        # trusted), attaching to it works — wait and retry.
        last_error = None
        connected = False
        for _ in range(4):
            client = BleakClient(known or address)
            try:
                await client.connect(timeout=10.0)
                connected = True
                break
            except BleakError as e:
                last_error = e
                msg = str(e).lower()
                if "br-connection-not-supported" in msg or "not supported" in msg or "not available" in msg:
                    client = None
                    await asyncio.sleep(3.0)
                    continue
                raise
        if not connected:
            raise last_error or BleakError("connect failed")
        if notify_uuid:
            try:
                await client.start_notify(notify_uuid, notification_handler)
            except Exception:
                pass
        respond({"connected": True, "address": address, "via": "bluez-object" if known else "scan",
                 "write": write_uuid, "response": write_response})
    except Exception as e:
        message = str(e) or type(e).__name__
        if lookup_error:
            message += f" ({lookup_error})"
        respond({"error": message})
        client = None


async def do_write(hex_data: str, wait_response: bool = True, expect=None, response=None):
    if not client or not client.is_connected:
        respond({"error": "not connected"})
        return

    try:
        data = bytes.fromhex(hex_data.replace(" ", ""))
        resp = write_response if response is None else bool(response)

        if wait_response:
            # Anything queued now (late replies, check-line notices) predates this command.
            drop_pending_notifications()

        await client.write_gatt_char(write_uuid, data, response=resp)

        if not wait_response:
            respond({"ok": True})
            return

        try:
            respond({"response": (await next_reply(timeout=5.0, expect=expect)).hex()})
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
            await do_connect(
                address,
                service=cmd.get("service"),
                char=cmd.get("char"),
                notify=cmd.get("notify"),
                response=bool(cmd.get("response", False)),
            )
    elif action == "write":
        data = cmd.get("data")
        if not data:
            respond({"error": "data required"})
        else:
            wait = cmd.get("wait", True)
            expect = set(cmd["expect"]) if cmd.get("expect") else None
            await do_write(data, wait_response=wait, expect=expect, response=cmd.get("response"))
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
