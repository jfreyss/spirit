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

package com.actelion.research.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * Utility methods on IO
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
		char[] buf = new char[512];
		int c;
		StringBuilder sb = new StringBuilder();
		while(sb.length()<maxSize && ( c = reader.read(buf, 0, Math.min(buf.length, maxSize-sb.length()))) > 0) {
			sb.append(buf, 0, c);
		}
		String s = sb.toString();

		//Remove BOM
		if(s.startsWith("\uFEFF")) s = s.substring(1);
		return s;
	}

	/**
	 * Converts the stream to a string, without the BOM (unicode start of file)
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static String streamToString(InputStream is) throws IOException {
		String s = streamToString(is, "UTF-8");

		//Remove BOM
		if(s.startsWith("\uFEFF")) s = s.substring(1);
		return s;
	}

	public static String streamToString(InputStream is, String encoding) throws IOException {
		byte[] buf = new byte[512];
		int c;
		StringBuilder sb = new StringBuilder();
		while(( c = is.read(buf)) > 0) {
			sb.append(new String(buf, 0, c, encoding));
		}
		return sb.toString();
	}

	public static byte[] getBytes(File f) throws IOException {
		FileInputStream is = new FileInputStream(f);
		byte[] res = new byte[(int) f.length()];
		is.read(res);
		is.close();
		return res;
	}

	public static byte[] getBytes(InputStream is) throws IOException {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			redirect(is, os);
			return os.toByteArray();
		}
	}

	public static void bytesToFile(byte[] bytes, File f) throws IOException {
		try(ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
			try(FileOutputStream os = new FileOutputStream(f)) {
				redirect(is, os);
			}
		}
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
		try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"))) {
			return readerToString(reader, maxSize);
		}
	}


	public static void redirect(byte[] bytes, OutputStream os) throws IOException {
		try(ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
			redirect(is, os);
		}
	}

	public static void redirect(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[512];
		int c;
		while((c=is.read(buf))>0) {
			os.write(buf, 0, c);
		}
	}

	public static void redirect(Reader is, Writer os) throws IOException {
		char[] buf = new char[512];
		int c;
		while((c=is.read(buf))>0) {
			os.write(buf, 0, c);
		}
	}

	public static byte[] serialize(Object o) throws IOException {
		try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ObjectOutputStream s = new ObjectOutputStream(out);
			s.writeObject(o);
			return out.toByteArray();
		}
	}

	public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		try(ByteArrayInputStream out = new ByteArrayInputStream(bytes)) {
			ObjectInputStream s = new ObjectInputStream(out);
			return s.readObject();
		}
	}

	public static void copy(File src, File dest) throws IOException {
		try(InputStream is = new FileInputStream(src); OutputStream os = new FileOutputStream(dest)) {
			redirect(is, os);
		}
	}

	/**
	 * Creates File in the temporary folder
	 * @param prefix
	 * @param suffix
	 * @return
	 */
	public static File createTempFile(String prefix, String suffix) {
		if (suffix == null) suffix = ".tmp";

		File tmpdir = new File(System.getProperty("java.io.tmpdir"));
		if(!tmpdir.exists()) tmpdir.mkdirs();
		for(int i=0; ; i++) {
			File file = new File(tmpdir, prefix + FormatterUtils.formatYYYYMMDD() + (i==0?"": "_" + i) + suffix);
			if(!file.exists()) return file;

		}
	}

}

