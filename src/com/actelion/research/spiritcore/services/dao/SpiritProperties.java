/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.property.SpiritProperty;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritRights.UserType;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.MiscUtils;

/**
 * Singleton used to encapsulate the SpiritProperty, stored in the Spirit database.
 *
 * @author Joel Freyss
 */
public class SpiritProperties {

	private static SpiritProperties instance = null;
	private Map<String, String> properties;
	private String dbVersion;

	private SpiritProperties() {
		try {
			properties = loadProperties();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static SpiritProperties getInstance() {
		if(instance==null) {
			synchronized (SpiritProperties.class) {
				if(instance==null) {
					instance = new SpiritProperties();
				}
			}
		}
		return instance;
	}

	public static void reset() {
		instance = null;
	}

	/**
	 * Gets the value of a simple property or the default value
	 * @param p
	 * @return
	 */
	public String getValue(PropertyKey p) {
		assert p.getParentProperty()==null;
		assert p!=PropertyKey.DB_VERSION;

		//Retrieve the value first from the adapter, then from the DB
		String key = p.getKey();
		String v = properties.get(key);
		if(v==null) v = p.getDefaultValue();
		return v;
	}

	public int getValueInt(PropertyKey p) {
		try {
			return Integer.parseInt(getValue(p));
		} catch(Exception e) {
			return Integer.parseInt(p.getDefaultValue());
		}
	}

	/**
	 * Gets the value of a simple property or the default value
	 * The result is split by ','
	 */
	public String[] getValues(PropertyKey p) {
		return MiscUtils.split(getValue(p), ",");
	}

	public String getValue(PropertyKey p, String nestedValue) {
		return getValue(p, new String[]{nestedValue});
	}

	/**
	 * Gets the value of a nested property or the default value. One must give the values of all the parent properties
	 */
	public String getValue(PropertyKey p, String[] nestedValues) {
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

		//Retrieve the value first from the adapter, then from the DB
		String key = sb.toString();
		String v = properties.get(key);
		if(v==null) v = p.getDefaultValue(nestedValues);
		return v;
	}

	public String[] getValues(PropertyKey p, String nestedValue) {
		if(nestedValue==null) return new String[0];
		return getValues(p, new String[]{nestedValue});
	}

	/**
	 * Gets the value of a nested property or the default value. One must give the values of all the parent properties
	 * The result is splitted by ','
	 */
	public String[] getValues(PropertyKey p, String[] nestedValues) {
		return MiscUtils.split(getValue(p, nestedValues), ",");
	}

	/**
	 * Returns true if the given property is checked
	 */
	public boolean isChecked(PropertyKey p) {
		return "true".equals(getValue(p));
	}

	/**
	 * Returns true if the given property is checked
	 */
	public boolean isChecked(PropertyKey p, String nestedValues) {
		return "true".equals(getValue(p, nestedValues));
	}

	/**
	 * Returns true if the given property is checked
	 */
	public boolean isChecked(PropertyKey p, String[] nestedValues) {
		return "true".equals(getValue(p, nestedValues));
	}

	public boolean isSelected(PropertyKey p, String val) {
		return MiscUtils.contains(getValues(p), val);
	}

	public boolean isSelected(PropertyKey p, String nestedValues, String val) {
		return MiscUtils.contains(getValues(p, nestedValues), val);
	}

	public boolean isSelected(PropertyKey p, String[] nestedValues, String val) {
		return MiscUtils.contains(getValues(p, nestedValues), val);
	}


	/**
	 * Set the value of a simple property (without saving)
	 * @param p
	 * @param v
	 */
	public void setValue(PropertyKey p, String v) {
		setValue(p, new String[]{}, v);
	}

	/**
	 * Set the value of a nested property (without saving)
	 * @param p
	 * @param v
	 */
	public void setValue(PropertyKey p, String nested, String v) {
		setValue(p, new String[]{nested}, v);
	}

	/**
	 * Set the value of a simple or nested property (without saving)
	 * @param p
	 * @param nested an array, whose length is equal to the number of parents of p
	 * @param v
	 */
	public void setValue(PropertyKey p, String[] nested, String v) {
		StringBuilder propertyKey = new StringBuilder();
		propertyKey.append(p.getKey());

		PropertyKey c = p.getParentProperty();
		for (int i = nested.length-1; i >= 0; i--) {
			assert c!=null;
			propertyKey.insert(0, c.getKey() + "." + nested[i] + ".");
			c = c.getParentProperty();
		}
		assert c==null;

		//Make sure the user is not allowed to update this property
		String key = propertyKey.toString();
		properties.put(key, v);
	}

	/**
	 * Gets all values in a String map.
	 * The returned values may be overidden by the adapter
	 * @return
	 */
	public Map<String, String> getValues() {
		Map<String, String> values = new HashMap<>();
		values.putAll(properties);
		values.putAll(DBAdapter.getInstance().getProperties());
		LoggerFactory.getLogger(getClass()).debug("properties="+properties);
		return values;
	}

	public void setValues(Map<String, String> map) {
		this.properties = new HashMap<>();
		this.properties.putAll(map);
		LoggerFactory.getLogger(getClass()).debug("properties="+properties);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Loads the properties from the DB in a Map<String, String>
	 * @return
	 */
	private Map<String, String> loadProperties() {
		Map<String, String> keyValuePairs = new HashMap<>();
		EntityManager em = null;
		try {
			em = JPAUtil.createManager();
			List<SpiritProperty> properties = em.createQuery("from SpiritProperty").getResultList();
			for(SpiritProperty p: properties) {
				if(p==null) continue;
				if(PropertyKey.DB_VERSION.getKey().equals(p.getId())) {
					dbVersion = p.getValue();
				} else {
					keyValuePairs.put(p.getId(), p.getValue());
				}
			}
			keyValuePairs.putAll(DBAdapter.getInstance().getProperties());
			LoggerFactory.getLogger(SpiritProperties.class).debug("properties="+keyValuePairs);
		} finally {
			if(em!=null) try {em.close();} catch (Exception e) {e.printStackTrace();}
		}
		return keyValuePairs;
	}

	public List<SpiritProperty> getProperties() {
		List<SpiritProperty> res = new ArrayList<>();
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			if(DBAdapter.getInstance().getProperties().containsKey(entry.getKey())) continue;
			if(PropertyKey.DB_VERSION.getKey().equals(entry.getKey())) {
				dbVersion = entry.getValue();
			} else {
				res.add(new SpiritProperty(entry.getKey(), entry.getValue()));
			}
		}
		return res;
	}

	/**
	 * Save the given properties. Always open a new entitymanager, so this function can even be called in any context
	 * @param keyValuePairs
	 */
	public void saveValues() {

		EntityManager em = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = em.getTransaction();
			txn.begin();
			for (SpiritProperty p : getProperties()) {
				LoggerFactory.getLogger(SpiritProperty.class).debug("Write "+p.getId()+" = "+p.getValue());
				em.merge(p);
			}
			SpiritProperty p = new SpiritProperty(PropertyKey.DB_VERSION.getKey(), dbVersion);
			LoggerFactory.getLogger(SpiritProperty.class).debug("Write "+p.getId()+" = "+p.getValue());
			em.merge(p);

			txn.commit();
			txn = null;
		} catch(Exception e) {
			if (txn != null) try {txn.rollback();} catch (Exception e2) {e2.printStackTrace();}
			txn = null;
			throw e;
		}
		//Force Reload
		SpiritProperties.instance = null;
	}

	//////////////////////////////////////////////////////////////////////////////////////
	public String getDBVersion() {
		return dbVersion;
	}

	/**
	 * The DB version is a special property, because this one cannot be set by the user.
	 * @param value
	 */
	public void setDBVersion(String value) {
		this.dbVersion = value;
	}

	public String[] getUserRoles() {
		Set<String> roles = new LinkedHashSet<>();
		roles.add(SpiritUser.ROLE_ADMIN);
		for (String string : MiscUtils.split(getValue(PropertyKey.USER_ROLES), ",")) {
			roles.add(string);
		}
		return roles.toArray(new String[roles.size()]);
	}

	public boolean isOpen() {
		try {
			return "open".equals(getValue(PropertyKey.USER_OPENBYDEFAULT));
		} catch (Exception e) {
			return "true".equals(PropertyKey.USER_OPENBYDEFAULT.getDefaultValue());
		}
	}

	/**
	 * Is the software running in advanced mode. The advanced mode allows the following extra features:
	 * - edit biosample in form mode
	 *
	 * @return
	 */
	public boolean isAdvancedMode() {
		return SpiritProperties.getInstance().isChecked(PropertyKey.SYSTEM_ADVANCED);
	}


	/**
	 * Check the user rights for the given actionType and userType.
	 */
	public boolean isChecked(SpiritRights.ActionType action, SpiritRights.UserType userType) {
		return isChecked(action, userType, null);
	}

	/**
	 * Check the user rights for the given actionType and role.
	 */
	public boolean isChecked(SpiritRights.ActionType action, String role) {
		return isChecked(action, null, role);
	}

	public static String getKey(SpiritRights.ActionType action, SpiritRights.UserType userType, String role) {
		String key = "rights." + action + "_" + (userType==null? "role_" + role: userType);
		return key;
	}

	/**
	 * Check the user rights for the given actionType and userType.
	 * If userType==null, then a proper role must be given
	 *
	 * By default:
	 * - the admin can do everything
	 * - everybody can create samples
	 * - the owner, the updater have edit rights
	 *
	 * @param action
	 * @param userType
	 * @param role
	 */
	public boolean isChecked(SpiritRights.ActionType action, SpiritRights.UserType userType, String role) {
		boolean defaultValue;
		if(SpiritUser.ROLE_ADMIN.equals(role)) {
			defaultValue = true;
		} else if(action.name().contains("CREATE")) {
			defaultValue = true;
		} else if(action.name().contains("READ")) {
			defaultValue = SpiritProperties.getInstance().isOpen() || userType==UserType.CREATOR || userType==UserType.UPDATER;
		} else if(action.name().contains("WORK")) {
			defaultValue = userType==UserType.CREATOR || userType==UserType.UPDATER;
		} else if(action.name().contains("EDIT")) {
			defaultValue = userType==UserType.CREATOR || userType==UserType.UPDATER;
		} else if(action.name().contains("DELETE")) {
			defaultValue = userType==UserType.CREATOR;
		} else {
			defaultValue = false;
		}

		return isChecked(action, userType, role, defaultValue);
	}

	/**
	 * Check the user rights for the given actionType and userType.
	 * If userType==null, then a proper role must be given
	 * If the settings are not saved, returns the default value
	 * @param action
	 * @param userType
	 * @param role
	 * @param defaultValue
	 * @return
	 */
	public boolean isChecked(SpiritRights.ActionType action, SpiritRights.UserType userType, String role, boolean defaultValue) {
		String key = getKey(action, userType, role);
		String v = properties.get(key);
		boolean res = v==null? defaultValue: "true".equals(v);
		return res;
	}

	public void setChecked(SpiritRights.ActionType action, SpiritRights.UserType userType, boolean val) {
		setChecked(action, userType, null, val);
	}

	public void setChecked(SpiritRights.ActionType action, String role, boolean val) {
		setChecked(action, null, role, val);
	}
	/**
	 * Sets the user rights for the given actionType and userType.
	 * If userType==null, then a proper role must be given
	 * Calling this function does not save the properties. use "saveProperties" to save
	 * @param action
	 * @param userType
	 * @param role
	 * @param val
	 */
	public void setChecked(SpiritRights.ActionType action, SpiritRights.UserType userType, String role, boolean val) {
		String key = getKey(action, userType, role);
		properties.put(key, val?"true":"false");
	}

}
