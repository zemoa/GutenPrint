package net.zemoa.gutenprint.infra.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.DatagramPacket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zemoa.gutenprint.domains.printers.DiscoveryResult
import net.zemoa.gutenprint.domains.printers.PrinterDiscovery
import net.zemoa.gutenprint.domains.printers.PrinterObservation

class AndroidPrinterDiscovery(context: Context) : PrinterDiscovery {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val wifi = context.getSystemService(WifiManager::class.java)

    override suspend fun discover(): DiscoveryResult = withContext(Dispatchers.IO) {
        if (!hasLocalNetwork()) return@withContext DiscoveryResult(networkAvailable = false)
        val lock = wifi.createMulticastLock("GutenPrintDiscovery").apply { setReferenceCounted(false) }
        runCatching {
            lock.acquire()
            discoverMdns()
        }.getOrElse { DiscoveryResult(networkAvailable = true) }
            .also { if (lock.isHeld) lock.release() }
    }

    override suspend fun verify(ipv4Address: String): PrinterObservation? = withContext(Dispatchers.IO) {
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ipv4Address, IPP_PORT), VERIFY_TIMEOUT_MS)
            }
            PrinterObservation(ipv4Address = ipv4Address)
        }.getOrNull()
    }

    private fun discoverMdns(): DiscoveryResult {
        val printers = linkedMapOf<String, PrinterObservation>()
        MulticastSocket(MDNS_PORT).use { socket ->
            socket.reuseAddress = true
            socket.soTimeout = RECEIVE_TIMEOUT_MS
            socket.joinGroup(MDNS_ADDRESS)
            SERVICE_TYPES.forEach { type ->
                val query = dnsQuery(type)
                socket.send(DatagramPacket(query, query.size, MDNS_ADDRESS, MDNS_PORT))
            }
            val buffer = ByteArray(MAX_PACKET_SIZE)
            val deadline = System.currentTimeMillis() + DISCOVERY_WINDOW_MS
            while (System.currentTimeMillis() < deadline) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val address = (packet.address as? Inet4Address)?.hostAddress ?: continue
                    parseResponse(packet.data, packet.length, address)?.let { observation ->
                        printers[observation.ipv4Address] = observation
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    break
                }
            }
            socket.leaveGroup(MDNS_ADDRESS)
        }
        return DiscoveryResult(networkAvailable = true, printers = printers.values.toList())
    }

    private fun dnsQuery(serviceType: String): ByteArray {
        val result = ArrayList<Byte>()
        result += 0.toByte()
        result += 0.toByte()
        result += 0.toByte()
        result += 0.toByte()
        result += 0.toByte()
        result += 1.toByte()
        result += 0.toByte()
        result += 0.toByte()
        result += 0.toByte()
        result += 0.toByte()
        result += 0.toByte()
        result += 0.toByte()
        serviceType.split('.').forEach { label ->
            result += label.length.toByte()
            label.toByteArray(Charsets.US_ASCII).forEach(result::add)
        }
        result += 0.toByte()
        result += 0.toByte()
        result += 12.toByte()
        result += 0.toByte()
        result += 1.toByte()
        return result.toByteArray()
    }

    private fun parseResponse(data: ByteArray, length: Int, sourceAddress: String): PrinterObservation? {
        if (length < DNS_HEADER_SIZE) return null
        var offset = DNS_HEADER_SIZE
        val questionCount = readUnsignedShort(data, 4)
        val answerCount = readUnsignedShort(data, 6)
        val authorityCount = readUnsignedShort(data, 8)
        val additionalCount = readUnsignedShort(data, 10)
        repeat(questionCount) {
            val question = readName(data, offset, length) ?: return null
            offset = question.second + 4
            if (offset > length) return null
        }

        var serviceName: String? = null
        var brand: String? = null
        var model: String? = null
        val textValues = mutableListOf<String>()
        repeat(answerCount + authorityCount + additionalCount) {
            val recordName = readName(data, offset, length) ?: return null
            offset = recordName.second
            if (offset + 10 > length) return null
            val type = readUnsignedShort(data, offset)
            val recordLength = readUnsignedShort(data, offset + 8)
            val recordStart = offset + 10
            val recordEnd = recordStart + recordLength
            if (recordEnd > length) return null
            when (type) {
                PTR_TYPE -> serviceName = readName(data, recordStart, length)?.first
                TXT_TYPE -> {
                    var textOffset = recordStart
                    while (textOffset < recordEnd) {
                        val textLength = data[textOffset].toInt() and 0xff
                        textOffset++
                        if (textOffset + textLength > recordEnd) break
                        textValues += String(data, textOffset, textLength, Charsets.UTF_8)
                        textOffset += textLength
                    }
                }
            }
            offset = recordEnd
        }
        val metadata = textValues.joinToString(" ")
        if (metadata.contains("epson", ignoreCase = true)) brand = "Epson"
        model = XP_MODEL.find(metadata)?.value
        return PrinterObservation(
            ipv4Address = sourceAddress,
            name = serviceName?.substringBefore('.')?.takeUnless { it.isNullOrBlank() },
            brand = brand,
            model = model,
        )
    }

    private fun readName(data: ByteArray, start: Int, length: Int): Pair<String, Int>? {
        val labels = mutableListOf<String>()
        var cursor = start
        var nextOffset = start
        var jumped = false
        while (cursor < length) {
            val size = data[cursor].toInt() and 0xff
            if (size == 0) {
                if (!jumped) nextOffset = cursor + 1
                return labels.joinToString(".") to nextOffset
            }
            if (size and 0xc0 == 0xc0) {
                if (cursor + 1 >= length) return null
                val pointer = ((size and 0x3f) shl 8) or (data[cursor + 1].toInt() and 0xff)
                if (!jumped) nextOffset = cursor + 2
                cursor = pointer
                jumped = true
                continue
            }
            if (size > 63 || cursor + 1 + size > length) return null
            labels += String(data, cursor + 1, size, Charsets.UTF_8)
            cursor += size + 1
        }
        return null
    }

    private fun readUnsignedShort(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xff) shl 8) or (data[offset + 1].toInt() and 0xff)

    private fun hasLocalNetwork(): Boolean = connectivity.activeNetwork?.let { network ->
        connectivity.getNetworkCapabilities(network)?.let { capabilities ->
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
    } == true

    private companion object {
        const val IPP_PORT = 631
        const val VERIFY_TIMEOUT_MS = 1_500
        const val MDNS_PORT = 5353
        const val RECEIVE_TIMEOUT_MS = 250
        const val DISCOVERY_WINDOW_MS = 1_500L
        const val MAX_PACKET_SIZE = 4_096
        const val DNS_HEADER_SIZE = 12
        const val PTR_TYPE = 12
        const val TXT_TYPE = 16
        val MDNS_ADDRESS: InetAddress = InetAddress.getByName("224.0.0.251")
        val SERVICE_TYPES = listOf("_ipp._tcp.local", "_ipps._tcp.local")
        val XP_MODEL = Regex("XP[- ]?6\\d{3}", RegexOption.IGNORE_CASE)
    }
}
