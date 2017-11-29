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

package com.actelion.research.spiritapp.ui.location;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationLabeling;
import com.actelion.research.spiritcore.business.location.LocationType;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class LocationBrowser extends JPanel {

	public static final String PROPERTY_LOCATION_SELECTED = "location_selected";
	public static enum LocationBrowserFilter {
		ALL,
		RACKS,
		CONTAINER
	}

	private LocationBrowserFilter filter;
	private CardLayout cardLayout = new CardLayout();
	private Location location = null;

	private JCustomTextField locationTextField = new JCustomTextField();
	private List<LocationComboBox> locationComboBoxes = new ArrayList<>();

	private Dimension layoutSize = new Dimension(220, FastFont.getDefaultFontSize()+12+2);
	private boolean allowTextEditing = true;
	private JPanel textPanel = new JPanel(new BorderLayout());
	private JPanel comboPanel = new JPanel(null) {
		@Override
		public void doLayout() {
			int width = getWidth();
			if(width<=0) return;

			int x = 0;
			int maxX = 0;
			int y = 0;
			boolean first = true;
			for (int i = 0; i < locationComboBoxes.size(); i++) {
				LocationComboBox c = locationComboBoxes.get(i);
				Location l = c.getSelection();
				int w = l==null?50: getFontMetrics(c.getFont()).stringWidth(l.getName())+36;

				if(first || x+w+5<width) {
					c.setBounds(x, y, w, FastFont.getDefaultFontSize()+12);
					x+=w-2;
					first = false;
				} else {
					y+=FastFont.getDefaultFontSize()+12+2;
					x=0;
					c.setBounds(x, y, w, FastFont.getDefaultFontSize()+12);
					x+=w-2;
				}
				maxX = Math.max(maxX, x);
			}
			layoutSize.width = width;
			layoutSize.height = y + FastFont.getDefaultFontSize()+12+2;
		}
	};

	public LocationBrowser() {
		this(LocationBrowserFilter.ALL);
	}


	public LocationBrowser(LocationBrowserFilter filter) {
		super();
		this.filter = filter;
		setLayout(cardLayout);
		setOpaque(true);
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(), BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0, Color.GRAY), BorderFactory.createLineBorder(Color.LIGHT_GRAY))));

		locationTextField.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
		locationTextField.setFont(FastFont.REGULAR);
		locationTextField.addTextChangeListener(ev-> {
			try {
				updateBioLocation(locationTextField.getText());
			} catch(Exception e) {
				JExceptionDialog.showError(e);
			}
		});

		comboPanel.setBackground(Color.WHITE);
		textPanel.setOpaque(false);
		comboPanel.setOpaque(false);
		add("text", textPanel);
		add("combo", comboPanel);
		cardLayout.show(LocationBrowser.this, "combo");

		textPanel.add(BorderLayout.CENTER, locationTextField);

		MouseListener ma = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(isEnabled() && allowTextEditing) {
					cardLayout.show(LocationBrowser.this, "text");
					locationTextField.setText(location==null?"": location.getHierarchyFull());
					locationTextField.selectAll();
					locationTextField.requestFocusInWindow();
				}
			}
		};
		FocusListener fl = new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(), BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0, Color.GRAY), BorderFactory.createLineBorder(Color.LIGHT_GRAY))));
			}

			@Override
			public void focusGained(FocusEvent e) {
				Color c = UIUtils.getColor(115,164,209);
				setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(c, 1), BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0, Color.GRAY), BorderFactory.createLineBorder(Color.LIGHT_GRAY))));
			}
		};

		final AWTEventListener listener = event-> {
			if(((MouseEvent)event).getID()==MouseEvent.MOUSE_CLICKED) {
				if(event.getSource()!=locationTextField && locationTextField.isFocusOwner()) {
					try {
						updateBioLocation(locationTextField.getText());
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		};


		locationTextField.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK);
			}
			@Override
			public void focusLost(FocusEvent e) {
				try {
					updateBioLocation(locationTextField.getText());
				} catch(Exception ex) {
					ex.printStackTrace();
				}
				Toolkit.getDefaultToolkit().removeAWTEventListener(listener);
			}
		});

		comboPanel.addMouseListener(ma);
		locationTextField.addFocusListener(fl);
		updateView();
	}

	public void setAllowTextEditing(boolean allowTextEditing) {
		this.allowTextEditing = allowTextEditing;
	}
	public boolean isAllowTextEditing() {
		return allowTextEditing;
	}

	@Override
	public void setFont(Font font) {
		super.setFont(font);
		if(locationTextField!=null) locationTextField.setFont(font);
	}

	private void updateBioLocation(String fullLocation) throws Exception {

		fullLocation = fullLocation.replaceAll("[\n\r]", "");
		cardLayout.show(LocationBrowser.this, "combo");
		if(fullLocation.length()==0) {
			setBioLocation(null);
		} else {
			Location loc = DAOLocation.getCompatibleLocation(fullLocation, SpiritFrame.getUser());
			if(loc==null) throw new Exception("Invalid location: "+fullLocation);
			setBioLocation(loc);
		}
		LocationBrowser.this.firePropertyChange(PROPERTY_LOCATION_SELECTED, null, getBioLocation());
	}

	@Override
	public Dimension getPreferredSize() {
		comboPanel.doLayout();
		Dimension minSize = getMinimumSize();
		Dimension dim = new Dimension(Math.max(layoutSize.width + 2, minSize.width), Math.max(layoutSize.height+2, minSize.height));
		return dim;
	}

	private int push = 0;
	private void updateView() {
		comboPanel.removeAll();
		locationComboBoxes.clear();
		List<Location> hierarchy = new ArrayList<>();
		if(location!=null) {
			location = JPAUtil.reattach(location);
			hierarchy = location.getHierarchy();
		}

		Location parent = null;
		for (int i = 0; i < hierarchy.size()+1; i++) {
			final Location parentFinal = parent;

			//Find possible choices
			List<Location> nextChildren = new ArrayList<>();
			for (Location l : parent==null? DAOLocation.getLocationRoots(): parent.getChildren()) {
				if(l.getLocationType()==null) continue;
				if(!SpiritRights.canRead(l, SpiritFrame.getUser())) continue;
				if(filter==LocationBrowserFilter.CONTAINER && l.getLocationType().getPositionType()!=LocationLabeling.NONE) continue;
				if(filter==LocationBrowserFilter.RACKS && l.getLocationType().getPositionType()!=LocationLabeling.NONE && l.getLocationType()!=LocationType.RACK) continue;

				nextChildren.add(l);
			}

			if(nextChildren.size()==0) break;

			Collections.sort(nextChildren);
			Location sel = i<hierarchy.size()? hierarchy.get(i): null;
			final LocationComboBox locComboBox = new LocationComboBox(nextChildren);

			locComboBox.setSelection(sel);
			locComboBox.addTextChangeListener(evt -> {
				if(push>0) return;
				try {
					push++;
					//New location selected
					if(locComboBox.getSelection()==null) {
						location = parentFinal;
					} else {
						location = locComboBox.getSelection();
					}
					updateView();
					LocationBrowser.this.firePropertyChange(PROPERTY_LOCATION_SELECTED, null, location);
					if(getParent()!=null && getParent().getParent() instanceof JScrollPane) {
						final JScrollPane sp = (JScrollPane) getParent().getParent();
						SwingUtilities.invokeLater(()-> {
							sp.getHorizontalScrollBar().setValue(sp.getHorizontalScrollBar().getMaximum());
						});
					}
				} finally {
					push--;
				}

			});
			locationComboBoxes.add(locComboBox);
			comboPanel.add(locComboBox);

			nextChildren = new ArrayList<>();
			parent = sel;
		}

		comboPanel.validate();
		repaint();


	}

	public void setBioLocation(Location location) {
		if(push>0) return;
		this.location = location;
		updateView();
	}

	public Location getBioLocation() {
		return location;
	}


}
