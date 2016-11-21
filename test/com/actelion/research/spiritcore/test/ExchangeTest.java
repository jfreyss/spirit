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

package com.actelion.research.spiritcore.test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Test;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.SchemaCreator;
import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOExchange;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping;
import com.actelion.research.spiritcore.services.exchange.Exporter;
import com.actelion.research.spiritcore.services.exchange.Importer;

public class ExchangeTest extends AbstractSpiritTest {

	@Test
	public void testImportDemo() throws Exception {
		JPAUtil.closeFactory();
		SchemaCreator.clearExamples(user);
		Assert.assertFalse(JPAUtil.getManager().getTransaction().isActive());
		
		{
			Exchange exchange = initDemoExamples(user);
			Study s = exchange.getStudies().iterator().next();
			Assert.assertEquals(2, s.getNamedSamplings().size());
			Assert.assertEquals(2, s.getSamplings("Blood Sampling", "Blood: EDTA; Alive").size());
			
			Biotype blood = Biotype.mapName(exchange.getBiotypes()).get("Blood");
			Assert.assertNotNull(blood);
			BiotypeMetadata bloodType = blood.getMetadata("Type");
			Assert.assertNotNull(bloodType);
		}
		JPAUtil.clear();
		{
			
			
			//Check biotypes
			Biotype blood = DAOBiotype.getBiotype("Blood");
			Assert.assertNotNull(blood);
			BiotypeMetadata bloodType = blood.getMetadata("Type");
			Assert.assertNotNull(bloodType);	
			
			//Check the studies
			Assert.assertTrue(DAOStudy.getStudies().size()>=2);
			Study s1 = DAOStudy.queryStudies(StudyQuery.createForLocalId("IVV2016-1"), user).get(0);
			Study s2 = DAOStudy.queryStudies(StudyQuery.createForLocalId("IVV2016-2"), user).get(0);
			Assert.assertEquals("IVV2016-1", s1.getIvv());
			Assert.assertEquals("IVV2016-2", s2.getIvv());
			Assert.assertEquals(2, s1.getNamedSamplings().size());
			
			List<Sampling> samplings = s1.getSamplings("Blood Sampling", "Blood: EDTA; Alive");
			Assert.assertEquals(2, samplings.size());
			Assert.assertTrue(samplings.get(0).getSamples().size()>0);
			Assert.assertTrue(samplings.get(1).getSamples().size()>0);
			Assert.assertEquals(samplings.get(0).getSamples().size(), samplings.get(1).getSamples().size());

			
			//
			//Check the biosamples
			BiosampleQuery q1 = new BiosampleQuery();
			q1.setStudyIds(s1.getStudyId());
			List<Biosample> biosamples = DAOBiosample.queryBiosamples(q1, user);
			Assert.assertTrue(biosamples.size()>0);
			for (Biosample b : biosamples) {
				System.out.println("ExchangeTest.testImportDemo() "+b.getSampleId()+ " from " +b.getParent() + ": "+ b.getInfos());
				if(b.getParent()==null) {
					Assert.assertEquals("Rat", b.getMetadataValue("Type"));
					Assert.assertTrue(b.getAttachedSampling()==null);
				} else {
					Assert.assertTrue(b.getAttachedSampling()!=null);
				}
			}
			BiosampleQuery q2 = new BiosampleQuery();
			q2.setStudyIds(s1.getStudyId());
			q2.setBiotype(DAOBiotype.getBiotype("Blood"));
			List<Biosample> bloods = DAOBiosample.queryBiosamples(q2, user);
			Assert.assertTrue(bloods.size()>0);
			System.out.println(bloods.get(0).getMetadataAsString());
			Assert.assertEquals("EDTA Alive", bloods.get(0).getMetadataAsString());
			
			
			//Check the locations
			Location loc = DAOLocation.getCompatibleLocation("Office/Lab 2/Tank A/Tower 1/Box 1", user);
			Assert.assertTrue("The location " + "Office/Lab 2/Tank A/Tower 1/Box 1" + " is empty", loc.getBiosamples().size()>0);
			for (Biosample b : loc.getBiosamples()) {
				Assert.assertTrue("The position " + b + " is null", b.getPos()>=0);
				Assert.assertTrue("The attached sampling of " + b + " is null", b.getParent()==null || b.getAttachedSampling()!=null);
			}
			Assert.assertTrue(loc.getBiosamples().size()>0);
			
			//Check the results
			ResultQuery qr = new ResultQuery();
			qr.setStudyIds(s1.getStudyId());
			List<Result> results = DAOResult.queryResults(qr, user);
			Assert.assertTrue(results.size()>0);
			for (Result r : results) {			
				Assert.assertTrue(r.getOutputResultValuesAsString().length()>0);
			}
		}
		
		{
			//Check if we do the import it twice (->map, without inserts)
			try (InputStream is = DBAdapter.class.getResourceAsStream("demo.spirit")) {
				Exchange exchange = Importer.read(new InputStreamReader(is));
				ExchangeMapping mapping = new ExchangeMapping(exchange);
				DAOExchange.persist(mapping, user);
			}		
			Assert.assertTrue(DAOStudy.getStudies().size()>=2);
		}
		
		
		
		//Reset
		JPAUtil.closeFactory();
		EntityManager session = null;
		try {			
			session = JPAUtil.createManager();
			session.getTransaction().begin();
			DAOStudy.deleteStudies(session, DAOStudy.getStudies(), true, user);
			session.getTransaction().commit();									
		} catch(Throwable e3) {
			if(session!=null && session.getTransaction().isActive()) session.getTransaction().rollback();
			throw e3;
		} finally {
			session.close();
		}								
		
		try (InputStream is = DBAdapter.class.getResourceAsStream("demo.spirit")) {
			Exchange exchange = Importer.read(new InputStreamReader(is));
			JPAUtil.pushEditableContext(user);
			ExchangeMapping mapping = new ExchangeMapping(exchange);
			DAOExchange.persist(mapping, user);
		} catch(Throwable e2) {
			e2.printStackTrace();
			throw e2;
		} finally {
			JPAUtil.popEditableContext();
		}
		{
			//Test new imports: mapping should have been made on the existing biotypes/tests
			List<Study> studies = DAOStudy.queryStudies(StudyQuery.createForLocalId("IVV2016-1"), user);
			Assert.assertEquals(1, studies.size());
			int sid = studies.get(0).getId();
			List<Biosample> biosamples = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForSids(Collections.singleton(sid)), user);
			Assert.assertTrue(biosamples.size()>0);
			for (Biosample b : biosamples) {
				if(b.getParent()==null) {
					Assert.assertEquals("Rat", b.getMetadataValue("Type"));
					Assert.assertTrue(b.getAttachedSampling()==null);
				} else {
					Assert.assertTrue(b.getAttachedSampling()!=null);
				}
			}
			BiosampleQuery q2 = new BiosampleQuery();
			q2.setSids(Collections.singleton(sid));
			q2.setBiotype(DAOBiotype.getBiotype("Blood"));
			List<Biosample> bloods = DAOBiosample.queryBiosamples(q2, user);
			Assert.assertTrue(bloods.size()>0);
			Assert.assertEquals("EDTA Alive", bloods.get(0).getMetadataAsString());
			
			List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForSids(Collections.singleton(sid)), user);
			Assert.assertTrue(results.size()>0);
			for (Result r : results) {
				Assert.assertTrue(r.getOutputResultValuesAsString().length()>0);
			}

			
		}

	}

	/*
	@Test
	public void testImportExchange1() throws Exception {
		Assert.assertFalse(JPAUtil.getManager().getTransaction().isActive());

		int n = DAOStudy.getStudies().size();

		//1st import: should create without error
		try (FileReader r =  new FileReader(new File("test/files/S-00085.spirit"))) {
			Exchange exchange = Importer.read(r);			
			ExchangeMapping mapping = new ExchangeMapping(exchange);
			DAOExchange.persist(mapping, user);			
		}
		Assert.assertEquals(n+1, DAOStudy.getStudies().size());
	
//		//2nd import: should replace
//		try (FileReader r =  new FileReader(new File("test/files/S-00085.spirit"))) {
//			Exchange exchange = Importer.read(r);
//			ExchangeMapping mapping = new ExchangeMapping(exchange, MappingAction.MAP_REPLACE);			
//			DAOExchange.persist(mapping, user);
//		}
//		Assert.assertEquals(1, DAOStudy.getStudies(user).size());

		//3rd import: skip (default)
		try (FileReader r =  new FileReader(new File("test/files/S-00085.spirit"))) {
			Exchange exchange = Importer.read(r);
			ExchangeMapping mapping = new ExchangeMapping(exchange, MappingAction.SKIP);			
			DAOExchange.persist(mapping, user);
		}
		Assert.assertEquals(n+1, DAOStudy.getStudies().size());

		//4th import: should create a new study
		try (FileReader r =  new FileReader(new File("test/files/S-00085.spirit"))) {
			Exchange exchange = Importer.read(r);
			ExchangeMapping mapping = new ExchangeMapping(exchange, MappingAction.CREATE);			
			DAOExchange.persist(mapping, user);
		}
		Assert.assertEquals(n+2, DAOStudy.getStudies().size());

	}
*/
	@Test
	public void testExportExchange() throws Exception {
		//Export some data
		SchemaCreator.createExamples(user);
		
		ResultQuery q = new ResultQuery();
		q.setKeywords("LCMS Organ");
		List<Result> results = DAOResult.queryResults(q, user);
		int n = results.size();
		Assert.assertTrue(n>0);

		Exchange exchange = new Exchange("test");
		exchange.addResults(results);
		
		for (Result r : exchange.getResults()) {
			System.out.println("ExchangeTest.testExportExchange() "+r+" "+r.getBiosample()+" "+r.getBiosample().getAttachedSampling());
		}
		for (Study s : exchange.getStudies()) {
			for (NamedSampling ns : s.getNamedSamplings()) {
				for (Sampling ss : ns.getAllSamplings()) {
					System.out.println("ExchangeTest.testExportExchange() "+s+" - "+ns.getId()+"/"+ ns+" - "+ ss.getId() + "/"+ss);
				}
			}
		}
		
		StringWriter writer = new StringWriter();
		Exporter.write(exchange, writer);
		
		
		//Clear
		SchemaCreator.clearExamples(user);
		Assert.assertEquals(0, DAOResult.queryResults(q, user).size());
		
		//Import
		exchange = Importer.read(new StringReader(writer.toString()));
		ExchangeMapping mapping = new ExchangeMapping(exchange);
		DAOExchange.persist(mapping, user);
		
		//ReTest query
		Assert.assertEquals(n, DAOResult.queryResults(q, user).size());
		
		

	}

	
}
