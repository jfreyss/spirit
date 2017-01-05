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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoSize;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.location.Direction;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationLabeling;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.UIUtils;

public class RackDepictor extends JPanel {
	
	private static final Color HIGHLIGHT_COLOR = new Color(225, 235, 205);
	private static final Color SELECTED_COLOR = new Color(245, 245, 225);

	private final RackDropListener dropListener = new RackDropListener(this);
	private RackDepictorRenderer renderer = new DefaultRackDepictorRenderer();

	private final int MARGIN = 2;
	private final LocationDepictor depictor;
	private Location location;
	
	private int cols;
	private int rows;
	
	private int maxSize;	
	private int cellWidth;
	private int cellHeight;
	private int hoverPos = -1;
	
	private final Map<Integer, Container> pos2Containers = new HashMap<>();
	private Map<Integer, Rectangle> pos2Shapes = new HashMap<>();
	private Set<Integer> selectedPoses = new HashSet<>();
	private int focusPos;
	private Set<Integer> highlightPoses = new HashSet<>();
	private Set<Container> highlightContainers = new HashSet<>();
	
	private List<RackDepictorListener> listeners = new ArrayList<>();

	public RackDepictor() {
		this(null);
	}
	
	public RackDepictor(final LocationDepictor depictor) {
		this.depictor = depictor;
				
		setDropTarget(new DropTarget(this, dropListener));
		DragSource dragSource = DragSource.getDefaultDragSource();
		dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, dropListener);
		
		
		setFocusable(true);
		setRequestFocusEnabled(true);
		
		PopupAdapter ma = new PopupAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {
				if(hoverPos>=0) {
					hoverPos =-1;
					setCursor(null);
					repaint();
				}
			}
			
			
			@Override
			public void mouseMoved(MouseEvent e) {
				int pos = getPosAt(e.getX(), e.getY());
				if(pos!=hoverPos) {
					hoverPos = pos;
					setCursor( hoverPos>=0 && pos2Containers.get(hoverPos)!=null? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR): null);
					repaint();
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				
				int pos = getPosAt(e.getX(), e.getY());
				addSelection(pos, e.isControlDown(), e.isShiftDown(), !SwingUtilities.isLeftMouseButton(e));
				requestFocus();
				for (RackDepictorListener listener : getRackDepictorListeners()) {					
					listener.onSelect(selectedPoses, pos2Containers.get(pos), e.getClickCount() == 2);
				}
				repaint();
			}
			
