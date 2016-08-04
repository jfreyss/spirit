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

import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleOrRackTab;
import com.actelion.research.spiritapp.spirit.ui.scanner.SpiritScanner.Verification;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;
import com.actelion.research.util.ui.scanner.ScannerConfiguration;

/**
 * Scan rack and populate the given BiosampleOrRackTab.
 * The scanned containers have to be empty, the tables are always reset
 * 
 * @author freyssj
 *
 */
public abstract class ScanRackForDepictorAction extends ScanRackAction {
	
	private BiosampleOrRackTab tab;
	private boolean proposeRackCreation;
	
	public ScanRackForDepictorAction(SpiritScanner scanner, BiosampleOrRackTab tab, Verification method, boolean proposeRackCreation) {
		super("Scan Rack", IconType.SCANNER.getIcon(), scanner);
		scanner.setVerification(method);
		this.tab = tab;
		this.proposeRackCreation = proposeRackCreation;
	}
	
	/**
	 * By default, the program will ask for the configuration (ie returns null)
	 * @return
	 */
	public ScannerConfiguration getScannerConfiguration() {
		return null;
	}
	@Override
	public Location scan() throws Exception {	
		//Scan and validate
		Location rack = scanner.scan(getScannerConfiguration(), false, null);			
		tab.setRack(rack);
		return rack;		
	}	
	

	@Override
	public void postScan(Location scannedRack) throws Exception {
		if(proposeRackCreation) {
			new SelectRackAction(scanner) {				
				@Override
				protected void eventRackSelected(Location rack) throws Exception {
					//Update the containers
					for (Biosample b : tab.getBiosamples()) {
						b.setLocPos(rack, rack.parsePosition(b.getContainer().getScannedPosition()));
					}
					tab.repaint();					
				}
			}.actionPerformed(null);
		}
	}
	

}
