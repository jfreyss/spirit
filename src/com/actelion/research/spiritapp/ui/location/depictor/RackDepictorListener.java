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

package com.actelion.research.spiritapp.ui.location.depictor;

import java.awt.Component;
import java.awt.Point;
import java.util.Collection;
import java.util.List;

import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.location.Location;

public abstract class RackDepictorListener {

	public void onSelect(Collection<Integer> selectedPoses, Container lastSelect, boolean dblClick) {}
	public void onPopup(Collection<Integer> selectedPoses, Container lastSelect, Component comp, Point point) {}
	
	
	public void locationSelected(Location location) {}
	public void locationPopup(Location location, Component comp, Point point) {}	
	
	public boolean acceptDrag() {return false;}
	
	public void containerDropped(List<Container> containers, List<Integer> toPoses) throws Exception {}
	
}
