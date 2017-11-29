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

package com.actelion.research.spiritapp.ui.location;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.SpiritTab;
import com.actelion.research.spiritapp.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritapp.ui.location.depictor.LocationDepictor;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class LocationTab extends SpiritTab {

	//West Components
	private final JSplitPane westPane;
	private LocationSearchPane searchPane;
	private BiosampleTabbedPane detailPane = new BiosampleTabbedPane();

	//East Components
	private LocationBrowser locationBrowser = new LocationBrowser();
	private LocationOrTableTabbedPane tabbedPane = new LocationOrTableTabbedPane();
	private boolean first = true;
	private int push = 0;

	public LocationTab(SpiritFrame frame) {
		this(frame, null);
	}

	public LocationTab(SpiritFrame frame, Biotype forcedBiotype) {
		super(frame, "Locations", IconType.LOCATION.getIcon());

		searchPane = new LocationSearchPane(frame, forcedBiotype);


		//CSV Export
		JButton csvButton = new JIconButton(IconType.CSV, "CSV");
		csvButton.addActionListener(e-> tabbedPane.exportToCsv());

		//Excel Export
		JButton excelButton = new JIconButton(IconType.EXCEL, "XLS");
		excelButton.addActionListener(e-> tabbedPane.exportToExcel());

		JPanel buttonsPanel = createButtonsPanel();
		JPanel locationPanel = UIUtils.createBox(
				tabbedPane,
				UIUtils.createBox(locationBrowser, null, null, null, UIUtils.createHorizontalBox(tabbedPane.getLocationDepictor().createZoomPanel(), csvButton, excelButton)),
				buttonsPanel==null? null: buttonsPanel);

		westPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.VERTICAL_SPLIT, searchPane, detailPane);
		westPane.setDividerLocation(1200);

		JSplitPane contentPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT, westPane, locationPanel);
		contentPane.setDividerLocation(300);

		//If a query is made, restricts the displayed locations to the result's location
		searchPane.addPropertyChangeListener(LocationSearchPane.PROPERTY_QUERIED,  evt-> {
			detailPane.setBiosamples(null);
			//			locationDepictor.setAcceptedAdminLocations(searchPane.getLocationQuery().isEmpty()? null: searchPane.getAcceptedAdminLocations());
		});

		//If the locationBrowser is changed, update the location
		locationBrowser.addPropertyChangeListener(LocationBrowser.PROPERTY_LOCATION_SELECTED, evt-> {
			Location l = locationBrowser.getBioLocation();
			searchPane.getLocationTable().addRow(l);
			setBioLocation(l);
		});

		tabbedPane.addPropertyChangeListener(LocationOrTableTabbedPane.PROPERTY_SELECTION, e-> {
			@SuppressWarnings("unchecked")
			Collection<Biosample> sel = (Collection<Biosample>) e.getNewValue();

			if(sel.size()>0 && SpiritRights.canReadBiosamples(sel, SpiritFrame.getUser())) {
				if(westPane.getDividerLocation()>getHeight()-100) {
					westPane.setDividerLocation(580);
				}
				detailPane.setBiosamples(sel);
			} else {
				if(westPane.getDividerLocation()<getHeight()-100) {
					westPane.setDividerLocation(2400);
				}
				detailPane.setBiosamples(null);
			}
		});

		//Link the BiosampleTable to the depictor
		searchPane.getLocationTable().getSelectionModel().addListSelectionListener(e->{
			if(e.getValueIsAdjusting()) return;
			if(push>0) return;
			List<Location> selection = searchPane.getLocationTable().getSelection();
			if(selection.size()==1) {
				Location sel = selection.size()==1? selection.get(0): null;
				setBioLocation(sel, -1, true);
			} else {
				locationBrowser.setBioLocation(null);
				setBioLocation(null, -1, true);
			}

		});

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, contentPane);
	}

	/**
	 * Set the location for this tab, by updating the table, the browser, and the locationdepictor
	 * @param location
	 */
	public void setBioLocation(final Location location) {
		setBioLocation(location, -1, false);
	}

	public void setBioLocation(final Location location, final int pos) {
		setBioLocation(location, pos, false);
	}

	private void setBioLocation(final Location location, final int pos, final boolean onlySetDepictorPosition) {
		first = false;
		new SwingWorkerExtended("Set Location", this, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			@Override
			protected void done() {
				//Add location in table if needed
				if(location!=null && !searchPane.getLocationTable().getRows().contains(location)) {
					searchPane.getLocationTable().addRow(location);
				}
				if(location!=null && !onlySetDepictorPosition) {
					searchPane.getLocationTable().setSelection(Collections.singletonList(location));
				}

				//Update the browser
				locationBrowser.setBioLocation(location);
				tabbedPane.getLocationDepictor().setSelectedPoses(null);

				//Update the depictor
				tabbedPane.setBioLocation(location);
				if(pos>=0) tabbedPane.getLocationDepictor().setSelectedPoses(Collections.singletonList(pos));
			}
		};
	}


	@SuppressWarnings("unchecked")
	@Override
	public <T> void fireModelChanged(SpiritChangeType action, Class<T> what, Collection<T> details) {
		if(!isShowing()) return;

		if(what==Location.class && action==SpiritChangeType.MODEL_DELETED) {
			//Remove the locations
			List<Location> locs = (List<Location>) details;
			System.out.println("LocationTab.fireModelChanged() remove"+locs);

			//Refresh their parent
			List<Location> parents = JPAUtil.reattach(Location.getParents(locs));
			List<Location> rows = new ArrayList<>(searchPane.getLocationTable().getRows());
			System.out.println("LocationTab.fireModelChanged(1) "+rows);
			rows.removeAll(locs);
			rows = JPAUtil.reattach(rows);
			searchPane.getLocationTable().setRows(rows);
			System.out.println("LocationTab.fireModelChanged(2) "+rows);

			//Set the active location: parent
			Location parent = parents.size()==0? null: parents.get(0);
			setBioLocation(parent);
		} else if(what==Location.class && details.size()>0) {
			//Reload the locations
			List<Location> locs = (List<Location>) details;
			locs = JPAUtil.reattach(locs);

			//Add the parents
			for (Location l : new ArrayList<>(locs)) {
				if(l!=null && l.getParent()!=null && !locs.contains(l.getParent())) {
					locs.add(l.getParent());
				}
			}

			//Refresh location or parents
			List<Location> rows = new ArrayList<>(searchPane.getLocationTable().getRows());
			rows = JPAUtil.reattach(rows);
			rows.addAll(locs);
			searchPane.getLocationTable().setRows(rows);

			//Set the active location: parent
			if(action==SpiritChangeType.MODEL_ADDED) {
				setBioLocation(locs.size()>=1 && locs.get(0).getParent()!=null? locs.get(0).getParent(): null);
			} else {
				setBioLocation(locs.size()>=1? locs.get(0): null);
			}
		} else {
			Location l = tabbedPane.getBioLocation();
			l = JPAUtil.reattach(l);
			//			Collection<Container> sel = locationDepictor.getSelectedContainers();
			//
			//
			//			//Refresh the loc (set null first to be sure to trigger a change)
			//			locationDepictor.setSelectedContainers(sel);
			setBioLocation(l);
		}
	}

	public LocationDepictor getLocationDepictor() {
		return tabbedPane.getLocationDepictor();
	}

	public List<Location> getLocations() {
		return searchPane.getLocationTable().getRows();
	}

	/**
	 * To be overriden by classes to get a custom button panel
	 * @return
	 */
	protected JPanel createButtonsPanel() {
		return null;
	}

	@Override
	public void onTabSelect() {
		if(getRootPane()!=null){
			getRootPane().setDefaultButton(searchPane.getSearchButton());
		}
		if(first) {
			first = false;
			if(getFrame()!=null && getFrame().getStudyId().length()>0) {
				searchPane.query();
			} else {
				searchPane.query();
				//				searchPane.queryMyLocations();
			}
		}
	}

	@Override
	public void onStudySelect() {
		searchPane.query();
	}



}
