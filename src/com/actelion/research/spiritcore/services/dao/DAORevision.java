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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.IAuditable;
import com.actelion.research.spiritcore.business.IObject;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.property.SpiritProperty;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.FormatterUtils;

/**
 * DAO functions linked to audit functions
 *
 * @author Joel Freyss
 */
public class DAORevision {

	public static class RevisionQuery {
		private String userIdFilter;
		private int sidFilter;
		private String studyIdFilter;
		private int revId;
		private Date fromDate;
		private Date toDate;
		private boolean studies = true;
		private boolean samples = true;
		private boolean results = true;
		private boolean locations = true;
		private boolean admin = true;

		public RevisionQuery() {
		}

		public RevisionQuery(String userFilter, String studyIdFilter, Date fromDate, Date toDate, boolean studies, boolean samples, boolean results, boolean locations, boolean admin) {
			super();
			this.userIdFilter = userFilter;
			this.studyIdFilter = studyIdFilter;
			this.fromDate = fromDate;
			this.toDate = toDate;
			this.studies = studies;
			this.samples = samples;
			this.results = results;
			this.locations = locations;
			this.admin = admin;
		}

		public int getRevId() {
			return revId;
		}

		public void setRevId(int revId) {
			this.revId = revId;
		}
		public String getUserIdFilter() {
			return userIdFilter;
		}

		public void setUserIdFilter(String userIdFilter) {
			this.userIdFilter = userIdFilter;
		}
		public int getSidFilter() {
			return sidFilter;
		}
		public void setSidFilter(int sidFilter) {
			this.sidFilter = sidFilter;
		}
		public String getStudyIdFilter() {
			return studyIdFilter;
		}
		public void setStudyIdFilter(String studyIdFilter) {
			this.studyIdFilter = studyIdFilter;
		}
		public Date getFromDate() {
			return fromDate;
		}
		public void setFromDate(Date fromDate) {
			this.fromDate = fromDate;
		}
		public Date getToDate() {
			return toDate;
		}
		public void setToDate(Date toDate) {
			this.toDate = toDate;
		}
		public boolean isStudies() {
			return studies;
		}
		public void setStudies(boolean studies) {
			this.studies = studies;
		}
		public boolean isSamples() {
			return samples;
		}
		public void setSamples(boolean samples) {
			this.samples = samples;
		}
		public boolean isResults() {
			return results;
		}
		public void setResults(boolean results) {
			this.results = results;
		}
		public boolean isLocations() {
			return locations;
		}
		public void setLocations(boolean locations) {
			this.locations = locations;
		}
		public boolean isAdmin() {
			return admin;
		}
		public void setAdmin(boolean admin) {
			this.admin = admin;
		}



	}
	public static class Revision implements Comparable<Revision> {
		private Date date;
		private int revId;
		private String user;
		private List<IAuditable> auditable = new ArrayList<>();
		private RevisionType type;

