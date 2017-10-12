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
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.Privacy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class LocationDepictor extends JPanel {

	private Location location;
	private final int LEGEND_HEIGHT = FastFont.BIGGER.getSize() + 8;

	private boolean showOneEmptyPosition = true;
	private double zoomFactor = 1;

	private JScrollPane rackPanelScrollPane;
	private final RackDepictor rackPanel = new RackDepictor(this);
	private boolean forRevisions;


	public LocationDepictor() {
		super(new BorderLayout());

		setPreferredSize(new Dimension(400,400));
		setMinimumSize(new Dimension(300,300));

		rackPanelScrollPane = new JScrollPane(rackPanel);
		rackPanelScrollPane.getVerticalScrollBar().setUnitIncrement(200);
		rackPanelScrollPane.getHorizontalScrollBar().setUnitIncrement(200);
		rackPanelScrollPane.setBounds(0, LEGEND_HEIGHT, getWidth(), getHeight()-LEGEND_HEIGHT);
		add(rackPanelScrollPane);

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
		this.location = loc;

		if(!forRevisions) location = JPAUtil.reattach(location);
		rackPanel.setBiolocation(location);
		validate();
		repaint();
	}

	@Override
	public void doLayout() {
		if(rackPanelScrollPane!=null) {
			rackPanelScrollPane.setBounds(0, LEGEND_HEIGHT, getWidth(), getHeight()-LEGEND_HEIGHT);
		}
		rackPanel.doLayout();
	}

	public Location getBioLocation() {
		return rackPanel.getBioLocation();
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
		zoomOutButton.addActionListener(e-> {
			int index = Arrays.binarySearch(factors, zoomFactor);
			if(index<0) index = -index;
			if(index<=0) return;
			index--;
			setZoomFactor(factors[index]);
			zoomInButton.setEnabled(true);
			zoomOutButton.setEnabled(index>0);
		});
		zoomInButton.addActionListener(e-> {
			int index = Arrays.binarySearch(factors, zoomFactor);
			if(index<0) index = -index;
			if(index>=factors.length-1) return;
			index++;
			setZoomFactor(factors[index]);
			zoomInButton.setEnabled(index<factors.length-1);
			zoomOutButton.setEnabled(true);
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
		doLayout();
	}

	/**
	 * Compute the position where the drop would occur
	 * @param startPos
	 * @param containers
	 * @return
	 */
	public boolean computeDroppedPoses(int startPos, Collection<Container> containers) {
		if(rackPanel==null) return false;
		boolean res =  rackPanel.getDropListener().computeDroppedPoses(startPos, -1, containers);
		repaint();
		return res;
	}


	public List<Integer> getDroppedPoses() {
		return rackPanel==null? null: getDropListener().getDroppedPoses();
	}

	public RackDropListener getDropListener() {
		return getRackPanel().getDropListener();
	}

	public void setForRevisions(boolean forRevisions) {
		this.forRevisions = forRevisions;
	}



	@Override
	public void paint(Graphics graphics) {

		Graphics2D g = (Graphics2D) graphics;

		UIUtils.applyDesktopProperties(g);
		g.setPaint(new GradientPaint(0, 0, UIUtils.getColor(230, 240, 255), getWidth(), getHeight(), UIUtils.getColor(245, 245, 250)));
		g.fillRect(0, 0, getWidth(), getHeight());

		Privacy inherited = location==null? Privacy.PUBLIC: location.getInheritedPrivacy();
		Color fgColor = location==null || location.getPrivacy()==Privacy.INHERITED? Color.BLACK:
			UIUtils.darker(location.getPrivacy()==Privacy.PRIVATE? Color.RED: location.getPrivacy()==Privacy.PROTECTED? Color.ORANGE: Color.BLACK, .5);

		if(location!=null) {

			//Header
			Color bgColor;
			if(location.getInheritedEmployeeGroup()!=null) {
				bgColor = UIUtils.getRandomBackground(location.getInheritedEmployeeGroup().getName().hashCode());
			} else {
				bgColor = UIUtils.getColor(190, 190, 210);
			}
			Color bg1 = UIUtils.getDilutedColor(bgColor, Color.LIGHT_GRAY, .7);
			Color bg2 = UIUtils.getTransparentColor(bg1);

			g.setPaint(new GradientPaint(0,1, bg1, 0, LEGEND_HEIGHT, bg2));
			g.fillRect(0, 0, getWidth(), LEGEND_HEIGHT);


			//Draw Icon
			if(location.getLocationType()!=null) {
				Image img = location.getLocationType().getImageThumbnail();
				if(img!=null) g.drawImage(img, 2, 2, this);
			}
			int left = 25;


			//Draw Description below
			String fullName = location.getHierarchyFull();
			if(location.getDescription()!=null && location.getDescription().length()>0) fullName+=" - "+location.getDescription();

			//Draw Name
			g.setFont(FastFont.BIGGER);
			g.setColor(fgColor);
			g.drawString(fullName, left, FastFont.BIGGER.getSize()+4);
			left += g.getFontMetrics().stringWidth(fullName) + 5;


			//Draw Occupancy of the box
			if(location.getOccupancy()>0) {
				String s = "(" + location.getOccupancy() + (location.getCols()>0? "/" + (location.getCols()*location.getRows()) :"") + " samples)";
				g.setFont(FastFont.REGULAR);
				g.drawString(s, left, g.getFont().getSize()+4);
				left += g.getFontMetrics().stringWidth(s)+5;
			}

			//Draw Barcode to the right
			g.setColor(Color.GRAY);
			g.setFont(FastFont.SMALLER);
			String s = location.getLocationId();
			g.drawString(s, Math.max(left+10, getWidth() - g.getFontMetrics().stringWidth(s) - 4), g.getFont().getSize()+2);

			//Privacy
			if(location.getEmployeeGroup()!=null) {
				s = location.getPrivacy() + " (" + location.getEmployeeGroup().getName() + ")";
				g.setFont(FastFont.BOLD);
				g.setColor(inherited.getBgColor().darker().darker());
				g.drawString(s, left+10, g.getFont().getSize()+4);
			}

			paintChildren(graphics);

			g.setColor(UIUtils.getColor(50,50,50));
			g.drawLine(0, 0, getWidth()-1, 0);
			g.drawLine(0, 0, 0, getHeight()-1);
			g.drawLine(getWidth()-1, 0, getWidth()-1, getHeight()-1);
			g.drawLine(0, getHeight()-1, getWidth()-1, getHeight()-1);
			g.setColor(UIUtils.brighter(g.getColor(),.5));
			g.drawLine(getWidth()-2, 1, getWidth()-2, getHeight()-2);
			g.drawLine(1, getHeight()-2, getWidth()-2, getHeight()-2);

		}


	}

	public String[][] getLocationLayout() {
		return getRackPanel().getLocationLayout();
	}

	public String getTooltip(Location location) {
		if(location==null || location.getId()<=0) return null;
		EmployeeGroup eg = location.getInheritedEmployeeGroup();
		return "<html><div style='font-size:8px'>" +
		location.getLocationType().getName() + ":<br> <b style='font-size:9px'>" + location.getHierarchyFull() + "</b><br>" +
		(location.getDescription()==null || location.getDescription().length()==0?"": location.getDescription() + "<br>") +
		(location.getInheritedPrivacy()!=Privacy.PUBLIC? location.getInheritedPrivacy().getName() + (eg==null?"": " to " + eg.getName() + "<br>"): "") +
		(location.getUpdUser()!=null && location.getUpdDate()!=null && location.getCreDate()!=null && location.getUpdDate().after(location.getCreDate())? "<i>Updated by " + location.getUpdUser() + " [" + FormatterUtils.formatDateOrTime(location.getUpdDate())+ "]</i><br>": "") +
		(location.getCreUser()!=null && location.getCreDate()!=null? "<i>Created by " + location.getCreUser() + " [" + FormatterUtils.formatDateOrTime(location.getCreDate())+ "]</i>": "") +
		"</div></html>";
	}

}
