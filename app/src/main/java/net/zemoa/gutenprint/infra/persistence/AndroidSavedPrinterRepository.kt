package net.zemoa.gutenprint.infra.persistence

import android.content.Context
import net.zemoa.gutenprint.domains.printers.Printer
import net.zemoa.gutenprint.domains.printers.SavedPrinterRepository

class AndroidSavedPrinterRepository(context: Context) : SavedPrinterRepository {
    private val preferences = context.getSharedPreferences("saved-printers", Context.MODE_PRIVATE)

    override suspend fun load(): List<Printer> = preferences.all.mapNotNull { (address, value) ->
        val fields = (value as? String)?.split('|') ?: return@mapNotNull null
        Printer(
            ipv4Address = address,
            name = fields.getOrNull(0).nullIfEmpty(),
            brand = fields.getOrNull(1).nullIfEmpty(),
            model = fields.getOrNull(2).nullIfEmpty(),
            macAddress = fields.getOrNull(3).nullIfEmpty(),
            isSaved = true,
        )
    }

    override suspend fun save(printer: Printer) {
        preferences.edit().putString(
            printer.ipv4Address,
            listOf(printer.name, printer.brand, printer.model, printer.macAddress)
                .joinToString("|") { it.orEmpty() },
        ).apply()
    }

    override suspend fun remove(ipv4Address: String) {
        preferences.edit().remove(ipv4Address).apply()
    }

    private fun String?.nullIfEmpty() = takeUnless { it.isNullOrEmpty() }
}
