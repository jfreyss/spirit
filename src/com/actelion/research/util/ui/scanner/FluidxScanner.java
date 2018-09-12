/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
 * CH-4123 Allschwil, Switzerland.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author Joel Freyss
 */

package com.actelion.research.util.ui.scanner;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * Default Scanner, that should replace the former XTR96Pro and XTR96 in one
 *
 * @author freyssj
 *
 */
public class FluidxScanner {

	public static final String SCANNER_NAME = "fluidx";


	public static class InputListenerThread extends Thread {
		private Socket sock;
		public String lastOutput = "";
		public String buffer = "";
		public InputListenerThread(Socket sock) {
			this.sock = sock;
			setDaemon(true);
		}
		@Override
		public void run() {
			try {
				DataInputStream is = new DataInputStream(sock.getInputStream());
				byte[] buf = new byte[2048];
				while(!interrupted()) {
					try {Thread.sleep(50);}catch (Exception e) {e.printStackTrace(); return;}
					int read = is.read(buf);
					if(read>=0) {
						lastOutput = new String(buf, 0, read);
						buffer += lastOutput;
					}
				}
			} catch (Exception e) {
				//Stream closed
			}
		}
	}

	public FluidxScanner() {
		try {
			this.inetAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<RackPos> scanTubes(ScannerConfiguration config) throws Exception {
		return scanPlate(config).getTubes();
	}

	public static List<RackPos> getTestTubes() throws Exception {
		List<RackPos> testTubes = new ArrayList<>();
		testTubes.add(new RackPos("A/01", "0198515274"));
		//		testTubes.add(new RackPos("A/02", "1107428641"));
		//		testTubes.add(new RackPos("B/01", "1160713712"));
		//		testTubes.add(new RackPos("C/01", "1160750266"));
		return testTubes;
	}

	private String inetAddress = "127.0.0.1";

	/**
	 * Scans a rack and returns the list of Positionable Tube (ids and positions).<br>
	 * The tubes are not loaded from the DB
	 * @param config
	 * @return
	 * @throws IOException
	 * @throws NoReadException
	 */
	public Plate scanPlate(ScannerConfiguration config) throws Exception {

		//Check that we have write access on the current drive
		if(!new File(".").canWrite()) throw new IOException("The working directory must be somewhere where you have write access.\n Currently it is: "+new File(".").getAbsolutePath());

		if("true".equals(System.getProperty("simulateScanner"))) {
			return new Plate(config.getRows(), config.getCols(), getTestTubes());
		}

		//Run the Scanner
		Socket sock = null;
		OutputStream os = null;
		Process p = null;
		try {
			sock = new Socket(inetAddress, 201);
			os = sock.getOutputStream();
			System.out.println("Winsock open");
		} catch (Exception e) {
			System.out.println("Winsock closed "+e);
			File directory = getDirectory();
			if(directory==null) throw new Exception("Could not find XTR96 installation");
			p = Runtime.getRuntime().exec(new File(directory, "xtr-96.exe -s").getAbsolutePath());
			//Wait until ready
			long time = System.currentTimeMillis();
			while(System.currentTimeMillis()-time<10000) {
				try {
					sock = new Socket(inetAddress, 201);
					os = sock.getOutputStream();
					System.out.println("LAUNCHED");
					break;
				} catch (Exception ex) {
					System.err.println("wait "+(System.currentTimeMillis()-time)+"ms "+p.getOutputStream());

					//Still not ready
				}
			}
		}
		if(os==null) {
			throw new Exception("Scanner Winsock interface is not responding on " + inetAddress + " (port:201)");
		}

		if(config.regEditConfig!=null) {
			InputListenerThread thread = new InputListenerThread(sock);
			thread.start();
			os.write(("set tube = " + config.regEditConfig).getBytes());
			int count = 0;
			do {
				try {Thread.sleep(500);}catch (Exception e) {e.printStackTrace();}
				System.out.println("set tube = " + config.regEditConfig+" > "+thread.lastOutput);
			} while(thread.lastOutput.indexOf("OK")<0 && count++<180);
			System.out.println("set tube = " + config.regEditConfig+" > "+thread.lastOutput);
			thread.interrupt();
		}




		try {Thread.sleep(2000);}catch (Exception e) {e.printStackTrace();}
		final InputListenerThread thread = new InputListenerThread(sock);
		thread.start();
		{
			os.write("get".getBytes());
			String last = config.last;
			int count = 0;
			do {
				try {Thread.sleep(500);}catch (Exception e) {e.printStackTrace();}
				System.out.println("decode-->"+thread.lastOutput);
			} while(thread.lastOutput.indexOf(last)<0 && count++<180); //90seconds
			System.out.println("decode-->"+thread.lastOutput);
		}

		//Terminate in an independant thread (Fluidx needs his time, and we should not call terminate before it is really finished)
		final OutputStream tmpOs = os;
		final Process tmpProcess = p;
		new Thread() {
			@Override
			public void run() {
				try {
					try {Thread.sleep(2000);}catch (Exception e) {e.printStackTrace();}
					tmpOs.write("terminate".getBytes());

					thread.interrupt();
					try { thread.wait();}catch (Exception e) {e.printStackTrace();}
					System.out.println("terminate-->"+thread.lastOutput);

					try {Thread.sleep(10000);}catch (Exception e) {}

					if(tmpProcess!=null) tmpProcess.destroy();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
		return new Plate(config.getRows(), config.getCols(), parseResults(thread.buffer));

	}

	protected static List<RackPos> parseResults(String res) throws NoReadException {
		int index = res.indexOf("...A01");
		if(index>0) res = res.substring(index+3);

		List<RackPos> tubes = new ArrayList<RackPos>();
		List<RackPos> noread = new ArrayList<RackPos>();
		String[] s = res.split("\n");
		for(String t: s) {
			String[] v = t.split(",");
			if(v.length < 2) continue;

			String pos = v[0].trim();
			String barcode = v[1].trim();

			//normalize position to look like L/dd
			String normalPos;
			try {
				int dd = Integer.parseInt(pos.substring(1));
				normalPos = pos.substring(0, 1).toUpperCase() +  "/" + new DecimalFormat("00").format(dd);
			} catch (Exception e) {
				normalPos = "??";
			}

			if(RackPos.NOREAD.equals(barcode)) {
				noread.add(new RackPos(normalPos, barcode));
			} else if(!"No Tube".equalsIgnoreCase(barcode)) {
				tubes.add(new RackPos(normalPos, barcode));
			}
		}

		if(noread.size()>0) {
			throw new NoReadException(noread);
		}

		return tubes;
	}

	private static File getDirectory() {
		File directory = new File("E:\\Program Files\\FluidX\\xtr-96\\");
		if(!directory.exists()) directory = new File("C:\\Program Files\\FluidX\\xtr-96\\");
		if(!directory.exists()) directory = new File("C:\\Program Files (x86)\\FluidX\\xtr-96\\");
		if(!directory.exists()) directory = new File("D:\\Program Files\\FluidX\\xtr-96\\");
		if(!directory.exists()) directory = new File("C:\\Program Files\\FluidX\\Intellicode\\");
		if(!directory.exists()) directory = new File("C:\\Program Files (x86)\\FluidX\\Intellicode\\");
		if(!directory.exists()) directory = null;
		return directory;
	}
	public static boolean isInstalled() {
		return "true".equals(System.getProperty("simulateScanner")) || getDirectory()!=null;
	}

	public static void main(String[] args) throws Exception {

		try(Socket sock = new Socket(InetAddress.getLocalHost(), 201)) {
			System.out.println("FluidxScanner.main()" +  sock.getOutputStream());
		}
		//		System.out.println("FluidxScanner.main()" +  new Socket("10.180.233.249", 201).getOutputStream());
		System.out.println("FluidxScanner.main() " + new FluidxScanner().scanPlate(ScannerConfiguration.SCANNER_CONFIGURATION_MATRIX_1_0PP));
	}
}

