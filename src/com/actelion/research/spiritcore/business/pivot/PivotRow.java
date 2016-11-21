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

import java.util.HashMap;
import java.util.Map;

import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.util.CompareUtils;

public class PivotRow implements Comparable<PivotRow> {
	
	private final PivotDataTable table;
	private final Group group;
	private final Phase phase;
	private final Test test;
	private String key;
	private ResultValue rv;
	
	private final Map<PivotColumn, PivotCell> column2cell = new HashMap<PivotColumn, PivotCell>();
	
	public PivotRow(PivotDataTable table, ResultValue rv, String key) {
		PivotTemplate template = table.getTemplate(); 
		Result r = rv.getResult();
		this.table = table;
		this.group = template.getWhere(PivotItemFactory.STUDY_GROUP)==Where.ASROW? r.getGroup(): null;
		this.phase = template.getWhere(PivotItemFactory.STUDY_PHASE_DATE)==Where.ASROW? r.getInheritedPhase(): null;
		this.test = template.getWhere(PivotItemFactory.RESULT_TEST)==Where.ASROW? r.getTest(): null;
		this.rv = rv;
		this.key = key;
	}
	
	
	public String getKey() {
		return key;
	}
	
	@Override
	public int hashCode() {		
		return key.hashCode();
	}
	
	protected void addValue(PivotColumn pivotColumn, ResultValue value) {
		getPivotCell(pivotColumn).addValue(value);
	}
	
	public PivotCell getPivotCell(PivotColumn pivotColumn) {
		PivotCell cell = column2cell.get(pivotColumn);
		if(cell==null) {
			cell = new PivotCell(table);
			column2cell.put(pivotColumn, cell);
		}
		return cell;
	}

	public ResultValue getRepresentative() {
		return rv;
	}
	public Group getGroup() {
		return group;
	}	
	
	@Override
	public int compareTo(PivotRow o) {
		int c;
		c = CompareUtils.compare(group, o.group);
		if(c!=0) return c;
		
		c = CompareUtils.compare(phase, o.phase);
		if(c!=0) return c;
		
		c = CompareUtils.compare(test, o.test);
		if(c!=0) return c;
				
		c = CompareUtils.compare(key, o.key);
		if(c!=0) return c;

		return c;
	}

	@Override
	public String toString() {
		return key;
	}
}
