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

package com.actelion.research.spiritcore.services;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import com.actelion.research.spiritcore.adapter.DBAdapter;


public class GenerateDBPassword {

	public static void main(String[] args) throws Exception {
		String res = JOptionPane.showInputDialog(null, "Enter the password to encrypt: ", "Password Encryption", JOptionPane.QUESTION_MESSAGE);
		if(res==null) return;
		
		StringEncrypter encrypter = new StringEncrypter("program from joel");
		String p = encrypter.encrypt(res.trim().toCharArray());
		
		System.out.println("The encrypted password is:  "+p);
		
		JTextArea ta = new JTextArea(p);
		ta.setEditable(false);
		ta.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JOptionPane.showMessageDialog(null, ta, "The encrypted password is:", JOptionPane.INFORMATION_MESSAGE);
	}
	
	public static String convertToNew(String s) {
		return new StringEncrypter(DBAdapter.KEY).encrypt(new StringEncrypter(DBAdapter.KEY, false).decrypt(s));
	}
}
