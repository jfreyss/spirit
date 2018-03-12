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

package com.actelion.research.spiritapp.ui.util.component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import com.actelion.research.util.IOUtils;

public class MTBalance {
	private final File f;
	private long position;
	private int token = 2;
	private long lastEdit;
	
	public static boolean SIMULATE = false;
	
	public MTBalance(String file, int token) throws IOException {
		this.token = token;
		
		File f = new File(file);
		if(!f.exists()) throw new IOException("Cannot find "+file);
		this.f = f;
		InputStreamReader is = new InputStreamReader(new FileInputStream(f), "UTF-16");
		String content = IOUtils.readerToString(is);
		this.position = content.length();
		lastEdit = f.lastModified();
		is.close();
	}
	
	private int count = 0;
	public synchronized List<Double> getWeights() throws IOException {
		
		if(SIMULATE) {
			if((++count)%25==0) {
				return Collections.singletonList(new Double(count%3==0? 0: 200+count)); 
			} else {
				return new ArrayList<Double>();
			}
		}
		
		List<Double> res = new ArrayList<Double>();
		
		if(f.lastModified()==lastEdit) return res;
		lastEdit = f.lastModified();
		InputStreamReader is = new InputStreamReader(new FileInputStream(f), "UTF-16");
		String content = IOUtils.readerToString(is);
		is.close();
		if(content.length()<=position) return res;
		
		String s = content.substring((int)position+1);
		long processed = position+1;

		if(s.length()>0) {
			StringTokenizer st = new StringTokenizer(s, "\n", true);
			int read = 0;
			while(st.hasMoreTokens()) {
				String line = st.nextToken();
				if(line.endsWith("\r")) line = line.substring(0, line.length()-1);
				read+=line.length();
				if(line.equals("\n")) {
					processed+=read;
					read=0;
					continue;
				}


				String[] tokens = line.split("\t");
				if(token<tokens.length && tokens[token].length()>0) {					
					try {
						double d = Double.parseDouble(tokens[token]);
						System.out.println("MTBalance: Read "+d);
						if(d>0.01) res.add(d);						
					} catch (Exception e) {
						throw new IOException("Cannot read balance file. Conversion from '"+line+"' into double");
					}
					processed+=read;
					read=0;
				}
				
			}
			position = processed;
		}
		
		
		
		return res;
	}
	
	public void close() {
		
	}
	
	public static void main(String[] args) throws Exception {
		final MTBalance mt = new MTBalance("d:/balance.txt", 0);
		new Thread() {
			@Override
			public void run() {
				try {
					while(true) {
						List<Double> ws = mt.getWeights();
						if(ws.size()>0) System.out.println("got "+ws);
						try {Thread.sleep(2000);} catch (Exception e) {}
					}
				} catch (Exception e) {
					e.printStackTrace();					
				}
			}
		}.start();
		
	}
	
	
	
}
