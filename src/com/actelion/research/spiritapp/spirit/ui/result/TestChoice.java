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

package com.actelion.research.spiritapp.spirit.ui.result;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.util.Config;
import com.actelion.research.util.ui.JTextComboBox;

public class TestChoice extends JTextComboBox {
	private Map<String, Test> map = new HashMap<>();

	public TestChoice() {

		setOpaque(false);
		setAllowTyping(false);

		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String test = getSelection()==null? null: getSelection().getName();
				if(test!=null) {
					Spirit.getConfig().setProperty("test", test);
				}
			}
		});
		setListCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				String v = (String) value;
				Test t = map.get(v);
				if(t!=null) {
					setText("<html><span style='color:gray'>" + t.getCategory() + "</span> " + t.getName());
				}
				return this;
			}
		});
		reset();
	}

	@Override
	public Collection<String> getChoices() {
		List<Test> tests = DAOTest.getTests();
		map = Test.mapName(tests);
		return map.keySet();
	}

	/**
	 * Select the memorized selection
	 */
	public void reset() {
		Config config = Spirit.getConfig();
		String previousTest = config.getProperty("test", (String) null);
		setText(previousTest);
	}

	public void setSelection(Test test) {
		if(test!=null) {
			setText(test.getName());
		} else {
			setText("");
		}
	}

	public Test getSelection() {
		return DAOTest.getTest(getText());
	}


}
