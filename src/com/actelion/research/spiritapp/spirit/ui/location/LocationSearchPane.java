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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.lf.BiotypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.lf.StudyComboBox;
import com.actelion.research.spiritapp.spirit.ui.util.bgpane.JBGScrollPane;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationQuery;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class LocationSearchPane extends JPanel {

	public static final String PROPERTY_QUERIED = "queried";
	
	private final Biotype forcedBiotype;
	private final JPanel topPanel = new JPanel(new BorderLayout());
	private final JCustomTextField keywordsTextField = new JCustomTextField(20, "", "Name");
	private final LocationTable locationTable = new LocationTable();
	private final StudyComboBox studyComboBox;
	private final BiotypeComboBox biotypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes());
	private final JCheckBox emptyCheckbox = new JCheckBox("Empty", true);
	private final JCheckBox nonEmptyCheckbox = new JCheckBox("Non Empty", true);
	private final JButton viewMineButton = new JIconButton(new Action_ViewMine());
	private final JButton resetButton = new JIconButton(new Action_Reset());
	private final JButton searchButton = new JIconButton(new Action_Search());
	
	private List<Location> acceptedAdminLocations;
	
	public LocationSearchPane(Biotype forcedBiotype) {
		super(new BorderLayout(0, 0));		
		JPanel filterLocation = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		filterLocation.setBackground(Color.WHITE);
		studyComboBox = new StudyComboBox(RightLevel.WRITE, "StudyId");
		viewMineButton.setEnabled(Spirit.getUser()!=null && Spirit.getUser().getMainGroup()!=null);
		
		c.anchor = GridBagConstraints.WEST;
		this.forcedBiotype = forcedBiotype;
		if(forcedBiotype==null) {
			c.gridwidth = 1; c.gridx = 1; c.gridy = 1; c.weightx = 1; filterLocation.add(studyComboBox, c);
		} else {
			biotypeComboBox.setSelection(forcedBiotype);
			biotypeComboBox.setEnabled(false);
		}
		c.gridwidth = 1; c.gridx = 1; c.gridy = 2; c.weightx = 1; filterLocation.add(biotypeComboBox, c);
		c.gridwidth = 1; c.gridx = 1; c.gridy = 3; c.weightx = 1; filterLocation.add(keywordsTextField, c);
		c.gridwidth = 1; c.gridx = 1; c.gridy = 4; c.weightx = 1; filterLocation.add(Box.createVerticalStrut(3), c);
		c.gridwidth = 1; c.gridx = 1; c.gridy = 5; c.weightx = 1; filterLocation.add(UIUtils.createHorizontalBox(emptyCheckbox, nonEmptyCheckbox), c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1; c.gridx = 1; c.gridy = 10; c.weightx = 1; filterLocation.add(UIUtils.createHorizontalBox(viewMineButton, Box.createHorizontalGlue(), resetButton, searchButton), c);
			
		
		topPanel.add(BorderLayout.CENTER, filterLocation);
		add(BorderLayout.NORTH, topPanel);
		add(BorderLayout.CENTER, new JBGScrollPane(locationTable, 2));
		setMinimumSize(new Dimension(150, 200));
		setPreferredSize(new Dimension(200, 200));

		keywordsTextField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				query();
			}
		});
	}
	
	public LocationQuery getLocationQuery() {
		//Create a standard query
		LocationQuery query = new LocationQuery();
		
		query.setStudyId(studyComboBox.getText());
		query.setName(keywordsTextField.getText());
		query.setBiotype(biotypeComboBox.getSelection());
		query.setOnlyOccupied(emptyCheckbox.isSelected() && !nonEmptyCheckbox.isSelected()? Boolean.FALSE:
			!emptyCheckbox.isSelected() && nonEmptyCheckbox.isSelected()? Boolean.TRUE:
			null);			
		return query;
	}
		
	public LocationTable getLocationTable() {
		return locationTable;
	}
	
	public void reset() {
		keywordsTextField.setText("");
		if(biotypeComboBox.isEnabled()) biotypeComboBox.setSelection(null);
		studyComboBox.setText(null);
		emptyCheckbox.setSelected(true);
		nonEmptyCheckbox.setSelected(true);
		query();
		
	}
	 
	public void query() {
		query(getLocationQuery());
	}
	public void query(final LocationQuery query) {
		new SwingWorkerExtended("Querying...", this, SwingWorkerExtended.FLAG_ASYNCHRONOUS50MS) {
			private List<Location> res;
			@Override
			protected void doInBackground() throws Exception {
				if(keywordsTextField.getText().length()>0) {
					res = new ArrayList<Location>(DAOLocation.getCompatibleLocations(keywordsTextField.getText(), Spirit.getUser()));
					
					if(res.size()>0) {					
						locationTable.setRows(res);		
						if(res.size()==1) {
							locationTable.setSelection(Collections.singletonList(res.get(0)));
						}
						return;
					}
				}
				//Query
				if(query.isEmpty()) {
					res = DAOLocation.getLocationRoots(Spirit.getUser());
					acceptedAdminLocations = null;
				} else {			
					res = DAOLocation.queryLocation(query, Spirit.getUser());
					acceptedAdminLocations = res;
				}
			}
			
			@Override
			protected void done() {
				System.out.println("LocationSearchPane.query(...).new SwingWorkerExtended() {...}.done() ");
				for (Location location : res) {
					System.out.println("LocationSearchPane.query(...).new SwingWorkerExtended() {...}.done() "+location.getId());
				}
				res = JPAUtil.reattach(res);
				
				//Update the table
				locationTable.setRows(res);
				if(res.size()==1) {
					locationTable.setSelection(Collections.singletonList(res.get(0)));
				} else {
					locationTable.setSelection(null);
				}
				
				LocationSearchPane.this.firePropertyChange(PROPERTY_QUERIED, 0, 1);

			}
		};
		
	}
	
	public void repopulate() {
		studyComboBox.reload();
	}
	
	public void setQuery(LocationQuery filter) {
		studyComboBox.setText(filter.getStudyId()==null? null: filter.getStudyId());
		keywordsTextField.setText(filter.getName());
		query();
	}
		
	public void setSelection(Location location) {				
		locationTable.setSelection(Collections.singletonList(location));
	}
	
	public Location getSelection() {
		return locationTable.getSelection().size()>0? locationTable.getSelection().get(0): null; 
	}
	
	public String getStudyId() {
		return studyComboBox.getText();
	}
	
	public void setStudyId(String v) {
		if(!v.startsWith("S")) v = ""; 
		studyComboBox.setText(v);
		query();
	}
	
	public JButton getSearchButton() {
		return searchButton;
	}

	public List<Location> getAcceptedAdminLocations() {
		return acceptedAdminLocations;
	}
	
	public void queryMyLocations() {
		if(Spirit.getUser()==null || Spirit.getUser().getMainGroup()==null) return;
		LocationQuery query = new LocationQuery();
		query.setEmployeeGroup(Spirit.getUser().getMainGroup());
		query.setBiotype(forcedBiotype);
		query(query);
	}
	
	public class Action_ViewMine extends AbstractAction {
		public Action_ViewMine() {
			super("MyLocations");
			setToolTipText("Query and display the locations used by my department");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			queryMyLocations();
		}			
	}
	
	public class Action_Search extends AbstractAction {
		public Action_Search() {
			super("Search");			
			putValue(AbstractAction.SMALL_ICON, IconType.SEARCH.getIcon());
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			query();
		}
			
	}
	
	public class Action_Reset extends AbstractAction {
		public Action_Reset() {
			super("");
			putValue(Action.SMALL_ICON, IconType.CLEAR.getIcon());
			setToolTipText("Reset all query fields");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			reset();
		}
	}
}
