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

package com.actelion.research.spiritapp.spirit.ui.util.lf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.actelion.research.spiritapp.spirit.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.Strategy;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.TextComboBoxNode;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;

public class PhaseNode extends TextComboBoxNode {
	private Study study;
	
	public PhaseNode(FormTree tree, final Strategy<String> strategy) {
		super(tree, "Phases", true, strategy);		
	}
	
	@Override
	public Collection<String> getChoices() {
		List<String> list = new ArrayList<String>();
		if(study!=null) {
			for (Phase phase : study.getPhases()) {
				list.add(phase.getShortName());
			}
		}		
		return list;
	}
	
	public void setStudy(Study study) {
		this.study = study;
		getComponent().setText("");
		setVisible(study!=null);
	}
	
}