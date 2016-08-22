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

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.HSQLMemoryAdapter;
import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOExchange;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping.MappingAction;
import com.actelion.research.spiritcore.services.exchange.Importer;

public class DAOExchangeTest {

	private static SpiritUser user;
	
	@BeforeClass
	public static void initDB() throws Exception {
		System.out.println("DAOExchangeTest.initDB()");
		//Init user
		user = SpiritUser.getFakeAdmin();
		
		//Init DB and test emptynes
		JPAUtil.close();
		DBAdapter.setAdapter(new HSQLMemoryAdapter());
	}
	
	
	@Test
	public void testImportDemo() throws Exception {
		JPAUtil.close();
		DBAdapter.setAdapter(new HSQLMemoryAdapter());
		Assert.assertFalse(JPAUtil.getManager().getTransaction().isActive());
		
		int n = DAOStudy.getStudies().size();
		{
			try (InputStream is = DBAdapter.class.getResourceAsStream("demo.spirit")) {
				if(is==null) {
					throw new Exception("The system could not find the default configuration. The system is therefore empty");
				}
				Exchange exchange = Importer.read(new InputStreamReader(is));
				Assert.assertTrue("The demo file has only " + exchange.getStudies().size() + " studies", exchange.getStudies().size()>=2);
	
				Study s = exchange.getStudies().iterator().next();
				Assert.assertEquals(2, s.getNamedSamplings().size());
				Assert.assertEquals(2, s.getSamplings("Blood Sampling", "Blood: EDTA; Alive").size());
				
				Biotype blood = Biotype.mapName(exchange.getBiotypes()).get("Blood");
				Assert.assertNotNull(blood);
				BiotypeMetadata bloodType = blood.getMetadata("Type");
				Assert.assertNotNull(bloodType);
				
				ExchangeMapping mapping = new ExchangeMapping(exchange);
				DAOExchange.persist(mapping, user);
			}		
			
	
			JPAUtil.clear();
			
			
			//Check biotypes
			Biotype blood = DAOBiotype.getBiotype("Blood");
			Assert.assertNotNull(blood);
			BiotypeMetadata bloodType = blood.getMetadata("Type");
			System.out.println("DAOExchangeTest.testImportDemo() after bloodType.getId()= "+bloodType.getId());
			Assert.assertNotNull(bloodType);	
			
			//Check the studies
			Assert.assertTrue(DAOStudy.getStudies().size()>=n+2);
			int nref1 = DAOStudy.getStudies().size()-1;
			int nref2 = DAOStudy.getStudies().size()-2;
			Assert.assertEquals("IVV2016-1", DAOStudy.getStudies().get(nref1).getIvv());
			Assert.assertEquals("IVV2016-2", DAOStudy.getStudies().get(nref2).getIvv());
			Study s2016_1 = DAOStudy.getStudies().get(nref1);
			Assert.assertEquals(2, s2016_1.getNamedSamplings().size());
			
			List<Sampling> samplings = s2016_1.getSamplings("Blood Sampling", "Blood: EDTA; Alive");
			Assert.assertEquals(2, samplings.size());
			Assert.assertTrue(samplings.get(0).getSamples().size()>0);
			Assert.assertTrue(samplings.get(1).getSamples().size()>0);
			Assert.assertEquals(samplings.get(0).getSamples().size(), samplings.get(1).getSamples().size());

			
			//
			//Check the biosamples
			BiosampleQuery q1 = new BiosampleQuery();
			q1.setStudyIds(DAOStudy.getStudies().get(nref1).getStudyId());
			List<Biosample> biosamples = DAOBiosample.queryBiosamples(q1, user);
			Assert.assertTrue(biosamples.size()>0);
			for (Biosample b : biosamples) {
				System.out.println("DAOExchangeTest.testImportDemo() "+b+">"+ b.getMetadataMap());
				if(b.getParent()==null) {
					Assert.assertEquals("Rat", b.getMetadata("Type").getValue());
					Assert.assertTrue(b.getAttachedSampling()==null);
				} else {
					Assert.assertTrue(b.getAttachedSampling()!=null);
				}
			}
			BiosampleQuery q2 = new BiosampleQuery();
			q2.setStudyIds(s2016_1.getStudyId());
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
			qr.setStudyIds(DAOStudy.getStudies().get(nref1).getStudyId());
			List<Result> results = DAOResult.queryResults(qr, user);
			Assert.assertTrue(results.size()>0);
			for (Result r : results) {			
				Assert.assertTrue(r.getOutputResultValuesAsString().length()>0);
			}
		}
		
		{
			//Check if we do the import it twice (->map, without inserts)
			try (InputStream is = DBAdapter.class.getResourceAsStream("demo.spirit")) {
				if(is==null) {
					throw new Exception("The system could not find the default configuration. The system is therefore empty");
				}
				Exchange exchange = Importer.read(new InputStreamReader(is));
				ExchangeMapping mapping = new ExchangeMapping(exchange);
				DAOExchange.persist(mapping, user);
			}		
			Assert.assertTrue(DAOStudy.getStudies().size()>=n+2);
		}

	}

	
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

		//3rd import: do nothing
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


	
}
