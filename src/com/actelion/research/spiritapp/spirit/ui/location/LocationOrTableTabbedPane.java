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

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.LocationDepictor;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.RackDepictorListener;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.CSVUtils;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JExceptionDialog;

public class LocationOrTableTabbedPane extends JCustomTabbedPane {

	public static final String PROPERTY_SELECTION = "property_selection";
	private LocationDepictor locationDepictor = new LocationDepictor();
	private BiosampleTable biosampleTable = new BiosampleTable();
	private Location location;

	public LocationOrTableTabbedPane() {
		super(JTabbedPane.BOTTOM);

		add("Graphical", locationDepictor);
		add("Table", new JScrollPane(biosampleTable));
		addChangeListener(e-> {
			//Memorize the selection
			List<Biosample> selection;
			if(getSelectedIndex()==0) {
				selection = biosampleTable.getSelection();
			} else {
				selection = Container.getBiosamples(locationDepictor.getSelectedContainers());
			}

			//		locationDepictor.setDisplayChildren(true);
			locationDepictor.setShowOneEmptyPosition(false);


			//Update
			updateDepictorOrTable();

			//Keep the selection
			if(getSelectedIndex()==0) {
				locationDepictor.setSelectedContainers(Biosample.getContainers(selection));
			} else {
				biosampleTable.setSelection(selection);
			}
		});


		//hide or show the selected biosamples if a selection is made
		locationDepictor.addRackDepictorListener(new RackDepictorListener() {
			@Override
			public void onSelect(Collection<Integer> pos, Container lastSelect, boolean dblClick) {
				Set<Container> sel = locationDepictor.getSelectedContainers();
				if(sel.size()==1 && SpiritRights.canReadBiosamples(sel.iterator().next().getBiosamples(), SpiritFrame.getUser())) {
					firePropertyChange(PROPERTY_SELECTION, null, sel.iterator().next().getBiosamples());
				} else {
					firePropertyChange(PROPERTY_SELECTION, null, new ArrayList<>());
				}
			}
			//			@Override
			//			public void locationSelected(final Location location) {
			//				setBioLocation(location);
			//			}
			@Override
			public void onPopup(Collection<Integer> pos, Container lastSelect, Component comp, Point point) {
				Set<Container> containers = locationDepictor.getSelectedContainers();
				if(pos.size()>0) {
					ContainerActions.createPopup(containers).show(comp, point.x, point.y);
				}
			}
			@Override
			public void locationPopup(Location location, Component comp, Point point) {
				LocationActions.createPopup(location).show(comp, point.x, point.y);
			}
		});


		//Link the biosampleTab view to the graphicalTab view
		biosampleTable.getSelectionModel().addListSelectionListener(e-> {
			if(e.getValueIsAdjusting()) return;
			firePropertyChange(PROPERTY_SELECTION, null, biosampleTable.getSelection());
		});

		BiosampleActions.attachPopup(biosampleTable);


	}

	public LocationDepictor getLocationDepictor() {
		return locationDepictor;
	}

	public BiosampleTable getBiosampleTable() {
		return biosampleTable;
	}


	public Location getBioLocation() {
		return location;
	}

	public void setBioLocation(Location location) {
		this.location = location;
		updateDepictorOrTable();
	}

	/**
	 * Precondition: locationBrowser.location is set
	 * Recreate the LocationDepictor or the table (depending which one is visible)
	 */
	private void updateDepictorOrTable() {
		if(getSelectedIndex()==0) {
			locationDepictor.setBioLocation(location);
		} else {
			List<Biosample> biosamples = new ArrayList<>();
			if(location!=null) {
				for (Biosample b: location.getBiosamples()) {
					if(SpiritRights.canRead(b, SpiritFrame.getUser())) {
						biosamples.add(b);
					}
				}
			}
			Collections.sort(biosamples, Biosample.COMPARATOR_POS);
			biosampleTable.setRows(biosamples);
		}
	}

	public void exportToCsv() {
		try {
			if(getSelectedIndex()==0) {
				CSVUtils.exportToCsv(locationDepictor.getLocationLayout());
			} else {
				CSVUtils.exportToCsv(biosampleTable.getTabDelimitedTable());
			}
		} catch(Exception ex) {
			JExceptionDialog.showError(ex);
		}
	}

	public void exportToExcel() {
		try {
			if(getSelectedIndex()==0) {
				String[][] layout = locationDepictor.getLocationLayout();
				if(layout==null) throw new Exception("You cannot export the layout if there are biosamples");
				POIUtils.exportToExcel(layout, POIUtils.ExportMode.HEADERS_TOPLEFT);
			} else {
				POIUtils.exportToExcel(biosampleTable.getTabDelimitedTable(), POIUtils.ExportMode.HEADERS_TOP);
			}
		} catch(Exception ex) {
			JExceptionDialog.showError(ex);
		}
	}
}
