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

package com.actelion.research.spiritapp.slidecare.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.slide.ContainerTemplate;
import com.actelion.research.spiritcore.business.slide.SampleDescriptor;
import com.actelion.research.spiritcore.business.slide.Template;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

/**
 * Class used to preview a template
 * @author J
 *
 */
public class TemplatePreviewPanel extends JPanel {
	
	private final ContainerType containerType;
	private JPanel centerPanel = new JPanel();
	private int minBlocNo;
	private List<TemplatePreviewOnePanel> containerPanels = new ArrayList<>();
	
	public TemplatePreviewPanel(ContainerType containerType, int minBlocNo) {
		super(new BorderLayout());

		this.containerType = containerType;
		this.minBlocNo = minBlocNo;
		
		add(BorderLayout.CENTER, centerPanel);
		
		
		containerPanels.add(createContainerPanel());			
		updateView();
	}
	
	public ContainerType getType2Create() {
		return containerType;
	}
	
	private TemplatePreviewOnePanel createContainerPanel() {
		
		ContainerTemplate tpl = new ContainerTemplate();
		tpl.setBlocNo(minBlocNo + containerPanels.size());
		TemplatePreviewOnePanel slidePanel = new TemplatePreviewOnePanel(this, tpl);
		slidePanel.addPropertyChangeListener(TemplatePreviewOnePanel.PROPERTY_UPDATED, new PropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				firePropertyChange(TemplatePreviewOnePanel.PROPERTY_UPDATED, evt.getOldValue(), evt.getNewValue());
			}
		});
		return slidePanel;
	}
	
	public void updateView() {
		centerPanel.removeAll();
		centerPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(0, 2, 0, 2);
		c.weightx = 0; c.weighty = 0;
		for (int i = 0; i < containerPanels.size(); i++) {
			final TemplatePreviewOnePanel panel = containerPanels.get(i);
			
			JButton deleteButton = new JIconButton(IconType.DEL_ROW, "Remove");
			deleteButton.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					containerPanels.remove(panel);
					updateView();
				}
			});
			
			c.gridx = i; c.gridy = 0; centerPanel.add(deleteButton, c);
			c.gridx = i; c.gridy = 1; centerPanel.add(panel, c);			
		}
		
		JButton addButton = new JIconButton(IconType.ADD_ROW, "Add "+containerType.getName());
		addButton.addActionListener(new ActionListener() {				
			@Override
			public void actionPerformed(ActionEvent e) {
				containerPanels.add(createContainerPanel());
				updateView();
			}
		});
		c.gridx = containerPanels.size(); c.gridy = 0; centerPanel.add(addButton, c);
		
		c.weightx = 1; c.weighty = 1;
		c.gridx = containerPanels.size(); c.gridy = 4; centerPanel.add(Box.createGlue(), c);
		
		validate();
		repaint();
		firePropertyChange(TemplatePreviewOnePanel.PROPERTY_UPDATED, null, "");
	}
	
	
	public List<TemplatePreviewOnePanel> getSlidePanels() {
		return containerPanels;
	}

	public Template getTemplate() {
		Template slideTemplate = new Template(containerType);

		List<ContainerTemplate> containerTemplates = new ArrayList<ContainerTemplate>();
		for (TemplatePreviewOnePanel p : containerPanels) {
			containerTemplates.add(p.getContainerTemplate());
		}
		slideTemplate.setContainerTemplates(containerTemplates);
		return slideTemplate;
	}
	
	
	public boolean hasSample(SampleDescriptor sample) {
		Template tpl = getTemplate();
		for (ContainerTemplate s : tpl.getContainerTemplates()) {
			for (SampleDescriptor ss : s.getSampleDescriptors()) {
				if(ss.equals(sample)) return true;
			}
		}
		
		return false;
	}

}
