package com.absinthe.kage.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetUtils {

    private static final String TAG = NetUtils.class.getSimpleName();
    private static final String DEFAULT_IP = "192.168.1.100";

    /**
     * 匹配C类地址IP
     **/
    private static final String REGEX_C_IP =
            "^192\\.168\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)$";
    /**
     * 匹配A类地址IP
     **/
    private static final String REGEX_A_IP =
            "^10\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)$";
    /**
     * 匹配B类地址IP
     **/
    private static final String REGEX_B_IP =
            "^172\\.(1[6-9]|2\\d|3[0-1])\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25\\d)$";
    private static final Pattern IP_PATTERN = Pattern.compile("(" + REGEX_A_IP + ")|" + "(" + REGEX_B_IP + ")|" + "(" + REGEX_C_IP + ")");

    @NonNull
    public static String getLocalAddress() {
        String hostIp;
        Enumeration<NetworkInterface> networkInterfaces = null;

        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        InetAddress address;
        if (networkInterfaces != null) {
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                while (inetAddresses.hasMoreElements()) {
                    address = inetAddresses.nextElement();
                    String hostAddress = address.getHostAddress();
                    Matcher matcher = IP_PATTERN.matcher(hostAddress);
                    if (matcher.matches()) {
                        hostIp = hostAddress;
                        Log.i(TAG, "Local IP: " + hostIp);
                        return hostIp;
                    }
                }
            }
        }
        return DEFAULT_IP;
    }

}
