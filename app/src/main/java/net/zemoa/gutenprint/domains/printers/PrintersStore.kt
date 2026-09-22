package net.zemoa.gutenprint.domains.printers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.zemoa.gutenprint.domains.interfaces.Logger
import net.zemoa.gutenprint.domains.interfaces.NoOpLogger

data class PrintersState(
    val printers: List<Printer> = emptyList(),
    val isDiscovering: Boolean = false,
    val networkAvailable: Boolean = true,
    val discoveryFailed: Boolean = false,
)

class PrintersStore(
    private val discovery: PrinterDiscovery,
    private val savedPrinters: SavedPrinterRepository,
    private val drivers: DriverRegistry,
    private val logger: Logger = NoOpLogger,
) {
    private companion object {
        const val TAG = "PrintersStore"
    }

    private val mutableState = MutableStateFlow(PrintersState())
    private val mutex = Mutex()
    val state: StateFlow<PrintersState> = mutableState.asStateFlow()

    suspend fun initialize() = mutex.withLock {
        val stored = savedPrinters.load().map { it.copy(isSaved = true, isAvailable = false) }
        mutableState.value = mutableState.value.copy(printers = merge(emptyList(), stored))
    }

    suspend fun discover() {
        if (!mutex.tryLock()) return
        try {
            if (mutableState.value.isDiscovering) return
            mutableState.value = mutableState.value.copy(isDiscovering = true, discoveryFailed = false)
            logger.info(TAG, "Starting printer discovery")
            try {
                val result = discovery.discover()
                val discovered = result.printers.map { it.toPrinter() }
                val retained = mutableState.value.printers
                    .map { it.copy(isAvailable = false, isSelected = false) }
                mutableState.value = mutableState.value.copy(
                    printers = merge(discovered, retained),
                    isDiscovering = false,
                    networkAvailable = result.networkAvailable,
                    discoveryFailed = false,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                logger.error(TAG, "Printer discovery failed", exception)
                mutableState.value = mutableState.value.copy(isDiscovering = false, discoveryFailed = true)
            }
        } finally {
            mutex.unlock()
        }
    }

    suspend fun add(ipv4Address: String) {
        val observation = try {
            discovery.verify(ipv4Address)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            logger.warning(TAG, "Manual printer verification failed: ${exception.message}")
            null
        }
        val printer = (observation ?: PrinterObservation(ipv4Address)).toPrinter(observation != null)
        mutex.withLock {
            mutableState.value = mutableState.value.copy(printers = merge(listOf(printer), mutableState.value.printers))
        }
    }

    suspend fun select(ipv4Address: String) = mutex.withLock {
        mutableState.value = mutableState.value.copy(
            printers = mutableState.value.printers.map {
                it.copy(isSelected = it.ipv4Address == ipv4Address && it.isAvailable && it.isSupported)
            },
        )
    }

    suspend fun save(ipv4Address: String) = mutex.withLock {
        mutableState.value.printers.firstOrNull { it.ipv4Address == ipv4Address }?.let { printer ->
            val saved = printer.copy(isSaved = true)
            savedPrinters.save(saved)
            mutableState.value = mutableState.value.copy(printers = merge(listOf(saved), mutableState.value.printers))
        }
    }

    suspend fun removeSaved(ipv4Address: String) = mutex.withLock {
        savedPrinters.remove(ipv4Address)
        mutableState.value = mutableState.value.copy(
            printers = mutableState.value.printers.mapNotNull {
                if (it.ipv4Address != ipv4Address) {
                    it
                } else if (it.isAvailable) {
                    it.copy(isSaved = false)
                } else {
                    null
                }
            }
        )
    }

    private fun PrinterObservation.toPrinter(available: Boolean = true) = Printer(
        ipv4Address = ipv4Address,
        name = name,
        brand = brand,
        model = model,
        macAddress = macAddress,
        isAvailable = available,
        isSupported = drivers.isSupported(brand, model),
    )

    private fun merge(incoming: List<Printer>, existing: List<Printer>): List<Printer> {
        val merged = (existing + incoming).groupBy { it.ipv4Address }.map { (_, entries) ->
            entries.reduce { old, new ->
                new.copy(
                    name = new.name ?: old.name,
                    brand = new.brand ?: old.brand,
                    model = new.model ?: old.model,
                    macAddress = new.macAddress ?: old.macAddress,
                    isSaved = old.isSaved || new.isSaved,
                    isSelected = old.isSelected && new.isAvailable && new.isSupported,
                )
            }
        }
        return merged.sortedWith(compareByDescending<Printer> { it.isSaved }.thenBy { it.name ?: it.ipv4Address })
    }
}
