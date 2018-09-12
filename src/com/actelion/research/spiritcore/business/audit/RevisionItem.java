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

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.actelion.research.spiritcore.business.IAuditable;
import com.actelion.research.spiritcore.business.audit.DifferenceItem.ChangeType;
import com.actelion.research.spiritcore.business.study.Study;

/**
 * RevisionItem is used to display the audit trail to the user.
 * The Revisions are transaction based: for each transaction, there is one Revision. For each Revision, there are multiple RevisionItem being changed.
 *
 * @author Joel Freyss
 *
 */
public class RevisionItem implements Comparable<RevisionItem> {
	private Revision revision;
	private DifferenceItem differenceItem;


	public RevisionItem() {
	}

	public RevisionItem(Revision revision, DifferenceItem differenceItem) {
		super();
		this.revision = revision;
		this.differenceItem = differenceItem;
	}

	public Study getStudy() {
		return revision.getStudy();
	}

	@Override
	public int compareTo(RevisionItem o) {
		return revision.compareTo(o.revision);
	}

	public Date getDate() {
		return revision.getDate();
	}

	public int getRevId() {
		return revision.getRevId();
	}

	public String getUser() {
		return revision.getUser();
	}

	public String getReason() {
		return revision.getReasonsOfChange(getField());
	}

	public String getEntityType() {
		return differenceItem.getEntityType();
	}

	public String getEntityName() {
		return differenceItem.getEntityName();
	}

	public String getField() {
		return differenceItem.getField();
	}

	public String getOldValue() {
		return differenceItem.getOldValue();
	}

	public String getNewValue() {
		return differenceItem.getNewValue();
	}

	public ChangeType getChangeType() {
		return differenceItem.getChangeType();
	}


	public IAuditable getAuditable() {
		for (IAuditable t : revision.getAuditables()) {
			if(differenceItem.getClass().isInstance(t) && differenceItem.getEntityId().equals(""+t.getSerializableId())) return t;
		}
		return null;
	}

	public static Set<String> getEntityTypes(Collection<RevisionItem> items) {
		Set<String> res = new HashSet<>();
		if(items!=null) {
			for (RevisionItem item : items) {
				res.add(item.getEntityType());

			}
		}
		return res;
	}

}