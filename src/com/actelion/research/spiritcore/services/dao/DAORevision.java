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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.IObject;
import com.actelion.research.spiritcore.business.biosample.ActionComments;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRevisionEntity;
import com.actelion.research.spiritcore.util.Formatter;

public class DAORevision {

	
	public static class Revision implements Comparable<Revision> {
		private Date date;
		private int revId;
		private String user;
		private List<IObject> entities = new ArrayList<>();
		private RevisionType type;
		
		@Override
		public int compareTo(Revision o) {
			return -(revId-o.revId);
		}
		
		@Override
		public String toString() {
			List<Biosample> biosamples = getBiosamples();
			List<Result> results = getResults();
			List<Study> studies = getStudies();
			List<Test> tests = getTests();
			List<Location> locations = getLocations();
			List<Biotype> biotypes = getBiotypes();
			
			StringBuilder sb = new StringBuilder();
			sb.append(revId + ". " + user + ": " + (type==RevisionType.ADD?"Add": type==RevisionType.DEL?"Del": "Upd"));
			sb.append((studies.size()==0? "": studies.size()==1? " " + studies.get(0).getStudyId(): " " + studies.size() + " Studies") +
			(biosamples.size()==0? "": " " + biosamples.size() + " Biosamples") +
			(locations.size()==0? "": locations.size()==1? " " + locations.get(0).getName(): " " + locations.size() + " Locations") +
			(results.size()>0? " " + results.size() + " Results": "") +
			(biotypes.size()==0? "": biotypes.size()==1? " " + biotypes.get(0).getName(): " " + biotypes.size() + " Biotypes") +
			(tests.size()==0? "": tests.size()==1? " " + tests.get(0).getName(): " " + tests.size() + " Tests " ));
			sb.append(" (" + Formatter.formatDateTime(date) + ")");
			return sb.toString();
		}

		public Date getDate() {
			return date;
		}

		public int getRevId() {
			return revId;
		}

		public String getUser() {
			return user==null? "??": user;
		}
		
		private<T extends IObject> List<T> extract(Class<T> claz) {
			List<T> res = new ArrayList<>();
			for (IObject t : entities) {
				if(claz.isInstance(t)) res.add((T)t);
			}
			return res;
		}		
		
		public List<Biosample> getBiosamples() {
			return extract(Biosample.class);
		}

		public List<Result> getResults() {
			return extract(Result.class);
		}

		public List<Study> getStudies() {
			return extract(Study.class);
		}

		public List<Location> getLocations() {
			return extract(Location.class);
		}

		public List<Biotype> getBiotypes() {
			return extract(Biotype.class);
		}

		public List<Test> getTests() {
			return extract(Test.class);
		}
		public RevisionType getRevisionType() {
			return type;
		}
		
		public List<IObject> getEntities() {
			return entities;
		}
		public void setEntities(List<IObject> entities) {
			this.entities = entities;
		}
		

	}
	
	@SuppressWarnings("unchecked")
	public static List<Revision> getRevisions(IObject obj) {
		long s = System.currentTimeMillis();
		EntityManager session = JPAUtil.getManager();
		AuditReader reader = AuditReaderFactory.get(session);	
		AuditQuery query = reader.createQuery().forRevisionsOfEntity(obj.getClass(), false, true).add(AuditEntity.id().eq(obj.getId()));
		List<Object[]> res = query.getResultList();
		
		Map<String, Revision> map = mapRevisions(res, obj.getClass());
		List<Revision> revisions = new ArrayList<>(map.values());
		Collections.sort(revisions);
		LoggerFactory.getLogger(DAORevision.class).debug("Loaded revisions for " + obj+" in "+(System.currentTimeMillis()-s)+"ms");
		return revisions;
	}
	
	
	public static Revision getRevision(int revId) {
		long s = System.currentTimeMillis();
		EntityManager session = JPAUtil.getManager();
		AuditReader reader = AuditReaderFactory.get(session);	
		
		List<Class<? extends IObject>> entityClasses = new ArrayList<>();
		entityClasses.add(Study.class);
		entityClasses.add(Biosample.class);
		entityClasses.add(Result.class);
		entityClasses.add(Location.class);
		entityClasses.add(Biotype.class);
		entityClasses.add(Test.class);
		
		List<Object[]> objects = queryForRevisions(reader, entityClasses, revId, revId, null);
		
		Map<String, Revision> map = mapRevisions(objects, null);
		assert map.values().size()==1;
		Revision rev = map.values().iterator().next();
		LoggerFactory.getLogger(DAORevision.class).debug("Loaded revisions " + revId + " in "+(System.currentTimeMillis()-s)+"ms");
		return rev;
	}
	
