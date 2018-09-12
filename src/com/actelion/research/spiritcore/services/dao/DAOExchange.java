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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping;

/**
 * DAO functions linked to import / export of exchange files
 *
 * @author Joel Freyss
 */
public class DAOExchange {


	public static void persist(ExchangeMapping mapping, SpiritUser user) throws Exception {
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			persist(session, mapping, user);
			txn.commit();
			txn = null;
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}
	}

	public static void persist(EntityManager session, ExchangeMapping mapping, SpiritUser user) throws Exception {
		if(user==null) throw new Exception("You must give a user");



		//Retrieve the mapped objects
		List<Test> tests = mapping.getMappedTests();
		List<Biotype> biotypes = mapping.getMappedBiotypes();

		List<Study> studies = mapping.getMappedStudies();
		List<Biosample> biosamples = mapping.getMappedBiosamples();
		List<Location> locations = mapping.getMappedLocations();
		List<Result> results = mapping.getMappedResults();


		if(biotypes.size()>0 && !user.isSuperAdmin()) throw new Exception("You must be an admin to save biotypes");
		if(tests.size()>0 && !user.isSuperAdmin()) throw new Exception("You must be an admin to save tests");


		//Save the different entities. Careful: the order of those statements is important:
		//test/biotype, studies, locations, biosample, results
		LoggerFactory.getLogger(DAOExchange.class).debug("Persist Biotypes: " + biotypes);
		DAOBiotype.persistBiotypes(session, biotypes, user);
		session.flush();

		LoggerFactory.getLogger(DAOExchange.class).debug("Persist Tests: " + tests);
		DAOTest.persistTests(session, tests, user);
		session.flush();

		LoggerFactory.getLogger(DAOExchange.class).debug("Persist Studies: " + studies);
		DAOStudy.persistStudies(session, studies, user);
		session.flush();

		LoggerFactory.getLogger(DAOExchange.class).debug("Persist Locations: n=" + locations.size());
		DAOLocation.persistLocations(session, locations, user);
		session.flush();

		LoggerFactory.getLogger(DAOExchange.class).debug("Persist Biosamples: n=" + biosamples.size());
		DAOBiosample.persistBiosamples(session, biosamples, user);
		session.flush();

		//Remap results
		for (Result result : results) {
			Map<TestAttribute, ResultValue> map2 = new HashMap<>();
			for (Map.Entry<TestAttribute, ResultValue> e : result.getResultValueMap().entrySet()) {
				Test t = e.getKey().getTest();
				if(t.getId()<=0) {
					t = (Test) session.createQuery("from Test t where name = ?1").setParameter(1, t.getName()).getSingleResult();
				}
				assert t.getId()>0;
				assert t.getAttribute(e.getKey().getName()).getId()>0;
				e.getKey().setTest(t);
				map2.put(t.getAttribute(e.getKey().getName()), e.getValue());
			}

		}

		LoggerFactory.getLogger(DAOExchange.class).debug("Persist Results: n=" + results.size());
		DAOResult.persistResults(session, results, user);

	}
}
