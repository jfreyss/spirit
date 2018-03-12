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

package com.actelion.research.spiritcore.services;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class StringEncrypter {

	private final static String ENCODING1 = "UTF-8";
	private final static String ENCODING2 = "Cp1252";

	private Cipher ecipher;
	private Cipher dcipher;
	private boolean newSystem = true;



	/**
	 * Constructor used to create this object.  Responsible for setting
	 * and initializing this object's encrypter and decrypter Chipher instances
	 * given a Pass Phrase and algorithm.
	 * @param passPhrase Pass Phrase used to initialize both the encrypter and
	 *                   decrypter instances.
	 */
	public StringEncrypter(String passPhrase) {

		// 8-bytes Salt
		byte[] salt = {
				(byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32,
				(byte)0x56, (byte)0x34, (byte)0xE3, (byte)0x03
		};

		// Iteration count
		int iterationCount = 19;

		try {

			KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount);
			SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);

			ecipher = Cipher.getInstance(key.getAlgorithm());
			dcipher = Cipher.getInstance(key.getAlgorithm());

			// Prepare the parameters to the cipthers
			AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);

			ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
			dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Takes a single String as an argument and returns an Encrypted version
	 * of that String.
	 * @param str String to be encrypted
	 * @return <code>String</code> Encrypted version of the provided String
	 */
	public String encrypt(char[] str) {
		try {
			if(newSystem) {
				byte[] utf8 = new String(str).getBytes("UTF-8");
				byte[] enc = ecipher.doFinal(utf8);
				String res = byteToHex(enc);
				if(!new String(str).equals(new String(decrypt(res)))) throw new RuntimeException("decryption mismatch ");
				return res;
			} else {
				byte[] utf8 = new String(str).getBytes(ENCODING1);
				byte[] enc = ecipher.doFinal(utf8);
				String res = new String(enc, ENCODING2);
				return res;
			}



		} catch (Exception e) {
			throw new RuntimeException("Cannot encrypt", e);
		}
	}


	/**
	 * Takes a encrypted String as an argument, decrypts and returns the
	 * decrypted String.
	 * @param str Encrypted String to be decrypted
	 * @return <code>String</code> Decrypted version of the provided String
	 */
	public char[] decrypt(String str) {

		try {

			if(newSystem) {
				byte[] utf8 = dcipher.doFinal(hexToByte(str));
				return new String(utf8, "UTF-8").toCharArray();
			} else {
				// Decode base64 to get bytes
				byte[] dec = str.getBytes(ENCODING2);

				// Decrypt
				byte[] utf8 = dcipher.doFinal(dec);

				// Decode using utf-8
				return new String(utf8, ENCODING1).toCharArray();
			}

		} catch (Exception e) {
			throw new RuntimeException("Cannot decrypt ", e);
		}
	}

	public static String byteToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}

	public static byte[] hexToByte(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

}