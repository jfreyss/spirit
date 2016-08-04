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

package com.actelion.research.spiritapp.spirit.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class HugoNaming {

	public static class Symbol {
		private final String approvedSymbol;
		private final String approvedName;
		private final List<String> synonyms;
		
		private Symbol(String approvedSymbol, String approvedName, List<String> synonyms) {
			this.approvedSymbol = approvedSymbol;
			this.approvedName = approvedName;
			this.synonyms = synonyms;
		}
		
		public String getApprovedName() {
			return approvedName;
		}
		public String getApprovedSymbol() {
			return approvedSymbol;
		}
		public List<String> getSynonyms() {
			return synonyms;
		}
		@Override
		public String toString() {
			return getApprovedSymbol() + " (" +getApprovedName() + ")";
		}
	}
	
	private static String FILE = "\\\\actelch02\\PGM\\ActelionResearch\\Spirit\\bin\\hugo.txt";
	
	private List<Symbol> symbols = null;
	
	private static HugoNaming _instance = null;
	
	public static HugoNaming getInstance() throws Exception {
		if(_instance==null) {
			_instance = new HugoNaming();
		}
		return _instance;		
	}
	
	public HugoNaming() throws Exception  {
		//Open file
		LineNumberReader reader = new LineNumberReader(new BufferedReader(new FileReader(new File(FILE))));
		try {
			symbols = new ArrayList<HugoNaming.Symbol>();
			//Skip headers
			reader.readLine();
			
			String line;
			while((line=reader.readLine())!=null) {
				String[] tokens = line.split("\t");
				if(!"Approved".equals(tokens[3])) continue;
				String symb = tokens[1];
				String name = tokens[2];
				String previous = tokens[4];
				String syn = tokens[5];
				
				List<String> synonyms = new ArrayList<String>();
				StringTokenizer st1 = new StringTokenizer(previous, ", ");
				while(st1.hasMoreTokens()) synonyms.add(st1.nextToken());
				StringTokenizer st2 = new StringTokenizer(syn, ", ");
				while(st2.hasMoreTokens()) synonyms.add(st2.nextToken());
				
				Symbol symbol = new Symbol(symb, name.toLowerCase(), synonyms);
				symbols.add(symbol);				
			}
			System.out.println("HugoNaming read: "+symbols.size()+" symbols");
		} finally {		
			reader.close();
		}
	}
	
	
	public Symbol findSymbol(String name) {
		for (Symbol symbol : symbols) {
			if(symbol.getApprovedSymbol().equalsIgnoreCase(name)) return symbol;
			for (String syn : symbol.getSynonyms()) {
				if(syn.equalsIgnoreCase(name)) return symbol;				
			}
		}
		name = name.toLowerCase();
		for (Symbol symbol : symbols) {
			if(symbol.getApprovedName().contains(name)) return symbol;
		}
		return null;
	}
	
	public String suggestName(String name) {
		String prefix = name;
		String suffix = "";
		for (char delim : "-_ ".toCharArray()) {
			if(name.indexOf(delim)>0) {
				prefix = name.substring(0, name.indexOf(delim));
				suffix = name.substring(name.indexOf(delim)+1);
				break;
			}
		}
		Symbol symbol = findSymbol(prefix);
		if(symbol==null) {
			return "";
		} else {
			String formatted =  symbol.getApprovedSymbol() + (suffix.length()>0? "-" + suffix:"");
			return formatted;
		}
		
	}
	
	
	
	public static void main(String[] args) throws Exception {
		HugoNaming naming = getInstance();
		System.out.println("MAP3K12->"+naming.suggestName("MAP3K12"));
		System.out.println("C20orf174->"+naming.suggestName("C20orf174"));
		System.out.println("ZNF780b->"+naming.suggestName("ZNF780B"));
		
		
	}
}
