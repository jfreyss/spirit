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

import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionType;
import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.IAuditable;
import com.actelion.research.spiritcore.business.audit.DifferenceList;
import com.actelion.research.spiritcore.services.SpiritUser;
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
		LoggerFactory.getLogger(SpiritRevisionListener.class).info("New revision > reason=" + rev.getReason());

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
		if(rev.getDifferenceList()==null) {
			//skip
		} else if(DBAdapter.getInstance().isAuditSimplified() && rev.getDifferenceList().size()>4) {
			//Ignore other changes, show only 4 first changes
		} else {
			try {
				//Look at the entity change and prepare a message describing the differences between the 2 revisions
				Pair<IAuditable, DifferenceList> change = DAORevision.getLastChange(revisionType, entityClass, entityId);
				if(change!=null) {
					DifferenceList l = rev.getDifferenceList();
					l.addAll(change.getSecond());
					rev.setDifferenceList(l);

					//Update the modified studyId: >0 when one study is updated, -1 when none is modified, 0 when unknown
					int sid = change.getFirst().getSid();
					if(sid>0) {
						if(rev.getSid()<0) {
							rev.setSid(sid);
						} else if(rev.getSid()!=sid) {
							rev.setSid(0);
						}
					}
				}
			} catch (Exception e) {
				LoggerFactory.getLogger(SpiritRevisionListener.class).warn("Could not compute difference from audit table ", e);
			}
		}
	}

}
