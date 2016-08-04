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

package com.actelion.research.spiritapp.spirit.ui.pivot;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.actelion.research.spiritapp.spirit.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritapp.spirit.ui.study.GroupLabel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.pivot.PivotRow;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;

public class PivotTableModel extends ExtendTableModel<PivotRow> {

	public static final Column<PivotRow, Group> COLUMN_GROUP = new Column<PivotRow, Group>("Group", Group.class, 30) {
		private GroupLabel groupLabel = new GroupLabel();

		@Override
		public String getCategory() {
			return "Row";
		}
		@Override
		public Group getValue(PivotRow row) {
			Result b = row.getRepresentative().getResult();
			return b==null? null: b.getGroup();
		}
		
		@Override
		public JComponent getCellComponent(AbstractExtendTable<PivotRow> table, PivotRow row, int rowNo, Object value) {
			groupLabel.setVerticalAlignment(SwingUtilities.TOP);
			groupLabel.setGroup((Group) value);
			groupLabel.setCondenseText(table.isPreferredCondenseText());
			return groupLabel;
		}		
	};
	
	public static final Column<PivotRow, String> COLUMN_SUBGROUP = new Column<PivotRow, String>("St.", String.class, 15, 15) {
		@Override
		public String getCategory() {
			return "Row";
		}
		@Override
		public String getValue(PivotRow row) {
			Result r = row.getRepresentative().getResult();
			Biosample b = r.getBiosample();
			if(b==null || b.getInheritedStudy()==null) return null;
			
			Group group = b.getInheritedGroup();
			String val = group==null? null: group.getNSubgroups()>1? ""+(b.getInheritedSubGroup()+1): null;
			return val;
		}
		
		private GroupLabel groupLabel = new GroupLabel();
		@Override
		public JComponent getCellComponent(AbstractExtendTable<PivotRow> table, PivotRow row, int rowNo, Object value) {
			Result r = row.getRepresentative().getResult();
			groupLabel.setVerticalAlignment(SwingUtilities.TOP);
			groupLabel.setText((String)value, r==null || r.getBiosample()==null? null: r.getBiosample().getInheritedGroup());
			return groupLabel;
		}
		
	};

	public static final Column<PivotRow, Biosample> COLUMN_TOPID = new Column<PivotRow, Biosample>("TopId", Biosample.class, 70) {		
		private SampleIdLabel sampleIdLabel = new SampleIdLabel(false, false);

		@Override
		public String getCategory() {
			return "Row";
		}
		@Override
		public Biosample getValue(PivotRow row) {
			Biosample b = row.getRepresentative().getResult().getBiosample();
			return b==null? null: b.getTopParent();
		}
		
		@Override
		public JComponent getCellComponent(AbstractExtendTable<PivotRow> table, PivotRow row, int rowNo, Object value) {
			sampleIdLabel.setBiosample((Biosample) value);
			return sampleIdLabel;
		}
	};
	
	
	public PivotTableModel() {
	}
	
	@Override
	public void clear() {
		allColumns.clear();
		displayed.clear();
		rows.clear();
		fireTableDataChanged();
	}
	
}
