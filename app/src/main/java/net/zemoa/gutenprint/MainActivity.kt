package net.zemoa.gutenprint

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import net.zemoa.gutenprint.infra.logging.AndroidLogger
import net.zemoa.gutenprint.views.printing.PrintingRoute
import net.zemoa.gutenprint.views.printers.PrinterSelectionRoute
import net.zemoa.gutenprint.views.theme.GutenPrintTheme

class MainActivity : AppCompatActivity() {
    private companion object {
        const val TAG = "MainActivity"
    }

    private val logger = AndroidLogger()
    private val viewModel by lazy {
        ViewModelProvider(this, MainViewModelFactory(application))[MainViewModel::class.java]
    }
    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let(::select)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.info(TAG, "Activity created")
        setContent {
            GutenPrintTheme {
                val showPrinters by viewModel.showPrinters.collectAsStateWithLifecycle()
                if (showPrinters) {
                    PrinterSelectionRoute(
                        store = viewModel.printers,
                        onClose = viewModel::closePrinterSelection,
                    )
                } else {
                    PrintingRoute(
                        store = viewModel.store,
                        onOpenPicker = ::openPicker,
                        onSelectPrinter = viewModel::openPrinterSelection,
                        selectedPrinter = viewModel.selectedPrinter.collectAsStateWithLifecycle().value,
                    )
                }
            }
        }
        if (savedInstanceState == null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun openPicker() {
        logger.info(TAG, "Opening document picker")
        picker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        })
    }

    private fun handleIntent(intent: Intent) {
        logger.debug(TAG, "Handling intent: action=${intent.action} type=${intent.type}")
        when (intent.action) {
            Intent.ACTION_SEND -> getSharedUri(intent)?.let(::select)
                ?: viewModel.store.rejectUnreadableFile()
            Intent.ACTION_SEND_MULTIPLE -> viewModel.store.rejectMultipleFiles()
        }
    }

    private fun select(uri: Uri) {
        logger.debug(TAG, "Selecting shared URI: $uri")
        lifecycleScope.launch { viewModel.store.select(uri.toString()) }
    }

    @Suppress("DEPRECATION")
    private fun getSharedUri(intent: Intent): Uri? =
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

}
