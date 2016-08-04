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

package com.actelion.research.spiritapp.spirit.ui.container;

import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.JPopupMenu;

import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.util.ui.PopupAdapter;

public class ContainerActions {
	
	public static void attachPopup(final ContainerTable table) {
		table.addMouseListener(new PopupAdapter(table) {
			@Override
			protected void showPopup(MouseEvent e) {
				ContainerActions.createPopup(table.getSelection()).show(table, e.getX(), e.getY());				
			}
		});
	}
	
	public static JPopupMenu createPopup(Collection<Container> containers) {			
		return BiosampleActions.createPopup(Container.getBiosamples(containers));
	}

}
