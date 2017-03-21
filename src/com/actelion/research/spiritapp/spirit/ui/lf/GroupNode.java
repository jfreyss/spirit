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

package com.actelion.research.spiritapp.spirit.ui.lf;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.Strategy;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.TextComboBoxOneNode;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.ui.UIUtils;


public class GroupNode extends TextComboBoxOneNode {
	private Study study;
	public GroupNode(FormTree tree, Strategy<String> strategy) {
		super(tree, "Group", strategy);		
		
		getComponent().setListCellRenderer(new DefaultListCellRenderer() {
			
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {			
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if(study!=null) {
					Group g = study.getGroup((String) value);
					if(g!=null && g.getColor()!=null) {
						setBackground(UIUtils.getDilutedColor(g.getColor(), getBackground()));
					}
						
				}
				return this;
			}
		});
	}
	
	@Override
	public Collection<String> getChoices() {
		List<String> list = new ArrayList<String>();
		if(study!=null) {
			for (Group group : study.getGroups()) {
				list.add(group.getName());
			}
		}		
		return list;
	}
	
	public void setStudy(Study study) {
		this.study = study;
		getComponent().setEnabled(!SpiritRights.isBlindAll(study, SpiritFrame.getUser()));
		getComponent().setText("");		
		setVisible(study!=null);
	}
	
	
	
}