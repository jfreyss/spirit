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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.HSQLMemoryAdapter;
import com.actelion.research.spiritcore.adapter.PropertyKey;
import com.actelion.research.spiritcore.adapter.SchemaCreator;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.LogEntry;
import com.actelion.research.spiritcore.business.LogEntry.Action;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationQuery;
import com.actelion.research.spiritcore.business.location.LocationType;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAODocument;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAOLog;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;

/**
 * Basic tests on an empty DB
 * @author freyssj
 *
 */
public class SimpleDBTest {

	private static SpiritUser user;
	
	@BeforeClass
	public static void initDB() throws Exception {
		//Init user
		user = SpiritUser.getFakeAdmin();
		
		//Init DB
		DBAdapter.setAdapter(new HSQLMemoryAdapter());
		SchemaCreator.clearExamples(user);
	}

	@Test
	public void testBiosamples() throws Exception {
		//Persist biotype
		Biotype biotype = new Biotype();
		biotype.setCategory(BiotypeCategory.PURIFIED);
		biotype.setName("BioTest");
		biotype.setPrefix("test-");
		biotype.getMetadata().add(new BiotypeMetadata("meta1", DataType.ALPHA));
		biotype.getMetadata().add(new BiotypeMetadata("meta2", DataType.NUMBER));
		biotype.getMetadata().add(new BiotypeMetadata("meta3", DataType.D_FILE));
		DAOBiotype.persistBiotype(biotype, user);
		
		//Persist biosamples
		Biosample b1 = new Biosample(biotype);
		Biosample b2 = new Biosample(biotype);
		
		List<Biosample> list = new ArrayList<>();
		list.add(b1);
		list.add(b2);

		DAOBiosample.persistBiosamples(list, user);
		
		//Update
		b1.setMetadata("meta1", "ALPHA1 ALPHA3");
		b2.setMetadata("meta1", "ALPHA1 ALPHA2");
		b1.getMetadata("meta3").setLinkedDocument(new Document(new File("c:/DBAR_Ver.txt")));
		DAOBiosample.persistBiosamples(list, user);

		JPAUtil.clear();
		
		
		
		//Query
		BiosampleQuery q = new BiosampleQuery();
		q.setKeywords("ALPHA1");
		Assert.assertEquals(2, DAOBiosample.queryBiosamples(q, user).size());
		
		q.setKeywords("ALPHA3 ALPHA1");
		Assert.assertEquals(1, DAOBiosample.queryBiosamples(q, user).size());
		Biosample b = DAOBiosample.queryBiosamples(q, user).get(0);
		Assert.assertEquals("DBAR_Ver.txt", b.getMetadata("meta3").getLinkedDocument().getFileName());
		Assert.assertEquals("DBAR_Ver.txt", b.getMetadata("meta3").getLinkedDocument().getFileName());
		Assert.assertNotNull(b.getMetadata("meta3").getLinkedDocument().getBytes());
		Assert.assertNotNull(DAODocument.getDocument(b.getMetadata("meta3").getLinkedDocument().getId()).getBytes());

		q.setKeywords("ALPHA2 or ALPHA3");
		Assert.assertEquals(2, DAOBiosample.queryBiosamples(q, user).size());

		q.setKeywords("ALPHA2 ALPHA3");
		Assert.assertEquals(0, DAOBiosample.queryBiosamples(q, user).size());

		//Delete biotype (not allowed)
		try {
			DAOBiotype.deleteBiotype(biotype, user);
			throw new AssertionError("Deletion biotype should not be allowed");
		} catch(Exception e) {
		}
		
		//Delete biosamples
		DAOBiosample.deleteBiosamples(list, user);

		//Delete biotype
		DAOBiotype.deleteBiotype(biotype, user);
		
		
		//Retest query
		q.setKeywords("ALPHA1");
		Assert.assertEquals(0, DAOBiosample.queryBiosamples(q, user).size());

	}
	
