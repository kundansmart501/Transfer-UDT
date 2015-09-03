package com.captionlab.udt.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.barchart.udt.SocketUDT;
import com.barchart.udt.net.NetSocketUDT;
import com.barchart.udt.util.LogUtil;

public class UDTClient {
	private static final Logger log = Logger.getLogger(UDTClient.class);

	private static final long start = System.currentTimeMillis();
	private static int count = 0;
	private static final String sourceFile = "/home/kundan/KundanData/Download/dracula.mkv";
	private static final String targetFile = "dracula.mkv";
	private static Future<Boolean> monResult = null;
	private static  boolean finished = false;
	public static void main(String []args) throws Exception {
		LogUtil.configureLog();
		final NetSocketUDT clientSocket = new NetSocketUDT();
		/*final SocketAddress serverAddress = 
				new InetSocketAddress("182.71.214.250", 7777);*/
		final SocketAddress serverAddress = 
				new InetSocketAddress("192.168.2.191", 7777);
		clientSocket.connect(serverAddress);
		log.info("Connected!!");
		final File f = new File(sourceFile);
		final FileInputStream is = new FileInputStream(f);
		OutputStream os = clientSocket.getOutputStream();
		monResult = Executors.newSingleThreadExecutor()
				.submit(new Callable<Boolean>() {
					@Override
					public Boolean call() {
						return monitor(clientSocket.socketUDT());
					}
				});
		//time();
		os.write((targetFile+"\n"+f.length()+"\n").getBytes("UTF-8"));
		//IOUtils.copy(is, os);
		copy(is, os);
		log.info("DONE WITH COPY!!");
		finished = true;
		if (monResult != null)
			monResult.get();
		Thread.sleep(220 * 1000);
		//clientSocket.close();
	}

	public static boolean monitor(final SocketUDT socket) {

		System.out.println(
				"SendRate(Mb/s)\tRTT(ms)\tCWnd\tPktSndPeriod(ms)\tRecvACK\tRecvNAK");
		try {
			while (!finished) {

				Thread.sleep(1000);

				socket.updateMonitor(false);

				System.out.printf(
						"%.2f\t\t" + "%.2f\t" + "%d\t" + "%.2f\t\t\t" + "%d\t"
								+ "%d\n",
						socket.monitor().mbpsSendRate(), socket.monitor().currentMillisRTT(),
						socket.monitor().currentCongestionWindow(),
						socket.monitor().currentSendPeriod(),
						socket.monitor().globalReceivedAckTotal(),
						socket.monitor().globalReceivedNakTotal());
			}

			return true;

		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
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
