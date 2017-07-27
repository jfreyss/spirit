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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Amount;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.biosample.LocationFormat;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JComponentNoRepaint;

public class ContainerLabel extends JComponentNoRepaint {

	public static final Color CONTAINERID_COLOR = UIUtils.getColor(0, 0, 128);

	private ContainerDisplayMode displayMode;

	private ContainerType containerType;
	private String cidOrBid;
	private String containerId;
	private String fullLocation;
	private Amount amount;
	private Location location;

	public static enum ContainerDisplayMode {
		CONTAINER_TYPE,
		CONTAINERID,
		NAME_POS,
		CONTAINERID_OR_BIOSAMPLEID,
		FULL
	}

	public ContainerLabel() {
		this(ContainerDisplayMode.FULL);
	}

	public ContainerLabel(ContainerDisplayMode displayMode) {
		this.displayMode = displayMode;
		setOpaque(true);
	}

	public ContainerLabel(ContainerDisplayMode displayMode, Container container) {
		this(displayMode);
		setContainer(container);
	}

	public void setDisplayMode(ContainerDisplayMode displayMode) {
		this.displayMode = displayMode;
	}

	public void setContainer(Container container) {
		this.containerType = container==null? null: container.getContainerType();
		this.cidOrBid = container==null? null: container.getContainerOrBiosampleId();
		this.containerId = container==null? null: container.getContainerId();
		this.amount = container==null? null: container.getAmount();
		this.location = container==null? null: container.getLocation();

		if(container!=null && container.getLocation()!=null && container.getFirstBiosample()!=null ) {
			fullLocation = container.getFirstBiosample().getLocationString(LocationFormat.MEDIUM_POS, SpiritFrame.getUser());
		} else {
			fullLocation = null;
		}
	}

	public void setBiosample(Biosample biosample) {
		this.containerType = biosample==null? null: biosample.getContainerType();
		this.cidOrBid = biosample==null? null: biosample.getContainerId()==null? biosample.getSampleId(): biosample.getContainerId();
		this.containerId = biosample==null? null: biosample.getContainerId();
		this.amount = biosample==null? null: biosample.getAmountAndUnit();
		this.location = biosample==null? null: biosample.getLocation();

		if(biosample!=null && biosample.getLocation()!=null) {
			fullLocation = biosample.getLocationString(LocationFormat.MEDIUM_POS, SpiritFrame.getUser());
		} else {
			fullLocation = null;
		}
	}


