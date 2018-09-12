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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.LockMode;
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
import com.actelion.research.spiritcore.business.audit.DifferenceItem.ChangeType;
import com.actelion.research.spiritcore.business.audit.DifferenceList;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.audit.RevisionQuery;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.property.SpiritProperty;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.Pair;

/**
 * DAO functions linked to audit functions
 *
 * @author Joel Freyss
 */
public class DAORevision {

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
	public static List<Revision> getLastRevisions(IAuditable obj, int maxRevId, int n) {
		Serializable id;
		if(obj instanceof IObject) {
			id = ((IObject)obj).getId();
		} else if(obj instanceof SpiritProperty) {
			id = ((SpiritProperty)obj).getId();
		} else {
			assert false;
			return null;
		}
		return getLastRevisions(obj.getClass(), id, maxRevId, n);

	}

	/**
	 * Returns all revisions of the given entity until the given maxRevId
	 * @param obj
	 * @param maxRevId (-1, to ignore)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Revision> getLastRevisions(Class<?> claz, Serializable entityId, int maxRevId, int n) {

		long s = System.currentTimeMillis();
		EntityManager session = JPAUtil.getManager();
		AuditReader reader = AuditReaderFactory.get(session);
		AuditQuery query = reader.createQuery()
				.forRevisionsOfEntity(claz, false, true)
				.setCacheable(false)
				.setLockMode(LockMode.NONE);
		query.add(AuditEntity.id().eq(entityId));

		if(maxRevId>0) {
			query.add(AuditEntity.revisionNumber().le(maxRevId));
		}
		if(n>0) {
			query.setMaxResults(n);
			query.addOrder(AuditEntity.revisionNumber().desc());
		}
		List<Revision> revisions = getRevisions(null, query.getResultList());
		LoggerFactory.getLogger(DAORevision.class).debug("Loaded revisions for " + claz.getSimpleName() + "("+entityId + ") maxRevId="+maxRevId + "-" + n + " in " + (System.currentTimeMillis()-s)+"ms");
		return revisions;
	}

	/**
	 * Get the different versions of several elements.
	 * Returns a map of element to a list of audited versions, the first element of the list being the most recent version
	 * @param obj
	 * @return
	 */
	public static<T extends IObject> Map<T, List<T>> getHistories(Collection<T> col) {
		Map<T, List<T>> res = new HashMap<>();
		for (T t : col) {
			res.put(t, getHistory(t));
		}
		return res;
	}

	/**
	 * Get the different version of an element, the first element of the list shows the most recent version
	 * @param obj
	 * @return
	 */
	public static<T extends IObject> List<T> getHistory(T obj) {
		return getHistory(obj.getClass(), obj.getId(), -1);
	}

