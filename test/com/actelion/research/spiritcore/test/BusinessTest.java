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

import org.junit.Assert;
import org.junit.Test;

import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;

/**
 * Test functions from the business package
 * @author freyssj
 *
 */
public class BusinessTest {

	@Test
	public void testPhases() {
		Phase p1 = new Phase("d-2_5h");
		Assert.assertEquals(p1.getDays(), -2);
		Assert.assertEquals(p1.getHours(), 5);
		Assert.assertEquals(p1.getMinutes(), 0);
		Assert.assertEquals(p1.getLabel(), "");		
		Assert.assertEquals(p1.getName(), "d-2_5h");		
	
	
		Phase p2 = new Phase("d-1");
		Assert.assertEquals(p2.getDays(), -1);
		Assert.assertEquals(p2.getHours(), 0);
		Assert.assertEquals(p2.getMinutes(), 0);
		Assert.assertEquals(p2.getLabel(), "");		
		Assert.assertEquals(p2.getName(), "d-1");		

		Phase p3 = new Phase(" d5 ");
		Assert.assertEquals(p3.getDays(), 5);
		Assert.assertEquals(p3.getHours(), 0);
		Assert.assertEquals(p3.getMinutes(), 0);
		Assert.assertEquals(p3.getLabel(), "");		
		Assert.assertEquals(p3.getName(), "d5");		

		Phase p4 = new Phase("d5_10h");
		Assert.assertEquals(p4.getDays(), 5);
		Assert.assertEquals(p4.getHours(), 10);
		Assert.assertEquals(p4.getMinutes(), 0);
		Assert.assertEquals(p4.getLabel(), "");		
			
		Phase p5 = new Phase("d5_10h20 label");
		Assert.assertEquals(p5.getDays(), 5);
		Assert.assertEquals(p5.getHours(), 10);
		Assert.assertEquals(p5.getMinutes(), 20);
		Assert.assertEquals(p5.getLabel(), "label");		
	
		Phase p6 = new Phase("1. label");
		Assert.assertEquals(p6.getDays(), 1);
		Assert.assertEquals(p6.getHours(), 0);
		Assert.assertEquals(p6.getMinutes(), 0);
		Assert.assertEquals(p6.getLabel(), "label");		
	}

	@Test
	public void testGroup() {
		Group g1 = new Group("1");
		Assert.assertEquals("1", g1.getShortName());
		Assert.assertEquals("", g1.getNameWithoutShortName());
		
		Group g2 = new Group("1 Name");
		Assert.assertEquals("1", g2.getShortName());
		Assert.assertEquals("Name", g2.getNameWithoutShortName());

		Group g3 = new Group("NAME");
		Assert.assertEquals("NAME", g3.getShortName());
		Assert.assertEquals("", g3.getNameWithoutShortName());
		
		Group g4 = new Group("TOOLONG");
		Assert.assertEquals("TOOL", g4.getShortName());
		Assert.assertEquals("ONG", g4.getNameWithoutShortName());

	}

}
