package net.zemoa.gutenprint

import kotlinx.coroutines.test.runTest
import net.zemoa.gutenprint.domains.printers.DiscoveryResult
import net.zemoa.gutenprint.domains.printers.Driver
import net.zemoa.gutenprint.domains.printers.DriverCatalog
import net.zemoa.gutenprint.domains.printers.DriverRegistry
import net.zemoa.gutenprint.domains.printers.Printer
import net.zemoa.gutenprint.domains.printers.PrinterDiscovery
import net.zemoa.gutenprint.domains.printers.PrinterObservation
import net.zemoa.gutenprint.domains.printers.PrintersStore
import net.zemoa.gutenprint.domains.printers.SavedPrinterRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import net.zemoa.gutenprint.infra.drivers.epson.EpsonXp6000Driver

class PrintersStoreTest {
    @Test
    fun discoveredPrintersAreMergedWithSavedPrintersByAddress() = runTest {
        val saved = Printer("192.168.1.10", "Saved", "Epson", "XP-6105", isSaved = true)
        val discovery = FakeDiscovery(
            DiscoveryResult(true, listOf(PrinterObservation("192.168.1.10", "Detected", "Epson", "XP-6105")))
        )
        val store = PrintersStore(discovery, FakeSaved(saved), DriverRegistry(DriverCatalog(listOf(EpsonXp6000Driver()))))
        store.initialize()

        store.discover()

        assertEquals(1, store.state.value.printers.size)
        assertTrue(store.state.value.printers.single().isSaved)
        assertTrue(store.state.value.printers.single().isAvailable)
    }

    @Test
    fun unsupportedOrUnavailablePrintersCannotBeSelected() = runTest {
        val discovery = FakeDiscovery(
            DiscoveryResult(true, listOf(PrinterObservation("192.168.1.10", "Unknown")))
        )
        val store = PrintersStore(discovery, FakeSaved(), DriverRegistry(DriverCatalog(emptyList())))

        store.discover()
        store.select("192.168.1.10")

        assertFalse(store.state.value.printers.single().isSelected)
    }

    @Test
    fun failedManualVerificationKeepsPrinterVisibleAsUnavailable() = runTest {
        val store = PrintersStore(FakeDiscovery(null), FakeSaved(), DriverRegistry(DriverCatalog(emptyList())))

        store.add("192.168.1.20")

        val printer = store.state.value.printers.single()
        assertEquals("192.168.1.20", printer.ipv4Address)
        assertFalse(printer.isAvailable)
        assertFalse(printer.isSupported)
    }

    @Test
    fun printerMissingFromSuccessfulDiscoveryBecomesUnavailableAndIsDeselected() = runTest {
        val discovery = FakeDiscovery(
            DiscoveryResult(true, listOf(PrinterObservation("192.168.1.10", "Epson", "Epson", "XP-6105")))
        )
        val store = PrintersStore(discovery, FakeSaved(), DriverRegistry(DriverCatalog(listOf(EpsonXp6000Driver()))))

        store.discover()
        store.select("192.168.1.10")
        discovery.result = DiscoveryResult(true)
        store.discover()

        assertFalse(store.state.value.printers.single().isAvailable)
        assertFalse(store.state.value.printers.single().isSelected)
    }

    @Test
    fun removingSaveKeepsACurrentlyAvailablePrinterInTheList() = runTest {
        val printer = Printer("192.168.1.10", "Epson", "Epson", "XP-6105", isAvailable = true, isSaved = true)
        val repository = FakeSaved(printer)
        val store = PrintersStore(
            FakeDiscovery(DiscoveryResult(true, listOf(PrinterObservation("192.168.1.10", "Epson")))),
            repository,
            DriverRegistry(DriverCatalog(emptyList())),
        )
        store.initialize()
        store.discover()

        store.removeSaved("192.168.1.10")

        assertEquals(1, store.state.value.printers.size)
        assertFalse(store.state.value.printers.single().isSaved)
    }

    private class FakeDiscovery(var result: DiscoveryResult?) : PrinterDiscovery {
        override suspend fun discover() = result ?: DiscoveryResult(true)
        override suspend fun verify(ipv4Address: String) = result?.printers?.firstOrNull()
    }

    private class FakeSaved(private vararg val initial: Printer) : SavedPrinterRepository {
        override suspend fun load() = initial.toList()
        override suspend fun save(printer: Printer) = Unit
        override suspend fun remove(ipv4Address: String) = Unit
    }
}
