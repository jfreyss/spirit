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

package com.actelion.research.spiritcore.business.pivot;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.util.CompareUtils;

/**
 * Class representing a column in the PivotTable.
 * PivotColumns are sorted by testName, sampling, phase, title
 * @author freyssj
 *
 */
public class PivotColumn implements Comparable<PivotColumn> {
	private final PivotDataTable table;
	private String title;
	private Phase phase;
	private TestAttribute testAttribute;
	
	public PivotColumn(PivotDataTable table, Phase phase, TestAttribute testAttribute, String title) {
		this.table = table;		
		this.phase = phase;		
		this.title = title;
		this.testAttribute = testAttribute;

	}
	
	@Override
	public String toString() {
		return title;
	}
	
	public String getTitle() {
		return title;
	}
	
	@Override
	public int hashCode() {
		return title.hashCode();
	}
	
	@Override
	public int compareTo(PivotColumn o) {
		
		String title1 = title;
		String title2 = o.title;
		
		int ind1 = title1.indexOf("<r>");
		int ind2 = title2.indexOf("<r>");
		if(ind1>=0 && ind2>=0) {
			int c;
			
			c = title1.substring(0, ind1).compareToIgnoreCase(title2.substring(0, ind2));
			if(c!=0) return c;
			
			c = phase==null? (o.phase==null?0: 1): phase.compareTo(o.phase);

			if(c!=0) return c;
			
		} else if(ind1>=0 && ind2<0) {
			return 1;
		} else if(ind1<0 && ind2>=0) {
			return -1;
		}
		
		int c = testAttribute==null? (o.testAttribute==null?0: 1): testAttribute.compareTo(o.testAttribute);
		if(c!=0) return c;

		return CompareUtils.compare(title1, title2); //use compareutils to compare -1, +3_8, +4 correctly 
	}
	
	@Override
	public boolean equals(Object obj) {
		return title.equals(((PivotColumn)obj).title);
	}

	public PivotDataTable getTable() {
		return table;
	}
	
	/**
	 * Util function to get the results associated to this column
	 */
	public List<Result> getResults(){
		List<Result> res = new ArrayList<Result>(); 
		for(PivotRow r: table.getPivotRows()) {
			res.addAll(r.getPivotCell(this).getResults());
		}
		return res;
	}
}
