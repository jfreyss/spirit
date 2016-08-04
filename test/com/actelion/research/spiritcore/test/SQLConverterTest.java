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

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.services.migration.MigrationScript;
import com.actelion.research.util.SQLConverter;
import com.actelion.research.util.SQLConverter.SQLVendor;

public class SQLConverterTest {
	static Connection hsqlConn;
	@BeforeClass
	public static void initDB() throws Exception {
		hsqlConn = DriverManager.getConnection("jdbc:hsqldb:mem:spirit", "SA", "");;
	}
	
	@Test
	public void testTables() throws Exception {
		String script = "create schema test;\n"
				+ "create table test.revinfo (rev number(10,0), revtstmp number(19,0), primary key (rev));\n"
				+ "create table test.biosample (id number(19) not null, constraint bio_pk primary key (id)) ;"
				+ "create table test.spirit_property (id varchar2(64 char) not null, value varchar2(128 char), constraint spp_pk primary key (id));" 
				+ "create table test.biosample_biosample (biosample_id number(19) not null, linked_id number(19) not null, bm_id number(19) not null);"
				+ "alter table test.revinfo add (userid2 varchar2(20 char));"
				+ "alter table test.biosample_biosample add constraint bb_fk1 foreign key (biosample_id) references test.biosample (id);"
				+ "alter table test.spirit_property modify (id varchar2(80 char));"				
				+ "";
		
		MigrationScript.executeScript(hsqlConn, SQLConverter.convertScript(script, SQLVendor.HSQL), true, null);
		
	}
	
	@Test
	public void testUpdates() throws Exception {
		String script = "create schema test2;\n"
				+ "create table test2.spirit_property (id varchar2(64 char) not null, value varchar2(128 char), constraint spp_pk primary key (id));" 
				+ "insert into test2.spirit_property values('myProp', 'myValue')" 
				+ "update test2.spirit_property set value = 'VALUE=' || replace(replace(value, '\\', '\\\\'), ';', '\\;');\n";
		MigrationScript.executeScript(hsqlConn, SQLConverter.convertScript(script, SQLVendor.HSQL), true, null);
	}
}
