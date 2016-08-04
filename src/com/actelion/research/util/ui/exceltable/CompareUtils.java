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

package com.actelion.research.util.ui.exceltable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;




@SuppressWarnings({"unchecked", "rawtypes"})
public class CompareUtils {

	public static <T> boolean contains(T[] array, T object) {
		for (T t : array) {
			if(t==object) return true;
		}
		return false;
	}

	public static final int compare(String o1, String o2) {
		if(o1==null && o2==null) return 0; 
		if(o1==null) return 1; 
		if(o2==null) return -1;
		if(o1.equals(o2)) return 0; 
		
		
		//Compare first the doubles
		try {
			return compare(Double.parseDouble(o1), Double.parseDouble(o2));			
		} catch (Exception e) {			
		}

		String s1 = o1.replace('-', ' ').replace('.', ' ').replace(':', ' ');
		String s2 = o2.replace('-', ' ').replace('.', ' ').replace(':', ' ');
		//Compare the prefix
		try {
			int index1 = s1.indexOf(' ');
			int index2 = s2.indexOf(' ');
			if(index1>0 && index2>0 && index1<5 && index2<5) {
				int c = compare(Integer.parseInt(s1.substring(0, index1)), Integer.parseInt(s2.substring(0, index2)));
				if(c!=0) return c;

				return compare(s1.substring(index1+1), s2.substring(index2+1));
			}
		} catch (Exception e) {			
		}

		//Compare the suffix
		try {
			int index1 = s1.lastIndexOf(' ');
			int index2 = s2.lastIndexOf(' ');
			if(index1>0 && index2>0 /*&& index1<10 && index2<10*/) {
				int c = compare(s1.substring(0, index1), s2.substring(0, index2));
				if(c!=0) return c;

				return compare(Integer.parseInt(s1.substring(index1+1)), Integer.parseInt(s2.substring(index2+1)));
			}
		} catch (Exception e) {			
		}

		//Compare the strings
		return o1.toString().compareToIgnoreCase(o2.toString());
	}

	public static int compare(Comparable o1, Comparable o2) {
		if(o1==null && o2==null) return 0; 
		if(o1==null) return 1; //Null at the end
		if(o2==null) return -1;

		if((o1 instanceof String) && (o2 instanceof String)) {
			return compare((String) o1, (String) o2);
		}
		return o1.compareTo(o2);
	}
	
	public static final Comparator<Comparable> COMPARATOR = new Comparator<Comparable>() {
		
		@Override
		public int compare(Comparable o1, Comparable o2) {
			return CompareUtils.compare(o1, o2);
		}
	};
	
	public static void main(String[] args) {
		List<String> l = Arrays.asList(new String[] {"", null, "2.A", "10.B-1", "10.B-10", "10.B-2", "1.C", "21.D", "3.B-3",
		"SL002354-1","SL002354-10","SL002354-11","SL002354-2"		
		});
		Collections.sort(l, COMPARATOR);
		for (String s : l) {
			System.out.println(s);
		}
	}

}
