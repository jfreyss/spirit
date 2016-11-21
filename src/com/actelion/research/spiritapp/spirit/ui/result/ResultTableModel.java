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
import com.actelion.research.spiritapp.spirit.ui.util.component.DocumentTextField;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;

public class ResultTableModel extends ExtendTableModel<Result> {

	
	public Column<Result, ResultValue> createValueColumn(final int index, final OutputType type) {
		
		
		return new Column<Result, ResultValue>((type==OutputType.OUTPUT? "Output": type==OutputType.INPUT? "Input": "Info") + (index+1) , ResultValue.class) {
			
			private ValuePanel valuePanel = new ValuePanel();

			@Override
			public float getSortingKey() {
				return 6f + type.ordinal();
			}
			
			@Override
			public ResultValue getValue(Result row) {
				TestAttribute ta = type==OutputType.OUTPUT? row.getTest().getOutputAttributes().get(index):
					type==OutputType.INPUT? row.getTest().getInputAttributes().get(index):
						row.getTest().getInfoAttributes().get(index);
						
				ResultValue val = row.getResultValue(ta);	
				if(val.getValue()==null) return null;
				return val;				
			}
						
			@Override
			public JComponent getCellComponent(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
				ResultValue v = (ResultValue) value;
				if(v!=null) { 
					valuePanel.setValue(type, v);
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
			public boolean mouseDoubleClicked(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
				TestAttribute ta = type==OutputType.OUTPUT? row.getTest().getOutputAttributes().get(index):
					type==OutputType.INPUT? row.getTest().getInputAttributes().get(index):
						row.getTest().getInfoAttributes().get(index);
						
				if(ta.getDataType()==DataType.D_FILE && row.getResultValue(ta).getLinkedDocument()!=null) {
					DocumentTextField.open(row.getResultValue(ta).getLinkedDocument());
					return true;
				}
				return false;
			}
			
			@Override
			public boolean isMultiline() {
				return true;
			}
			@Override
			public boolean shouldMerge(Result r1, Result r2) {
				return false;
			}
		};			
	}		
	
	
	private List<Column<Result, ?>> inputOutputColumns = new ArrayList<>();
	private final boolean compact;


	public ResultTableModel(boolean compact) {
		this.compact = compact;
		initColumns();
	}
	
	
	protected void initColumns() {
		
		Set<Biosample> biosamples = Result.getBiosamples(getRows());
		Set<Biosample> topSamples = Biosample.getTopParentsInSameStudy(biosamples);
		boolean differentParents = !biosamples.equals(topSamples);
		
		
		
		List<Column<Result, ?>> columns = new ArrayList<>();
		columns.add(COLUMN_ROWNO);
		if(!compact) columns.add(new ElbColumn());
		if(!compact) columns.add(new StudyIdColumn());
		if(!compact) columns.add(new StudyGroupColumn());
		if(!compact) columns.add(new StudySubGroupColumn());
		if(!compact) columns.add(new TopIdColumn().setHideable(!differentParents));
		columns.add(new PhaseColumn());
		if(!compact) columns.add(new SampleIdColumn());
		if(!compact) columns.add(new MetadataColumn());
		columns.add(new TestNameColumn());

		columns.addAll(createInputOutputColumns());

		columns.add(new CommentsColumn());		
		columns.add(new CreationColumn(true));
		
		setColumns(columns);
				
	}
	
	
	@Override
	public List<Column<Result, ?>> getPossibleColumns() {
		List<Column<Result, ?>> res = new ArrayList<>();
		res.add(new ContainerColumn());
		res.add(new QualityColumn());
		res.add(new CreationColumn(false));
		return res;
	}
	
	public List<Column<Result, ?>> createInputOutputColumns() {
		int maxInput = 0;
		int maxOutput = 0;
		int maxInfos = 0;
		Set<Test> tests = Result.getTests(getRows());
		for (Test t : tests) {
			maxInput = Math.max(maxInput, t.getInputAttributes().size());
			maxOutput = Math.max(maxOutput, t.getOutputAttributes().size());
			maxInfos = Math.max(maxInfos, t.getInfoAttributes().size());
		}
		
		inputOutputColumns.clear();		
		for (int i = 0; i < maxInput; i++) {
			inputOutputColumns.add(createValueColumn(i, OutputType.INPUT));					
		}
		for (int i = 0; i < maxOutput; i++) {
			inputOutputColumns.add(createValueColumn(i, OutputType.OUTPUT));					
		}
		for (int i = 0; i < maxInfos; i++) {
			inputOutputColumns.add(createValueColumn(i, OutputType.INFO));					
		}
		return inputOutputColumns;		
	}
	
	
	
}
