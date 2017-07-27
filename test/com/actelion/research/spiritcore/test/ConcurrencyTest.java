package com.actelion.research.spiritcore.test;

import java.util.Collections;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.util.MiscUtils;

import junit.framework.AssertionFailedError;

public class ConcurrencyTest extends AbstractSpiritTest {

	private static EmployeeGroup eg1;
	private static Employee emp1a, emp1b;

	@BeforeClass
	public static void initSimpleTestData() throws Exception {

		//Create 2 users
		eg1 = new EmployeeGroup("GroupConcurrency");
		DAOEmployee.persistEmployeeGroups(MiscUtils.listOf(eg1), user);


		emp1a = new Employee("EmpConcurrency1");
		emp1a.setEmployeeGroups(Collections.singleton(eg1));

		emp1b = new Employee("EmpConcurrency2");
		emp1b.setEmployeeGroups(Collections.singleton(eg1));
		DAOEmployee.persistEmployees(MiscUtils.listOf(emp1a, emp1b), user);


		//Create one biotype
		Biotype biotype = new Biotype("Test2");
		biotype.setCategory(BiotypeCategory.PURIFIED);
		DAOBiotype.persistBiotype(biotype, user);
	}

	/**
	 * Test that 2 users cannot overwrite the modifications of each other
	 * @throws Exception
	 */
	@Test
	public void testConcurentEditionBiosample() throws Exception {
		Biotype biotype = DAOBiotype.getBiotype("Test2");
		Assert.assertNotNull(biotype);
		Biosample b = new Biosample(biotype);
		b.setEmployeeGroup(eg1);

		DAOBiosample.persistBiosamples(MiscUtils.listOf(b), user);
		Assert.assertNotNull(b.getUpdDate());
		Assert.assertNotNull(b.getUpdUser());

		Biosample b1 = b.clone();
		Biosample b2 = b.clone();

		//Save a first version of b
		b1.setSampleName("Test1");
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b1), new SpiritUser(emp1a));

		//Save a second version of b
		try {
			b2.setSampleName("Test2");
			DAOBiosample.persistBiosamples(MiscUtils.listOf(b2), new SpiritUser(emp1b));
			throw new AssertionFailedError("Saving by emp1b should not be possible");
		} catch (Exception e) {
			//OK
			System.err.println(e);
		}
	}

	/**
	 * Test that 2 users cannot overwrite the modifications of each other
	 * @throws Exception
	 */
	@Test
	public void testConcurentEditionStudy() throws Exception {
		Study s = new Study();
		s.setAdminUsers(MiscUtils.listOf(emp1a.getUserName(), emp1b.getUserName()));


		DAOStudy.persistStudies(MiscUtils.listOf(s), user);
		Assert.assertTrue(s.getId()>0);
		Assert.assertNotNull(s.getUpdDate());
		Assert.assertNotNull(s.getUpdUser());

		Study s1 = s.clone();
		Study s2 = s.clone();
		Assert.assertNotNull(s1.getUpdDate());
		Assert.assertNotNull(s1.getUpdUser());

		//Save a first version of b
		s1.setTitle("Test1");
		DAOStudy.persistStudies(MiscUtils.listOf(s1), new SpiritUser(emp1a));

		//Save a second version of b
		try {
			s2.setTitle("Test2");
			DAOStudy.persistStudies(MiscUtils.listOf(s2), new SpiritUser(emp1b));
			throw new AssertionFailedError("Saving by emp1b should not be possible");
		} catch (Exception e) {
			//OK
			System.err.println(e);
		}
	}
}
