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

package com.actelion.research.spiritapp.spirit.ui.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

import com.actelion.research.spiritapp.spirit.ui.result.column.CommentsColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.ContainerColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.CreationColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.ElbColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.MetadataColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.PhaseColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.QualityColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.SampleIdColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.StudyGroupColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.StudyIdColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.StudySubGroupColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.TestNameColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.TopIdColumn;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;

public class ResultTableModel extends ExtendTableModel<Result> {

	
	public Column<Result, ResultValue> createValueColumn(final int index, final boolean isOutput) {
		
		return new Column<Result, ResultValue>((isOutput? "Result\nValue ": "Result\nInput ") , ResultValue.class) {
			
			@Override
			public float getSortingKey() {
				return isOutput? 7f : 6f;
			}
			
			@Override
			public ResultValue getValue(Result row) {
				List<ResultValue> vals = isOutput? row.getOutputResultValues(): row.getInputResultValues();
				if(index<0 || index>=vals.size()) return null;
				ResultValue val = vals.get(index);	
				if(val.getValue()==null) return null;
				return val;				
			}
			
			private ValuePanel valuePanel = new ValuePanel();
			
			@Override
			public JComponent getCellComponent(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
				ResultValue v = (ResultValue) value;
				if(v!=null) { 
					valuePanel.setValue(isOutput, v);
					return valuePanel;
				}
				return super.getCellComponent(table, row, rowNo, value);
			}
			
			@Override
			public void postProcess(AbstractExtendTable<Result> table, Result row, int rowNo, Object value, JComponent comp) {
				ResultValue v = (ResultValue) value;
				if(v!=null && v.getResult().getQuality()!=null && v.getResult().getQuality().getBackground()!=null) {				
					comp.setBackground(v.getResult().getQuality().getBackground());
				}
			}
			
			@Override
			public boolean isMultiline() {
				return true;
			}
		};			
	}		
	
	
	private List<Column<Result, ?>> inputOutputColumns = new ArrayList<Column<Result, ?>>();


	
	

	public ResultTableModel() {
		initColumns();
	}
	
	
	protected void initColumns() {
		
		Set<Biosample> biosamples = Result.getBiosamples(getRows());
		Set<Biosample> topSamples = Biosample.getTopParentsInSameStudy(biosamples);
		boolean differentParents = !biosamples.equals(topSamples);
		
		
		
		List<Column<Result, ?>> columns = new ArrayList<Column<Result,?>>();
		columns.add(COLUMN_ROWNO);
		columns.add(new ElbColumn());
		columns.add(new StudyIdColumn());
		columns.add(new StudyGroupColumn());
		columns.add(new StudySubGroupColumn());
		columns.add(new TopIdColumn().setHideable(!differentParents));
		columns.add(new PhaseColumn());
		columns.add(new SampleIdColumn());
		columns.add(new MetadataColumn());
		columns.add(new TestNameColumn());

		columns.addAll(createInputOutputColumns());

		columns.add(new CommentsColumn());		
		columns.add(new CreationColumn(true));
		
		setColumns(columns);
				
	}
	
	
	@Override
	public List<Column<Result, ?>> getPossibleColumns() {
		List<Column<Result, ?>> res = new ArrayList<Column<Result, ?>>();
		res.add(new ContainerColumn());
		res.add(new QualityColumn());
		res.add(new CreationColumn(false));
		return res;
	}
	
	public List<Column<Result, ?>> createInputOutputColumns() {
		int maxInput = 0;
		int maxOutput = 0;
		for (Result res : getRows()) {
			maxInput = Math.max(maxInput, res.getInputResultValues().size());
			maxOutput = Math.max(maxOutput, res.getOutputResultValues().size());
		}
		
		inputOutputColumns.clear();
		for (int i = 0; i < maxInput; i++) {
			inputOutputColumns.add(createValueColumn(i, false));					
		}
		for (int i = 0; i < maxOutput; i++) {
			inputOutputColumns.add(createValueColumn(i, true));					
		}
		return inputOutputColumns;		
	}
	
	
	
}
