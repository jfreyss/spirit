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

import java.text.*;
import java.util.*;

/**
 * 
 * @author freyssj
 */
public class StringUtils {

	/**
	 * Adds Spaces to the String so that none of the words are longer than n characters
	 * @param s
	 * @param size
	 * @return
	 */
	public static final String addSpaces(String s, int n) {
		StringBuffer res = new StringBuffer();
		int count = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if(Character.isSpaceChar(c)) {
				count = 0;
			} else {
				if(count++>n) {
					count = 0;
					res.append(' ');
				} 
			}
			res.append(c);
		}		
		return res.toString();
	}
	
	
	public static final int[] intervalToInt(String s) {
		
		if(s.length()<1) throw new IllegalArgumentException("The interval cannot be empty");
		
		List<Integer> res = new ArrayList<Integer>();
		int ref = -1;
		int state = 0;
		int j;
		char symbol = ' ';
		for (int i = 0; i < s.length(); ) {
			if(s.charAt(i)==' ') {i++;continue;}
			switch (state) {
			case 0: //read number
				if(!Character.isDigit(s.charAt(i))) throw new IllegalArgumentException("Invalid character: "+s.charAt(i));
				for (j = i+1; j < s.length() && Character.isDigit(s.charAt(j)); j++) {}			
				int num = Integer.parseInt(s.substring(i, j));
				i = j;
				state = 1;
				
				switch (symbol) {
				case ' ':
					break;
				case '-':
					if(num<=ref) throw new IllegalArgumentException("Invalid expression "+ref+symbol+num);
					for (int k = ref+1; k < num; k++) {
						res.add(k);											
					}
					break;
				case ',': case ';':
					if(num<ref) throw new IllegalArgumentException("Invalid expression "+ref+symbol+num);
					break;
				default:
					throw new IllegalArgumentException("Invalid character '"+symbol+"'");
				}
				res.add(num);					
				ref = num;
				
				
				break;
			case 1: //read symbol
				if(Character.isDigit(s.charAt(i))) throw new IllegalArgumentException("Invalid symbol: "+s.charAt(i));
				symbol = s.charAt(i);
				state = 0;
				i++;
				break;			
			}
			
		}
		int[] r = new int[res.size()];
		for (int i = 0; i < res.size(); i++) {
			r[i] = res.get(i);
		}
		return r;
		
		
	}
	
	public static final String convertToCSV(String[] s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length; i++) {
			if(i>0) sb.append(", ");
			s[i] = s[i].trim().replace("\"", "\"\"");
			if(s[i].indexOf(",")>=0) s[i] = '"' + s[i] + '"';
			sb.append(s[i].trim());
		}
		return sb.toString();
	}
	
	public static final String convertToCSV(int[] s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length; i++) {
			if(i>0) sb.append(", ");
			sb.append(s[i]);
		}
		return sb.toString();
	}
	
	public static final String[] convertCSV(String s) {
		if(s==null) return new String[]{};
		StringTokenizer st = new StringTokenizer(s, ",\"", true);
		int state = 0;
		List<String> values = new ArrayList<String>();
		String value = "";
		String lastToken = null;
		while(st.hasMoreTokens()) {
			String token = st.nextToken();
			if(state==0) {
				if(token.equals(",")) {
					values.add(value.trim());
					value = "";
				} else if(token.equals("\"")) {
					if("\"".equals(lastToken)) {value+="\"";state=1;}
					else state = 1;
				} else {
					value += token;
				}
			} else { //state==1
				if(token.equals(",")) {
					value += token;
				} else if(token.equals("\"")) {
					if("\"".equals(lastToken)) {value+="\"";state=0;}
					else state = 0;
				} else {
					value += token;
				}
			}
			lastToken = token;
		}
		if(lastToken!=null) values.add(value.trim());
		return values.toArray(new String[0]);
	}

	  /**
	  * Replace characters having special meaning <em>inside</em> HTML tags
	  * with their escaped equivalents, using character entities such as <tt>'&amp;'</tt>.
	  *
	  * <P>The escaped characters are :
	  * <ul>
	  * <li> <
	  * <li> >
	  * <li> "
	  * <li> '
	  * <li> \
	  * <li> &
	  * </ul>
	  *
	  * <P>This method ensures that arbitrary text appearing inside a tag does not "confuse"
	  * the tag. For example, <tt>HREF='Blah.do?Page=1&Sort=ASC'</tt>
	  * does not comply with strict HTML because of the ampersand, and should be changed to
	  * <tt>HREF='Blah.do?Page=1&amp;Sort=ASC'</tt>. This is commonly seen in building
	  * query strings. (In JSTL, the c:url tag performs this task automatically.)
	  */
	  public static final String forHTMLTag(String aTagFragment){
	    final StringBuffer result = new StringBuffer();

	    final StringCharacterIterator iterator = new StringCharacterIterator(aTagFragment);
	    char character =  iterator.current();
	    while (character != CharacterIterator.DONE ){
	      if (character == '<') {
	        result.append("&lt;");
	      }
	      else if (character == '>') {
	        result.append("&gt;");
	      }
	      else if (character == '\"') {
	        result.append("&quot;");
	      }
	      else if (character == '\'') {
	        result.append("&#039;");
	      }
	      else if (character == '\\') {
	         result.append("&#092;");
	      }
	      else if (character == '&') {
	         result.append("&amp;");
	      }
	      else {
	        //the char is not a special one
	        //add it to the result as is
	        result.append(character);
	      }
	      character = iterator.next();
	    }
	    return result.toString();
	  }

	  /**
	   * Escape sensitive characters (&<>") by converting them to &amp; &lt; &gt; &quot;
	   * @param text
	   * @return
	   */
	  public static final String convertForUrl(String text){
		  text = text.replace("&", "&amp;");
		  text = text.replace("<", "&lt;");
		  text = text.replace(">", "&gt;");
		  text = text.replace("\"", "&quot;");
		  
		  return text;
	  }
	  /**
	   * Opposite of escape function s = unescape(escape(s))
	   * @param text
	   * @return
	   */
	  public static final String unconvertForUrl(String text){
		  text = text.replace("&lt;", "<");
		  text = text.replace("&gt;", ">");
		  text = text.replace("&quot;", "\"");		  
		  text = text.replace("&amp;", "&");
		  return text;
	  }
	  
	  public static final String nvl(String s) {
		  return s!=null?s:"";
	  }
	  
	  public static final int atoi(String s) {
		  try {
			  return Integer.parseInt(s);
		  } catch (Exception e) {
			  return -1;
		  }		  
	  }
	  public static final int[] atoi(String[] s) {
		  int[] res = new int[s.length]; 
		  try {
			  for (int i = 0; i < res.length; i++) {
				  res[i] = Integer.parseInt(s[i]);
			  }
			  return res;
		  } catch (Exception e) {
			  return null;
		  }		  
	  }
	  
	  
	  public static String skipHTML(String s) {
		  boolean inTags = false;
		  boolean inSimpleQuote = false;
		  boolean inDoubleQuote = false;
		  StringBuilder sb = new StringBuilder();
		  for (int i = 0; i < s.length(); i++) {
			  if(s.charAt(i)=='<') {
				  inTags = true;
			  } else if(s.charAt(i)=='>' && !inSimpleQuote && !inDoubleQuote) {
				  inTags = false;
			  } else if(s.charAt(i)=='\"' && inTags) {
				  inDoubleQuote = !inDoubleQuote;
			  } else if(s.charAt(i)=='\'' && inTags) {
				  inSimpleQuote = !inSimpleQuote;
			  } else if(!inTags) {
				  sb.append(s.charAt(i));
			  }
			
		  }
		  
		  return sb.toString();		  
	  }
}
