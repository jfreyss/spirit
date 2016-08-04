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

package com.actelion.research.spiritcore.services.dao;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.adapter.ConfigProperty;
import com.actelion.research.spiritcore.adapter.PropertyKey;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.MiscUtils;

public class ConfigProperties {

	private static ConfigProperties instance = null;
	private Map<String, String> properties;
	private Boolean hasWorkflow; 
	
	private ConfigProperties() {
		properties = getProperties();
	}
	
	public static ConfigProperties getInstance() {
		if(instance==null) {
			synchronized (ConfigProperties.class) {
				if(instance==null) {
					instance = new ConfigProperties();
				}
			}
		}
		return instance;
	}
	public static void clear() {
		instance = null;
	}
	/**
	 * Gets the value of a simple property or the default value
	 * @param p
	 * @return
	 */
	public String getValue(PropertyKey p) {
		String v = properties.get(p.getKey());
		if(v==null) v = p.getDefaultValue();
		return v;
	}
	
	/**
	 * Gets the value of a simple property or the default value
	 * The result is splitted by ','
	 */
	public String[] getValues(PropertyKey p) {
		return MiscUtils.split(getValue(p), ",");
	}
	public boolean isChecked(PropertyKey p) {
		return "true".equals(getValue(p));
	}


	/**
	 * Gets the value of a nested property or the default value. One must give the values of all the parent properties
	 */
	public String getValue(PropertyKey p, String... nestedValues) {
		LinkedList<PropertyKey> list = new LinkedList<>();
		PropertyKey tmp = p.getParentProperty();
		while(tmp!=null) {
			list.addFirst(tmp);
			tmp = tmp.getParentProperty();
		}
		
		assert list.size()==nestedValues.length;		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			sb.append(list.get(i).getKey());
			sb.append("." + nestedValues[i]);
		}
		sb.append("." + p.getKey());
		String v = properties.get(sb.toString());
		if(v==null) v = p.getDefaultValue(nestedValues);
		return v;
	}
	
	
	/**
	 * Gets the value of a nested property or the default value. One must give the values of all the parent properties
	 * The result is splitted by ','
	 */
	public String[] getValues(PropertyKey p, String... nestedValues) {
		return MiscUtils.split(getValue(p, nestedValues), ",");
	}

	public boolean isChecked(PropertyKey p, String... nestedValues) {
		return "true".equals(getValue(p, nestedValues));
	}


	public void setValue(PropertyKey p, String v) {
		properties.put(p.getKey(), v);
	}
	
	public Map<String, String> getValues() {
		LoggerFactory.getLogger(getClass()).debug("properties="+properties);
		return properties;
	}
	
	public void setValues(Map<String, String> map) {
		assert map.containsKey(PropertyKey.DB_VERSION.getKey());
		
		this.properties = new HashMap<>();
		this.properties.putAll(map);
		LoggerFactory.getLogger(getClass()).debug("properties="+properties);
	}
	
	public void saveValues() {
		saveProperties(properties);
		
		//Force Reload
		instance = null;
	}
		
	//////////////////////////////////////////////////////////////////////////////////////
	private static Map<String, String> getProperties() {
		Map<String, String> keyValuePairs = new HashMap<>();
		EntityManager em = JPAUtil.getManager();
		for(ConfigProperty p: (List<ConfigProperty>) em.createQuery("from ConfigProperty").getResultList()) {
			if(p==null) continue;
			keyValuePairs.put(p.getKey(), p.getValue());			
		}
		LoggerFactory.getLogger(ConfigProperties.class).debug("properties="+keyValuePairs);
		return keyValuePairs;
	}	
	/**
	 * Save the given properties. Always open a new entitymanager, so this function can even be called in any context 
	 * @param keyValuePairs
	 */
	private static void saveProperties(Map<String, String> keyValuePairs) {
		LoggerFactory.getLogger(ConfigProperties.class).debug("properties="+keyValuePairs);
		EntityManager em = JPAUtil.createManager();
		EntityTransaction txn = null;
		try {
			txn = em.getTransaction();
			txn.begin();
			for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
				LoggerFactory.getLogger(ConfigProperty.class).debug("Write "+entry.getKey()+" = "+entry.getValue());
				ConfigProperty p = new ConfigProperty(entry.getKey(), entry.getValue());
				em.merge(p);
			}		
			txn.commit();
			
		} catch(Exception e) {
			if (txn != null) try {txn.rollback();} catch (Exception e2) {}
			txn = null;
			throw e;
		} finally {
			em.close();
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	public String getDBVersion() {
		return getValue(PropertyKey.DB_VERSION);
	}
	
	/**
	 * The DB version is a special property, because this one cannot be set by the user.
	 * @param value
	 */
	public void setDBVersion(String value) {
		setValue(PropertyKey.DB_VERSION, value);
	}	
	
	public String[] getUserRoles() {
		Set<String> roles = new TreeSet<>();
		for (String string : MiscUtils.split(getValue(PropertyKey.USER_ROLES), ",")) {
			roles.add(string);
		}
		roles.add(SpiritUser.ROLE_ADMIN);
		roles.add(SpiritUser.ROLE_READALL);
		return roles.toArray(new String[roles.size()]);
	}
	
	public boolean isOpen() {
		return "open".equals(getValue(PropertyKey.RIGHTS_MODE));
	}
	

	/**
	 * Return true, if the system has been set to have a workflow. 
	 * IE: there are promoters (other than ALL), or states come from an other state
	 * @return
	 */
	public boolean hasStudyWorkflow() {
		if(hasWorkflow==null) {
			hasWorkflow = false;
			for (String state : ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES)) {
				if(ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES_FROM, state).length>0) {
					hasWorkflow = true; 
					break;
				}
				if(ConfigProperties.getInstance().getValue(PropertyKey.STUDY_STATES_PROMOTERS, state).length()>0 &&
						!ConfigProperties.getInstance().getValue(PropertyKey.STUDY_STATES_PROMOTERS, state).equals("ALL")) {
					hasWorkflow = true; 
					break;
				}
			}			
		}
		return hasWorkflow;
	}


}
