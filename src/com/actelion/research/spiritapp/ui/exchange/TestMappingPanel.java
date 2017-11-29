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

package com.actelion.research.spiritapp.ui.exchange;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping.EntityAction;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

public class TestMappingPanel extends JPanel implements IMappingPanel {
	private Test test;
	
	private ImporterDlg dlg;
	
	private final MappingPanel testMappingPanel;
	private final JGenericComboBox<Test> testComboBox;
	
	//
	private final JPanel centerPanel = new JPanel();
	private final List<MappingPanel> attributeMappingPanels = new ArrayList<>();
	private final List<JGenericComboBox<TestAttribute>> attributeComboboxes = new ArrayList<>();
	private List<TestAttribute> attributes = new ArrayList<>();
	
	public TestMappingPanel(ImporterDlg dlg, Test fromTest) {
		super(new BorderLayout());
		this.dlg = dlg;
		this.test = fromTest;
		setMinimumSize(new Dimension(200, 200));

		List<Test> possibleMatches = DAOTest.getTests();
		 
		if(dlg.getExchange()!=null && dlg.getExchange().getResults().size()>0) {
			//Find attribute to be skipped
			attribute: for (TestAttribute ta : test.getAttributes()) {
				assert ta!=null: test + " has null metadata: " + test.getAttributes();
				for (Result r : dlg.getExchange().getResults()) {
					assert r.getTest()!=null;
					if(r.getTest().equals(ta.getTest()) && r.getResultValue(ta)!=null && (r.getResultValue(ta).getValue()!=null && r.getResultValue(ta).getValue().length()>0)) {
						//Don't skip this attribute, because one value is not empty
						attributes.add(ta);
						continue attribute;
					}					
				}
			}
		} else {
			//Kepp all attributes
			attributes.addAll(fromTest.getAttributes());
		}

		//Init components
		testComboBox = new JGenericComboBox<Test> (possibleMatches, "Map to...");
		testMappingPanel = new MappingPanel(testComboBox);
		testMappingPanel.addPropertyChangeListener(MappingPanel.PROPERTY_ACTION, new PropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				updateLayout();
			}
		});
		if(possibleMatches.size()==0) testMappingPanel.setMappingAction(EntityAction.CREATE);

		testComboBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				Test toTest = testComboBox.getSelection();
				for (int index = 0; index < attributes.size(); index++) {
					TestAttribute ta = attributes.get(index);
					JGenericComboBox<TestAttribute> comboBox = null;
					if(toTest!=null) {
						comboBox = new JGenericComboBox<>(toTest.getAttributes(), "Map to...");						
						comboBox.setSelectionString(ta.getName());
					}
					attributeMappingPanels.get(index).setMappingComponent(comboBox);
					if(comboBox!=null && comboBox.getSelectedIndex()>0) {
						attributeMappingPanels.get(index).setMappingAction(EntityAction.MAP_REPLACE);
						attributeMappingPanels.get(index).setCreationEnabled(false);
					} else {
						attributeMappingPanels.get(index).setCreationEnabled(true);
					}
				}

			}
		});
		
		for (int index = 0; index < attributes.size(); index++) {
			attributeMappingPanels.add(new MappingPanel(null));
			attributeComboboxes.add(new JGenericComboBox<TestAttribute>());
		}
		
		//Preselection
		testComboBox.setSelectionString(test.getName());
		if(testComboBox.getSelectedIndex()>0) {
			testMappingPanel.setMappingAction(EntityAction.MAP_REPLACE);
			testMappingPanel.setCreationEnabled(false);
		} else {
			testMappingPanel.setCreationEnabled(true);
		}
		
		//Init Layout
		JPanel mapTestPanel = UIUtils.createHorizontalBox(testMappingPanel, Box.createHorizontalGlue());
		mapTestPanel.setOpaque(false);
		add(BorderLayout.NORTH, mapTestPanel);
		JPanel panel = UIUtils.createHorizontalBox(centerPanel, Box.createGlue());
		panel.setOpaque(true);
		setOpaque(false);
		
		add(BorderLayout.CENTER, new JScrollPane(panel));
		
		updateView();
	}
	
	private void updateLayout() {
		
		centerPanel.removeAll();
		List<JComponent> formComponents = new ArrayList<>();
		EntityAction action = testMappingPanel.getMappingAction();
		if(action!=EntityAction.SKIP) {
			for (int index = 0; index < attributes.size(); index++) {
				TestAttribute m = attributes.get(index);
				formComponents.add(new JLabel("<html>Attribute '<b>"+m.getName()+"</b>': "));
				formComponents.add(attributeMappingPanels.get(index));
				if(action==EntityAction.CREATE) {
					attributeMappingPanels.get(index).setMappingAction(EntityAction.CREATE);
					attributeMappingPanels.get(index).setMappingComponent(null);
				}
			}
		}
		
		centerPanel.add(UIUtils.createHorizontalBox(UIUtils.createTable(formComponents), Box.createHorizontalGlue()));
		
		centerPanel.revalidate();
		
	}
	
	public void updateView() {
		ExchangeMapping mapping = dlg.getMapping();
		testMappingPanel.setMappingAction(mapping.getTest2action().get(test.getName()));
		testComboBox.setSelection(mapping.getTest2mappedTest().get(test.getName()));
		updateLayout();		
	}
	
	@SuppressWarnings("unchecked")
	public void updateMapping() {
		ExchangeMapping mapping = dlg.getMapping();
		mapping.getTest2action().put(test.getName(), testMappingPanel.getMappingAction());
		mapping.getTest2mappedTest().put(test.getName(), testComboBox.getSelection());
		
		for (int i = 0; i < attributes.size(); i++) {
			TestAttribute m = attributes.get(i);
			MappingPanel mappingPanel = attributeMappingPanels.get(i);
			mapping.getTestAttribute2mappingAction().put(new com.actelion.research.spiritcore.util.Pair<String, String>(test.getName(), m.getName()), mappingPanel.getMappingAction());
			
			if(mappingPanel.getMappingComponent()!=null && mappingPanel.getMappingComponent() instanceof JGenericComboBox) {
				JGenericComboBox<TestAttribute> combobox = ((JGenericComboBox<TestAttribute>) mappingPanel.getMappingComponent());
				if(combobox.getSelection()!=null) {
					mapping.getTestAttribute2mappedTestAttribute().put(new com.actelion.research.spiritcore.util.Pair<String, String>(test.getName(), m.getName()), combobox.getSelection());				
				}
			}
		}
	}
	
}
