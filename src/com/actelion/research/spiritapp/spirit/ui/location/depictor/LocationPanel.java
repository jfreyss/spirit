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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationType.Disposition;
import com.actelion.research.spiritcore.business.location.LocationType.LocationCategory;
import com.actelion.research.spiritcore.business.location.Privacy;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.UIUtils;

/**
 * There are 3 types of panel:
 * - Parent: name displayed, big child
 * - Main: big name, children, big positions, containers
 * - Children: name, children, small positions
 * 
 * 
 * Examples
 * parent (H91)
 *  - parent (Lab)
 *    - main (Tank)
 *       - child (Tower)    - child (Tower)
 *       	- child (Box)     - child (Box)
 *       
 * @author freyssj
 *
 */
public class LocationPanel extends JPanel {

	private static Color MAIN_BACKGROUND = new Color(230, 240, 255);
	protected static enum Type {PARENT, MAIN, CHILD}
	private Type type;
	
	private final int LEGEND_HEIGHT = FastFont.BIGGER.getSize() + 8;
	private final LocationDepictor depictor;
	private Location location;
		
	private final int MARGIN = 2;
	private final int PADDING = -1;
	private int offset_children = 3;
	private int offset_positions = 3;
	
	private int displayChildrenDepth = 0;
	private int depth = 0;
	private boolean displayPositions;
	private boolean hover;
	
	protected LocationPanel(final LocationDepictor depictor) {
		super(null);
		this.depictor = depictor;
		setFocusable(true);
		
		MouseAdapter ma = new PopupAdapter() {
						
			@Override
			public void mouseExited(MouseEvent e) {
				if(type==Type.MAIN) return;
				hover = false; 
				repaint();				
				setCursor(null);
			}
			
			@Override
			public void mouseMoved(MouseEvent e) {
				if(type==Type.MAIN) return;
				boolean shouldHover = e.getX()>3 && e.getX()<getWidth()-3 && e.getY()>1 && e.getY()< (getComponentCount()==0? getHeight()-5: 20);
				if(hover!=shouldHover) {
					hover = shouldHover;
					repaint();
					setCursor(hover? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR): null);
				}
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if(!hover) return;
				super.mouseClicked(e);
				grabFocus();
				for (RackDepictorListener listener : depictor.getRackDepictorListeners()) {
					if(listener!=null) {
						if(e.getButton()==MouseEvent.BUTTON1) {
							listener.locationSelected(getBioLocation());
						}
					}
				}
			}
			@Override
			protected void showPopup(MouseEvent e) {
				requestFocusInWindow();
				for (RackDepictorListener listener : depictor.getRackDepictorListeners()) {
					if(listener!=null) {
						listener.locationPopup(getBioLocation(), LocationPanel.this, e.getPoint());
					}
				}
			}

			
		};
		
