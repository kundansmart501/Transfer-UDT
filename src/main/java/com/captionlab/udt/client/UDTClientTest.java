package com.captionlab.udt.client;

public class UDTClientTest {
	
	private static final String sourceFile = "/home/kundan/KundanData/Download/dracula.mkv";
	private static final String targetFile = "dracula.mkv";
	private static final String ipAddress = "182.71.214.250";
	private static final int port = 7777;
	
	public static void main(String []args){
		UDTClient udtClient = new UDTClient();
		udtClient.transferFile(sourceFile, targetFile, ipAddress, port);
	}
}
