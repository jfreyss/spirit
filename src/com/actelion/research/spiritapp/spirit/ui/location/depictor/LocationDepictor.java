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

package com.actelion.research.spiritapp.spirit.ui.location.depictor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationType.LocationCategory;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class LocationDepictor extends JPanel {

	private Location location;
	
	private boolean showOneEmptyPosition = true;
	private double zoomFactor = 1;
	
	private Set<Location> acceptedAdminLocations = null;
	
	private boolean displayChildren = false;
	private LocationPanel activeLocationPanel;
	
	private final RackDepictor rackPanel = new RackDepictor(this);
	private boolean forRevisions;


	public LocationDepictor() {
		super(new BorderLayout());
		
		LocationPanel topLocationPanel = new LocationPanel(this);		
		activeLocationPanel = topLocationPanel;
		add(BorderLayout.CENTER, topLocationPanel);
				
		setPreferredSize(new Dimension(400,400));
		setMinimumSize(new Dimension(300,300));
	
		//Refresh only when shown or resized, and not before (to save time)
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
			}
			@Override
			public void componentResized(ComponentEvent e) {
				boolean isVisible = true;
				Component c = LocationDepictor.this;
				while(c!=null) {
					if(!c.isShowing()) {isVisible=false; break;}
					c = c.getParent();
				}
				
				if(!isVisible) return;				
				updateView();
			}
		});
		
	}

	public RackDepictor getRackPanel() {
		return rackPanel;
	}
	
	public void setHighlightPositions(Collection<Integer> positions) {
		getRackPanel().setHighlightPoses(positions);		
		getRackPanel().repaint();
	}
	
	public void setHighlightContainers(Collection<Container> containers) {
		getRackPanel().setHighlightContainers(containers);		
		getRackPanel().repaint();
	}
	
	public Set<Integer> getHighlightPoses() {
		return getRackPanel().getHighlightPoses();
	}
	
	/**
	 * Updates the location of this depictor.
	 * The location is updated/reattached if it is different from the current one (expect if the locationDepictor is set forRevisions) 
	 * @param loc
	 */
	public void setBioLocation(Location loc) {
//		if(!forRevisions && ((this.location == null && loc==null) || (this.location!=null && this.location.equals(loc)))) return;
		this.location = loc;
		
		if(!forRevisions) location = JPAUtil.reattach(location);
		updateView();		
	}
	
	public void updateView() {
		
		final LocationPanel top = new LocationPanel(LocationDepictor.this);
		top.setSize(LocationDepictor.this.getSize());
		LocationPanel active;	
	
		int depth = acceptedAdminLocations!=null && acceptedAdminLocations.size()>0? 7: 5;
		if(displayChildren) {
			if(location==null) {
				List<Location> roots = new ArrayList<>(); 
				if(getAcceptedAdminLocations()==null || getAcceptedAdminLocations().size()>0) {
					//Display locations either if 
					// - there was no search provided
					// - there is a search with some valid results
					for (Location location : DAOLocation.getLocationRoots()) {
						if(location.getLocationType().getCategory()!=LocationCategory.MOVEABLE && getAcceptedAdminLocations()!=null && getAcceptedAdminLocations().size()>0 && !getAcceptedAdminLocations().contains(location)) {
							continue;
						}
						if(!SpiritRights.canRead(location, Spirit.getUser())) {
							continue;
						}
						roots.add(location);
					}
				}
				active = top.initializeLayoutForTop(roots, 0, depth);
			} else {
				active = top.initializeLayoutForParents(location.getHierarchy(), 0, depth);
			}
		} else {
			active = top.initializeLayoutForMain(location, 0, 0);
		}
		
		activeLocationPanel = active;
		
		removeAll();
		add(BorderLayout.CENTER, top);
		revalidate();
		repaint();		

	}
	

	public Location getBioLocation() {
		return activeLocationPanel.getBioLocation();
	}
			
	public Set<Integer> getSelectedPoses() {
		return getRackPanel().getSelectedPoses();
	}
	
	public Set<Container> getSelectedContainers() {
		return getRackPanel().getSelectedContainers();
	}
	
	private int push = 0;
	public void setSelectedPoses(List<Integer> selectedPoses) {
		getRackPanel().setSelectedPoses(selectedPoses);
		if(push>0) return;
		try {
			push++;
			//fire the selection event
			for (RackDepictorListener listener : getRackDepictorListeners()) {					
				listener.onSelect(selectedPoses, null, false);
			}
		} finally {
			push--;
		}
	}
	
	public void setSelectedContainers(Collection<Container> selection) {
		getRackPanel().setSelectedPoses(Container.getPoses(selection));
		getRackPanel().repaint();
		
		if(push>0) return;
		try {
			push++;
			//fire the selection event
			for (RackDepictorListener listener : getRackDepictorListeners()) {					
				listener.onSelect(getRackPanel().getSelectedPoses(), null, false);
			}
		} finally {
			push--;
		}
	}
	
	public void addRackDepictorListener(RackDepictorListener listener) {
		this.rackPanel.addRackDepictorListener(listener);
	}
	public List<RackDepictorListener> getRackDepictorListeners() {
		return rackPanel.getRackDepictorListeners();
	}
	
	public void setDisplayChildren(boolean displayChildren) {
		this.displayChildren = displayChildren;
		updateView();
	}
	
	
	public boolean isDisplayChildren() {
		return displayChildren;
	}


	/**
	 * @param showOneEmptyPosition the showOneEmptyPosition to set
	 */
	public void setShowOneEmptyPosition(boolean showOneEmptyPosition) {
		this.showOneEmptyPosition = showOneEmptyPosition;
	}

	/**
	 * @return the showOneEmptyPosition
	 */
	public boolean isShowOneEmptyPosition() {
		return showOneEmptyPosition;
	}
	
	public JPanel createZoomPanel() {
		final JButton zoomInButton = new JIconButton(IconType.ZOOM_IN, "");
		final JButton zoomOutButton = new JIconButton(IconType.ZOOM_OUT, "");
		final double[] factors = new double[] {.4, .5, .6, .8, 1, 1.2, 1.6};
		zoomOutButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = Arrays.binarySearch(factors, zoomFactor);
				if(index<0) index = -index;
				if(index<=0) return;
				index--;
				setZoomFactor(factors[index]);
				zoomInButton.setEnabled(true);
				zoomOutButton.setEnabled(index>0);
			}
		});
		zoomInButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = Arrays.binarySearch(factors, zoomFactor);
				if(index<0) index = -index;
				if(index>=factors.length-1) return;
				index++;
				setZoomFactor(factors[index]);
				zoomInButton.setEnabled(index<factors.length-1);
				zoomOutButton.setEnabled(true);
			}
		});
		zoomInButton.setToolTipText("Zoom In");
		zoomOutButton.setToolTipText("Zoom Out");				
		zoomInButton.setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
		zoomOutButton.setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
		
		JPanel panel = UIUtils.createHorizontalBox(zoomOutButton, zoomInButton, Box.createHorizontalGlue());
		panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		panel.setBackground(Color.LIGHT_GRAY);
		return panel;
	}
	
	public double getZoomFactor() {
		return zoomFactor;
	}
	public void setZoomFactor(double zoomFactor) {
		this.zoomFactor = zoomFactor;
		updateView();
	}
	
	public String[][] getLocationLayout( ) {
		return activeLocationPanel.getLocationLayout();
	}
	
	/**
	 * Compute the position where the drop would occur 
	 * @param startPos
	 * @param containers
	 * @return
	 */
	public boolean computeDroppedPoses(int startPos, Collection<Container> containers) {
		if(activeLocationPanel==null) return false;
		boolean res =  getRackPanel().getDropListener().computeDroppedPoses(startPos, -1, containers);
		activeLocationPanel.repaint();
		return res;
	}
	
	
	public List<Integer> getDroppedPoses() {
		return activeLocationPanel==null? null: getDropListener().getDroppedPoses();
	}
		
	/**
	 * Restricts the display of admin locations to the given location. 
	 * This function is used to simplify the view (display of bacteria freezers only)
	 * @param locs
	 */
	public void setAcceptedAdminLocations(Collection<Location> locs) {
		if(locs==null) {
			this.acceptedAdminLocations = null;
		} else {
			this.acceptedAdminLocations = new HashSet<Location>();
			for (Location loc : locs) {				
				while(loc!=null && !acceptedAdminLocations.contains(loc)) {
					acceptedAdminLocations.add(loc);
					loc = loc.getParent();					
				}				
			}
		}
		updateView();
	}
	
	/**
	 * Returns null to display all locations or a set of locations, which could be diplayed. 
	 * @return
	 */
	public Set<Location> getAcceptedAdminLocations() {
		return acceptedAdminLocations;
	}
	
	public RackDropListener getDropListener() {
		return getRackPanel().getDropListener();
	}
	public void setForRevisions(boolean forRevisions) {
		this.forRevisions = forRevisions;
	}
	

}
