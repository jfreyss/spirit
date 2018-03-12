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

package com.actelion.research.spiritapp.ui.result;

import java.awt.Component;
import java.util.Collection;

import javax.swing.JLabel;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.util.Config;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.JObjectComboBox;

public class TestComboBox extends JObjectComboBox<Test> {


	private boolean showHidden;

	public TestComboBox() {
		this(false);
	}

	public TestComboBox(boolean showHidden) {
		this.showHidden = showHidden;
		addActionListener(e-> {
			String test = getSelection()==null? null: getSelection().getName();
			if(test!=null) {
				Spirit.getConfig().setProperty("test", test);
			}
		});
		reset();
	}

	@Override
	public Component processCellRenderer(JLabel comp, String name, int index) {
		Test t = getMap().get(name);
		if(t!=null) {
			comp.setText("<html><table><tr style='white-space:nowrap'><td style='width:150px'>" + MiscUtils.convert2Html(t.getName()) + "</td><td style='color:gray; font-size:90%; text-align:right'>" + MiscUtils.removeHtml(t.getCategory()) + "</td></tr></table> ");
		} else {
			comp.setText("");
		}
		return comp;
	}


	@Override
	public Collection<Test> getValues() {
		return DAOTest.getTests(showHidden);
	}

	/**
	 * Select the memorized selection
	 */
	public void reset() {
		Config config = Spirit.getConfig();
		String previousTest = config.getProperty("test", (String) null);
		setText(previousTest);
	}

	@Override
	public String convertObjectToString(Test obj) {
		return obj==null?"": obj.getName();
	}

}
