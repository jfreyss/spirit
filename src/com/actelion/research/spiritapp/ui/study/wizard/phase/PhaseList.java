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

package com.actelion.research.spiritapp.ui.study.wizard.phase;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;

import com.actelion.research.spiritapp.ui.study.PhaseLabel;
import com.actelion.research.spiritcore.business.study.Phase;

public class PhaseList extends JList<Phase> {

	private final DefaultListModel<Phase> model = new DefaultListModel<>();

	public PhaseList() {
		super();
		
		setModel(model);
		setCellRenderer(new DefaultListCellRenderer() {
			PhaseLabel lbl = new PhaseLabel();
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				lbl.setPhase((Phase)value);
				lbl.setBackground(getBackground());
				lbl.setForeground(getForeground());
				lbl.setBorder(getBorder());
				return lbl;
			}
		});				
	}
	
	public void setSelectedPhases(Collection<Phase> phases) {
		Set<Phase> toSelect = new HashSet<>(phases);
		for(int i=0; i<model.getSize(); i++) {
			if(toSelect.contains(model.get(i))) {
				getSelectionModel().addSelectionInterval(i, i);
			}
		}
	}
	
	public List<Phase> getSelectedPhases() {
		List<Phase> res = new ArrayList<>();
		for(int i: getSelectedIndices()) {
			res.add(model.get(i));
		}
		return res;
	}
	
	/**
	 * Set the phases of the model, without changing the selection
	 * @param phases
	 */
	public void setPhases(Collection<Phase> phases) {
		List<Phase> sel = getSelectedPhases();
		model.clear();
		if(phases!=null) {
			for(Phase phase: phases) {
				model.addElement(phase);
			}
		}
		setSelectedPhases(sel);
	}
	
	
}
