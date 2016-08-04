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

package com.actelion.research.spiritapp.spirit.ui.util.correction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
/**
 * Example of use:
 * <pre>
 * 		CorrectionMap<BiotypeMetadata, Biosample> correctionMap = new CorrectionMap<BiotypeMetadata, Biosample>();
 * 		Set<String> possibleValues = new HashSet<String>(SpiritExcelTable.getAutoCompletionFields(att));
 *		Correction<BiotypeMetadata, Biosample> correction = correctionMap.getCorrection(att, value);
 *		if(correction==null) {
 *			correction = correctionMap.addCorrection(att, value, possibleValues);
 *		}
 *		correction.getAffectedData().add(biosample);
 *		
 *		//Display Correction Dlg
 *		if(correctionMap.size()>0) {
 *			CorrectionDlg<BiotypeMetadata, Biosample> dlg = new CorrectionDlg<BiotypeMetadata, Biosample>(this, correctionMap, !validateOnly) {
 *				@Override
 *				public String getSuperCategory(BiotypeMetadata att) {
 *					return att.getBiotype().getName() + " - " + att.getName();
 *				}
 *				@Override
 *				protected String getName(BiotypeMetadata att) {
 *					return att.getName();
 *				}
 *				@Override
 *				protected List<String> getAutoCompletionFieldsFor(BiotypeMetadata att) {
 *					return SpiritExcelTable.getAutoCompletionFields(att);
 *				}
 *				@Override
 *				protected void performCorrection(Correction<BiotypeMetadata, Biosample> correction, String newValue) {
 *					for (Biosample b : correction.getAffectedData()) {
 *						b.getMetadata(correction.getAttribute()).setValue(newValue);							
 *					}						
 *				}
 *			};
 *			if(dlg.getReturnCode()!=CorrectionDlg.OK) return;
 *		}
 * </pre>
 * 
 * @author freyssj
 *
 * @param <ATTRIBUTE>
 * @param <DATA>
 */
public class CorrectionMap<ATTRIBUTE, DATA> extends TreeMap<ATTRIBUTE, List<Correction<ATTRIBUTE, DATA>>> {
	
	
	public int getItemsWithSuggestions() {
		int n = 0;
		for (ATTRIBUTE key : keySet()) {
			List<Correction<ATTRIBUTE, DATA>> l = get(key);
			for (Correction<ATTRIBUTE, DATA> correction : l) {
				if(correction.isMustBeChanged() || correction.getSuggestedValue()!=null) n++; 				
			}
		}
		return n;
	}
	
	public Correction<ATTRIBUTE, DATA> getCorrection(ATTRIBUTE att, String value) {
		List<Correction<ATTRIBUTE, DATA>> l = get(att);
		if(l==null) return null;
		for (Correction<ATTRIBUTE, DATA> correction : l) {
			if(correction.getValue().equals(value)) return correction;
		}
		return null;		
	}

	public Correction<ATTRIBUTE, DATA> addCorrection(ATTRIBUTE att, String value, String suggestedValue, boolean mustBeChanged) {
		return addCorrection(att, value, Collections.singletonList(suggestedValue), mustBeChanged);
	}
	public Correction<ATTRIBUTE, DATA> addCorrection(ATTRIBUTE att, String value, List<String> suggestedValues, boolean mustBeChanged) {
		String bestValue = null;
		float bestScore = 0.4f;
		for (String suggestedValue : suggestedValues) {
			float score = getScore(value, suggestedValue);
			if(score>bestScore) {
				bestValue = suggestedValue;
				bestScore = score;
			}
		}
		Correction<ATTRIBUTE, DATA> correction = getCorrection(att, value);
		if(correction==null) {			
			List<Correction<ATTRIBUTE, DATA>> list = get(att);
			if(list==null) {
				list = new ArrayList<Correction<ATTRIBUTE, DATA>>();
				put(att, list);
			}

			correction = new Correction<ATTRIBUTE, DATA>(att, value, suggestedValues, bestValue, bestValue!=null? bestScore: 0, mustBeChanged);
			list.add(correction);
			
			
		}
		return correction;
	}
	
	public static float getScore(String string1, String string2) {
		string1 = string1.replaceFirst("\\s|\\.|\\-", "");
		string2 = string2.replaceFirst("\\s|\\.|\\-", "");
		
		if(string1.length()==0 || string2.length()==0) return 0f;
		float score = 0;

		int m = getModifiedLevenshteinDistance(string1, string2);
		score = Math.max(0, 1 - (m / (10f*Math.max(string1.length(), string2.length()))));
		
		System.out.println("Score>>> "+ string1+"-"+string2+" > "+m+" > "+score);
		return score;
	}
	
	
	
