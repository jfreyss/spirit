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

import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.SchemaCreator;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.audit.LogEntry;
import com.actelion.research.spiritcore.business.audit.LogEntry.Action;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.audit.RevisionQuery;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.services.dao.DAOLog;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.services.migration.MigrationScript;

public class DatabaseTest extends AbstractSpiritTest {


	@BeforeClass
	public static void init() throws Exception {
		SchemaCreator.clearExamples(user);
	}

	@Test
	public void validateDB() throws Exception {
		DBAdapter.getInstance().preInit();
		JPAUtil.initFactory(DBAdapter.getInstance(), "validate");
	}

	@Test
	public void testProperties() throws Exception {
		//Test we can retrieve study properties
		for(PropertyKey key: PropertyKey.getPropertyKeys(PropertyKey.Tab.STUDY)) {
			SpiritProperties.getInstance().getValue(key);
		}

		//Test we can retrieve study metadata and default values
		for(String metadata: SpiritProperties.getInstance().getValues(PropertyKey.STUDY_METADATA)) {
			String name = SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_NAME, metadata);
			Assert.assertNotNull(name);

			String type = SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_DATATYPE, metadata);
			Assert.assertNotNull(DataType.valueOf(type));

			if(DataType.valueOf(type)==DataType.LIST) {
				Assert.assertTrue(SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_PARAMETERS, metadata).length()>0);
			}
		}

		//Test we can retrieve the version
		String version = SpiritProperties.getInstance().getValue(PropertyKey.DB_VERSION);
		Assert.assertNotNull(version);

		//Test we can retrieve the rights
		String value = SpiritProperties.getInstance().getValue(PropertyKey.USER_OPENBYDEFAULT);
		Assert.assertEquals(PropertyKey.USER_OPENBYDEFAULT.getDefaultValue(), value);

		//Test we can save
		SpiritProperties.reset();
		SpiritProperties.getInstance().setValue(PropertyKey.USER_OPENBYDEFAULT, "test");
		SpiritProperties.getInstance().saveValues();

		SpiritProperties.reset();
		Assert.assertEquals("test", SpiritProperties.getInstance().getValue(PropertyKey.USER_OPENBYDEFAULT));

		//Test versioning
		RevisionQuery q = new RevisionQuery(null, null, null, null, false, false, false, false, true);
		Revision rev = DAORevision.queryRevisions(q).get(0);
		Assert.assertEquals(1, rev.getSpiritProperties().size());
		Assert.assertEquals("rights.mode", rev.getSpiritProperties().get(0).getKey());
		Assert.assertEquals("test", rev.getSpiritProperties().get(0).getValue());
		Assert.assertEquals("######", rev.getUser());
		System.out.println("DatabaseTest.testProperties() "+rev.getSpiritProperties());

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
	public void testMigration() throws Exception {
		//Init schema
		MigrationScript.assertLatestVersion();

		//Go back
		SpiritProperties.getInstance().setDBVersion("1.9");
		SpiritProperties.getInstance().saveValues();
		Assert.assertEquals("1.9", MigrationScript.getDBVersion());

		//Test version and update
		try {
			MigrationScript.assertLatestVersion();
			throw new AssertionError("Version should be wrong");
		} catch(Exception e) {
			System.out.println("DatabaseTest.testMigration() Update DB");
			MigrationScript.updateDB(DBAdapter.getInstance().getVendor(), null);
			System.out.println("DatabaseTest.testMigration() Updated DB");
		}

		//Validate
		DBAdapter.getInstance().validate();
		SpiritProperties.getInstance().setDBVersion(MigrationScript.getExpectedDBVersion());
		SpiritProperties.getInstance().saveValues();
		MigrationScript.assertLatestVersion();
	}

	@Test
	public void testRecentChanges() throws Exception {
		DAOStudy.getRecentChanges(5);
	}


}
