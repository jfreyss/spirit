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

package com.actelion.research.spiritapp.spirit.ui.util.scanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.actelion.research.spiritapp.spirit.ui.biosample.edit.EditBiosampleTable;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.location.Location;

/**
 * Scan rack and populate the given EditBiosampleTable.
 * The scanned containers have to be empty
 * 
 *  preScan and postScan can be overridden
 * @author freyssj
 *
 */
public class ScanRackForTableAction extends ScanRackAction {
	
	private EditBiosampleTable table;
	
	public ScanRackForTableAction(SpiritScanner scanner, EditBiosampleTable table) {
		super("Scan Rack", scanner);
		this.table = table;
	}
	
	@Override
	public Location scan() throws Exception {
		final Biotype biotype = table.getModel().getBiotype();
		//Scan and validate
		Location rack = scanner.scan(biotype.getContainerType());
		
		
		List<Biosample> rows = new ArrayList<Biosample>(rack.getBiosamples());
		for (Biosample b : rows) {
			b.setBiotype(biotype);
		}
		Collections.sort(rows);
		table.setRows(rows);
		table.getModel().initColumns();
		table.resetPreferredColumnWidth();
		return rack;
	}	
	
	

}
