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

package com.actelion.research.spiritapp.spirit.ui.exchange;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.actelion.research.spiritcore.services.exchange.ExchangeMapping.EntityAction;
import com.actelion.research.util.ui.UIUtils;

public class MappingPanel extends JPanel {
	public static final String PROPERTY_ACTION = "changed";

	private final JToggleButton b1 = new JToggleButton("Create");
	private final JToggleButton b2 = new JToggleButton("Ignore");
	private final JToggleButton b3 = new JToggleButton("Map");
	private final JPanel mappingPanel = new JPanel(new GridLayout());
	private JComponent mappingComponent;

	public MappingPanel(final JComponent mappingChoices) {
		super(new BorderLayout());
		setOpaque(false);
		b1.setOpaque(false);
		b2.setOpaque(false);
		b3.setOpaque(false);
		mappingPanel.setOpaque(false);
		
		
		ButtonGroup group = new ButtonGroup();
		group.add(b1);
		group.add(b2);
		group.add(b3);
		
				
//		add(BorderLayout.CENTER, UIUtils.createHorizontalBox(b1, b2, b3, mappingPanel));
//		add(BorderLayout.EAST, Box.createHorizontalGlue());
		add(BorderLayout.WEST, UIUtils.createHorizontalBox(b1, b2, b3));
		add(BorderLayout.CENTER, mappingPanel);
		
		setMappingComponent(mappingChoices);
		b2.setSelected(true);


		ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mappingPanel.setVisible(MappingPanel.this.mappingComponent!=null && b3.isSelected());
				firePropertyChange(PROPERTY_ACTION, null, "");
			}
		};
		b1.addActionListener(al);
		b2.addActionListener(al);
		b3.addActionListener(al);
		
	}
	
	public void setMappingComponent(JComponent mappingComponent) {
		this.mappingComponent = mappingComponent;
		mappingPanel.removeAll();
		if(MappingPanel.this.mappingComponent!=null) {
			mappingPanel.add(UIUtils.createHorizontalBox(new JLabel(" to "), mappingComponent));
			mappingPanel.setVisible(b3.isSelected());
		}
		mappingPanel.revalidate();
		
		//Allow Map Button or not
		if(mappingComponent==null) {
			b3.setVisible(false);
		} else {
			mappingComponent.setOpaque(false);
			b3.setVisible(true);
			mappingPanel.setVisible(b3.isSelected());
		}		
	}
	
	public void setMappingAction(EntityAction action) {
		if(action==null) {
			b1.setSelected(true);
			return;
		}
		switch (action) {
		case CREATE:
			if(b1.isEnabled()) b1.setSelected(true);
			break;
		case SKIP:
			if(b2.isEnabled()) b2.setSelected(true);
			break;
		case MAP_REPLACE:
			if(b3.isEnabled()) b3.setSelected(true);
			break;
		}
		if(mappingComponent!=null) mappingPanel.setVisible(b3.isSelected());
	}
	
	public EntityAction getMappingAction() {
		if(b1.isSelected()) return EntityAction.CREATE;
		if(b2.isSelected()) return EntityAction.SKIP;
		return EntityAction.MAP_REPLACE;
	}
	
	public void setCreationEnabled(boolean enabled) {
		b1.setEnabled(enabled);
		if(!enabled && b1.isSelected()) b2.setSelected(true);
	}
	
	public JComponent getMappingComponent() {
		return mappingComponent;
	}
}
