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

package com.actelion.research.spiritapp.spirit.ui.result.edit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import com.actelion.research.spiritapp.spirit.ui.lf.SpiritExcelTable;
import com.actelion.research.spiritapp.spirit.ui.result.column.AttributeColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.CommentsColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.CreationColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.ElbColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.PhaseColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.QualityColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.SampleIdColumn;
import com.actelion.research.spiritapp.spirit.ui.result.column.TopIdColumn;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.util.ui.EasyClipboard;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.FillCellAction;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class EditResultTable extends SpiritExcelTable<Result>  {
	
	private EditResultDlg dlg;
//	private EditResultTab tab;
	
	/**
	 * Constructor
	 */
	public EditResultTable(EditResultDlg dlg) {
		super(new EditResultTableModel());
		this.dlg = dlg;
//		this.tab = tab;		
	}
	
	@Override
	public EditResultTableModel getModel() {
		return (EditResultTableModel) super.getModel();
	}

	public void updateModel(final Test test) {
		//Update the results to the new test
		for(Result r: getModel().getRows()) {
			r.setTest(test);
		}		
		if(getModel().getRows().size()==0 && test!=null) {
			List<Result> results = new ArrayList<>();
			results.add(new Result(test));
			getModel().setRows(results);			
		}
		
		//update the test
		getModel().setTest(test);
		
		Set<Biosample> biosamples = Result.getBiosamples(getModel().getRows());
		Set<Biosample> topIds = Biosample.getTopParentsInSameStudy(biosamples);
		boolean differentParent = Collections.disjoint(biosamples, topIds);
		
		
		//Recreate the columns
		List<Column<Result,?>> columns = new ArrayList<>();
		if(test!=null) {
			
			columns.add(getModel().COLUMN_ROWNO);
			columns.add(new ElbColumn());
			
			
			columns.add(new TopIdColumn().setHideable(!differentParent));
			columns.add(new SampleIdColumn());
			columns.add(new PhaseColumn());
			
			for (final TestAttribute att : test.getAttributes()) {
				columns.add(new AttributeColumn(att));	
			}
			
			columns.add(new CommentsColumn());			
			columns.add(new QualityColumn());
			columns.add(new CreationColumn(true).setHideable(true));
			columns.add(new CreationColumn(false));
			
		}
		getModel().setColumns(columns);
		getModel().showAllHideable(false);
		resetPreferredColumnWidth();
		
	}
	
	@Override
	protected void populateHeaderPopup(JPopupMenu popupMenu, Column<Result, ?> column) {
		
		popupMenu.add(new JSeparator());
		
		if(column instanceof AttributeColumn) {
			AttributeColumn attColumn = (AttributeColumn) column;
			if(attColumn.getAttribute().getDataType()==DataType.LIST) {
				List<String> options = Arrays.asList(attColumn.getAttribute().getParametersArray());
				popupMenu.add(new FillCellAction(this, column, options));				
			} else {
				popupMenu.add(new FillCellAction(this, column));
			}
		} else { 
			popupMenu.add(new FillCellAction(this, column));
		}		
	}
	
//	/**
//	 * @param study the study to set
//	 */
//	public void setStudy(Study study) {
//		getModel().setStudy(study);
//	}
	
	@Override
	protected void pasteSelection() {
		Test test = getModel().getTest();
//		Study study = getModel().getStudy();
		
		//Check if we paste a pivot table
		String paste = EasyClipboard.getClipboard();
		if(paste==null) return;
		
		String[][] table = POIUtils.convertTable(paste);
		
		if(DEBUG) System.out.println("ExcelTable: Paste "+paste);
		if(table.length==0) return;
		
		if(table.length>0) {
			
			//Analyze first row
			boolean isPivot = table[0].length>getColumnCount();
			for (int i = 0; !isPivot && i < table[0].length; i++) {
				String s = table[0][i].replace(" ", "");
				if(s.equalsIgnoreCase("sampleid") || s.equalsIgnoreCase("animalid") || s.equalsIgnoreCase("phase")) isPivot = true;
			}
			
			//Ask to pivot
			if(isPivot) {
				int res = JOptionPane.showConfirmDialog(getParent(), "Are you pasting a pivoted table?", "Pivot Table?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, IconType.PIVOT.getIcon());
				if(res==JOptionPane.YES_OPTION) {
					//Pivot table
					try {
						List<Result> results = PivotDlg.parse(test, table);
						dlg.addResults(results, false);
					} catch (Exception e) {
						JExceptionDialog.showError(EditResultTable.this, e);
					}
					return;
				}
			} 
		}
			
		
		
		Exception exception = null;
		try {
			super.pasteSelection();
		} catch (Exception ex) {
			exception = ex;
		}

		
		if(exception!=null && dlg==null) {
			JExceptionDialog.showError(EditResultTable.this, exception);
		} else if(exception!=null) {
			JOptionPane.showMessageDialog(EditResultTable.this, "Pasting data returned some errors:\n"+exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		
	}
	
	
	@Override
	public void setRows(List<Result> data) {
		for (Result result : data) {
			assert result.getTest()!=null;
			assert result.getTest().equals(getModel().getTest());
		}
		super.setRows(data);
	}
}
