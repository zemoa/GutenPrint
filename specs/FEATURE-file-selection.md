# Feature: File Selection

## Scope

Allow the user to provide one file for a print job through the Android file picker or the Android sharing feature.

## Functional requirements

- Accept files provided through the Android file picker or Android sharing.
- Accept exactly one file for a print job.
- Accept PDF, DOCX, JPG, and PNG files in the first release.
- Reject unsupported files with a notification identifying that the format is not supported.
- Reject files that cannot be opened with a notification identifying that the file cannot be read.
- Reject a file when it cannot be copied to temporary storage, with a notification identifying the copy failure.
- Reject a shared input that contains multiple files, with a notification identifying that one file is required.
- Make the selected file available for preview before printing.
- Ensure that a successful preview corresponds to the selected file.
- If preview rendering fails, notify the user and display a clearly identified generic visual indicating that the actual preview is unavailable.
- Allow printing when the preview of an otherwise accepted file cannot be prepared.
- Allow the user to replace the selected file before printing.
- Delete the previous temporary file after the replacement file has been validated and copied successfully.
- Keep the file temporarily for the current print journey only.
- Delete the temporary file when the journey ends.
- Remove temporary files left after an unexpected application stop when the application starts again.

## User journeys

### File picker

1. The user opens the Android file picker.
2. The user selects one file.
3. The application checks the file format and that it can be opened.
4. The application copies the file to temporary storage.
5. The selected file is made available for preview and printing.

If the user cancels the picker without selecting a file, the current state remains unchanged.

### Android sharing

1. The user shares one file with the application.
2. The application checks the file format and that it can be opened.
3. The application copies the file to temporary storage.
4. The selected file is made available for preview and printing.

If the shared input contains multiple files, the application rejects it and displays a notification explaining that one file is required.

### File replacement

1. The user selects another file before printing.
2. The application validates and copies the new file.
3. After the new file has been copied successfully, the application deletes the previous temporary file.

If the new file is rejected, the previous file and its preview remain available.

If the preview of the new file cannot be prepared but the file is otherwise accepted, the file remains available for printing and the application displays the generic visual indicating that the actual preview is unavailable.

## Constraints

- A print job concerns exactly one file.
- The feature works without Internet access. Communication with a printer, when needed, occurs over the local network (LAN).
- The feature must not retain a print history.
