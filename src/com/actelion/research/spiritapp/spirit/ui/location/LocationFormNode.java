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

import javax.swing.JComponent;

import com.actelion.research.spiritapp.spirit.ui.util.formtree.AbstractNode;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.Strategy;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.util.ui.FastFont;

public class LocationFormNode extends AbstractNode<Location> {

	private LocationBrowser locationBrowser = new LocationBrowser(); 
	
	public LocationFormNode(FormTree tree, String label, Strategy<Location> strategy) {
		super(tree, label, strategy);
		locationBrowser.setFont(FastFont.SMALL);
	}

	@Override
	public JComponent getComponent() {
		return locationBrowser;
	}

	@Override
	public JComponent getFocusable() {
		return locationBrowser;
	}

	@Override
	protected void updateView() {
		if(strategy!=null) {
			locationBrowser.setBioLocation(strategy.getModel());
		}
	}

	@Override
	protected void updateModel() {
		if(strategy!=null) {
			strategy.setModel(locationBrowser.getBioLocation());
		}
		
	}

	@Override
	protected boolean isFilled() {
		return locationBrowser.getBioLocation()!=null;
	}

	
}
