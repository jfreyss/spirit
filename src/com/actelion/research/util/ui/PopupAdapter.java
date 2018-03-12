/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.util.ui;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.JTable;

public abstract class PopupAdapter extends MouseAdapter {
	
	private JTable table;
	
	public PopupAdapter() {}
	
	public PopupAdapter(JTable table) {
		this.table = table;
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if(table!=null) {
			//Make the table selection on right click also
			Point p = e.getPoint();
			 
			// get the row index that contains that coordinate
			int rowNumber = table.rowAtPoint( p );
			int colNumber = table.columnAtPoint( p );
 
			// set the selected interval of rows. Using the "rowNumber"
			// variable for the beginning and end selects only that one row.
 			if(rowNumber>=0 && colNumber>=0) {
				if(Arrays.binarySearch(table.getSelectedRows(), rowNumber)<0 || Arrays.binarySearch(table.getSelectedColumns(), colNumber)<0) {
					table.setRowSelectionInterval(rowNumber, rowNumber);
					table.setColumnSelectionInterval(colNumber, colNumber);
				}
 			}
		}
		
		if(e.isPopupTrigger()) showPopup(e);
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		if(e.isPopupTrigger()) showPopup(e);
	}
	
	protected abstract void showPopup(MouseEvent e);
	
}
