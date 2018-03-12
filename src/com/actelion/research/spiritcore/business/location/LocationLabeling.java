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

package com.actelion.research.spiritcore.business.location;

import java.text.DecimalFormat;

public enum LocationLabeling {
	NONE("None", Direction.LEFT_RIGHT),
	NUM("1,2,...", Direction.LEFT_RIGHT),
	NUM_I("1,2 (Vertical)", Direction.TOP_BOTTOM),
	ALPHA("A1,A2,A3,...", Direction.LEFT_RIGHT);
	
	private final String name;	
	private final Direction defaultDirection;	
	
	private LocationLabeling(String name, Direction defaultDirection) {
		this.name = name;
		this.defaultDirection = defaultDirection;
	}
	
	public String getName() {
		return name;
	}
	
	public Direction getDefaultDirection() {
		return defaultDirection;
	}
	
	@Override
	public String toString() {
		return getName();
	}


	/**
	 * Format a locpos using the appropriate formatting (A/01...A/02, 1..2, ...)
	 * @param locPos
	 * @return
	 */
	public String formatPosition(Location loc, int pos) {
		if(pos<0) return "";
		switch(this) {
		case NUM:
			return "" + (pos+1);
		case NUM_I:
			return "" + (getCol(loc, pos) * loc.getRows() + getRow(loc, pos) + 1);
		case ALPHA:
			return (""+ (char)(getRow(loc, pos)+'A')) + "/" + new DecimalFormat("00").format(getCol(loc, pos)+1);
		default:
			return "";
		}
	}
	
	public String formatPosition(Location loc, int row, int col) {
		return formatPosition(loc, getPos(loc, row, col));
	}

	
	public int getPos(Location loc, String posString) throws Exception {
		int pos;
		switch(this) {
		case NUM:
			if(posString.length()==0) throw new IllegalArgumentException("Position is required");
			try {
				pos = Integer.parseInt(posString)-1;
			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Invalid position for "+loc.getHierarchyFull()+": "+posString);
			}
			if(loc.getSize()>=0 && (pos<0 || pos>=loc.getSize())) throw new Exception("Out of Range Position for "+loc.getHierarchyFull());
			return pos;
		case NUM_I:
			
			if(posString.length()==0) throw new IllegalArgumentException("Position is required");
			try {
				int val = Integer.parseInt(posString)-1;
				int row = val % loc.getRows();
				int col = val / loc.getRows();
				pos = row * loc.getCols() + col; 
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid position for "+loc.getHierarchyFull());
			}
			
			
			if(loc.getSize()>=0 && (pos<0 || pos>=loc.getSize())) throw new Exception("Out of Range Position for "+loc.getHierarchyFull());
			return pos;
			
		case ALPHA: // could be formatted like A/1, A/01, A1, A01
			posString = posString.toUpperCase();
			if(posString.length()==0) throw new Exception("Position is required in "+loc.getHierarchyFull());
			try {
				int row = posString.charAt(0) - 'A';
				int col = posString.charAt(1)=='/'? Integer.parseInt(posString.substring(2))-1: Integer.parseInt(posString.substring(1))-1;
				if(row<0 || row>=loc.getRows()) throw new Exception("Invalid row "+row+">="+loc.getRows());
				if(col<0 || col>=loc.getCols()) throw new Exception("Invalid column");
				pos = row * loc.getCols() + col;
				return pos;
			} catch (Exception e) {
				throw new Exception("Could not parse: "+posString+"  (Required Format (" + ((char)('A'+loc.getRows()) + "/" + new DecimalFormat("00").format(1+loc.getCols())) + " for " + loc.getHierarchyFull()+"): "+e);
			}
		default:
			return -1;
		}
	}
	
	
	public int getRow(Location loc, int position) {
		if(loc!=null && loc.getCols()>0 && loc.getRows()>0) {
			return position/loc.getCols();
		} else {
			return 1;
		}
	}
	public int getCol(Location loc, int position) {
		if(loc!=null && loc.getCols()>0 && loc.getRows()>0) {
			return position%loc.getCols();
		} else {
			return 1;
		}
	}
	
	public int getPos(Location loc, int row, int col) {
		if(loc!=null && loc.getCols()>=0 && loc.getRows()>=0) {
			return row*loc.getCols()+col;
		} else {
			return -1;
		}
	}
	
	
	/**
	 * Return the next index using the appropriate direction
	 * @return
	 */
	public int getNext(Location location, int startPos, Direction dir, int n) {
		if(startPos<0 || startPos>=location.getSize()) return -1;
		switch(dir) {
		case LEFT_RIGHT:
			if(startPos+n>=location.getSize()) return -1;
			return startPos+n;
		case TOP_BOTTOM:
			int row = getRow(location, startPos);
			int col = getCol(location, startPos);
			
			col = col + (row + n) / location.getRows();			
			row = (row + n) % location.getRows();
			if(col>=location.getCols() || row>=location.getRows()) return -1;
			
			return getPos(location, row, col);
		default:
			throw new IllegalArgumentException("Invalid direction: "+dir);
		}
	}
	
	public int getNextForPattern(Location location, int startPos, int originalPosForPattern) {
		if(startPos<0 || startPos>=location.getSize()) return -1;
		int pos = startPos + originalPosForPattern;				
		if(pos>=location.getSize()) return -1;
		return pos;
		
	}


	public static LocationLabeling get(String toStringRepresentation) {
		for (LocationLabeling l : values()) {
			if(l.getName().equalsIgnoreCase(toStringRepresentation)) return l;
		}
		return null;
	}
	

}
