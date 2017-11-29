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

package com.actelion.research.spiritapp.ui.biosample.column;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.biosample.edit.EditBiosampleTable;
import com.actelion.research.spiritapp.ui.biosample.editor.MetadataFullCellEditor;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerMethod;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.JMapLabelNoRepaint;

public class CombinedColumn extends Column<Biosample, CombinedColumnMap> {

	private final JMapLabelNoRepaint comp = new JMapLabelNoRepaint();

	public CombinedColumn() {
		super("Sample\nMetadata", CombinedColumnMap.class, 60, 420);
	}


	@Override
	public float getSortingKey() {
		return 6.99f;
	}


	@Override
	public CombinedColumnMap getValue(Biosample row) {
		CombinedColumnMap map = new CombinedColumnMap();
		if(row.getBiotype()!=null) map.put("Biotype", row.getBiotype().getName());
		for(BiosampleLinker linker: BiosampleLinker.getLinkers(row, LinkerMethod.DIRECT_LINKS) ) {
			if(linker.getType()==LinkerType.SAMPLEID) continue;
			if(linker.getBiotypeMetadata()!=null && linker.getBiotypeMetadata().isSecundary()) continue;
			if(linker.getAggregatedMetadata()!=null && linker.getAggregatedMetadata().isSecundary()) continue;

			String value = linker.getValue(row);
			if(value==null || value.length()==0) continue;
			map.put(linker.getLabelShort(), value);
		}
		if(map.size()==0) return null;
		return map;
	}

	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		CombinedColumnMap map = value==null? getValue(row): (CombinedColumnMap) value;
		comp.setOpaque(true);
		comp.setMap(map);
		return comp;
	}

	@Override
	public void setValue(Biosample row, CombinedColumnMap value) {
		//Done by the custom editor
	}

	@Override
	public boolean isEditable(Biosample row) {return true;}

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		return new MetadataFullCellEditor((EditBiosampleTable) table);
	}

}