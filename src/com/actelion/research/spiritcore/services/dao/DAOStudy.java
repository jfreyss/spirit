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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.hibernate.jpa.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.Document.DocumentType;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Randomization;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.Formatter;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.QueryTokenizer;
import com.actelion.research.spiritcore.util.Triple;
import com.actelion.research.util.PriorityQueue;
import com.actelion.research.util.PriorityQueue.Elt;

@SuppressWarnings("unchecked")
public class DAOStudy {
	
	private static Logger logger = LoggerFactory.getLogger(DAOStudy.class);
	

	public static List<Study> getStudies() {
		EntityManager session = JPAUtil.getManager();
		List<Study> res = (List<Study>) Cache.getInstance().get("allstudies");
		if(res==null) {
			res = session.createQuery("from Study").getResultList();
			Collections.sort(res);
			Cache.getInstance().add("allstudies", res, Cache.LONG);
		}
		return res;
	}
	
	public static List<Study> getRecentStudies(SpiritUser user, RightLevel level) {
		String key = "studies_"+user+"_"+level+"_"+JPAUtil.getManager();
		List<Study> studies = (List<Study>) Cache.getInstance().get(key);
		
		//Make sure studies are in the same session, or reset the cache
		if(studies!=null && studies.size()>0 && !JPAUtil.getManager().contains(studies.get(0))) {
			studies = null;
		}
		
		//Load the studies
		if(studies==null) {
			EntityManager session = JPAUtil.getManager();
			Query query = session.createQuery("from Study s where s.creDate > ?1");
			Calendar cal = Calendar.getInstance();
			cal.setTime(JPAUtil.getCurrentDateFromDatabase());
			cal.add(Calendar.DAY_OF_YEAR, -365);
			query.setParameter(1, cal.getTime());
			
			List<Study> res = query.getResultList();
			
				
			studies = new ArrayList<>();
			if(user!=null) {
				for (Study study : res) {
					if(level==RightLevel.ADMIN) {
						if(SpiritRights.canAdmin(study, user)) studies.add(study);
					} else if(level==RightLevel.BLIND) {
						if(SpiritRights.canBlind(study, user)) studies.add(study);
					} else if(level==RightLevel.WRITE) {
						if(SpiritRights.canExpert(study, user)) studies.add(study);					
					} else if(level==RightLevel.READ) {
						if(SpiritRights.canRead(study, user)) studies.add(study);					
					} else if(level==RightLevel.VIEW) {
						if(ConfigProperties.getInstance().isOpen() || SpiritRights.canRead(study, user)) studies.add(study);					
					}
				}
			} else {
				studies.addAll(studies);
			}
			Collections.sort(studies);
			
			Cache.getInstance().add(key, studies, 120);
		}
		return studies;
		
	}	
	
	protected static void postLoad(Collection<Study> studies) {
		if(studies==null) return;
		//2nd loading pass: load the tests from the serialized measurements
		Set<Integer> testIds = new HashSet<>();
		for (Study study : studies) {
			testIds.addAll(Measurement.getTestIds(study.getAllMeasurementsFromActions()));			
			testIds.addAll(Measurement.getTestIds(study.getAllMeasurementsFromSamplings()));			
		}
		
		Map<Integer, Test> id2test =  JPAUtil.mapIds(DAOTest.getTests(testIds));
		for (Study study : studies) {
			for(StudyAction a: study.getStudyActions()) {
				for(Measurement m: a.getMeasurements()) {
					m.setTest(id2test.get(m.getTestId()));
				}
			}
			for(NamedSampling ns: study.getNamedSamplings()) {
				for(Sampling s: ns.getAllSamplings()) {
					for(Measurement m: s.getMeasurements()) {
						m.setTest(id2test.get(m.getTestId()));
					}
				}				
			}
		}
	}
	
	public static List<String> getAllMetadata(String key) {
		List<String> res = (List<String>) Cache.getInstance().get("study_"+key);
		if(res==null) {
			Set<String> set = new TreeSet<>();
			for (Study s : getStudies()) {
				if(s.getMetadata().get(key)!=null) {
					set.add(s.getMetadata().get(key));
				}
			}
			res = new ArrayList<>(set);
			Cache.getInstance().add("study_"+key, res, Cache.LONG);
		} 
		return res;
	}
	
	public static void fullLoad(Study study) {
		if(study==null) return;

		study.getPhases().iterator();
		study.getGroups().iterator();
		study.getNamedTreatments().iterator();

		for(NamedSampling ns: study.getNamedSamplings()) {
			ns.getAllSamplings().iterator();
		}
		
		for(StudyAction a: study.getStudyActions()) {
			a.getGroup();
			a.getPhase();
			a.getNamedTreatment();
			a.getNamedSamplings();
		}

	}
	
	public static Study getStudy(int id) {
		EntityManager session = JPAUtil.getManager();
		List<Study> res = (List<Study>) session.createQuery("select s from Study s where s.id = ?1")
				.setParameter(1, id)
				.getResultList();		
		Study s = res.size()==1? res.get(0): null;
		if(s!=null) postLoad(Collections.singleton(s));
		return s;
	}
	
	public static Study getStudyByStudyId(String studyId) {
		EntityManager session = JPAUtil.getManager();
		List<Study> res = (List<Study>) session.createQuery("select s from Study s where s.studyId = ?1")
				.setParameter(1, studyId)
				.getResultList();
		Study s = res.size()==1? res.get(0): null;
		if(s!=null) postLoad(Collections.singleton(s));
		return s;
	}
	
