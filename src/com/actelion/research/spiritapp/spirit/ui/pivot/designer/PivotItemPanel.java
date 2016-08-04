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
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
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
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;

import com.actelion.research.spiritcore.business.pivot.PivotItem;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;
import com.actelion.research.util.ui.FastFont;

public class PivotItemPanel extends JPanel implements DragGestureListener, DragSourceListener, Transferable, Comparable<PivotItemPanel> {

	public static final int HEIGHT = 16;
	
	public static final DataFlavor ITEM_FLAVOR = new DataFlavor(List.class, "mytype");
	public static final DataFlavor[] supportedFlavors = new DataFlavor[] {ITEM_FLAVOR};

	private final ItemPanelControler controler;
	private final PivotItem item;
	private boolean hover;
	
	
	public PivotItemPanel(final ItemPanelControler controler, final PivotItem item) {	
		super(new GridBagLayout());
		this.item = item;
		this.controler =  controler;
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 1;
		c.weighty = 1;

		setOpaque(false);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {hover = false;repaint();}
			
			@Override
			public void mouseEntered(MouseEvent e) {hover = true;repaint();}
		});
		
		DragSource ds = new DragSource();
		ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
		setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
	}
	
	
	@Override
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		super.paint(g);
		Color c1, c2;
		if(hover) {
			c1 = item.getClassifier().getBgcolor();// new Color(242, 242, 252);
			c2 = new Color(188, 188, 208);
		} else {
			c1 = item.getClassifier().getBgcolor();//new Color(242, 242, 242);
			c2 = new Color(188, 188, 188);
		}
		if(hover) {
			g.setColor(Color.DARK_GRAY);
			g.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 5, 5);
			g.setPaint(new GradientPaint(0, 0, c1, 0, getHeight(), c2));
			g.fillRoundRect(0, 0, getWidth()-2, getHeight()-2, 5, 5);
			g.setColor(Color.LIGHT_GRAY);
			g.drawRoundRect(0, 0, getWidth()-2, getHeight()-2, 5, 5);			
		} else {
			g.setColor(Color.LIGHT_GRAY);
			g.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 5, 5);
			g.setPaint(new GradientPaint(0, 0, c1, 0, getHeight(), c2));
			g.fillRoundRect(0, 0, getWidth()-2, getHeight()-2, 5, 5);
			g.setColor(Color.DARK_GRAY);
			g.drawRoundRect(0, 0, getWidth()-2, getHeight()-2, 5, 5);
		}

		String name = item.getShortName();
		
		
		g.setColor(Color.BLACK);
		g.setFont(FastFont.BOLD);
		g.drawString(name, 4 + 2 + (hover?1:0), getHeight()/2+4 + (hover?1:0));
	}
	
	@Override
	public Dimension getPreferredSize() {		
		return new Dimension(getFontMetrics(FastFont.BOLD).stringWidth(item.getShortName())+10, HEIGHT);
	}
	
	
	

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if(flavor.equals(ITEM_FLAVOR)) {
			return Collections.singletonList(item);
		} 
		throw new UnsupportedFlavorException(flavor);
	}
	
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return supportedFlavors;
	}
	
	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(ITEM_FLAVOR);
	}

	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		controler.placeItem(item, Where.MERGE);
		controler.updateView();
		
		BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		paint(g);
		g.dispose();
		
		BufferedImage cur = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		g = cur.getGraphics();
		g.drawImage(img, 0, 0, this);
		g.setColor(Color.BLACK);
		g.dispose();
		
		Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(cur, new Point(0,0), "custom");
		dge.startDrag(cursor, this, this);

	}

	
	@Override
	public int compareTo(PivotItemPanel o) {
		return item.getSortOrder() - o.getSortOrder();
	}
	
	public int getSortOrder() {
		return item.getSortOrder();
	}
	public PivotItem getItem() {
		return item;
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
