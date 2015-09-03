/**
 * Copyright (C) 2009-2013 Barchart, Inc. <http://www.barchart.com/>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.barchart.udt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.barchart.udt.net.NetServerSocketUDT;

public class AppServer {

	/**
	 * @param args
	 * @throws IOException
	 */

	static Logger log = LoggerFactory.getLogger(AppServer.class);
	
	private static int count;
	protected static long start;
	private static final ExecutorService readPool = Executors.newCachedThreadPool();

	public static void main(final String[] args) throws IOException {

		int port = 9000;

		if (args.length > 1) {
			System.out.println("usage: appserver [server_port]");
			return;
		}

		if (args.length == 1) {
			port = Integer.parseInt(args[0]);
		}

		final NetServerSocketUDT acceptorSocket = new NetServerSocketUDT();
		acceptorSocket.bind(new InetSocketAddress(getLocalHost(), port), 256);

		System.out.printf("server is ready at port: %d\n", port);
		System.out.println("server is ready");
		while (true) {

			final Socket clientSocket = acceptorSocket.accept();

			// Start the read ahead background task
			Executors.newSingleThreadExecutor().submit(new Callable<Boolean>() {
				@Override
				public Boolean call() {
					return clientTask(clientSocket);
				}
			});
		}
	}

	public static boolean clientTask(final Socket clientSocket) {

		/*final byte[] data = new byte[10000];

		try {

			final InputStream is = clientSocket.getInputStream();

			while (true) {

				int remain = data.length;

				while (remain > 0) {
					final int ret = is.read(data, data.length - remain, remain);
					remain -= ret;
				}
			}

		} catch (final IOException ioe) {
			ioe.printStackTrace();
			return false;
		}*/
		
		copyFile(clientSocket);
		return true;
	}
	
	private static void  copyFile(final Socket sock) {
		log.info("Inside copy file ");
		final Runnable runner = new Runnable() {
			public void run() {
				InputStream is = null;
				OutputStream os = null;
				try {
					is = sock.getInputStream();
					final byte[] bytes = new byte[1024];
					final int bytesRead = is.read(bytes);
					final String str = new String(bytes);
					if (str.startsWith("GET ")) {
						int nameIndex = 0;
						for (final byte b : bytes) {
							if (b == '\n') {
								break;
							}
							nameIndex++;
						}
						// Eat the \n.
						nameIndex++;
						final String fileName = new String(bytes, 4, nameIndex).trim();
						log.info("File name: "+fileName);
						final File f = new File(fileName);
						final FileInputStream fis = new FileInputStream(f);
						os = sock.getOutputStream();

						copy(fis, os, f.length());
						os.close();
						return;
					}
					int nameIndex = 0;
					int lengthIndex = 0;
					boolean foundFirst = false;
					//boolean foundSecond = false;
					for (final byte b : bytes) {
						if (!foundFirst) {
							nameIndex++;
						}
						lengthIndex++;
						if (b == '\n') {
							if (foundFirst) {
								break;
							}
							foundFirst = true;
						}
					}
					if (nameIndex < 2) {
						// First bytes was a LF?
						sock.close();
						return;
					}
					String dataString = new String(bytes);
					int fileLengthIndex = dataString.indexOf("\n",nameIndex+1);
					final String fileName = new String(bytes, 0, nameIndex).trim();
					final String lengthString = dataString.substring(nameIndex,fileLengthIndex);
					log.info("lengthString "+lengthString);
					final long length = Long.parseLong(lengthString);
					final File file = new File(fileName);
					os = new FileOutputStream(file);
					final int len = bytesRead - lengthIndex;
					if (len > 0) {
						os.write(bytes, lengthIndex, len);
					}
					start = System.currentTimeMillis();
					copy(is, os, length);
				} catch (final IOException e) {
					log.info("Exception reading file...", e);
				} finally {
					IOUtils.closeQuietly(is);
					IOUtils.closeQuietly(os);
					IOUtils.closeQuietly(sock);
				}
			}
		};
		log.info("Executing copy...");
		readPool.execute(runner);
	}

	/**
	 * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
	 * <code>OutputStream</code>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * 
	 * @param input  the <code>InputStream</code> to read from
	 * @param output  the <code>OutputStream</code> to write to
	 * @param length 
	 * @return the number of bytes copied
	 * @throws NullPointerException if the input or output is null
	 * @throws IOException if an I/O error occurs
	 * @since Commons IO 1.3
	 */
	/*
    private static long copy(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[1024 * 4];
        count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
	 */

	private static long copy(final InputStream input, final OutputStream output, 
			final long length) throws IOException {

		final int DEFAULT_BUFFER_SIZE = 1024 * 4;
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		// long count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
			if (count == length) {
				break;
			}
			// log.info("Bytes written: "+count);
		}
		final long end = System.currentTimeMillis();
		log.info("TOTAL TIME: " + (end - start) / 1000 + " seconds");
		return count;
	}

	private void time() {
		final TimerTask tt = new TimerTask() {

			@Override
			public void run() {
				final long cur = System.currentTimeMillis();
				final long secs = (cur - start)/1000;
				log.info("Received: "+count/1024+" SPEED: "+(count/1024)/secs + "KB/s");
			}
		};
		final Timer t = new Timer();
		t.schedule(tt, 2000, 2000);
	}

	/**
	 * Many Linux systems typically return 127.0.0.1 as the localhost address
	 * instead of the address assigned on the local network. It has to do with
	 * how localhost is defined in /etc/hosts. This method creates a quick
	 * UDP socket and gets the local address for the socket on Linux systems
	 * to get around the problem. This can also happen on OSX in newer
	 * versions of the OS.
	 * 
	 * @return The local network address in a cross-platform manner.
	 * @throws UnknownHostException If the host is considered unknown for 
	 * any reason.
	 */
	private static InetAddress getLocalHost() throws UnknownHostException {
		final InetAddress is = InetAddress.getLocalHost();
		if (!is.isLoopbackAddress()) {
			return is;
		}

		return getLocalHostViaUdp();
	}

	private static InetAddress getLocalHostViaUdp() throws UnknownHostException {
		final InetSocketAddress sa = new InetSocketAddress("www.google.com", 80);

		DatagramSocket sock = null;
		try {
			sock = new DatagramSocket();
			sock.connect(sa);
			final InetAddress address = sock.getLocalAddress();
			return address;
		} catch (final SocketException e) {
			log.warn("Exception getting address", e);
			return InetAddress.getLocalHost();
		} finally {
			if (sock != null) {
				sock.close();
			}
		}
	}
}