	public static List<Study> getStudyByIvvOrStudyId(String ivvs) {
		EntityManager session = JPAUtil.getManager();
		String sql="";
		try {
			sql = "select s from Study s where " + QueryTokenizer.expandOrQuery("s.ivv = ? or s.studyId = ?", ivvs);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		List<Study> res = (List<Study>) session.createQuery(sql)
				.getResultList();
		postLoad(res);
		return res;
	}
		
	public static List<ContainerType> getContainerTypes(Study study) {
		List<ContainerType> res = (List<ContainerType>) Cache.getInstance().get("study_containers_"+study);
		if(res==null) {
			EntityManager session = JPAUtil.getManager();
			res = (List<ContainerType>) session.createQuery("select distinct(b.container.containerType) from Biosample b where b.inheritedStudy = ?1 and b.container.containerType is not null")
					.setParameter(1, study)
//					.setHint(QueryHints.READ_ONLY, !JPAUtil.isEditableContext())
					.getResultList();		
			Collections.sort(res);
			Cache.getInstance().add("study_containers_"+study, res);
		}
		return res;
	}
	
	public static List<Biotype> getBiotypes(String studyId){
		List<Biotype> res = (List<Biotype>) Cache.getInstance().get("study_biotypes_"+studyId);
		if(res==null) {
			EntityManager session = JPAUtil.getManager();
			res = (List<Biotype>) session.createQuery("select distinct(b.biotype) from Biosample b where b.inheritedStudy.studyId = ?1 and b.biotype is not null")
					.setParameter(1, studyId)
					.getResultList();
			Collections.sort(res);
			Cache.getInstance().add("study_biotypes_"+studyId, res);
		}
		return res;
	}
	
	
	public static List<Study> queryStudies(StudyQuery q, SpiritUser user) throws Exception {
		EntityManager session = JPAUtil.getManager();
		long s = System.currentTimeMillis();

		String jpql = "SELECT s FROM Study s where 1=1 ";
		StringBuilder clause = new StringBuilder();
		List<Object> parameters = new ArrayList<Object>();
		//new Biosample().getg
		if(q!=null) {
			
			if(q.getStudyIds()!=null && q.getStudyIds().length()>0) {				
				clause.append(" and (" + QueryTokenizer.expandOrQuery("s.studyId = ?", q.getStudyIds()) + ")");
			}
			
			if(q.getKeywords()!=null && q.getKeywords().length()>0) {
				String expr = "lower(s.studyId) like lower(?)" +
								" or lower(s.ivv) like lower(?)" +
								" or lower(s.serializedMetadata) like lower(?)" +
								" or lower(s.title) like lower(?)" +
								" or lower(s.adminUsers) like lower(?)" + 
								" or lower(s.expertUsers) like lower(?)" + 
								" or lower(s.blindUsers) like lower(?)" + 
								" or lower(s.state) like lower(?)" + 
								" or s.id in (select nt.study.id from NamedTreatment nt where lower(nt.name) like lower(?) or lower(nt.compoundName) like lower(?) or lower(nt.compoundName2) like lower(?))";
				clause.append(" and (" + QueryTokenizer.expandQuery(expr, q.getKeywords(), true, true) + ")");
			}			
			
			if(q.getState()!=null && q.getState().length()>0) {
				clause.append(" and (s.state = ?)");
				parameters.add(q.getState());
			}

			if(q.getUser()!=null && q.getUser().length()>0) {
				clause.append(" and (lower(s.adminUsers) like lower(?) or lower(s.expertUsers) like lower(?) or lower(s.creUser) like lower(?))");
				parameters.add("%"+q.getUser()+"%");
				parameters.add("%"+q.getUser()+"%");
				parameters.add("%"+q.getUser()+"%");
			}
			
			
			if(q.getUpdDays()!=null && q.getUpdDays().length()>0) {
				String digits = MiscUtils.extractStartDigits(q.getUpdDays());
				if(digits.length()>0) { 
					try {
						clause.append(" and s.updDate > ?");
						Calendar cal = Calendar.getInstance();
						cal.setTime(new Date());
						cal.add(Calendar.DAY_OF_YEAR, -Integer.parseInt(digits));
						parameters.add(cal.getTime());
					} catch (Exception e) {
					}
				}		
			}
			if(q.getCreDays()!=null && q.getCreDays().length()>0) {
				String digits = MiscUtils.extractStartDigits(q.getCreDays());
				if(digits.length()>0) { 
					try {
						clause.append(" and s.creDate > ?");	
						Calendar cal = Calendar.getInstance();
						cal.setTime(new Date());
						cal.add(Calendar.DAY_OF_YEAR, -Integer.parseInt(digits));
						parameters.add(cal.getTime());
					} catch (Exception e) {
					}
				}		
			}
			if(q.getRecentStartDays()>0) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date());
				cal.add(Calendar.DAY_OF_YEAR, -q.getRecentStartDays());
				clause.append(" and s.startingDate > ?");	
				parameters.add(cal.getTime());
			}
		
			if(clause.length()>0) jpql += clause;
		}
		jpql = JPAUtil.makeQueryJPLCompatible(jpql);
		Query query = session.createQuery(jpql);
		for (int i = 0; i < parameters.size(); i++) {
			query.setParameter(1+i, parameters.get(i));				
		}
		query.setHint(QueryHints.HINT_READONLY, !JPAUtil.isEditableContext());
		List<Study> studies = query.getResultList();
		
