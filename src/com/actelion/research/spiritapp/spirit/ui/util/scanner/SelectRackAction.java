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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.util.ui.JExceptionDialog;

public abstract class SelectRackAction extends AbstractAction {
	
	private final SpiritScanner model;
	
	public SelectRackAction(SpiritScanner model) {
		super("Set RackId");
		this.model = model;
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		showDlgForRackCreation();
	}
	
	
	/**
	 * Propose creation of new rack, 
	 * to be called in postscan if needed
	 */
	public void showDlgForRackCreation() {
		final SelectRackDlg dlg = new SelectRackDlg(model.getScannerConfiguration(), model.getScannedRack());
		dlg.setVisible(true);
		
		if(dlg.isSuccess()) {
			try {
				eventRackSelected(dlg.getSelection());
			} catch(Exception e) {
				JExceptionDialog.showError(e);
				showDlgForRackCreation();
			}
			
		}
	}
	
	/**
	 * To be overridden by implementing class
	 * @param rack
	 */
	protected abstract void eventRackSelected(Location rack) throws Exception;

}
