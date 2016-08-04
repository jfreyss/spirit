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

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.util.Config;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

public class TestChoice extends JPanel {
	
	private final JGenericComboBox<String> testCategoryComboBox = new JGenericComboBox<String>();
	private final JGenericComboBox<Test> testComboBox = new JGenericComboBox<Test>();
	
	
	public TestChoice() {
		
		setOpaque(false);
		setLayout(new GridLayout());
		add(UIUtils.createHorizontalBox(testCategoryComboBox, testComboBox));
		
		testCategoryComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				populateTests();
			}
		});
		
		testComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String test = getSelection()==null? null: getSelection().getName();
				if(test!=null) {
					Spirit.getConfig().setProperty("test", test);
				}
			}
		});
		repopulate();
		reset();
	}
	
	public void repopulate() {
		testCategoryComboBox.setValues(DAOTest.getTestCategories(), true);
	}
	
	public void reset() {
		//Select the memorized selection
		Config config = Spirit.getConfig();
		String previousTest = config.getProperty("test", (String) null);
		if(previousTest!=null) {
			Test test = DAOTest.getTest(previousTest);
			setSelection(test);
		}		
	}
	
	private void populateTests() {
		//Populate the tests		
		Set<Test> tests = new TreeSet<Test>();
		for (Test test : DAOTest.getTests()) {
			if(test.getCategory()!=null && test.getCategory().equals(testCategoryComboBox.getSelection())) {
				tests.add(test);
			}
		}
		testComboBox.setValues(tests, true);
	}
	
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(320, 22);
	}
	
	public void addActionListener(ActionListener listener){
		testCategoryComboBox.addActionListener(listener);
		testComboBox.addActionListener(listener);
	}
	
	public void setSelection(Test test) {
		if(test!=null) {
			testCategoryComboBox.setSelection(test.getCategory());
			populateTests();
			testComboBox.setSelection(test);
		} else {
			testComboBox.setSelection(null);
			populateTests();
		}
	}
	
	public void setTestCategory(String n) {
		if(n==null) {
			testComboBox.setSelectedIndex(0);
		} else {
			testCategoryComboBox.setSelection(n);
		}
		populateTests();			
	}		
	
	public void setTestName(Test n) {
		if(n==null) testComboBox.setSelectedIndex(0);
		else testComboBox.setSelection(n);
	}
	
	public Test getSelection() {
		return testComboBox.getSelection();
	}
	
	
	@Override
	public void setEnabled(boolean enabled) {
		testCategoryComboBox.setEnabled(enabled);
		testComboBox.setEnabled(enabled);
	}
	@Override
	public boolean isEnabled() {
		return testCategoryComboBox.isEnabled();
	}
	
	
}
