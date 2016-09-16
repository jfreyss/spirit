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

package com.actelion.research.spiritapp.spirit.ui.location;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.util.Config;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class MoveLocationDialog extends JSpiritEscapeDialog {

	private List<Location> locations;
	private LocationBrowser locationBrowser = new LocationBrowser(); 
	
	public MoveLocationDialog(List<Location> locations) {
		super(UIUtils.getMainFrame(), "Move Location", MoveLocationDialog.class.getName());
		this.locations = locations;
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBorder(BorderFactory.createEtchedBorder());
		centerPanel.add(BorderLayout.NORTH, new JLabel("Move " + (locations.size()==1? locations.get(0).getName(): locations.size()+" locations") + " to: "));
		centerPanel.add(BorderLayout.CENTER, new JScrollPane(locationBrowser));
		
		Set<Location> parents = new HashSet<Location>();
		for (Location location : locations) {
			if(location.getParent()!=null) parents.add(location.getParent());			
		}
		if(parents.size()>0) {
			locationBrowser.setBioLocation(parents.iterator().next());
		}
				
		
		//Buttons
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				eventOk();
			}
		});
		
		//Content Pane
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.CENTER, centerPanel);
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));
		setContentPane(contentPanel);
		setSize(300, 250);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
		
	}
	
	public void eventOk() {
		try {
			
			//Update the location
			DAOLocation.moveLocations(locations, locationBrowser.getBioLocation(), Spirit.askForAuthentication());
			SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Location.class, locations);
			Config.getInstance(".spirit").setProperty("location.move", locationBrowser.getBioLocation()==null?-1:locationBrowser.getBioLocation().getId());				
			
			dispose();
		} catch (Exception e) {
			JExceptionDialog.showError(this, e);
		}
	}
	
}
