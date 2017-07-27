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

import org.junit.Assert;
import org.junit.Test;

import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationQuery;
import com.actelion.research.spiritcore.business.location.LocationType;
import com.actelion.research.spiritcore.services.dao.DAOLocation;

/**
 * Basic tests on an empty DB
 * @author freyssj
 *
 */
public class LocationTest extends AbstractSpiritTest {

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

		//Query
		q = new LocationQuery();
		q.setName("Test*");
		Assert.assertEquals(0, DAOLocation.queryLocation(q, user).size());

	}


}
