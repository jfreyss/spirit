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

package com.actelion.research.spirit.test;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationFlag;
import com.actelion.research.spiritcore.business.location.LocationQuery;
import com.actelion.research.spiritcore.business.location.LocationType;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.MiscUtils;

/**
 * Basic tests on an empty DB
 * @author freyssj
 *
 */
public class LocationTest extends AbstractSpiritTest {

	/**
	 * Test CRUD operations on locations
	 * @throws Exception
	 */
	@Test
	public void testLocationsCRUD() throws Exception {
		Location l1 = new Location();
		l1.setName("TestLocation");
		l1.setLocationType(LocationType.BUILDING);

		Location l2 = new Location();
		l2.setName("TestLocation");
		l2.setLocationType(LocationType.BUILDING);

		//Create loc
		try {
			DAOLocation.persistLocations(MiscUtils.listOf(l1, l2), user);
			throw new AssertionError("Creation of 2 locations with same name is not allowed");
		} catch (Exception e) {
			JPAUtil.clear();
			l2.setName("OtherLocation");
			DAOLocation.persistLocations(MiscUtils.listOf(l1, l2), user);
		}

		Assert.assertTrue(l1.getId()>0);
		Assert.assertTrue(l2.getId()>0);

		//Update
		l1 = DAOLocation.getLocation(null, "TestLocation");
		Assert.assertTrue(l1.getId()>0);
		l1.setLocationFlag(LocationFlag.GREEN);
		DAOLocation.persistLocations(Collections.singleton(l1), user);

		//Query
		LocationQuery q = new LocationQuery();
		q.setName("Test*");
		Assert.assertEquals(1, DAOLocation.queryLocation(q, user).size());
		Location l3 = DAOLocation.getCompatibleLocation("TestLocation", user);
		Assert.assertEquals(l1, l3);
		Assert.assertEquals(LocationFlag.GREEN, l3.getLocationFlag());

		//Delete location
		DAOLocation.deleteLocations(Collections.singleton(l1), user);

		//Query should return nothing
		q = new LocationQuery();
		q.setName("Test*");
		Assert.assertEquals(0, DAOLocation.queryLocation(q, user).size());
	}

	@Test
	public void testLocationsSaveDuplicate() throws Exception {
		Location l1 = new Location();
		l1.setName("TestLocation");
		l1.setLocationType(LocationType.BUILDING);

		try {
			Location l2 = new Location();
			l2.setName("TestLocation");
			l2.setLocationType(LocationType.BUILDING);
			DAOLocation.persistLocations(MiscUtils.listOf(l1, l2), user);
			throw new Exception("Duplicate Name should fail");
		} catch (Exception e) {
			//OK
		}
	}

	/**
	 * Test that changing/moving a location updates the history of the samples
	 * @throws Exception
	 */
	@Test
	public void testLocationsMove() throws Exception {
		initDemoExamples(user);

		//Create some locations
		Location l1 = new Location();
		l1.setName("TESTLocation");
		l1.setLocationType(LocationType.BUILDING);

		Location l2 = new Location();
		l2.setName("TESTLocation2");
		l2.setLocationType(LocationType.BUILDING);

		Location l3 = new Location();
		l3.setName("TESTRack");
		l3.setLocationType(LocationType.RACK);
		l3.setParent(l1);

		DAOLocation.persistLocations(MiscUtils.listOf(l1, l2, l3), user);


		//Create a sample in l3
		Biosample b = new Biosample(DAOBiotype.getBiotype("Plasma"));
		b.setLocation(l3);
		System.out.println("LocationTest.testLocationsMove() "+b.getSampleId());
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b), user);

		//The history should contain exactly 1 version
		Assert.assertEquals(1, DAORevision.getHistory(b).size());

		//Move l3
		l3.setParent(l2);
		DAOLocation.persistLocations(MiscUtils.listOf(l3), user);

		//Test that the move has been recorded on the biosample
		List<Biosample> history = DAORevision.getHistory(b);
		Assert.assertEquals(2, history.size());
		Assert.assertEquals(l3, history.get(1).getLocation());
		Assert.assertEquals(l1, history.get(1).getLocation().getParent());
		Assert.assertEquals(l3, history.get(0).getLocation());
		Assert.assertEquals(l2, history.get(0).getLocation().getParent());


	}
}