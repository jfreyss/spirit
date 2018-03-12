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

package com.actelion.research.spiritapp.ui.location;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
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

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.component.BiotypeComboBox;
import com.actelion.research.spiritapp.ui.util.component.JBGScrollPane;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationQuery;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class LocationSearchPane extends JPanel {

	public static final String PROPERTY_QUERIED = "queried";

	private final SpiritFrame frame;
	private final Biotype forcedBiotype;
	private final JCustomTextField keywordsTextField = new JCustomTextField(20, "", "Name");
	private final LocationTable locationTable = new LocationTable();

	private final BiotypeComboBox biotypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes());
	private final JCheckBox emptyCheckbox = new JCheckBox("Empty", true);
	private final JCheckBox nonEmptyCheckbox = new JCheckBox("Non Empty", true);
	private final JButton resetButton = new JIconButton(new Action_Reset());
	private final JButton searchButton = new JIconButton(new Action_Search());

	private List<Location> acceptedAdminLocations;

	public LocationSearchPane(SpiritFrame frame, Biotype forcedBiotype) {
		super(new BorderLayout());
		this.frame = frame;

		JPanel filterLocation = UIUtils.createTable(1, 0, 1,
				biotypeComboBox,
				keywordsTextField,
				UIUtils.createHorizontalBox(emptyCheckbox, nonEmptyCheckbox));
		filterLocation.setOpaque(true);
		filterLocation.setBackground(Color.WHITE);
		//		viewMineButton.setVisible(SpiritFrame.getUser()!=null && SpiritFrame.getUser().getMainGroup()!=null);

		this.forcedBiotype = forcedBiotype;
		if(forcedBiotype!=null) {
			biotypeComboBox.setSelection(forcedBiotype);
			biotypeComboBox.setEnabled(false);
		}

		add(BorderLayout.NORTH, UIUtils.createBox(new JScrollPane(filterLocation), null, UIUtils.createHorizontalBox(/*viewMineButton,*/ Box.createHorizontalGlue(), resetButton, searchButton)));

		//		JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JBGScrollPane(locationTable1), new JBGScrollPane(locationTable2));
		add(BorderLayout.CENTER, new JBGScrollPane(locationTable));
		setMinimumSize(new Dimension(150, 200));
		setPreferredSize(new Dimension(200, 200));

		keywordsTextField.addActionListener(e-> {
			query();
		});
		emptyCheckbox.setOpaque(false);
		emptyCheckbox.addActionListener(e-> {
			if(emptyCheckbox.isSelected() && nonEmptyCheckbox.isSelected()) {
				nonEmptyCheckbox.setSelected(false);
			}
		});
		nonEmptyCheckbox.setOpaque(false);
		nonEmptyCheckbox.addActionListener(e-> {
			if(emptyCheckbox.isSelected() && nonEmptyCheckbox.isSelected()) {
				emptyCheckbox.setSelected(false);
			}
		});
	}

	public LocationQuery getLocationQuery() {
		//Create a standard query
		LocationQuery query = new LocationQuery();

		query.setStudyId(SpiritFrame.getStudyId());
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
		frame.setStudyId("");
		keywordsTextField.setText("");
		if(biotypeComboBox.isEnabled()) biotypeComboBox.setSelection(null);
		emptyCheckbox.setSelected(true);
		nonEmptyCheckbox.setSelected(true);
		queryMyLocations();

	}

	public void query() {
		query(getLocationQuery());
	}

	public void query(final LocationQuery query) {

		new SwingWorkerExtended("Querying Locations", this, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			private List<Location> res;
			@Override
			protected void doInBackground() throws Exception {
				//Query
				if(query.isEmpty()) {
					res = new ArrayList<>();
					acceptedAdminLocations = null;
					locationTable.setHighlightRows(null);
				} else {
					res = DAOLocation.queryLocation(query, SpiritFrame.getUser());
					acceptedAdminLocations = res;
					locationTable.setHighlightRows(res);
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
					locationTable.clearSelection();
				}

				LocationSearchPane.this.firePropertyChange(PROPERTY_QUERIED, 0, 1);

			}
		};
	}

	public void setQuery(LocationQuery filter) {
		//		studyComboBox.setText(filter.getStudyId()==null? null: filter.getStudyId());
		keywordsTextField.setText(filter.getName());
		query();
	}

	public void setSelection(Location location) {
		locationTable.setSelection(Collections.singletonList(location));
	}

	public Location getSelection() {
		return locationTable.getSelection().size()>0? locationTable.getSelection().get(0): null;
	}

	public JButton getSearchButton() {
		return searchButton;
	}

	public List<Location> getAcceptedAdminLocations() {
		return acceptedAdminLocations;
	}

	public void queryMyLocations() {
		LocationQuery query = new LocationQuery();
		query.setEmployeeGroup(SpiritFrame.getUser().getMainGroup());
		query.setBiotype(forcedBiotype);
		query.setFilterAdminLocation(true);
		query(query);
	}

	public class Action_Search extends AbstractAction {
		public Action_Search() {
			super("Search");
			putValue(AbstractAction.SMALL_ICON, IconType.SEARCH.getIcon());
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			LocationQuery q = getLocationQuery();
			if(q.isEmpty()) {
				q = new LocationQuery();
				q.setEmployeeGroup(SpiritFrame.getUser().getMainGroup());
				q.setBiotype(forcedBiotype);
				q.setFilterAdminLocation(true);
			}
			query(q);
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
