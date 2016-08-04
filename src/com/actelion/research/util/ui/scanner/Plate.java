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

package com.actelion.research.util.ui.scanner;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A scanned Plate
 * @author freyssj
 *
 */
public class Plate implements Serializable {
	
	private int rows;
	private int cols;
	private String plateId;
	private List<RackPos> tubes = null;
	
	public Plate() {}
	
	public Plate(int rows, int cols) {
		this(rows, cols, new ArrayList<RackPos>());
	}
	
	public Plate(int rows, int cols, List<RackPos> tubes) {
		this.rows = rows;
		this.cols = cols;
		this.tubes = tubes;
	}
	
	public String getPlateId() {
		return plateId;
	}

	public void setPlateId(String plateId) {
		this.plateId = plateId;
	}

	public RackPos getRackPos(String pos) {
		if(getTubes()==null) throw new IllegalArgumentException("Positions are not loaded");
		for (RackPos pp : getTubes()) {
			if(pp.getPosition()!=null && pp.getPosition().equals(pos)) return pp;
		}
		return null;
	}
	
	public RackPos getRackPos(int row, int col) {
		String pos = (char)('A'+row) + "/" + new DecimalFormat("00").format(col+1);
		return getRackPos(pos);
		
	}
	
	
	/**
	 * Positions can be null if the positions are not loaded
	 * @return
	 */
	public List<RackPos> getTubes() {
		return tubes;
	}
	public void setTubes(List<RackPos> positions) {
		this.tubes = positions;
	}
	
	@Override
	public String toString() {
		return plateId==null?"N/A": plateId;
	}
	
	@Override
	public boolean equals(Object obj) {
 		return obj instanceof Plate && ((Plate)obj).getPlateId().equals(getPlateId());
	}
	@Override
	public int hashCode() {
		return getPlateId()==null? 0: getPlateId().hashCode();
	}
		
	public int getRows() {
		return rows;
	}
	
	public int getCols() {
		return cols;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public void setCols(int cols) {
		this.cols = cols;
	}

}
