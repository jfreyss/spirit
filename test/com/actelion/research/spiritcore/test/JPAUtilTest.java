package com.actelion.research.spiritcore.test;

import java.util.Collections;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.JPAUtil;


public class JPAUtilTest extends AbstractSpiritTest {
	
	@BeforeClass
	public static void init() throws Exception {
		ExchangeTest.initDemoExamples(user);		
	}
	
	@Test
	public void testRefresh() throws Exception {
		
		//create sample
		Biosample b = new Biosample(DAOBiotype.getBiotype("Animal"));
		b.setSampleId("Test");
		b.setComments("initial comments");
		DAOBiosample.persistBiosamples(Collections.singletonList(b), user);
		
		//Update in an other session
		{
			EntityManager em = JPAUtil.createManager();
			Biosample b2 = em.find(Biosample.class, b.getId());
			b2.setComments("other comments");
			
			em.getTransaction().begin();
			DAOBiosample.persistBiosamples(em, Collections.singletonList(b2), user);
			em.getTransaction().commit();
			em.close();
		}
		
		//reload in general session -> reload from cache
		b = DAOBiosample.getBiosample("Test");
		Assert.assertEquals("initial comments", b.getComments());
		
		
		//refresh -> reload from db
		JPAUtil.clear();
		b = DAOBiosample.getBiosample("Test");
		Assert.assertEquals("other comments", b.getComments());
		
	}

}
