# Phomemo D30: protocol and BluePaper backend

Notes for the Phomemo D30 backend (branch `feat/phomemo-d30`). The D30 is a cheap BLE-only
thermal label maker. Unlike Niimbot it has **no RFID/chipped rolls and no consumable
authentication** — it prints on third-party tape.

## Device

| Property | Value |
|---|---|
| Media | 6 / 12 / 14 / 15 mm tape; **12 mm effective print width** |
| Resolution | 203 dpi, 96 dots across the head (12 bytes/row) |
| BLE name | `D30` (no OS pairing; GATT only) |
| GATT service | `0000ff00-0000-1000-8000-00805f9b34fb` |
| Write characteristic | `0000ff02-0000-1000-8000-00805f9b34fb` (**write-with-response**) |
| Notify characteristic | `0000ff03-0000-1000-8000-00805f9b34fb` (optional; not needed to print) |

BlueZ may mis-select the classic (BR/EDR) bearer for this LE-only device; see the Vibe-D30
`docs/ble.md` for the workaround. Writing must use GATT *write-with-response*.

## Print payload (Epson ESC/POS `GS v 0` raster)

```
1B 40                ESC @  - initialize
1D 76 30 00          GS v 0, m=0 - raster bit image
wLo wHi hLo hHi      width in bytes, height in rows
<rows>               1-bpp, MSB first, 1 = dark, 12 bytes/row
1B 64 00             ESC d 0 - print and feed
```

Sent in **128-byte writes** (`write-with-response`). One header/data/footer per copy.

Confirmed against real hardware by:
- https://github.com/stev0760/Vibe-D30
- https://github.com/odensc/phomemo-d30-web-bluetooth
- `phomemo-tools` (CUPS driver `rastertopd30`)

## How BluePaper drives it

- `DeviceRegistry` entry `d30`: 203 dpi, 96 px, `rotation = -90`, `PrinterProtocol.ESC_POS_RASTER`,
  and a `GattProfile` (`ff00`/`ff02`/`ff03`, `writeWithResponse = true`). Label sizes are
  `(length × 12 mm)`; the renderer rotates so rows are 12 bytes wide.
- `PhomemoD30Protocol` builds the header/footer and chunks the payload.
- `PhomemoD30Client` writes header → data chunks → footer per copy over `BleTransport.writeBytes`.
- `BleTransport.connect(device, gatt)` selects the explicit endpoint; implemented for the desktop
  Bleak bridge, the BlueZ D-Bus transport, and Android Kable.

## BlueZ bearer gotcha

The D30 advertises classic (BR/EDR) hints (Class of Device, SPP/HID UUIDs) but is LE-only, so
BlueZ may pick the classic bearer and the connect fails with `br-connection-not-supported`.
Bond **and trust** the device so BlueZ auto-connects the LE link; the bridge then attaches to
that link and retries on the bearer error. If a fresh setup still fails, the storage-level fix is
in the Vibe-D30 `docs/ble.md` (`PreferredBearer=le`, drop `LastUsedBearer` and `[LinkKey]`).

The D30 also sleeps after a few idle minutes — press its power button before scanning.

## Open questions

- Phomemo D30 tape may carry a small NFC/RFID chip on some rolls. This backend ignores it (prints
  regardless). Whether any D30 firmware enforces such a tag is unverified; the 12 mm tapes used so
  far are plain thermal. Investigate with the PN532 before relying on generic tape.
- Only 12 mm tape is configured; 6/14/15 mm need `LabelSize` entries and a matching head width.
- Print-start offset drifts with where the tape was last torn (see Vibe-D30's calibration).
