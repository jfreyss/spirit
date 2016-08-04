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

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableColumn;

import com.actelion.research.spiritapp.spirit.ui.pivot.column.PivotCellColumn;
import com.actelion.research.spiritapp.spirit.ui.pivot.column.PivotStringColumn;
import com.actelion.research.spiritapp.spirit.ui.pivot.designer.PivotTemplateDlg;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.pivot.Computed;
import com.actelion.research.spiritcore.business.pivot.GraphpadPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotCell;
import com.actelion.research.spiritcore.business.pivot.PivotCellKey;
import com.actelion.research.spiritcore.business.pivot.PivotColumn;
import com.actelion.research.spiritcore.business.pivot.PivotDataTable;
import com.actelion.research.spiritcore.business.pivot.PivotItem;
import com.actelion.research.spiritcore.business.pivot.PivotItemFactory;
import com.actelion.research.spiritcore.business.pivot.PivotRow;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Aggregation;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTable;
import com.actelion.research.util.ui.exceltable.FastHeaderRenderer;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;


public class PivotTable extends ExtendTable<PivotRow> {


	private PivotDataTable data;
	
	public PivotTable() {
		super(new PivotTableModel());
		
		setBorderStrategy(BorderStrategy.WHEN_DIFFERENT_VALUE);
		setUseSmartHeight(false);
		setRowHeight(20);
	}
	
	@Override
	public PivotTableModel getModel() {
		return (PivotTableModel) super.getModel();
	}

			
	public void setPivotDataTable(PivotDataTable data) {
		this.data = data;
		
		PivotTableModel model = getModel();
		model.clear();	
		
		if(data.getResults().size()==0) {
			model.fireTableStructureChanged();
			return;
		}
		
		//Set columns
		List<Column<PivotRow, ?>> columns = new ArrayList<Column<PivotRow, ?>>();
		for (PivotItem item : data.getTemplate().getPivotItems(Where.ASROW)) {
			if(item==null) continue;
			if(item==PivotItemFactory.STUDY_GROUP) {
				columns.add(PivotTableModel.COLUMN_GROUP);
			} else if(item==PivotItemFactory.STUDY_SUBGROUP) {
				columns.add(PivotTableModel.COLUMN_SUBGROUP);
			} else if(item==PivotItemFactory.BIOSAMPLE_TOPID) {
				columns.add(PivotTableModel.COLUMN_TOPID);
			} else {
				columns.add(new PivotStringColumn(item));
			}
		}
		for (final PivotColumn col : data.getPivotColumns()) {
			columns.add(new PivotCellColumn(col));
		}
		model.setColumns(columns);
		
		//Set Rows
		model.setRows(data.getPivotRows());

		
		//Reset Columns width+headers		
		resetPreferredColumnWidth();
		
	}
	
	private boolean hasNestedCells() {
		for(int rowNo = 0; rowNo<getRowCount(); rowNo++) {
			for(int col = 0 ; col<getColumnCount(); col++) {
				Object o = getValueAt(rowNo, col);
				if(!(o instanceof PivotCell)) continue;
				for (PivotCellKey key: ((PivotCell)o).getNestedKeys()) {
					if(key.getKey().length()>0 ){
						return true;
					}
				}
			}
		}			
		return false;
	}
	
	@Override
	public String[][] getTabDelimitedTable() {
		int[] cols = new int[getColumnCount()];
		int[] rows = new int[getRowCount()];
		for (int i = 0; i < cols.length; i++) {
			cols[i] = i;
		}
		for (int i = 0; i < rows.length; i++) {
			rows[i] = i;
		}
		return getTabDelimitedTable(cols, rows);
	}
	
