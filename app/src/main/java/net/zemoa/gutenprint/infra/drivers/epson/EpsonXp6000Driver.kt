package net.zemoa.gutenprint.infra.drivers.epson

import net.zemoa.gutenprint.domains.printers.Driver

class EpsonXp6000Driver : Driver {
    override fun supports(brand: String?, model: String?): Boolean {
        if (brand?.trim()?.equals("Epson", ignoreCase = true) != true) return false
        return XP_6000_MODEL.matches(model?.trim().orEmpty())
    }

    private companion object {
        val XP_6000_MODEL = Regex("XP[- ]?6\\d{3}", RegexOption.IGNORE_CASE)
    }
}
