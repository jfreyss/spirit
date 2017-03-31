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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;

public class LocationTextField extends JCustomTextField {

	private LocationBrowser locationBrowser = new LocationBrowser();
	private Dimension size = new Dimension(160, 27);


	@Override
	public Dimension getMinimumSize() {
		return size;
	}

	public LocationTextField() {
		super(JCustomTextField.ALPHANUMERIC, 22);
		locationBrowser.setAllowTextEditing(false);

		setLayout(null);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(!isEnabled())return;
				showPopup();
			}
		});
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				hidePopup();
				try {
					if(getText().length()==0) {
						setBioLocation(null);
					} else {
						Location loc = DAOLocation.getCompatibleLocation(getText(), SpiritFrame.getUser());
						setBioLocation(loc);
					}
				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
				}

			}
		});
		locationBrowser.addPropertyChangeListener(LocationBrowser.PROPERTY_LOCATION_SELECTED, evt -> {
			Location loc = locationBrowser.getBioLocation();

			System.out.println("LocationTextField.LocationTextField() "+getText()+" to become "+  loc);

			setText(loc==null?"": loc.getHierarchyFull());
		});

		addAncestorListener(new AncestorListener(){
			@Override
			public void ancestorAdded(AncestorEvent event){ hidePopup();}
			@Override
			public void ancestorRemoved(AncestorEvent event){ hidePopup();}
			@Override
			public void ancestorMoved(AncestorEvent event){
				if (event.getSource() != LocationTextField.this) hidePopup();
			}});

		AWTEventListener listener = event-> {
			if((event instanceof MouseEvent ) && ((MouseEvent)event).getID()==MouseEvent.MOUSE_CLICKED) {
				if(frame!=null && (event.getSource() instanceof Component) && SwingUtilities.getWindowAncestor((Component) event.getSource())!=frame &&  event.getSource()!=LocationTextField.this) {
					hidePopup();
				}
			}
		};
		Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK);


		setBioLocation(null);
	}


	public void setBioLocation(Location loc) {
		setText(loc==null?"":loc.getHierarchyFull());
		locationBrowser.setBioLocation(loc);
	}

	public Location getBioLocation() throws Exception {
		if(isFocusOwner()) {
			//The user is focused on the textfield and therefore still in editmode
			try {
				if(getText().length()==0) {
					locationBrowser.setBioLocation(null);
				} else {
					Location loc = DAOLocation.getCompatibleLocation(getText(), SpiritFrame.getUser());
					locationBrowser.setBioLocation(loc);
				}
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return locationBrowser.getBioLocation();
	}





	private JDialog frame;
	public void hidePopup() {
		if(frame!=null) {
			frame.dispose();
			frame = null;
		}
	}

	public void showPopup() {

		if(!isShowing() || frame!=null) return;
		final Point p = LocationTextField.this.getLocationOnScreen();

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.CENTER, locationBrowser);
		panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		panel.setBackground(Color.WHITE);

		SwingUtilities.invokeLater(()-> {
			if(frame!=null) frame.dispose();
			if(LocationTextField.this.getTopLevelAncestor() instanceof JDialog) {
				frame = new JDialog((JDialog)LocationTextField.this.getTopLevelAncestor(), false);
			} else if(LocationTextField.this.getTopLevelAncestor() instanceof JFrame) {
				frame = new JDialog((JFrame)LocationTextField.this.getTopLevelAncestor(), false);
			} else {
				System.err.println("Invalid topparent: "+LocationTextField.this.getTopLevelAncestor());
				return;
			}
			frame.setUndecorated(true);
			frame.setContentPane(panel);
			frame.setAlwaysOnTop(true);
			frame.setSize(300, 70);
			int x = p.x;
			int y = p.y+getBounds().height;
			if(y+frame.getHeight()>Toolkit.getDefaultToolkit().getScreenSize().height) {
				x = p.x+getBounds().width;
				y = Toolkit.getDefaultToolkit().getScreenSize().height - frame.getHeight();
			}
			if(x+frame.getWidth()>Toolkit.getDefaultToolkit().getScreenSize().width) {
				x = Toolkit.getDefaultToolkit().getScreenSize().width - frame.getWidth();
			}
			frame.setFocusableWindowState(false);
			frame.setLocation(x, y);
			frame.setVisible(true);
		});

	}


	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g.setColor(new Color(225, 240, 255));
		g.fillRect(getWidth()-16, 2, 14, getHeight()-4);
		g.setColor(Color.LIGHT_GRAY);
		g.drawLine(getWidth()-16, 2, getWidth()-16, getHeight()-2);
		g.setColor(Color.BLACK);

		g.drawOval(getWidth()-12, 6, 8, 8);
		g.drawOval(getWidth()-10, 8, 4, 4);
	}


}
