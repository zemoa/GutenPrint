package net.zemoa.gutenprint

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.zemoa.gutenprint.domains.printing.PrintingStore
import net.zemoa.gutenprint.infra.files.AndroidFileRepository
import net.zemoa.gutenprint.infra.files.AndroidPreviewRepository
import net.zemoa.gutenprint.infra.logging.AndroidLogger

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val logger = AndroidLogger()
    private val files = AndroidFileRepository(application.contentResolver, application.cacheDir, logger)
    val store = PrintingStore(files, AndroidPreviewRepository(application.cacheDir, logger), logger)

    init {
        val startedAt = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            files.clearTemporaryFiles(startedAt)
        }
    }
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(application) as T
}