	public static List<Revision> getRevisions(String userFilter, Date untilDate, int daysBefore, boolean studies, boolean samples, boolean results, boolean locations, boolean admin) {
		if(untilDate==null) untilDate = new Date();
		long s = System.currentTimeMillis();
		EntityManager session = JPAUtil.getManager();
		AuditReader reader = AuditReaderFactory.get(session);
		Calendar cal = Calendar.getInstance();
		cal.setTime(untilDate);
		cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH)+1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		untilDate = cal.getTime();
		
		cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH)-daysBefore);
		Date date1 = cal.getTime();
		int rev1;
		try {
			rev1 = reader.getRevisionNumberForDate(date1).intValue() + 1;
		} catch (Exception e) {
			rev1 = 0;
		}
		int rev2;
		try {
			rev2 = reader.getRevisionNumberForDate(untilDate).intValue();
		} catch (Exception e) {
			rev2 = 0;
		}
		
		List<Class<? extends IObject>> entityClasses = new ArrayList<>();
		if(studies) entityClasses.add(Study.class);
		if(samples) entityClasses.add(Biosample.class);
		if(results) entityClasses.add(Result.class);
		if(locations) entityClasses.add(Location.class);
		if(admin) entityClasses.add(Biotype.class);
		if(admin) entityClasses.add(Test.class);
		
		List<Object[]> objects = queryForRevisions(reader, entityClasses, rev1, rev2, userFilter);
		Map<String, Revision> map = mapRevisions(objects, null);

		List<Revision> revisions = new ArrayList<>(map.values());
		Collections.sort(revisions);
		LoggerFactory.getLogger(DAORevision.class).debug("Loaded revisions in "+(System.currentTimeMillis()-s)+"ms");
		return revisions;
	}
	
	private static List<Object[]> queryForRevisions(AuditReader reader, List<Class<? extends IObject>> entityClasses, int minRev, int maxRev, String userFilter) {
		List<Object[]> res = new ArrayList<>();
		
		for(Class<?> claz: entityClasses ) {
			AuditQuery query = reader.createQuery().forRevisionsOfEntity(claz, false, true).add(AuditEntity.revisionNumber().between(minRev, maxRev));
			if(userFilter!=null && userFilter.length()>0) {
				query = query.add(AuditEntity.property("updUser").eq(userFilter));
			}
			res.addAll(query.getResultList());
		}
		return res;
	}
	

	@SuppressWarnings("unchecked")
	private static Map<String, Revision> mapRevisions(List<Object[]> objects, Class<?> claz){
		Map<String, Revision> res = new TreeMap<>();
		Set<Integer> addAndDelRevIds = new HashSet<>();
		for (Object[] a: objects) {
			if(claz!=null && !claz.isInstance(a[0])) continue;
			
			IObject entity = (IObject) a[0];
			SpiritRevisionEntity rev = (SpiritRevisionEntity) a[1];
			RevisionType type = (RevisionType) a[2];
			
			if(type==RevisionType.ADD || type==RevisionType.DEL) {
				addAndDelRevIds.add(rev.getId());
			}
			
			String key = rev.getId() + "." + type.name();
			Revision r = res.get(key);
			if(r==null) {
				r = new Revision();
				r.date = rev.getRevisionDate();
				r.revId = rev.getId();
				r.user = rev.getUserId();
				r.type = type;
				res.put(key, r);
			}		
			r.getEntities().add(entity);			
		}
		
		//Clean revisions:
		// - Hide MOD for ADD revisions
		// - Hide MOD for DEL revisions
		// - ADD and DEL in one revision should not be possible
		for (Integer id : addAndDelRevIds) {
			res.remove(id + "." + RevisionType.MOD.name());
		}
		
		return res;
	}
	
	
	/**
	 * Restore the given objects. This function merges the objects, while setting the updDate, updUser and adding the comments
	 * @param objects
	 * @param user
	 * @param comments
	 * @throws Exception
	 */
	public static void restore(List<? extends IObject> objects, String user, String comments) throws Exception {
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn =  session.getTransaction();
		
		try {
			if(comments==null|| comments.trim().length()==0) throw new Exception("You must give a reason");
			txn.begin();
			Date now = JPAUtil.getCurrentDateFromDatabase();

			for (Object object : objects) {
				IObject clone = ((IObject) object);
				updateUpd(clone, now, user, comments);
				
				
				session.merge(clone);
					
			}
			txn.commit();		
			txn = null;
		} catch (Exception e) {
			if(txn!=null && txn.isActive()) txn.rollback();
			throw e;
		} 
	}
	

	/**
	 * Cancel the change, ie go to the version minus one for each object in the revision.
	 * 
	 * If this revision is a deletion->insert older version
	 * If this revision is a insert->delete
	 * If this revision is an update->update older version
	 * 
	 * @param revision
	 * @param user
	 * @param comments
	 * @throws Exception
	 */
	public static void revert(Revision revision, String user, String comments) throws Exception {
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn =  session.getTransaction();
		
		try {
			txn.begin();
			Date now = JPAUtil.getCurrentDateFromDatabase();
			int revId = revision.getRevId();
			
			AuditReader reader = AuditReaderFactory.get(session);
			
			
			//Query modified entities during this revision
			List<Class<? extends IObject>> entityClasses = new ArrayList<>();
			entityClasses.add(Study.class);
			entityClasses.add(Biosample.class);
			entityClasses.add(Result.class);
			entityClasses.add(Location.class);
			entityClasses.add(Biotype.class);
			entityClasses.add(Test.class);						
			List<Object[]> res = queryForRevisions(reader, entityClasses, revId, revId, null);
			
			
			List<IObject> toDelete = new ArrayList<>();
			List<IObject> toInsert = new ArrayList<>();
			List<IObject> toUpdate = new ArrayList<>();
			
			for (Object[] a : res) {
				IObject entity = (IObject) a[0];
				RevisionType type = (RevisionType) a[2];
				if(revision.type==type) {
					IObject obj = reader.find(entity.getClass(), entity.getId(), revId-1);
					if(obj==null) throw new Exception("The "+entity.getClass().getSimpleName()+" "+entity.getId()+" could not be restored at rev="+(revId-1));
					if(type==RevisionType.ADD) {
						toDelete.add(obj);
					} else if(type==RevisionType.DEL) {
						toInsert.add(obj);
					} else if(type==RevisionType.MOD) {
						toUpdate.add(obj);
					}
				}
			}
			
			LoggerFactory.getLogger(DAORevision.class).debug("toInsert="+toInsert.size()+" toDelete="+toDelete.size()+" toUpdate="+toUpdate.size());
			for (IObject o : toInsert) {
				updateUpd(o, now, user, comments);
				LoggerFactory.getLogger(DAORevision.class).debug("persist "+o.getClass().getSimpleName()+" "+o.getId()+":"+o);
				if(o.getId()<0) {
					session.persist(o);
				} else {
					session.merge(o);
				}
			}
			for (IObject o : toUpdate) {
				updateUpd(o, now, user, comments);
				LoggerFactory.getLogger(DAORevision.class).debug("merge "+o.getClass().getSimpleName()+" "+o.getId()+":"+o);
				session.merge(o);
			}
			for (IObject o : toDelete) {
				updateUpd(o, now, user, comments);
				LoggerFactory.getLogger(DAORevision.class).debug("remove "+o.getClass().getSimpleName()+" "+o.getId()+":"+o);
				session.remove(o);
			}
			
			txn.commit();		
			txn = null;
		} catch (Exception e) {
			if(txn!=null && txn.isActive()) txn.rollback();
			throw e;
		} 
	}

	
	/**
	 * Updates the date, user, comments of the given object 
	 * @param clone
	 * @param now
	 * @param user
	 * @param comments
	 * @throws Exception
	 */
	private static void updateUpd(IObject clone, Date now, String user, String comments) throws Exception {
		if(clone instanceof Biosample) {
			((Biosample) clone).addAction(new ActionComments(((Biosample) clone), comments));
			((Biosample) clone).setUpdDate(now);
			((Biosample) clone).setUpdUser(user);
		} else if(clone instanceof Study) {
			((Study) clone).setNotes((((Study) clone).getNotes()==null?"": ((Study) clone).getNotes() + " - ") +  comments);
			((Study) clone).setUpdDate(now);
			((Study) clone).setUpdUser(user);
		} else if(clone instanceof Test) {
			((Test) clone).setUpdDate(now);
			((Test) clone).setUpdUser(comments);
		} else if(clone instanceof Result) {
			((Result) clone).setComments((((Result) clone).getComments()==null?"": ((Result) clone).getComments() + " - ") + comments);
			((Result) clone).setUpdDate(now);
			((Result) clone).setUpdUser(user);
		} else if(clone instanceof Location) {
			((Location) clone).setUpdDate(now);
			((Location) clone).setUpdUser(user);
		} else if(clone instanceof Test) {
			((Test) clone).setUpdDate(now);
			((Test) clone).setUpdUser(user);
		} else if(clone instanceof Biotype) {
			((Biotype) clone).setUpdDate(now);
			((Biotype) clone).setUpdUser(user);
		} else {
			throw new Exception("Invalid object "+clone);
		}
	}
	

	
}
