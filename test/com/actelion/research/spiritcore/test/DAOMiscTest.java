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

import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.HSQLMemoryAdapter;
import com.actelion.research.spiritcore.adapter.PropertyKey;
import com.actelion.research.spiritcore.business.LogEntry;
import com.actelion.research.spiritcore.business.LogEntry.Action;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.ConfigProperties;
import com.actelion.research.spiritcore.services.dao.DAOLog;
import com.actelion.research.spiritcore.services.dao.JPAUtil;

public class DAOMiscTest {
	
	
	@SuppressWarnings("unused")
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
	public void validateDB() throws Exception {
		JPAUtil.close();
		System.out.println("DAOMiscTest.validateDB()");
		DBAdapter.getAdapter().preInit();
		System.out.println("DAOMiscTest.validateDB2()");
		JPAUtil.initFactory(DBAdapter.getAdapter(), "validate");
		System.out.println("DAOMiscTest.validateDB3()");
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
		
		String version = ConfigProperties.getInstance().getValue(PropertyKey.DB_VERSION);
		Assert.assertNotNull(version);
		
		String value = ConfigProperties.getInstance().getValue(PropertyKey.RIGHTS_MODE);
		Assert.assertEquals(PropertyKey.RIGHTS_MODE.getDefaultValue(), value);
		
		ConfigProperties.clear();
		ConfigProperties.getInstance().setValue(PropertyKey.RIGHTS_MODE, "test");
		ConfigProperties.getInstance().saveValues();
		
		ConfigProperties.clear();
		Assert.assertEquals("test", ConfigProperties.getInstance().getValue(PropertyKey.RIGHTS_MODE));
		

	}
}
