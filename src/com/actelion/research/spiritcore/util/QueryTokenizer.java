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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class QueryTokenizer {
	/**
	 * Tokenize a string considering:
	 * - " for quoting strings (skipped)
	 * - , ; as delimiters (skipped)
	 * - (, ) as specials characters (returned) 
	 * @param query
	 * @return
	 */
	public static String[] tokenize(String query) {
		return tokenize(query, ",;");
	}
	public static String[] tokenize(String query, String delimiters) {
		boolean inQuote = false;
		List<String> res = new ArrayList<String>();
		
		StringBuilder tok = new StringBuilder();
		for(int i=0; i<query.length(); i++) {
			char c = query.charAt(i);
			if(c=='"' || c=='\'') {
				tok.append(c);
				//quote
				if(inQuote) {
					inQuote = false;
				} else {
					inQuote = true;
				}
			} else if(delimiters.indexOf(c)>=0) {
				//delimiter
				if(inQuote) {
					tok.append(c);
				} else {
					if(tok.length()>0) {
						res.add(tok.toString().trim());
						tok.setLength(0);
					}
				}				
			} else if(c=='(' || c==')') {
				//special
				if(inQuote) {
					tok.append(c);
				} else {
					if(tok.length()>0) {
						res.add(tok.toString().trim());
						tok.setLength(0);
					}
					tok.append(c);
					res.add(tok.toString().trim());
					tok.setLength(0);
				}	
			} else {
				tok.append(c);
			}			
		}
		if(tok.length()>0) {
			res.add(tok.toString().trim());
			tok.setLength(0);
		}

		
		return res.toArray(new String[res.size()]); 
	}
	
	/**
	 * Expands a sql clause containing "field like ?" or "= ?" and values "A, B" to "field like A OR field like B"
	 * This version uses OR as default and does not add wildcards.
	 * i.e. this functions calls expandQuery(,,false, false)
	 * @param sqlClause
	 * @param values
	 * @return
	 * @throws Exception
	 */
	public static String expandOrQuery(String sqlClause, String values) throws Exception {
		if(!sqlClause.contains("?")) throw new Exception("SQL should be formatted 'FIELD1 like ? [or/and FIELD2 like ?]'"); 
		return expandQuery(sqlClause, values, false, false);
	}
	
	/**
	 * Expands a sql clause containing "field like ?" or "= ?" and values "A, B" to "field like A AND field like B"
	 * This version uses AND as default and does not add wildcards.
	 * i.e. this functions calls expandQuery(,,true, false)
	 * @param sqlClause
	 * @param values
	 * @return
	 * @throws Exception
	 */
	public static String expandAndQuery(String sqlClause, String values) throws Exception {
		if(!sqlClause.contains("?")) throw new Exception("SQL should be formatted 'FIELD1 like ? [or/and FIELD2 like ?]'"); 
		return expandQuery(sqlClause, values, true, true);
	}

	
	/**
	 * Expands a sql query containing "like ?" or "= ?" and a clause like "A and B"
	 * 
	 * @param sqlClause - a sql clause containing ?, such as "field = ?" or "lower(field) like lower(?)" or any complex query like "field1 = ? or field2 = ?".
	 * @param queryString - the searched values separated by comma or semicolumn, such as "A, B" or "A or B". Spaces are not used as separator
	 * @param useAndClause - (sqlClause = 'field = ?', query = 'A, B') is transformed to (field = A AND field = B) if true or to (field = A OR field = B) if false 
	 * @param addWildcards - if wildcards '%' should automatically be added - use only if query contains 'like'
	 * @return
	 * @throws Exception
	 */
	public static String expandQuery(String sqlClause, String queryString, boolean useAndClause, boolean addWildcards) throws Exception {
		
		if(queryString.length()==0) {
			//always false
			return "1=0";
		}
		
		assert !useAndClause || !sqlClause.replaceAll(" ", "").contains("=?");
		String[] split = tokenize(queryString, ",; ");
		int pushBraces = 0;
		StringBuilder res = new StringBuilder();
		boolean expectKeyword = false;
		String defaultKeyword = useAndClause? " and ": " or ";
		for(int i=0; i<split.length; i++) {
			String tok = split[i];
			if("(".equals(tok)) {
				if(expectKeyword) res.append(defaultKeyword);
				res.append("(");
				pushBraces++;
				expectKeyword = false;
			} else if(")".equals(tok)) {
				if(!expectKeyword) throw new Exception("Missing expression in "+queryString);
				res.append(")");
				pushBraces--;
				if(pushBraces<0) throw new Exception("Unexpected ')' sign in "+queryString);
				expectKeyword = true;
			} else if(expectKeyword && ("and".equalsIgnoreCase(tok) || "or".equalsIgnoreCase(tok))) {
				res.append(" "+tok+" ");				
				expectKeyword = false;
			} else {
				
				
				if(expectKeyword) res.append(defaultKeyword);
				tok = escapeForSQL(tok.replace('*', '%'));
				if(tok.length()>1 && tok.startsWith("\"") && tok.endsWith(tok.substring(0,1))) {
					//Exact Search, never add wildcards
					tok = tok.substring(1, tok.length()-1);
				} else if(addWildcards) {
					//Add wildcards if not already present
					if(!tok.startsWith("%")) tok = "%" + tok;
					if(!tok.endsWith("%")) tok = tok + "%";					
				}
				
				//Add the expression
				res.append("(" + sqlClause.replace("?", "'" + tok + "'") + ")");
				expectKeyword = true;
			}
		}
		if(pushBraces!=0) throw new Exception("Missing ')' sign in "+sqlClause+"/"+queryString);
		if(!expectKeyword)  throw new Exception("Missing expression in "+sqlClause+"/"+queryString);

		return "("+res.toString()+")";
	}
	
	
	/**
	 * Check if the queryString "(lung left)" matches the given value (ex. "left/lung".
	 * This function is equivalent to expandQuery (with AND keywords) except that it does not expand the SQL but check directly if the given value matches the query.
	 * To support OR keywords, one need to build a tree, which is beyond the scope. 
	 * @return
	 */
	public static boolean matchQuery(String value, String queryString) {
		if(value==null) value = "";
		String[] split = tokenize(queryString, ",; ()");
		for(int i=0; i<split.length; i++) {
			String tok = split[i];
			if("and".equalsIgnoreCase(tok) || "or".equalsIgnoreCase(tok)) {
				//ignore
			} else {		
				if(tok.length()>1 && tok.startsWith("\"") && tok.endsWith(tok.substring(0,1))) {
					//Exact Search, never add wildcards
					if(!value.equals(tok)) return false;
				} else {
					if(!value.replaceAll("\\*", "").contains(tok.replaceAll("\\*", ""))) return false;
				}
			}
		}
		return true;
	}
	
	
	/**
	 * Convert a string for inclusion into SQL like queries.
	 * ie. "*object's*" is converted to "%object''s"
	 * @param tok
	 * @return
	 */
	public static String escapeForSQL(String tok) {
		tok = tok.replace('*', '%').replace("'", "''");
		return tok;
	}
	
	
	public static String[] split(String items) {
		return MiscUtils.split(items, MiscUtils.SPLIT_SEPARATORS_WITH_SPACE);
	}
	
	/**
	 * Creates a query: "[label in (1000items) or]* label in (<1000 items)"
	 * items are split using , ; tabs spaces as separators
	 * @param label
	 * @param items
	 * @return
	 */
	public static<T> String expandForIn(String label, String items) {
		return expandForIn(label, Arrays.asList(split(items)));
	}

	public static<T> String expandForIn(String label, String[] items) {
		return expandForIn(label, Arrays.asList(items));
	}

	/**
	 * Creates a query: "[label in (1000items) or]* label in (<1000 items)"
	 * @param label
	 * @param items
	 * @return
	 */
	public static<T> String expandForIn(String label, Collection<T> items) {
		if(items==null) return "0=1"; //always false
		List<T> list = new ArrayList<T>(new HashSet<T>(items));
		if(list.size()==0) return "0=1"; //always false
		
		StringBuilder sb = new StringBuilder();
		if(list.size()>1000) sb.append("(");
		
		for (int i = 0; i < list.size(); i+=1000) {
			if(i>0) sb.append(" or ");
			sb.append(label + " in ("); 
			for (int j = i; j < list.size() && j<i+1000; j++) {				
				if(j>i) sb.append(",");
				T o = list.get(j);
				if(o instanceof Number) {
					sb.append((Number)o);
				} else {
					sb.append("'" + o.toString().replace("'", "''") + "'");				
				}
			}
			sb.append(")");
		}

		if(list.size()>1000) sb.append(")");		
		return sb.toString(); 
	}

	
	public static String getHelp(boolean andField) {
		if(andField) {
			return "<ul style='margin:0px;margin-left:10px;padding:0px;font-size:8px'>"
					+ "Returns results containing ALL of the entries<br>"
					+ "(You can use query like: filter1 and (\"filter 2\" or \"filter 3\"))"
					+ "</ul>";
		} else {
			return "<ul style='margin:0px;margin-left:10px;padding:0px;font-size:8px'>"
					+ "Returns results containing ANY of the entries"
					+ "</ul>";
		}
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println("TOKENIZE="+Arrays.toString(tokenize("(\"In Split\" or outside split),   end \"no end")));
		System.out.println("TOKENIZE="+Arrays.toString(tokenize("45789 or \"joel freyss\"")));
		System.out.println("QUERY="+expandOrQuery("sampleId = ?", "45789 or \"joel freyss\""));
		System.out.println("QUERY="+expandOrQuery("keyword like ? ", "(rat wistar) or (human man or human female)"));
		System.out.println("QUERY="+expandOrQuery("keyword = ? or name = ?", "rat wistar"));
		System.out.println("QUERY="+expandOrQuery("keyword = ? or name = ?", "georges"));
		System.out.println("QUERY="+expandOrQuery("keyword like ?", "\"or\" \"and\""));
		try {
			System.out.println("QUERY="+expandOrQuery("keyword", "(rat wistar) or (human man or human female"));  //--> error
		} catch(Exception ex) {
			System.out.println("QUERY error: "+ex);
		}
		try {
			System.out.println("QUERY="+expandOrQuery("keyword", "(rat and wistar and) or (human man or human female)"));  //--> error
		} catch(Exception ex) {
			System.out.println("QUERY error: "+ex);
		}
		System.out.println("MATCH true=?"+matchQuery("lung/left", "left and lung"));
		System.out.println("MATCH false=?"+matchQuery("lung/left", "right lung"));
		
	}
}
