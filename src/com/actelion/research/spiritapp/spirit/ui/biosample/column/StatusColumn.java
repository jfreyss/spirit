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
import javax.swing.SwingConstants;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class StatusColumn extends Column<Biosample, Status> {

	public StatusColumn() {
		super("Status", Status.class, 50, 100);
		lbl.setFont(FastFont.SMALL);
	}
	
	@Override
	public float getSortingKey() {
		return 9.6f;
	}
	
	@Override
	public Status getValue(Biosample row) {
		return row.getStatus();
	}		
	@Override
	public boolean isEditable(Biosample row) {
		return false;
	}
	
	private static JLabelNoRepaint lbl = new JLabelNoRepaint();
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		Status status = (Status) value;
		lbl.setVerticalAlignment(SwingConstants.TOP);
		if(status==null) {
			lbl.setText(null);
			lbl.setBackground(Color.WHITE);
		} else {
			lbl.setText(status.getName());
			lbl.setForeground(status.getForeground());
			lbl.setBackground(status.getBackground());
			
		}
		return lbl;
	}
	
	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		super.postProcess(table, row, rowNo, value, comp);
		if(comp instanceof JLabelNoRepaint) ((JLabelNoRepaint)comp).setVerticalAlignment(SwingConstants.TOP);
		
		Status status = (Status) value;
		if(status!=null) {
			if(status.getForeground()!=null) lbl.setForeground(status.getForeground());
			if(status.getBackground()!=null) lbl.setBackground(status.getBackground());
			
		}
	}
	
}