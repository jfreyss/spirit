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

package com.actelion.research.spiritcore.business.biosample;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.envers.Audited;

import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;

@Entity
@DiscriminatorValue("Group")
@Audited
public class ActionMoveGroup extends ActionBiosample {
	
	public ActionMoveGroup() {
	}
	
	public ActionMoveGroup(Biosample biosample, Phase phase, Group newGroup, Integer newSubGroup) {
		super(biosample, null);
		setComments((newGroup==null?"Reserve":newGroup)+(newSubGroup==null?"":"/"+(newSubGroup+1)));
	}
	
	@Override
	public String getDetails() {
		return "Assigned to "+getComments();		
	}
	
}
