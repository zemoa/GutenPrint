package net.zemoa.gutenprint.domains.printers

data class Printer(
    val ipv4Address: String,
    val name: String?,
    val brand: String?,
    val model: String?,
    val macAddress: String? = null,
    val isAvailable: Boolean = false,
    val isSupported: Boolean = false,
    val isSaved: Boolean = false,
    val isSelected: Boolean = false,
)

data class PrinterObservation(
    val ipv4Address: String,
    val name: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val macAddress: String? = null,
)

data class DiscoveryResult(
    val networkAvailable: Boolean,
    val printers: List<PrinterObservation> = emptyList(),
)

interface PrinterDiscovery {
    suspend fun discover(): DiscoveryResult
    suspend fun verify(ipv4Address: String): PrinterObservation?
}

interface SavedPrinterRepository {
    suspend fun load(): List<Printer>
    suspend fun save(printer: Printer)
    suspend fun remove(ipv4Address: String)
}
