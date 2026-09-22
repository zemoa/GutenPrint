package net.zemoa.gutenprint.domains.printers

interface Driver {
    fun supports(brand: String?, model: String?): Boolean
}

class DriverCatalog(val drivers: List<Driver>)

class DriverRegistry(private val catalog: DriverCatalog) {
    fun isSupported(brand: String?, model: String?): Boolean =
        brand != null && model != null && catalog.drivers.any { it.supports(brand, model) }
}
