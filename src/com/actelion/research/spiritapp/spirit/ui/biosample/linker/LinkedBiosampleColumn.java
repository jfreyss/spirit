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

import java.util.Collections;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritapp.spirit.ui.biosample.dialog.BiosamplePopupDlg;
import com.actelion.research.spiritapp.spirit.ui.biosample.editor.BiosampleCellEditor;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;

public class LinkedBiosampleColumn extends AbstractLinkerColumn<Biosample> {

	private static SampleIdLabel sampleIdLabel = new SampleIdLabel(true, true);

	protected LinkedBiosampleColumn(BiosampleLinker linker) {
		super(linker, Biosample.class, 70, 250);
	}
	@Override
	public Biosample getValue(Biosample row) {
		row = linker.getLinked(row);
		if(row==null  || !row.getBiotype().equals(getType().getBiotype())) return null;
		Biosample res = row.getMetadataBiosample(getType());
		if(res==null) {
			res = new Biosample(row.getMetadataValue(getType()));
		}
		return res;
	}
	@Override
	public void setValue(Biosample row, Biosample value) {
		row.setMetadataBiosample(getType(), value);
	}

	@Override
	public void paste(Biosample row, String value) throws Exception {
		if(!isEditable(row)) return;
		if(value==null || value.length()==0) {
			setValue(row, null);
		} else {
			Biosample b = DAOBiosample.getBiosample(value);
			if(b==null) throw new Exception(value+" is not a valid sampleId");
			setValue(row, b);
		}
	}

	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		sampleIdLabel.setBiosample((Biosample) value);
		sampleIdLabel.setHighlight(false);
		return sampleIdLabel;
	}

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		String typeName = getType().getParameters();
		Biotype type = null;
		if(typeName!=null && typeName.length()>0) {
			type = DAOBiotype.getBiotype(typeName);
		}
		return new BiosampleCellEditor(type);
	}

	@Override
	public boolean mouseDoubleClicked(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		Biosample v = (Biosample) value;
		if(v!=null) {
			BiosamplePopupDlg.showBiosamples(Collections.singleton(v));
		}
		return false;
	}
}