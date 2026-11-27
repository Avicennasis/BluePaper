# Niimbot D110_M: hardware notes (RFID, protocol, connections)

Findings from bringing up a real D110_M on Linux (2026-11-21, PR #96). Everything here was
observed on one printer unless marked otherwise. Raw bytes are as captured. Read this before
changing print, connection or RFID behaviour.

## Summary

- The D110_M speaks **protocol v4** and only prints with the **v4 print sequence** (NiimBlue's
  `D110M_V4` task). With the legacy D110 sequence it acks every command, reports 100%, and burns nothing.
- Image rows must fit the **96-dot printhead**. Wider rows (119 dots for a 15 mm label at 203 dpi)
  are accepted and the label feeds, but nothing burns.
- The printer **enforces the label roll's RFID tag**:
  - If the tag's used count is past its total, the v4 `PrintStart` is rejected with
    `0xDB 0x14` (WriteRfidFail).
  - With **no readable tag**, the printer accepts a v4 job, reports it complete, feeds a label,
    and burns nothing. The official app refuses in that state ("F1 No label paper detected").
  - BluePaper now checks first (heartbeat type 4) and refuses with a clear message.
- The printer can be reached over **Bluetooth LE** or **USB-C** (CDC-ACM serial). Both carry the
  same protocol. USB showed no extra capabilities: same commands, same replies, same RFID state.

## The printer

| Property | Value | Source |
|---|---|---|
| Bluetooth name | `D110_M-HC23072563` (keeps the underscore) | BlueZ |
| Bluetooth address | `C9:36:C4:07:25:63` | BlueZ |
| GATT service / characteristic | `e7810a71-73ae-499d-8c15-faa9aef0c3f2` / `bef8d6c9-9c21-4c9e-b632-bd58c1009f9f` | BlueZ, bridge |
| Model id (GetInfo key 8) | `0x0910` = **2320** | reply `55554802091053aaaa` |
| Firmware (GetInfo key 9) | bytes `04 1e` = **4.30** | reply `55554902041e51aaaa` |
| Hardware (GetInfo key 12) | bytes `04 01` = **4.01** | reply `55554c0204014baaaa` |
| Connect (`0xC1`) result | `3` = ConnectedV3 | reply `5555c20103c0aaaa` |
| PrinterStatusData (`0xA5`) | `3030232800c80000000a00030112ee00`; bytes 11–12 `03 01` = 301 → **protocol v4** | reply `0xB5` |
| Resolution / printhead | **203 dpi**, **96 dots** (12 mm) | niimbluelib model db (id 2320) and its D110M v4.23 test |
| USB | `3513:0001` "YICHIP FS USB", CDC-ACM → `/dev/ttyACM0` | `lsusb`, `dmesg` |

niimbluelib's own reference dump from a D110M v4.23 has the same `PrinterStatusData` bytes.

## Connections

### Bluetooth LE (Linux, BlueZ + Python Bleak bridge)

- **A printer connected to the PC stops advertising.** Discovery never shows it, and
  `BleakClient(address)` fails with "not found", because it scans for an advertisement first.
  The fix is to list paired devices from BlueZ's object tree, and to hand bleak the existing
  BlueZ object (`BLEDevice(address, name, {"path", "props"})`). bleak then skips `Connect()` when
  the device is already connected.
- During a page the printer sends **unsolicited `0xD3` "check line" notices** (rows printed so
  far, e.g. `00c7` = 199, `0161` = 353). If one is taken as a reply, every later reply shifts
  by one. Replies must be matched by type (see the table below).
- The bridge's `disconnect` calls BlueZ `Disconnect()`, which drops the printer's link to the PC.

### USB-C (CDC-ACM serial)

- Plugging in the printer creates `/dev/ttyACM0` (group `dialout`). It answers the same packets
  at 115200 baud. Replies arrive as arbitrary chunks, so they are reassembled from the stream
  (`PacketStreamReader`).
- BluePaper only probes ports whose USB id is a known printer chip (`3513:0001`). Writing to
  arbitrary serial devices isn't safe. NiimBlue waits 10 ms between packets, and so does BluePaper.

## Print protocol

### Request → reply types (all confirmed on this printer)

| Request | Reply | | Request | Reply |
|---|---|---|---|---|
| `0x21` SetDensity | `0x31` | | `0xE3` PageEnd | `0xE4` |
| `0x23` SetLabelType | `0x33` | | `0xA3` PrintStatus | `0xB3` |
| `0x01` PrintStart | `0x02` | | `0xF3` PrintEnd | `0xF4` |
| `0x03` PageStart | `0x04` | | `0x1A` GetRfid | `0x1B` |
| `0x13` SetPageSize | `0x14` | | `0xC1` Connect | `0xC2` |
| `0x15` SetQuantity | `0x16` | | `0xA5` PrinterStatusData | `0xB5` |
| `0x40` GetInfo(k) | `0x40 + k` | | `0xDC` Heartbeat(4) | `0xD9` |

`0xDB` is an error report and can arrive in place of any reply. `0xD3` is never a reply.

### v4 sequence (what prints)

`SetLabelType(type)` → `SetDensity(3)` → `PrintStart` 9 bytes `[pages u16, 0,0,0,0, color, speed, flag]`
→ `PrintStatus` sent without waiting for the reply (the official app does this) →
`SetPageSize` 13 bytes `[rows u16, cols u16, copies u16, 0 ×7]` → rows → `PageEnd` → poll
`PrintStatus` until `page == copies` → `PrintEnd`.

- Use type `1` for gap labels and type `5` for transparent labels.
  [NiimBlue label types](https://printers.niim.blue/other/label-types/)
- Rows: `0x85` `[y u16, counts ×3, repeat, bits…]`. For a 96-dot head the counts are the black
  pixels per third (4 bytes each). If a row is wider than three thirds, the total is sent as
  `[0, low, high]`. Blank rows can be sent as `0x84` `[y u16, repeat]`.
- `cols` is padded to a multiple of 8.
- Status `0xB3` (8 bytes) during a good job: `[page u16][print %][feed %][4 bytes]`. Print % climbs
  0 → 100, then feed % climbs 0 → 100, then page = 1. Bytes 4–5 rose by 238 during a 239-row page.
  This may be a running count of lines printed, but that's unconfirmed.

### Legacy D110 sequence (what doesn't print on this printer)

1-byte `PrintStart`, 4-byte `SetPageSize` plus `SetQuantity`, and `0x85` rows with zero pixel
counts. The printer acks everything and reports 100%/page 1, yet burns nothing. niimbluelib
notes the 4-byte page size gives a blank first print on B1-class printers.

## RFID

### What the tag reports (GetRfid `0x1A` → `0x1B`)

Raw reply from the original roll:
`55551b27 881d7915b2181080 08 3031323232323831 10 505a314841323335323930303835303500ba00bb01 ea aaaa`

| Field | Value |
|---|---|
| uuid (8 bytes) | `881d7915b2181080` |
| barcode | `01222281` |
| serial | `PZ1HA23529008505` |
| total | `0x00ba` = 186 |
| used | `0x00bb` = 187 (it read 185 before the first test job) |
| type | 1 (labels with gaps) |

There is **no flag byte before the uuid** on this printer. BluePaper's `RFIDResponse.fromData`
assumes one and returns `null` (not fixed yet). With no tag present the reply is `55551b01001aaaaa`
(data `00`).

The roll physically had 50+ labels left when the tag read 186 total. Treat total/used as the
tag's bookkeeping, not as a reliable count of labels on the roll.

### Behaviour observed

| State | Legacy job | v4 job | Official app |
|---|---|---|---|
| Tag readable, used < total | Acked, reported complete, counted on the tag (185 → 187 over two jobs), nothing burned (rows were 177 dots wide) | Physically printed on the [tagged transparent roll](#tagged-transparent-roll-follow-up) over USB | Not tested |
| Tag readable, used > total (187/186) | Not tested | `PrintStart` → `0xDB 0x14` WriteRfidFail; nothing feeds | Not tested |
| No readable tag | Not tested in a confirmed no-tag state | Job completes (100%, page 1), label feeds, **nothing burns** (timeline step 7; the state check straight after showed no readable tag) | Refuses: "F1 No label paper detected. Please use NIIMBOT label paper…" |

Heartbeat type 4 in the no-tag state: `5555d90b0f09024a00000000000000…` → `[?, ?, battery 2,
temp 74, lid 0 = closed, paper 0 = inserted, rfid 0 = not read, …]` (niimbluelib
`parseHeartbeatAdvanced2Response`). The basic heartbeat (`0xDD`, 10 bytes) reports its lid byte as
"open" on this closed printer, so BluePaper doesn't trust it.

### Timeline of print attempts (2026-11-21)

1. RFID read: total 186, used 185.
2. Legacy job, 300 dpi config, 177-dot rows. Status polling crashed on a `0xD3` mismatch. No print.
3. Legacy job, same config, bridge fixed. Reported "1/1 pages". No print. The tag now read used
   187, so steps 2–3 each counted one label (inferred from 185 → 187; not read in between).
4. v4 job, 203 dpi, 119-dot rows. `PrintStart` rejected: `0xDB 0x14` (WriteRfidFail).
5. GUI jobs (legacy, then v4) with 119-dot rows, while the user had another roll's tag placed for
   the reader. Labels fed blank. Whether that tag was being read wasn't captured.
6. Rows capped at 96 dots: **black printed** on a half-loaded label (the first burn). Tag state not captured.
7. Same code, label properly loaded: blank, twice. The second run was fully logged: 239 rows,
   2,736 black dots, counts correct, status 100%/100%/page 1, `PrintEnd` acked.
8. State check (the exhausted tag had been peeled off the roll): lid closed, paper inserted,
   **RFID not read**. GetRfid returned no tag. The official app gave "F1 No label paper detected".
9. Pre-print check added. The same state now stops the job before `PrintStart`, over both Bluetooth and USB.

### Tagged transparent roll follow-up

The spare roll was marked `TT14*30-210 TRANSPARENT` (14 mm across the roll, 30 mm along the feed).
On firmware 4.30, USB heartbeat confirmed the closed lid, paper present, and a readable tag.
`GetRfid` reported barcode `092323005`, type `5`, and a tag total of 252. The
`GetPaperInfo` reply was four bytes with the roll seated. The packaging supplied the dimensions
for the test.

Two single-label USB jobs used protocol v4, `SetLabelType(5)`, density 3, and a 96 × 239-dot
raster. Both produced visible black print. The second rotated the raster by −90°, the orientation
in BluePaper's D110_M profile, and the operator confirmed that “OK” read along the 30 mm edge.
The counter rose from 1 to 3 across the two jobs. This was a direct USB test of the printer.

### Conclusions and open questions

- Steps 7–8 plus the official app's behaviour indicate the firmware won't heat the head without a
  readable tag. It doesn't report an error over v4 either; it just completes the job.
- Whether step 6 printed because a tag was read at that moment is unknown.
- Whether jobs without a readable tag change anything on the printer isn't known.

### Deliberately not pursued

Rewriting or resetting a tag's count, and any way to print past the printer's tag check. These
are Niimbot's consumable authentication. Research into them was stopped by the assistant's
safety filters during this session and wasn't continued.

## Error codes seen or mapped

From niimbluelib `PrinterErrorCode`. `PrinterErrors` in BluePaper turns these into messages.

| Code | Meaning |
|---|---|
| `0x01` | Cover open |
| `0x02` | No paper |
| `0x14` | **WriteRfidFail**: seen on `PrintStart` with used 187 / total 186 |
| `0x1d` | RfidTagNotWritten |
| `0x22` | NotSupportWrittenRfid |

## Known issues not fixed

- `RFIDResponse.fromData` expects a flag byte the D110_M doesn't send, so it returns `null`.
- `PrinterClient.printV4` hard-codes label type `1`. `CommandBuilder.setLabelType` accepts only
  `1` through `3`, so BluePaper's normal v4 path cannot select transparent mode.
- `PrinterClient.getSoftwareVersion` reads bytes `04 1e` as 1054/100 = "10.54"; the firmware is 4.30.
- `CommandBuilder.startPrintV2` (B-series) byte layout differs from niimbluelib's `printStart7b`.
- niimbluelib drives D11_H / D101 / B1 with newer print tasks too; only D110_M was switched.

## Read-only firmware inspection

### CRC32 signature

REA with Binwalk 3.1.0 reported one CRC32 signature in each stock update package. Binwalk's
parser reports the 16 bytes used for signature matching, which are the first four 32-bit
words. It assigned confidence `0` in both scans.
[Binwalk 3.1.0 signature code](https://github.com/ReFirmLabs/binwalk/blob/v3.1.0/src/signatures/hashes.rs)

A byte-for-byte check found **one complete 1,024-byte IEEE CRC-32 lookup table in each
package**. Both matched all 256 little-endian words generated from the reflected polynomial
`0xEDB88320`. The reference calculation was checked against Python's `zlib.crc32` on
`123456789` before comparing the table bytes. The table content is identical across the two
packages. Its runtime use remains unverified.
[RFC 1952, Appendix 8](https://www.rfc-editor.org/rfc/rfc1952#section-8)

| Package | Size (bytes) | SHA-256 |
|---|---:|---|
| `D110_M_4.30.bin` | 116,240 | `9350d5a278d88af80c83b8312b338c10a43a71bfb188b014681348f92ba20dc0` |
| `D110_M_4.31.bin` | 116,544 | `dad95f6f1214781b607f1b569f4cf7848cf1adcb88eab58d2c1a6a27e7aa279e` |

The archived 4.30 package identifies model 2320 and matches the version reported over USB.
The 4.31 package was used for the offline comparison.
[NiimBlue stock D110_M firmware archive](https://fw.niim.blue/stable/D110_M/)

### Binwalk, REA, and Ghidra setup (Linux x86-64)

The bench used REA 6.5.0, Binwalk 3.1.0, Ghidra 12.1.4, and the Temurin 21.0.13.1+2 JDK.
The tools were installed under the user's `~/.local` directory, with launchers for
`binwalk` and `ghidraRun` linked from `~/.local/bin`.

1. With Rustup, a C toolchain, `pkg-config`, and Fontconfig and FreeType development libraries
   available, build the locked Binwalk release. REA's Binwalk integration uses
   `prlimit` from `util-linux`.
   [Binwalk v3.1.0](https://github.com/ReFirmLabs/binwalk/releases/tag/v3.1.0) ·
   [REA firmware analysis](https://github.com/morluto/rea/blob/main/docs/guides/firmware/firmware-analysis.md)

   ```sh
   rustup toolchain install stable --profile minimal --no-self-update
   cargo +stable install --locked --version 3.1.0 --root "$HOME/.local/opt/binwalk-3.1.0" binwalk
   ```

2. Verify the upstream download checksums and extract the Ghidra and Temurin releases to
   `~/.local/opt/ghidra_12.1.4_PUBLIC` and `~/.local/opt/temurin-jdk-21.0.13.1+2`.
   Confirm that `bin/java` and `bin/javac` in the JDK directory both report version 21.
   On this workstation the default `java` was version 21 and the default `javac` was
   version 17, which caused REA's Ghidra check to fail. Setting `JAVA_HOME` for REA to
   the dedicated JDK resolved it. Ghidra's standalone launcher uses the same JDK through
   `JAVA_HOME_OVERRIDE` in `support/launch.properties`.
   [Ghidra 12.1.4](https://github.com/NationalSecurityAgency/ghidra/releases/tag/Ghidra_12.1.4_build) ·
   [Temurin JDK 21 release](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.13.1%2B2)

3. Use the existing REA 6.5.0 installation or install it from a supported Node.js runtime.
   The bench used Node.js 22.23.4. Install REA locally with
   `npm install --global --prefix "$HOME/.local" rea-agents@6.5.0`. The Codex registration on
   the bench calls `~/.local/bin/rea mcp` from `~/.codex2/config.toml`. Add the environment
   table to that registration, substituting your actual absolute paths. A new MCP process
   picks up these settings at launch.
   [REA installation guide](https://github.com/morluto/rea/blob/main/docs/getting-started/installation.md)

   ```toml
   [mcp_servers.rea.env]
   REA_BINWALK_COMMAND = "/home/your-user/.local/opt/binwalk-3.1.0/bin/binwalk"
   GHIDRA_INSTALL_DIR = "/home/your-user/.local/opt/ghidra_12.1.4_PUBLIC"
   JAVA_HOME = "/home/your-user/.local/opt/temurin-jdk-21.0.13.1+2"
   ```

For shell verification, supply the same paths to the REA CLI. The Ghidra check uses a local
ELF executable. The firmware check inventories signatures without extracting the package.

```sh
GHIDRA_INSTALL_DIR="$HOME/.local/opt/ghidra_12.1.4_PUBLIC" \
  JAVA_HOME="$HOME/.local/opt/temurin-jdk-21.0.13.1+2" \
  rea inspect /usr/bin/true --provider ghidra --format json

REA_BINWALK_COMMAND="$HOME/.local/opt/binwalk-3.1.0/bin/binwalk" \
  rea inspect-firmware-regions /absolute/path/to/D110_M_4.30.bin --format json
```

## References

- niimbluelib (`github.com/MultiMote/niimbluelib`, checked at `35110a7`, 2026-11-08):
  - `print_tasks/D110MV4PrintTask.ts`, `print_tasks/index.ts` (task per model and protocol version)
  - `packets/packet_generator.ts` (packet layouts), `utils/utils.ts` `countPixelsForBitmapPacket`
  - `packets/packet_parser.ts` (status data, heartbeats), `packets/enumerations.ts` `PrinterErrorCode`
  - `printer_models.ts` (D110_M id 2320, 203 dpi, 96-dot head), `client/serial_impl.ts` (115200 baud, 10 ms gap)
  - `test/dumps.ts` `D110M_V4_23`
- NiimBlue app (`github.com/MultiMote/niimblue`, `937200f`): clamps label width to the printhead
  (`PrinterConnector.svelte`) and warns "Label width is too big for your printer" (`PrintPreview.svelte`).
- BluePaper PR #96: the fixes described here.
