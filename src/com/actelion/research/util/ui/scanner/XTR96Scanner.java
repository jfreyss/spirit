/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2016 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16,
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.actelion.research.util.Config;
import com.actelion.research.util.IOUtils;


public class XTR96Scanner {

	public static final String SCANNER_NAME = "xtr96";
	public static final boolean overwriteRegistry = false;
	

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
					try {Thread.sleep(50);}catch (Exception e) {return;}
					int read = is.read(buf);
					if(read>=0) {
						lastOutput = new String(buf, 0, read);
						buffer += lastOutput;
					}
				}
			} catch (Exception e) {
			}	
		}
		public String resetBuffer() {
			String res = buffer;
			buffer = "";
			return res;
		}
		
		
	}
	
	public XTR96Scanner() {
	}
	
	public static final boolean isSelected() {
		return XTR96Scanner.SCANNER_NAME.equals(Config.getInstance("HTS").getProperty("scanner", XTR96Scanner.SCANNER_NAME));
	}

	public List<RackPos> scanTubes(ScannerConfiguration config) throws IOException, NoReadException {
		return scanPlate(config).getTubes();
	}
	
//	private static int testNo = 0;  
//	public static void configureTest(int i) {
//		testNo = i;
//	}
	public static List<RackPos> getTestTubes(int i) throws IOException, NoReadException {
		List<RackPos> testTubes = new ArrayList<RackPos>();
		if(i==0) {
			testTubes.add(new RackPos("A/01", "GT00032531"));
			testTubes.add(new RackPos("A/02", "GT00032532"));
			testTubes.add(new RackPos("A/03", "GT00032533"));
			testTubes.add(new RackPos("A/04", "GT00032534"));
			testTubes.add(new RackPos("A/05", "GT00032535"));
			testTubes.add(new RackPos("A/06", "GT00032536"));
			testTubes.add(new RackPos("A/07", "GT00032537"));
			testTubes.add(new RackPos("B/01", "No Read"));
//			testTubes.add(new ScannedTube("A/08", "GT00032538"));
//			testTubes.add(new ScannedTube("A/09", "GT00032539"));
//			testTubes.add(new ScannedTube("A/10", "GT00032540"));
//			testTubes.add(new ScannedTube("A/11", "GT00032541"));
//			testTubes.add(new ScannedTube("A/12", "GT00032542"));
			throw new NoReadException(testTubes);
		} else if(i==1) {
			testTubes.add(new RackPos("A/01", "GT00033531"));
			testTubes.add(new RackPos("A/02", "GT00033532"));
			testTubes.add(new RackPos("A/03", "GT00033533"));
			testTubes.add(new RackPos("A/04", "GT00033534"));
			testTubes.add(new RackPos("A/05", "GT00033535"));
			testTubes.add(new RackPos("A/06", "GT00033536"));
			testTubes.add(new RackPos("A/07", "GT00033537"));
//			testTubes.add(new ScannedTube("A/08", "GT00033538"));
//			testTubes.add(new ScannedTube("A/09", "GT00033539"));
//			testTubes.add(new ScannedTube("A/10", "GT00033540"));
//			testTubes.add(new ScannedTube("A/11", "GT00033541"));
//			testTubes.add(new ScannedTube("B/01", "GT00033542"));
		} else {
			testTubes.add(new RackPos("A/01", "GT00034531"));
			testTubes.add(new RackPos("A/02", "GT00034532"));
			testTubes.add(new RackPos("A/03", "GT00034533"));
			testTubes.add(new RackPos("A/04", "GT00034534"));
			testTubes.add(new RackPos("A/05", "GT00034535"));
			testTubes.add(new RackPos("A/06", "GT00034536"));
			testTubes.add(new RackPos("A/07", "GT00034537"));
			testTubes.add(new RackPos("A/08", "GT00034538"));
			testTubes.add(new RackPos("A/09", "GT00034539"));
			testTubes.add(new RackPos("A/10", "GT00034540"));
			testTubes.add(new RackPos("B/01", "GT00034541"));
			testTubes.add(new RackPos("B/02", "GT00034542"));			
		}
		return testTubes;
	}
	
	
	/**
	 * Scans a rack and returns the list of Positionable Tube (ids and positions).<br>
	 * The tubes are not loaded from the DB
	 * @param config
	 * @return
	 * @throws IOException
	 * @throws NoReadException
	 */
	public Plate scanPlate(ScannerConfiguration config) throws IOException, NoReadException {
		
		//Check that we have write access on the current drive
		boolean test = new File(".").canWrite() && !new File(".").getAbsolutePath().startsWith("P:") && !new File(".").getAbsolutePath().contains("actelch02") && !new File(".").getAbsolutePath().contains("ares");
		if(!test) throw new IOException("The working directory must be somewhere where you have write access.\n Currently it is: "+new File(".").getAbsolutePath());
		
//		if("baerr".equals(System.getProperty("user.name")) || "freyssj".equals(System.getProperty("user.name"))) {
//			return new Plate(config.getRows(), config.getCols(), getTestTubes(testNo));
//		}
		
		if(overwriteRegistry) {
			URL url = null;
			try {
				url = XTR96Scanner.class.getResource("/resources/xtr96.reg");
				InputStream is = url.openStream();
				new File("c:/tmp").mkdirs();
				OutputStream os = new FileOutputStream("c:/tmp/xtr96.reg");
				IOUtils.redirect(is, os);
				is.close();
				os.close();
				
				Runtime.getRuntime().exec("regedit /s c:/tmp/xtr96.reg");
				
			} catch (Exception e) {
				System.err.println("Could not reset the scanner registry ("+url+")");
				e.printStackTrace();
			}
		}
		
		//Run the Scanner
		Socket sock = null;
		OutputStream os = null;
		Process p = null;
		try {
			sock = new Socket("127.0.0.1", 201);
			os = sock.getOutputStream();
		} catch (Exception e) {
			File directory = getDirectory();
			if(directory==null) throw new IOException("Could not find XTR96 installation");
			p = Runtime.getRuntime().exec(new File(directory, "xtr-96.exe").getAbsolutePath());
			//Wait until ready
			long time = System.currentTimeMillis();
			while(System.currentTimeMillis()-time<120000) {
				try {
					sock = new Socket("127.0.0.1", 201);
					os = sock.getOutputStream();
					System.out.println("LAUNCHED");
					break;
				} catch (Exception ex) {
					//Still not ready
				}
			}
		}
		if(os==null) throw new IOException("os is null");
		
		if(config.regEditConfig!=null) {
			InputListenerThread thread = new InputListenerThread(sock);		
			thread.start();
			os.write(("set tube = " + config.regEditConfig).getBytes());
			do {
				try {Thread.sleep(1000);}catch (Exception e) {}
				System.out.println("set tube-->"+thread.lastOutput);
			} while(thread.lastOutput.indexOf("OK")<0);
			System.out.println("set tube-->"+thread.lastOutput);
			thread.interrupt();
		}

		
		try {Thread.sleep(1000);}catch (Exception e) {}
		//os.write("minimise".getBytes());
		//try {Thread.sleep(100);}catch (Exception e) {}
		{
			InputListenerThread thread = new InputListenerThread(sock);		
			thread.start();
			os.write("scan only".getBytes());
			String last = "OK";
			int count = 0;
			do {
				try {Thread.sleep(1000);}catch (Exception e) {}
				System.out.println("scan only-->"+thread.lastOutput);
			} while(thread.lastOutput.indexOf(last)<0 && count++<30);
			System.out.println("scan only-->"+thread.lastOutput);
			thread.interrupt();
		}
		
		try {Thread.sleep(2000);}catch (Exception e) {}
		InputListenerThread thread = new InputListenerThread(sock);		
		thread.start();
		{
			os.write("decode".getBytes());
			String last = config.last;
			int count = 0;
			do {
				try {Thread.sleep(1000);}catch (Exception e) {}
				System.out.println("decode-->"+thread.lastOutput);
			} while(thread.lastOutput.indexOf(last)<0 && count++<30);
			System.out.println("decode-->"+thread.lastOutput);
		}
		
		os.write("terminate".getBytes());
		
		thread.interrupt();
		try { thread.wait();}catch (Exception e) {}
		System.out.println("terminate-->"+thread.lastOutput);

		try {Thread.sleep(1000);}catch (Exception e) {}
		
		if(p!=null) p.destroy();

		return new Plate(config.getRows(), config.getCols(), parseResults(thread.buffer));
		
	}
	
	private static List<RackPos> parseResults(String res) throws NoReadException {
		int index = res.indexOf("...A01");
		if(index>0) res = res.substring(index+3);
		
		List<RackPos> tubes = new ArrayList<RackPos>();		
		List<RackPos> noread = new ArrayList<RackPos>();		
		String[] s = res.split("\n");
		for(String t: s) {
			String[] v = t.split(",");
			if(v.length!=2) continue;
			
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
			} else if(!"No Tube".equals(barcode)) {
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
		if(!directory.exists()) directory = null;
		return directory;
	}
	public static boolean isInstalled() {
		return getDirectory()!=null;
	}
}

