package com.captionlab.udt.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.barchart.udt.net.NetSocketUDT;
import com.barchart.udt.util.LogUtil;

public class UDTClient {
	private static final Logger log = Logger.getLogger(UDTClient.class);

	private static final long start = System.currentTimeMillis();
	private static int count = 0;
	private static final String sourceFile = "/home/kundan/Music/02_-_Tu_Hai_Ki_Nahi_-_www_songsfarm_info.mp3";
	private static final String targetFile = "02_-_Tu_Hai_Ki_Nahi_-_www_songsfarm_info.mp3";
	
	public static void main(String []args) throws Exception {
		LogUtil.configureLog();
		final Socket clientSocket = new NetSocketUDT();
		/*final SocketAddress serverAddress = 
				new InetSocketAddress("182.71.214.250", 7777);*/
		final SocketAddress serverAddress = 
				new InetSocketAddress("192.168.2.157", 7777);
		clientSocket.connect(serverAddress);
		log.info("Connected!!");
		final File f = new File(sourceFile);
		final FileInputStream is = new FileInputStream(f);
		OutputStream os = clientSocket.getOutputStream();
		time();
		os.write((targetFile+"\n"+f.length()+"\n").getBytes("UTF-8"));
		//IOUtils.copy(is, os);
		copy(is, os);
		log.info("DONE WITH COPY!!");
		Thread.sleep(220 * 1000);
		clientSocket.close();
	}

	private static long copy(final InputStream input, final OutputStream output)
			throws IOException {

		final int DEFAULT_BUFFER_SIZE = 1024 * 4;
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		final long end = System.currentTimeMillis();
		log.info("TOTAL TIME: "+(end-start)/1000 + " seconds");
		return count;
	}

	private static void time() {
		final TimerTask tt = new TimerTask() {
			@Override
			public void run() {
				final long cur = System.currentTimeMillis();
				final long secs = (cur - start)/1000;
				log.info("TRANSFERRED: "+count/1024+" SPEED: "+(count/1024)/secs + "KB/s");
				log.info("Thread name "+Thread.currentThread().getId());
			}
		};
		final Timer t = new Timer();
		t.schedule(tt, 2000, 2000);
	}
}
