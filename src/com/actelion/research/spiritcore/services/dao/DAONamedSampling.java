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

package com.actelion.research.spiritcore.services.dao;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.QueryTokenizer;

/**
 * DAO functions linked to sampling templates
 *
 * @author Joel Freyss
 */
public class DAONamedSampling {

	/**
	 * Gets the NamedSamplings, either
	 * - created by the given user
	 * - in the given study, or if null, where the user has rights
	 * @param user
	 * @param orStudy
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<NamedSampling> getNamedSamplings(SpiritUser user, Study study) {
		assert user!=null;

		List<Integer> sids;
		if(study==null) {
			sids = JPAUtil.getIds(DAOStudy.getRecentStudies(user, RightLevel.READ));
		} else {
			sids = Collections.singletonList(study.getId());
		}

		EntityManager session = JPAUtil.getManager();
		Query query = session.createQuery("SELECT ns FROM NamedSampling ns " +
				" WHERE SIZE(ns.samplings)>0 and (" +
				" (ns.creUser = ?1 and ns.study is null) " +
				" or ( " + QueryTokenizer.expandForIn("ns.study.id", sids) + "))")
				.setParameter(1, user.getUsername());


		List<NamedSampling> res = query.getResultList();
		Collections.sort(res);
		Collections.reverse(res);
		return res;
	}

	public static void deleteNamedSampling(NamedSampling ns, SpiritUser user) throws Exception {
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {

			//Check rights
			if(user!=null && !SpiritRights.canEdit(ns, user)) throw new Exception("You are not allowed to delete this sampling");


			txn = session.getTransaction();
			txn.begin();

			ns = session.merge(ns);
			ns.remove();
			session.remove(ns);

			txn.commit();
			txn = null;
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {}
		}
	}
	public static NamedSampling persistNamedSampling(NamedSampling ns, SpiritUser user) throws Exception {
		if(user==null) throw new Exception("the user must be authenticated");
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {

			txn = session.getTransaction();
			txn.begin();
			Date now = JPAUtil.getCurrentDateFromDatabase();

			if(ns.getStudy()!=null) {
				//There is a study
				if(ns.getStudy().getId()<=0) throw new Exception("The study must be already saved. Contact IT");

				//Make sure the study-name combination is unique
				for (NamedSampling n : ns.getStudy().getNamedSamplings()) {
					if(!n.equals(ns) && n.getName().equals(ns.getName())) {
						throw new Exception("The sampling template's name must be unique per study");
					}
				}

			} else {

				//Make sure the creUser-name combination is unique
				int count = session.createQuery("SELECT ns FROM NamedSampling ns " +
						" WHERE " +
						" ns.creUser = ?1 and ns.study = ?2 and ns.id<>?3")
						.setParameter(1, user.getUsername())
						.setParameter(2, ns.getStudy())
						.setParameter(3, ns.getId()).getResultList().size();
				if(count>0) {
					throw new Exception("The sampling template's name must be unique for each user");
				}
			}



			//			ns.setUpdUser(user.getUsername());
			//			ns.setUpdDate(now);
			if(ns.getId()>0) {
				if(!session.contains(ns)) ns = session.merge(ns);
			} else {
				ns.setCreUser(user.getUsername());
				ns.setCreDate(now);
				session.persist(ns);
			}

			txn.commit();
			txn = null;

			if(ns.getStudy()!=null) ns.getStudy().getNamedSamplings().add(ns);


			return ns;
		} catch (Exception e) {
			if(txn!=null && txn.isActive()) try{ txn.rollback();} catch(Exception e2) {}
			throw e;
		}
	}


}
