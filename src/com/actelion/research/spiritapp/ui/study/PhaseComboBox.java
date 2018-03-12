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

package com.actelion.research.spiritapp.ui.study;

import java.awt.Component;
import java.util.Collection;
import java.util.Date;

import javax.swing.JLabel;

import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JObjectComboBox;

public class PhaseComboBox extends JObjectComboBox<Phase> {

	private PhaseLabel phaseLabel = new PhaseLabel();

	public PhaseComboBox() {
		this("Phase...");
	}

	public PhaseComboBox(String label) {
		setTextWhenEmpty(label);
	}

	public PhaseComboBox(Collection<Phase> phases) {
		this();
		setValues(phases);
	}

	public PhaseComboBox(Collection<Phase> phases, String label) {
		this();
		setTextWhenEmpty(label);
		setValues(phases);
	}

	public void selectCurrentPhase() {
		Date now = JPAUtil.getCurrentDateFromDatabase();
		for (Phase phase : getValues()) {
			if(Phase.isSameDay(phase.getAbsoluteDate(), now)) {
				setSelection(phase);
				return;
			}
		}
	}

	@Override
	public Component processCellRenderer(JLabel comp, String object, int index) {
		Phase phase = getMap().get(object);
		phaseLabel.setPhase(phase);
		phaseLabel.setBackground(comp.getBackground());
		phaseLabel.setForeground(comp.getForeground());
		phaseLabel.setBorder(comp.getBorder());
		return phaseLabel;
	}


	@Override
	public String convertObjectToString(Phase obj) {
		return obj==null?"": obj.getName();
	}

}
