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
import javax.swing.JScrollPane;

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
	private final JCustomTextField keywordsTextField = new JCustomTextField(20, "", "Name");
	private final LocationTable locationTable = new LocationTable();
	private final StudyComboBox studyComboBox;
	private final BiotypeComboBox biotypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes());
	private final JCheckBox emptyCheckbox = new JCheckBox("Empty", false);
	private final JCheckBox nonEmptyCheckbox = new JCheckBox("Non Empty", false);
	private final JButton viewMineButton = new JIconButton(new Action_ViewMine());
	private final JButton resetButton = new JIconButton(new Action_Reset());
	private final JButton searchButton = new JIconButton(new Action_Search());
	
	private List<Location> acceptedAdminLocations;
	
	public LocationSearchPane(Biotype forcedBiotype) {
		super(new BorderLayout());
		
		studyComboBox = new StudyComboBox(RightLevel.WRITE, "StudyId");

		JPanel filterLocation = UIUtils.createTable(1, 0, 1, 
				studyComboBox,
				biotypeComboBox,
				keywordsTextField,
				UIUtils.createHorizontalBox(emptyCheckbox, nonEmptyCheckbox));
		filterLocation.setOpaque(true);
		filterLocation.setBackground(Color.WHITE);
		viewMineButton.setVisible(Spirit.getUser()!=null && Spirit.getUser().getMainGroup()!=null);
		
		this.forcedBiotype = forcedBiotype;
		if(forcedBiotype!=null) {
			studyComboBox.setVisible(false);
			biotypeComboBox.setSelection(forcedBiotype);
			biotypeComboBox.setEnabled(false);
		}
		
		add(BorderLayout.NORTH, UIUtils.createBox(new JScrollPane(filterLocation), null, UIUtils.createHorizontalBox(viewMineButton, Box.createHorizontalGlue(), resetButton, searchButton)));
		add(BorderLayout.CENTER, new JBGScrollPane(locationTable, 2));
		setMinimumSize(new Dimension(150, 200));
		setPreferredSize(new Dimension(200, 200));

		keywordsTextField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				query();
			}
		});
		emptyCheckbox.setOpaque(false);
		emptyCheckbox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(emptyCheckbox.isSelected() && nonEmptyCheckbox.isSelected()) {
					nonEmptyCheckbox.setSelected(false);
				}
			}
		});
		nonEmptyCheckbox.setOpaque(false);
		nonEmptyCheckbox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(emptyCheckbox.isSelected() && nonEmptyCheckbox.isSelected()) {
					emptyCheckbox.setSelected(false);
				}
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
		new SwingWorkerExtended("Querying...", this, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			private List<Location> res;
			@Override
			protected void doInBackground() throws Exception {
				if(keywordsTextField.getText().length()>0) {
					res = new ArrayList<>(DAOLocation.getCompatibleLocations(keywordsTextField.getText(), Spirit.getUser()));
					
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
