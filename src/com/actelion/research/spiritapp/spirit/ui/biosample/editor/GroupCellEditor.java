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

package com.actelion.research.spiritapp.spirit.ui.biosample.editor;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.study.GroupLabel;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.ui.JTextComboBox;

public abstract class GroupCellEditor extends AbstractCellEditor implements TableCellEditor {

	private boolean allowTyping;
	private Study study;
	private final List<Group> groups = new ArrayList<>();
	private final JTextComboBox cb = new JTextComboBox() {
		GroupLabel gl = new GroupLabel();
		@Override
		public Component processCellRenderer(JLabel comp, String value, int index) {
			gl.setText(value, index>=1 && index-1<groups.size()? groups.get(index-1): null);
			return gl;
		}
	};


	public GroupCellEditor(boolean allowTyping) {
		this.allowTyping = allowTyping;
		cb.setAllowTyping(allowTyping);
		cb.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
	}


	public abstract Study getStudy(int row);

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		this.study = getStudy(row);
		groups.clear();
		List<String> choices = new ArrayList<>();
		if(study!=null) {
			groups.addAll(study.getGroups());
			for (Group g : groups) {
				choices.add(g.getBlindedName(SpiritFrame.getUsername()));
			}
		}
		cb.setChoices(choices);
		cb.setText(value==null?"": ((Group)value).getBlindedName(SpiritFrame.getUsername()));
		cb.selectAll();
		return cb;
	}

	@Override
	public Group getCellEditorValue() {
		if(cb.getText().length()==0) return null;

		for (Group group : groups) {
			if(group.getName().equals(cb.getText())) {
				return group;
			}
		}
		if(allowTyping) {
			Group group = new Group(cb.getText());
			group.setStudy(study);
			return group;
		}
		return null;
	}
}