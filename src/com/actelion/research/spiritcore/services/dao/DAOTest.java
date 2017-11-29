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

/**
 * DAO functions linked to Tests
 *
 * @author Joel Freyss
 */
public class DAOTest {

	private static Logger logger = LoggerFactory.getLogger(DAOTest.class);

	public static final String WEIGHING_TESTNAME = "Weighing";
	public static final String LENGTH_TESTNAME = "Length";
	public static final String FOODWATER_TESTNAME = "FoodWater";
	public static final String OBSERVATION_TESTNAME =  "Observation";

	public static List<Test> getTestsFromElbs(String elbs) {
		try {
			EntityManager session = JPAUtil.getManager();
			List<Test> tests = session.createQuery("select distinct(r.test) FROM Result r where " + QueryTokenizer.expandOrQuery("r.elb = ?", elbs)).getResultList();
			Collections.sort(tests);
			return tests;
		} catch(Exception e) {
			e.printStackTrace();
			return new ArrayList<Test>();
		}
	}


	private static Map<Integer, Test> getId2TestMap() {
		@SuppressWarnings("unchecked")
		Map<Integer, Test> id2Test = (Map<Integer, Test>) Cache.getInstance().get("id2test"+JPAUtil.getManager());
		if(id2Test==null) {

			EntityManager session = JPAUtil.getManager();
			List<Test> res = session.createQuery(
					"SELECT distinct(t) FROM Test t left join fetch t.attributes").getResultList();
			id2Test = JPAUtil.mapIds(res);
			Cache.getInstance().add("id2Test_"+JPAUtil.getManager(), id2Test, Cache.FAST);
		}
		return id2Test;
	}

	public static Test getTest(int id) {
		return getId2TestMap().get(id);
	}

	public static List<Test> getTests(Collection<Integer> ids) {
		Map<Integer, Test> id2Test = getId2TestMap();

		List<Test> res = new ArrayList<>();
		for (Integer id : ids) {
			Test t = id2Test.get(id);
			if(t!=null) res.add(t);
		}
		return res;
	}

	public static Test getTest(String name) {
		Map<Integer, Test> id2Test = getId2TestMap();
		for (Test t : id2Test.values()) {
			if(t.getName().equals(name)) return t;
		}
		return null;
	}

	public static List<Test> getTests() {
		return getTests(false);
	}

	public static List<Test> getTests(boolean showHidden) {
		Map<Integer, Test> id2Test = getId2TestMap();
		List<Test> tests = new ArrayList<>();
		for (Test test : id2Test.values()) {
			if(!showHidden && test.isHidden()) continue;
			tests.add(test);
		}

		Collections.sort(tests);
		return tests;
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

			if(test.getCategory()==null || test.getCategory().trim().length()==0) throw new Exception("Category is required");
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
		Cache.getInstance().remove("id2test"+JPAUtil.getManager());
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
			Cache.getInstance().remove("id2test"+JPAUtil.getManager());

		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {}
		}
	}

	public static Set<String> getAutoCompletionFields(TestAttribute att) {
		return getAutoCompletionFields(Collections.singleton(att), null);
	}

	public static Set<String> getAutoCompletionFields(Collection<TestAttribute> atts, Study study) {
		EntityManager session = JPAUtil.getManager();
		Query query =  session.createQuery(
				"select distinct(rv.value) from ResultValue rv" +
						" where " + QueryTokenizer.expandForIn("rv.attribute.id", JPAUtil.getIds(atts)) + " and rv.value is not null " +
						(study!=null? " and rv.result.biosample.inheritedStudy.id = "+study.getId():""));

		return new TreeSet<String>( query.getResultList());
	}

	public static Map<TestAttribute, Collection<String>> getInputFields(Integer testId, String studyIds) throws Exception {
		EntityManager session = JPAUtil.getManager();
		Map<TestAttribute, Collection<String>> res = new HashMap<>();

		if(studyIds==null || studyIds.trim().length()==0) return res;
		Test test = DAOTest.getTest(testId);
		if(test==null) return res;

		for (TestAttribute att : test.getInputAttributes()) {
			StringBuilder jpql = new StringBuilder();
			jpql.append("SELECT distinct(rv.value) FROM ResultValue rv " +
					" where (" + QueryTokenizer.expandOrQuery("rv.result.biosample.inheritedStudy.studyId = ?", studyIds) + ")" +
					" and rv.attribute.id = "+att.getId() /*+ " and rv.attribute.outputType = 'INPUT'"*/);

			Query query = session.createQuery(jpql.toString());
			List<String> choices = new ArrayList<>();

			for (String string : (List<String>) query.getResultList()) {
				if(string==null) {
					choices.add("");
				} else {
					choices.add(string);
				}
			}
			res.put(att, choices);
		}

		return res;
	}


	public static int countRelations(TestAttribute testAttribute) {
		if (testAttribute == null || testAttribute.getId() <= 0) return 0;
		int id = testAttribute.getId();
		EntityManager session = JPAUtil.getManager();
		return ((Long) session.createQuery("select count(*) from ResultValue rv where rv.attribute.id = " + id + " and rv.value is not null and length(rv.value)>0").getSingleResult()).intValue();
	}


}
