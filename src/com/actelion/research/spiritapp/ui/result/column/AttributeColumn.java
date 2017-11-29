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

package com.actelion.research.spiritapp.ui.result.column;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritapp.ui.biosample.editor.AutoCompletionCellEditor;
import com.actelion.research.spiritapp.ui.biosample.editor.DateStringCellEditor;
import com.actelion.research.spiritapp.ui.biosample.editor.MetadataComboboxCellEditor;
import com.actelion.research.spiritapp.ui.biosample.editor.MultiComboboxCellEditor;
import com.actelion.research.spiritapp.ui.result.edit.BiosampleIdCellEditor;
import com.actelion.research.spiritapp.ui.util.lf.LF;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.AlphaNumericalCellEditor;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class AttributeColumn extends Column<Result, String> {
	
	private TestAttribute att;

	private SampleIdLabel sampleIdLabel = new SampleIdLabel();
	
	public AttributeColumn(final TestAttribute att) {
		super(att.getName(), String.class, att.getDataType()==DataType.BIOSAMPLE? 105: 70);
		this.att = att;
	}
	
	@Override
	public float getSortingKey() {
		return 6f + att.getOutputType().ordinal()*.5f;
	}
	
	@Override
	public String getValue(Result row) {
		if(row.getResultValue(att)==null) return "Invalid attribute??";
		return row.getResultValue(att).getValue();
	}
	@Override
	public void setValue(Result row, String value) {
		try {
			if(att!=null && att.getDataType()==DataType.D_FILE) {
				throw new Exception("Cannot copy a document");
			}
			row.setValue(att, value);		
			if(att!=null && att.getDataType()==DataType.BIOSAMPLE) {
				if(value==null || value.length()==0) {
					row.getResultValue(att).setLinkedBiosample(null);
				} else {
					Biosample b = DAOBiosample.getBiosample(value);
					if(b==null) b = new Biosample(value);
					row.getResultValue(att).setLinkedBiosample(b);					
				}
			}
		} catch(Exception e) {
			JExceptionDialog.showError(e);

		}
	}

	public TestAttribute getAttribute() {
		return att;
	}
	
		
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
		if(att.getDataType()==DataType.BIOSAMPLE) {
			Biosample b = row.getResultValue(att).getLinkedBiosample();
			if(b!=null) {
				sampleIdLabel.setBiosample(b);
			} else {
				sampleIdLabel.setBiosample(new Biosample((String)value));
			}
			return sampleIdLabel;
		} else {
			return super.getCellComponent(table, row, rowNo, value);
		}
	}
	
	@Override
	public void postProcess(AbstractExtendTable<Result> table, Result row, int rowNo, Object value, JComponent comp) {
		TestAttribute ta = getAttribute();
		DataType datatype = ta.getDataType();
		//Check value is correct
		boolean warning = false;
		if(datatype==DataType.AUTO) {
			String sel = (String) value;
			if(sel!=null && sel.length()>0) {
				warning = true;
				for (String string : DAOTest.getAutoCompletionFields(ta)) {
					if(sel.equals(string)) {warning = false; break;}
				}
			} else {	
				warning = false;
			}						
		} else if(datatype==DataType.NUMBER) {
			String sel = (String) value;
			warning = sel!=null && !ResultValue.isValidDouble(sel);
		} else if(datatype==DataType.DATE) {
			String sel = (String) value;
			warning = sel!=null && !sel.equals(FormatterUtils.cleanDateTime(sel));
		}
		
		
		comp.setForeground(warning? LF.COLOR_WARNING_FOREGROUND: comp.getForeground());
		comp.setBackground(ta.isRequired()? LF.BGCOLOR_REQUIRED: comp.getBackground());
		

		if(datatype==DataType.NUMBER) {
			((JLabelNoRepaint) comp).setHorizontalAlignment(JLabel.RIGHT);
		} else if(datatype==DataType.FORMULA) {
			comp.setForeground(Color.BLUE);
		}
	}
	
	@Override
	public boolean isEditable(Result row) {
		return att.getDataType()!=DataType.FORMULA;
	}
	
	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Result> table) {		
		switch (att.getDataType()) {
		case BIOSAMPLE:
			Biotype biotype = null;
			if(att.getParameters()!=null && att.getParameters().length()>0) {
				biotype = DAOBiotype.getBiotype(att.getParameters());
				if(biotype==null) System.err.println("Invalid biotype for attribute "+att+" : "+att.getParameters());
			}
			return new BiosampleIdCellEditor(biotype);
		case ALPHA:
			return new AlphaNumericalCellEditor();	
		case NUMBER:
			return new AlphaNumericalCellEditor();
		case AUTO:
			return new AutoCompletionCellEditor(att);
		case LIST:
			return new MetadataComboboxCellEditor(att.getParameters());
		case MULTI:
			return new MultiComboboxCellEditor(att.getParameters());
		case FORMULA:
			return null;
		case DATE:
			return new DateStringCellEditor();
		default:
			throw new IllegalArgumentException("Invalid datatype, no editor for: "+att.getDataType());
		}			
	}
	
}