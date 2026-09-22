package net.zemoa.gutenprint

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.zemoa.gutenprint.domains.printing.PrintingStore
import net.zemoa.gutenprint.domains.printers.DriverCatalog
import net.zemoa.gutenprint.domains.printers.DriverRegistry
import net.zemoa.gutenprint.domains.printers.PrintersStore
import net.zemoa.gutenprint.infra.files.AndroidFileRepository
import net.zemoa.gutenprint.infra.files.AndroidPreviewRepository
import net.zemoa.gutenprint.infra.drivers.epson.EpsonXp6000Driver
import net.zemoa.gutenprint.infra.logging.AndroidLogger
import net.zemoa.gutenprint.infra.network.AndroidPrinterDiscovery
import net.zemoa.gutenprint.infra.persistence.AndroidSavedPrinterRepository

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val logger = AndroidLogger()
    private val files = AndroidFileRepository(application.contentResolver, application.cacheDir, logger)
    val store = PrintingStore(files, AndroidPreviewRepository(application.cacheDir, logger), logger)
    val printers = PrintersStore(
        AndroidPrinterDiscovery(application),
        AndroidSavedPrinterRepository(application),
        DriverRegistry(DriverCatalog(listOf(EpsonXp6000Driver()))),
        logger,
    )
    private val _showPrinters = MutableStateFlow(false)
    val showPrinters = _showPrinters.asStateFlow()
    val selectedPrinter = printers.state.map { state ->
        state.printers.firstOrNull { it.isSelected }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        val startedAt = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            files.clearTemporaryFiles(startedAt)
        }
        viewModelScope.launch {
            printers.initialize()
            printers.discover()
        }
    }

    fun openPrinterSelection() {
        _showPrinters.value = true
        viewModelScope.launch { printers.discover() }
    }

    fun closePrinterSelection() {
        _showPrinters.value = false
    }
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(application) as T
}
