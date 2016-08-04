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

import java.util.*;

/**
 * 
 * @author freyssj
 */
public class PriorityQueue<OBJECT> {
	
	private List<Elt> list = new LinkedList<Elt>();
	
	public class Elt {
		public Elt(OBJECT obj, double val) {
			this.obj = obj;
			this.val = val;
		}
		public final OBJECT obj;
		public final double val;
		
		@Override
		public String toString() {
			return obj+">"+val;
		}
	}
	
	/**
	 * Adds an element to the queue. If 2 elements have the same val, the 
	 * index is of the new element is undetermined 
	 * Complexity O(ln n) where n=size of the queue
	 * 
	 * @param elt
	 */
	public void add(Elt elt) {
		//Dichotomy to find the best index
		int begin = 0;
		int end = list.size();
		while(end>begin+1) {
			int med = (begin+end)/2;
			Elt e = list.get(med);
			if(e.val<elt.val) {
				begin = med;
			} else {
				end = med;
			}
		}
		//insert at begin or begin+1
		if(begin<list.size() && list.get(begin).val>elt.val) list.add(begin, elt);
		else if(begin+1<list.size()) list.add(begin+1, elt);
		else list.add(elt);
	}
	public void add(OBJECT obj, double val) {
		add(new Elt(obj, val));
	}
	
	public Elt popElt() {
		return size()>0? list.remove(0): null;
	}
	public Elt getElt(int i) {
		return list.get(i);
	}

	public OBJECT pop() {
		return popElt().obj;
	}	
	public OBJECT get(int i) {
		return getElt(i).obj;
	}
	
	public OBJECT remove(int i) {
		return list.remove(i).obj;
	}
	
	
	public int size() {
		return list.size();
	}
	
	public void trimTo(int size) {
		while(list.size()>size) {
			list.remove(list.size()-1);			
		}
	}
	
	public List<Elt> getList() {
		return list;
	}
}
