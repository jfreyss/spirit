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

package com.actelion.research.spiritcore.services.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionType;
import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.IAuditable;
import com.actelion.research.spiritcore.business.IObject;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Pair;

/**
 * When a new revision is created, we make sure to add the logged in user, and the date of the DB.
 *
 * @author Joel Freyss
 *
 */
@RevisionEntity
public class SpiritRevisionListener implements EntityTrackingRevisionListener {


	/**
	 * For each new revision, update:
	 * - the user who did it
	 * - the time (taken from the DB)
	 * - the reason for change as set in JPAUtil.getReasonForChange
	 */
	@Override
	public void newRevision(Object revisionEntity) {
		//Update the revision to be saved, by adding the user, the DB date, the reason for change, and by resetting the study.id to -1
		SpiritRevisionEntity rev = (SpiritRevisionEntity) revisionEntity;
		SpiritUser user = JPAUtil.getSpiritUser();
		rev.setUserId(user==null?"NA": user.getUsername());
		rev.setTimestamp(JPAUtil.getCurrentDateFromDatabase().getTime());
		rev.setReason(JPAUtil.getReasonForChange());
		rev.setSid(-1);

		DAOBarcode.reset();
		LoggerFactory.getLogger(SpiritRevisionListener.class).info("New revision > reason="+rev.getReason()+", diff="+rev.getDifference());

	}
	/**
	 * For each entity change, compute the difference to the previous version, and store it
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void entityChanged(Class entityClass, String entityName, Serializable entityId, RevisionType revisionType, Object revisionEntity) {
		SpiritRevisionEntity rev = (SpiritRevisionEntity) revisionEntity;
		if(!IAuditable.class.isAssignableFrom(entityClass)) return;

		//Compute difference
		rev.getCounter().put(entityClass, (rev.getCounter().get(entityClass)==null?0:rev.getCounter().get(entityClass))+1);
		if(DBAdapter.getInstance().isAuditSimplified()) {
			//For speed purpose and if the audit is simplified, we only record what was changed and not the difference
			rev.setDifference(countDifference(rev));
		} else if(rev.getDifference()==null || rev.getDifference().length()<SpiritRevisionEntity.MAX_DIFF_LENGTH) {
			try {
				//Look at the entity change and prepare a message describing the differences between the 2 revisions
				Pair<IAuditable, String> change = DAORevision.getLastChange(revisionType, entityClass, entityId);
				if(change!=null) {
					if(change.getSecond().length()>0) {
						rev.getChanges().add(change);
						rev.setDifference(computeDifference(rev));
					}

					//Set up the study, which was modified
					int sid = change.getFirst().getSid();
					if(rev.getSid()<0) {
						rev.setSid(sid);
					} else if(rev.getSid()!=sid) {
						rev.setSid(0);
					}
				}
			} catch (Exception e) {
				LoggerFactory.getLogger(SpiritRevisionListener.class).warn("Could not compute difference from audit table ", e);
			}
		}
	}


	private String countDifference(SpiritRevisionEntity rev) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Class<?>, Integer> e : rev.getCounter().entrySet()) {
			if(sb.length()>0) sb.append("\n");
			sb.append(e.getValue()+" "+e.getKey().getSimpleName());
		}

		String diff = sb.toString();
		if(diff.length()>SpiritRevisionEntity.MAX_DIFF_LENGTH) diff = diff.substring(0, SpiritRevisionEntity.MAX_DIFF_LENGTH-3) + "...";
		return diff;
	}

	/**
	 *Compute the difference
	 * - Map each change to the list of entities that have been changed
	 * - Build a short human readable description of the change: the description is a combination of either:
	 *     - entityId: change
	 *	   - entityId1, entityId2: change
	 *     - n what: change
	 *     - ...
	 * @param rev
	 */
	@SuppressWarnings("rawtypes")
	private String computeDifference(SpiritRevisionEntity rev) {

		StringBuilder sb = new StringBuilder();
		Map<String, List<IAuditable>> change2entities = new LinkedHashMap<>();
		for (Pair<IAuditable, String> e: rev.getChanges()) {
			List<IAuditable> l = change2entities.get(e.getSecond());
			if(l==null) change2entities.put(e.getSecond(), l = new ArrayList<>());
			l.add(e.getFirst());
		}

		for (Map.Entry<String, List<IAuditable>> e : change2entities.entrySet()) {
			Map<Class, List<Object>> classes2objects = MiscUtils.mapClasses(e.getValue());
			for (Map.Entry<Class, List<Object>> e2 : classes2objects.entrySet()) {
				if(sb.length()>0) sb.append("\n");

				//Add the entity being modified to the message
				String entity = "";
				if(IObject.class.isAssignableFrom(e2.getKey()) ) {
					if(e2.getValue().size()<=2) entity = MiscUtils.flatten(e2.getValue(), ", ") + "> ";
					else entity = e2.getValue().size()+" "+e2.getKey().getSimpleName() + "> ";
				}
				String append = entity + e.getKey().replaceAll("\n", entity + "\n");
				sb.append(append);
				if(sb.length()>=SpiritRevisionEntity.MAX_DIFF_LENGTH) break;
			}
			if(sb.length()>=SpiritRevisionEntity.MAX_DIFF_LENGTH) break;
		}

		String diff = sb.toString();
		if(diff.length()>SpiritRevisionEntity.MAX_DIFF_LENGTH) diff = diff.substring(0, SpiritRevisionEntity.MAX_DIFF_LENGTH-3) + "...";
		return diff;
	}


}
