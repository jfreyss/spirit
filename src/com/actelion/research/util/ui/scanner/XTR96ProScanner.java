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
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.actelion.research.util.Config;


public class XTR96ProScanner {

	public static final String SCANNER_NAME = "xtr96Pro";
	

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
	
	public static final boolean isSelected() {
		return XTR96ProScanner.SCANNER_NAME.equals(Config.getInstance("HTS").getProperty("scanner", XTR96ProScanner.SCANNER_NAME));
	}

	/**
	 * Scans a rack and returns the list of Positionable Tube (ids and positions).<br>
	 * The tubes are not loaded from the DB
	 * @param config
	 * @return
	 * @throws IOException
	 * @throws NoReadException
	 */
	public Plate scanPlate(ScannerConfiguration config) throws Exception {
		Plate res = new Plate(8, 12);
		
		//Check that we have write access on the current drive
		boolean test = new File(".").canWrite() && !new File(".").getAbsolutePath().startsWith("P:") && !new File(".").getAbsolutePath().contains("actelch02") && !new File(".").getAbsolutePath().contains("ares");
		if(!test) throw new IOException("The working directory must be somewhere where you have write access.\n Currently it is: "+new File(".").getAbsolutePath());
		
		if("baerr".equals(System.getProperty("user.name")) || "freyssj".equals(System.getProperty("user.name"))) {
			return new Plate(config.getRows(), config.getCols(), XTR96Scanner.getTestTubes(0));
		}
		
		
		//Run the Scanner
		Socket sock = null;
		OutputStream os = null;
		try {
			sock = new Socket("127.0.0.1", 200);
			os = sock.getOutputStream();
		} catch (Exception e) {
			File directory = getDirectory();
					
			if(directory==null) throw new IOException("Cannot find XTR PRO directory");
			
			Runtime.getRuntime().exec(new File(directory, "xtr-96 Pro.exe -s").getAbsolutePath());
			try {Thread.sleep(10000);} catch (Exception e2) {}
			sock = new Socket("127.0.0.1", 200);
			os = sock.getOutputStream();
		}
		
		
		

		System.out.println("Socket opened");
		
		try {Thread.sleep(200);} catch (Exception e) {}

		try{
			//Set Tube config
			{
				InputListenerThread thread = new InputListenerThread(sock);		
				thread.start();
				
				String setConfig;
				if(config==ScannerConfiguration.SCANNER_CONFIGURATION_MATRIX_PP) {
					setConfig = "set tube = Matrix"; 
				} else if(config==ScannerConfiguration.SCANNER_CONFIGURATION_MATRIX_1_0PP) {
					setConfig = "set tube = Matrix"; 
				} else if(config==ScannerConfiguration.SCANNER_CONFIGURATION_MATRIX_GLASS) {
					setConfig = "set tube = Glass96";
				} else if(config==ScannerConfiguration.SCANNER_CONFIGURATION_RACK24) {
					setConfig = "set tube = Glass24";
				} else {
					throw new IOException("Invalid config for the scanner: "+ config);
				}
				int count = 0;		
				System.out.println("send "+setConfig);
				os.write(setConfig.getBytes());
				do {
					try {Thread.sleep(500);}catch (Exception e) {}
					System.out.println("-->"+thread.lastOutput);
				} while(thread.lastOutput.indexOf("OK")<0 && count++<120); //timeout of 1min
				thread.interrupt();
			}
	

			System.out.println("tube set");
			
			try {Thread.sleep(100);} catch (Exception e) {}
			
			//Get Tubes
			boolean ok = false;
			boolean error = false;
			InputListenerThread thread = new InputListenerThread(sock);		
			{
				thread.start();
				os.write("get".getBytes());
				int count = 0;			
				do {
					try {Thread.sleep(500);}catch (Exception e) {}
					System.out.println("-->"+thread.lastOutput);
					if(thread.lastOutput.indexOf("OK")>=0) ok = true;
					if(thread.lastOutput.indexOf("Error")>=0) error = true;
					
				} while(!ok && !error && count++<120);  //timeout of 1min
				thread.interrupt();
	
			}
			String scannedTubes = thread.buffer;
			
			if(error) {
				thread = new InputListenerThread(sock);		
				{
					thread.start();
					os.write("switch".getBytes());
					int count = 0;
					boolean none = false;
					do {
						try {Thread.sleep(500);}catch (Exception e) {}
						System.out.println("-->"+thread.lastOutput);
						if(thread.lastOutput.indexOf("None")>=0) none = true;
						if(thread.lastOutput.indexOf("Error")>=0) error = true;
						
					} while(!none && !error && count++<120);  //timeout of 1min
					thread.interrupt();
	
					if(config==ScannerConfiguration.SCANNER_CONFIGURATION_RACK24) {
						if(error) {
							//a rack24 that is inverted is still OK
						} else {
							//a rack 24 with a read error and a switch ok has an unknown problem
							throw new Exception("There is an unknown problem with the scanner on the RACK24");
						}
					} else {
						if(error) {
							//a 96rack with a wrong switch
							throw new Exception("You should invert the Rack");
						} else {
							//a 96rack with an ok switch
							throw new Exception("There is an unknown problem with the scanner");
						}
					}
				}
			}
			
			try {Thread.sleep(100);}catch (Exception e) {}		
			
			try { thread.wait();} catch (Exception e) {}
	
			List<RackPos> tubes = parseResults(scannedTubes);	
			res.setTubes(tubes);
			return res;
		} finally {
			os.close();
		}
		
	}
	
	
	public List<RackPos> scanTubes(ScannerConfiguration config) throws Exception {
		return scanPlate(config).getTubes();
	}
	private static List<RackPos> parseResults(String res) throws NoReadException {
		int index = res.indexOf("...A01");
		if(index>0) res = res.substring(index+3);
		
		List<RackPos> tubes = new ArrayList<>();		
		List<RackPos> noread = new ArrayList<>();		
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
		File directory = new File("C:\\Program Files\\FluidX\\xtr-96 Pro\\");
		if(!directory.exists()) directory = new File("D:\\Program Files\\FluidX\\xtr-96 Pro\\");
		if(!directory.exists()) directory = new File("E:\\Program Files\\FluidX\\xtr-96 Pro\\");
		if(!directory.exists()) directory = null;
		return directory;
	}
	public static boolean isInstalled() {
		return getDirectory()!=null;
	}
	
}
