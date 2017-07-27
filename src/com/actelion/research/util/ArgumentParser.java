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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * Argument parsers parses the arguments sent in the main() method and returns a
 * map<String,String> of argument to value.
 * <br>
 * The aguments have to be entered such as -NAME VALUE or -NAME "LONG VALUE"
 *
 */
public class ArgumentParser {

	private final Map<String, String> map = new HashMap<>();
	private final String arguments;


	public ArgumentParser(String[] args) {
		//Concatenate arguments
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			if(i>0) sb.append(" ");
			sb.append(args[i]);
		}
		this.arguments = sb.toString();
		init();

	}

	public ArgumentParser(String args) {
		this.arguments = args;
		init();
	}

	private void init() {
		//Tokenize arguments
		List<String> newArgs = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(arguments, " \"", true);
		String buf = "";
		boolean inBrackets = false;
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if(token.equals(" ")) {
				if(inBrackets) buf += token;
			} else if(token.equals("\"")) {
				if(inBrackets) {
					if(buf.length()>0) newArgs.add(buf);
					buf = "";
					inBrackets = false;
				} else {
					inBrackets = true;
				}
			} else {
				if(inBrackets) buf += token;
				else newArgs.add(token);
			}
		}
		if(buf.length()>0) newArgs.add(buf);


		String name = null;
		String oldName = null;
		for (String s : newArgs) {
			//Is it an argument Name i.e. "-name" (-5 is not an argument name)
			boolean isArgumentName;
			if(s.startsWith("-")) {
				try {
					Double.parseDouble(s.substring(1));
					isArgumentName = false;
				} catch (NumberFormatException e) {
					isArgumentName = true;
				}
			} else {
				isArgumentName = false;
			}

			//Process the token
			if(isArgumentName) {
				if(name!=null) {
					System.err.println("Invalid argument: -"+name);
				} else {
					name = s.substring(1).toLowerCase();
					if(map.containsKey(s)) {
						System.err.println("Duplicated argument: -"+s);
					}
				}
			} else {
				if(name==null) {
					if(oldName==null) System.err.println("No argument for: -"+s);
					else map.put(oldName, map.get(oldName)+" "+s);
				} else {
					//System.out.println(name + "->"+s);
					map.put(name, s);
					oldName = name;
					name = null;
				}
			}
		}
	}

	/**
	 * Checks that all the arguments are contained in the list of allowed arguments
	 * @param allowedParametersCommaSeparated
	 * @throws Exception, if an argument is invalid
	 */
	public void validate(String allowedParametersCommaSeparated) throws Exception {
		Set<String> parameters = new TreeSet<>();
		for (String s : allowedParametersCommaSeparated.split(",")) {
			parameters.add(s.toLowerCase().trim());
		}

		for (String key : map.keySet()) {
			if(!parameters.contains(key)) {
				throw new Exception("Invalid parameter: "+key);
			}
		}
	}


	public String getArgument(String name) {
		return map.get(name.toLowerCase());
	}
	public int getArgumentInt(String name, int def) {
		try {
			return Integer.parseInt(map.get(name.toLowerCase()));
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * @return the arguments
	 */
	public String getArguments() {
		return arguments;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String key : map.keySet()) {
			sb.append(key + " -> " + map.get(key) + "\r\n");
		}
		return sb.toString();
	}

}
