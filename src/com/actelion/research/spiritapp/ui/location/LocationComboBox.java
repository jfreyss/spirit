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

package com.actelion.research.spiritapp.ui.location;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.util.ui.JTextComboBox;

public class LocationComboBox extends JTextComboBox {
	
	private List<Location> values = new ArrayList<>();

	public LocationComboBox() {
		super(false);
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				showPopup();
			}
		});
	}

	public LocationComboBox(List<Location> values) {
		this();
		this.values = values;
	}
	
	@Override
	public Collection<String> getChoices() {
		List<String> res = new ArrayList<String>();
		for (Location l : values) {
			res.add(l.getName());
		}
		return res;
	}
	
	public Location getSelection() {
		String text = getText();
		for (Location l : values) {
			if(text.equals(l.getName())) return l;
		}
		return null;
	}
	
	public void setSelection(Location loc) {
		if(loc==null) {
			setText("");
			return;
		}
		for (Location l : values) {
			if(loc.equals(l)) {
				setText(loc.getName());
				return;
			}
		}
		setText("");
	}
}
