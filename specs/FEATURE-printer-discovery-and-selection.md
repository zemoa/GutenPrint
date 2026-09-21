# Feature: Printer Discovery and Selection

## Scope

The application searches available networks for printers, displays them with their status, and allows one supported and available printer to be selected for the current print job.

The feature does not contain printer-brand- or printer-family-specific behavior.

## Automatic Discovery

Discovery starts automatically:

- when the application opens;
- when the printer selection screen opens.

Discovery runs in the background and does not block other user actions.

The user can restart discovery using pull-to-refresh.

If discovery is already running, a new request is ignored.

A global loading indicator is displayed while discovery is running.

## Network Scope

Discovery searches all available networks.

Discovery uses mDNS / DNS-SD and IPP announcements over mDNS.

If no local network is available, the screen displays the following dedicated state:

- English: `No local network`;
- French: `Aucun réseau local disponible`.

## Printer List

For each printer, the list displays:

- name;
- brand and model;
- IPv4 address;
- availability;
- a saved-printer indicator.

Saved printers appear first.

The visible availability statuses are:

- English: `Available` and `Unavailable`;
- French: `Disponible` and `Indisponible`.

A printer is available when it responds to network discovery.

If a printer name is unavailable, the list displays:

- English: `Unknown printer`;
- French: `Imprimante inconnue`.

If a printer brand or model is unavailable, the list displays:

- English: `Unknown`;
- French: `Inconnu`.

## Unsupported Printers

Unsupported printers remain visible in the main list.

They are clearly identified as unsupported and cannot be selected.

The printer is supported when a corresponding driver exists in the driver catalog.

If verification cannot determine whether a printer is supported, the printer is treated as unsupported.

The unsupported status is displayed as:

- English: `Unsupported`;
- French: `Non supportée`.

## Selection

Only one printer can be selected for the current print job.

A printer can be selected only if it is:

- supported;
- available.

Selection happens immediately, without an additional screen or confirmation.

A saved printer that was not detected remains visible as unavailable and cannot be selected.

If the selected printer becomes unavailable, it is automatically deselected.

## Manual Addition

The user can add a printer using its IPv4 address.

Other address formats are outside the current scope.

An invalid IPv4 address prevents form validation.

After the address is validated, the application checks the printer.

A manually added printer appears immediately in the list after verification.

A manually added printer that does not respond to verification is added to the list as unavailable.

A manually added printer identified as unsupported remains in the list but cannot be selected.

## Saving Printers

The user can save a printer from the list.

A saved printer is identified with an icon and the following status:

- English: `Saved`;
- French: `Sauvegardée`.

Saved printers are retained permanently after the application is closed.

They remain visible when they are not detected and are then displayed as unavailable.

A saved printer can be removed from the list after confirmation.

When a discovered printer has the same IPv4 address as a saved printer, both entries are automatically merged.

## No Results and Errors

If no printer is found, the screen displays a simple empty state:

- English: `No printer detected`;
- French: `Aucune imprimante détectée`.

If an error prevents discovery from working:

- already known or saved printers remain visible;
- an error message is displayed to the user:
  - English: `Discovery failed`;
  - French: `Échec de la découverte`.
