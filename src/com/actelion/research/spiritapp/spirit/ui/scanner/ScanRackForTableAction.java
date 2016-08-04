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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.actelion.research.spiritapp.spirit.ui.biosample.edit.EditBiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.scanner.SpiritScanner.Verification;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

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
	private boolean proposeRackCreation;

	
	public ScanRackForTableAction(SpiritScanner model, EditBiosampleTable table, Verification method, boolean proposeRackCreation) {
		super("Scan Rack", IconType.SCANNER.getIcon(), model);
		model.setVerification(method);
		this.table = table;
		this.proposeRackCreation = proposeRackCreation;
	}
	
	
	@Override
	public void preScan() throws Exception {
		//Check tubes are all empty
		final Biotype biotype = table.getModel().getBiotype();
		if(biotype==null || biotype.isAbstract()) throw new Exception("You can only perform a scan if you select a concrete biotype");
		for (Biosample b : table.getRows()) {
			if(!b.isEmpty()) throw new Exception("You can only perform a scan on an empty table");			
		}		
		super.preScan();
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
	
	@Override
	public void postScan(Location scannedRack) throws Exception {
		if(proposeRackCreation) {
			new SelectRackAction(scanner) {				
				@Override
				protected void eventRackSelected(Location rack) throws Exception {
					//Convert the scanner position to the real position
					for (Biosample b : table.getRows()) {
						b.setLocation(rack);
						if(b.getContainer()!=null) b.setPos(rack==null? -1: rack.parsePosition(b.getContainer().getScannedPosition()));
					}
					table.repaint();					
				}
			}.actionPerformed(null);
		}
	}
	

}
