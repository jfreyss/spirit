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

package com.actelion.research.spiritapp.spirit.ui.biosample.linker;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.biosample.editor.AutoCompletionCellEditor;
import com.actelion.research.spiritapp.spirit.ui.biosample.editor.DateStringCellEditor;
import com.actelion.research.spiritapp.spirit.ui.biosample.editor.MetadataComboboxCellEditor;
import com.actelion.research.spiritapp.spirit.ui.biosample.editor.MultiComboboxCellEditor;
import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.util.Formatter;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.AlphaNumericalCellEditor;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class MetadataColumn extends AbstractLinkerColumn<String> {
	
//	private DicoLabel dicoLabel = new DicoLabel();
	private JLabelNoRepaint defaultLabel = new JLabelNoRepaint();
	
	protected MetadataColumn(final BiosampleLinker linker) {
		super(linker, String.class, 30, 400);
	}
	
	@Override
	public String getValue(Biosample row) {
		row = linker.getLinked(row);
		if(row==null || row.getBiotype()==null || !row.getBiotype().equals(getType().getBiotype()) || row.getMetadata(getType())==null) return null;
		String v = row.getMetadata(getType()).getValue();
		if(v==null || v.length()==0) return null;
		return v;
	}
	@Override
	public void setValue(Biosample row, String value) {
		row.setMetadata(getType(), value);
	}
	
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {	
//		if(getType().getDataType()==DataType.DICO) {
//			dicoLabel.setText((String) value);
//			return dicoLabel;
//		} else {
			defaultLabel.setText((String) value);
			return defaultLabel;
//		}
	}
	
	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		BiotypeMetadata mt = getType();
		DataType datatype = mt.getDataType();
		switch (datatype) {
		case ALPHA:
			return new AlphaNumericalCellEditor();	
		case NUMBER:
			return new AlphaNumericalCellEditor();
		case AUTO:
			return new AutoCompletionCellEditor(getType());
		case LIST:
			return new MetadataComboboxCellEditor(mt.getParameters());
		case MULTI:
			return new MultiComboboxCellEditor(mt.getParameters());
		case DATE:
			return new DateStringCellEditor();
//		case DICO:
//			return new DicoCellEditor(mt.getParameters());
//		case ELN:
//			return new AlphaNumericalCellEditor();
		default:
			throw new IllegalArgumentException("Invalid datatype, no editor for: "+datatype);
		}
	}
	
	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value1, JComponent comp) {
		super.postProcess(table, row, rowNo, value1, comp);
		if(row==null) return;
		if(!table.isEditable()) return;

		if(!linker.isLinked() && getType()!=null && getType().isRequired()) {
			comp.setBackground(LF.BGCOLOR_REQUIRED);
		}

		BiotypeMetadata mt = getType();
		if(row.getBiotype()==null || !mt.getBiotype().equals(row.getBiotype())) {
			comp.setBackground(ExtendTableModel.COLOR_NONEDIT);
			return;
		}

		
		String value = (String) value1;		
		DataType datatype = mt.getDataType();
		boolean warning = false;
		boolean error = false;
		if(datatype==DataType.AUTO) {
			if(value!=null && value.length()>0) {
				warning = !DAOBiotype.getAutoCompletionFields(mt, null).contains(value);
			} else {	
				warning = false;
			}											
		} else if(datatype==DataType.LIST) {
			if(value!=null && value.length()>0) {
				error = !mt.extractChoices().contains(value);
			} else {
				error = false;
			}											
		} else if(datatype==DataType.NUMBER) {
			warning = value!=null && !ResultValue.isValidDouble(value);
		} else if(datatype==DataType.DATE) {
			warning = value!=null && !value.equals(Formatter.cleanDateTime(value));
		}
		if(warning) {
			comp.setForeground(LF.COLOR_WARNING_FOREGROUND);						
		} else if(error) {
			comp.setForeground(LF.COLOR_ERROR_FOREGROUND);												
		}
		
		
		if(!linker.isLinked() && getType()!=null && getType().isRequired()) {
			comp.setBackground(LF.BGCOLOR_REQUIRED);
		}
	}
	
	@Override
	public boolean isAutoWrap() {
		return true;
	}
	
	@Override
	public boolean mouseDoubleClicked(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		String v = (String) value;
		if(v!=null && (v.startsWith("http://") || v.startsWith("https://"))) {
			try{
				Desktop.getDesktop().browse(new URI(v));
				return true;
			} catch(Exception e) {
				JExceptionDialog.showError(e);
			}
			
		}
		return false;
	}
	
	@Override
	public void populateHeaderPopup(final AbstractExtendTable<Biosample> table, JPopupMenu popupMenu) {
		popupMenu.add(new JSeparator());
		popupMenu.add(new JCustomLabel("Sort", Font.BOLD));
		popupMenu.add(new AbstractAction("Sort by "+getShortName()) {
			@Override
			public void actionPerformed(ActionEvent e) {
				BiotypeMetadata mt = getType();
				DataType datatype = mt.getDataType();
				if(datatype==DataType.DATE) {
					table.sortBy(MetadataColumn.this, 0, new Comparator<Biosample>() {
						@Override
						public int compare(Biosample o1, Biosample o2) {
							return CompareUtils.DATE_COMPARATOR.compare(MetadataColumn.this.getValue(o1), MetadataColumn.this.getValue(o2));
						}
					});
				} else {
					table.sortBy(MetadataColumn.this, 0, new Comparator<Biosample>() {
						@Override
						public int compare(Biosample o1, Biosample o2) {
							return CompareUtils.STRING_COMPARATOR.compare(MetadataColumn.this.getValue(o1), MetadataColumn.this.getValue(o2));
						}
					});
				}
			}
		});
	}
	

}