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

import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.util.ui.JCustomTextField;

public class ContainerTextField extends JCustomTextField {

	/**ContainerType used to suggest name*/
//	private ContainerType containerType;
		
//	private JButton generateButton = new JButton("Gen");
	private Dimension size = new Dimension(160, 27);
	
	@Override
	public Dimension getPreferredSize() {
		return size;
	}
	
	@Override
	public Dimension getMinimumSize() {
		return size;
	}
	
	public ContainerTextField() {		
		super(JCustomTextField.ALPHANUMERIC, 15);
		setLayout(null);
		
//		generateButton.setFont(FastFont.REGULAR.deriveSize(8));
//		generateButton.setBorder(null);
//		generateButton.setToolTipText("Set the location (or checkin/relocate)");
//		add(generateButton);
//		 
//		generateButton.addActionListener(new ActionListener() {
//			private String generated = null;
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				if(ContainerIdTextField.this.isFocusOwner() || generateButton.isFocusOwner()) {
//					if(containerType==null) {
//						JExceptionDialog.show(ContainerIdTextField.this, "You must first select a containerType");
//					} else {
//						if(generated==null) {
//							generated = DAOBarcode.getNextId(containerType);
//						}
//						setText(generated);
//					}
//				}
//			}
//		});
		
	}

	
//	@Override
//	public void doLayout() {
//		Dimension size = getSize();
//		generateButton.setBounds(size.width-18, 1, 18, size.height-2);
//		generateButton.setEnabled(getText().length()==0);
//	}
	
	public void setContainerType(ContainerType containerType) {
//		this.containerType = containerType;
//		generateButton.setEnabled(containerType!=null && containerType.getBarcodeType()==BarcodeType.GENERATE);
	}
	
//	@Override
//	public void setBorder(Border border) {
//		if(border==null) {
//			super.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 17));
//		} else {
//			super.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(0, 0, 0, 17)));			
//		}
//	}

}
