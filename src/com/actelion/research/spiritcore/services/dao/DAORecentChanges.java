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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.util.CompareUtils;

public class DAORecentChanges {

	public static class RecentChange {
		public Study study;
		
		public List<Biosample> biosamples = new ArrayList<Biosample>();
		public List<Result> results = new ArrayList<Result>();
		
		public Date date;
		public String userId;

		
		@Override
		public String toString() {
			return study+" - "+date+" "+userId;
		}
		
	}
	
	
	private static RecentChange update(Map<Study, RecentChange> res, Study study, Date date, String userId) {
		RecentChange rc = res.get(study);
		if(rc==null) {
			rc = new RecentChange();
			rc.study = study;
			res.put(study, rc);			
		}
		if(date!=null && (rc.date==null || rc.date.before(date))) {
			rc.date = date;
			rc.userId = userId;
		}
		return rc;
	}
	
	public static List<Study> getRecentChangesFast(Date d) throws Exception {		
		EntityManager em = JPAUtil.getManager();
		Set<Study> list1 = new TreeSet<Study>(); 
		list1.addAll(em.createQuery("select s from Study s where s.updDate > ?1 ").setParameter(1, d).setLockMode(LockModeType.NONE).getResultList());	
		list1.addAll(em.createQuery("select b.inheritedStudy from Biosample b where b.updDate > ?1 and b.inheritedStudy is not null").setParameter(1, d).setLockMode(LockModeType.NONE).getResultList());	
		list1.addAll(em.createQuery("select r.biosample.inheritedStudy from Result r where r.updDate > ?1 and r.biosample.inheritedStudy is not null").setLockMode(LockModeType.NONE).setParameter(1, d).getResultList());	
		return new ArrayList<Study>(list1);
	}
	
	public static void main(String[] args) throws Exception {
		JPAUtil.getManager();
		long s = System.currentTimeMillis();

		int ndays = 3;

		Calendar cal = Calendar.getInstance();
		cal.setTime(JPAUtil.getCurrentDateFromDatabase());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR)-ndays);
		Date d = cal.getTime();
		
		long s1, s2;
		s = System.currentTimeMillis();
		for(int i=0; i<3;i++) {
			List<RecentChange> rcs = getRecentChanges(ndays, false, null);
			List<Study> studies = new ArrayList<Study>(); for (RecentChange rc : rcs) { studies.add(rc.study);}			
			DAOStudy.countResultsByStudyTest(studies);
			DAOStudy.countSamplesByStudyBiotype(studies);		
		}
		
		s1 = (System.currentTimeMillis()-s);
		
		s = System.currentTimeMillis();
		for(int i=0; i<3;i++) {
			List<Study> studies = getRecentChangesFast(d);
			System.out.println("DAORecentChanges.main() "+studies);
			DAOStudy.countResultsByStudyTest(studies);
			DAOStudy.countSamplesByStudyBiotype(studies);
			DAOStudy.countRecentSamplesByBiotype(d);		
		}
		s2 = (System.currentTimeMillis()-s);
		System.out.println("DAORecentChanges.main(old) "+s1+"ms");
		System.out.println("DAORecentChanges.main(new) "+s2+"ms");
	}
	
	public static List<RecentChange> getRecentChanges(int days, boolean created, SpiritUser user) throws Exception {
		//Query Last study		
		StudyQuery qStudy = new StudyQuery();
		if(created) qStudy.setCreDays(days);
		else qStudy.setUpdDays(days);	
		List<Study> studies = DAOStudy.queryStudies(qStudy, user);
	
		Map<Study, RecentChange> res = new HashMap<Study, RecentChange>();
		
		for (Study study : studies) {
			Date d = created?study.getCreDate(): study.getUpdDate();
			update(res, study, d, study.getUpdUser());			
		}
		
		//Query Last Biosamples		
		BiosampleQuery qBiosamples = new BiosampleQuery();
		
		if(created) qBiosamples.setCreDays(days);
		else qBiosamples.setUpdDays(days);		
		List<Biosample> biosamples = DAOBiosample.queryBiosamples(qBiosamples, user);

		for (Biosample b : biosamples) {
			Date d = created?b.getCreDate(): b.getUpdDate();
			RecentChange rc = update(res, b.getInheritedStudy(), d, b.getUpdUser());			
			rc.biosamples.add(b);
		}

		//Query Last Results
		ResultQuery qResults = new ResultQuery();
		if(created) qResults.setCreDays(days);
		else qResults.setUpdDays(days);
		qResults.setQuality(null);
		
		List<Result> results = DAOResult.queryResults(qResults, user);

		for (Result r : results) {
			Date d = created?r.getCreDate(): r.getUpdDate();
			RecentChange rc = update(res, r.getStudy(), d, r.getUpdUser());			
			rc.results.add(r);
		}
		
		List<RecentChange> list = new ArrayList<RecentChange>(res.values());
		Collections.sort(list, new Comparator<RecentChange>() {
			@Override
			public int compare(RecentChange o1, RecentChange o2) {					
				return CompareUtils.compare(o1.study, o2.study);
			}
		});

		return list;
		
	}
	
}
