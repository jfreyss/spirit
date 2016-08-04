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

package com.actelion.research.spiritcore.adapter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.ConfigProperties;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.migration.MigrationScript;

public class SchemaCreator {

	private static final String CREATE_BEFORE = "CREATE SCHEMA SPIRIT;";
	private static final String CREATE_AFTER = "" 
			+ "CREATE INDEX AUD_1_IDX ON SPIRIT.ASSAY_RESULT_AUD (BIOSAMPLE_ID);\n "
			+ "CREATE INDEX AUD_2_IDX ON SPIRIT.ASSAY_RESULT_VALUE_AUD (ASSAY_RESULT_ID);\n "
			+ "CREATE INDEX AUD_3_IDX ON SPIRIT.BIOSAMPLE_AUD (ATTACHEDSTUDY_ID);\n "
			+ "CREATE INDEX AUD_4_IDX ON SPIRIT.BIOSAMPLE_AUD (PARENT_ID);\n "
			+ "CREATE INDEX AUD_5_IDX ON SPIRIT.BIOSAMPLE_ACTION_AUD (BIOSAMPLE_ID);\n"
			+ "";
	
	public static void recreateTables(DBAdapter adapter) throws Exception {
		LoggerFactory.getLogger(HSQLFileAdapter.class).warn("Creating tables");
		
		try {
			adapter.executeScripts(CREATE_BEFORE, false);
			
			//Use JPA system to recreate the tables
			JPAUtil.initFactory(adapter, "update");
			
			
			//Create one group
			EmployeeGroup group = new EmployeeGroup("Group");
			DAOEmployee.persistEmployeeGroup(group, SpiritUser.getFakeAdmin());

			Employee employee = new Employee("admin");
			employee.getEmployeeGroups().add(group);
			employee.setRoles(Collections.singleton("admin"));
			DAOEmployee.persistEmployee(employee, SpiritUser.getFakeAdmin());
			
			
			//The version is now the latest: update the version
			String version = MigrationScript.getExpectedDBVersion();
			ConfigProperties.getInstance().setDBVersion(version);
			ConfigProperties.getInstance().saveValues();
			
			adapter.executeScripts(CREATE_AFTER, false);
			LoggerFactory.getLogger(HSQLFileAdapter.class).debug("DB UPDATED");
		} catch(Exception e2) {
			LoggerFactory.getLogger(HSQLFileAdapter.class).error("***Spirit.LocalFactory: Could not update DB", e2);
			throw e2;
		}
	}
	
	
	public static void displayTables(DBAdapter adapter) {
		try {
			Connection conn = adapter.getConnection();			
			{
				Statement stmt = conn.createStatement();
				ResultSet rs =  stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.SYSTEM_CROSSREFERENCE");
				System.out.println("### FOREIGN KEYS ###");
				boolean first = true;
				int n = 0;
				while(rs.next()) {
					if(first) {
						first = false;
						n = rs.getMetaData().getColumnCount();
						for (int i = 1; i<=n; i++) {
							System.out.print(rs.getMetaData().getColumnName(i)+"\t");
						}
					}
											
					for (int i = 1; i<=n; i++) {
						System.out.print(rs.getString(i)+"\t");
					}
					System.out.println();
				}
				rs.close();
				stmt.close();
			}
			{
				Statement stmt = conn.createStatement();
				ResultSet rs =  stmt.executeQuery("SELECT TABLE_SCHEM, TABLE_NAME, COLUMN_NAME, TYPE_NAME, COLUMN_SIZE, DECIMAL_DIGITS, IS_NULLABLE FROM INFORMATION_SCHEMA.SYSTEM_COLUMNS WHERE TABLE_SCHEM NOT LIKE 'INFO_%' ");
				System.out.println("");
				System.out.println("### COLUMNS ###");
				boolean first = true;
				int n = 0;
				while(rs.next()) {
					if(first) {
						first = false;
						n = rs.getMetaData().getColumnCount();
						for (int i = 1; i<=n; i++) {
							System.out.print(rs.getMetaData().getColumnName(i)+"\t");
						}
					}
											
					for (int i = 1; i<=n; i++) {
						System.out.print(rs.getString(i)+"\t");
					}
					System.out.println();
				}
				rs.close();
				stmt.close();
			}

		} catch(Exception e) {
			
			e.printStackTrace();
		}
		
	}

}
