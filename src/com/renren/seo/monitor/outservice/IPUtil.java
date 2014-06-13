package com.renren.seo.monitor.outservice;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class IPUtil {
	private static String defaultIP = "127.0.0.1";
	private static String localIP = defaultIP;

	public static String getLocalIp() {
		if(localIP != defaultIP){
			return localIP;
		}
		try {
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface
					.getNetworkInterfaces();

			InetAddress ip = null;
			OutLoop: while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = (NetworkInterface) allNetInterfaces
						.nextElement();
				Enumeration<InetAddress> addresses = netInterface
						.getInetAddresses();
				while (addresses.hasMoreElements()) {
					ip = (InetAddress) addresses.nextElement();
					if (ip != null && ip instanceof Inet4Address) {
						localIP = ip.getHostAddress();
						if(!"127.0.0.1".equals(localIP)){
							break OutLoop;
						}
						
					}
				}
			}
		} catch (SocketException e) {
			localIP = defaultIP;
		}
		return localIP;
	}
}
