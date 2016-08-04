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

package com.actelion.research.spiritapp.spirit.ui.location;

import java.awt.Color;

import javax.swing.ImageIcon;

import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.Privacy;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class LocationLabel extends JLabelNoRepaint {
	
	private final boolean fullName;
	
	public LocationLabel(boolean fullName) {
		setOpaque(true);
		this.fullName = fullName;
	}
	
	public void setLocation(Location location) {
		if(location==null) {
			setText("");
			setIcon(null);
		} else {
			Privacy privacy = location.getInheritedPrivacy();
			Color foreground = privacy==Privacy.PRIVATE?Color.RED: privacy==Privacy.PROTECTED?Color.ORANGE: privacy==Privacy.PUBLIC?Color.GREEN: Color.ORANGE;
			setForeground(foreground.darker());
			setText(fullName? location.getHierarchyFull(): location.getName());
			setIcon(location.getLocationType()==null? null: new ImageIcon(location.getLocationType().getImageThumbnail()));
		}
	}

}