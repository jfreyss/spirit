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

import java.util.List;

import com.actelion.research.spiritcore.util.MiscUtils;

/**
 * Action can be treatments, assigned/move to group, volume modification, moved location 
 * It is redundant to the audit but it is used to save 'worthwhile actions'.
 * As such, actions are never updated and can only be deleted when the underlying sample is deleted
 * 
 * @author freyssj
 *
 */
public abstract class ActionBiosample {	
	protected String comments;
		
	public ActionBiosample() {
	}
	
	public String getDetails() {
		return getComments();
	}
	
	public final String getComments() {
		return comments;
	}

	public final void setComments(String comments) {
		this.comments = comments;
	}

	@Override
	public boolean equals(Object obj) {		
		if(obj==null) return false;
		if(!(obj instanceof ActionBiosample)) return false;
		return getDetails().equals(((ActionBiosample)obj).getDetails());
	}
	
	public abstract String serialize();
	
	public static ActionBiosample deserialize(String s) {
		List<String> strings = MiscUtils.deserializeStrings(s);
		if(strings==null || strings.size()==0) return null;
		if("Comments".equals(strings.get(0))) {
			return ActionComments.deserialize(strings);
		} else if("Treatment".equals(strings.get(0))) {
			return ActionTreatment.deserialize(strings);
		} else {
			return null;
		}
	}
}
