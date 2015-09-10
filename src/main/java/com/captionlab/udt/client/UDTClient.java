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

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.barchart.udt.FactoryUDT;
import com.barchart.udt.OptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.ccc.UDPBlast;
import com.barchart.udt.net.NetSocketUDT;
import com.barchart.udt.util.LogUtil;

public class UDTClient {
	private static final Logger log = Logger.getLogger(UDTClient.class);
	private boolean wait = false;
	private boolean finished = false;
	// UDT has roughly 7% congestion control overhead
	// so we need to take that into account if we want
	// to limit wire speed
	private final double maxBW = 100 * 0.93;
	private long count = 0;
	private final long start = System.currentTimeMillis();
	
	public UDTClient(){
		LogUtil.configureLog();
	}
	
	public void transferFile(String sourceFile,String targetFile,String ipAddress,int port){
		this.finished = false;
		Future<Boolean> monResult = null;
		try{
			final NetSocketUDT clientSocket = new NetSocketUDT();
			clientSocket.socketUDT().setOption(OptionUDT.UDT_CC, new FactoryUDT<UDPBlast>(
					UDPBlast.class));
			final SocketAddress serverAddress = 
					new InetSocketAddress(ipAddress, port);
			clientSocket.socketUDT().setOption(OptionUDT.UDT_SNDTIMEO,60000);
			clientSocket.connect(serverAddress);
			log.info("Connected!! timeout "+clientSocket.getSoTimeout());
			final Object obj = clientSocket.socketUDT().getOption(OptionUDT.UDT_CC);
			final UDPBlast objCCC = (UDPBlast) obj;
			objCCC.setRate((int) maxBW);
			final File f = new File(sourceFile);
			final FileInputStream is = new FileInputStream(f);
			OutputStream os = clientSocket.getOutputStream();
			/*monResult = Executors.newSingleThreadExecutor()
					.submit(new Callable<Boolean>() {
						@Override
						public Boolean call() {
							return monitor(clientSocket.socketUDT());
						}
					});*/
			time(start);
			os.write((targetFile+"\n"+f.length()+"\n").getBytes("UTF-8"));
			count = copy(is, os,start);
			this.finished = true;
			Thread.sleep(220 * 5000);
			log.info("DONE WITH COPY!!");
			/*if (monResult != null){
				monResult.get();
			}*/
			IOUtils.closeQuietly(clientSocket);
		}catch(Exception e){
			log.error("Exception occured "+e);
		}
	}

	
	public boolean monitor(final SocketUDT socket) {

		System.out.println(
				"SendRate(Mb/s)\tRTT(ms)\tCWnd\tPktSndPeriod(ms)\tRecvACK\tRecvNAK\tPacketSent");
		try {
			if(wait){
				log.info("Waiting..........");
				Thread.sleep(10000);
			}
			while (!finished) {
				Thread.sleep(1000);
				socket.updateMonitor(false);
				System.out.printf(
						"%.3f\t\t" + "%.2f\t" + "%d\t" + "%.2f\t\t\t" + "%d\t"
								+ "%d\t"+"%d\n",
						socket.monitor().mbpsSendRate()/10, socket.monitor().currentMillisRTT(),
						socket.monitor().currentCongestionWindow(),
						socket.monitor().currentSendPeriod(),
						socket.monitor().globalReceivedAckTotal(),
						socket.monitor().globalReceivedNakTotal(),socket.monitor().localPacketsSent());
			}
			return true;
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private long copy(final InputStream input, final OutputStream output,final long start)
					throws IOException {

		final int DEFAULT_BUFFER_SIZE = 1024 * 4;
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
			//Tested pausing transfer for 10 secs
			/*if(count == 4096*15){
				System.out.println("Let me sleep some time !!");
				try {
					this.wait = true;
					Thread.sleep(10000);
					this.wait = false;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}*/
		}
		final long end = System.currentTimeMillis();
		log.info("TOTAL TIME: "+(end-start)/1000 + " seconds");
		return count;
	}

	private void time(final long start) {
		final TimerTask tt = new TimerTask() {
			@Override
			public void run() {
				final long cur = System.currentTimeMillis();
				final long secs = (cur - start)/1000;
				log.info("TRANSFERRED: "+count/1024+" SPEED: "+(count/1024)/secs + "KB/s");
			}
		};
		final Timer t = new Timer();
		t.schedule(tt, 2000, 2000);
	}
}
