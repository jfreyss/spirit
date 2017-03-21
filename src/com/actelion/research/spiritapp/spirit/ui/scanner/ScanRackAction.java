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

package com.actelion.research.spiritapp.spirit.ui.scanner;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.iconbutton.IconType;

/**
 * Scan rack and populate the given EditBiosampleTable.
 * The scanned containers have to be empty
 * 
 *  preScan and postScan can be overridden
 * @author freyssj
 *
 */
public abstract class ScanRackAction extends AbstractAction {
	
	protected final SpiritScanner scanner;
	
	public ScanRackAction(String label, SpiritScanner scanner) {
		super(label, IconType.SCANNER.getIcon());
		this.scanner = scanner;
	}
		
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			Location scannedRack = scan();
			postScan(scannedRack);
		} catch(Exception ex) {
			JExceptionDialog.showError(ex);
		}
	}
	
	/**To be implemented*/
	public abstract Location scan() throws Exception;
	
	/**Could be overriden */
	public void postScan(Location scannedRack) throws Exception  {}
	

	public SpiritScanner getScanner() {
		return scanner;
	}
	
}
