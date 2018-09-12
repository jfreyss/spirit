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

package com.actelion.research.spiritcore.business.audit;

import java.util.List;

import com.actelion.research.spiritcore.util.MiscUtils;

public class DifferenceItem {

	public enum ChangeType {
		ADD, MOD, DEL
	}


	private String entityType;
	private String entityId;
	private ChangeType changeType;
	private String entityName;
	private String field;
	private String newValue;
	private String oldValue;
	private Integer sid;

	public DifferenceItem(String entityType, String entityId, String entityName,  String field, String newValue, String oldValue, Integer sid, ChangeType changeType) {
		this.entityType = entityType;
		this.entityId = entityId;
		this.entityName = entityName;
		this.field = field;
		this.newValue = newValue==null?"": newValue.replace('\n', ' ');
		this.oldValue = oldValue==null?"": oldValue.replace('\n', ' ');
		this.sid = sid;
		this.changeType = changeType;
	}


	public static DifferenceItem deserialize(String s) {
		List<String> l = MiscUtils.deserializeStrings(s);
		Integer sid = MiscUtils.parseInt(6<l.size()? l.get(6): null);


		ChangeType changeType;
		try {
			changeType = ChangeType.values()[Integer.parseInt(7<l.size()? l.get(7): null)];
		} catch (Exception e) {
			System.err.println(e+" "+l);
			changeType = 4<l.size() && "Created".equals(l.get(4))? ChangeType.ADD: 6<l.size() && "Deleted".equals(l.get(4))? ChangeType.DEL: ChangeType.MOD;
		}

		return new DifferenceItem(l.get(0), 1<l.size()? l.get(1): null, 2<l.size()? l.get(2): null, 3<l.size()? l.get(3): null, 4<l.size()? l.get(4): null, 5<l.size()? l.get(5): null, sid, changeType);
	}

	public String serialize() {
		return MiscUtils.serializeStrings(MiscUtils.listOf(entityType, entityId, entityName, field, newValue, oldValue, sid==null?"":""+sid, "" + changeType.ordinal()));
	}


	public String getEntityType() {
		return entityType;
	}


	public String getEntityId() {
		return entityId;
	}


	public String getEntityName() {
		return entityName;
	}


	public String getField() {
		return field==null?"": field;
	}


	public String getNewValue() {
		return newValue;
	}


	public String getOldValue() {
		return oldValue;
	}


	public Integer getSid() {
		return sid;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

}
