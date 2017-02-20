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

package com.actelion.research.spiritapp.spirit.ui.study.depictor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JScrollPane;

import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class ZoomScrollPane extends JLayeredPane {
	private JScrollPane sp;
	private JButton zoomIn = new JIconButton(IconType.ZOOM_IN,"");
	private JButton zoomOut = new JIconButton(IconType.ZOOM_OUT,"");
	
	public ZoomScrollPane(final StudyDepictor depictor) {
		super();
		
		sp = new JScrollPane(depictor);
		sp.getVerticalScrollBar().setUnitIncrement(20);
		add(zoomIn, JLayeredPane.POPUP_LAYER);
		add(zoomOut, JLayeredPane.POPUP_LAYER);
		add(sp, JLayeredPane.DEFAULT_LAYER);
		
		
		zoomIn.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				depictor.setSizeFactor(depictor.getSizeFactor()+1);
				zoomIn.setEnabled(depictor.getSizeFactor()<3);
				zoomOut.setEnabled(depictor.getSizeFactor()>-3);
				ZoomScrollPane.this.validate();
			}
		});
		zoomOut.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				depictor.setSizeFactor(depictor.getSizeFactor()-1);
				zoomIn.setEnabled(depictor.getSizeFactor()<2);
				zoomOut.setEnabled(depictor.getSizeFactor()>-2);
				ZoomScrollPane.this.validate();
			}
		});
	}
	

	@Override
	public void doLayout() {	
		sp.setBounds(0, 0, getWidth(), getHeight());
		
		zoomOut.setBounds(getWidth()-48, 2, 24, 24);
		zoomIn.setBounds(getWidth()-48+22, 2, 24, 24);
		
	}
	
	
}
