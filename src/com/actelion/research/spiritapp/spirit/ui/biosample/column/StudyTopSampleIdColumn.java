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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import com.actelion.research.spiritapp.spirit.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class StudyTopSampleIdColumn extends Column<Biosample, Biosample> {
	
	private static SampleIdLabel sampleIdLabel = new SampleIdLabel();
	
	public StudyTopSampleIdColumn() {
		super("\nTopId", Biosample.class, 90, 200);	
	}
	
	@Override
	public String getCategory() {
		return "Top";
	}
	
	
	@Override
	public float getSortingKey() {return 3.9f;}
	
	@Override
	public Biosample getValue(Biosample row) {
		if(row==null) return null;
		return row.getTopParent();
	}

	@Override
	public boolean isEditable(Biosample row) {return false;}
	
	@Override
	public String getToolTipText() {return "The TopParent Id";}	
	
	@Override
	public void populateHeaderPopup(final AbstractExtendTable<Biosample> table, JPopupMenu popupMenu) {
		popupMenu.add(new JSeparator());
		popupMenu.add(new JCustomLabel("Sort", Font.BOLD));

		popupMenu.add(new AbstractAction("Sort by SampleId") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Collections.sort(table.getModel().getRows(), new Comparator<Biosample>() {
					@Override
					public int compare(Biosample o1, Biosample o2) {
						return CompareUtils.compare(o1.getTopParent().getSampleId(), o2.getTopParent().getSampleId());
					}
				});
				table.getModel().fireTableDataChanged();
			}
		});
		popupMenu.add(new AbstractAction("Sort by SampleName") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Collections.sort(table.getModel().getRows(), new Comparator<Biosample>() {
					@Override
					public int compare(Biosample o1, Biosample o2) {
						return CompareUtils.compare(o1.getTopParent().getSampleName(),o2.getTopParent().getSampleName());
					}
				});
				table.getModel().fireTableDataChanged();
			}
		});
	}
	
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		sampleIdLabel.setBiosample((Biosample)value);			
		sampleIdLabel.setHighlight(false);
		return sampleIdLabel;
	}
	
	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		comp.setBackground(LF.BGCOLOR_LINKED);
		comp.setForeground(LF.FGCOLOR_LINKED);
	}
}