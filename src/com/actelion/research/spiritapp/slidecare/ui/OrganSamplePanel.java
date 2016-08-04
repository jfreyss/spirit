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

package com.actelion.research.spiritapp.slidecare.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
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
import java.io.IOException;
import java.util.EnumSet;

import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.ui.icons.ImageFactory;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoFormat;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoSize;
import com.actelion.research.spiritcore.business.slide.SampleDescriptor;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;

/**
 * Panel used to represent the description of a sample (type, container, metadata)
 * @author freyssj
 *
 */
public class OrganSamplePanel extends JPanel  implements DragGestureListener, DragSourceListener, Transferable {

	
	public static final DataFlavor SAMPLING_FLAVOR = new DataFlavor(OrganSamplePanel.class, "SamplingItem");
	public static final DataFlavor[] supportedFlavors = new DataFlavor[] {SAMPLING_FLAVOR};

	public static final int WIDTH = 180;
	public static final int HEIGHT = 38;
	
	private Biosample compatible;
	private TemplatePreviewOnePanel slidePanel = null;
	private boolean hover = false;
	private SampleDescriptor containerSample;
	
	public OrganSamplePanel() {
		setMinimumSize(new Dimension(WIDTH, HEIGHT));
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {hover = false;repaint();}
			
			@Override
			public void mouseEntered(MouseEvent e) {hover = true;repaint();}
		});
		
		DragSource ds = new DragSource();
		ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this);
		setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
	}

	public OrganSamplePanel(SampleDescriptor slideSample, TemplatePreviewOnePanel slidePanel) {
		this();
		setSample(slideSample);
		this.slidePanel = slidePanel;		
	}
	
	public boolean isInSlide() {
		return slidePanel!=null;
	}
	
	public void setSample(SampleDescriptor slideSample) {
		this.containerSample = slideSample;
		this.compatible = slideSample==null? null: slideSample.createCompatibleBiosample();
	}
	
	public SampleDescriptor getSample() {
		return containerSample;
	}
	
	@Override
	protected void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		Graphics2D g = (Graphics2D) gr;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		int height = getHeight();
		
		g.setColor(hover? new Color(215, 225, 255): new Color(225, 235, 245));
		UIUtils.fillRoundRect3D(g, 0, 0, getWidth(), height, 5);
		g.setColor(Color.BLUE);
		UIUtils.drawRoundRect3D(g, 0, 0, getWidth(), height, 5);
		
		//draw animalNo
		int animalNo = containerSample.getAnimalNo();
		g.setFont(FastFont.MONO.deriveSize(18).deriveFont(Font.BOLD));
		g.setColor(new Color(160, 175, 235));
		String s = "Animal "+(animalNo+1);
		g.drawString(s, getWidth() - g.getFontMetrics().stringWidth(s)-2, height - 4);
		
		
		
		if(compatible!=null) {
			//draw type
			Image img = ImageFactory.getImage(compatible, 30);
			g.drawImage(img, 1, 2, this);
			g.setColor(Color.GRAY);
			g.setFont(FastFont.REGULAR.deriveSize(9));
			g.drawString(compatible.getBiotype().getName(), 30, 11);
			
			//draw container
			if(containerSample.getContainerType()!=null) {
				img = containerSample.getContainerType().getImageThumbnail();
				g.drawImage(img, 1, height-img.getHeight(this)-3, this);

				//Draw BlocNo
				if(containerSample.getBlocNo()!=null) {
					g.setFont(FastFont.MONO.deriveSize(18).deriveFont(Font.BOLD));
					g.setColor(Color.LIGHT_GRAY);
					g.drawString(""+containerSample.getBlocNo(), 5, height - 6);
					g.setColor(Color.BLACK);
					g.drawString(""+containerSample.getBlocNo(), 6, height - 7);
				}
				
			}
			
			//Draw sample info
			g.setColor(Color.BLACK);
			g.setFont(FastFont.REGULAR.deriveSize(11));
			UIUtils.drawString(g, compatible.getInfos(EnumSet.of(InfoFormat.SAMPLENAME, InfoFormat.METATADATA, InfoFormat.COMMENTS), InfoSize.COMPACT), 30, 23, getWidth()-32, height-23);
		}
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return supportedFlavors;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(SAMPLING_FLAVOR);
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if(flavor.equals(SAMPLING_FLAVOR)) {
			return this;
		} 
		throw new UnsupportedFlavorException(flavor);
	}

	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		paint(g);
		g.dispose();
		
		BufferedImage cur = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		g = cur.getGraphics();
		g.drawImage(img, 0, 0, this);
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, 31, 31);
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
		hover = false;repaint();		
	}

	@Override
	public void dragDropEnd(DragSourceDropEvent dsde) {
		
		if(slidePanel!=null) {			
			//Must remove the item from the slide
			slidePanel.removeSampling(this); 
		}
		
		hover = false;
		repaint();		
	}
	
	
}
