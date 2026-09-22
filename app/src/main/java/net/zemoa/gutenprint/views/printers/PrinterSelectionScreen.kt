package net.zemoa.gutenprint.views.printers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import net.zemoa.gutenprint.R
import net.zemoa.gutenprint.domains.printers.Printer
import net.zemoa.gutenprint.domains.printers.PrintersState
import net.zemoa.gutenprint.domains.printers.PrintersStore

@Composable
fun PrinterSelectionRoute(store: PrintersStore, onClose: () -> Unit) {
    val state by store.state.collectAsStateWithLifecycle()
    PrinterSelectionScreen(state, onClose, store::discover, store::select, store::save, store::removeSaved, store::add)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSelectionScreen(
    state: PrintersState,
    onClose: () -> Unit = {},
    onRefresh: suspend () -> Unit = {},
    onSelect: suspend (String) -> Unit = {},
    onSave: suspend (String) -> Unit = {},
    onRemoveSaved: suspend (String) -> Unit = {},
    onAdd: suspend (String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val refreshState = rememberPullToRefreshState()
    var address by remember { mutableStateOf("") }
    var invalidAddress by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<Printer?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_printer_title)) },
                navigationIcon = { IconButton(onClick = onClose) { Text("×") } },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isDiscovering,
            onRefresh = { scope.launch { onRefresh() } },
            state = refreshState,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!state.networkAvailable) Text(stringResource(R.string.no_local_network))
                if (state.discoveryFailed) Text(stringResource(R.string.discovery_failed))
                if (state.isDiscovering) CircularProgressIndicator()
                if (!state.isDiscovering && state.networkAvailable && state.printers.isEmpty()) {
                    Text(stringResource(R.string.no_printer_detected))
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.printers, key = { it.ipv4Address }) { printer ->
                        PrinterRow(printer, onSelect, onSave, { pendingRemoval = printer })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = address,
                        onValueChange = { address = it; invalidAddress = false },
                        label = { Text(stringResource(R.string.printer_ipv4)) },
                        isError = invalidAddress,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = {
                        if (IPV4_REGEX.matches(address)) {
                            scope.launch { onAdd(address); address = "" }
                        } else invalidAddress = true
                    }) { Text(stringResource(R.string.add_printer)) }
                }
            }
        }
    }
    pendingRemoval?.let { printer ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.remove_saved_printer_title)) },
            text = { Text(printer.name ?: printer.ipv4Address) },
            confirmButton = {
                Button(onClick = {
                    scope.launch { onRemoveSaved(printer.ipv4Address); pendingRemoval = null }
                }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = { Button(onClick = { pendingRemoval = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun PrinterRow(
    printer: Printer,
    onSelect: suspend (String) -> Unit,
    onSave: suspend (String) -> Unit,
    onRemoveRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = printer.isAvailable && printer.isSupported) {
                scope.launch { onSelect(printer.ipv4Address) }
            },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(printer.name ?: stringResource(R.string.unknown_printer))
            Text("${printer.brand ?: stringResource(R.string.unknown)} ${printer.model ?: stringResource(R.string.unknown)}")
            Text(printer.ipv4Address)
            Text(stringResource(if (printer.isAvailable) R.string.available else R.string.unavailable))
            if (!printer.isSupported) Text(stringResource(R.string.unsupported_printer))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (printer.isSaved) {
                    Text(stringResource(R.string.saved_printer))
                    Button(onClick = onRemoveRequest) { Text(stringResource(R.string.remove)) }
                } else {
                    Button(onClick = { scope.launch { onSave(printer.ipv4Address) } }) {
                        Text(stringResource(R.string.save_printer))
                    }
                }
            }
        }
    }
}

private val IPV4_REGEX = Regex("^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$")
