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

package com.actelion.research.spiritapp.ui.biosample.column;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritapp.ui.biosample.edit.EditBiosampleTableModel;
import com.actelion.research.spiritapp.ui.biosample.editor.BiosampleCellEditor;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class ParentBiosampleColumn extends Column<Biosample, Biosample> {

	private EditBiosampleTableModel model;
	private static SampleIdLabel sampleIdLabel = new SampleIdLabel();

	public ParentBiosampleColumn(EditBiosampleTableModel model) {
		super((model==null || model.getBiotype()==null || model.getBiotype().getCategory()!=BiotypeCategory.PURIFIED || model.getBiotype().getParent()==null?"": model.getBiotype().getParent().getName()) + "\nParentId", Biosample.class, 90, 200);
		this.model = model;
	}

	@Override
	public float getSortingKey() {return 4.4f;}

	@Override
	public Biosample getValue(Biosample row) {
		return row.getParent();
	}

	@Override
	public void setValue(Biosample row, Biosample value) {
		assert model!=null;

		if(value==null || value.getSampleId()==null || value.getSampleId().length()==0) {
			//Set Parent to null if value is null
			row.setParent(null);
		} else if(value.getId()>0) {
			row.setParent(value);
		} else {
			Biosample parent = null;
			//Find if the parent in present in an other row
			for (Biosample r : model.getRows()) {
				if(r.getSampleId().equals(value.getSampleId())) {
					parent = r;
					break;
				}
			}

			//Otherwise load the sample from the DB by sampleId or then by study/name
			if(parent==null) {
				parent = DAOBiosample.getBiosample(value.getSampleId());
			}
			if(parent==null) {
				parent = DAOBiosample.getBiosample(row.getInheritedStudy(), value.getSampleId());
			}
			if(parent==null) {
				parent = value;
			}
			row.setParent(parent);
		}
		if(row.getParent()!=null && row.getParent().getInheritedStudy()!=null) {
			row.setAttachedStudy(null);
		}
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
		return model!=null;
	}

	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		sampleIdLabel.setBiosample((Biosample)value);
		sampleIdLabel.setError(!SpiritRights.canEdit((Biosample)value, SpiritFrame.getUser()));
		sampleIdLabel.setHighlight(false);
		return sampleIdLabel;
	}

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		Biotype forced = null;
		if(model!=null && model.getBiotype()!=null && model.getBiotype().getCategory()==BiotypeCategory.PURIFIED && model.getBiotype().getParent()!=null) {
			forced = model.getBiotype().getParent();
		}

		return new BiosampleCellEditor(forced);
	}
}