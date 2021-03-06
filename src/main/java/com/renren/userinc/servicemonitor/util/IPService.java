package com.renren.userinc.servicemonitor.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class IPService {
	private static String defaultIP = "127.0.0.1";
	private static String localIP = defaultIP;
	
	static {
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
						String ipStr = ip.getHostAddress();
						if(!defaultIP.equals(ipStr)){
							localIP = ipStr;
							break OutLoop;
						}
						
					}
				}
			}
		} catch (SocketException e) {
			// do nothing, localIP = defaultIP
		}
	}

	public static String getLocalIp() {
		return localIP;
	}
	
	public static void main(String[] args){
		System.out.println(IPService.getLocalIp());
	}
}
