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

package com.actelion.research.spiritapp.ui.util.scanner;

import com.actelion.research.spiritapp.ui.biosample.BiosampleOrRackTab;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.util.ui.scanner.ScannerConfiguration;

/**
 * Scan rack and populate the given BiosampleOrRackTab.
 * The scanned containers have to be empty, the tables are always reset
 * 
 * @author freyssj
 *
 */
public abstract class ScanRackForBiosampleOrRackTabAction extends ScanRackAction {
	
	private BiosampleOrRackTab tab;
	
	public ScanRackForBiosampleOrRackTabAction(SpiritScanner scanner, BiosampleOrRackTab tab) {
		super("Scan Rack", scanner);
		this.tab = tab;
	}
	
	/**
	 * If not overriden, this will ask for the configuration before scanning
	 * @return
	 */
	public ScannerConfiguration getScannerConfiguration() {
		return null;
	}

	/**
	 * Scan and set the update the rack
	 */
	@Override
	public Location scan() throws Exception {	
		Location rack = scanner.scan(getScannerConfiguration(), false);			
		tab.setRack(rack);
		return rack;		
	}	
}
