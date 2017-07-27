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
import javax.swing.JPopupMenu;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.biosample.editor.LocationCellEditor;
import com.actelion.research.spiritapp.spirit.ui.location.ContainerLabel;
import com.actelion.research.spiritapp.spirit.ui.location.ContainerLabel.ContainerDisplayMode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.LocationFormat;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class ContainerLocationPosColumn extends Column<Biosample, String> {
	
	private static ContainerLabel containerLabel = new ContainerLabel(ContainerDisplayMode.NAME_POS);
	
	public ContainerLocationPosColumn() {
		super("Container\nLocation", String.class, 50);
	}
		
	@Override
	public float getSortingKey() {
		return 2.31f;
	}
	@Override
	public String getValue(Biosample row) {
		if(row==null || row.getLocation()==null) return null;
		return row.getLocationString(LocationFormat.FULL_POS, SpiritFrame.getUser());
	}
	
	@Override
	public void setValue(Biosample row, String value) {
		try {
			paste(row, value);
		} catch (Exception e) {
			JExceptionDialog.showError(e);
		}
	}		
	
	@Override
	public boolean isEditable(Biosample row) {
		if(row==null) return false;
		Location location = row.getLocation();
		if(row.getBiotype()!=null && row.getBiotype().isAbstract()) return false;
		if(location!=null && !SpiritRights.canEdit(location, SpiritFrame.getUser())) return false;
		return true;
	}

	@Override
	public void paste(Biosample row, String value) throws Exception {
		DAOLocation.updateLocation(row, value, SpiritFrame.getUser());						
	}
	
	
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample b, int rowNo, Object value) {
		containerLabel.setBiosample(b);
		return containerLabel;
	}
	
	
	@Override
	public void populateHeaderPopup(final AbstractExtendTable<Biosample> table, JPopupMenu popupMenu) {
//		super.populateHeaderPopup(table, popupMenu);		
		ContainerFullColumn.populateLocationHeaderPopupStatic(this, table, popupMenu);
	}
	
	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		return new LocationCellEditor();
	}
	
	@Override
	public boolean isAutoWrap() {
		return false;
	}
	
	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		comp.setBackground(LF.BGCOLOR_LOCATION);
	}

	
}