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
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.container.ContainerActions;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.LocationDepictor;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.RackDepictorListener;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritTab;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.CSVUtils;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class LocationTab extends JPanel implements ISpiritTab {
	
	//West Components
	private final JSplitPane westPane;
	private LocationSearchPane searchPane;
	private BiosampleTabbedPane detailPane = new BiosampleTabbedPane();

	//East Components
	private LocationBrowser locationBrowser = new LocationBrowser();
	private JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);	
	private LocationDepictor locationDepictor = new LocationDepictor();
	private BiosampleTable biosampleTable = new BiosampleTable();
	private boolean first = true;
	private int push = 0;
	
	public LocationTab(Biotype forcedBiotype) {		
		searchPane = new LocationSearchPane(forcedBiotype);
		
		locationDepictor.setDisplayChildren(true);
		locationDepictor.setShowOneEmptyPosition(false);
				
		//Layout
		tabbedPane.add("Graphical", locationDepictor);
		tabbedPane.add("Biosamples", new JScrollPane(biosampleTable));		
		tabbedPane.addChangeListener(new ChangeListener() {			
			@Override
			public void stateChanged(ChangeEvent e) {
				//Memorize the selection
				List<Biosample> selection;
				if(tabbedPane.getSelectedIndex()==0) {
					selection = biosampleTable.getSelection(); 
				} else {
					selection = Container.getBiosamples(locationDepictor.getSelectedContainers());					
				}
				
				//Update
				updateDepictorOrTable();

				//Keep the selection
				if(tabbedPane.getSelectedIndex()==0) {
					locationDepictor.setSelectedContainers(Biosample.getContainers(selection));
				} else {
					biosampleTable.setSelection(selection);					
				}
				

			}
		});
		
		JButton csvButton = new JIconButton(IconType.CSV, "CSV");
		csvButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if(tabbedPane.getSelectedIndex()==0) {
						CSVUtils.exportToCsv(locationDepictor.getLocationLayout());
					} else {
						CSVUtils.exportToCsv(biosampleTable.getTabDelimitedTable());
					}
				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});
		
		JButton excelButton = new JIconButton(IconType.EXCEL, "XLS");
		excelButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if(tabbedPane.getSelectedIndex()==0) {
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
		});
		
		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.add(BorderLayout.CENTER, locationBrowser);
		northPanel.add(BorderLayout.EAST, UIUtils.createHorizontalBox(locationDepictor.createZoomPanel(), csvButton, excelButton));
		
		JPanel locationPanel = new JPanel(new BorderLayout());
		locationPanel.add(BorderLayout.NORTH, northPanel);
		locationPanel.add(BorderLayout.CENTER, tabbedPane);
		
		JPanel buttonsPanel = createButtonsPanel();
		if(buttonsPanel!=null) locationPanel.add(BorderLayout.SOUTH, buttonsPanel);
		
		westPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchPane, detailPane);
		JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, westPane, locationPanel);
		
		westPane.setDividerLocation(1200);
		westPane.setOneTouchExpandable(true);
		contentPane.setDividerLocation(300);
		contentPane.setOneTouchExpandable(true);
		
		//If a query is made, restricts the displayed locations to the result's location
		searchPane.addPropertyChangeListener(LocationSearchPane.PROPERTY_QUERIED, new PropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				detailPane.setBiosamples(null);
				locationDepictor.setAcceptedAdminLocations(searchPane.getLocationQuery().isEmpty()? null: searchPane.getAcceptedAdminLocations());
			}
		});

		//If the locationBrowser is changed, update the location
		locationBrowser.addPropertyChangeListener(LocationBrowser.PROPERTY_LOCATION_SELECTED, new PropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {				
				searchPane.getLocationTable().addRow(locationBrowser.getBioLocation());
				setBioLocation(locationBrowser.getBioLocation());
			}
		});
		
		//hide or show the selected biosamples if a selection is made
		locationDepictor.addRackDepictorListener(new RackDepictorListener() {			
			@Override
			public void onSelect(Collection<Integer> pos, Container lastSelect, boolean dblClick) {		
				Set<Container> sel = locationDepictor.getSelectedContainers();
				if(sel.size()==0) return;
				
				if(sel.size()==1 && SpiritRights.canReadBiosamples(sel.iterator().next().getBiosamples(), Spirit.getUser())) {
					if(westPane.getDividerLocation()>getHeight()-100) {
						westPane.setDividerLocation(580);
					}
					detailPane.setBiosamples(sel.iterator().next().getBiosamples());					
				} else {
					if(westPane.getDividerLocation()<getHeight()-100) {
						westPane.setDividerLocation(2400);
					}
					detailPane.setBiosamples(null);
				}
			}
			@Override
			public void locationSelected(final Location location) {
				setBioLocation(location);
			}				
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
		
		BiosampleActions.attachPopup(biosampleTable);

		//Link the BiosampleTable to the depictor
		searchPane.getLocationTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {	
				if(e.getValueIsAdjusting()) return;
				if(push>0) return;
				List<Location> selection = searchPane.getLocationTable().getSelection();
				if(selection.size()==1) {
					Location sel = selection.size()==1? selection.get(0): null;
					setBioLocation(sel, 1, true);
				} else {
					locationBrowser.setBioLocation(null);
					setBioLocation(null, 1, true);
				}
			}
		});
		
		//Link the biosampleTab view to the graphicalTab view
		biosampleTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				
				if(biosampleTable.getSelection().size()>0 && SpiritRights.canReadBiosamples(biosampleTable.getSelection(), Spirit.getUser())) {
					if(westPane.getDividerLocation()>getHeight()-100) {
						westPane.setDividerLocation(580);
					}
					detailPane.setBiosamples(biosampleTable.getSelection());
				} else {
					if(westPane.getDividerLocation()<getHeight()-100) {
						westPane.setDividerLocation(2400);
					}
					detailPane.setBiosamples(null);
				}
			}
		});
		
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, contentPane);
		
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				System.out.println("LocationTab.LocationTab() first="+first);
				if(getRootPane()!=null){
					getRootPane().setDefaultButton(searchPane.getSearchButton());		
				}
				if(first) {
					first = false;
					System.out.println("LocationTab.LocationTab() getStudyIds='"+getStudyIds()+"'");
					if(getStudyIds().length()>0) {
						searchPane.query();
					} else {
						searchPane.queryMyLocations();
					}
				}
			}
		});
		

		
	}	
	
	/**
	 * Set the location for this tab, by updating the table, the browser, and the locationdepictor
	 * @param location
	 */
	public void setBioLocation(final Location location) {
		setBioLocation(location, -1);
	}
	
	public void setBioLocation(final Location location, final int pos) {
		setBioLocation(location, -1, false);		
	}

	private void setBioLocation(final Location location, final int pos, final boolean onlyDepictor) {
		first = false;
		new SwingWorkerExtended("Set Location", this, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			@Override
			protected void done() {
				//Add location in table if needed
				if(location!=null && !searchPane.getLocationTable().getRows().contains(location)) {
					searchPane.getLocationTable().addRow(location);
				}
				if(location!=null && !onlyDepictor) {
					searchPane.getLocationTable().setSelection(Collections.singletonList(location));
				}
				
				//Update the browser
				locationBrowser.setBioLocation(location);
				locationDepictor.setSelectedPoses(null);
				
				//Update the depictor
				updateDepictorOrTable();
				if(pos>=0) locationDepictor.setSelectedPoses(Collections.singletonList(pos));
			}
		};

	}
		
	/**
	 * Precondition: locationBrowser.location is set
	 * Recreate the LocationDepictor or the table (depending which one is visible)
	 */
	private void updateDepictorOrTable() {
		Location location = locationBrowser.getBioLocation();
		if(tabbedPane.getSelectedIndex()==0) {
			locationDepictor.setBioLocation(location);
		} else {			
			List<Biosample> biosamples = new ArrayList<>();
			if(location!=null) {
				for (Biosample b: location.getBiosamples()) {
					if(SpiritRights.canRead(b, Spirit.getUser())) {
						biosamples.add(b);
					}
				}
			}
			Collections.sort(biosamples, Biosample.COMPARATOR_POS);
			biosampleTable.setRows(biosamples);
		}
		
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> void fireModelChanged(SpiritChangeType action, Class<T> what, List<T> details) {
		if(!isShowing()) return;
				
		if(what==Location.class && action==SpiritChangeType.MODEL_DELETED) {
			//Remove the locations
			List<Location> locs = (List<Location>) details;
			
			//Refresh their parent
			List<Location> parents = JPAUtil.reattach(Location.getParents(locs));
			List<Location> rows = new ArrayList<>(searchPane.getLocationTable().getRows());
			rows.removeAll(locs);
			rows = JPAUtil.reattach(rows);
			searchPane.getLocationTable().setRows(rows);
			
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
		} 
		Location l = locationDepictor.getBioLocation();
		Collection<Container> sel = locationDepictor.getSelectedContainers();		

		
		//Refresh the loc (set null first to be sure to trigger a change)
		setBioLocation(null);
		setBioLocation(l);
		locationDepictor.setSelectedContainers(sel);
	}
	
	@Override
	public void refreshFilters() {
		searchPane.repopulate();
		searchPane.query();
	}

	@Override
	public String getStudyIds() {
		return searchPane.getStudyId();
	}
	
	@Override
	public void setStudyIds(String s) {
		if(s==null) return;
		String currentStudy = searchPane.getStudyId();
		if(currentStudy!=null && currentStudy.equals(s)) return; //no need to refresh
		
		searchPane.setStudyId(s);		
	}
	
	public LocationDepictor getLocationDepictor() {
		return locationDepictor;
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
	
	
}