		addMouseListener(ma);
		addMouseMotionListener(ma);
		setOpaque(false);
	}
	
	protected LocationPanel initializeLayoutForMain(Location main, int depth, int displayChildrenDepth) {
		removeAll();
		this.location = main;			
		this.type = Type.MAIN;
		this.depth = depth;
		this.displayChildrenDepth = displayChildrenDepth;		
		this.displayPositions = true;

		setToolTipText(location==null?null : getTooltip(location));
		updateView();
		return this; 
	}
	
	/**
	 * Create a hierarchy of PARENT LocationPanel (no content, no children, just the name)
	 * 
	 * Return the main locationPanel
	 * @param hierarchy [TOP, ..., Parent, MAIN]
	 * @param displayChildrenDepth
	 */
	protected LocationPanel initializeLayoutForParents(List<Location> hierarchy, int depth, int displayChildrenDepth) {
		if(hierarchy==null || hierarchy.size()==0) return this;
		if(hierarchy.size()==1) return initializeLayoutForMain(hierarchy.get(0), depth, displayChildrenDepth);			
				
		this.location = hierarchy.get(0);
		this.type = Type.PARENT;
		this.depth = depth;
		this.displayPositions = false;
		setToolTipText(null);

		Rectangle r2 = new Rectangle(0, LEGEND_HEIGHT, getWidth(), getHeight()-LEGEND_HEIGHT);
		LocationPanel ld = new LocationPanel(depictor);
		ld.setBounds(r2);			
		add(ld);				
		
		setToolTipText(getTooltip(location));
		return ld.initializeLayoutForParents(hierarchy.subList(1, hierarchy.size()), depth, displayChildrenDepth);
	}
	
	protected LocationPanel initializeLayoutForTop(List<Location> children, int depth, int displayChildrenDepth) {
		if(children==null) children = new ArrayList<>();
		
		
		this.type = Type.CHILD;
		this.location = null;
		this.depth = depth;
		this.displayChildrenDepth = displayChildrenDepth;
		
		int cols = (getWidth()<=0? 1200: getWidth()) / 300;		
//		cols = (int) (Math.sqrt(children.size())+1);
		int rows = (children.size()-1) / cols + 1;
		
		double width  = ((double)getWidth() - MARGIN*2) / cols; 
		double height = ((double)getHeight() - LEGEND_HEIGHT - MARGIN) / rows;
		
		if(height>350) height = 350;
		if(width>height*3) width = height*3;
		int i = 0;
		
		removeAll();
		for (Location child : children) {
			int row = i / cols;
			int col = i % cols;
			Rectangle r = new Rectangle(MARGIN + (int)(width * col), LEGEND_HEIGHT + (int)(height * row) , (int)width - PADDING, (int)height - PADDING);
			LocationPanel ld = new LocationPanel(depictor);
			add(ld);
			ld.setBounds(r);
			ld.initializeLayoutForChild(child, depth+1, displayChildrenDepth);
			i++;
		}
		setToolTipText(null);

		return this;
		
	}
	
	protected void initializeLayoutForChild(Location child, int depth, int displayChildrenDepth) {
		this.type = Type.CHILD;
		this.location = child;
		this.depth = depth;
		this.displayChildrenDepth = displayChildrenDepth;
		this.displayPositions = false;
		setToolTipText(getTooltip(location));

		updateView();
		
	}
	
		
	protected void updateView() {
		removeAll();
		if(getWidth()<=0) return;
		if(location==null || !SpiritRights.canRead(location, Spirit.getUser())) return;
		
		offset_children = LEGEND_HEIGHT;
		offset_positions = offset_children; 
		

		//////////////////////////////////////////////////////////////////////////////////////////
		//Display children locations
		if(displayChildrenDepth>0 && location!=null && getWidth()>50 && getHeight()>15) {
			List<Location> children = new ArrayList<>();
			boolean checkFilters = depictor.getAcceptedAdminLocations()!=null
					&& depictor.getAcceptedAdminLocations().size()>0
					&& location.getLocationType().getCategory()!=LocationCategory.MOVEABLE
					&& !Collections.disjoint(depictor.getAcceptedAdminLocations(), location.getHierarchy());
					
			for (Location l : location.getChildren()) {
				//Skip location if we have a restriction (ex: query on bacteria freezer)
				if(checkFilters && l.getLocationType().getCategory()!=LocationCategory.MOVEABLE && depictor.getAcceptedAdminLocations()!=null && depictor.getAcceptedAdminLocations().size()>0 && !depictor.getAcceptedAdminLocations().contains(l)) {
					continue;
				}
				//Skip location if we don't have sufficient rights
				if(!SpiritRights.canRead(l, Spirit.getUser())) continue; 				
				children.add(l);
			}
			Collections.sort(children);
						
			int nChildren = Math.min(120, children.size());
			int height = !displayPositions || location.getBiosamples().size()==0? getHeight(): getHeight()/2;
			if(nChildren>0) {
				int rows;
				int cols;
				double heightChild;				
				double widthChild;
				
				if(location.getLocationType().getDisposition()==Disposition.HORIZONTAL) {
					cols = Math.min(12, nChildren);
					rows = (nChildren-1) / cols + 1;
					cols = (nChildren-1) / rows + 1;
					heightChild = (height-offset_children-MARGIN+PADDING)/rows;				
					widthChild = (getWidth()-MARGIN*2+PADDING)/cols;
					
				} else if(location.getLocationType().getDisposition()==Disposition.VERTICAL) {
					rows = Math.min(12, nChildren);
					cols = (nChildren-1) / rows + 1;
					rows = (nChildren-1) / cols + 1;
					heightChild = (height-offset_children-MARGIN+PADDING)/rows;				
					widthChild = (getWidth()-MARGIN*2+PADDING)/cols;
				} else {
					cols = Math.min(12, (int) Math.sqrt(nChildren-1) + 1); 				
					rows = (nChildren-1) / cols + 1;
					
					heightChild = (height-offset_children-MARGIN+PADDING)/rows;				
					widthChild = (getWidth()-MARGIN*2+PADDING)/cols;
				}
				
//				heightChild = Math.min(heightChild, 400/(depth+1));
//				widthChild = Math.min(widthChild, 400/(depth+1));
				
				offset_positions = offset_children + (int)(rows*heightChild+10);
				
				int child = 0;
				for(Location loc: children) {
					Rectangle r = new Rectangle(
							(int) ((child % cols) * widthChild + MARGIN),
							(int) ((child / cols) * heightChild + offset_children),
							(int) (widthChild - PADDING),
							(int) (heightChild - PADDING));
							
					LocationPanel ld = new LocationPanel(depictor);
					add(ld);
					ld.setBounds(r);
					ld.initializeLayoutForChild(loc, depth+1, heightChild>=16 && widthChild>=32? displayChildrenDepth-1: 0);
					child++;
					if(child>=nChildren) break;
				}
			}
		}
		
		
		//////////////////////////////////////////////////////////////////////////////////////////
		//Display Containers
		if(displayPositions) {
			depictor.getRackPanel().setBiolocation(location);
			JScrollPane rackPanelScrollPane = new JScrollPane(depictor.getRackPanel());
			rackPanelScrollPane.getVerticalScrollBar().setUnitIncrement(200);
			rackPanelScrollPane.getHorizontalScrollBar().setUnitIncrement(200);			
			rackPanelScrollPane.setBounds(0, offset_positions, getWidth(), getHeight()-offset_positions);			
			add(rackPanelScrollPane);
		}
		
	}
	
	
	
	
	
	

	public Location getBioLocation() {
		return location;
	}
	
	@Override
	public void paint(Graphics graphics) {
		
		Graphics2D g = (Graphics2D) graphics;
		Toolkit tk = Toolkit.getDefaultToolkit();
  		@SuppressWarnings("rawtypes")
		Map map = (Map)(tk.getDesktopProperty("awt.font.desktophints"));
  		if (map != null) {
  		    ((Graphics2D)g).addRenderingHints(map);
  		}
		
		int d = Math.min(depth, 4);
		
		if(type==Type.MAIN) {
			g.setPaint(new GradientPaint(0, 0, MAIN_BACKGROUND, getWidth(), getHeight(), getBackground(depth)));			
			g.fillRect(0, 0, getWidth(), getHeight());
		} else {
			g.setPaint(new GradientPaint(0, 0, getChildBackground(depth), getWidth(), getHeight(), getBackground(depth)));			
			g.fillRect(0, 0, getWidth(), getHeight());			
		}

		Privacy inherited = location==null? Privacy.PUBLIC: location.getInheritedPrivacy();
		Color fgColor = hover? Color.BLUE: 
				location==null || location.getPrivacy()==Privacy.INHERITED? Color.BLACK: 
				UIUtils.darker(location.getPrivacy()==Privacy.PRIVATE? Color.RED: location.getPrivacy()==Privacy.PROTECTED? Color.ORANGE: Color.BLACK, .5);

		if(location!=null) {
			
			//Header
			Color bgColor = hover || type==Type.MAIN? 
					UIUtils.getColor(140, 140, 255): UIUtils.getColor(190+d*10, 190+d*10, 210+d*10);// Color.WHITE; //eg==null || inherited==Privacy.PUBLIC? Color.WHITE: UIUtils.getRandomBackground(eg.getName().hashCode());
			Color bg1 = UIUtils.getDilutedColor(bgColor, Color.LIGHT_GRAY, .7);		
			Color bg2 = UIUtils.getTransparentColor(bg1);
			
			g.setPaint(new GradientPaint(0,1, bg1, 0, LEGEND_HEIGHT, bg2));
			g.fillRect(0, 0, getWidth(), LEGEND_HEIGHT);
	
			
			//Draw Name
			if(type==Type.MAIN || type==Type.PARENT) {
				
				//Draw Icon
				if(location.getLocationType()!=null) {
					Image img = location.getLocationType().getImageThumbnail();
					if(img!=null) g.drawImage(img, 2, 2, this);
				}
				int left = 25;

				
				//Draw Description below
				String fullName = location.getName();
				if(location.getDescription()!=null && location.getDescription().length()>0) fullName+=" - "+location.getDescription();
				
				//Draw Name
				g.setFont(type==Type.MAIN? FastFont.BIGGEST: FastFont.BIGGER);
				g.setColor(fgColor);
				g.drawString(fullName, left, FastFont.BIGGER.getSize()+4);
				left += g.getFontMetrics().stringWidth(fullName) + 5;
				
				
				if(type==Type.MAIN) {
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
				}				
	
				//Privacy
				if(location.getEmployeeGroup()!=null) {
					String s = location.getPrivacy() + " (" + location.getEmployeeGroup().getName() + ")";
					g.setFont(FastFont.BOLD);
					g.setColor(inherited.getBgColor().darker().darker());
					g.drawString(s, left+10, g.getFont().getSize()+4);
				}
				
			} else if(getHeight()>16) { //Child Mode with sufficient height
				
				//Draw Icon
				if(location.getLocationType()!=null) {
					Image img = location.getLocationType().getImage();
					if(img!=null) g.drawImage(img, 1, 1, 14, 14, this);
				}
				int left = 16;
				
				g.setColor(fgColor);
				String s = location.getName();
				if(location.getDescription()!=null && location.getDescription().length()>0) s+=" - "+location.getDescription();
				g.setFont(location.getLocationType()==null || location.getLocationType().getCategory()==LocationCategory.ADMIN? FastFont.BOLD: location.getLocationType().getCategory()==LocationCategory.CONTAINER? FastFont.REGULAR: FastFont.MEDIUM);
				g.drawString(s, left, g.getFont().getSize()+1);
								
			}
		}
			
		paintChildren(graphics);

		g.setColor(UIUtils.getColor(50+20*d,50+20*d,50+20*d));		
		g.drawLine(0, 0, getWidth()-1, 0);
		g.drawLine(0, 0, 0, getHeight()-1);
		g.drawLine(getWidth()-1, 0, getWidth()-1, getHeight()-1);
		g.drawLine(0, getHeight()-1, getWidth()-1, getHeight()-1);
		g.setColor(UIUtils.brighter(g.getColor(),.5));		
		g.drawLine(getWidth()-2, 1, getWidth()-2, getHeight()-2);
		g.drawLine(1, getHeight()-2, getWidth()-2, getHeight()-2);


	}
	
	public void setDisplayChildrenDepth(int displayChildrenDepth) {
		this.displayChildrenDepth = displayChildrenDepth;
	}
	
	protected Type getType() {
		return type;
	}
	
	public String[][] getLocationLayout() {
		return depictor.getRackPanel().getLocationLayout();
	}

	public LocationDepictor getDepictor() {
		return depictor;
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
	
	private Color getChildBackground(int depth) {
		return UIUtils.getColor(230,230,235);
	}

	private Color getBackground(int depth) {
		return UIUtils.getColor(245, 245, 250);
	}


}
