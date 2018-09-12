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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.actelion.research.spiritcore.business.audit.DifferenceItem.ChangeType;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;

/**
 * Utility class used to store the list differences between entities.
 * DifferenceList extends a list containing: entityType, entityId, entityName, field, newValue, oldValue
 *
 * @author Joel Freyss
 */
public class DifferenceList extends ArrayList<DifferenceItem> {


	/**
	 * Helper functions to simplify adding entries
	 */
	private String entityType = null;
	private String entityId = null;
	private String entityName = null;
	private Integer sid;

	public DifferenceList() {
	}

	public DifferenceList(String entityType, Object entityId, String entityName, Integer sid) {
		assert entityType!=null;
		assert entityId!=null;
		this.entityType = entityType;
		this.entityId = ""+entityId;
		this.entityName = entityName;
		this.sid = sid;
	}

	public static DifferenceList deserialize(String serialized) {
		DifferenceList res = new DifferenceList();
		if(serialized==null) return res;
		for(String s: serialized.split("\n")) {
			DifferenceItem item = DifferenceItem.deserialize(s);
			if(item!=null) res.add(item);
		}
		return res;
	}

	public String serialize() {
		StringBuilder sb = new StringBuilder();
		for (DifferenceItem a : this) {
			if(sb.length()>0) sb.append("\n");
			sb.append(a.serialize());
		}
		return sb.toString();
	}


	public void add(String key, ChangeType changeType) {
		this.add(entityType, entityId, entityName, key, "", "", sid, changeType);
	}


	public void add(String key, String newValue, String oldValue) {
		this.add(entityType, entityId, entityName, key, newValue, oldValue, sid, ChangeType.MOD);
	}

	public void add(String entityType, String entityId, String entityName, String key, String newValue, String oldValue, Integer sid, ChangeType changeType) {
		assert this.entityId==null || this.entityId.equals(entityId);
		if(newValue==null) newValue = "";
		else if(newValue.length()>100) newValue = newValue.substring(0, 100) + "...";
		if(oldValue==null) oldValue = "";
		else if(oldValue.length()>100) oldValue = oldValue.substring(0, 100) + "...";

		DifferenceItem item = new DifferenceItem(entityType, entityId, entityName, key, newValue, oldValue, sid, changeType);
		super.add(item);
	}

	public DifferenceList filter(String entityType, Serializable entityId, Integer sid) {
		DifferenceList l = new DifferenceList();
		for (DifferenceItem e : this) {
			if(entityType!=null && !entityType.equals(e.getEntityType())) continue;
			if(entityId!=null && !entityId.toString().equals(e.getEntityId())) continue;
			if(sid!=null && e.getSid()!=null && !sid.equals(e.getSid())) continue;
			if(e.getChangeType()==ChangeType.MOD && e.getOldValue()==null && e.getNewValue()==null) continue;
			if(e.getChangeType()==ChangeType.MOD && e.getOldValue()!=null && e.getOldValue().equals(e.getNewValue())) continue;
			l.add(e);
		}
		Collections.sort(l, (e1,e2)-> {
			int c = CompareUtils.compare(e1.getEntityName(), e2.getEntityName());
			if(c!=0) return c;
			c = CompareUtils.compare(e1.getEntityId(), e2.getEntityId());
			if(c!=0) return c;
			return CompareUtils.compare(e1.getField(), e2.getField());
		});
		return l;
	}

	public String toHtmlString(boolean displayEntity) {
		List<String> items = new ArrayList<>();
		for (DifferenceItem e : this) {
			StringBuilder sb = new StringBuilder();
			if(displayEntity) sb.append("<b>" + MiscUtils.removeHtml(e.getEntityName()) + "</b> > ");
			sb.append((e.getField()!=null && !"ACTION".equals(e.getField())? (e.getChangeType()!=null? e.getChangeType().toString()+" ":"") + e.getField(): "")
					+ ((e.getNewValue()!=null && e.getNewValue().length()>0) || (e.getOldValue()!=null && e.getOldValue().length()>0)?
							(e.getField()!=null && e.getField().length()>0?": ":"")
							+ (e.getNewValue()!=null && e.getNewValue().length()>0? e.getNewValue():"")
							+ (e.getOldValue()!=null && e.getOldValue().length()>0? " <i>(was " + e.getOldValue() + ")</i>": "")
							: ""));
			items.add(sb.toString());
		}
		Collections.sort(items);
		return "<html><body style='white-space:nowrap'>" + MiscUtils.flatten(items, "<br>");
	}

	public String toTsvString() {
		StringBuilder sb = new StringBuilder();
		for (DifferenceItem e : this) {
			List<String> l = MiscUtils.listOf(e.getEntityName(), e.getField(), e.getNewValue(), e.getOldValue());
			sb.append(MiscUtils.flatten(l, "\t"));
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return serialize();
	}


}
