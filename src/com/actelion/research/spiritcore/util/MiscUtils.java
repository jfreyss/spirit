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

package com.actelion.research.spiritcore.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class MiscUtils {

	public static final String SPLIT_SEPARATORS = ",;\n\t";
	public static final String SPLIT_SEPARATORS_WITH_SPACE = SPLIT_SEPARATORS + " ";
	
	private static final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
	
	private static final Date parseDate(String s) {
		try {
			return s == null ? null : dateFormat.parse(s);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static String extractCommonPrefix(Collection<String> strings) {
		if(strings==null || strings.size()==0) return "";
		if(strings.size()==1) return strings.iterator().next();
		
		String res = null;
		for (String string : strings) {
			if(string==null || string.length()==0) continue;
			if(res==null) {
				res = string;
			} else {
				int commonLen = 0;
				while(commonLen < res.length() && commonLen<string.length() && res.charAt(commonLen)==string.charAt(commonLen)) {
					commonLen++;					
				}
				res = res.substring(0, commonLen);
			}
		}
		//Remove the final '/' characters
		if(res!=null && res.length()>0 && res.charAt(res.length()-1)=='/') {
			res = res.substring(0, res.length()-1);
		}
		
		return res;
	}
	
	public static String extractCommonSuffix(Collection<String> strings) {
		if(strings==null || strings.size()==0) return "";
		if(strings.size()==1) return strings.iterator().next();
		
		String res = null;
		for (String string : strings) {
			if(string==null || string.length()==0) continue;
			if(res==null) {
				res = string;
			} else {
				int commonLen = 0;
				while(commonLen < res.length() && commonLen<string.length() && res.charAt(res.length()-1-commonLen)==string.charAt(string.length()-1-commonLen)) {
					commonLen++;
				}
				res = res.substring(res.length()-commonLen);
			}
		}
				
		return res;
	}

	public static String extractModifier(String s) {
		String[] modifiers = new String[] {"<", "<=", ">=", ">", "="};
		s = s.trim();
		for (String modifier : modifiers) {
			if(s.startsWith(modifier)) return modifier;
		}
		return "=";
	} 
	
	public static String extractText(String s) {
		String modifier = extractModifier(s);
		if(!s.startsWith(modifier)) return s;
		return s.substring(modifier.length()).trim();
	}
	
	public static Date extractDate(String s) {
		String text = extractText(s);
		return parseDate(text);
	}
	
	public static Date addDays(Date d, int days) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.add(Calendar.DAY_OF_YEAR, days);
		return cal.getTime();
	}
	
	
	public static String removeHtml(String s) {
		if(s==null) return null;
		return s.replaceAll("<br>", "\n")
				.replaceAll("&nbsp;"," ")
				.replaceAll("\\<([^=>]*(=\\'.*?\\')?(=\\\".*?\\\")?)+>","")
//				.replaceAll("\\<.*?>","")
				.replaceAll("\t", " ")
				.replaceAll("[\r\n]+", "\n")
				.replaceAll("[ ]+", " ").trim();
	}
	public static String removeHtmlAndNewLines(String s) {
		if(s==null) s = "";
		s = removeHtml(s).replace("\n", " ").replace("\"", "");
		if(s.length()==0) s = "";
		return s;
	}
	
	public static String convertLabel(String s) {		
		if(s==null) s = "";
		s = s
				.replaceAll("<B>(.*?)\n", "<span style='font-weight:bold'>$1</span>\n")
				.replaceAll("<I>(.*?)\n", "<i>$1</i>\n")
				.replaceAll("<m>(.*?)\n", "<span style='color:880088'>$1</span>\n")
				.replaceAll("<b>(.*?)\n", "<span style='color:0000CC'>$1</span>\n")
				.replaceAll("<c>(.*?)\n", "<span style='color:004466'>$1</span>\n")
				.replaceAll("<y>(.*?)\n", "<span style='color:334400'>$1</span>\n")
				.replaceAll("<r>(.*?)\n", "<span style='color:CC0000'>$1</span>\n")
				.replaceAll("<g>(.*?)\n", "<span style='color:666666'>$1</span>\n")
				.replace("\n", " ")
				.replace("\"", "");
		if(s.length()==0) s = "-";
		return s;
	}
	
	public static String extractStartDigits(String s) {
		if(s==null) return null;
		Matcher m = Pattern.compile("^[0-9]*").matcher(s);
		if(m.find()) return m.group();
		else return "";
	}
	
	public static String flatten(Object[] strings) {
		return flatten(strings, ", ");
	}
	public static String flatten(Object[] strings, String separator ) {
		return flatten(Arrays.asList(strings), separator);
	}
	public static String flatten(Collection<?> strings) {
		return flatten(strings, ", ");
	}
	public static String flatten(Collection<?> strings, String separator ) {
		if(strings==null) return "";
		StringBuilder sb = new StringBuilder();
		for (Object s : strings) {
			if(s==null || s.toString().length()==0) continue;
			sb.append((sb.length()>0? separator: "") + s );
		}
		return sb.toString();
	}

	
	public static String[] cutText(String text, int maxLength) {
		if(text==null) return new String[0];
		
		List<String> res = new ArrayList<String>(); 
		int offset = 0;
		int minLength = Math.max(2, maxLength/3);
		while(offset<text.length()) {
			int index = Math.min(text.length()-1, offset+maxLength-1);
			for (; index >= offset + minLength; index--) {
				char c = text.charAt(index);
				if(c==' ' || c=='-') {break;}
//				if(Character.isUpperCase(c)) {break;}
			}
			if(index<=offset+minLength) index = Math.min(text.length(), offset+maxLength);
			res.add(text.substring(offset, index).trim());
			offset = index;
		}		
		return res.toArray(new String[res.size()]);
		
	}
	

	public static String unsplit(String[] strings) {
		return unsplit(strings, ", ");
	}
	public static String unsplit(String[] strings, String separator) {
		if(strings==null) return "";
		StringBuilder sb = new StringBuilder();
		for (String s : strings) {
			if(s==null || s.toString().length()==0) continue;
			if(s.indexOf(separator)>=0) {
				s = "\"" + s + "\"";
			}
			sb.append((sb.length()>0?separator:"")+ s );
		}
		return sb.toString();
	}

	public static List<Integer> splitIntegers(String s) {
		List<Integer> res = new ArrayList<Integer>();
		for(String string: split(s, SPLIT_SEPARATORS+" ")) {
			try {
				res.add(Integer.parseInt(string));
			} catch(Exception e) {
				throw new IllegalArgumentException(string+" is not an integer");
			}
		}
		return res;
	}
	
	public static List<Long> splitLong(String s) {
		List<Long> res = new ArrayList<Long>();
		for(String string: split(s, SPLIT_SEPARATORS+" ")) {
			try {
				res.add(Long.parseLong(string));
			} catch(Exception e) {
				throw new IllegalArgumentException(string+" is not an integer");
			}
		}
		return res;
	}
	
	/**
	 * Split a string, using the standard separators (,;\n\t)
	 * Unlike String.split, the string can contain the separator but they must be escaped with backslash
	 * 
	 * The opposite function is unsplit.
	 * 
	 * @param s
	 * @return
	 */
	public static String[] split(String s) {
		return split(s, SPLIT_SEPARATORS);
	}
	
	/**
	 * Split a string, using the given separators.
	 * Unlike String.split, the string can contain the separator but they must be escaped with backslash
	 * 
	 * The opposite function is unsplit.
	 * 
	 * Ex: split(" ",quoted"\,escaped\\,ok ", ",") will return those 2 elements [",quoted",escaped\],[ok].
	 * 
	 * 
	 * @param s
	 * @param separators
	 * @return
	 */
	public static String[] split(String s, String separators) {
		if(s==null) return new String[0];
		StringTokenizer st = new StringTokenizer(s, "\"" + separators, true);
		List<String> res = new ArrayList<String>();
		boolean inQuote = false;
		StringBuilder sb = new StringBuilder();
		while(st.hasMoreTokens()) {
			String token = st.nextToken();
			if(token.equals("\"")) {
				if(inQuote) {
					if(sb.toString().trim().length()>0) res.add(sb.toString().trim());
					sb.setLength(0);
					inQuote = false;
				} else {
					inQuote = true;
				}
			} else if(!inQuote && separators.indexOf(token)>=0) {
				if(sb.toString().trim().length()>0) res.add(sb.toString().trim());
				sb.setLength(0);    			
			} else {
				sb.append(token);
			}    				
		}
		if(sb.toString().trim().length()>0) res.add(sb.toString().trim());
		
		return res.toArray(new String[res.size()]);
	}
	

	/**
	 * Split a query by And / Or keywords.
	 * 
	 * @return always an odd number of items: ex: "<=5", "AND", ">2"
	 */
	public static String[] splitByOrAnd(String string){
		String s[] = split(string, " ");
		List<String> res = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length; i++) {
			if(s[i].equalsIgnoreCase("AND") || s[i].equalsIgnoreCase("OR")) {
				if(sb.length()>0 && (res.size()%2)==0) res.add(sb.toString());
				if(res.size()%2==1) res.add(s[i]);
				sb.setLength(0);
			} else {
				sb.append((sb.length()>0?" ":"") + s[i]);
			}
		}
		if(sb.length()>0 && (res.size()%2)==0) {
			res.add(sb.toString());
		} else if(res.size()%2==0 && res.size()>0) {
			res.remove(res.size()-1);
		}
		
		return res.toArray(new String[res.size()]);
	}
	
	/**
	 * Extract n items from the list, if the n>list.size, return all.
	 * Otherwise return n elements using a progressive incrementation
	 * @param biosamples
	 * @param size
	 * @return
	 */
	public static<T> List<T> subList(List<T> list, int size) {
		if(size>=list.size()) return list;
		
		List<T> res = new ArrayList<T>();
		//We choose alpha such as sum(1+(alpha*i), i, 0, n-1)) = list.size
		int alpha = -2*(size-list.size()-1)/(size*(size-1));
		int index = 0;
		for(int i=0; i<size; i++) {
			if(index>=list.size()) return res;
			res.add(list.get(index));

			index += 1 + (int) (i * alpha);
		}
		return res;		
	}

	public static String concatenate(String[][] rows, boolean removeSpecialCharacters) {
		StringBuilder sb = new StringBuilder();
		for (String[] strings : rows) {
			for (int i = 0; i < strings.length; i++) {
				if(i>0) sb.append("\t");
				String s = strings[i];
				if(s==null) s = "";
				if(removeSpecialCharacters) s = s.replaceAll("\t|\n", " ");
				sb.append(s);				
			}
			sb.append(System.getProperty("line.separator"));
		}
		
		return sb.toString();
	}
	
	public static Integer parseInt(String integer) {
		try {
			return Integer.parseInt(integer.trim());
		} catch(Exception e) {
			return null;		
		}
	}
	
	public static Double parseDouble(String doub) {
		try {
			return Double.parseDouble(doub.trim());
		} catch(Exception e) {
			return null;		
		}
	}

	/**
	 * Serializes a Map<Integer,String> like: 1->joel, 2->to;=to, 3->null to 1=joel;2=to\;=to;3=
	 * All characters must be accepted. Returned string is [[id=string][#id=string]*]
	 * @param map
	 * @return
	 */
	public static String serializeIntegerMap(Map<Integer, String> map) {
		StringBuilder sb = new StringBuilder();
		for (Entry<Integer, String> entry : map.entrySet()) {
			if(entry.getKey()<0) throw new RuntimeException("Cannot serialize: "+map);
			if(sb.length()>0) sb.append(";");
			sb.append(entry.getKey());
			sb.append("=");
			String s = entry.getValue();
			if(s!=null && s.length()>0) {
				if(s.contains("\\")) s = s.replace("\\", "\\\\");
				if(s.contains(";")) s = s.replace(";", "\\;");
				if(s.contains("\t")) s = s.replace("\t", "\\\t");
				sb.append(s);
			}
		}
		return sb.toString();
	}
	
	/**
	 * Inverse function of serializeIntegerMap
	 * @param data
	 * @return
	 */
	public static Map<Integer, String> deserializeIntegerMap(String data) {
		Map<Integer, String> map = new LinkedHashMap<>();
		if(data==null) return map;
		
		//Automat algorithm to parse data
		boolean inKey = true;
		int id = 0;
		StringBuilder value = new StringBuilder();
		for (int i = 0; i < data.length(); i++) {
			char c = data.charAt(i);
			if(inKey) {
				if(c=='=') {
					inKey = false;
				} else {
					if(c<'0' || c>'9') throw new RuntimeException("Cannot deserialize: "+data);
					id = id*10 + (c-'0');
				}
			} else {
				if(c=='\\') { //Escape character
					i++;
					if(i>=data.length()) throw new RuntimeException("Cannot deserialize: "+data);
					value.append(data.charAt(i));
				} else if(c==';' || c=='\t') {
					inKey = true;
					map.put(id, value.toString());
					id = 0; 
					value.setLength(0);
				} else {
					value.append(c);					
				}
			}
		}
		if(id>0) {
			map.put(id, value.toString());
		}
		return map;
	}
	
	
	/**
	 * Serializes a Map<Integer,String> like: 1->joel, 2->to;=to, 3->null to 1=joel;2=to\;=to;3=
	 * All characters must be accepted. Returned string is [[id=string][;id=string]*]
	 * @param map
	 * @return
	 */
	public static String serializeStringMap(Map<String, String> map) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry : map.entrySet()) {
			if(sb.length()>0) sb.append(";");
			sb.append(entry.getKey().replace("\\", "\\\\").replace(";", "\\;").replace("\t", "\\\t").replace("=", "\\="));
			sb.append("=");
			String s = entry.getValue();
			if(s!=null && s.length()>0) {
				sb.append(s.replace("\\", "\\\\").replace(";", "\\;").replace("\t", "\\\t").replace("=", "\\="));
			}
		}
		return sb.toString();
	}
	
	/**
	 * Inverse function of serializeIntegerMap
	 * @param data
	 * @return
	 */
	public static Map<String, String> deserializeStringMap(String data) {
		Map<String, String> map = new LinkedHashMap<>();
		if(data==null) return map;
		
		//Automat algorithm to parse data
		boolean inKey = true;
		StringBuilder key = new StringBuilder();
		StringBuilder value = new StringBuilder();
		for (int i = 0; i < data.length(); i++) {
			char c = data.charAt(i);
			if(inKey) {
				if(c=='\\') { //Escape character
					i++;
					if(i>=data.length()) throw new RuntimeException("Cannot deserialize: "+data);
					key.append(data.charAt(i));
				} else if(c=='=') {
					inKey = false;
				} else {
					key.append(c);
				}
			} else {
				if(c=='\\') { //Escape character
					i++;
					if(i>=data.length()) throw new RuntimeException("Cannot deserialize: "+data);
					value.append(data.charAt(i));
				} else if(c==';' || c=='\t') {
					inKey = true;
					map.put(key.toString(), value.toString());
					key.setLength(0); 
					value.setLength(0);
				} else {
					value.append(c);					
				}
			}
		}
		if(key.length()>0) {
			map.put(key.toString(), value.toString());
		}
		return map;
	}
	
	/**
	 * 1 -> 2
	 * 1. -> 2.
	 * 1C -> 1D
	 * 1C1 -> 1C2
	 * 1C9 -> 1C10
	 * A->B
	 * Z->??
	 * @param name
	 * @return
	 */
	public static String incrementName(String name) {
		String abbr = name;
		String suffix = "";		
		for (int i = abbr.length()-1; i > 0 ; i--) {
			if(!Character.isDigit(abbr.charAt(i)) && !Character.isLetter(abbr.charAt(i))) {
				suffix = abbr.charAt(i) + suffix;
			} else {
				break;
			}
		}
		abbr = abbr.substring(0, abbr.length()-suffix.length());
		if(abbr==null || abbr.length()==0) return "1" + suffix;
				
		if(abbr.length()>0 && Character.isDigit(abbr.charAt(abbr.length()-1))) {
			//increment number
			int index;
			for(index=abbr.length()-1; index>=0 && Character.isDigit(abbr.charAt(index)); index--) {}
			index++;				
									
			String number = abbr.substring(index);
			return abbr.substring(0, index) + (Integer.parseInt(number)+1) + suffix;
		} else if(abbr.length()>0 && Character.isLetter(abbr.charAt(abbr.length()-1))) {
			//increment letter
			int index;
			for(index=abbr.length()-1; index>=0 && Character.isLetter(abbr.charAt(index)); index--) {}						
			index++;				
			String letter = abbr.substring(index);
			if(letter.endsWith("Z") || letter.endsWith("z")) return abbr.substring(0, index) + "?" + suffix;			
			return abbr.substring(0, index) + letter.substring(0, letter.length()-1) + (char)(letter.charAt(letter.length()-1)+1) + suffix;
		}
		return "??";
	}
	
	/**
	 * Returns true if obj is contained within array 
	 */
	public static<T> boolean contains(T[] array, T obj) {
		for (T t : array) {
			if(obj.equals(t)) return true;
		}
		return false;
	}
		
	/**
	 * Returns true if one of the objects is contained within array 
	 */
	public static<T> boolean contains(T[] array, Collection<T> objects) {
		for(T obj: objects) {
			for (T t : array) {
				if(obj.equals(t)) return true;
			}
		}
		return false;
	}
	
	public static void removeNulls(Collection<?> collection) {
		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext();) {
			if(iterator.next()==null) iterator.remove();
		}
	}
	
	/**
	 * Some quick tests
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(convertLabel("<B>Bold\n<c>Cyan\nTest"));
		
		System.out.println(extractStartDigits("F5A78"));
		System.out.println(extractStartDigits("45F5A78"));
		System.out.println(Arrays.toString(cutText("Weighing Test", 12)));
		System.out.println(Arrays.toString(cutText("WeighingTest", 12)));
		System.out.println(Arrays.toString(cutText("Weighing-Test", 12)));
		System.out.println(Arrays.toString(cutText("WeightIncrease", 12)));
		System.out.println(Arrays.toString(cutText("FoodAndWater Intake", 12)));
		System.out.println(Arrays.toString(cutText("Longestwithoutspaces", 12)));
		
		System.out.println(Arrays.toString(splitByOrAnd("<3 and >1")));
		System.out.println(Arrays.toString(splitByOrAnd("<3 and or >1")));
		System.out.println(Arrays.toString(splitByOrAnd("<3 <2 and or >1")));

		String s = "2=two;3=third;4=special\\;;5=end";
		System.out.println();
		System.out.println(s);
		System.out.println(deserializeIntegerMap(s));
		System.out.println(serializeIntegerMap(deserializeIntegerMap(s)));
		assert s.equals(serializeIntegerMap(deserializeIntegerMap(s)));
		
		String s2 = "2=two\t3=third\t4=special\\;\t5=end";
		System.out.println();
		System.out.println(s2);
		System.out.println(deserializeIntegerMap(s2));
		System.out.println(serializeIntegerMap(deserializeIntegerMap(s2)));
		assert s.equals(serializeIntegerMap(deserializeIntegerMap(s2)));
		
		System.out.println();
		System.out.println(incrementName(""));
		System.out.println(incrementName("1"));
		System.out.println(incrementName("1."));
		System.out.println(incrementName("_9_"));
		System.out.println(incrementName("A"));
		System.out.println(incrementName("1A"));
		System.out.println(incrementName("1A1"));
		System.out.println(incrementName("1A19"));
		System.out.println(incrementName("{1d}"));
		System.out.println(incrementName("{1z}"));
		assert incrementName("").equals("1");
		assert incrementName("1").equals("2");
		assert incrementName("_9_").equals("_10_");
		assert incrementName("1.").equals("2.");
		assert incrementName("A").equals("B");
		assert incrementName("1A").equals("1B");
		assert incrementName("1A1").equals("1A2");
		assert incrementName("1A19").equals("1A20");
		assert incrementName("{1d}").equals("{1e}");

		
	}

	public static String convert2Html(String s) {
		if(s==null) return "";
		
		//Convert special chars
		s = s.replaceAll("&", "&amp;");
		s = s.replaceAll("<", "&lt;");
		s = s.replaceAll(">", "&gt;");
		
		//convert tab to tables;
		StringBuilder sb = new StringBuilder();
		boolean inTable = false;
		String[] lines = s.split("\n"); 
		for(String line: lines ) {
			if(line.indexOf('\t')<0) {
				if(inTable) {
					sb.append("</table>");
					inTable = false;
				}				
				sb.append(line+"<br>");
			} else {
				if(inTable) {
					sb.append("<tr><td>" + line.replaceAll("\t", "</td><td>") + "</td></tr>");
					
				} else {
					inTable = true;
					sb.append("<table style='border:solid 1px gray'>");
					sb.append("<tr><th>" + line.replaceAll("\t", "</th><th>") + "</th></tr>");
					
				}
			}
		}
		if(inTable) sb.append("</table>");
		
		s = sb.toString();
		return s;
	}
	
	
	
}
