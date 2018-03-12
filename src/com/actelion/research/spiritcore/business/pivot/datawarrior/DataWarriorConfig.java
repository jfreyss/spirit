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

package com.actelion.research.spiritcore.business.pivot.datawarrior;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.actelion.research.spiritcore.business.pivot.Computed;
import com.actelion.research.spiritcore.business.pivot.PivotItem;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;
import com.actelion.research.spiritcore.business.result.TestAttribute;

public class DataWarriorConfig {

	public enum ChartType {
		SCATTER("scatter"),
		WHISKERS("whiskers"),
		BOXPLOT("boxes");
		private String name;

		private ChartType(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private Set<PivotDataType> merge = new HashSet<PivotDataType>();
	private PivotDataType xAxis;
	private PivotDataType separate;
	private ChartType type = ChartType.SCATTER;
	private PivotDataType split;
	private Computed computed = Computed.NONE;
	private boolean logScale = false;

	private Set<TestAttribute> skippedAttributes;
	private List<String> viewNames;

	private PivotTemplate custom = null;

	/**
	 * Export All columns,even those not specified in the view
	 */
	private boolean exportAll = true;


	public boolean isSet(PivotDataType p) {
		if(xAxis==p) return true;
		if(separate==p) return true;
		if(split==p) return true;
		if(merge.contains(p)) return true;

		return false;
	}
	public void unset(PivotDataType p) {
		if(xAxis==p) xAxis = null;
		if(separate==p) separate = null;
		if(split==p) split = null;
		if(merge.contains(p)) merge.remove(p);
	}

	public Set<PivotDataType> getMerge() {
		return merge;
	}

	public void addMerge(PivotDataType p) {
		unset(p);
		merge.add(p);
	}

	public PivotDataType getXAxis() {
		return xAxis;
	}

	public void setXAxis(PivotDataType xAxis) {
		unset(xAxis);
		this.xAxis = xAxis;
	}

	public PivotDataType getSeparate() {
		return separate;
	}

	public void setSeparate(PivotDataType separate) {
		unset(separate);
		this.separate = separate;
	}


	public ChartType getType() {
		return type;
	}

	public void setType(ChartType type) {
		this.type = type;
	}


	public PivotDataType getSplit() {
		return split;
	}

	public void setSplit(PivotDataType split) {
		unset(split);
		this.split = split;
	}

	public Computed getComputed() {
		return computed;
	}

	public void setComputed(Computed computed) {
		this.computed = computed;
	}

	@Override
	public String toString() {
		return  "xAxis="+xAxis+"\n" +
				"separate="+separate+"\n" +
				"split="+split+"\n" +
				"computed="+computed+"\n" +
				"merge="+merge+"\n" +
				"type="+type;
	}

	public static DataWarriorConfig createCustomModel(PivotTemplate custom) {
		assert custom!=null;

		//Cell items cannot be exported to DW, so move them to cols
		PivotTemplate tpl = new PivotTemplate(custom);
		for(PivotItem pv: tpl.getPivotItems(Where.ASCELL)) {
			tpl.setWhere(pv, Where.ASCOL);
		}

		//Create our standard model
		DataWarriorConfig model = new DataWarriorConfig();
		model.setCustomTemplate(tpl);
		model.setType(ChartType.SCATTER);
		model.setXAxis(PivotDataType.GROUP);
		model.setSeparate(null);
		model.setSplit(null);
		return model;
	}

	public boolean isLogScale() {
		return logScale;
	}
	public void setLogScale(boolean logScale) {
		this.logScale = logScale;
	}

	public PivotTemplate getCustomTemplate() {
		return custom;
	}

	/**
	 * Set the template and move all nested items to the row level
	 */
	public void setCustomTemplate(PivotTemplate tpl) {
		PivotTemplate tmp = new PivotTemplate(tpl);
		for (PivotItem pv : tmp.getPivotItems(Where.ASCELL)) {
			tmp.setWhere(pv, Where.ASROW);
		}
		this.custom = tmp;
	}
	public List<String> getViewNames() {
		return viewNames;
	}
	public void setViewNames(List<String> viewNames) {
		this.viewNames = viewNames;
	}
	public Set<TestAttribute> getSkippedAttributes() {
		return skippedAttributes;
	}
	public void setSkippedAttributes(Set<TestAttribute> skippedAttributes) {
		this.skippedAttributes = skippedAttributes;
	}
	public void setExportAll(boolean exportAll) {
		this.exportAll = exportAll;
	}
	public boolean isExportAll() {
		return exportAll;
	}
}