			@Override
			protected void showPopup(MouseEvent e) {
				int pos = getPosAt(e.getX(), e.getY());
				addSelection(pos, e.isControlDown(), e.isShiftDown(), true);
				
				for (RackDepictorListener listener : getRackDepictorListeners()) {					
					listener.onPopup(selectedPoses, pos2Containers.get(pos), RackDepictor.this, e.getPoint());
				}
				repaint();
			}
			
		};
		addMouseListener(ma);
		addMouseMotionListener(ma);
		
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(location==null) return;
				if(e.getKeyCode()==37) { // left
					int pos = location.getLabeling().getNext(location, focusPos, Direction.LEFT_RIGHT, -1);					
					addSelection(pos, e.isControlDown(), e.isShiftDown(), false);
					requestFocus();
					for (RackDepictorListener listener : getRackDepictorListeners()) {					
						listener.onSelect(selectedPoses, pos2Containers.get(pos), false);
					}
				} else if(e.getKeyCode()==38) { // up
					int pos = location.getLabeling().getNext(location, focusPos, Direction.TOP_BOTTOM, -1);
					addSelection(pos, e.isControlDown(), e.isShiftDown(), false);
					requestFocus();
					for (RackDepictorListener listener : getRackDepictorListeners()) {					
						listener.onSelect(selectedPoses, pos2Containers.get(pos), false);
					}
				} else if(e.getKeyCode()==39) { // right
					int pos = location.getLabeling().getNext(location, focusPos, Direction.LEFT_RIGHT, 1);
					addSelection(pos, e.isControlDown(), e.isShiftDown(), false);
					requestFocus();
					for (RackDepictorListener listener : getRackDepictorListeners()) {					
						listener.onSelect(selectedPoses, pos2Containers.get(pos), false);
					}
				} else if(e.getKeyCode()==40) { // down
					int pos = location.getLabeling().getNext(location, focusPos, Direction.TOP_BOTTOM, 1);
					addSelection(pos, e.isControlDown(), e.isShiftDown(), false);
					requestFocus();
					for (RackDepictorListener listener : getRackDepictorListeners()) {					
						listener.onSelect(selectedPoses, pos2Containers.get(pos), false);
					}

				} else if( e.getModifiers() == KeyEvent.CTRL_MASK && e.getKeyCode() == 65) {//CtrlA
					selectedPoses.clear();
					int n = location.getLabeling()==LocationLabeling.NONE? location.getContainers().size(): location.getSize();
					for (int i = 0; i<n; i++) {
						selectedPoses.add(i);						
					}
					focusPos = n-1;
					requestFocus();
					for (RackDepictorListener listener : getRackDepictorListeners()) {					
						listener.onSelect(selectedPoses, null, false);
					}
					repaint();
					return;
				} else {
					return;
				}
			}
		});
		
		ToolTipManager.sharedInstance().registerComponent(this);
	}
	
	public int getPosAt(int x, int y) {
		for (int pos: pos2Shapes.keySet()) {
			Rectangle rect = pos2Shapes.get(pos);
			if(rect.contains(x, y)) return pos;
		}
		return -1;
	}
	
	@Override
	public String getToolTipText(MouseEvent event) {
		int pos = getPosAt(event.getX(), event.getY());
		Container c = pos2Containers.get(pos);
		if(c==null || !SpiritRights.canReadBiosamples(c.getBiosamples(), Spirit.getUser())) return null;
				
		return "<html><body><div style='font-size:8px'>" +
			"<b>" + c.getContainerOrBiosampleId() + "</b>" + 
			("\n" + c.getPrintStudyLabel(Spirit.getUsername())  + "\n" + c.getPrintMetadataLabel(InfoSize.EXPANDED)).replaceAll("(\n)+", "<br>");
	}
	

	protected void createPosCache() {
		pos2Containers.clear();
		if(location==null) return;
		if(location.getLabeling()==LocationLabeling.NONE) {
			List<Biosample> biosamples = new ArrayList<>(location.getBiosamples());
			Collections.sort(biosamples, new Comparator<Biosample>() {
				@Override
				public int compare(Biosample o1, Biosample o2) {
					int c = o1.getPos()-o2.getPos();
					if(c!=0) return c;
					return o1.compareTo(o2);
				}
			});
			List<Container> containers = Biosample.getContainers(biosamples, true);						
			int index = 0;
			for (Container c : containers) {
				pos2Containers.put(index++, c);				
			}			
		} else if(location.getBiosamples()!=null) {
			for (Biosample b : location.getBiosamples()) {
				Container c = b.getContainer();
				if(b.getScannedPosition()!=null) {
					int pos;
					try {
						pos = location.parsePosition(b.getScannedPosition());
						if(pos2Containers.get(pos)!=null) System.err.println("The pos "+pos+" is taken 2 times in "+location);
						pos2Containers.put(pos, c);
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else if(b.getPos()>=0) {
					if(pos2Containers.get(b.getPos())!=null) System.err.println("The pos "+b.getPos()+" is taken 2 times "+location);
					pos2Containers.put(b.getPos(), c);
				} else {
					System.err.println("The pos of "+b+" is null!?");
				}
			}
		}
	}
	
	public void setBiolocation(Location location) {
		this.location = location;
		
		//Create the cache of positions
		createPosCache();
		
		//Set preferred size
		if(location==null) {
			return;
		}
	}
	
	public void doLayout() {
		if(location==null) return;
		maxSize = location.getSize()<=0? pos2Containers.size() + (depictor!=null && depictor.isShowOneEmptyPosition()?1:0) : 
			Math.max(pos2Containers.size(), location.getSize());
				
		if(location.getCols()>0 && location.getRows()>0) {
			cellWidth = (getParent().getWidth()-MARGIN*2) / location.getCols();
			cellHeight = (getParent().getHeight()-MARGIN*2) / location.getRows();			
			cellWidth = Math.min(cellWidth, cellHeight*16/11);
		} else {
			cellWidth = (getParent().getWidth()-MARGIN*2) / 12;
		}
		
		cellWidth = Math.max(30, Math.min(200, cellWidth));
		
		
		cellWidth = (int) (cellWidth  * (depictor==null? 1: depictor.getZoomFactor()));
		cellHeight = cellWidth * 11/16;
		
		if(location.getLabeling()==LocationLabeling.NONE) { 				
			cols = Math.max(4, (getWidth() - 15) / cellWidth); 
			rows = Math.max(4, (int)(.99 + 1.0 * maxSize / cols));			
		} else {
			cols = location.getCols()>0? location.getCols(): (int)(Math.sqrt(maxSize)+.5);
			rows = location.getRows()>0? location.getRows(): (int)(.99 + 1.0 * maxSize / cols);			
			maxSize = Math.max(pos2Containers.size(), cols * rows);											
		}
		if(cols<=0) cols = 1;
		if(rows<=0) rows = 1;
		
				
		pos2Shapes.clear();
		for (int pos = 0; pos < maxSize; pos++) {
			int col = pos % cols;
			int row = pos / cols;
			Rectangle r = new Rectangle(
					MARGIN + (int)(col*cellWidth), 
					MARGIN + (int)(row*cellHeight),				
					(int)((col+1)*cellWidth) - (int)(col*cellWidth), 
					(int)((row+1)*cellHeight) - (int)(row*cellHeight));
			pos2Shapes.put(pos, r);
		}
		
		//Add scrolling bars by setting the size if needed
		Dimension dim = new Dimension(
				(int)(MARGIN*2 + cellWidth * cols), 
						(int)(MARGIN*2 + cellHeight * rows));			
		if(dim.width!=getPreferredSize().width || dim.height!=getPreferredSize().height) {
			setPreferredSize(dim);
			setSize(dim);			
		}
	}
	
	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		Toolkit tk = Toolkit.getDefaultToolkit();
  		@SuppressWarnings("rawtypes")
		Map map = (Map)(tk.getDesktopProperty("awt.font.desktophints"));
  		if (map != null) {
  		    ((Graphics2D)g).addRenderingHints(map);
  		}
		super.paintComponent(g);
		
		for (int pos = 0; pos < maxSize; pos++) {			
			
			Rectangle r = pos2Shapes.get(pos);
			assert r!=null;
			
			Container c = pos2Containers.get(pos);
			boolean selected = selectedPoses.contains(pos);			
			boolean highlight = highlightPoses.contains(pos) || (c!=null && highlightContainers.contains(c));			
			boolean canRead = c==null || SpiritRights.canReadBiosamples(c.getBiosamples(), Spirit.getUser());

			//Draw background
			Color bgColor = dropListener.getPos2Color().get(pos);			
			if(bgColor!=null) {
				//ok
			} else if(selected) {
				bgColor = SELECTED_COLOR;
			} else if(highlight) {
				bgColor = HIGHLIGHT_COLOR;
			} else {
				bgColor = renderer.getWellBackground(location, pos, c);
			}
			g.setColor(bgColor);
			g.fillRect(r.x, r.y, r.width, r.height);
			if(focusPos==pos) {
				g.setColor(UIUtils.getColor(0, 0, 255, 50));			
				g.fillRect(r.x, r.y, r.width, r.height);
			} else if(hoverPos==pos) {
				g.setColor(UIUtils.getColor(255, 255, 0, 50));			
				g.fillRect(r.x, r.y, r.width, r.height);
			}
			
			
			//draw the standard border 
			g.setColor(Color.GRAY);
			g.drawRect(r.x, r.y, r.width, r.height);

			//Draw content
			if(!canRead) {
				g.setColor(Color.LIGHT_GRAY);
				g.drawLine(r.x, r.y, r.x+r.width, r.y+r.height);
				
				if(g.getFontMetrics().stringWidth("NoRights")<r.width) {
					g.setColor(Color.GRAY);
					g.drawString("NoRights", r.x+r.width/2 - g.getFontMetrics().stringWidth("NoRights")/2, r.y + r.height/2);
				}
			
			} else { 
				renderer.paintWellPre(this, g, location, pos, c, r);
				renderer.paintWell(this, g, location, pos, c, r);
				renderer.paintWellPost(this, g, location, pos, c, r);					
			}
						
			//Draw position
			if(r.height>30) {
				g.setFont(FastFont.SMALLER);
				String s = location.formatPosition(pos);
				int w = g.getFontMetrics().stringWidth(s);
				g.setColor(bgColor);
				g.fillRect(r.x+r.width - 1 - w, r.y+1, w, g.getFont().getSize());
				g.setColor(Color.BLACK);
				g.drawString(s, r.x+r.width - w, r.y+g.getFont().getSize());
			}

		}

		//Draw Selection and highlight Borders
		for (int pos = 0; pos < maxSize; pos++) {			
			
			Rectangle r = pos2Shapes.get(pos);
			Container c = pos2Containers.get(pos);
			boolean selected = selectedPoses.contains(pos);			
			boolean highlight = highlightPoses.contains(pos) || (c!=null && highlightContainers.contains(c));			
			
			//Draw highlight border
			if(selected && highlight) {
				g.setStroke(new BasicStroke(2f));
				g.setColor(Color.RED);
				g.drawRoundRect(r.x-1, r.y-1, r.width+2, r.height+2,6, 6);
			} else if(selected) {
				g.setStroke(new BasicStroke(2f));
				g.setColor(Color.BLACK);
				g.drawRoundRect(r.x-1, r.y-1, r.width+2, r.height+2,6,6);
			} else if(highlight) {
				g.setStroke(new BasicStroke(1.5f));
				g.setColor(Color.RED);
				g.drawRect(r.x, r.y, r.width, r.height);
			}
			
		
		}
		

	}
	
	
	public String[][] getLocationLayout() {
		if(location.getSize()<=0 && location.getBiosamples().isEmpty()) return null;
		String[][] res = new String[rows+1][cols+1];
		for (int r = 0; r < rows; r++) {
			res[r+1][0] = location.getLabeling()==LocationLabeling.ALPHA? "" + ((char) ('A' + r)): location.getLabeling()==LocationLabeling.NUM? "" + (1+r): "";
		}
		for (int c = 0; c < cols; c++) {
			res[0][c+1] = location.getLabeling()==LocationLabeling.ALPHA || location.getLabeling()==LocationLabeling.NUM? "" + (1+c): "";
		}
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				int pos = r*cols + c;
				Container container = pos2Containers.get(pos);
				if(container!=null) {
					StringBuilder sb = new StringBuilder();
					sb.append(container.getContainerOrBiosampleId() + "\n" + container.getPrintStudyLabel(Spirit.getUsername()) + "\n" +container.getPrintMetadataLabel(InfoSize.EXPANDED));						
					res[r+1][c+1] = sb.toString();
				} else {
					res[r+1][c+1] = "";
				}
			}
		}
		
		return res;
	}
	
	private int lastPos = -1;
	
	protected void addSelection(int pos, boolean ctrlDown, boolean shiftDown, boolean isRightClick) {
		if(!ctrlDown && !shiftDown && !(isRightClick && selectedPoses.contains(pos))) {
			selectedPoses.clear();
		}
		
		if(shiftDown && focusPos>=0) {
			int col1 = lastPos%cols;
			int row1 = lastPos/cols;

			int newPos = pos;
			int col2 = newPos%cols;
			int row2 = newPos/cols;
						
			Set<Integer> poses = new TreeSet<>();
			for (int row = Math.min(row1, row2); row <= Math.max(row1, row2); row++) {
				for (int col = Math.min(col1, col2); col <= Math.max(col1, col2); col++) {
					poses.add(row*cols+col);					
				}					
			}							
			selectedPoses.clear();
			selectedPoses.addAll(poses);
			focusPos = newPos;
//			lastShiftSelection.clear();
//			lastShiftSelection.addAll(poses);
		} else {
			if(!ctrlDown || !selectedPoses.contains(pos)) {
				selectedPoses.add(pos);
			} else {
				selectedPoses.remove(pos);
			}
			lastPos = pos;
			focusPos = pos;
//			lastShiftSelection.clear();
		}
			
		repaint();
	}
	
	public Set<Integer> getSelectedPoses() {
		return Collections.unmodifiableSet(selectedPoses);
	}
	
	public void setSelectedPoses(Collection<Integer> selectedPoses) {
		this.selectedPoses.clear();
		if(selectedPoses!=null && selectedPoses.size()>0) {
			this.selectedPoses.addAll(selectedPoses);
			this.focusPos = Collections.max(selectedPoses); 
		}
		repaint();
	}
	
	public Set<Container> getSelectedContainers() {
		Set<Container> res = new HashSet<>();
		for (int pos: selectedPoses) {
			if(pos2Containers.get(pos)!=null) {
				res.add(pos2Containers.get(pos));
			}
		}
		return res;
	}
	
	public LocationDepictor getDepictor() {
		return depictor;
	}
	public Location getBioLocation() {
		return location;
	}
	public RackDropListener getDropListener() {
		return dropListener;
	}
	public Set<Integer> getHighlightPoses() {
		return highlightPoses;
	}
	public void setHighlightPoses(Collection<Integer> highlightPoses) {
		this.highlightPoses.clear();
		if(highlightPoses!=null) this.highlightPoses.addAll(highlightPoses);
	}
	public void setHighlightContainers(Collection<Container> highlightContainers) {
		this.highlightContainers.clear();
		if(highlightContainers!=null) this.highlightContainers.addAll(highlightContainers);
	}
	
	public void addRackDepictorListener(RackDepictorListener listener) {
		this.listeners.add(listener);
	}
	public List<RackDepictorListener> getRackDepictorListeners() {
		return listeners;
	}
	

	public void setRackDepictorRenderer(RackDepictorRenderer renderer) {
		this.renderer = renderer;
	}
	public RackDepictorRenderer getRackDepictorRenderer() {
		return renderer;
	}
	
	
}