		@Override
		public int hashCode() {
			return revId;
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Revision)) return false;
			return revId == ((Revision)obj).revId && type == ((Revision)obj).type;
		}

		@Override
		public int compareTo(Revision o) {
			return -(revId-o.revId);
		}

		public String getWhat() {
			List<Biosample> biosamples = getBiosamples();
			List<Result> results = getResults();
			List<Study> studies = getStudies();
			List<Test> tests = getTests();
			List<Location> locations = getLocations();
			List<Biotype> biotypes = getBiotypes();
			List<SpiritProperty> properties = getSpiritProperties();
			String t = (type==RevisionType.ADD?"Add": type==RevisionType.DEL?"Del": "Upd") + " ";

			List<String> desc = new ArrayList<>();
			if(studies.size()>0) desc.add("Study (" + t + (studies.size()==1? studies.get(0).getStudyId(): studies.size()) + ")");
			if(biosamples.size()>0) desc.add("Biosample (" + t + (biosamples.size()==1? biosamples.get(0).getSampleId(): biosamples.size()) + ")");
			if(locations.size()>0) desc.add("Location (" + t + (locations.size()==1? locations.get(0).getName(): locations.size())  + ")");
			if(results.size()>0) desc.add("Results (" + t + (results.size()==1? results.get(0).getTest().getName() + " " + results.get(0).getBiosample().getSampleId(): results.size()) + ")");
			if(biotypes.size()>0) desc.add("Biotypes (" + t + (biotypes.size()==1? biotypes.get(0).getName(): biotypes.size()) + ")");
			if(tests.size()>0) desc.add("Tests (" + t + (tests.size()==1? tests.get(0).getName(): tests.size()) + ")");
			if(properties.size()>0) desc.add("Properties (" + t + (properties.size()==1? properties.get(0).getKey(): properties.size()) + ")");

			return desc.size()>3? "Various": MiscUtils.flatten(desc);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(revId + ". " + user + ": ");
			sb.append(getWhat());
			sb.append(" (" + FormatterUtils.formatDateTime(date) + ")");
			return sb.toString();
		}

		public Date getDate() {
			return date;
		}

		public int getRevId() {
			return revId;
		}

		public String getUser() {
			return user==null? "": user;
		}

		private<T> List<T> extract(Class<T> claz) {
			List<T> res = new ArrayList<>();
			for (Object t : auditable) {
				if(claz.isInstance(t)) res.add((T)t);
			}
			return res;
		}

		public List<SpiritProperty> getSpiritProperties() {
			return extract(SpiritProperty.class);
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

		public List<IAuditable> getAuditables() {
			return auditable;
		}

		public void setAuditables(List<IAuditable> entities) {
			this.auditable = entities;
		}
	}

	/**
	 * Returns all revisions of the given entity
	 * @param obj
	 * @param maxRevId
	 * @return
	 */
	public static List<Revision> getLastRevisions(IAuditable obj) {
		return getLastRevisions(obj, -1, -1);
	}



	/**
	 * Returns last revision of the given entity until the given maxRevId
	 * @param obj
	 * @param maxRevId (-1, to ignore)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Revision getLastRevision(IAuditable obj, int maxRevId) {
		List<Revision> revisions = getLastRevisions(obj, maxRevId, 1);
		return revisions.size()>0? revisions.get(0): null;
	}

	/**
	 * Returns all revisions of the given entity until the given maxRevId
	 * @param obj
	 * @param maxRevId (-1, to ignore)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Revision> getLastRevisions(IAuditable obj, int maxRevId, int n) {

		Object id;
		if(obj instanceof IObject) {
			id = ((IObject)obj).getId();
		} else if(obj instanceof SpiritProperty) {
			id = ((SpiritProperty)obj).getKey();
		} else {
			assert false;
			return null;
		}

		long s = System.currentTimeMillis();
		EntityManager session = JPAUtil.getManager();
		AuditReader reader = AuditReaderFactory.get(session);
		AuditQuery query = reader.createQuery().forRevisionsOfEntity(obj.getClass(), false, true);
		query.add(AuditEntity.id().eq(id));

		if(maxRevId>0) {
			query.add(AuditEntity.revisionNumber().le(maxRevId));
		}
		if(n>0) {
			query.setMaxResults(n);
			query.addOrder(AuditEntity.revisionNumber().desc());
		}
		List<Revision> revisions = getRevisions(query.getResultList());
		LoggerFactory.getLogger(DAORevision.class).debug("Loaded revisions for " + obj + "-"+maxRevId + "-" + n + " in " + (System.currentTimeMillis()-s)+"ms");
		return revisions;
	}


	/**
	 * Returns last version of the given entity until the given maxRevId
	 * @param obj
	 * @param maxRevId (-1, to ignore)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static<T extends IAuditable> T getLastVersion(T obj, int maxRevId) {
		Revision rev = getLastRevision(obj, maxRevId);
		if(rev==null) return null;
		assert rev.getAuditables().size()==1;
		return (T) rev.getAuditables().get(0);
	}

	/**
	 * Get the different version of a samples, the first index shows the most recent version
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static<T extends IObject> Map<T, List<T>> getHistories(Collection<T> col) {
		Map<T, List<T>> res = new HashMap<>();
		for (T t : col) {
			res.put(t, getHistory(t));
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	public static<T extends IObject> List<T> getHistory(T obj) {
		long s = System.currentTimeMillis();
		EntityManager session = JPAUtil.getManager();
		AuditReader reader = AuditReaderFactory.get(session);
		AuditQuery query = reader.createQuery()
				.forRevisionsOfEntity(obj.getClass(), true, false)
				.add(AuditEntity.id().eq(obj.getId()))
				.addOrder(AuditEntity.revisionNumber().desc());
		List<T> res = query.getResultList();

		LoggerFactory.getLogger(DAORevision.class).debug("Loaded history for " + obj+" in "+(System.currentTimeMillis()-s)+"ms");
		return res;
	}


	public static Revision getRevision(int revId) {

		RevisionQuery q = new RevisionQuery();
		q.setRevId(revId);

		List<Revision> revisions = queryRevisions(q);
		return revisions.size()>0? revisions.get(0): null;
	}

	public static List<Revision> queryRevisions(RevisionQuery query) {
		assert query!=null;
		long s = System.currentTimeMillis();
		EntityManager session = JPAUtil.getManager();
		AuditReader reader = AuditReaderFactory.get(session);


		int rev1 = 0;
		try {
			rev1 = query.getRevId()>0? query.getRevId(): query.getFromDate()==null? 0: reader.getRevisionNumberForDate(query.getFromDate()).intValue();
		} catch (Exception e) {
			//before first revision->ignore
		}
		int rev2 = Integer.MAX_VALUE;
		try {
			rev2 = query.getRevId()>0? query.getRevId(): query.getToDate()==null? Integer.MAX_VALUE: reader.getRevisionNumberForDate(query.getToDate()).intValue();
		} catch (Exception e) {
			//after last revision->ignore
		}
		LoggerFactory.getLogger(DAORevision.class).debug("getRevisions between " + rev1 + " and " + rev2);
		List<Class<?>> entityClasses = new ArrayList<>();
		if(query.isStudies()) entityClasses.add(Study.class);
		if(query.isSamples()) entityClasses.add(Biosample.class);
		if(query.isResults()) entityClasses.add(Result.class);
		if(query.isLocations()) entityClasses.add(Location.class);
		if(query.isAdmin()) entityClasses.add(Biotype.class);
		if(query.isAdmin()) entityClasses.add(Test.class);
		if(query.isAdmin()) entityClasses.add(SpiritProperty.class);

		List<Revision> revisions = getRevisions(queryForRevisions(reader, entityClasses, rev1, rev2, query.getUserIdFilter(), query.getSidFilter(), query.getStudyIdFilter()));
		LoggerFactory.getLogger(DAORevision.class).debug("Loaded revisions in "+(System.currentTimeMillis()-s)+"ms");

		//Post filter per study
		List<Revision> res = new ArrayList<>();
		for (Revision revision : revisions) {
			if(query.getSidFilter()>0 || (query.getStudyIdFilter()!=null && query.getStudyIdFilter().length()>0) ) {
				boolean ok = false;
				if(!ok) {
					for(Study study: revision.getStudies()) {
						if(query.getSidFilter()>0 && query.getSidFilter()==study.getId()) ok = true;
						else if(query.getStudyIdFilter()!=null && query.getStudyIdFilter().contains(study.getStudyId())) ok = true;
					}
				}
				if(!ok) {
					for(Biosample b: revision.getBiosamples()) {
						if(b.getInheritedStudy()!=null) {
							if(query.getSidFilter()>0 && query.getSidFilter()==b.getInheritedStudy().getId()) ok = true;
							else if(query.getStudyIdFilter()!=null && query.getStudyIdFilter().contains(b.getInheritedStudy().getStudyId())) ok = true;
						}
					}
				}
				if(!ok) {
					for(Result r: revision.getResults()) {
						if(r.getStudy()!=null) {
							if(query.getSidFilter()>0 && query.getSidFilter()==r.getStudy().getId()) ok = true;
							else if(query.getStudyIdFilter()!=null &&  query.getStudyIdFilter().contains(r.getStudy().getStudyId())) ok = true;
						}
					}
				}

				if(!ok) {
					continue;
				}
			}
			res.add(revision);
		}


		return res;
	}

	private static List<Object[]> queryForRevisions(AuditReader reader, List<Class<?>> entityClasses, int minRev, int maxRev, String userFilter, int sid, String studyIdFilter) {
		List<Object[]> res = new ArrayList<>();
		LoggerFactory.getLogger(DAORevision.class).debug("queryForRevisions "+entityClasses+" "+userFilter+" "+studyIdFilter+" "+minRev+" "+maxRev);
		//Find the study Id from the studyId (the study may have been deleted)
		if(sid<=0 && studyIdFilter!=null && studyIdFilter.length()>0) {
			AuditQuery query = reader.createQuery().forRevisionsOfEntity(Study.class, false, true)
					.add(AuditEntity.revisionType().eq(RevisionType.ADD))
					.add(AuditEntity.property("studyId").eq(studyIdFilter));
			List<Object[]> array = query.getResultList();
			for (Object[] a: array) {
				Study entity = (Study) a[0];
				sid = entity.getId();
				break;
			}
			if(sid<=0) return res;
		}

		for(Class<?> claz: entityClasses ) {
			AuditQuery query = reader.createQuery().forRevisionsOfEntity(claz, false, true)
					.add(AuditEntity.revisionNumber().between(minRev, maxRev));
			if(userFilter!=null && userFilter.length()>0) {
				query = query.add(AuditEntity.property("updUser").eq(userFilter));
			}
			if(studyIdFilter!=null && studyIdFilter.length()>0) {
				//If a studyId filter is given, query the properyId directly
				if(claz==Study.class) {
					query = query.add(AuditEntity.property("id").eq(sid));
				} else if(claz==Biosample.class) {
					query = query.add(AuditEntity.property("inheritedStudy").eq(new Study(sid)));
				} else if(claz==Result.class) {
					query = query.add(AuditEntity.property("study").eq(new Study(sid)));
				} else {
					continue;
				}
			}
			res.addAll(query.getResultList());
		}
		return res;
	}

	/**
	 * Group the modified entities per revisionId/type
	 * @param objects
	 * @return
	 */
	private static List<Revision> getRevisions(List<Object[]> objects){
		Map<Integer, Revision> map = new HashMap<>();
		for (Object[] a: objects) {

			Object entity = a[0];
			if(!(entity instanceof IAuditable)) continue;

			SpiritRevisionEntity rev = (SpiritRevisionEntity) a[1];
			RevisionType type = (RevisionType) a[2];

			Revision r = map.get(rev.getId());
			if(r==null) {
				r = new Revision();
				r.revId = rev.getId();
				r.date = rev.getRevisionDate();
				r.user = rev.getUserId();
				map.put(rev.getId(), r);
				r.type = type;
			} else {
				if(type==RevisionType.DEL) r.type = RevisionType.DEL;
				else if(type==RevisionType.ADD && r.type!=RevisionType.DEL) r.type = RevisionType.ADD;
			}
			r.getAuditables().add((IAuditable)entity);
		}

		List<Revision> res = new ArrayList<>(map.values());
		Collections.sort(res);
		return res;
	}

	/**
	 * Restore the given objects. This function merges the objects, while setting the updDate, updUser and adding the comments
	 * @param objects
	 * @param user
	 * @param comments
	 * @throws Exception
	 */
	public static void restore(Collection<? extends IObject> objects, SpiritUser user, String comments) throws Exception {
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn =  session.getTransaction();

		try {
			if(comments==null || comments.trim().length()==0) throw new Exception("You must give a reason");
			txn.begin();
			Date now = JPAUtil.getCurrentDateFromDatabase();
			Map<String, IObject> mapMerged = new HashMap<>();
			for (IObject entity : objects) {
				remap(session, entity, now, user, comments, mapMerged);
				session.merge(entity);
				mapMerged.put(entity.getClass() + "_" + entity.getId(), null);
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
	public static void revert(Revision revision, SpiritUser user, String comments) throws Exception {
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn =  session.getTransaction();

		try {
			Date now = JPAUtil.getCurrentDateFromDatabase();
			int revId = revision.getRevId();

			AuditReader reader = AuditReaderFactory.get(session);

			//Query modified entities during this revision
			Map<String, IObject> mapMerged = new HashMap<>();
			txn.begin();

			for(Class<IObject> claz : new Class[]{Biotype.class, Test.class, Study.class, Location.class, Biosample.class, Result.class}) {
				List<Class<?>> entityClasses = new ArrayList<>();
				entityClasses.add(claz);
				List<Object[]> res = queryForRevisions(reader, entityClasses, revId, revId, null, -1, null);


				List<IObject> toDelete = new ArrayList<>();
				List<IObject> toMerge = new ArrayList<>();

				for (Object[] a : res) {
					IObject entity = (IObject) a[0];
					RevisionType type = (RevisionType) a[2];
					if(revision.type==type) {
						if(type==RevisionType.ADD) {
							//Attach the entity to be deleted
							IObject obj = session.merge(entity);
							toDelete.add(obj);
						} else {
							//Attach the revision to be restored
							IObject obj = reader.find(entity.getClass(), entity.getId(), revId-1);
							if(obj==null) throw new Exception("The "+entity.getClass().getSimpleName()+" "+entity.getId()+" could not be found at rev="+(revId-1));
							toMerge.add(obj);
						}
						mapMerged.put(entity.getClass() + "_" + entity.getId(), null);
					}
				}

				LoggerFactory.getLogger(DAORevision.class).debug(claz+" >  toMerge="+toMerge.size()+" toDelete="+toDelete.size());
				int step = 0;
				while(toMerge.size()>0 && step++<10) {
					for (IObject o : new ArrayList<>(toMerge)) {
						int id = o.getId();
						boolean success = remap(session, o, now, user, comments, mapMerged);
						if(!success) continue;
						toMerge.remove(o);

						LoggerFactory.getLogger(DAORevision.class).debug("merge "+o.getClass().getSimpleName()+" "+o.getId()+":"+o);
						mapMerged.put(o.getClass() + "_" + id, session.merge(o));
					}
				}
				for (IObject o : toDelete) {
					LoggerFactory.getLogger(DAORevision.class).debug("remove "+o.getClass().getSimpleName()+" "+o.getId()+":"+o);
					session.remove(o);
				}
			}
			txn.commit();
			txn = null;
		} catch (Exception e) {
			e.printStackTrace();
			if(txn!=null && txn.isActive()) txn.rollback();
			throw e;
		}
	}


	/**
	 * Compute the LastChange for each revision.
	 * Because this function can take some time,
	 * @param rev
	 * @return
	 */
	public static Map<Revision, String> getLastChanges(Collection<Revision> revisions) {
		Map<Revision, String> changeMap = new HashMap<>();
		for (Revision r : revisions) {
			if(revisions.size()>100 && r.getAuditables().size()>1) {
				changeMap.put(r, "-");
			} else {
				changeMap.put(r, DAORevision.getLastChange(r));
			}
		}
		return changeMap;
	}


	/**
	 * Computes the differences between the objects in this revision and the last update.
	 * A maximum of 4 differences is returned (separated by newlines)
	 * @param rev
	 * @return
	 */
	public static String getLastChange(Revision rev) {
		if(rev.type==RevisionType.DEL) return "Deleted";
		if(rev.type==RevisionType.ADD) return "First version";
		final int MAX = 2;

		LinkedHashSet<String> diffs = new LinkedHashSet<>();
		for(IAuditable s: rev.getAuditables()) {
			if(diffs.size()>=MAX) break;
			IAuditable previous = getLastVersion(s, rev.revId-1);
			String diff = s.getDifference(previous);
			if(diff!=null && diff.length()>0) diffs.add(diff);
		}

		//Keep a maximum of 4 changes
		if(diffs.size()>=MAX) {
			diffs.add("...");
		}
		return MiscUtils.flatten(diffs, "\n");
	}

	/**
	 * Updates the date, user, comments of the given object
	 * @param clone
	 * @param now
	 * @param user
	 * @param comments
	 * @throws Exception
	 */
	private static boolean remap(EntityManager session, IObject clone, Date now, SpiritUser user, String comments, Map<String, IObject> mapMerged) throws Exception {

		LoggerFactory.getLogger(DAORevision.class).debug("Restore "+clone.getClass().getSimpleName()+": "+clone);
		boolean success = true;
		if(clone instanceof Study) {
			Study s = ((Study) clone);
			s.setNotes((s.getNotes()==null || s.getNotes().length()==0? "": s + " - ") +  comments);
			s.setUpdDate(now);
			s.setUpdUser(user.getUsername());

		} else if(clone instanceof Biosample) {
			Biosample b = (Biosample) clone;
			if(mapMerged!=null && b.getInheritedStudy()!=null && mapMerged.containsKey(Study.class+"_"+b.getInheritedStudy().getId())) {
				Study s = (Study)mapMerged.get(Study.class+"_"+b.getInheritedStudy().getId());
				if(s!=null) {
					b.setInheritedStudy(s);
					b.setAttachedStudy(b.getAttachedStudy()==null? null: s);
					b.setInheritedGroup(b.getInheritedGroup()==null? null: s.getGroup(b.getInheritedGroup().getName()));
					b.setInheritedPhase(b.getInheritedPhase()==null? null: s.getPhase(b.getInheritedPhase().getName()));
					if(b.getAttachedSampling()!=null && b.getAttachedSampling().getNamedSampling()!=null) {
						Sampling sampling = s.getSampling(b.getAttachedSampling().getNamedSampling().getName(), b.getAttachedSampling().getDetailsLong());
						b.setAttachedSampling(sampling);
					}
				} else {
					success = false;
				}
			}
			if(mapMerged!=null && b.getParent()!=null && mapMerged.containsKey(Biosample.class+"_"+b.getParent().getId())) {
				Biosample parentMerged = (Biosample) mapMerged.get(Biosample.class+"_"+b.getParent().getId());
				if(parentMerged!=null) {
					b.setParent(parentMerged);
				} else {
					success = false;
				}
			}

			b.setChildren(new HashSet<Biosample>());
			b.setUpdDate(now);
			b.setUpdUser(user.getUsername());

			//Update linked documents
			for(BiotypeMetadata bm: b.getBiotype().getMetadata()) {
				if(bm.getDataType()==DataType.D_FILE || bm.getDataType()==DataType.FILES) {
					Document doc = b.getMetadataDocument(bm);
					b.setMetadataDocument(bm, doc==null? null: new Document(doc.getFileName(), doc.getBytes()));
				}
			}

		} else if(clone instanceof Test) {
			((Test) clone).setUpdDate(now);
			((Test) clone).setUpdUser(user.getUsername());
		} else if(clone instanceof Result) {
			Result r = (Result) clone;
			if( r.getPhase()!=null && r.getPhase().getStudy()!=null) {
				if(mapMerged!=null && mapMerged.containsKey(Study.class+"_"+r.getPhase().getStudy().getId())) {
					Study s = (Study)mapMerged.get(Study.class+"_"+r.getPhase().getStudy().getId());
					Phase p = s==null? null: s.getPhase(r.getPhase().getName());
					if(s!=null) {
						r.setPhase(p);
					} else {
						success = false;
					}
				} else if(!session.contains(r.getPhase())) {
					Study s = DAOStudy.getStudyByStudyId(r.getPhase().getStudy().getStudyId());
					Phase p = s.getPhase(r.getPhase().getName());
					r.setPhase(p);
				}
			}
			if(r.getBiosample()!=null) {
				if(mapMerged!=null && mapMerged.containsKey(Biosample.class+"_"+r.getBiosample().getId())) {
					//The linked object is also reverted
					Biosample b = (Biosample)mapMerged.get(Biosample.class+"_"+r.getBiosample().getId());
					if(b!=null) {
						r.setBiosample(b);
					} else {
						success = false;
					}
				} else if(!session.contains(r.getBiosample())) {
					//The linked object was not reverted. However its id may differ, if it was reverted before
					Biosample b = DAOBiosample.getBiosample(r.getBiosample().getSampleId());
					r.setBiosample(b);
				}
			}
			r.setComments((r.getComments()==null?"": r.getComments() + " - ") + comments);
			r.setUpdDate(now);
			r.setUpdUser(user.getUsername());
		} else if(clone instanceof Location) {
			((Location) clone).setUpdDate(now);
			((Location) clone).setUpdUser(user.getUsername());
		} else if(clone instanceof Test) {
			((Test) clone).setUpdDate(now);
			((Test) clone).setUpdUser(user.getUsername());
		} else if(clone instanceof Biotype) {
			((Biotype) clone).setUpdDate(now);
			((Biotype) clone).setUpdUser(user.getUsername());
		} else {
			throw new Exception("Invalid object "+clone);
		}
		return success;
	}

}