	/**
	 * Get the different version of an element, the first element of the list shows the most recent version
	 * @param obj
	 * @return
	 */
	public static<T extends IObject> List<T> getHistory(Class claz, Serializable objectId, int maxRevs) {
		long s = System.currentTimeMillis();
		EntityManager session = JPAUtil.getManager();
		AuditReader reader = AuditReaderFactory.get(session);
		AuditQuery query = reader.createQuery()
				.forRevisionsOfEntity(claz, true, false)
				.add(AuditEntity.id().eq(objectId))
				.addOrder(AuditEntity.revisionNumber().desc())
				.setCacheable(false)
				.setLockMode(LockMode.NONE);
		if(maxRevs>0) {
			query.setMaxResults(maxRevs);
		}
		List<T> res = query.getResultList();
		for (T t : res) {
			session.detach(t);
		}

		LoggerFactory.getLogger(DAORevision.class).debug("Loaded history for " + claz.getSimpleName() + ": (" + objectId + ") in " + (System.currentTimeMillis()-s) + "ms");
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
			rev1 = query.getRevId()>0? query.getRevId(): query.getFromDate()==null? 0: reader.getRevisionNumberForDate(query.getFromDate()).intValue()+1;
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
		Set<Class<?>> entityClasses = new HashSet<>();
		if(query.isStudies()) entityClasses.add(Study.class);
		if(query.isSamples()) entityClasses.add(Biosample.class);
		if(query.isResults()) entityClasses.add(Result.class);
		if(query.isLocations()) entityClasses.add(Location.class);
		if(query.isAdmin()) entityClasses.add(Biotype.class);
		if(query.isAdmin()) entityClasses.add(Test.class);
		if(query.isAdmin()) entityClasses.add(Employee.class);
		if(query.isAdmin()) entityClasses.add(EmployeeGroup.class);
		if(query.isAdmin()) entityClasses.add(SpiritProperty.class);

		List<Revision> revisions = getRevisions(entityClasses, queryForRevisions(reader, entityClasses, rev1, rev2, query.getUserIdFilter(), query.getSidFilter(), query.getStudyIdFilter()));
		LoggerFactory.getLogger(DAORevision.class).debug("Loaded revisions in "+(System.currentTimeMillis()-s)+"ms");

		//Post filter per study
		List<Revision> res = new ArrayList<>();
		for (Revision revision : revisions) {
			System.out.println("DAORevision.queryRevisions() "+revision+" "+revision.getType()+" "+revision.getDifference());
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

	private static List<Object[]> queryForRevisions(AuditReader reader, Set<Class<?>> entityClasses, int minRev, int maxRev, String userFilter, int sid, String studyIdFilter) {
		List<Object[]> res = new ArrayList<>();
		LoggerFactory.getLogger(DAORevision.class).debug("queryForRevisions "+entityClasses+" "+userFilter+" "+sid+" "+studyIdFilter+" "+minRev+" "+maxRev);
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
			if (userFilter!=null
					&& userFilter.length()>0
					&& (claz==Result.class 
							|| claz==Biosample.class
							|| claz==Study.class
							|| claz==Location.class)) {
				query = query.add(AuditEntity.property("updUser").eq(userFilter));
			}
			if(sid>0) {
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
	private static List<Revision> getRevisions(Set<Class<?>> entityClasses, List<Object[]> objects){
		Map<Integer, Revision> map = new HashMap<>();
		for (Object[] a: objects) {

			Object entity = a[0];
			if(entityClasses==null && !(entity instanceof IAuditable)) continue;
			else if(entityClasses!=null && !entityClasses.contains(entity.getClass())) continue;


			SpiritRevisionEntity rev = (SpiritRevisionEntity) a[1];
			RevisionType type = (RevisionType) a[2];

			Revision r = map.get(rev.getId());
			if(r==null) {
				int sid = rev.getSid();
				Study study = DAOStudy.getStudy(sid);
				r = new Revision(rev.getId(), type, study, rev.getReason(), rev.getDifferenceList(), rev.getUserId(), rev.getRevisionDate());
				map.put(rev.getId(), r);
			} else {
				if(type==RevisionType.DEL) r.setType(RevisionType.DEL);
				else if(type==RevisionType.ADD && r.getType()!=RevisionType.DEL) r.setType(RevisionType.ADD);
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
	public static void restore(Collection<? extends IObject> objects, SpiritUser user) throws Exception {
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn =  session.getTransaction();

		try {
			txn.begin();
			Date now = JPAUtil.getCurrentDateFromDatabase();
			Map<String, IObject> mapMerged = new HashMap<>();
			for (IObject entity : objects) {
				remap(session, entity, now, user, mapMerged);
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
	 * Cancel the change, ie. go to the version minus one for each object in the revision.
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
	@SuppressWarnings("unchecked")
	public static void revert(Revision revision, SpiritUser user) throws Exception {
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
				List<Object[]> res = queryForRevisions(reader, Collections.singleton(claz), revId, revId, null, -1, null);


				List<IObject> toDelete = new ArrayList<>();
				List<IObject> toMerge = new ArrayList<>();

				for (Object[] a : res) {
					IObject entity = (IObject) a[0];
					RevisionType type = (RevisionType) a[2];
					if(revision.getType()==type) {
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
						boolean success = remap(session, o, now, user, mapMerged);
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
	 * Gets the last version and the change for the given entity, in the given context.
	 * This method should return the last change, even if the changes of the given entity have not been committed.
	 *
	 * This method should be used exclusively from the SpritRevisionEntityListener to record the differences between the object to be saved and the last revision.
	 * (when the record to be saved in already in the audit table)
	 *
	 * @param entityClass
	 * @param entityId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static<T extends IAuditable> Pair<IAuditable, DifferenceList> getLastChange(RevisionType revisionType, Class<T> entityClass, Serializable entityId) {
		//Query the 2 last revisions of entityClass:entityId
		EntityManager session = JPAUtil.getManager();
		AuditReader reader = AuditReaderFactory.get(session);
		AuditQuery query = reader.createQuery()
				.forRevisionsOfEntity(entityClass, false, true)
				.add(AuditEntity.id().eq(entityId))
				.addOrder(AuditEntity.revisionNumber().desc())
				.setMaxResults(2)
				.setCacheable(false)
				.setLockMode(LockMode.NONE);
		List<Object[]> histories = query.getResultList();

		//Compute the difference between those 2 last versions
		assert histories.size()>0;

		DifferenceList diff = null;
		if(revisionType==RevisionType.DEL) {
			diff = ((T)histories.get(0)[0]).getDifferenceList(null);
			diff.add("", ChangeType.DEL);
			assert diff.size()==1;
		} else if(revisionType==RevisionType.ADD) {
			diff = ((T)histories.get(0)[0]).getDifferenceList(null);
			diff.add("", ChangeType.ADD);
			assert diff.size()==1;
		} else if(histories.size()>=2) {
			diff = ((T)histories.get(0)[0]).getDifferenceList((T)histories.get(1)[0]);
		} else {
			return null;
		}
		return new Pair<IAuditable, DifferenceList>((T)histories.get(0)[0], diff);
	}

	/**
	 * Returns the lastChange from the given entity.
	 * This method should be called outside the SpritRevisionEntityListener (when the record is not yet in the audit table)
	 *
	 * @param auditable
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static<T extends IAuditable> Pair<T, DifferenceList> getLastChange(IAuditable auditable) {
		//Query the 2 last revisions of entityClass:entityId
		EntityManager session = JPAUtil.getManager();
		AuditReader reader = AuditReaderFactory.get(session);
		AuditQuery query = reader.createQuery()
				.forRevisionsOfEntity(auditable.getClass(), true, true)
				.add(AuditEntity.id().eq(auditable.getSerializableId()))
				.addOrder(AuditEntity.revisionNumber().desc())
				.setMaxResults(1)
				.setCacheable(false)
				.setLockMode(LockMode.NONE);
		List<IAuditable> histories = query.getResultList();

		//Compute the difference between those 2 last versions
		if(histories.size()==0) {
			return new Pair<T, DifferenceList>(null, new DifferenceList());
		} else {
			return new Pair<T, DifferenceList>((T)histories.get(0), auditable.getDifferenceList(histories.get(0)));
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
	private static boolean remap(EntityManager session, IObject clone, Date now, SpiritUser user, Map<String, IObject> mapMerged) throws Exception {

		LoggerFactory.getLogger(DAORevision.class).debug("Restore "+clone.getClass().getSimpleName()+": "+clone);
		boolean success = true;
		if(clone instanceof Study) {
			Study s = ((Study) clone);
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
