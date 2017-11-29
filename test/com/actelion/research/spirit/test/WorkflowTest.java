package com.actelion.research.spirit.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.services.helper.WorkflowHelper;
import com.actelion.research.spiritcore.util.MiscUtils;


public class WorkflowTest extends AbstractSpiritTest {

	@BeforeClass
	public static void init() throws Exception {
		ExchangeTest.initDemoExamples(user);
	}

	@Test
	public void testWorkflow() throws Exception {
		//Define roles
		SpiritProperties.getInstance().setValue(PropertyKey.USER_ROLES, "FM, QA, scientist");

		List<Employee> employees = new ArrayList<>();
		Employee fm = new Employee("FM", Collections.singleton("FM"));
		Employee qa = new Employee("QA", Collections.singleton("QA"));
		Employee scientist = new Employee("scientist", Collections.singleton("scientist"));
		employees.add(fm);
		employees.add(qa);
		employees.add(scientist);
		DAOEmployee.persistEmployees(employees, user);

		//Define workflow
		SpiritProperties.getInstance().setValue(PropertyKey.STUDY_STATES, "proposed, approved, ongoing, stopped, sealed");
		SpiritProperties.getInstance().setValue(PropertyKey.STUDY_DEFAULTSTATE, "proposed");
		SpiritProperties.getInstance().setValue(PropertyKey.STUDY_STATES_FROM, "proposed", "approved");
		SpiritProperties.getInstance().setValue(PropertyKey.STUDY_STATES_FROM, "approved", "proposed, ongoing");
		SpiritProperties.getInstance().setValue(PropertyKey.STUDY_STATES_FROM, "ongoing", "approved");
		SpiritProperties.getInstance().setValue(PropertyKey.STUDY_STATES_FROM, "stopped", "ongoing");
		SpiritProperties.getInstance().setValue(PropertyKey.STUDY_STATES_FROM, "sealed", "ongoing");
		SpiritProperties.getInstance().setValue(PropertyKey.STUDY_STATES_PROMOTERS, "proposed", "");
		SpiritProperties.getInstance().setValue(PropertyKey.STUDY_STATES_PROMOTERS, "approved", "FM");
		SpiritProperties.getInstance().setValue(PropertyKey.STUDY_STATES_PROMOTERS, "ongoing", "FM");
		SpiritProperties.getInstance().setValue(PropertyKey.STUDY_STATES_PROMOTERS, "stopped", "FM, scientist");
		SpiritProperties.getInstance().setValue(PropertyKey.STUDY_STATES_PROMOTERS, "sealed", "FM, scientist");
		SpiritProperties.getInstance().saveValues();

		//Test descriptions
		WorkflowHelper.getStateDescriptions();
		WorkflowHelper.getWorkflowDescription("proposed");


		//Test
		Assert.assertEquals(5, SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES).length);
		Assert.assertEquals("approved", SpiritProperties.getInstance().getValue(PropertyKey.STUDY_STATES_FROM, "proposed"));

		//Define new study
		Study s = new Study();
		s.setState(SpiritProperties.getInstance().getValue(PropertyKey.STUDY_DEFAULTSTATE));
		DAOStudy.persistStudies(Collections.singleton(s), user);

		Assert.assertEquals("proposed", MiscUtils.flatten(WorkflowHelper.getNextStates(s, DAOSpiritUser.loadUser("scientist"))));
		Assert.assertEquals("proposed, approved", MiscUtils.flatten(WorkflowHelper.getNextStates(s, DAOSpiritUser.loadUser("FM"))));

		//promote
		s.setState("approved");
		DAOStudy.persistStudies(Collections.singleton(s), user);

		s.setState("ongoing");
		DAOStudy.persistStudies(Collections.singleton(s), user);

		Assert.assertEquals("ongoing", MiscUtils.flatten(WorkflowHelper.getNextStates(s, DAOSpiritUser.loadUser("QA"))));
		Assert.assertEquals("ongoing, stopped, sealed", MiscUtils.flatten(WorkflowHelper.getNextStates(s, DAOSpiritUser.loadUser("scientist"))));
		Assert.assertEquals("approved, ongoing, stopped, sealed", MiscUtils.flatten(WorkflowHelper.getNextStates(s, DAOSpiritUser.loadUser("FM"))));



	}

}