	public String[][] getTabDelimitedTable(int[] cols, int[] rows) {
		
		List<List<String>> table = new ArrayList<List<String>>();

		if(data==null) return new String[0][0];
		PivotTemplate tpl = data.getTemplate();
		boolean showExtraColumns = !(tpl instanceof GraphpadPivotTemplate);
		
		//Find if we have nested cells
		boolean hasNestedCells = hasNestedCells();

		
		//
		//First the headers
		List<String> row;
		table.add(row = new ArrayList<String>());
		for(int ci = 0 ; ci<cols.length; ci++) {
			int col = cols[ci];
			int modelCol = convertColumnIndexToModel(col);
			Column<PivotRow, ?> c = getModel().getColumn(modelCol);
			if(c.getColumnClass()==PivotCell.class) {
				row.add(MiscUtils.removeHtml(c.getName()));	
				if(hasNestedCells && tpl.getAggregation()!=Aggregation.HIDE) row.add("");
				if(showExtraColumns && tpl.getComputed()!=Computed.NONE) {
					row.add(MiscUtils.removeHtml(c.getName() + "\n" + tpl.getComputed()));	
				}
			} else {
				row.add(MiscUtils.removeHtml(c.getName()));
			}
		}
		table.add(row = new ArrayList<String>());
		
		//
		//Then the rows
		boolean hasData = true;
		for(int ri = 0; ri<rows.length; ri++) {
			int rowNo = rows[ri];
			int subRow = 0;
			hasData = true;
			while(hasData) {
				hasData = false;
				for(int ci = 0 ; ci<cols.length; ci++) {
					int col = cols[ci];
					Column<PivotRow, ?> c = getModel().getColumn(col);
					Object o = getValueAt(rowNo, col);
					if(c.getColumnClass()==PivotCell.class) {
						PivotCell cell = (PivotCell) o;
						PivotCellKey cellKey = subRow<cell.getNestedKeys().size()? cell.getNestedKeys().get(subRow): null;
						if(cellKey==null) {
							row.add("");
							if(hasNestedCells && tpl.getAggregation()!=Aggregation.HIDE) row.add("");
							if(showExtraColumns && tpl.getComputed()!=Computed.NONE) row.add("");	
						} else {
							hasData = true;
							PivotCell subvl = cell.getNested(cellKey);
							if(tpl.getAggregation()==Aggregation.HIDE || hasNestedCells) row.add(cellKey==null?"": cellKey.getKey());						
							if(tpl.getAggregation()!=Aggregation.HIDE) row.add(formatForTab(subvl.getValue()));
							if(showExtraColumns && tpl.getComputed()!=Computed.NONE) row.add(formatForTab(subvl.getComputed()));									
						}
					} else {
						if(subRow==0) {
							row.add(formatForTab(o));
						} else {
							row.add("");
						}
					}
				}
				if(hasData) table.add(row = new ArrayList<String>());
				else row.clear();
				subRow++;
			} //end has-data
		}
		String[][] res = new String[table.size()-1][table.get(0).size()];
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res[i].length; j++) {
				List<String> l = i<table.size()? table.get(i): null;
				res[i][j] = l!=null && j<l.size()? l.get(j): "";
			
			}
		}
				
		return res;
	}
	

	public List<PivotCell> getSelectedPivotCells() {
		List<PivotCell> res = new ArrayList<PivotCell>();
		for (int row : getSelectedRows()) {
			for (int col : getSelectedColumns()) {
				Object value = getValueAt(row, col);
				if(value instanceof PivotCell) {
					res.add( (PivotCell) value);
				}						
			}
		}
		return res;
	}
	public List<Result> getSelectedResults() {
		final Set<Result> results = new LinkedHashSet<Result>(); 
		
		Collection<PivotCell> cells = getSelectedPivotCells();
		if(cells.size()>0) {
			for (PivotCell sel : cells) {
				results.addAll(sel.getResults());
			}
		}
		return new ArrayList<Result>(results);
	}
	
	public Collection<Biosample> getSelectedBiosamples() {
		Set<Biosample> res = new HashSet<Biosample>();
		int[] selRows = getSelectedRows();
		int[] selCols = getSelectedColumns();
		for(int r=0; r<selRows.length; r++) {
			for(int c=0; c<selCols.length; c++) {
				PivotRow pivotRow = getModel().getRows().get(selRows[r]);			
				Column<PivotRow, ?> col = getModel().getColumn(convertColumnIndexToModel(selCols[c]));
				if(col.getColumnClass()==Biosample.class) {
					Biosample b = (Biosample) col.getValue(pivotRow);
					if(b!=null) res.add(b);
				} else if(col.getColumnClass()==PivotCell.class) {
					PivotCell pivotCell = (PivotCell) col.getValue(pivotRow);
					for(Result rr: pivotCell.getResults()) {
						if(rr.getBiosample()!=null) res.add(rr.getBiosample());
					}
				}
			}
		}
		return res;
	}
	
	public Collection<Biosample> getHighlightedSamples() {
		int[] selRows = getSelectedRows();
		int[] selCols = getSelectedColumns();
		if(selRows.length==1 && selCols.length==1) {
			PivotRow b = getModel().getRows().get(selRows[0]);
			Column<PivotRow, ?> col = getModel().getColumn(convertColumnIndexToModel(selCols[0]));
			
			if(col.getColumnClass()==Biosample.class) {
				Biosample res = (Biosample) col.getValue(b);
				return res==null? new HashSet<Biosample>(): Collections.singleton(res);
			}
		}
		return new HashSet<Biosample>();
	}
	

	public PivotDataTable getPivotDataTable() {
		return data;
	}
	
	
	private final FastHeaderRenderer<PivotRow> renderer = new FastHeaderRenderer<PivotRow>() {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JComponent comp = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			
			int col = convertColumnIndexToModel(column);
			comp.setOpaque(true);
			if(col<getModel().getColumns().size() && getModel().getColumns().get(col) instanceof PivotCellColumn) {				
				comp.setBackground(PivotTemplateDlg.COLUMN_COLOR);
				if(comp instanceof JLabelNoRepaint) {
					((JLabelNoRepaint) comp).setVerticalAlignment(SwingConstants.TOP);
					((JLabelNoRepaint) comp).setCondenseText(((AbstractExtendTable<?>) table).isPreferredCondenseText());
				}
			} else {
				comp.setBackground(PivotTemplateDlg.ROW_COLOR);
			}
			return comp;
		}
	};
	
	@Override
	protected void setHeaderRenderers() {
		for (int col = 0; col < getColumnModel().getColumnCount(); col++) {
			TableColumn column = getColumnModel().getColumn(col);
			if(column.getHeaderRenderer()!=renderer) {
				column.setHeaderRenderer(renderer);
			}
		}			
	}
}
