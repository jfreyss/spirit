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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleComboBox;
import com.actelion.research.spiritapp.spirit.ui.container.ContainerActions;
import com.actelion.research.spiritapp.spirit.ui.container.ContainerTable;
import com.actelion.research.spiritapp.spirit.ui.container.ContainerTableModel.ContainerTableModelType;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.spirit.ui.util.bgpane.JBGScrollPane;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

public abstract class InventoryPanel extends JPanel {

	private ContainerType containerType;
	private Study study;
	private ContainerTable table = new ContainerTable(ContainerTableModelType.EXPANDED);
	
	private BiosampleComboBox animalComboBox = new BiosampleComboBox();
	private ContentComboBox contentComboBox = new ContentComboBox();
	private List<Container> containers;


	public InventoryPanel(ContainerType containerType) {		
		super(new BorderLayout());
		this.containerType = containerType;
		ContainerActions.attachPopup(table);
		
		//FiltersPanel
		ActionListener searchAction = new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				search();
			}
		};
		animalComboBox.addActionListener(searchAction);
		contentComboBox.addActionListener(searchAction);
		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.add(BorderLayout.WEST, UIUtils.createHorizontalBox(
				new JLabel("Filter by Animal: "), animalComboBox, Box.createHorizontalStrut(10), 
				new JLabel("Bloc-No: "), contentComboBox, Box.createHorizontalStrut(10),
				createEastPanel(), 
				Box.createHorizontalGlue()));
		
		add(BorderLayout.NORTH, northPanel);
		add(BorderLayout.CENTER, new JBGScrollPane(table, 1));
	}
	
	public void synchroFiltersFrom(InventoryPanel inventoryPanel) {
		animalComboBox.setSelection(inventoryPanel.animalComboBox.getSelection());
		String c = inventoryPanel.contentComboBox.getSelection();
		if(c==null) return;
		if(c.startsWith("Bl.")) c = c.substring(0, c.indexOf("\n"));
		for(String s: contentComboBox.getValues()) {
			if(s.startsWith(c)) {
				contentComboBox.setSelection(s);
				break;
			}
		}
	}
	
	
	public void updateFilters(final Study study, final InventoryPanel lastInventoryPanel) {
		
		if(study==null) {
			this.study = study;
			this.containers = null;
			table.setRows(new ArrayList<Container>());
			if(lastInventoryPanel!=null) {
				synchroFiltersFrom(lastInventoryPanel);
				search();
			}
		} else if(!study.equals(this.study)){
			this.study = study;
			new SwingWorkerExtended("Loading Containers", this, false) {				
				List<Biosample> animals;
				Map<String, Integer> content2count;
				@Override
				protected void doInBackground() throws Exception {
					animals = study.getTopAttachedBiosamples();	
					
					BiosampleQuery q = new BiosampleQuery();
					q.setStudyIds(study.getStudyId());
					q.setContainerType(containerType);
					containers = Biosample.getContainers(DAOBiosample.queryBiosamples(q, null));
					Collections.sort(containers);
					
					content2count = new LinkedHashMap<String, Integer>();
					for (Container c : containers) {
						String content =  c.getBlocDescription(); 
						
						Integer n = content2count.get(content);
						if(n==null) content2count.put(content, 1);
						else content2count.put(content, n+1);
					}
					
				}
				
				@Override
				protected void done() {
					animalComboBox.setValues(animals, "Animal...");
					List<String> contents = new ArrayList<String>(content2count.keySet());
					Collections.sort(contents, CompareUtils.STRING_COMPARATOR);
					contentComboBox.setValues(contents, "Template...");
					contentComboBox.setContent2count(content2count);
					if(lastInventoryPanel!=null) synchroFiltersFrom(lastInventoryPanel);
					search();
				}
			};
		} else {
			if(lastInventoryPanel!=null) {
				synchroFiltersFrom(lastInventoryPanel);
				search();
			}
		}
	}
	
	public void search() {
		if(containers==null) {
			table.setRows(new ArrayList<Container>());
		} else {

			List<Container> filtered = new ArrayList<>();
			for (Container container : containers) {
				if(animalComboBox.getSelection()!=null && !Biosample.getTopParents(container.getBiosamples()).contains(animalComboBox.getSelection())) continue;
				if(contentComboBox.getSelection()!=null && !contentComboBox.getSelection().equals(container.getBlocDescription())) continue;
				
				filtered.add(container);
			}
			table.setRows(filtered);
			SpiritContextListener.setStatus(filtered.size() + " " + containerType + "s");
		}

	}
	

	public abstract JComponent createEastPanel();

}
