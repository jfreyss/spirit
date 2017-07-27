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

import org.junit.Assert;
import org.junit.Test;

import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.Document.DocumentType;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.util.IOUtils;

/**
 * Test functions from the business package
 * @author freyssj
 *
 */
public class BusinessTest {

	@Test
	public void testPhases() {
		Phase p0 = new Phase("");
		Assert.assertEquals("", p0.getName());
		Assert.assertEquals("", p0.getLabel());

		Phase p1 = new Phase("d-2_5h");
		Assert.assertEquals(-2, p1.getDays());
		Assert.assertEquals(5, p1.getHours());
		Assert.assertEquals(0, p1.getMinutes());
		Assert.assertEquals("", p1.getLabel());
		Assert.assertEquals("d-2_5h", p1.getName());


		Phase p2 = new Phase("d-1");
		Assert.assertEquals(-1, p2.getDays());
		Assert.assertEquals(0, p2.getHours());
		Assert.assertEquals(0, p2.getMinutes());
		Assert.assertEquals("", p2.getLabel());
		Assert.assertEquals("d-1", p2.getName());

		Phase p3 = new Phase(" d5 ");
		Assert.assertEquals(5, p3.getDays());
		Assert.assertEquals(0, p3.getHours());
		Assert.assertEquals(0, p3.getMinutes());
		Assert.assertEquals("", p3.getLabel());
		Assert.assertEquals("d5", p3.getName());

		Phase p4 = new Phase("d5_10h");
		Assert.assertEquals(5, p4.getDays());
		Assert.assertEquals(10, p4.getHours());
		Assert.assertEquals(0, p4.getMinutes());
		Assert.assertEquals("", p4.getLabel());

		Phase p5 = new Phase("d5_10h20 label");
		Assert.assertEquals(5, p5.getDays());
		Assert.assertEquals(10, p5.getHours());
		Assert.assertEquals(20, p5.getMinutes());
		Assert.assertEquals("label", p5.getLabel());

		Phase p6 = new Phase("1. label");
		Assert.assertEquals(1, p6.getDays());
		Assert.assertEquals(0, p6.getHours());
		Assert.assertEquals(0, p6.getMinutes());
		Assert.assertEquals("label", p6.getLabel());


		Phase p7 = new Phase("d0_treatment");
		Assert.assertEquals(0, p7.getDays());
		Assert.assertEquals(0, p7.getMinutes());
		Assert.assertEquals("treatment", p7.getLabel());

		Phase p8 = new Phase("treatment ");
		Assert.assertEquals(0, p8.getMinutes());
		Assert.assertEquals("treatment", p8.getName());

		Phase p9 = new Phase("d5 6 20 treatment ");
		Assert.assertEquals(5, p9.getDays());
		Assert.assertEquals(6, p9.getHours());
		Assert.assertEquals(20, p9.getMinutes());
		Assert.assertEquals("treatment", p9.getLabel());

		Phase p10 = new Phase("d5 -6 20 treatment ");
		Assert.assertEquals(5, p10.getDays());
		Assert.assertEquals(-6, p10.getHours());
		Assert.assertEquals(20, p10.getMinutes());
		Assert.assertEquals("treatment", p10.getLabel());

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


	@Test
	public void testDocumentZip() throws Exception {
		//Add 1st doc
		Document doc = new Document(DocumentType.ZIP);
		doc.addZipEntry(new Document("1", "abc".getBytes()));


		Document retrieved = doc.getZipEntry(0);
		Assert.assertNotNull(retrieved);
		Assert.assertEquals("1", retrieved.getFileName());
		Assert.assertEquals("abc", new String(retrieved.getBytes()));
		Assert.assertNull(doc.getZipEntry(1));


		//Add 2nd doc
		doc.addZipEntry(new Document("2", "def".getBytes()));
		IOUtils.bytesToFile(doc.getBytes(), new File("d:\\tmp\\zip.zip"));


		retrieved = doc.getZipEntry(0);
		Assert.assertNotNull(retrieved);
		Assert.assertEquals("1", retrieved.getFileName());
		Assert.assertEquals("abc", new String(retrieved.getBytes()));
		retrieved = doc.getZipEntry(1);
		Assert.assertNotNull(retrieved);
		Assert.assertEquals("2", retrieved.getFileName());
		Assert.assertEquals("def", new String(retrieved.getBytes()));
		Assert.assertNull(doc.getZipEntry(3));

		//delete 1st entry
		doc.removeZipEntry(0);

		retrieved = doc.getZipEntry(0);
		Assert.assertNotNull(retrieved);
		Assert.assertEquals("2", retrieved.getFileName());
		Assert.assertEquals("def", new String(retrieved.getBytes()));
		Assert.assertNull(doc.getZipEntry(1));

		//delete again
		doc.removeZipEntry(0);
		Assert.assertNull(doc.getZipEntry(0));
	}

	@Test
	public void testTestAtrributes() {
		Assert.assertEquals("mg/ml", TestAttribute.extractUnit("conc. (mg/ml)"));
		Assert.assertEquals("mg/ml", TestAttribute.extractUnit("conc. [mg/ml]"));
		Assert.assertEquals("conc.", TestAttribute.extractNameWithoutUnit("conc. [mg/ml]"));
		Assert.assertEquals("mg/ml", TestAttribute.extractUnit("conc(2). (mg/ml) "));
		Assert.assertEquals("conc(2). suffix", TestAttribute.extractNameWithoutUnit("conc(2). (mg/ml) suffix"));
		Assert.assertEquals("mg/ml", TestAttribute.extractUnit("conc(2). (mg/ml) suffix"));
		Assert.assertEquals("conc(2).", TestAttribute.extractNameWithoutUnit("conc(2). (mg/ml) "));
		Assert.assertEquals(null, TestAttribute.extractUnit("conc. [mg/ml)"));
		Assert.assertEquals("conc. [mg/ml)", TestAttribute.extractNameWithoutUnit("conc. [mg/ml)"));
	}




}
