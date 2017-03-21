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

package com.actelion.research.spiritapp.spirit.ui.lf;

import java.util.Collection;
import java.util.Collections;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.Strategy;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.TextComboBoxOneNode;

public class CreUserNode extends TextComboBoxOneNode {
	public CreUserNode(FormTree tree, Strategy<String> strategy) {
		this(tree, "CreUser", strategy);			
	}	
	public CreUserNode(FormTree tree, String label, Strategy<String> strategy) {
		super(tree, label, strategy);
	}	
	
	@Override
	public Collection<String> getChoices() {
		String username = SpiritFrame.getUser()==null? System.getProperty("user.name"): SpiritFrame.getUser().getUsername();
		return Collections.singletonList(username);
	}
	
}
