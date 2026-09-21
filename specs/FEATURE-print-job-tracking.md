# Feature: Print Job Tracking

## Scope

Present the lifecycle of the current print job on a dedicated screen and allow the user to follow it until completion, failure, or cancellation.

## Functional requirements

- Manage one current print job at a time.
- Prevent a new print job from starting while the current job is active.
- Display the current print job on a dedicated tracking screen.
- Continue the print job when the user leaves the tracking screen.
- Allow the user to return to the tracking screen while the job is active.
- Present the following states with these labels:
  - `preparing`: English `Preparing`, French `Préparation`;
  - `sending_to_printer`: English `Sending to the printer`, French `Envoi à l’imprimante`;
  - `printing`: English `Printing`, French `Impression`;
  - `cancelling`: English `Cancelling`, French `Annulation en cours`;
  - `printer_problem`: English `Printer problem`, French `Problème d’imprimante`;
  - `completed`: English `Completed`, French `Terminé`;
  - `failed`: English `Failed`, French `Échec`;
  - `cancelled`: English `Cancelled`, French `Annulé`.
- Display a numeric percentage only when the driver reports that the value is reliable.
- Display the textual state instead of a percentage when no reliable numeric progress is available.
- Allow the user to request cancellation while the job is active.
- Represent a cancellation request with `cancelling` until the driver confirms or rejects it.
- Keep the current state active when the driver cannot cancel a job that has already been transmitted.
- Display a localized message corresponding to the error identifier supplied by the driver.
- Display the state concerned together with the localized error message.
- Support a simulated print flow for automated tests without exposing it as a user feature.
- Remove the current job and its temporary file as soon as the job reaches a terminal state.
- Return the user to the screen from which the print job was started after leaving a terminal state.

## State behavior

### Preparing

`preparing` covers the technical preparation of the file before communication with the printer.

The job enters `sending_to_printer` after the technical preparation succeeds.

If technical preparation fails, the job enters `failed` and displays the localized error message.

### Sending to the printer

`sending_to_printer` covers the transfer of the print job to the printer.

The job enters `printing` after the printer acknowledges receipt of the job.

If communication fails during the transfer, the job enters `failed` immediately and displays the localized error message.

### Printing

`printing` indicates that the printer has acknowledged the job and is processing it.

The job enters `completed` when the driver reports successful completion.

The job enters `failed` when the driver reports an unrecoverable failure.

If the printer becomes temporarily unavailable, the application retries communication every five seconds for a maximum of thirty seconds. The job remains active during this wait. If communication cannot be restored within thirty seconds, the job enters `failed`.

### Cancelling

When the user requests cancellation, the job enters `cancelling` while the driver processes the request.

The job enters `cancelled` when the driver confirms cancellation.

If the driver cannot cancel a job that has already been transmitted, the job remains active and the application displays the reason supplied by the driver.

### Printer problem

`printer_problem` indicates that the printer reports a problem requiring resolution, such as a lack of paper.

The job remains blocked in this state until the problem is resolved or the driver reports that the job has failed.

The application displays the localized message corresponding to the problem identifier supplied by the driver.

### Terminal states

`completed`, `failed`, and `cancelled` are terminal states.

The terminal state remains visible until the user leaves the tracking screen. The current job and its temporary file are removed when the terminal state is reached. The user is then returned to the screen from which the print job was started.

## User journeys

### Successful print

1. The user starts printing a prepared file.
2. The application creates the current print job in `preparing`.
3. The application prepares the file technically.
4. The application transfers the job in `sending_to_printer`.
5. The printer acknowledges receipt.
6. The application displays `printing` and a reliable percentage when one is available.
7. The printer reports successful completion.
8. The application displays `completed`.
9. The current job and its temporary file are removed.
10. The user leaves the tracking screen and returns to the originating screen.

### Cancellation

1. The user requests cancellation while the job is active.
2. The application displays `cancelling`.
3. The driver processes the cancellation request.
4. If cancellation succeeds, the application displays `cancelled`.
5. If cancellation is not possible, the job remains active and the application displays the localized reason supplied by the driver.

### Communication failure

1. The driver reports a communication failure during transfer.
2. The application immediately displays `failed`.
3. The application displays the localized message corresponding to the driver error identifier.
4. The current job and its temporary file are removed.

### Temporary printer unavailability

1. The driver reports that the printer is temporarily unavailable while printing.
2. The application keeps the job active and retries every five seconds.
3. If communication is restored within thirty seconds, the job resumes its previous flow.
4. If communication is not restored within thirty seconds, the application displays `failed` with the localized error message.

### Printer-reported problem

1. The driver reports a printer problem.
2. The application displays `printer_problem` and the localized problem message.
3. The job remains blocked until the driver reports resolution or failure.

### Application stop

If the application stops unexpectedly while a job is active, the job is not resumed. Temporary files left by the interrupted journey are removed at the next application start, as defined by the file-selection feature.

## Simulated print flow

The simulated print flow is available to automated tests and is not exposed as a user feature.

The nominal simulated flow progresses automatically through `preparing`, `sending_to_printer`, `printing`, and `completed`.

The simulation also supports deterministic scenarios for:

- a communication error;
- temporary printer unavailability;
- a printer-reported problem.

## Constraints

- No print history is retained after the job is complete.
- The feature works without Internet access, using the LAN when a real printer is involved.
- The feature depends on the printer abstraction and must not depend on a concrete printer brand.
- The feature manages only one file and one current print job.
- The feature does not expose the simulated print flow in the production user interface.
