package com.absinthe.kage.utils

import timber.log.Timber
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*
import java.util.regex.Pattern

object NetUtils {

    private const val DEFAULT_IP = "192.168.1.100"

    /**
     * 匹配C类地址IP
     */
    private const val REGEX_C_IP = "^192\\.168\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)$"

    /**
     * 匹配A类地址IP
     */
    private const val REGEX_A_IP = "^10\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)$"

    /**
     * 匹配B类地址IP
     */
    private const val REGEX_B_IP = "^172\\.(1[6-9]|2\\d|3[0-1])\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)$"
    private val IP_PATTERN = Pattern.compile("($REGEX_A_IP)|($REGEX_B_IP)|($REGEX_C_IP)")

    val localAddress: String
        get() {
            val hostIp: String
            var networkInterfaces: Enumeration<NetworkInterface>? = null
            try {
                networkInterfaces = NetworkInterface.getNetworkInterfaces()
            } catch (e: SocketException) {
                e.printStackTrace()
            }
            var address: InetAddress
            if (networkInterfaces != null) {
                while (networkInterfaces.hasMoreElements()) {
                    val networkInterface = networkInterfaces.nextElement()

                    if (networkInterface.isLoopback || networkInterface.isVirtual) {
                        continue
                    }

                    val inetAddresses = networkInterface.inetAddresses

                    while (inetAddresses.hasMoreElements()) {
                        address = inetAddresses.nextElement()
                        val hostAddress = address.hostAddress
                        val matcher = IP_PATTERN.matcher(hostAddress)

                        if (matcher.matches()) {
                            hostIp = hostAddress
                            Timber.i("Local IP: $hostIp")
                            return hostIp
                        }
                    }
                }
            }
            return DEFAULT_IP
        }
}