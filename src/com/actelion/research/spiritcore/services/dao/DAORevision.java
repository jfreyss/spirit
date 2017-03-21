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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.IObject;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.util.FormatterUtils;

public class DAORevision {


	public static class Revision implements Comparable<Revision> {
		private Date date;
		private int revId;
		private String user;
		private List<IObject> entities = new ArrayList<>();
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

		Map<String, Revision> map = mapRevisions(res);
		List<Revision> revisions = new ArrayList<>(map.values());
		Collections.sort(revisions);
		LoggerFactory.getLogger(DAORevision.class).debug("Loaded revisions for " + obj+" in "+(System.currentTimeMillis()-s)+"ms");
		return revisions;
	}

	/**
	 * Get the different version of a samples, the first index shows the most recent version
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static<T extends IObject> List<T> getHistory(T obj) {
		long s = System.currentTimeMillis();
		EntityManager session = JPAUtil.getManager();
		AuditReader reader = AuditReaderFactory.get(session);
		AuditQuery query = reader.createQuery()
				.forRevisionsOfEntity(obj.getClass(), true, false)
				.add(AuditEntity.id().eq(obj.getId()))
				//				.addOrder(new PropertyAuditOrder(new RevisionNumberPropertyName(), false));
				.addOrder(AuditEntity.revisionNumber().desc());
		List<T> res = query.getResultList();

		LoggerFactory.getLogger(DAORevision.class).debug("Loaded history for " + obj+" in "+(System.currentTimeMillis()-s)+"ms");
		return res;
	}


	public static Revision getRevision(int revId) {
		long s = System.currentTimeMillis();
		EntityManager session = JPAUtil.getManager();
		AuditReader reader = AuditReaderFactory.get(session);

		List<Class<?>> entityClasses = new ArrayList<>();
		entityClasses.add(Study.class);
		entityClasses.add(Biosample.class);
		entityClasses.add(Result.class);
		entityClasses.add(Location.class);
		entityClasses.add(Biotype.class);
		entityClasses.add(Test.class);

		List<Object[]> objects = queryForRevisions(reader, entityClasses, revId, revId, null);

		Map<String, Revision> map = mapRevisions(objects);
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
		List<Class<?>> entityClasses = new ArrayList<>();
		if(studies) entityClasses.add(Study.class);
		if(samples) entityClasses.add(Biosample.class);
		if(results) entityClasses.add(Result.class);
		if(locations) entityClasses.add(Location.class);
		if(admin) entityClasses.add(Biotype.class);
		if(admin) entityClasses.add(Test.class);

		List<Object[]> objects = queryForRevisions(reader, entityClasses, rev1, rev2, userFilter);
		Map<String, Revision> map = mapRevisions(objects);

		List<Revision> revisions = new ArrayList<>(map.values());
		Collections.sort(revisions);
		LoggerFactory.getLogger(DAORevision.class).debug("Loaded revisions in "+(System.currentTimeMillis()-s)+"ms");
		return revisions;
	}

	private static List<Object[]> queryForRevisions(AuditReader reader, List<Class<?>> entityClasses, int minRev, int maxRev, String userFilter) {
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


	/**
	 * Map revisions to their identifier (revId.revType)
	 * @param objects
	 * @return
	 */
	private static Map<String, Revision> mapRevisions(List<Object[]> objects){
		Map<String, Revision> res = new TreeMap<>();
		Set<Integer> addAndDelRevIds = new HashSet<>();
		for (Object[] a: objects) {

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
				List<Object[]> res = queryForRevisions(reader, entityClasses, revId, revId, null);


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
				while(toMerge.size()>0 && step++<6) {
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
			//
			//			for(Biosample b: s.getAttachedBiosamples()) {
			//				if(mapMerged!=null && b.getInheritedStudy()!=null && mapMerged.containsKey(Biosample.class+"_"+b.getId())) {
			//
			//				}
			//			}

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
			((Test) clone).setUpdUser(comments);
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
