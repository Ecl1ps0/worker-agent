package com.worker_agent.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class IPReciever {
  public static String getLocalIp() throws SocketException {
    Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
    while (nics.hasMoreElements()) {
        NetworkInterface nic = nics.nextElement();
        if (nic.isLoopback() || !nic.isUp() || nic.isVirtual()) continue;

        Enumeration<InetAddress> addrs = nic.getInetAddresses();
        while (addrs.hasMoreElements()) {
            InetAddress addr = addrs.nextElement();
            if (addr instanceof Inet4Address) {
                return addr.getHostAddress();
            }
        }
    }
    return "127.0.0.1";
}
}
