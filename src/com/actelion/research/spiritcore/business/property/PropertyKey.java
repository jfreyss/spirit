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

package com.actelion.research.spiritcore.business.property;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.FormatterUtils;

public class PropertyKey {

	public static enum Tab {
		INTERNAL,
		SYSTEM,
		USER,
		STUDY,
		BIOSAMPLE
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// INTERNAL PROPERTIES
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/** Contains the DB version. This property is required, otherwise it assumed to be the latest */
	public static final PropertyKey DB_VERSION = new PropertyKey(Tab.INTERNAL, "DB Version", "", "db.version", null);

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// SYSTEM PROPERTIES
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static final PropertyKey SYSTEM_HOMEDAYS = new PropertyKey(Tab.SYSTEM, "Last changes", "Home page shows changes from the last n. days", "system.home.days", "365", "1,3,7,15,31,90,365,3650");
	public static final PropertyKey SYSTEM_DATEFORMAT = new PropertyKey(Tab.SYSTEM, "DateTime Format", "Localized DateTime format. Be sure to never change it", "format.date", FormatterUtils.DateTimeFormat.SWISS.toString(), MiscUtils.flatten(FormatterUtils.DateTimeFormat.values())) {
		@Override
		public String getDefaultValue() {
			DateFormat formatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
			try {
				String pattern = ((SimpleDateFormat)formatter).toPattern();
				if(pattern.startsWith("d/") || pattern.startsWith("dd/")) return FormatterUtils.DateTimeFormat.EUROPEAN.toString();
				if(pattern.startsWith("d.") || pattern.startsWith("dd.")) return FormatterUtils.DateTimeFormat.SWISS.toString();
				if(pattern.startsWith("yy") || pattern.startsWith("yyyy")) return FormatterUtils.DateTimeFormat.YYYYMMDD.toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return FormatterUtils.DateTimeFormat.DDMMMYYYY.toString();
		}
	};
	public static final PropertyKey SYSTEM_ADVANCED = new PropertyKey(Tab.STUDY, "Feature: Advanced Mode", "Allow parent relationships", "system.advanced", "true", "true, false");
	public static final PropertyKey SYSTEM_RESULT = new PropertyKey(Tab.SYSTEM, "Result Tab is enabled", "Keep checked to have the result functionality", "system.result", "true", "true,false");
	public static final PropertyKey SYSTEM_USEBARCODESEQUENCE = new PropertyKey(Tab.SYSTEM, "Reserve Ids", "Use sequences to avoid concurrent users to use the same id. If checked, this will create holes in the id sequences, but prevent error message when concurrent insertion happen", "system.useBarcodeSequence", "true", "true,false");
	public static final PropertyKey SYSTEM_FILE_SIZE = new PropertyKey(Tab.SYSTEM, "Max FileSize [Mo]:", "Max file size for the documents", "system.document_size", "15");
	public static final PropertyKey SYSTEM_SESSION_TIMEOUT = new PropertyKey(Tab.SYSTEM, "Session timeout (min):", "Session timeout duration", "system.session.timeout", "0");
	public static final PropertyKey SYSTEM_ASKREASON = new PropertyKey(Tab.SYSTEM, "Ask for a reason when fields get updated:", "Ask for a reason for change when the user enters a change", "system.audit.reason", "false", "true,false");

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// USER PROPERTIES
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static final PropertyKey USER_ROLES = new PropertyKey(Tab.USER, "Roles", "comma separated list of roles", "user.roles", "");
	public static final PropertyKey USER_ONEROLE = new PropertyKey(Tab.USER, "Rights based on one role", "Are users requested to login with a specic role?<br>(if true, the user will be asked for a role upon login instead of having all roles simultaneously)", "user.login.role", "false", "true,false");
	public static final PropertyKey USER_USEGROUPS = new PropertyKey(Tab.USER, "Rights based on groups", "Are the rights only role based??<br>(if true, the rights are purely role based and not user/dept specific)", "user.login.groups", "true", "true,false");
	public static final PropertyKey USER_OPENBYDEFAULT = new PropertyKey(Tab.USER, "Default User Rights", "open = study and biosamples are readable by everyone<br>restricted = biosamples and their results are limited to the departments of the owner", "rights.mode", "restricted", "open, restricted");
	public static final PropertyKey USER_LOCKAFTER = new PropertyKey(Tab.USER, "Lock user after n attempts", "Number of failed attempts before locking user", "user.lock", "0", "0,3,5,10");

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// BIOSAMPLE PROPERTIES
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static final PropertyKey BIOSAMPLE_CONTAINERTYPES = new PropertyKey(Tab.BIOSAMPLE, "Use ContainerType", "allow", "biosample.containerType.allow", "true", "true, false");

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// STUDY PROPERTIES
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static final PropertyKey STUDY_STUDYID_PATTERN = new PropertyKey(Tab.STUDY, "StudyId Pattern", "Pattern for the studyId. ex: BA-{YY}.###, S-#####", "study.studyId.pattern", "S-#####");
	public static final PropertyKey STUDY_STUDYID_EDITABLE = new PropertyKey(Tab.STUDY, "StudyId Editable", "Can the user enter his own studyId", "study.studyId.edit", "false", "true,false");
	public static final PropertyKey STUDY_DOCUMENTS = new PropertyKey(Tab.STUDY, "Allow Documents", "allow documents to be attached to the study", "study.documents", "true", "true, false");
	public static final PropertyKey STUDY_TYPES = new PropertyKey(Tab.STUDY, "Study Types (CSV)", "configurable study types allowed by the study (CSV)", "study.types", "");

	/**
	 * study.metadata contains the list of configurable metadata (csv)
	 * For each metadata, we can define:
	 * <li>KEY_STUDY_METADATA.{METADATA}.type = {DATATYPE}
	 * <li>KEY_STUDY_METADATA.{METADATA}.required = {DATATYPE}
	 * <li>KEY_STUDY_METADATA.{METADATA}.parameters = {parameters for the datatype: ex list of choices}
	 * <li>KEY_STUDY_METADATA.{METADATA}.roles = {ROLEs as csv}
	 * <li>KEY_STUDY_METADATA.{METADATA}.types = {TYPEs as csv}
	 * <li>KEY_STUDY_METADATA.{METADATA}.states = {STATESs as csv}
	 */
	public static final PropertyKey STUDY_METADATA = new PropertyKey(Tab.STUDY, "Metadata (CSV)", "Extra metadata used to configure the study (CSV)", "study.metadata", "CLINICAL, PROJECT, SITE, LICENSENO, EXPERIMENTER, DISEASEAREA");
	public static final PropertyKey STUDY_STATES = new PropertyKey(Tab.STUDY, "States (CSV)", "configurable workflow states allowed by the study (CSV)", "study.states", "EXAMPLE, TEST, ONGOING, PUBLISHED, STOPPED");
	public static final PropertyKey STUDY_DEFAULTSTATE = new PropertyKey(Tab.STUDY, "Default State", "when creating a new study", "study.defaultstate", "ONGOING", STUDY_STATES);

	public static final PropertyKey STUDY_METADATA_NAME = new PropertyKey(STUDY_METADATA, "Display Name", "Display for the end user", "name", "") {
		@Override public String getDefaultValue(String nestedValue) {
			return
					"CLINICAL".equals(nestedValue)?"Clinical Status":
						"PROJECT".equals(nestedValue)?"Project":
							"SITE".equals(nestedValue)?"Site":
								"LICENSENO".equals(nestedValue)?"License No.":
									"EXPERIMENTER".equals(nestedValue)?"Experimenter":
										"DISEASEAREA".equals(nestedValue)?"Disease Area": "";
		};
	};
	public static final PropertyKey STUDY_METADATA_DATATYPE = new PropertyKey(STUDY_METADATA, "DataType", "", "datatype", "", "ALPHA, AUTO, LIST, DATE") {
		@Override public String getDefaultValue(String nestedValue) {
			return "CLINICAL".equals(nestedValue)? DataType.LIST.name():
				"LICENSENO".equals(nestedValue)? DataType.ALPHA.name():
					"EXPERIMENTER".equals(nestedValue)? DataType.ALPHA.name():
						DataType.ALPHA.name();};
	};
	public static final PropertyKey STUDY_METADATA_PARAMETERS = new PropertyKey(STUDY_METADATA, "Parameters", "list of choices if datatype is LIST", "parameters", "") {
		@Override public String getDefaultValue(String nestedValue) {return "CLINICAL".equals(nestedValue)?"PRECLINICAL, CLINICAL": "";};
	};
	public static final PropertyKey STUDY_METADATA_TYPES = new PropertyKey(STUDY_METADATA, "Enabled in types", "In which study type, can we edit this metadata?<br>(empty = always enabled)", "types", "", STUDY_TYPES);
	public static final PropertyKey STUDY_METADATA_ROLES = new PropertyKey(STUDY_METADATA, "Editable roles", "Who is allowed to edit it?<br>(empty = always editable)", "roles", "", USER_ROLES);
	public static final PropertyKey STUDY_METADATA_STATES = new PropertyKey(STUDY_METADATA, "Editable in States", "In which states, can we edit it?<br>(empty = always editable)", "states", "", STUDY_STATES);
	public static final PropertyKey STUDY_METADATA_REQUIRED = new PropertyKey(STUDY_METADATA, "Required", "", "required", "false", "true,false");

	public static final PropertyKey STUDY_STATES_READ = new PropertyKey(STUDY_STATES, "Read roles", "(can query results/biosamples)", "read", "ALL", USER_ROLES) {
		@Override public String getDefaultValue(String nestedValue) {
			return SpiritProperties.getInstance().isOpen()?"ALL":
				nestedValue.equalsIgnoreCase("STOPPED")?"NONE": "";
		}
		@Override public String[] getSpecialChoices() {return new String[]{"ALL", "NONE"};}
	};

	public static final PropertyKey STUDY_STATES_WORK = new PropertyKey(STUDY_STATES, "Work Roles", "Roles of study workers (can add samples)", "work", "", USER_ROLES) {
		@Override public String getDefaultValue(String nestedValue) {return "ALL";};
		@Override public String[] getSpecialChoices() {return new String[]{"ALL", "NONE"};}
	};
	public static final PropertyKey STUDY_STATES_EDIT = new PropertyKey(STUDY_STATES, "Edit Roles", "Roles of study editors (can edit study design/rights)", "edit", "", USER_ROLES) {
		@Override public String getDefaultValue(String nestedValue) {return "";};
		@Override public String[] getSpecialChoices() {return new String[]{"ALL", "NONE"};}
	};
	public static final PropertyKey STUDY_STATES_DELETE = new PropertyKey(STUDY_STATES, "Delete Roles", "Delete study roles", "delete", "", USER_ROLES) {
		@Override public String getDefaultValue(String nestedValue) {return "";};
		@Override public String[] getSpecialChoices() {return new String[]{"ALL", "NONE"};}
	};
	public static final PropertyKey STUDY_STATES_SEALED = new PropertyKey(STUDY_STATES, "Sealed", "Should we seal the study in this state? (no more editable except by an admin)", "seal", "false", "true,false");


	public static final PropertyKey STUDY_FEATURE_STUDYDESIGN = new PropertyKey(Tab.STUDY, "Feature: Study Design", "Allow complex design and generation of samples", "study.feature.design", "true", "true, false");
	public static final PropertyKey STUDY_FEATURE_LIVEMONITORING = new PropertyKey(Tab.STUDY, "Feature: Live Monitoring", "Allow the live monitoring of participants", "study.feature.monitoring", "true", "true, false");


	private static Map<Tab, List<PropertyKey>> tab2properties;

	private Tab tab;
	private PropertyKey parentProperty;
	private String key;
	private String defaultValue;

	private String label;
	private String tooltip;

	private String options;
	private PropertyKey linkedOptions;

	private List<PropertyKey> nestedProperties = new ArrayList<>();

	private PropertyKey(PropertyKey parentKey, String label, String tooltip, String key, String defaultValue) {
		this(null, parentKey, label, tooltip, key, defaultValue, null, null);
	}
	private PropertyKey(PropertyKey parentKey, String label, String tooltip, String key, String defaultValue, String options) {
		this(null, parentKey, label, tooltip, key, defaultValue, options, null);
	}
	private PropertyKey(PropertyKey parentKey, String label, String tooltip, String key, String defaultValue, PropertyKey choicesKey) {
		this(null, parentKey, label, tooltip, key, defaultValue, null, choicesKey);
	}
	private PropertyKey(Tab tab, String label, String tooltip, String key, String defaultValue) {
		this(tab, null, label, tooltip, key, defaultValue, null, null);
	}
	private PropertyKey(Tab tab, String label, String tooltip, String key, String defaultValue, String options) {
		this(tab, null, label, tooltip, key, defaultValue, options, null);
	}
	private PropertyKey(Tab tab, String label, String tooltip, String key, String defaultValue, PropertyKey choicesKey) {
		this(tab, null, label, tooltip, key, defaultValue, null, choicesKey);
	}
	private PropertyKey(Tab tab, PropertyKey parentKey, String label, String tooltip, String key, String defaultValue, String options, PropertyKey linkedOptions) {
		this.tab = tab;
		this.parentProperty = parentKey;
		this.label = label;
		this.tooltip = tooltip;
		this.key = key;
		this.defaultValue = defaultValue;
		this.options = options;
		this.linkedOptions = linkedOptions;


		//Build the tree of properties
		if(tab!=null) {
			if(tab2properties==null) tab2properties = new HashMap<>();
			List<PropertyKey> properties = tab2properties.get(tab);
			if(properties==null) {
				tab2properties.put(tab, properties = new ArrayList<>());
			}
			properties.add(this);
		} else if(parentProperty!=null) {
			parentProperty.nestedProperties.add(this);
		} else {
			throw new RuntimeException("tab or parentProperty need to be set");
		}
	}

	public static List<PropertyKey> getPropertyKeys(Tab tab) {
		return tab2properties.get(tab);
	}

	public Tab getTab() {
		return tab;
	}

	public final PropertyKey getParentProperty() {
		return parentProperty;
	}
	public final String getKey() {
		return key;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public final String getLabel() {
		return label;
	}
	public String getTooltip() {
		return tooltip;
	}
	public String getOptions() {
		return options;
	}
	/**
	 * Returns the list of special choices and options concatenated in one list
	 */
	public String[] getChoices() {
		return getChoices(null);
	}
	/**
	 * Returns the list of special choices, addedChoices and options concatenated in one list
	 */
	public String[] getChoices(String addedOptions) {
		Set<String> res = new LinkedHashSet<>();
		if(getSpecialChoices()!=null) {
			res.addAll(Arrays.asList(getSpecialChoices()));
		}
		if(addedOptions!=null) {
			res.addAll(Arrays.asList(MiscUtils.split(addedOptions, ",")));
		}
		if(options!=null) {
			res.addAll(Arrays.asList(MiscUtils.split(options, ",")));
		}
		return res.toArray(new String[res.size()]);
	}
	public PropertyKey getLinkedOptions() {
		return linkedOptions;
	}

	public List<PropertyKey> getNestedProperties() {
		return nestedProperties;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof PropertyKey)) return false;
		return key!=null && key.equals(((PropertyKey)obj).getKey());
	}

	//can be overriden
	public String[] getSpecialChoices() {
		return null;
	}

	//can be overriden
	public String getDefaultValue(String nestedValue) {
		return getDefaultValue();
	}
	//can be overriden
	public String getDefaultValue(String... nestedValues) {
		return nestedValues.length==1? getDefaultValue(nestedValues[0]): getDefaultValue();
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public String toString() {
		return key;
	}

}