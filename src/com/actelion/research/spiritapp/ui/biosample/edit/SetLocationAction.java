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

package com.actelion.research.spiritapp.ui.biosample.edit;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.actelion.research.spiritapp.ui.location.CheckinDlg;
import com.actelion.research.util.ui.iconbutton.IconType;

public class SetLocationAction extends AbstractAction {
	private final EditBiosampleTable table;
	
	public SetLocationAction(EditBiosampleTable table) {
		super("Set Location", IconType.LOCATION.getIcon());
		this.table = table;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		new CheckinDlg(table.getRows(), false);
		table.repaint();		
		
	}	


}