	/**
	   * <p>Find the Levenshtein distance between two Strings.</p>
	   *
	   * <p>This is the number of changes needed to change one String into
	   * another, where each change is a single character modification (deletion,
	   * insertion or substitution).</p>
	   *
	   * <p>The previous implementation of the Levenshtein distance algorithm
	   * was from <a href="http://www.merriampark.com/ld.htm">http://www.merriampark.com/ld.htm</a></p>
	   *
	   * <p>Chas Emerick has written an implementation in Java, which avoids an OutOfMemoryError
	   * which can occur when my Java implementation is used with very large strings.<br>
	   * This implementation of the Levenshtein distance algorithm
	   * is from <a href="http://www.merriampark.com/ldjava.htm">http://www.merriampark.com/ldjava.htm</a></p>
	   *
	   * <pre>
	   * StringUtils.getLevenshteinDistance(null, *)             = IllegalArgumentException
	   * StringUtils.getLevenshteinDistance(*, null)             = IllegalArgumentException
	   * StringUtils.getLevenshteinDistance("","")               = 0
	   * StringUtils.getLevenshteinDistance("","a")              = 1
	   * StringUtils.getLevenshteinDistance("aaapppp", "")       = 7
	   * StringUtils.getLevenshteinDistance("frog", "fog")       = 1
	   * StringUtils.getLevenshteinDistance("fly", "ant")        = 3
	   * StringUtils.getLevenshteinDistance("elephant", "hippo") = 7
	   * StringUtils.getLevenshteinDistance("hippo", "elephant") = 7
	   * StringUtils.getLevenshteinDistance("hippo", "zzzzzzzz") = 8
	   * StringUtils.getLevenshteinDistance("hello", "hallo")    = 1
	   * </pre>
	   *
	   * @param s  the first String, must not be null
	   * @param t  the second String, must not be null
	   * @return result distance
	   * @throws IllegalArgumentException if either String input <code>null</code>
	   */
	  public static int getModifiedLevenshteinDistance(String s, String t) {
	      if (s == null || t == null) {
	          throw new IllegalArgumentException("Strings must not be null");
	      }

	      /*
	         The difference between this impl. and the previous is that, rather 
	         than creating and retaining a matrix of size s.length()+1 by t.length()+1, 
	         we maintain two single-dimensional arrays of length s.length()+1.  The first, d,
	         is the 'current working' distance array that maintains the newest distance cost
	         counts as we iterate through the characters of String s.  Each time we increment
	         the index of String t we are comparing, d is copied to p, the second int[].  Doing so
	         allows us to retain the previous cost counts as required by the algorithm (taking 
	         the minimum of the cost count to the left, up one, and diagonally up and to the left
	         of the current cost count being calculated).  (Note that the arrays aren't really 
	         copied anymore, just switched...this is clearly much better than cloning an array 
	         or doing a System.arraycopy() each time  through the outer loop.)

	         Effectively, the difference between the two implementations is this one does not 
	         cause an out of memory condition when calculating the LD over two very large strings.
	       */

	      int n = s.length(); // length of s
	      int m = t.length(); // length of t

	      if (n == 0) {
	          return m;
	      } else if (m == 0) {
	          return n;
	      }

	      if (n > m) {
	          // swap the input strings to consume less memory
	          String tmp = s;
	          s = t;
	          t = tmp;
	          n = m;
	          m = t.length();
	      }

	      int p[] = new int[n+1]; //'previous' cost array, horizontally
	      int d[] = new int[n+1]; // cost array, horizontally
	      int _d[]; //placeholder to assist in swapping p and d

	      // indexes into strings s and t
	      int i; // iterates through s
	      int j; // iterates through t

	      char t_j; // jth character of t

	      int cost; // cost

	      for (i = 0; i<=n; i++) {
	    	  p[i] = i*10;
	      }
	      for (j = 1; j<=m; j++) {
	          t_j = t.charAt(j-1);
	          d[0] = j*10;

	          for (i=1; i<=n; i++) {
	              cost = s.charAt(i-1)==t_j ? 0 : 
	            	  Character.toUpperCase(s.charAt(i-1))==Character.toUpperCase(t_j)? 1 :
	            		  10;
	              // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
	              d[i] = Math.min(Math.min(d[i-1]+10, p[i]+10),  p[i-1]+cost);
	          }

	          // copy current distance counts to 'previous row' distance counts
	          _d = p;
	          p = d;
	          d = _d;
	      }

	      // our last action in the above loop was to switch d and p, so p now 
	      // actually has the most recent cost counts
	      return p[n];
	  }


	  
	  public static int countOk = 0;
	  public static int countNOk = 0;
	  public static void test(String v1, String v2, String res) {
		  double score = getScore(v1, v2);
		  
		  boolean ok;
		  if(res.startsWith(">")) {
			  ok = score>=Double.parseDouble(res.substring(1));
		  } else if(res.startsWith("<")) {
			  ok = score<=Double.parseDouble(res.substring(1));
		  } else {
			  throw new IllegalArgumentException(res);
		  }
		  if(ok) countOk++;
		  else {
			  countNOk++;
			  System.out.println(v1+"-"+v2+">"+score);
		  }
	  }
	  
	  public static void main(String[] args) {
			test("Norm.Dist", "Norm. Dist", ">.95");
			test("LEPTIN", "LEPTIN", ">.95");
			test("LEPTIN", "z [kp]", "<.5");
			test("Leptin", "LEPTIN", ">.85");
			test("Leptin", "Leptin [mg/ml]", ">.85");
			test("LEPTIN", "Leptin [mg/ml]", ">.8");
			test("L.Lung", "R.Heart", "<.5");
			test("heart", "Earth", ">.5");
			test("il1", "IL1 + abcdefgh", ">.5");
			test("abcdefgh", "IL1 + abcdefgh", ">.5");
			test("il1", "IL- 1a", ">.5");
			test("short", "long text that should not match", "<.5");
			test("short", "short text that could match", ">.3");
			test("short", "short text", ">.5");
			test("short", "ccr6", "<.4");
			System.out.println("Sucess Rate: " + (countOk+"/"+(countNOk+countOk)));
		}
	
}
