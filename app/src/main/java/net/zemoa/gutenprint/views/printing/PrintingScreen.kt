package net.zemoa.gutenprint.views.printing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import net.zemoa.gutenprint.R
import net.zemoa.gutenprint.domains.printing.FileSelectionError
import net.zemoa.gutenprint.domains.printing.PreviewResult
import net.zemoa.gutenprint.domains.printing.PrintingState
import net.zemoa.gutenprint.domains.printing.PrintingStore

@Composable
fun PrintingRoute(
    store: PrintingStore,
    onOpenPicker: () -> Unit,
) {
    val state by store.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(store, resources) {
        store.events.collectLatest { error ->
            snackbarHostState.showSnackbar(resources.getString(error.messageRes()))
        }
    }

    PrintingScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onOpenPicker = onOpenPicker,
    )
}

@Composable
fun PrintingScreen(
    state: PrintingState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onOpenPicker: () -> Unit = {},
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.select_file_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Preview(
                result = state.preview,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = state.selectedFile?.displayName ?: stringResource(R.string.no_file_selected),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onOpenPicker,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.select_file))
            }
            Button(
                onClick = onOpenPicker,
                enabled = state.selectedFile != null && !state.isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.replace_file))
            }
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.print))
            }
        }
    }
}

@Composable
private fun Preview(
    result: PreviewResult?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        when (result) {
            is PreviewResult.Available -> result.image?.let { PreviewImage(it) }
            PreviewResult.Unavailable -> Image(
                painter = painterResource(android.R.drawable.ic_dialog_alert),
                contentDescription = stringResource(R.string.preview_unavailable),
            )
            null -> Spacer(Modifier)
        }
    }
}

@Composable
private fun PreviewImage(file: java.io.File) {
    val bitmap by produceState<Bitmap?>(initialValue = null, file) {
        value = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.path) }
    }
    if (bitmap == null) {
        CircularProgressIndicator()
    } else {
        val decodedBitmap = bitmap ?: return
        Image(
            bitmap = decodedBitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.file_preview),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

private fun FileSelectionError.messageRes(): Int = when (this) {
    FileSelectionError.UnsupportedFormat -> R.string.unsupported_format
    FileSelectionError.CannotRead -> R.string.file_cannot_read
    FileSelectionError.CopyFailed -> R.string.file_copy_failed
    FileSelectionError.MultipleFiles -> R.string.one_file_required
    FileSelectionError.PreviewUnavailable -> R.string.preview_unavailable
}

@Preview(showBackground = true)
@Composable
private fun PrintingScreenPreview() {
    net.zemoa.gutenprint.views.theme.GutenPrintTheme {
        PrintingScreen(state = PrintingState())
    }
}
