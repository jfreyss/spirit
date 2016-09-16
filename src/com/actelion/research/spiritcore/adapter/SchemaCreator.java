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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAOExchange;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping;
import com.actelion.research.spiritcore.services.exchange.Importer;
import com.actelion.research.spiritcore.services.migration.MigrationScript;

public class SchemaCreator {

	private static final String CREATE_BEFORE = "create schema spirit;";
	private static final String CREATE_AFTER = "" 
			+ "create index aud_1_idx on spirit.assay_result_aud (biosample_id);\n "
			+ "create index aud_2_idx on spirit.assay_result_value_aud (assay_result_id);\n "
			+ "create index aud_3_idx on spirit.biosample_aud (attachedstudy_id);\n "
			+ "create index aud_4_idx on spirit.biosample_aud (parent_id);\n "
			+ "create index aud_5_idx on spirit.biosample_action_aud (biosample_id);\n"
			+ "";
	
	public static void recreateTables(DBAdapter adapter) throws Exception {
		LoggerFactory.getLogger(HSQLFileAdapter.class).warn("Creating tables");
		
		try {
			adapter.executeScripts(CREATE_BEFORE, false);
			
			//Use JPA system to recreate the tables
			JPAUtil.initFactory(adapter, "update");
						
			//Create one group
			SpiritUser admin = SpiritUser.getFakeAdmin();
			EmployeeGroup group = new EmployeeGroup("Group");
			DAOEmployee.persistEmployeeGroups(Collections.singleton(group), admin);

			Employee employee = new Employee("admin");
			employee.getEmployeeGroups().add(group);
			employee.setRoles(Collections.singleton("admin"));
			DAOEmployee.persistEmployees(Collections.singleton(employee), admin);			
			
			//The version is now the latest: update the version
			String version = MigrationScript.getExpectedDBVersion();
			SpiritProperties.getInstance().setDBVersion(version);
			SpiritProperties.getInstance().saveValues();
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
	
	public static void clearExamples(SpiritUser user) throws Exception {
		List<Study> exampleStudies = DAOStudy.queryStudies(StudyQuery.createForState("EXAMPLE"),  user);
		EntityManager session = JPAUtil.getManager();
		try {
			session.getTransaction().begin();
			DAOStudy.deleteStudies(session, exampleStudies, true, user);
			LoggerFactory.getLogger(SchemaCreator.class).info("examples deleted: "+exampleStudies);						
			session.getTransaction().commit();
		} catch(Exception e) {
			if(session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			throw e;
		} 
	}

	public static Exchange createExamples(SpiritUser user) throws Exception {
		try(InputStream is = SchemaCreator.class.getResourceAsStream("demo.spirit")) {
			if(is==null) {
				throw new Exception("The system could not find the default configuration. The system is therefore empty");
			}
			Exchange exchange = Importer.read(new InputStreamReader(is));			
			ExchangeMapping mapping = new ExchangeMapping(exchange);
			DAOExchange.persist(mapping, user);
			LoggerFactory.getLogger(SchemaCreator.class).info("examples persisted");
			return exchange;
		} catch(Exception e) {
			LoggerFactory.getLogger(SchemaCreator.class).error("Could not persist examples: replace old examples", e);
			throw e;	
		}
	}
}
