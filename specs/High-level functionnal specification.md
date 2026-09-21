# High-level functional specification

## Purpose
This document outlines the application at a high level.
From a functional perspective, everything must refer to it: it ensures consistency between features.
It is not intended to be extremely precise, but it must be readable by a user who wants to understand what the product does.

## Application Goals
The application enables a user to print a file to a printer on the local network.
The application must work without Internet access. It only needs to access the printer on the same LAN.

## Languages
The application must be available in English and French. English is the default language.

A print job concerns one file. The file is provided through the Android file picker or the sharing feature of another application.
The file is kept temporarily while it is previewed and printed, then deleted. The application does not retain a print history.

## Supported Formats
- Text documents:
    - [ ] PDF
    - [ ] DOCX
- Image files:
    - [ ] JPG
    - [ ] PNG

The list of formats actually available in a release is defined by that release. A supported format must be printable and normally previewable. If rendering the selected file fails, the application notifies the user and displays a clearly identified generic visual indicating that the actual preview is unavailable. The generic visual must not be presented as a valid preview of the file.

## Supported Printers
The application detects printers on the local network, including those that are not yet supported.

Version 1 only allows printing with the following printers:
- Epson:
    - [ ] XP 6000 series, including models such as XP-6105

A detected but unsupported printer must be clearly identified and cannot be used for printing.
The user can also add a printer manually using its IPv4 address. The application then checks whether it is supported.

## Main Features
### Automatic Printer Discovery
The application must automatically search the LAN for available printers and determine whether they pass an availability check.
This action must run in the background at two points:
- when the application opens
- when selecting a printer

In all cases, it must not block the user from taking actions.

The user can save a printer. A saved printer remains visible even when it is not detected; its status must then indicate that it is unavailable.

### Preview
The user must be able to preview their print job before starting it.
The preview must correspond to the selected file. If rendering it fails, the application must notify the user and display a clearly identified generic visual instead. The generic visual must not be presented as a valid preview of the selected file.

Detailed print settings are not defined in this high-level functional specification. They will be specified in the concrete implementation, without exceeding the scope of printing a file.

### Track Print Job Progress
The user must be able to view the progress of a print job.
The minimum functional states are:
- preparing;
- sending to the printer;
- printing;
- completed;
- failed;
- cancelled.

The user must be able to cancel a print job in progress.
When the printer does not provide reliable numeric progress, the application displays the textual print status instead of a percentage.

If there is a communication error, the printer is unavailable, or the printer reports a problem, the application displays the error to the user. No history is retained once the job is complete.
