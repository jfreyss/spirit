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

package com.actelion.research.spiritapp.spirit.ui.study.depictor;

import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;


public class Selection {
	
	private final Group group;
	private final int subGroup;
	private final Phase phase;
	
	public Selection() {
		this.group = null;
		this.phase = null;
		this.subGroup = 0;
	}
	public Selection(Group group, Phase phase, int subGroup) {
		this.group = group;
		this.phase = phase;		
		this.subGroup = subGroup;				
	}

	public int getSubGroup() {
		return subGroup;
	}
	
	public Group getGroup() {
		return group;
	}

	public Phase getPhase() {
		return phase;
	}	
	
	@Override
	public boolean equals(Object obj) {
		Selection s2 = (Selection) obj;
		if(s2==null) return false;
		if((s2.group==null && group!=null) || (s2.group!=null && !s2.group.equals(group))) return false;
		if(s2.subGroup!=subGroup) return false;
		if((s2.phase==null && phase!=null) || (s2.phase!=null && !s2.phase.equals(phase))) return false;
		return true;
	}
	
	@Override
	public String toString() {
		return group +  "'" + subGroup  + " - " + phase;
	}

}