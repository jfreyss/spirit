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

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.container.ContainerLabel;
import com.actelion.research.spiritapp.spirit.ui.container.ContainerLabel.ContainerDisplayMode;
import com.actelion.research.spiritapp.spirit.ui.container.ContainerTypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ObjectComboBoxCellEditor;

public class ContainerTypeColumn extends Column<Biosample, ContainerType> {

	private static ContainerLabel containerLabel = new ContainerLabel(ContainerDisplayMode.CONTAINER_TYPE);

	public ContainerTypeColumn() {
		super("Container\nType", ContainerType.class, 40, 70);
	}
	@Override
	public String getCategory() {
		return "Container";
	}

	@Override
	public float getSortingKey() {return 2.1f;}


	@Override
	public ContainerType getValue(Biosample row) {
		return row.getContainerType();
	}
	@Override
	public void setValue(Biosample row, ContainerType value) {
		row.setContainerType(value);
	}
	@Override
	public boolean isEditable(Biosample row) {
		if(row!=null && (row.getBiotype()==null || row.isAbstract())) return false;
		return true;
	}

	@Override
	public void paste(Biosample row, String value) throws Exception {
		ContainerType type = ContainerType.get(value);
		setValue(row, type);
	}

	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample r, int rowNo, Object value) {
		containerLabel.setBiosample(r);
		return containerLabel;
	}

	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		comp.setForeground(Color.GRAY);
		comp.setBackground(LF.BGCOLOR_LOCATION);
	}

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		return new ObjectComboBoxCellEditor<ContainerType>(new ContainerTypeComboBox());
	}

}