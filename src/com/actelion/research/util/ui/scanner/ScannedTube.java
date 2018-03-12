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

package com.actelion.research.util.ui.scanner;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ScannedTube {

	private final int rackNo;
	private final String position;
	private final String tubeId;
	private Object object; 
	
	public ScannedTube(String tubeId) {
		this(null, tubeId);
	}	
	public ScannedTube(String position, String tubeId) {
		this(-1, position, tubeId);
	}
	public ScannedTube(int rackNo, String position, String tubeId) {
		this.rackNo = rackNo;
		this.position = position;
		this.tubeId = tubeId;
	}
	public ScannedTube(int row, int col) {
		this(-1, row, col, null);
	}
	public ScannedTube(int rackNo, int row, int col) {
		this(rackNo, row, col, null);
	}
	public ScannedTube(int rackNo, int row, int col, String tubeId) {
		this(rackNo, getPosition(row, col), tubeId);
	}

	public int getRackNo() {
		return rackNo;
	}
	
	public Object getObject() {
		return object;
	}
	
	public void setObject(Object object) {
		this.object = object;
	}
	
	public String getTubeId() {
		return tubeId;
	}
	
	public String getPosition() {
		return position;
	}
	public int getCol() {
		return getCol(position);
	}
	
	public int getRow() {
		return getRow(position);
	}
	
	/**
	 * Get the column from the position as string. Index is 0-based 
	 * (A/01 -> 0, A/02 -> 1)
	 */
	public static int getCol(String position) {
		if(position==null || position.length()<3) return -1;
		try {
			return Integer.parseInt(position.substring(position.length()-2))-1;
		} catch (Exception e) {
			return -1;
		}
	}
	
	/**
	 * Get the row from the position as string. Index is 0-based 
	 * (A/01 -> 0, A/02 -> 0)
	 */
	public static int getRow(String position) {
		if(position==null || position.length()<3) return -1;
		try {
			return position.toUpperCase().charAt(0)-'A';
		} catch (Exception e) {
			return -1;
		}		
	}
	
	public static String getPosition(int row, int col) {
		if(row<0 || col<0) return "";
		String pos = (char)('A'+row) + "/" + new DecimalFormat("00").format(col+1);
		return pos;
		
	}
	
	public static String getNormalizedPosition(String pos) {
		if(pos==null || pos.length()<2) return null;
		if(pos.indexOf("_")>=0) pos = pos.substring(pos.indexOf("_")+1);
		if(!Character.isLetter(pos.charAt(0))) return null;
		int row = pos.charAt(0)-'A';
		int col;
		try {
			col = Integer.parseInt(pos.substring(1))-1; 
		} catch (Exception e) {
			col = Integer.parseInt(pos.substring(2))-1; 
		}
		return getPosition(row, col);
	}
	public static List<String> getPositions(List<ScannedTube> pos) {
		List<String> res = new ArrayList<String>();
		for (ScannedTube p : pos) {
			res.add(p.getPosition());
		}
		return res;
	}
	

	public static final Comparator<ScannedTube> ROW_FIRST_COMPARATOR = new Comparator<ScannedTube>() {
		public int compare(ScannedTube o1, ScannedTube o2) {
			int cmpRow = o1.getRow() - o2.getRow();
			int cmpCol = o1.getCol() - o2.getCol();
			return cmpRow!=0? cmpRow: cmpCol;
		}
	};
	
	public static final Comparator<ScannedTube> COL_FIRST_COMPARATOR = new Comparator<ScannedTube>() {
		public int compare(ScannedTube o1, ScannedTube o2) {
			int cmpRow = o1.getRow() - o2.getRow();
			int cmpCol = o1.getCol() - o2.getCol();
			return cmpCol!=0? cmpCol: cmpRow;
		}
	};	
	
	
	public ScannedTube getNext(boolean toLeft, final int rows, final int cols) {
		int rackNo2 = rackNo;
		int row2 = getRow();
		int col2 = getCol();
		if(toLeft) {
			col2++;
			if(col2>=cols) {col2=0; row2++;}
			if(row2>=rows) {row2=0; rackNo2++;}
		} else {
			row2++;
			if(row2>=rows) {row2=0; col2++;}
			if(col2>=cols) {col2=0; rackNo2++;}			
		}
		return new ScannedTube(rackNo2, row2, col2);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getCol();
		result = prime * result + rackNo;
		result = prime * result + getRow();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		ScannedTube other = (ScannedTube) obj;
		if (getCol() != other.getCol()) return false;
		if (rackNo != other.rackNo) return false;
		if (getRow() != other.getRow()) return false;
		return true;
	}
	
	public static List<String> getTubeIds(Collection<ScannedTube> scannedTubes) {
		List<String> res = new ArrayList<String>();
		for (ScannedTube sc : scannedTubes) {
			if(sc.getTubeId()!=null) res.add(sc.getTubeId());
		}
		return res;
	}
	
	@Override
	public String toString() {
		return "Rack"+rackNo+"_"+getRow()+"x"+getCol()+": "+tubeId;
	}
}
