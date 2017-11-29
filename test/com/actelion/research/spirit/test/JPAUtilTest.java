package com.actelion.research.spirit.test;

import java.util.Collections;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.services.dao.DAOBarcode;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAODocument;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAOExchange;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAOLog;
import com.actelion.research.spiritcore.services.dao.DAONamedSampling;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.helper.BiosampleCreationHelper;
import com.actelion.research.spiritcore.services.helper.ExpressionHelper;
import com.actelion.research.spiritcore.services.helper.WorkflowHelper;


public class JPAUtilTest extends AbstractSpiritTest {

	@BeforeClass
	public static void init() throws Exception {
		ExchangeTest.initDemoExamples(user);
	}


	/**
	 * Instantiate all utility constructor to check if we get full coverage
	 * @throws Exception
	 */
	@Test
	public void testUtilityConstructors() throws Exception {
		new DAOBarcode();
		new DAOBiosample();
		new DAOBiotype();
		new DAODocument();
		new DAOEmployee();
		new DAOExchange();
		new DAOLocation();
		new DAOLog();
		new DAONamedSampling();
		new DAOResult();
		new DAORevision();
		new DAOSpiritUser();
		new DAOStudy();
		new DAOTest();

		new ExpressionHelper();
		new BiosampleCreationHelper();
		new WorkflowHelper();

		new JPAUtil();


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
		JPAUtil.clearAll();
		b = DAOBiosample.getBiosample("Test");
		Assert.assertEquals("other comments", b.getComments());

	}

}
