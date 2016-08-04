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

package com.actelion.research.spiritapp.spirit.ui.biosample;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import com.actelion.research.spiritcore.business.biosample.Biosample;

public class BiosampleList extends JList<Biosample> {
	
	
	
	public BiosampleList() {
		super(new DefaultListModel<Biosample>());
		setCellRenderer(new DefaultListCellRenderer() {
			private SampleIdLabel sampleIdLabel = new SampleIdLabel();
			
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				sampleIdLabel.setBiosample((Biosample)value);
				sampleIdLabel.setForeground(getForeground());
				sampleIdLabel.setBackground(getBackground());
				sampleIdLabel.setBorder(getBorder());
				return sampleIdLabel;				
			}
		});
		
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	}
	
	public void setBiosamples(Collection<Biosample> rows) {
		DefaultListModel<Biosample> model = (DefaultListModel<Biosample>) getModel();
		model.clear();
		for (Biosample b : rows) {
			model.addElement(b);
		}
	}
	
	public List<Biosample> getSelection() {		
		List<Biosample> res = new ArrayList<Biosample>(getSelectedValuesList());
		return res;
	}
	
	public void setSelection(List<Biosample> sel) {
		getSelectionModel().setValueIsAdjusting(true);
		getSelectionModel().clearSelection();
		for (int i = 0; i < getModel().getSize(); i++) {
			Biosample b = (Biosample) getModel().getElementAt(i);
			if(sel!=null && sel.contains(b)) {
				getSelectionModel().addSelectionInterval(i, i);
				ensureIndexIsVisible(i);
			}
		}
		getSelectionModel().setValueIsAdjusting(false);
	}
	
	
	
	

}
