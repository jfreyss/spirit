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

package com.actelion.research.spiritapp.spirit.ui.pivot.designer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.actelion.research.spiritcore.business.pivot.PivotItem;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;
import com.actelion.research.util.ui.FastFont;

public class PivotGroupPanel extends JPanel implements DragGestureListener, DragSourceListener, Transferable {

	private final int HEADER_WIDTH = 75;//58;
	private final ItemPanelControler controler;
	private int width, height;
	private String name;
	private boolean hover = false;

	public PivotGroupPanel(ItemPanelControler controler, String name) {
		this.name = name;
		this.controler = controler;
		
		DragSource ds = new DragSource();
		ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
		
		setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		setOpaque(false);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {hover = false;repaint();}
			
			@Override
			public void mouseEntered(MouseEvent e) {hover = true;repaint();}
		});
	}
	
	@Override
	public Dimension getPreferredSize() {
		doLayout();
		return new Dimension(width, height);
	}
	
	@Override
	public void doLayout() {
		//Line Header
		int maxWidth = getParent()==null? 500: getParent().getWidth();
		int offset = HEADER_WIDTH + 4; //getFontMetrics(FastFont.REGULAR.deriveSize(10)).stringWidth(name)+8;
		int x = offset;
		int y = 1;
		int max = offset;
		for (int i = 0; i < getComponentCount(); i++) {
			PivotItemPanel comp = (PivotItemPanel) getComponent(i);
			
			int w = (int) comp.getPreferredSize().getWidth();
			if(x+w>maxWidth) {
				x = offset;
				y+=PivotItemPanel.HEIGHT;
			}
			comp.setBounds(x, y, w, PivotItemPanel.HEIGHT);
			x += w;
			max = Math.max(max, x);
		}
		y+=PivotItemPanel.HEIGHT;
		width = Math.min(maxWidth, max + 20);
		height = y+1;
	}
	
	
	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;

		if(hover) {
			g.setBackground(new Color(255,255, 200));
			g.clearRect(0, 0, getWidth(), getHeight());
			
			g.setColor(Color.LIGHT_GRAY);
			g.drawRect(0, 0, getWidth()-1, getHeight()-1);
		}
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(name.startsWith("[")?  Color.DARK_GRAY: Color.BLACK);
		g.setFont(FastFont.REGULAR);
		g.drawString(name+":", (hover?1:0) + HEADER_WIDTH - g.getFontMetrics().stringWidth(name) - 4, PivotItemPanel.HEIGHT/2+5 + (hover?1:0));
		super.paintComponent(g);
		
	}
	

	private List<PivotItem> getItems() {
		List<PivotItem> res = new ArrayList<PivotItem>();
		for (int i = 0; i < getComponentCount(); i++) {
			PivotItemPanel p = (PivotItemPanel) getComponent(i);
			res.add(p.getItem());
		}
		return res;
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if(flavor.equals(PivotItemPanel.ITEM_FLAVOR)) {
			return getItems();
		} 
		throw new UnsupportedFlavorException(flavor);
	}
	
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return PivotItemPanel.supportedFlavors;
	}
	
	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(PivotItemPanel.ITEM_FLAVOR);
	}

	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		
		for (PivotItem item : getItems()) {
			controler.placeItem(item, Where.MERGE);
		}
		controler.updateView();			

		
		BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		paint(g);
		g.dispose();
		
		BufferedImage cur = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		g = cur.getGraphics();
		g.drawImage(img, 0, 0, this);
		g.setColor(Color.BLACK);
//		g.drawRect(0, 0, 31, 31);
		g.dispose();
		
		Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(cur, new Point(0,0), "custom");
		dge.startDrag(cursor, this, this);		
	}
	


	@Override
	public void dragEnter(DragSourceDragEvent dsde) {
	}

	@Override
	public void dragOver(DragSourceDragEvent dsde) {
	}

	@Override
	public void dropActionChanged(DragSourceDragEvent dsde) {
	}

	@Override
	public void dragExit(DragSourceEvent dse) {
	}

	@Override
	public void dragDropEnd(DragSourceDropEvent dsde) {
		controler.onDragEnd();
	}
	
}
