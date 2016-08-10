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

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.HSQLMemoryAdapter;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.LocationQuery;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;

/**
 * Tests DAOs
 * @author freyssj
 *
 */
public class DAOBusinessTest {

	private static SpiritUser user;
	
	@BeforeClass
	public static void initDB() throws Exception {
		//Set properties
		System.setProperty("show_sql", "true");
		
		//Init user
		user = SpiritUser.getFakeAdmin();
		
		//Init DB
		DBAdapter.setAdapter(new HSQLMemoryAdapter());
		
	}

	@Test
	public void testEmployeeGroups() throws Exception {
		Assert.assertEquals(1, DAOEmployee.getEmployeeGroups().size());
		Assert.assertEquals("Group", DAOEmployee.getEmployeeGroups().get(0).getName());
		
		EmployeeGroup group1 = new EmployeeGroup("Root");
		DAOEmployee.persistEmployeeGroup(group1, user);
		Assert.assertTrue(group1.getId()>0);

		EmployeeGroup group2 = new EmployeeGroup("Child");
		group2.setParent(group1);
		DAOEmployee.persistEmployeeGroup(group2, user);
		Assert.assertEquals(3, DAOEmployee.getEmployeeGroups().size());
		Assert.assertEquals(2, DAOEmployee.getEmployeeGroups("Root").size());
	}
	
	@Test
	public void testEmployees() throws Exception {
		Assert.assertEquals(1, DAOEmployee.getEmployees().size());
		Assert.assertEquals("admin", DAOEmployee.getEmployees().get(0).getUserName());
		
		Employee emp = new Employee("test");
		DAOEmployee.persistEmployee(emp, user);
		Assert.assertEquals(2, DAOEmployee.getEmployees().size());
		
	}

	@Test
	public void testStudy() throws Exception {
		int n = DAOStudy.getStudies().size();
		
		
		//Persist new Study
		Study study = new Study();
		study.setTitle("My New Study");
		DAOStudy.persistStudies(Collections.singleton(study), user);
		
		Assert.assertEquals(n+1, DAOStudy.getStudies().size());
		Assert.assertEquals("My New Study", DAOStudy.getStudies().get(0).getTitle());
		
		//Update study
		study.setNotes("some notes");
		DAOStudy.persistStudies(Collections.singleton(study), user);
		Assert.assertEquals(n+1, DAOStudy.getStudies().size());
		Assert.assertEquals("some notes", DAOStudy.getStudies().get(0).getNotes());
		
		//Query
		StudyQuery q = new StudyQuery();
		q.setKeywords("new study");
		List<Study> studies = DAOStudy.queryStudies(q, user);
		Assert.assertEquals(1, studies.size());
		
		q.setKeywords("rubbish keywords");
		studies = DAOStudy.queryStudies(q, user);
		Assert.assertEquals(0, studies.size());
		
		//Delete
		DAOStudy.deleteStudy(study, user);
		Assert.assertEquals(n, DAOStudy.getStudies().size());
	}
	
	@Test
	public void testBiosamples() throws Exception {
		
		//Query
		BiosampleQuery q = new BiosampleQuery();
		q.setKeywords("test");
		DAOBiosample.queryBiosamples(q, user);
	}
	
	@Test
	public void testLocations() throws Exception {
		//Query
		LocationQuery q = new LocationQuery();
		q.setName("test");
		DAOLocation.queryLocation(q, user);

	}
	
	@Test
	public void testResults() throws Exception {
		//Query
		ResultQuery q = new ResultQuery();
		q.setKeywords("test");
		DAOResult.queryResults(q, user);
	}
}
