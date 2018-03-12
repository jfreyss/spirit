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

package com.actelion.research.spiritapp.ui.location;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import com.actelion.research.spiritcore.business.location.LocationType;
import com.actelion.research.spiritcore.business.location.LocationType.LocationCategory;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

public class LocationTypeComboBox extends JGenericComboBox<LocationType> {
	
	public LocationTypeComboBox() {
		super();

		setPreferredWidth(100);
		setMaximumRowCount(35);
		
		repopulate();		
	}
	
	public void repopulate() {
		List<LocationType> values = new ArrayList<>();
		for (LocationType locType : LocationType.getValues()) {
			values.add(locType);
		}
		setValues(values, true);
	}
	
	@Override
	public Component processCellRenderer(JLabel comp, LocationType type, int index) {
		comp.setIcon(type==null? null: new ImageIcon(type.getImageThumbnail()));
		comp.setIconTextGap(0);
		comp.setBackground(UIUtils.getDilutedColor(comp.getBackground(), 
				type==null? Color.WHITE: 
				type.getCategory()==LocationCategory.ADMIN? Color.RED: 
				type.getCategory()==LocationCategory.CONTAINER?Color.CYAN:  
				Color.WHITE, .9));
		return comp;
	}


}