		Collections.sort(studies, new Comparator<Study>() {
			@Override
			public int compare(Study o1, Study o2) {
				return -o1.getCreDate().compareTo(o2.getCreDate());
			}
		});
		
		//All users can view all studies except test studies
		if(user!=null) {
			for (Iterator<Study> iterator = studies.iterator(); iterator.hasNext();) {
				Study study = iterator.next();
				if(!SpiritRights.canRead(study, user)) iterator.remove();			
			}
		}
		LoggerFactory.getLogger(DAOStudy.class).info("queryStudies() in "+(System.currentTimeMillis()-s)+"ms");
//		postLoad(studies);
		return studies;
	}
	
	/**
	 * Persists the study
	 * @param study
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public static void persistStudies(Collection<Study> studies, SpiritUser user) throws Exception {
		EntityManager session = JPAUtil.getManager();		
		//Start the transaction
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();

			persistStudies(session, studies, user);
			
			txn.commit();
			txn = null;			
		} finally {
			if(txn!=null && txn.isActive()) try{ txn.rollback();} catch(Exception e2) {}
		}
		
	}
	/**
	 * Persists the study
	 * @param study
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public static void persistStudies(EntityManager session, Collection<Study> studies, SpiritUser user) throws Exception {
		
		assert studies!=null;
		logger.info("Persist "+studies.size()+ "(studies)");
		assert user!=null;
		assert session!=null;
		assert session.getTransaction().isActive();

		
		//Test that nobody else modified the study
		for (Study study : studies) {
			
			if(study.getId()>0) {
				EntityManager ses = null;
				try {
					ses = JPAUtil.createManager();
					Object[] lastUpdate = (Object[]) ses.createQuery("select s.updDate, s.updUser from Study s where s.id = "+study.getId()).getSingleResult();
					Date lastDate = (Date) lastUpdate[0];
					String lastUser = (String) lastUpdate[1];
					
					if(lastDate!=null && lastUser!=null && !lastUser.equals(user.getUsername())) {
						int diffSeconds = (int) ((lastDate.getTime() - study.getUpdDate().getTime())/1000);
						if(diffSeconds>0) throw new Exception("The study "+study+" has just been updated by "+lastUser+" [" + diffSeconds + "seconds ago].\nYou cannot overwrite those changes unless you reopen the newest version.");
					}
				} finally {
					if(ses!=null) ses.close();
				}
			}		
			
				
			if(!SpiritRights.canAdmin(study, user) && !SpiritRights.canBlind(study, user)) throw new Exception("You are not allowed to edit this study");
			if(study.getId()<0 && getStudyByIvvOrStudyId(study.getIvv()).size()>0) throw new Exception("The internalId must be unique");
			Date now = JPAUtil.getCurrentDateFromDatabase();
			
			
			//Fix documents without docType (Spirit v<=1.9)
			for (Document doc : study.getDocuments()) {
				if(doc.getType()==null) {
					doc.setType(DocumentType.DESIGN);
				}
			}
	
			study.setUpdUser(user.getUsername());
			study.setUpdDate(now);
						
			for (StudyAction a : new ArrayList<>(study.getStudyActions())) {
				if(a.isEmpty() || a.getSubGroup()<0 || a.getSubGroup()>=a.getGroup().getNSubgroups()) {
					a.remove();
				}
			}
			
			for (NamedSampling ns : study.getNamedSamplings()) {
				for (Sampling n : ns.getAllSamplings()) {
					System.out.println("DAOStudy.persistStudy() "+study+" "+ns+" "+n.getDetailsLong());
					
				}
			}
			//Now save the study
			study.preSave();
			if(study.getId()>0) {				
				if(!session.contains(study)) {
					study = session.merge(study);
					logger.info("Merge "+study);
				}
			} else {
				study.setStudyId(getNextStudyId(session));
				study.setCreUser(study.getUpdUser());
				study.setCreDate(study.getUpdDate());
				session.persist(study);
				logger.info("Persist "+study);
			}
	
			for(Phase phase: study.getPhases()) {
				phase.serializeRandomization();
			}
		}

		Cache.getInstance().remove("studies_"+user);
		Cache.getInstance().remove("allstudies");

//		return study;
		
	}

	public static void deleteStudy(Study study, SpiritUser user) throws Exception {
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();

			deleteStudy(session, study, user);
			
			txn.commit();
			txn = null;
		} finally {
			if(txn!=null && txn.isActive()) try{ txn.rollback();} catch(Exception e2) {}			
		}
	}
		
	public static void deleteStudies(EntityManager session, Collection<Study> studies, SpiritUser user) throws Exception {
		for (Study study : studies) {
			deleteStudy(session, study, user);
		}
	}
	public static void deleteStudy(EntityManager session, Study study, SpiritUser user) throws Exception {
		if(!SpiritRights.canDelete(study, user)) throw new Exception("You are not allowed to delete the study");
		
		assert session!=null;
		assert session.getTransaction().isActive();

		study = session.merge(study);

		//Make sure that there are no attached results
		ResultQuery q = ResultQuery.createQueryForStudyIds(study.getStudyId());
		List<Result> l = DAOResult.queryResults(session, q, null);
		if(l.size()>0) throw new Exception("You cannot delete a study if there are " + l.size() + " results linked to it");
				
		//Make sure that there are no attached animals
		BiosampleQuery q2 = BiosampleQuery.createQueryForStudyIds(study.getStudyId());
		List<Biosample> l2 = DAOBiosample.queryBiosamples(session, q2, null);
		if(l2.size()>0) throw new Exception("You cannot delete this study because there are " + l2.size() +" biosamples linked to it");
		
		//Remove
		session.remove(study);
		
		Cache.getInstance().remove("studies_"+user);
		Cache.getInstance().remove("allstudies");
	}
	
	public static String getNextStudyId() {
		return getNextStudyId(JPAUtil.getManager());
	}
	
	public static String getNextStudyId(EntityManager session) {
		String s = (String) session.createQuery("select max(s.studyId) from Study s where s.studyId like ('S-%')")
				.setHint(QueryHints.HINT_READONLY, true)
				.getSingleResult();
		if(s==null) {
			return "S-00001";
		} else {
			try {
				int lastNumber = Integer.parseInt(s.substring(2));
				return "S-" + new DecimalFormat("00000").format(lastNumber+1);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static void changeOwnership(Study study, SpiritUser toUser, SpiritUser updater) {
		if(study.getId()<=0) return;

		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		Date now = JPAUtil.getCurrentDateFromDatabase();

		try {
			txn = session.getTransaction();
			txn.begin();		
			
			study.setUpdUser(updater.getUsername());
			study.setUpdDate(now);
			study.setCreUser(toUser.getUsername());
			session.merge(study);

			txn.commit();		
			txn = null;
		} finally {
			if(txn!=null) try{txn.rollback();}catch (Exception e) {}
		}				
	}
	
	/**
	 * Loads and populate the animals from the study randomization
	 * @param s
	 */
	public static void loadBiosamplesFromStudyRandomization(Randomization randomization) {
		
		List<AttachedBiosample> samples = randomization.getSamples();
		
		List<String> sampleIds = new ArrayList<String>();
		for (AttachedBiosample sample : samples) {
			String sampleId = sample.getSampleId();
			sampleIds.add(sampleId);
		}
		Map<String, Biosample> biosamples = DAOBiosample.getBiosamplesBySampleIds(sampleIds);
		
		for (AttachedBiosample sample : samples) {
			String sampleId = sample.getSampleId();
			if(sampleId==null) continue;
			if(sample.getBiosample()!=null && sample.getBiosample().getSampleId().equals(sampleId)) continue;
			
			//Check first in Spirit
			Biosample b = biosamples.get(sampleId);
			if(b==null) System.err.println(b+" not found");
			if(b==null) {
				//Then in AnimalDB
				b = new Biosample();
				b.setSampleId(sampleId);
				try {
					DAOBiosample.populateFromAnimalDB(b);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			sample.setBiosample(b);
		}
	}
	

	
	/**
	 * To allow animals to belong to 2 studies, we perform the following trick:
	 *            881234 
	 *            S-00001 (inherited only)
	 *          /         \
	 *  881234A             881234B
	 *  S-00001 (attached)  S-00002 (attached)
	 *  GrA (old)           GrB (new) 
	 *  
	 *  - We keep a topAnimal with no attached study
	 *  - From this topAnimal, we derive all the other animals belonging to studies
	 *  
	 *  
	 *  If an animal is reused more than twice, we continue the naming 'C', 'D', ...
	 *  
	 *  Doing so, the results are still attached to the same topAnimal, and to the group matching the given study (what we want)
	 *  
	 *  
	 * This function is supposed to be used exceptionally
	 *  
	 * @param animals
	 * @param study1
	 * @param study2
	 * @param user
	 * @return a map of animal (given as input) to a new animal (the duplicated) 
	 */
	public static Map<Biosample, Biosample> duplicateAnimalsForManyStudies(List<Biosample> animals, Study newStudy, SpiritUser user) throws Exception {
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		Map<Biosample, Biosample> old2new = new HashMap<Biosample, Biosample>();
		try {
			if(!SpiritRights.canAdmin(newStudy, user)) throw new Exception("You are not allowed to edit "+newStudy);
			for (Biosample animal : animals) {
				if(animal.getId()<=0) throw new Exception("The animal "+animal+" is not persistant");
				if(animal.getParent()!=null) throw new Exception("The animal "+animal+" has already a parent");
			}
			
			txn = session.getTransaction();
			txn.begin();
			Date now = JPAUtil.getCurrentDateFromDatabase();
			
			for (Biosample animal : animals) {
				String sampleId = animal.getSampleId();
				if(animal.getAttachedStudy()!=null) {
					//
					//Creates a Top animal belonging to study1 but not attached(assigned) to it
					Biosample topAnimal = animal.clone();
					topAnimal.setId(0); 
					topAnimal.setSampleId(sampleId+"_"); //new name to allow persistence
					topAnimal.setSampleName("");
					topAnimal.setAttachedStudy(null);
					topAnimal.setInheritedStudy(null);
					topAnimal.setInheritedGroup(null);
					topAnimal.setUpdDate(now);
					topAnimal.setUpdUser(user.getUsername());
					topAnimal.setContainer(null);
					topAnimal.setTopParent(topAnimal);
					session.persist(topAnimal);
					
					//The animal1 belongs to (as before)
					animal.setSampleId(sampleId+"A");
					animal.setParent(topAnimal);
					animal.setUpdDate(now);
					animal.setUpdUser(user.getUsername());
					animal.setTopParent(topAnimal);
					session.merge(topAnimal);
					
					//update the topparent from all animal1's children
					for (Biosample b : animal.getHierarchy(HierarchyMode.ALL)) {
						b.setTopParent(topAnimal);
					}
					
					//The animal2 belong to study2 / NoGroup
					Biosample animal2 = animal.clone();
					animal2.setId(0); 
					animal2.setSampleId(sampleId+"B");
					animal2.setAttachedStudy(newStudy);
					animal2.setInheritedStudy(newStudy);
					animal2.setInheritedGroup(null); //to be given later by the programmer
					animal2.setParent(topAnimal);
					animal2.setUpdDate(now);
					animal2.setUpdUser(user.getUsername());
					animal2.setContainer(null);
					animal2.setTopParent(topAnimal);
					session.persist(animal2);
					
					//reupdate sampleid
					topAnimal.setSampleId(sampleId);
					old2new.put(animal, animal2);
					
				} else {
					//The TopAnimal was already cloned, check the next available letter
					Biosample topAnimal = animal.getTopParent();
					char availableLetter = 'A';
					for (Biosample b : topAnimal.getChildren()) {
						if(b.getSampleId().startsWith(sampleId) && b.getSampleId().length()==sampleId.length()+1) {
							availableLetter = (char) Math.max(availableLetter, b.getSampleId().charAt(b.getSampleId().length()-1)+1);
						}
					}
					
					
					
					Biosample animal2 = animal.clone();
					animal2.setId(0); 
					animal2.setSampleId(sampleId+availableLetter);
					animal2.setAttachedStudy(newStudy);
					animal2.setInheritedStudy(newStudy);
					animal2.setInheritedGroup(null); //to be given later by the programmer
					animal2.setParent(topAnimal);
					animal2.setUpdDate(now);
					animal2.setUpdUser(user.getUsername());
					animal2.setContainer(null);
					animal2.setTopParent(topAnimal);
					session.persist(animal2);
					
					
					old2new.put(animal, animal2);
				}
				
				
			}
			
			
			
			
			txn.commit();
			txn = null;
		} finally {
			if(txn!=null && txn.isActive()) try{ txn.rollback();} catch(Exception e2) {}			
		}
		return old2new;
		
	}
	
	/**
	 * Check consistency of the study and returns a list of advices / problems
	 * 
	 * @param study
	 * @param checkSamples
	 * @return null if there are no warning
	 */
	public static String getTodoList(Study study) {
		StringBuilder sb = new StringBuilder();
		//We need groups
		if(study.getGroups().size()==0) {
			sb.append("You should define 1+ groups\n");
		}
		if(study.getPhases().size()==0) {
			sb.append("You should define 1+ phases\n");
		}
		
		//Make sure, there are no actions before the start phase
		checkActionsBeforeRnd: for (Group gr : study.getGroups()) {
			if(gr.getFromPhase()!=null) {
				for (StudyAction action : study.getStudyActions(gr)) {
					if(action.getPhase().getTime()>=gr.getFromPhase().getTime()) continue;
					if(action.isEmpty()) continue;
					sb.append("You should not have actions before the GroupAssignment (Group " + gr.getShortName() + " / " + action.getPhase().getShortName() + ")\n");
					break checkActionsBeforeRnd;
				}
			}
		}
		
		//Make sure, there are no actions after a necropsy phase
		checkActionsAfterNecro: for (Group gr : study.getGroups()) {
			for(int subGroupNo=0; subGroupNo<gr.getNSubgroups(); subGroupNo++) {
				Phase endPhase = gr.getEndPhase(subGroupNo);
				if(endPhase==null) continue;
				for (StudyAction action : study.getStudyActions(gr, subGroupNo)) {
					if(action.getPhase().getTime()<=endPhase.getTime()) continue;
					if(action.isEmpty()) continue;
					sb.append("You should not have actions after a necropsy (Gr." + gr.getShortName() + (gr.getNSubgroups()>0?" '"+(1+subGroupNo):"") +" / " + action.getPhase() + ")\n");
					break checkActionsAfterNecro;
				}
			}
		}
		
		//Make sure groups cannot be split from groups, having subgroups (no way no find out the subgroup without looking at the history)
		for (Group gr : study.getGroupsWithSplitting()) {
			if(gr.getNSubgroups()>1) {
				sb.append("The Group " + gr.getName() + " should not have subgroups because it is split\n");
				break;
			}
			
		}
		
		//Make sure the sum of animals is smaller in new groups
		for (Group gr : study.getGroups()) {
			int n = 0;
			for(Group g2: gr.getToGroups()) {
				n+=g2.getNAnimals();
			}
			if(n>gr.getNAnimals()) {
				sb.append("The Groups going from " + gr.getName() + " should not have more than "+gr.getNAnimals()+"\n");				
			}
		}
		
		//Make sure the foodWater is measured on different days
		Set<String> seenWater = new HashSet<String>();
		Set<String> seenFood = new HashSet<String>();
		for (StudyAction a: study.getStudyActions()) {
			String key = a.getGroup()+"_"+a.getSubGroup()+"_"+a.getPhase().getDays();
			if(a.isMeasureFood()) {
				if(seenFood.contains(key)) {
					sb.append("You should not have 2 Food measurements on " + a.getGroup() + " at " +a.getPhase().getShortName() + "\n");
					break;
				}
				seenFood.add(key);
			}
			if(a.isMeasureWater()) {
				if(seenWater.contains(key)) {
					sb.append("You should not have 2 Water measurements on " + a.getGroup() + " at " +a.getPhase().getShortName() + "\n");
					break;
				}
				seenWater.add(key);
			}
		}
		
		
		//Make sure, there are no measurement actions on a dividing group (no treated in applications)
		checkActionsOnDividing: for (Group gr : study.getGroups()) {
			if(gr.getDividingSampling()==null) continue;
			
			for (StudyAction action : study.getStudyActions(gr)) {					
				if(action.hasMeasurements()) {
					sb.append("You should not have measurements on a dividing group (Gr." + gr.getShortName() +" / " + action.getPhase() + ")\n");
					break checkActionsOnDividing;
				}
			}
		}
		
		//Warn if there are treatments on a dividing group (no treated in applications)
		checkActionsOnDividing: for (Group gr : study.getGroups()) {
			if(gr.getDividingSampling()==null) continue;
			
			for (StudyAction action : study.getStudyActions(gr)) {					
				if(action.getNamedTreatment()!=null) {
					sb.append("You should not have treatments on a dividing group (Gr." + gr.getShortName() +" / " + action.getPhase() + ")\n");
					break checkActionsOnDividing;
				}
			}
		}

		//Make sure all groups have a group assignment, or none
		int nGroupsWithRando = 0;
		for (Group gr : study.getGroups()) {
			if(gr.getFromPhase()!=null) nGroupsWithRando++;
		}
		if(nGroupsWithRando>0 && nGroupsWithRando<study.getGroups().size()) {
			sb.append("Some groups have no info regarding the GroupAssignment.\n");
		}
		
		//Check number of attached samples
		groupLoop: for (Group gr : study.getGroups()) {
			//if the group comes from an other group and the study is empty, skip it
			if(gr.getFromGroup()!=null && study.getAttachedBiosamples().size()==0) {
				continue groupLoop;
			}
			//if the group comes from an other group and this originating group has samples, skip it
			if(gr.getFromGroup()!=null && study.getTopAttachedBiosamples(gr.getFromGroup()).size()>0) {
				continue groupLoop;
			}
			//if the group leads to other groups and those have samples, skip it
			for (Group gr2 : study.getGroups()) {
				if(gr2.getFromGroup()==gr && gr2.getTopAttachedBiosamples().size()>0) {
					continue groupLoop;	
				}
			}
			
			
			for(int subGroupNo=0; subGroupNo<gr.getNSubgroups(); subGroupNo++) {
				Set<Biosample> attached = study.getTopAttachedBiosamples(gr, subGroupNo);
				Integer expected = gr.getSubgroupSize(subGroupNo);
				if(expected!=null && attached.size()>0 && expected>0 && attached.size()!=expected) {
					sb.append("Gr. " + gr.getShortName() + (gr.getNSubgroups()>1?" '"+(1+subGroupNo):"") + " has "+attached.size()+"/"+expected+" samples\n");
				}
			}
		}
		
		
		return sb.length()==0? null: sb.toString();
	}
	
//	/**
//	 * Return all samples that should be deleted:
//	 *  - phase where there are no available actions...
//	 *  
//	 * @param study
//	 * @return
//	 */
//	public static void classifySamples(Study study, Set<Biosample> invalidSamples, Set<Biosample> notAttached, Set<Biosample> validSamples, Set<Biosample> toBeCreated ) {
//		for (Biosample animal: study.getAttachedBiosamples()) {
//			for(Biosample b: animal.getChildren()) {
//				Sampling s = b.getAttachedSampling();
//				if(s==null) {
//					notAttached.add(b);
//					notAttached.addAll(b.getHierarchy(HierarchyMode.CHILDREN));
//				} else {
//					if(b.getInheritedPhase()==null) {
//						invalidSamples.add(b);
//						invalidSamples.addAll(b.getHierarchy(HierarchyMode.CHILDREN));
//					} else {
//						StudyAction action = animal.getStudyAction(b.getInheritedPhase());
//						if(action==null) {
//							invalidSamples.add(b);
//							invalidSamples.addAll(b.getHierarchy(HierarchyMode.CHILDREN));
//						}
//					}
//				}
//			}
//		}
//		
//		//Check samples that should be created
//		List<Biosample> samples = BiosampleCreationHelper.processTemplateInStudy(null, study, null, null, false, null, false);
//		for (Biosample b : samples) {
//			if(b.getAttachedStudy()!=null) continue; //skip animals
//			if(b.getId()<=0) {
//				toBeCreated.add(b);
//			} else {
//				validSamples.add(b);
//			}
//		}
//	}
//	
	
	public static List<Study> getRelatedStudies(List<Study> studies, SpiritUser user) {
		List<Study> res = new ArrayList<Study>();
		if(studies.size()==0) {
			return res;
		}
		res.addAll(getRelatedStudies(studies.get(0), user));
		for (int i = 1; i < studies.size(); i++) {
			List<Study> r = getRelatedStudies(studies.get(i), user);
			res.retainAll(r);
		}
		res.removeAll(studies);
		return res;
	}
	
	private static String tos(double[] a) {
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < a.length; j++) {
			sb.append((j>0?",":"") + Formatter.format2(a[j]));			
		}
		return "["+sb.toString()+"]";
	}
	public static List<Study> getRelatedStudies(Study study, SpiritUser user) {
		List<Study> res = new ArrayList<Study>();
		double[] desc1 = getDescriptors(study);
		PriorityQueue<Study> queue = new PriorityQueue<Study>();
		int last1 = study.getLastPhase()==null? 0: study.getLastPhase().getDays();
		for(Study s: getStudies()) {
			if(s==study) continue;
			double score = 0;
			Set<String> compounds = new HashSet<String>(study.getCompounds());
			if(compounds.size()>0) {
				compounds.retainAll(s.getCompounds());
				score += compounds.size()>0?0: 1;
			} else if(s.getCompounds().size()>0) {
				score += .5;
			}
//			if(CompareUtils.compare(s.getProject(), study.getProject())!=0) {
//				score += 1;
//			}
			
			int last2 = s.getLastPhase()==null? 0:s.getLastPhase().getDays();
			score += 3.0 * Math.abs(last1-last2) / Math.max(1, (last1+last2)/2);

			
			
			double[] desc2 = getDescriptors(s);
			for (int i = 0; i < desc2.length; i++) {
//				score += Math.abs(desc1[i]-desc2[i]) * 4.0 /desc2.length;
				score += Math.min(1, Math.abs(desc1[i]-desc2[i]) / ((.2+desc1[i]+desc2[i])/2)) * 4.0 /desc2.length;
			}		
			
			queue.add(s, score);
		}
		for(@SuppressWarnings("rawtypes") Elt e: queue.getList()) {
			if(res.size()>5) break;
			Study s = (Study) e.obj;
			System.out.println("# "+study+" - " + s+" > " + Formatter.format2(e.val) +" > "+ s.getCompounds() + " > " +tos(getDescriptors(s)));
			res.add(s);
		}
		
		return res;
	}
	
	/**
	 * Returns some descriptors of the study between 0-1
	 * @param s
	 * @return
	 */
	public static double[] getDescriptors(Study s) {
		int nMeasurements = 0;
		int nWeighings = 0;
		int nTreatments = 0;
		int nSamplings = 0;
		int nValid = 0;
		int nTotal = 0;
		double maxMeasurements = 0;
		double maxWeighings = 0;
		double maxTreatments = 0;
		double maxSamplings = 0;
		int nGroupsWithRnd = 0;
		int nGroupsWithFrom = 0;
//		int nGroupsWithStrats = 0;
		int nGroups = 0;
		for(Group g: s.getGroups()) {
			nGroups++;
			if(g.getFromGroup()!=null) nGroupsWithFrom++;
			if(g.getFromPhase()!=null) nGroupsWithRnd++;
//			if(g.getSubgroupSize()>1) nGroupsWithStrats++;
			for(int i=0; i<g.getNSubgroups(); i++) {
				
				int measurements = 0;
				int weighings = 0;
				int treatments = 0;
				int samplings = 0;
				int valid = 0;
				int total = 0;
				boolean ended = false;
				for(Phase p: s.getPhases()) {
					total++;
					if(g.getEndPhase(i)==p) ended = true;
					if(!ended) valid++;
					StudyAction a = s.getStudyAction(g, p, i);
					if(a==null) continue;
					if(a.getNamedTreatment()!=null) treatments++;
					if(a.getNamedSampling1()!=null || a.getNamedSampling2()!=null) samplings++;
					if(a.hasMeasurements()) measurements++;
					if(a.isMeasureWeight()) weighings++;
				}
				if(total==0) total=1;
				maxTreatments = Math.max(maxTreatments, (double)treatments/total);
				maxSamplings = Math.max(maxSamplings, (double)samplings/total);
				maxMeasurements = Math.max(maxMeasurements, (double)measurements/total);
				maxWeighings = Math.max(maxWeighings, (double)weighings/total);
				nTotal+=total;
				
				nTreatments+=treatments;
				nSamplings+=samplings;
				nMeasurements+=measurements;
				nWeighings+=weighings;
				nValid+=valid;
				
			}
		}
		if(nTotal==0) nTotal=1;
		if(nGroups==0) nGroups=1;
		return new double[] {
				(double) nGroupsWithFrom/nGroups, 
				(double) nGroupsWithRnd/nGroups,  
				(double)nTreatments/nTotal, 
				maxTreatments, 
				(double)nMeasurements/nTotal,
				maxMeasurements,
				(double)nWeighings/nTotal,
				(double)nSamplings/nTotal, 
				maxSamplings, 
				maxWeighings, 
				(double) nValid/nTotal}; 
		
	}
	
	
	
	public static Map<Biotype, Triple<Integer, String, Date>> countRecentSamplesByBiotype(Date minDate) {
		Map<Study, Map<Biotype, Triple<Integer, String, Date>>> res = countSamplesByStudyBiotype(null, minDate);
		if(res.size()>0) return res.values().iterator().next();
		return null;
	}
	public static Map<Study, Map<Biotype, Triple<Integer, String, Date>>> countSamplesByStudyBiotype(Collection<Study> studies) {
		return countSamplesByStudyBiotype(studies, null);
	}
	/**
	 * Return a map of study->(biotype->n.Samples)
	 * @param studies
	 * @return
	 */
	private static Map<Study, Map<Biotype, Triple<Integer, String, Date>>> countSamplesByStudyBiotype(Collection<Study> studies, Date minDate) {
		EntityManager session = JPAUtil.getManager();
		
		Map<Study, Map<Biotype, Triple<Integer, String, Date>>> res = new HashMap<>();		 
		Map<String, Map<String, Triple<Integer, String, Date>>> map = new HashMap<>();		 
		 
		String query = studies==null?
				"select '', b.biotype.name, b.updUser, count(b), max(b.updDate) "
				+ " from Biosample b "
				+ " where b.inheritedStudy is null"
				+ (minDate!=null?" and b.updDate > ?1":"")
				+ " group by b.biotype.name, b.updUser":
					
				"select b.inheritedStudy.studyId, b.biotype.name, b.updUser, count(b), max(b.updDate) "
				+ " from Biosample b "
				+ " where " + QueryTokenizer.expandForIn("b.inheritedStudy.id", JPAUtil.getIds(studies))
				+ " group by b.inheritedStudy.studyId, b.biotype.name, b.updUser";			
		
		Query q = session.createQuery(query);
		if(minDate!=null) q.setParameter(1, minDate);
		List<Object[]> results = q.getResultList();
		 
		for (Object[] strings : results) {
			 String sid = (String)strings[0];
			 String key = (String)strings[1];
			 int n = Integer.valueOf(""+strings[3]);
			 String user = (String) strings[2];
			 Date date = (Date) strings[4];

			 Map<String, Triple<Integer, String, Date>> m = map.get(sid);
			 if(m==null) {
				 m = new HashMap<>();
				 map.put(sid, m);
			 }
			 if(m.get(key)!=null) {
				 Triple<Integer, String, Date> e = m.get(key);
				 m.put(key, new Triple<Integer, String, Date>(e.getFirst() + n, e.getThird().after(date)? e.getSecond(): user, e.getThird().after(date)? e.getThird(): date));				 
			 } else {
				 m.put(key, new Triple<Integer, String, Date>(n, user, date));
			 }
		}
		
		 
		//Convert map to the underlying type
		Map<String, Study> mapStudy = Study.mapStudyId(studies);
		for (String n1 : map.keySet()) {
			Map<Biotype, Triple<Integer, String, Date>> m2 = new TreeMap<>();
			for (String n2 : map.get(n1).keySet()) {
				m2.put(DAOBiotype.getBiotype(n2), map.get(n1).get(n2));
			}			
			res.put(mapStudy.get(n1), m2);
		}
		
		return res;		
	}
	
	/**
	 * Return a map of studyId->(testName->n.Samples)
	 * @param studies
	 * @return
	 */
	public static Map<Study, Map<Test, Triple<Integer, String, Date>>> countResultsByStudyTest(Collection<Study> studies) {
		assert studies!=null;
		
		EntityManager session = JPAUtil.getManager();
		
		Map<Study, Map<Test, Triple<Integer, String, Date>>> res = new HashMap<>();		 
		Map<String, Map<String, Triple<Integer, String, Date>>> map = new HashMap<>();		 
		 
		String query = "select r.biosample.inheritedStudy.studyId, r.test.name, r.updUser, count(r), max(r.updDate) "
				+ " from Result r "
				+ " where " + QueryTokenizer.expandForIn("r.biosample.inheritedStudy.id", JPAUtil.getIds(studies))
				+ " group by r.biosample.inheritedStudy.studyId, r.test.name, r.updUser";
					
		List<Object[]> results = session.createQuery(query).getResultList();
		 
		for (Object[] strings : results) {
			 String sid = (String)strings[0];
			 String key = (String)strings[1];
			 int n = Integer.valueOf(""+strings[3]);
			 String user = (String) strings[2];
			 Date date = (Date) strings[4];

			
			 Map<String, Triple<Integer, String, Date>> m = map.get(sid);
			 if(m==null) {
				 m = new HashMap<>();
				 map.put(sid, m);
			 }
			 if(m.get(key)!=null) {
				 Triple<Integer, String, Date> e = m.get(key);
				 m.put(key, new Triple<Integer, String, Date>(e.getFirst() + n, e.getThird().after(date)? e.getSecond(): user, e.getThird().after(date)? e.getThird(): date));				 
			 } else {
				 m.put(key, new Triple<Integer, String, Date>(n, user, date));
			 }
		}
		 
		//Convert map to the underlying type
		Map<String, Study> mapStudy = Study.mapStudyId(studies);
		for (String n1 : map.keySet()) {
			Map<Test, Triple<Integer, String, Date>> m2 = new TreeMap<Test, Triple<Integer, String, Date>>();
			for (String n2 : map.get(n1).keySet()) {
				Test t = DAOTest.getTest(n2);
				if(t!=null && map.get(n1).get(n2)!=null) {
					m2.put(t, map.get(n1).get(n2));
				} else {
					System.err.println("Test "+n2+" not found");
				}
			}			
			res.put(mapStudy.get(n1), m2);
		}
		 
		return res;		
	}
	
	
	public static Map<Test, Integer> countResults(Collection<Study> studies, Biotype biotype) {
		
		EntityManager session = JPAUtil.getManager();
		
		Map<Test, Integer> res = new TreeMap<Test, Integer>();
		List<Object[]> results;
		String sql = "select r.test.id, count(r) from Result r"
					+ " where 1=1" 
					+ (studies==null || studies.size()==0? "": " and " + QueryTokenizer.expandForIn("r.biosample.inheritedStudy.id", JPAUtil.getIds(studies)))
					+ (biotype==null? "": " and " + "r.biosample.biotype.id = "+biotype.getId())
					+ " group by r.test.id";
		results = session.createQuery(sql).getResultList();
		
		for (Object[] strings : results) {
			Test t = DAOTest.getTest(Integer.parseInt(""+strings[0]));
			res.put(t, Integer.parseInt(""+strings[1]));
		}
		return res;		
	}

	
	public static void main(String[] args) throws Exception {
		final Study study1 = DAOStudy.getStudyByStudyId("S-00082");
		
				

		final long s = System.currentTimeMillis();
		fullLoad(study1);
		System.out.println("DAOStudy.main(0) "+(System.currentTimeMillis()-s)+"ms");
		
		System.out.println("DAOStudy" + getRelatedStudies(study1, DAOSpiritUser.loadUser("freyssj")));
	}
	
	
	
	
}



