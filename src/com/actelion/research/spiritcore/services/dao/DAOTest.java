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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.QueryTokenizer;

@SuppressWarnings("unchecked")
public class DAOTest {

	private static Logger logger = LoggerFactory.getLogger(DAOTest.class);

	public static final String WEIGHING_TESTNAME = "Weighing";
	public static final String LENGTH_TESTNAME = "Length";
	public static final String FOODWATER_TESTNAME = "FoodWater";
	public static final String OBSERVATION_TESTNAME =  "Observation";
	
	public static Collection<String> getTestCategories() {
		Set<String> res = new TreeSet<>();
		for(Test t: getTests()) {
			res.add(t.getCategory());
		}
		return res;
	}
	
	public static Test getFoodWaterTest() {
		Test test = getTest(FOODWATER_TESTNAME);
		if(test==null) throw new RuntimeException("The test "+FOODWATER_TESTNAME+" is not found");
		return test;
	}
	
	public static Test getWeighingTest() {
		Test test = getTest(WEIGHING_TESTNAME);
		if(test==null) throw new RuntimeException("The test "+WEIGHING_TESTNAME+" is not found");
		return test;
	}
	
	public static Test getObservationTest() {
		Test test = getTest(OBSERVATION_TESTNAME);
		if(test==null) throw new RuntimeException("The test "+OBSERVATION_TESTNAME+" is not found");
		return test;
	}
	
	
	public static Test getTest(int id) {
		getTests();
		Map<Integer, Test> id2Test = (Map<Integer, Test>) Cache.getInstance().get("id2Test");
		return id2Test.get(id);
	}
	
	public static List<Test> getTests(Collection<Integer> ids) {
		List<Test> res = new ArrayList<Test>();
		getTests();
		Map<Integer, Test> id2Test = (Map<Integer, Test>) Cache.getInstance().get("id2Test");
		for (int id : ids) {
			Test t = id2Test.get(id);
			if(t!=null) res.add(t);			
		}
		return res;
		
		
	}

	
	/**
	 * Return the union tests that were done in all given studies
	 * @param study
	 * @return
	 */
	public static List<Test> getTestsFromStudies(List<Study> studies) {
		return getTestsFromStudies(studies, null);		
	}
	
	/**
	 * Return the union tests that were done in all given studies
	 * @param study
	 * @param outIntersection (if not null, returns the intersection)
	 * @return
	 */
	public static List<Test> getTestsFromStudies(List<Study> studies, Set<Test> outIntersection) {
		EntityManager session = JPAUtil.getManager();
		boolean first = true;
		Set<Test> res = new TreeSet<Test>();
		
		for(Study study: studies) {
			List<Test> list = (List<Test>) session.createQuery(
					"select r.test FROM Result r where r.biosample.inheritedStudy = ?1").setParameter(1, study).getResultList();
			if(first) {
				res.addAll(list);
				if(outIntersection!=null) outIntersection.addAll(list);
				first=false;
			} else {
				res.addAll(list);				
				if(outIntersection!=null) outIntersection.retainAll(list); 
			}
		}
		
		return new ArrayList<Test>(res);
	}
	
	public static List<Test> getTestsFromElbs(String elbs) {
		try {
			EntityManager session = JPAUtil.getManager();
			String sql = "select distinct(r.test) FROM Result r where " + QueryTokenizer.expandOrQuery("r.elb = ?", elbs);
			List<Test> tests = (List<Test>) session.createQuery(sql);
			Collections.sort(tests);
			return tests;
		} catch(Exception e) {
			e.printStackTrace();
			return new ArrayList<Test>();
		}
	}
	
	public static Test getTest(String name) {
		getTests();
		Map<String, Test> name2Test = (Map<String, Test>) Cache.getInstance().get("name2Test");
		return name2Test.get(name);
	}

