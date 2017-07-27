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

package com.actelion.research.spiritapp.spirit.ui.biosample.column;

import java.awt.Color;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JComponent;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoFormat;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoSize;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class ChildrenColumn extends Column<Biosample, String> {

	public ChildrenColumn() {
		super("Linked\nChildren", String.class, 80);
	}
	@Override
	public float getSortingKey() {return 20.1f;}

	/**
	 * Split the children by biotype
	 * @param row
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Map<Biotype, Set<Biosample>> getMap(Biosample row) {

		Map<Biotype, Set<Biosample>> map = (Map<Biotype, Set<Biosample>>) row.getAuxiliaryInfos().get("tmp_children");
		if(map==null) {
			map = new TreeMap<Biotype, Set<Biosample>>();
			for (Biosample child : row.getHierarchy(HierarchyMode.CHILDREN)) {
				if(child.getStatus()==Status.TRASHED || child.getStatus()==Status.USEDUP) continue;

				Set<Biosample> set = map.get(child.getBiotype());
				if(set==null) {
					set = new HashSet<Biosample>();
					map.put(child.getBiotype(), set);
				}
				set.add(child);
			}
			row.getAuxiliaryInfos().put("tmp_children", map);
		}
		return map;
	}

	@Override
	public String getValue(Biosample row) {

		Map<Biotype, Set<Biosample>> map = getMap(row);
		if(map.size()==0) return null;

		StringBuilder sb = new StringBuilder();
		if(map.size()>3) {
			int n = 0;
			for (Biotype biotype : map.keySet()) {
				Set<Biosample> set = map.get(biotype);
				n+= set.size();
			}
			sb.append(n+" items");
		} else if(map.size()>=1) {
			for (Biotype biotype : map.keySet()) {
				Set<Biosample> set = map.get(biotype);
				String location = Biosample.getInfos(set, EnumSet.of(InfoFormat.LOCATION), InfoSize.ONELINE);
				String infos = Biosample.getBiotypeString(set);
				sb.append(set.size() + " " + infos + (location==null?"": "  " + location) + "\n");
			}
		}

		return sb.toString();
	}

	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		comp.setForeground(Color.BLUE);
	}


	@Override
	public boolean isMultiline() {
		return true;
	}

	@Override
	public boolean shouldMerge(Biosample r1, Biosample r2) {return false;}

	@Override
	public boolean isHideable() {
		return true;
	}

}