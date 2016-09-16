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

package com.actelion.research.spiritcore.business.pivot.datawarrior;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.spiritcore.business.pivot.FlatPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotDataTable;
import com.actelion.research.spiritcore.business.pivot.PivotItem;
import com.actelion.research.spiritcore.business.pivot.PivotItemClassifier;
import com.actelion.research.spiritcore.business.pivot.PivotItemFactory;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.services.SpiritUser;

/**
 * This enumeration enumerates the 5 big types of data that could be configured in DW
 * The biosample is always referenced at the row level and does not need to be shown here
 * @author freyssj
 *
 */
public enum PivotDataType {
	GROUP,
	PHASE,
	TOPSAMPLE,
	BIOTYPE,
	INPUT;
	
	
	public static Set<PivotDataType> getValues(List<Result> results, SpiritUser user) {
		Set<PivotDataType> res = new TreeSet<PivotDataType>();
		try {
			PivotTemplate tpl = new FlatPivotTemplate();			
			tpl.init(results);
			tpl.removeBlindItems(results, user);
			tpl.simplify(results);
			
			PivotDataTable table = new PivotDataTable(results, null, tpl);
			for(PivotDataType p: values()) {
				List<String> columns = p.getColumnNames(table);
				if(columns.size()>0) res.add(p);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}		
		return res;
	}
	
	/**
	 * Returns the first column name of the pivottable, which matches this type 
	 * @param table
	 * @return
	 */
	public String getColumnName(PivotDataTable table) {
		List<String> res = getColumnNames(table);
		if(res.size()>0) return res.get(0);
		return null;			
	}
	
	public List<String> getColumnNames(PivotDataTable table) {
		List<String> res = new ArrayList<String>();
		switch(this) {
		case INPUT:
			for(PivotItem item: table.getTemplate().getPivotItems(Where.ASROW)) {
				if(item==PivotItemFactory.RESULT_INPUT) {
					res.add(item.getFullName());
				}
			}
			break;
		case GROUP:
			for(PivotItem item: table.getTemplate().getPivotItems(Where.ASROW)) {
				if(item.getClassifier()==PivotItemClassifier.STUDY_GROUP && item.getFullName().startsWith("Group")) {
					res.add(item.getFullName());
				}
			}
			break;
		case PHASE:
			for(PivotItem item: table.getTemplate().getPivotItems(Where.ASROW)) {
				if(item.getClassifier()==PivotItemClassifier.STUDY_PHASE) {
					res.add(item.getFullName());
				}
			}
			break;
		case TOPSAMPLE:
			for(PivotItem item: table.getTemplate().getPivotItems(Where.ASROW)) {
				if(item.getClassifier()==PivotItemClassifier.TOP) {
					res.add(item.getFullName());
				}
			}
			break;				
		case BIOTYPE:
			for(PivotItem item: table.getTemplate().getPivotItems(Where.ASROW)) {
				if(item.getClassifier()==PivotItemClassifier.BIOSAMPLE) {
					res.add(item.getFullName());
				}
			}
			break;				
		default:
			throw new IllegalArgumentException("Not implemented for "+this);
		}
		return res;
	}	
	
}