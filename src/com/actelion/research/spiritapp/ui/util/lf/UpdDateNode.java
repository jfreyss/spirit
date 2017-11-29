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

package com.actelion.research.spiritapp.ui.util.lf;

import java.util.ArrayList;
import java.util.Collection;

import com.actelion.research.spiritapp.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.ui.util.formtree.Strategy;
import com.actelion.research.spiritapp.ui.util.formtree.TextComboBoxNode;

public class UpdDateNode extends TextComboBoxNode {
	private Collection<String> options = new ArrayList<String>();

	public UpdDateNode(FormTree tree, Strategy<String> strategy) {
		this(tree, "UpdDate", strategy);
	}
	public UpdDateNode(FormTree tree, String label, Strategy<String> strategy) {
		super(tree, label, strategy);
				
		options.add("1 day");
		options.add("7 days");
		options.add("30 days");
		options.add("90 days");
		options.add("365 days");	
	}
	
	@Override
	public Collection<String> getChoices() {
		return options;
	}
			
	
	
}
