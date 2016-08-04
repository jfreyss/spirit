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

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritapp.spirit.ui.biosample.edit.EditBiosampleTableModel;
import com.actelion.research.spiritapp.spirit.ui.biosample.editor.BiosampleCellEditor;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class ParentBiosampleColumn extends Column<Biosample, Biosample> {
	private EditBiosampleTableModel model;
	
	public ParentBiosampleColumn(EditBiosampleTableModel model) {
		super((model.getBiotype()==null || model.getBiotype().getCategory()!=BiotypeCategory.PURIFIED || model.getBiotype().getParent()==null?"Sample": model.getBiotype().getParent().getName()) + "\nParentId", Biosample.class, 90, 200);
		this.model = model;
	}
	
	@Override
	public float getSortingKey() {return 3.85f;}
	
	@Override
	public Biosample getValue(Biosample row) {	
		return row.getParent();
	}
	
	@Override
	public void setValue(Biosample row, Biosample value) {
		
		//Empty?
		if(value==null || value.getSampleId()==null || value.getSampleId().length()==0) {
			row.setParent(null);			
			return;
		}
		
		//Find parent in current rows
		for (Biosample b : model.getRows()) {
			if(b.getSampleId().equals(value.getSampleId())) {
				row.setParent(b);
				return;
			}
		}
		
		//Otherwise load the sample from the DB
		Biosample b = DAOBiosample.getBiosample(value.getSampleId());
		if(b!=null) value = b;
		row.setParent(value);
	}
	
	@Override
	public void paste(Biosample row, String value) throws Exception {
		if(value==null || value.length()==0) {
			setValue(row, null);
		} else {
			setValue(row, new Biosample(value));
		}
	}

	@Override
	public boolean isEditable(Biosample row) {
		return true;
	}
	
	private static SampleIdLabel sampleIdLabel = new SampleIdLabel();
	
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		sampleIdLabel.setBiosample((Biosample)value);
		sampleIdLabel.setError(!SpiritRights.canEdit((Biosample)value, Spirit.getUser()));
		sampleIdLabel.setHighlight(false);
		return sampleIdLabel;		
	}
	
	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		Biotype forced = null;
		if(model.getBiotype()!=null && model.getBiotype().getCategory()==BiotypeCategory.PURIFIED && model.getBiotype().getParent()!=null) {
			forced = model.getBiotype().getParent();	
		}
		return new BiosampleCellEditor(forced);
	}
}