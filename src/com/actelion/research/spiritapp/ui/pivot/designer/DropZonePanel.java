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

package com.actelion.research.spiritapp.ui.pivot.designer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JPanel;

import com.actelion.research.spiritcore.business.pivot.PivotItem;
import com.actelion.research.spiritcore.business.pivot.PivotItemClassifier;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;
import com.actelion.research.util.ui.FastFont;

public class DropZonePanel extends JPanel implements DropTargetListener {

	private final Set<PivotItem> items = new TreeSet<PivotItem>();
	private final ItemPanelControler controler;
	private final String description;
	
	public DropZonePanel(String description, ItemPanelControler controler, Color background) {
		super();
		this.description = description;
		this.controler = controler;		
		setDropTarget(new DropTarget(this, this));
		setBackground(background);
		setLayout(null);
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				updateView();
			}
		});
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Font f = FastFont.BOLD.increaseSize(10); 
		if(description!=null && description.length()>0) {
			g2.setColor(Color.DARK_GRAY);
			g2.setFont(f);
			g2.drawString(description, getWidth()-g.getFontMetrics().stringWidth(description)-8, 22);
		}


	}
	
	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}
	
	public void updateView() {
		//Clean an recreate the UI
		removeAll();
		Map<String, List<PivotItem>> map = PivotItem.mapClassifier(items);
		
		int x = 110;
		int y = -3;
		
		PivotItemClassifier previousClassifier = null;
		for (String title : map.keySet()) {
			if(map.get(title).size()==0) continue;
			PivotItemClassifier classifier = map.get(title).get(0).getClassifier();			
			if(classifier!=previousClassifier) {
				y+=4;
				previousClassifier = classifier;
			}
			
			PivotGroupPanel groupPanel = new PivotGroupPanel(controler, title);
			add(groupPanel);
			
			for (PivotItem pivotItem : map.get(title)) {
				final PivotItemPanel itemPanel = new PivotItemPanel(controler, pivotItem);
				groupPanel.add(itemPanel);
				itemPanel.addPropertyChangeListener(new PropertyChangeListener() {				
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
					}
				});				
			}
			
			Dimension dim = groupPanel.getPreferredSize();
			
			groupPanel.setBounds(2,  y, dim.width, dim.height);
			y+=dim.height;
			x = Math.max(x, dim.width+4);
		}
		
		setPreferredSize(new Dimension(x, y+10));
		validate();
		repaint();
	}
	
	public void clear() {
		items.clear();
	}
	
	public void addItem(PivotItem item) {
		items.add(item);
	}
	
	public void removeItem(PivotItem item) {
		items.remove(item);
	}
	public boolean hasItem(PivotItem item) {
		return items.contains(item);
	}
	
	
	private List<PivotItem> draggedItems = null;
	@SuppressWarnings("unchecked")
	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		if(dtde.isDataFlavorSupported(PivotItemPanel.ITEM_FLAVOR)) {
			dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
			Transferable transfer = dtde.getTransferable();
			try {
				List<PivotItem> items = (List<PivotItem>) transfer.getTransferData(PivotItemPanel.ITEM_FLAVOR);
				for (PivotItem pivotItem : items) {
					controler.placeItem(pivotItem, this);
				}
				controler.updateView();
				draggedItems = items;
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			dtde.rejectDrag();
		}
	}
	
	@Override
	public void dragExit(DropTargetEvent dte) {
		if(draggedItems!=null) {
			for (PivotItem pivotItem : draggedItems) {
				controler.placeItem(pivotItem, Where.MERGE);
			}
			controler.updateView();
			draggedItems = null;
		}
	}


	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		if(dtde.isDataFlavorSupported(PivotItemPanel.ITEM_FLAVOR)) {
			dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
		} else {
			dtde.rejectDrag();
		}
		
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void drop(DropTargetDropEvent dtde) {
		try {
			
			if(dtde.getTransferable().isDataFlavorSupported(PivotItemPanel.ITEM_FLAVOR)) {
				dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
	
				//Process the drop
				Object obj = dtde.getTransferable().getTransferData(PivotItemPanel.ITEM_FLAVOR);
				List<PivotItem> items = (List<PivotItem>) obj;
				for (PivotItem pivotItem : items) {
					controler.placeItem(pivotItem, this);				
				}
				controler.updateView();
				
				dtde.dropComplete(true);
			} else
				dtde.rejectDrop();
				
		} catch (Exception e) {
			e.printStackTrace();
			dtde.rejectDrop();
		}
		
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (PivotItem item : items) {
			if(sb.length()>0) sb.append(", ");
			sb.append(item);
		}
		return description + ">" + sb;
	}
	
	
	
}
