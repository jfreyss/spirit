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

package com.actelion.research.spiritapp.ui.exchange;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.ui.location.LocationBrowser;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping.EntityAction;
import com.actelion.research.util.ui.UIUtils;

public class LocationMappingPanel extends JPanel implements IMappingPanel  {
	private ImporterDlg dlg;
//	private Set<Location> locations;
	
	private final JPanel centerPanel = new JPanel();
	private final Map<String, MappingPanel> mappingPanels = new HashMap<>();
	
	public LocationMappingPanel(ImporterDlg dlg, Set<Location> fromLocations) {
		super(new BorderLayout());
		this.dlg = dlg;
		setMinimumSize(new Dimension(200, 200));
		
		//Sort locations
		Set<Location> locations = new TreeSet<>(fromLocations);

		//Init components
		List<JComponent> formComponents = new ArrayList<>();
		for (Location l : locations) {
			LocationBrowser locationBrowser = new LocationBrowser();
			locationBrowser.setMinimumSize(new Dimension(400, 26));
			MappingPanel mappingPanel = new MappingPanel(locationBrowser);
			mappingPanels.put(l.getHierarchyFull(), mappingPanel);
			
			formComponents.add(new JLabel("<html><b>"+l.getHierarchyFull()+"</b>: "));
			formComponents.add(mappingPanel);
			
			
			//Preselection
			try {
				Location match = DAOLocation.getCompatibleLocation(l.getHierarchyFull(), null);
				if(match!=null) {
					mappingPanel.setMappingAction(EntityAction.MAP_REPLACE);
					mappingPanel.setCreationEnabled(false);
					locationBrowser.setBioLocation(match);
				}
			} catch(Exception e) {
				//no match
				mappingPanel.setMappingAction(EntityAction.CREATE);
				mappingPanel.setCreationEnabled(true);
			}
			
		}		
		centerPanel.add(UIUtils.createHorizontalBox(UIUtils.createTable(formComponents), Box.createHorizontalGlue()));


		//Init Layout
		JButton createAllButton = new JButton("Create All");
		createAllButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				for (String l: mappingPanels.keySet()) {
					MappingPanel mappingPanel = mappingPanels.get(l);
					if(mappingPanel==null) return;
					mappingPanel.setMappingAction(EntityAction.CREATE);
				}
			}
		});
		JButton ignoreAllButton = new JButton("Ignore All");
		ignoreAllButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				for (String l: mappingPanels.keySet()) {
					MappingPanel mappingPanel = mappingPanels.get(l);
					if(mappingPanel==null) return;
					mappingPanel.setMappingAction(EntityAction.SKIP);
				}
			}
		});
		JButton mapAllButton = new JButton("Map All");
		mapAllButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				for (String l: mappingPanels.keySet()) {
					MappingPanel mappingPanel = mappingPanels.get(l);
					if(mappingPanel==null) return;
					if( ((LocationBrowser) mappingPanel.getMappingComponent()).getBioLocation()!=null) {
						mappingPanel.setMappingAction(EntityAction.MAP_REPLACE);
					}
				}
			}
		});
		
		add(BorderLayout.NORTH, UIUtils.createHorizontalBox(new JLabel("<html>Import Locations: "), UIUtils.createHorizontalBox(BorderFactory.createDashedBorder(null), createAllButton, ignoreAllButton, mapAllButton), Box.createHorizontalGlue()));
		add(BorderLayout.CENTER, new JScrollPane(UIUtils.createHorizontalBox(centerPanel, Box.createGlue())));
		
		
	}
	
	public void updateView() {
		// TODO Auto-generated method stub
		
	}
		
	public void updateMapping() {
		ExchangeMapping mapping = dlg.getMapping();
		for (String l: mappingPanels.keySet()) {
			MappingPanel mappingPanel = mappingPanels.get(l);
			LocationBrowser locationBrowser = (LocationBrowser) mappingPanel.getMappingComponent();
			
			mapping.getLocation2action().put(l, mappingPanel.getMappingAction());
			if(locationBrowser.getBioLocation()!=null) {
				mapping.getLocation2mappedLocation().put(l, locationBrowser.getBioLocation());
			}
		}
	}
	
}