	@Test
	public void testLocations() throws Exception {
		Location l = new Location();
		l.setName("TestLocation");
		l.setLocationType(LocationType.BUILDING);
		
		//Persist loc
		DAOLocation.persistLocations(Collections.singleton(l), user);
		
		
		//Query
		LocationQuery q = new LocationQuery();
		q.setName("Test*");
		Assert.assertEquals(1, DAOLocation.queryLocation(q, user).size());
		
		//Delete location
		DAOLocation.deleteLocations(Collections.singleton(l), user);


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
	public void testEmployeeGroups() throws Exception {
		Assert.assertEquals(1, DAOEmployee.getEmployeeGroups().size());
		Assert.assertEquals("Group", DAOEmployee.getEmployeeGroups().get(0).getName());
		
		EmployeeGroup group1 = new EmployeeGroup("Root");
		DAOEmployee.persistEmployeeGroups(Collections.singleton(group1), user);
		Assert.assertTrue(group1.getId()>0);

		EmployeeGroup group2 = new EmployeeGroup("Child");
		group2.setParent(group1);
		DAOEmployee.persistEmployeeGroups(Collections.singleton(group2), user);
		Assert.assertEquals(3, DAOEmployee.getEmployeeGroups().size());
		
		DAOEmployee.removeEmployeeGroup(group2, user);
		Assert.assertEquals(2, DAOEmployee.getEmployeeGroups().size());

	}
	
	@Test
	public void testEmployees() throws Exception {
		int n = DAOEmployee.getEmployees().size();
		
		//Create user
		Employee emp = new Employee("test");
		DAOEmployee.persistEmployees(Collections.singleton(emp), user);
		Assert.assertEquals(n+1, DAOEmployee.getEmployees().size());
		
		//update user
		emp = DAOEmployee.getEmployee("test");
		emp.setPassword(DBAdapter.getAdapter().encryptPassword("password".toCharArray()));
		Assert.assertNotNull(emp);
		
		emp.setManager(DAOEmployee.getEmployee("admin"));
		emp.getEmployeeGroups().add(DAOEmployee.getEmployeeGroup("Group"));
		DAOEmployee.persistEmployees(Collections.singleton(emp), user);
		
		emp = DAOEmployee.getEmployee("test");
		Assert.assertEquals(1, emp.getEmployeeGroups().size());
		Assert.assertTrue(emp.getManager()!=null);
		
		SpiritUser u2 = DAOSpiritUser.loadUser("test");
		Assert.assertNotNull(u2);
		DAOSpiritUser.authenticateUser(emp.getUserName(), "password".toCharArray());

		//Delete user
		DAOEmployee.removeEmployee(emp, user);
		emp = DAOEmployee.getEmployee("test");
		Assert.assertNull(emp);
			
	}

		
	@Test
	public void testLog() {
		List<LogEntry> entries = DAOLog.getLogs(null, null, -1);
		
		Assert.assertEquals(0, entries.size());
		DAOLog.log("test", Action.LOGON_SUCCESS);

		entries = DAOLog.getLogs(null, null, -1);
		Assert.assertEquals(1, entries.size());
		
		entries = DAOLog.getLogs("test", null, 1);
		Assert.assertEquals(1, entries.size());

		entries = DAOLog.getLogs("test2", null, 1);
		Assert.assertEquals(0, entries.size());

	}
	
	@Test
	public void testProperty() throws Exception {
		
		String version = SpiritProperties.getInstance().getValue(PropertyKey.DB_VERSION);
		Assert.assertNotNull(version);
		
		String value = SpiritProperties.getInstance().getValue(PropertyKey.RIGHTS_MODE);
		Assert.assertEquals(PropertyKey.RIGHTS_MODE.getDefaultValue(), value);
		
		SpiritProperties.clear();
		SpiritProperties.getInstance().setValue(PropertyKey.RIGHTS_MODE, "test");
		SpiritProperties.getInstance().saveValues();
		
		SpiritProperties.clear();
		Assert.assertEquals("test", SpiritProperties.getInstance().getValue(PropertyKey.RIGHTS_MODE));
		

	}
}
