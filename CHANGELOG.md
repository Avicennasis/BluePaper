# Changelog

All notable changes to `BluePaper` will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial project scaffolding.
- Printers already paired with the computer are listed when the scanner opens, tagged "Paired".
- D110_M printing over protocol v4 (NiimBlue's D110M_V4 sequence), chosen automatically when the printer reports v4.
- Imported images are shrunk to fit the label and centred.
- Printer error reports (lid open, out of labels, RFID tag full, …) are shown as readable messages.
- Pre-print check on protocol-v4 printers: lid open, no labels, or an unreadable roll RFID tag stops the
  job with a clear message instead of feeding a blank label.
- The editor selects the connected printer's model automatically.

### Fixed
- Build failed when the default `java` is a JRE: `:shared` and `:androidApp` now pin the JDK 17 toolchain.
- Connecting to a printer already connected to the PC failed with "device not found".
- Replies were mismatched once the printer sent 0xD3 check-line notices, breaking print-status polling.
- Scanning used one prefix per concurrent session, so only one model's printers were found.
- Load/Save crashed with "Cannot call invokeAndWait from the event dispatcher thread".
- D110_M configuration: 203 dpi with a 96-dot printhead (was 300 dpi); label canvases are capped at the
  printhead width, since wider rows printed blank.