	@Override
	protected void paintComponent(Graphics g2) {
		Graphics2D g = (Graphics2D) g2;
		super.paintComponent(g);

		if(!isVisible()) return;
		if(amount==null && containerType==null && location==null) return;

		if(displayMode==ContainerDisplayMode.FULL) {
			if(containerType!=null) {
				g.setFont(FastFont.SMALL);
				g.setColor(CONTAINERID_COLOR);
				String s = (containerType!=ContainerType.UNKNOWN && !containerType.isMultiple()? containerType.getShortName() + " ":"") + (containerId==null?"":containerId);
				g.drawString(s, 2, g.getFont().getSize()+1);
			}

			//Paint amount
			if(amount!=null && amount.getQuantity()!=null) {
				//Paint Amount
				g.setFont(FastFont.SMALL);
				g.setColor(Color.BLACK);
				String s = amount.toString();
				g.drawString(s, getWidth()-g.getFontMetrics().stringWidth(s)-2, g.getFont().getSize()+1);
			}

			//Paint Location
			if(fullLocation!=null) {
				boolean canView = SpiritRights.canRead(location, SpiritFrame.getUser());

				if(canView) {
					boolean canEdit = SpiritRights.canEdit(location, SpiritFrame.getUser());
					if(location.getLocationType()==null) g.setColor(LF.COLOR_ERROR_FOREGROUND);
					else if(location.getId()<=0) g.setColor(LF.COLOR_WARNING_FOREGROUND);
					else if(!canEdit) g.setColor(Color.BLACK);
					else g.setColor(LF.FGCOLOR_WRITE);
				} else {
					g.setColor(LF.FGCOLOR_READ);
				}
				g.setFont(FastFont.REGULAR);
				g.drawString(fullLocation, 2, FastFont.REGULAR.getSize() + FastFont.SMALL.getSize());
			}


		} else if(displayMode==ContainerDisplayMode.NAME_POS) {
			//Paint Location
			if(fullLocation!=null) {
				boolean canView = SpiritRights.canRead(location, SpiritFrame.getUser());
				if(canView) {
					boolean canEdit = SpiritRights.canEdit(location, SpiritFrame.getUser());
					if(location.getLocationType()==null) g.setColor(LF.COLOR_ERROR_FOREGROUND);
					else if(location.getId()<=0) g.setColor(LF.COLOR_WARNING_FOREGROUND);
					else if(!canEdit) g.setColor(Color.BLACK);
					else g.setColor(LF.FGCOLOR_WRITE);
				} else {
					g.setColor(LF.FGCOLOR_READ);
				}

				g.setFont(FastFont.REGULAR);
				g.drawString(fullLocation, 2, FastFont.REGULAR.getSize()+4);
			}
		} else if(displayMode==ContainerDisplayMode.NAME_POS) {
			//Paint Location
			if(fullLocation!=null) {
				boolean canView = SpiritRights.canRead(location, SpiritFrame.getUser());


				if(canView) {
					boolean canEdit = SpiritRights.canEdit(location, SpiritFrame.getUser());
					if(location.getLocationType()==null) g.setColor(LF.COLOR_ERROR_FOREGROUND);
					else if(location.getId()<=0) g.setColor(LF.COLOR_WARNING_FOREGROUND);
					else if(!canEdit) g.setColor(Color.BLACK);
					else g.setColor(LF.FGCOLOR_WRITE);
				} else {
					g.setColor(LF.FGCOLOR_READ);
				}

				g.setFont(FastFont.REGULAR);
				g.drawString(fullLocation, 2, FastFont.REGULAR.getSize()+4);
			}
		} else {
			//display image
			if(containerType!=null) {
				Image img = containerType.getImage(FastFont.getDefaultFontSize()*2);
				g.drawImage(img, 1, 0, this);
			}

			String containerId = displayMode==ContainerDisplayMode.CONTAINERID_OR_BIOSAMPLEID? cidOrBid: this.containerId;

			if(displayMode==ContainerDisplayMode.CONTAINER_TYPE ) {
				g.setColor(CONTAINERID_COLOR);
				if(containerType!=null && containerType.getName()!=null) {
					g.setFont(FastFont.REGULAR);
					g.drawString(containerType.getShortName(), 24, FastFont.REGULAR.getSize()+4);
				}
			} else if(containerId!=null && containerId.length()>0) {
				//Check if it is created
				g.setColor(CONTAINERID_COLOR);
				g.setFont(FastFont.REGULAR);
				g.drawString(containerId, 24, FastFont.REGULAR.getSize()+4);
			}
		}
	}

	@Override
	public Dimension getPreferredSize() {
		int height = FastFont.getDefaultFontSize()*2;
		if(displayMode==ContainerDisplayMode.FULL) {
			return new Dimension(Math.max(80, 4 + Math.max(
					fullLocation==null?0: getFontMetrics(FastFont.REGULAR).stringWidth(fullLocation),
							containerId==null?0:getFontMetrics(FastFont.REGULAR).stringWidth( (containerType==null?"":containerType.getName()+": ") + containerId))), height);
		} else  if(displayMode==ContainerDisplayMode.NAME_POS) {
			return new Dimension(Math.max(45, 16 + (fullLocation==null?0: getFontMetrics(FastFont.REGULAR).stringWidth(fullLocation))), height);
		} else if(displayMode==ContainerDisplayMode.CONTAINERID_OR_BIOSAMPLEID || displayMode==ContainerDisplayMode.CONTAINERID) {
			return new Dimension(Math.max(45, 24 + (cidOrBid==null?0:getFontMetrics(FastFont.REGULAR).stringWidth(cidOrBid))), height);
		} else {
			return new Dimension(45, height);
		}
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

}
