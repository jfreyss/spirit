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

package com.actelion.research.spiritapp.spirit.ui.scanner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.location.LocationBrowser;
import com.actelion.research.spiritapp.spirit.ui.location.LocationBrowser.LocationBrowserFilter;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.LocationDepictor;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationType;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.scanner.ScannerConfiguration;

/**
 * Dialog for selecting or creating a new rack, compatible with the given config
 * @author freyssj
 *
 */
public class SelectRackDlg extends JEscapeDialog {
	
	private final ScannerConfiguration config;
	private final JTabbedPane tabbedPane = new JCustomTabbedPane();

	private final JCustomTextField newRackNameTextField = new JCustomTextField(10, "", null);		
	private final LocationBrowser newRackParentBrowser = new LocationBrowser(LocationBrowserFilter.CONTAINER);
	private final LocationBrowser existingRackBrowser = new LocationBrowser(LocationBrowserFilter.RACKS);
	private final LocationDepictor existingRackDepictor = new LocationDepictor();

	private boolean success = false;
	private Location scannedRack;
	private Location selection;
	
	/**
	 * 
	 * @param config - not empty
	 * @param scannedRack - not empty
	 */
	public SelectRackDlg(ScannerConfiguration config, Location scannedRack) {
		super(UIUtils.getMainFrame(), "Set Rack", true);
		assert scannedRack!=null;
		assert config!=null;
		this.config = config;
		this.scannedRack = scannedRack;
		
		newRackParentBrowser.setMinimumSize(new Dimension(400, 50));		
		existingRackBrowser.setMinimumSize(new Dimension(400, 50));		
		existingRackDepictor.setMinimumSize(new Dimension(400, 300));
		
		final JButton skipButton = new JButton("Cancel"); 
		skipButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				selection = null;
				success = true;
				dispose();
			}
		});
		
		final JButton createButton = new JButton("Ok"); 
		createButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					eventOk();
				} catch(Exception e) {
					JExceptionDialog.showError(e);
				}
			}
		});
		
		existingRackBrowser.addPropertyChangeListener(LocationBrowser.PROPERTY_LOCATION_SELECTED, new PropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				refreshExistingRack();
			}
		});
		
		//////////////////////////////////////////////////////////////////////////////
		//centerPane		
		tabbedPane.add("Create new Rack", 
				UIUtils.createVerticalBox(
					new JLabel("RackId: "),
					UIUtils.createHorizontalBox(newRackNameTextField, Box.createHorizontalGlue()),
					Box.createVerticalStrut(10),
					new JLabel("Parent Location: "), 
					newRackParentBrowser,
					Box.createVerticalGlue()));
		tabbedPane.add("Add to an existing rack",
				UIUtils.createBox(existingRackDepictor, 
						UIUtils.createVerticalBox(new JLabel("Select Rack: "), existingRackBrowser),
						null, null, null												
				));
		
		//contentPanel
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.NORTH, UIUtils.createHorizontalBox(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(10, 0, 10, 0)), new JCustomLabel("Where do you want to store the samples?", FastFont.BOLD), Box.createHorizontalGlue()));
		contentPanel.add(BorderLayout.CENTER, tabbedPane);
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), skipButton, createButton));
		
		//Init variables
		if(scannedRack.getId()<=0) {
			newRackParentBrowser.setBioLocation(scannedRack.getParent());
			newRackNameTextField.setText(scannedRack.getName());
			tabbedPane.setSelectedIndex(0);
		} else if(scannedRack.getId()>0) {			
			existingRackBrowser.setBioLocation(scannedRack);
			refreshExistingRack();
			tabbedPane.setSelectedIndex(1);
		}

		
		setContentPane(contentPanel);
		getRootPane().setDefaultButton(createButton);
		pack();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(UIUtils.getMainFrame());		
	}
	
	private void refreshExistingRack() {
		Location toLoc = existingRackBrowser.getBioLocation();
		
		if(toLoc.getLocationType()!=LocationType.RACK) {
			existingRackDepictor.setBioLocation(null);
			return;
		}
		
		existingRackDepictor.setBioLocation(toLoc);
		
		//Highlight future position
		if(scannedRack==null) return;
		assert config!=null;

		Set<Integer> poses = new HashSet<Integer>();		
//		if(toLoc.getCols()==config.getCols() && toLoc.getRows()==config.getRows() && toLoc.isEmpty()) {
//			//Same positions			
//			poses.addAll(Container.getPoses(scannedRack.getContainers()));
//		} else {
			//Append to available
			Set<Integer> taken = Container.getPoses(toLoc.getContainers());
			for(int pos=0; pos<toLoc.getSize() && poses.size()<scannedRack.getContainers().size(); pos++) {
				if(taken.contains(pos)) continue;
				poses.add(pos);
			}
			
//		}
		existingRackDepictor.setHighlightPositions(poses);
		
		
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	private void eventOk() throws Exception {
		assert scannedRack!=null;
		if(tabbedPane.getSelectedIndex()==1) {
			//The user selected an existing Rack
			Location loc = existingRackBrowser.getBioLocation();
			
			if(!loc.isEmpty()) throw new Exception("The rack is not empty");
			if(loc.getCols()!=config.getCols() || loc.getRows()!=config.getRows()) throw new Exception("The rack'size is invalid");
			
			loc.setContainers(scannedRack.getContainers());
			
			
			
			
			selection = loc;
			
		} else if(tabbedPane.getSelectedIndex()==0) {
			//The user selected a new Rack
			String name = newRackNameTextField.getText();
			Location parent = newRackParentBrowser.getBioLocation();
			if(name.length()==0) {
				if(parent==null) {
					//Do nothing
					success=true;
					dispose();
				} else {
					throw new Exception("The RackId cannot be empty if you want to store it");
				}
			}
			
			if(parent!=null) {
				if(!SpiritRights.canEdit(parent, Spirit.getUser())) throw new Exception("You don't have rights over "+parent);
				
				Set<String> existingNames = new HashSet<String>();
				for(Location l: parent.getChildren()) {
					if(l!=scannedRack) existingNames.add(l.getName());
				}
				if(existingNames.contains(name)) throw new Exception(name+" already exists under "+parent);
			}
			
			//Update the rack
			scannedRack.setName(name);
			scannedRack.setParent(parent);
			scannedRack.setUpdUser(Spirit.getUser().getUsername());
			scannedRack.setUpdDate(new Date());
			scannedRack.setWasUpdated(true);
			
			selection = scannedRack;
		}
		success=true;
		dispose();

	}
	
	public boolean isSuccess() {
		return success;
	}
	

	public Location getSelection() {
		return selection;
	}
	
	
	

}