	public static List<Test> getTests() {
		List<Test> res = (List<Test>) Cache.getInstance().get("allTests");
		if(res==null) {
			
			EntityManager session = JPAUtil.getManager();
			res = (List<Test>) session.createQuery(
					"SELECT distinct(t) FROM Test t  left join fetch t.attributes").getResultList();
			Collections.sort(res);
			
			
			Map<String, Test> name2Test = new HashMap<>();
			Map<Integer, Test> id2Test = new HashMap<>();
			for (Test test : res) {
				name2Test.put(test.getName(), test);
				id2Test.put(test.getId(), test);
			}
			
			Cache.getInstance().add("allTests", res, 180);
			Cache.getInstance().add("name2Test", name2Test, 180);
			Cache.getInstance().add("id2Test", id2Test, 180);
		}
		return res;
	}
	

	public static void persistTests(Collection<Test> tests, SpiritUser user) throws Exception {
		if(tests==null || tests.size()==0) return;
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			persistTests(session, tests, user);
			txn.commit();
			txn = null;
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {}
		}		
	}
	public static void persistTests(EntityManager session, Collection<Test> tests, SpiritUser user) throws Exception {
		logger.info("Persist "+tests.size()+" tests");
		if(!SpiritRights.isSuperAdmin(user)) throw new Exception("You must be an admin to edit a test");
		
		Date now = JPAUtil.getCurrentDateFromDatabase();
		
		for (Test test : tests) {
			
			if(test.getName()==null || test.getName().trim().length()==0) throw new Exception("The test name cannot be null");
			test.setName(test.getName().trim());

			if(user!=null) {
				test.setUpdUser(user.getUsername());
				test.setUpdDate(now);
			}
			
			for (TestAttribute att : test.getAttributes()) {
				if(att.getName()==null || att.getName().length()==0) throw new Exception("The attributes must have a name");
				if(att.getDataType()==null) throw new Exception("You must specify a datatype for "+att);
				if(att.getDataType()==DataType.FORMULA && att.getOutputType()!=OutputType.OUTPUT) throw new Exception("The datatype formula is only allowed for output attributes"); 
				att.setTest(test);
			}
			
			if(test.getId()>0) {
				test = session.merge(test);
			} else {
				if(getTest(test.getName())!=null) throw new Exception("The test "+test.getName()+" is not unique");
				test.setCreUser(test.getUpdUser());
				test.setCreDate(test.getUpdDate());
				session.persist(test);				
			}
		}
		
		Cache.getInstance().remove("allTests");
		Cache.getInstance().remove("testCategories");
	}

	public static void removeTest(Test test, SpiritUser user) throws Exception {
		if(user==null || !user.isSuperAdmin()) throw new Exception("You must be am admin");
		//Make sure that there are no results linked to it
		ResultQuery q = new ResultQuery();
		q.getTestIds().add(test.getId());
		List<Result> results = DAOResult.queryResults(q, null);
		if(results.size()>0) throw new Exception("You cannot delete "+test+" before deleting the "+results.size()+" results");
		
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			if(!session.contains(test)) {
				test = session.merge(test);
			}
			session.remove(test);
			txn.commit();
			txn = null;
			Cache.getInstance().remove("allTests");
			Cache.getInstance().remove("testCategories");

		} finally {
			if(txn!=null) try{txn.rollback();}catch (Exception e) {}
		}	
	}
	
	
	public static Set<String> getAutoCompletionFields(TestAttribute att) {
		return getAutoCompletionFields(att, null);
	}
	public static Set<String> getAutoCompletionFields(TestAttribute att, Study study) {
		
		String key = "auto_"+att.getId()+"_"+(study==null?0:study.getId());
		
		//Retrieve from cache if possible
		Set<String> res = (Set<String>) Cache.getInstance().get(key);
		if(res==null) {
			EntityManager session = JPAUtil.getManager();
			Query query =  session.createQuery(
					"select distinct(v.value) from ResultValue v" +
					" where v.attribute = ?1 and v.value is not null " +
					(study!=null? " and (v in (select v2 from ResultValue v2 where v2.result.phase.study.id = "+study.getId()+") or v in (select v2 from ResultValue v2 where v2.result.biosample.inheritedStudy.id = "+study.getId()+"))":""))
				.setParameter(1, att);
			
			 res = new TreeSet<String>( (List<String>) query.getResultList());
			 Cache.getInstance().add(key, res, Cache.FAST);
			 logger.debug("loadded autocompletion fields for "+att+"/"+study+" > n="+res.size());
		}
		return res;
	}

}
