# Feature: Epson XP 6000 series

## Scope

Support printing to Epson XP 6000 series printers discovered or added on the local network.

This feature covers the Epson XP 6000 series, including models such as XP-6105. Other Epson
models outside the XP 6000 series are outside the scope.

## Supported printer identification

An Epson printer is supported when its announced model identifies it as a member of the XP 6000
series.

The printer is not considered supported when:

- the manufacturer is not identified as Epson;
- the announced model is not a member of the XP 6000 series;
- the model cannot be determined.

The generic printer flow displays unsupported printers but does not allow them to be
selected for printing.

## Network discovery

The XP 6000 series driver participates in local-network discovery through:

- mDNS / Bonjour;
- WSD / WS-Discovery;
- SNMP.

The discovery feature must expose the results of these mechanisms without requiring
Internet access. When several mechanisms report the same printer, the results are merged
using the IPv4 address as the identity key.

The discovery layer must retain the printer MAC address when it is available. The driver
uses this address for Wake-on-LAN.

The generic discovery specification must support the WSD and SNMP mechanisms required by
this feature. This feature specification does not define their generic behavior for other
printer families.

## Manual addition

The user can add an XP 6000 series printer by entering its IPv4 address.

After the address is validated, the application checks the printer and identifies its
manufacturer and model. A printer that does not respond remains visible as unavailable.
A printer that responds but is not identified as an XP 6000 series model remains visible as unsupported.

Manual addition uses the same availability and printer-status flow as automatic discovery.

## Availability check

The driver checks whether the printer can be reached on the local network and obtains the
printer status needed by the generic printer flow.

The printer is available when the check receives a valid response from an XP 6000 series printer. It is
unavailable when the check fails or the printer cannot be reached.

The printer status includes, when reported by the printer:

- the generic availability state;
- the IPP printer state;
- the IPP printer-state reasons;
- ink status;
- paper status;
- cover status.

Ink, paper, and cover information is displayed in the printer interface when available.
Missing status information is not treated as an error by itself.

## Printing

Printing uses standard IPP. The driver automatically determines whether the printer's
announced IPP connection requires an encrypted or unencrypted connection.

The driver accepts every document format supported by the generic printing flow. The XP-
6000 feature does not introduce a separate document-format restriction.

The driver reports the printer acknowledgment, printing progress when reliable, successful
completion, cancellation result, communication failures, and printer-reported problems
through the generic print-job flow.

## Wake-on-LAN

When the initial network check fails before printing, the driver sends a Wake-on-LAN packet
when the printer MAC address was obtained through discovery and Wake-on-LAN is available
on the local network.

After sending the packet, the application waits and checks the printer again using the
project's standard retry and timeout values. Printing starts only after the printer is
reachable again. If the printer remains unreachable, the generic print-job flow reports the
failure.

If the MAC address is unavailable or Wake-on-LAN cannot be used on the local network, the
driver does not attempt to send a packet and follows the generic unavailable-printer flow.

## Error reporting

The driver maps printer-specific conditions to the generic printer and print-job error
model. The user receives a generic error category together with the detailed localized
message when one is available.

The XP 6000 series error catalog includes these principal cases:

| Condition | English | French |
| --- | --- | --- |
| Printer unreachable | Printer unavailable | Imprimante indisponible |
| No paper | Out of paper | Papier absent |
| Cover open | Cover open | Capot ouvert |
| Low or empty ink | Ink problem | Probleme d'encre |
| Printing error | Printing error | Erreur d'impression |

The error catalog is used by both availability checks and print jobs. An IPP status or
reason that does not match a catalog entry is reported using the generic error category
and its available IPP detail.

## User-visible behavior

- A discovered XP 6000 series printer is displayed with its Epson brand, model, IPv4 address, availability,
  and supported status.
- An XP 6000 series printer added by IPv4 address is displayed immediately after verification.
- A saved XP 6000 series printer remains visible according to the generic saved-printer rules, including
  when it is unavailable.
- An unavailable or unsupported XP 6000 series printer cannot be selected for a print job.
- Status details and errors use the application's English and French localization flows.

## Acceptance scenarios

### Discover an XP 6000 series printer

1. Given an XP 6000 series printer is reachable on the local network.
2. When discovery runs through mDNS, WSD, or SNMP.
3. Then the printer is recognized as an Epson XP 6000 series printer and appears once in the printer list.

### Print to a discovered XP 6000 series printer

1. Given a reachable and supported XP 6000 series printer is selected.
2. When the user starts a print job with a format supported by the generic flow.
3. Then the driver sends the job through standard IPP and the generic print-job flow reports
   its progress and terminal result.

### Wake an unavailable XP 6000 series printer before printing

1. Given a discovered XP 6000 series printer has a known MAC address and the first network check fails.
2. When the user starts a print job.
3. Then the driver sends Wake-on-LAN, waits using the project's standard values, checks the
   printer again, and prints if the printer becomes reachable.

### Report a printer problem

1. Given an XP 6000 series printer reports a paper, cover, ink, or printing problem.
2. When the driver receives the corresponding printer status or IPP reason.
3. Then the generic flow displays the appropriate category and the localized XP 6000 series
   message.

## Constraints

- The feature works without Internet access. The device and printer communicate over the
  same LAN.
- Printer-specific behavior remains isolated in the Epson XP 6000 series driver.
- The printing domain and generic printer flows do not depend on Epson-specific details.
- The feature follows the generic printer persistence, selection, localization, and print-job
  rules.
