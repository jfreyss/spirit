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

package com.actelion.research.spiritapp.spirit.ui.study.wizard.phase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExcelTable;
import com.actelion.research.util.ui.exceltable.ExcelTableModel;

public class PhaseEditTable extends ExcelTable<Phase> {
	
	public static class PhaseEditTableModel extends ExcelTableModel<Phase>{ 
		public PhaseEditTableModel(final Study study, final Map<Phase, Pair<Integer, Integer>> phase2count) {
			List<Column<Phase, ?>> columns = new ArrayList<>();
			columns.add(new Column<Phase, String>("Name", String.class, 50) {
				@Override
				public String getValue(Phase row) {
					return row.getName();
				}
				public void setValue(Phase row, String value) {
					String v = Phase.cleanName(value, study.getPhaseFormat());
					if(v.length()==0) {
						try {
							PhaseDlg.checkCanDelete(row);
							row.setName(v);
						} catch(Exception e) {
							JExceptionDialog.showError(e);
						}
					} else {					
						row.setName(v);
					}
				}
			});
			if(phase2count!=null) {
				columns.add(new Column<Phase, Integer>("N.Bio.", Integer.class, 50) {
					@Override
					public String getToolTipText() {
						return "Number of linked biosamples";
					}
					@Override
					public Integer getValue(Phase row) {
						return phase2count.get(row)==null? null: phase2count.get(row).getFirst();
					}
					@Override
					public boolean isEditable(Phase row) {
						return false;
					}
					@Override
					public boolean isHideable() {
						return true;
					}
				});
				columns.add(new Column<Phase, Integer>("N.Res.", Integer.class, 50) {
					@Override
					public String getToolTipText() {
						return "Number of linked results";
					}
					@Override
					public Integer getValue(Phase row) {
						return phase2count.get(row)==null? null: phase2count.get(row).getSecond();
					}
					@Override
					public boolean isEditable(Phase row) {
						return false;
					}
					@Override
					public boolean isHideable() {
						return true;
					}
				});
			}
			setColumns(columns);
		}
		
		@Override
		public Phase createRecord() {
			return new Phase();
		}
	}
	
	public PhaseEditTable(Study study, Map<Phase, Pair<Integer, Integer>> phase2count) {
		super(new PhaseEditTableModel(study, phase2count));
		setCanSort(false);
		getModel().showAllHideable(false);
	}

	@Override
	public PhaseEditTableModel getModel() {
		return (PhaseEditTableModel) super.getModel();
	}
	
	public List<Phase> getNonEmptyRows() {
		List<Phase> res = new ArrayList<>();
		for (Phase phase : getRows()) {
			if(phase.getName()!=null && phase.getName().trim().length()>0) {
				res.add(phase);
			}
		}
		return res;
	}
}
