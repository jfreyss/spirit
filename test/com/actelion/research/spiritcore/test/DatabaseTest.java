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
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.SchemaCreator;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.property.PropertyKey;
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
		DBAdapter.getAdapter().preInit();
		JPAUtil.initFactory(DBAdapter.getAdapter(), "validate");		
	}
	
	@Test
	public void testProperties() throws Exception {
		for(PropertyKey key: PropertyKey.getPropertyKeys(PropertyKey.Tab.STUDY)) {			
			SpiritProperties.getInstance().getValue(key);
		}
				
		for(String metadata: SpiritProperties.getInstance().getValues(PropertyKey.STUDY_METADATA_NAME)) {
			String name = SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_NAME, metadata);
			Assert.assertNotNull(DataType.valueOf(name));
			
			String type = SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_DATATYPE, metadata);
			Assert.assertNotNull(DataType.valueOf(type));
			
			if(DataType.valueOf(type)==DataType.LIST) {
				Assert.assertTrue(SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_PARAMETERS, metadata).length()>0);
			}
		}
	}
	
	@Test
	public void testMigration() throws Exception {
		//Init schema
		MigrationScript.assertLatestVersion();
		
		//Go back
		SpiritProperties.getInstance().setDBVersion("1.9");
		SpiritProperties.getInstance().saveValues();
		Assert.assertEquals("1.9", MigrationScript.getDBVersion());
		
		//Test
		try {
			MigrationScript.assertLatestVersion();
			throw new AssertionError("Version should be wrong");
		} catch(Exception e) {
			MigrationScript.updateDB(DBAdapter.getAdapter().getVendor(), null);
		}
		
		//Validate
		DBAdapter.getAdapter().validate();
		SpiritProperties.getInstance().setDBVersion(MigrationScript.getExpectedDBVersion());
		SpiritProperties.getInstance().saveValues();
		MigrationScript.assertLatestVersion();
	}

	@Test
	public void testRecentChanges() throws Exception {
		DAOStudy.getRecentChanges(5);
	}


}
