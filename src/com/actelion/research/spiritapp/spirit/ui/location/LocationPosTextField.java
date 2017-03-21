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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.container.CheckinDlg;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.LocationFormat;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;

public class LocationPosTextField extends JCustomTextField {

	private Biosample biosample;
	private JButton setLocationButton = new JButton("Set");
	private Dimension size = new Dimension(160, 27);
	
	@Override
	public Dimension getMinimumSize() {
		return size;
	}
	
	public LocationPosTextField() {		
		super(JCustomTextField.ALPHANUMERIC, 22);
		
		setFont(FastFont.REGULAR);
		setLayout(null);
		
		setLocationButton.setFont(FastFont.SMALLER);
		setLocationButton.setBorder(null);
		setLocationButton.setToolTipText("Set the location (or checkin/relocate)");
		add(setLocationButton);
		
		setLocationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(LocationPosTextField.this.isFocusOwner() || setLocationButton.isFocusOwner()) {
					openLocationDlg();
				}
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount()>=2) {
					openLocationDlg();
				}
			}
		});
		
		setBiosample(null);
	}

	private void openLocationDlg() {
		if(biosample==null) {
			JExceptionDialog.showError(LocationPosTextField.this, "You must first have a biosample");
		} else {
			new CheckinDlg(Collections.singletonList(biosample), false);
			refreshText();
		}
	}
	
	@Override
	public void setBorder(Border border) {
		if(border==null) return;
		super.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(0, 0, 0, 12)));			
	}
	@Override
	public void doLayout() {
		super.doLayout();
		Dimension size = getSize();
		setLocationButton.setBounds(size.width-18, 2, 18, size.height-4);
	}
	
	public void setBiosample(Biosample b) {
		this.biosample = b;
		refreshText();
	}
	
	public void updateBiosample() throws Exception {
		if(biosample==null) return;
		if(biosample.getContainer()==null) biosample.setContainer(new Container());
			
		DAOLocation.updateLocation(biosample, getText(), SpiritFrame.getUser());
	}
	
	private void refreshText() {	
		String txt = biosample==null || biosample.getLocation()==null? "": biosample.getLocationString(LocationFormat.FULL_POS, SpiritFrame.getUser());			 
		setText(txt);
	}

}
