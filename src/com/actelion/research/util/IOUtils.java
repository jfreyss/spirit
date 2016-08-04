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

package com.actelion.research.util;

import java.io.*;


/**
 * 
 * @author freyssj
 */
public class IOUtils {

	public static void saveObject(String file, Object o) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		ObjectOutputStream s = new ObjectOutputStream(out);
		s.writeObject(o);
		out.close();
	}
	
	public static Object loadObject(String file) throws IOException, ClassNotFoundException {
		FileInputStream in = new FileInputStream(file);
		ObjectInputStream s = new ObjectInputStream(in);
		Object o = s.readObject();
		in.close();
		return o;
	}

	public static String readerToString(Reader reader) throws IOException {
		return  readerToString(reader, Integer.MAX_VALUE);
	}
	
	public static String readerToString(Reader reader, int maxSize) throws IOException {
		if(maxSize<=0) maxSize = Integer.MAX_VALUE;
		char[] buf = new char[512];
		int c;
		StringBuilder sb = new StringBuilder();
		while(sb.length()<maxSize && ( c = reader.read(buf, 0, Math.min(buf.length, maxSize-sb.length()))) > 0) {			
			sb.append(buf, 0, c);
		}
		return sb.toString();
	}		

	public static String streamToString(InputStream is) throws IOException {
		byte[] buf = new byte[512];
		int c;
		StringBuilder sb = new StringBuilder();
		while(( c = is.read(buf)) > 0) {
			sb.append(new String(buf, 0, c));
		}
		return sb.toString();
	}		
	
	public static byte[] getBytes(File f) throws IOException {
		FileInputStream is = new FileInputStream(f);
		byte[] res = new byte[(int) f.length()];
		is.read(res, 0, res.length);
		is.close();
		return res;
	}		
	
	public static void bytesToFile(byte[] bytes, File f) throws IOException {
		FileOutputStream os = new FileOutputStream(f);
		os.write(bytes);
		os.close();
	}
	
	public static void stringToFile(String s, File f) throws IOException {
		try (FileWriter os = new FileWriter(f)) {
			os.write(s);
		}
	}
	
	public static String fileToString(File f) throws IOException {
		return fileToString(f, Integer.MAX_VALUE);
	}
	
	public static String fileToString(File f, int maxSize) throws IOException {
		try (FileReader reader = new FileReader(f)) {
			return readerToString(reader, maxSize);
		}
	}
	
	/**
	 * Redirects the streams, without closing them
	 * @param is
	 * @param os
	 * @throws IOException
	 */
	public static void redirect(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[512];
		int c;
		while((c=is.read(buf))>0) {
			os.write(buf, 0, c);
		}
		is.close();
	}	

	/**
	 * Redirects the reader to the writer, without closing them
	 * @param is
	 * @param os
	 * @throws IOException
	 */
	public static void redirect(Reader is, Writer os) throws IOException {
		char[] buf = new char[512];
		int c;
		while((c=is.read(buf))>0) {
			os.write(buf, 0, c);
		}
		is.close();
	}
	
	public static void copy(File src, File dest) throws IOException {
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new BufferedInputStream(new FileInputStream(src));
			os = new BufferedOutputStream(new FileOutputStream(dest));
			redirect(is, os);
			
		} finally {
			try {if(is!=null) is.close();}catch(Exception e){}
			try {if(os!=null) os.close();}catch(Exception e){}
		}
	}

}
