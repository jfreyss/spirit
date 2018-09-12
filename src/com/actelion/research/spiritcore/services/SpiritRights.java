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

package com.actelion.research.spiritcore.services;

import java.util.Collection;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.Privacy;
import com.actelion.research.spiritcore.business.order.Order;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.MiscUtils;

/**
 * Class that compiles all user rights.
 * NB: SpiritUser==null means that the user is not logged
 * @author freyssj
 *
 */
public class SpiritRights {

	public enum ActionType {
		READ_STUDY("Read Study", "Who is allowed to read a study (its samples and results are not necessarily readable)"),
		WORK_STUDY("Work Study", "Who is allowed to change the status of a study"),
		EDIT_STUDY("Modify Study", "Who is allowed to edit a study (its samples and results are not necessarily editable)"),
		DELETE_STUDY("Delete Study", "Who is allowed to delete a study (its samples and results?"),

		READ_BIOSAMPLE("Read Biosample", "Who is allowed to read a biosample. Note: it is necessary to also have read rights on the study to read samples on a study."),
		WORK_BIOSAMPLE("Work Biosample", "Who is allowed to modify a biosample as part of the normal workflow (location, status). Note: it is necessary to also have read rights on the study to edit samples on a study."),
		EDIT_BIOSAMPLE("Edit Biosample", "Who is allowed to edit a biosample. Note: it is necessary to also have read rights on the study to edit samples on a study."),
		DELETE_BIOSAMPLE("Delete Biosample", "Who is allowed to edit a biosample. Note: it is necessary to also have read rights on the study to delete samples on a study."),

		READ_LOCATION("Read Location", "Who is allowed to read a protected/private location. ('public' location are always readable)"),
		EDIT_LOCATION("Edit Location", "Who is allowed to edit a location. ('public' location are always editable)"),
		DELETE_LOCATION("Delete Location", "Who is allowed to edit a location. ('public' location are always editable)"),

		READ_RESULT("Read Result", "Who is allowed to read a result. Note: it is necessary to also have read rights on the study to read results on a study."),
		EDIT_RESULT("Edit Result", "Who is allowed to edit an admin location. Note: it is necessary to also have edit rights on the study to edit results on a study."),
		DELETE_RESULT("Delete Result", "Who is allowed to edit an admin location. Note: it is necessary to also have edit rights on the study to delete results on a study.");


		private final String display;
		private final String tooltip;

		private ActionType(String display, String tooltip) {
			this.display = display;
			this.tooltip = tooltip;
		}

		public String getDisplay() {
			return display;
		}

		public String getTooltip() {
			return tooltip;
		}

	}

	public enum UserType {
		CREATOR,
		UPDATER
	}

	/**
	 * Is the user blinded for this study (all groups are blinded)
	 * @param study
	 * @param user
	 * @return
	 */
	public static boolean isBlindAll(Study study, SpiritUser user) {
		if(study==null || user==null) return false;
		return study.getBlindAllUsersAsSet().contains(user.getUsername());
	}

	/**
	 * Is the user blinded for this study (only the treatments, and group names are blinded)
	 * @param study
	 * @param user
	 * @return
	 */
	public static boolean isBlind(Study study, SpiritUser user) {
		if(study==null || user==null) return false;
		return study.getBlindDetailsUsersAsSet().contains(user.getUsername()) || study.getBlindAllUsersAsSet().contains(user.getUsername());
	}

	/**
	 * Return true if the user can write or blind the study.
	 * All write users can also work as blind, but not read is not enought to work as blind
	 * @param study
	 * @param user
	 * @return
	 */
	public static boolean canBlind(Study study, SpiritUser user) {
		return isBlind(study, user) || canWork(study, user);
	}


	/**
	 * Is the user allowed to read the study?
	 *
	 * @param study
	 * @param user
	 * @return
	 */
	public static boolean canRead(Study study, SpiritUser user) {
		if(user==null) return false;
		if(study==null) return true;
		if(study.getId()<=0) return true;

		//Check states specific roles
		String[] roles = SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES_READ, study.getState());
		if(roles.length>0) {
			if(MiscUtils.contains(roles, "NONE")) return false;
			if(MiscUtils.contains(roles, "ALL")) return true;
			if(MiscUtils.contains(roles, user.getRoles())) return true;
			return false;
		}

		//Otherwise, check generic roles
		for (String role : user.getRoles()) {
			if(SpiritProperties.getInstance().isChecked(ActionType.READ_STUDY, role)) return true;
		}

		//Check groups
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			if(user.getUsername().equals(study.getCreUser()) && SpiritProperties.getInstance().isChecked(ActionType.READ_STUDY, UserType.CREATOR)) return true;
			if(user.getUsername().equals(study.getUpdUser()) && SpiritProperties.getInstance().isChecked(ActionType.READ_STUDY, UserType.UPDATER)) return true;
		}

		//Return true by default if roles have not been defined and the system is open
		return SpiritProperties.getInstance().getUserRoles().length<=1 && SpiritProperties.getInstance().isOpen();
	}

	/**
	 * True if the user can work on the study: promote, add samples, ...
	 * @param study
	 * @param user
	 * @return
	 */
	public static boolean canWork(Study study, SpiritUser user) {
		if(user==null) return false;
		if(study==null) return false;
		if(study.getId()<=0) return true;

		//Check if the study is sealed: then no rights
		if("true".equals(SpiritProperties.getInstance().getValue(PropertyKey.STUDY_STATES_SEALED, study.getState()))) {
			return false;
		}

		//Check generic roles
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			for(EmployeeGroup eg: study.getEmployeeGroups()) {
				if(user.isMember(eg)) return true;
			}

			if(study.getCreUser().equals(user.getUsername()) && SpiritProperties.getInstance().isChecked(ActionType.WORK_STUDY, UserType.CREATOR)) return true;
			if(study.getUpdUser().equals(user.getUsername()) && SpiritProperties.getInstance().isChecked(ActionType.WORK_STUDY, UserType.UPDATER)) return true;
			for(String uid: user.getManagedUsers()) {
				if(study.getExpertUsersAsSet().contains(uid)) return true;
			}
			return canEdit(study, user);
		} else {

			String[] roles = SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES_WORK, study.getState());
			if(MiscUtils.contains(roles, "NONE")) return false;
			if(MiscUtils.contains(roles, "ALL")) return true;
			if(MiscUtils.contains(roles, user.getRoles())) return true;


			for (String role : user.getRoles()) {
				if(SpiritProperties.getInstance().isChecked(ActionType.WORK_STUDY, role)) return true;
			}
			return false;
		}

	}

	/**
	 * True if the user can edit the study, (and also add samples/results to it)
	 *
	 * @param study
	 * @param user
	 * @return
	 */
	public static boolean canEdit(Study study, SpiritUser user) {
		if(study==null) return true;
		if(user==null) return false;

		//Check if the study is sealed: then no rights
		if("true".equals(SpiritProperties.getInstance().getValue(PropertyKey.STUDY_STATES_SEALED, study.getState()))) {
			return false;
		}

		//Check states specific roles
		String[] roles = SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES_EDIT, study.getState());
		if(MiscUtils.contains(roles, "NONE")) return false;
		if(MiscUtils.contains(roles, "ALL")) return true;
		if(MiscUtils.contains(roles, user.getRoles())) return true;

		//Check generic roles
		for (String role : user.getRoles()) {
			if(SpiritProperties.getInstance().isChecked(ActionType.EDIT_STUDY, null, role)) return true;
		}

		//Check group/hierarchy rights (if needed)
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			if(study.getCreUser().equals(user.getUsername()) && SpiritProperties.getInstance().isChecked(ActionType.EDIT_STUDY, UserType.CREATOR)) return true;
			if(study.getUpdUser().equals(user.getUsername()) && SpiritProperties.getInstance().isChecked(ActionType.EDIT_STUDY, UserType.UPDATER)) return true;
			for(String uid: user.getManagedUsers()) {
				if(study.getAdminUsersAsSet().contains(uid)) {
					return true;
				}
			}
		}

		//Return true by default if roles have not been defined
		return SpiritProperties.getInstance().getUserRoles().length<=1;
	}

	/**
	 * True if the user can delete the study
	 * @param study
	 * @param user
	 * @return
	 */
	public static boolean canDelete(Study study, SpiritUser user) {
		if(user==null) return false;
		if(study==null) return false;

		//Check if the study is sealed: then no rights
		if("true".equals(SpiritProperties.getInstance().getValue(PropertyKey.STUDY_STATES_SEALED, study.getState()))) {
			return false;
		}
		//Check generic roles
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			if(study.getCreUser().equals(user.getUsername()) && SpiritProperties.getInstance().isChecked(ActionType.DELETE_STUDY, UserType.CREATOR)) return true;
			if(study.getUpdUser().equals(user.getUsername()) && SpiritProperties.getInstance().isChecked(ActionType.DELETE_STUDY, UserType.UPDATER)) return true;
		}
		for (String role : user.getRoles()) {
			if(SpiritProperties.getInstance().isChecked(ActionType.DELETE_STUDY, role)) return true;
		}

		return false;
	}


	/**
	 * True if the user can edit the namedSampling, ie: he can edit the linked
	 * @param ns
	 * @param user
	 * @return
	 */
	public static boolean canEdit(NamedSampling ns, SpiritUser user) {
		if(user==null) return false;
		if(ns==null) return false;
		if(ns.getStudy()!=null) {
			return canEdit(ns.getStudy(), user);
		} else {
			for(String uid: user.getManagedUsers()) {
				if(uid.equals(ns.getCreUser())) return true;
			}
			return false;
		}
	}

	/**
	 * True if the user can read all biosamples
	 * @param biosamples
	 * @param user
	 * @return
	 */
	public static boolean canReadBiosamples(Collection<Biosample> biosamples, SpiritUser user) {
		if(biosamples==null) return true;
		for (Biosample biosample : biosamples) {
			if(!canRead(biosample, user)) return false;
		}
		return true;
	}

	/**
	 * True if the user can read the biosample
	 * @param biosamples
	 * @param user
	 * @return
	 */
	public static boolean canRead(Biosample biosample, SpiritUser user) {
		if(user==null) return false;
		if(biosample==null || biosample.getId()<=0) return true;

		//Study right
		if(biosample.getInheritedStudy()!=null && !canRead(biosample.getInheritedStudy(), user)) return false;

		//Check generic roles
		for (String role : user.getRoles()) {
			if(SpiritProperties.getInstance().isChecked(ActionType.READ_BIOSAMPLE, role)) return true;
		}

		//Check group/hierarchy rights (if needed)
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			if(biosample.getCreUser().equals(user.getUsername()) && SpiritProperties.getInstance().isChecked(ActionType.READ_BIOSAMPLE, UserType.CREATOR)) return true;
			if(biosample.getUpdUser().equals(user.getUsername()) && SpiritProperties.getInstance().isChecked(ActionType.READ_BIOSAMPLE, UserType.UPDATER)) return true;
			for(String uid: user.getManagedUsers()) {
				if(biosample.getCreUser().equals(uid) && SpiritProperties.getInstance().isChecked(ActionType.READ_BIOSAMPLE, UserType.CREATOR)) return true;
				if(biosample.getUpdUser().equals(uid) && SpiritProperties.getInstance().isChecked(ActionType.READ_BIOSAMPLE, UserType.UPDATER)) return true;
			}

			//Everybody in the group has the rights
			if(biosample.getEmployeeGroup()!=null && user.isMember(biosample.getEmployeeGroup())) return true;
		}

		//Otherwise it depends of the location: public, protected (biosample shown without location), or member of private location
		Location location = biosample.getLocation();
		if(location!=null && location.getInheritedPrivacy()==Privacy.PUBLIC) return true;
		else if(location!=null && location.getInheritedPrivacy()==Privacy.PRIVATE && location.getInheritedEmployeeGroup()!=null && !user.isMember(location.getInheritedEmployeeGroup())) return false;

		//Return true by default if roles have not been defined and the system is open
		return SpiritProperties.getInstance().getUserRoles().length<=1 && SpiritProperties.getInstance().isOpen();

	}

	/**
	 * True if the user can work all biosamples
	 * @param biosamples
	 * @param user
	 * @return
	 */
	public static boolean canWorkBiosamples(Collection<Biosample> biosamples, SpiritUser user) {
		if(biosamples==null) return true;
		for (Biosample biosample : biosamples) {
			if(!canWork(biosample, user)) return false;
		}
		return true;
	}

	/**
	 * True if the user can edit all biosamples
	 * @param biosamples
	 * @param user
	 * @return
	 */
	public static boolean canEditBiosamples(Collection<Biosample> biosamples, SpiritUser user) {
		if(biosamples==null) return true;
		for (Biosample biosample : biosamples) {
			if(!canEdit(biosample, user)) return false;
		}
		return true;
	}

	/**
	 * A user can edit a biosample if:
	 * - he is the creator, owner, or is in the department of the sample
	 * - or he is responsible of the biosample's study (if any)
	 * - or he has edit access on the biosample's location (if any)
	 *
	 * @param biosample
	 * @param user
	 * @return
	 */
	public static boolean canWork(Biosample biosample, SpiritUser user) {
		if(user==null) return false;
		if(biosample==null) return true;
		if(biosample.getId()<=0) return true;


		//Study rights
		if(biosample.getInheritedStudy()!=null) {
			if(!canWork(biosample.getInheritedStudy(), user) && !canBlind(biosample.getInheritedStudy(), user)) return false;
		}

		//Check generic roles
		for (String role : user.getRoles()) {
			if(SpiritProperties.getInstance().isChecked(ActionType.WORK_BIOSAMPLE, role)) return true;
		}

		//Check group/hierarchy rights (if needed)
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			for(String uid: user.getManagedUsers()) {
				if(uid.equals(biosample.getCreUser()) && SpiritProperties.getInstance().isChecked(ActionType.WORK_BIOSAMPLE, UserType.CREATOR)) return true;
				if(uid.equals(biosample.getUpdUser()) && SpiritProperties.getInstance().isChecked(ActionType.WORK_BIOSAMPLE, UserType.UPDATER)) return true;
			}
			if(biosample.getEmployeeGroup()!=null && user.isMember(biosample.getEmployeeGroup())) return true;

			//Check generic roles
			for (String role : user.getRoles()) {
				if(SpiritProperties.getInstance().isChecked(ActionType.WORK_BIOSAMPLE, role)) return true;
			}
			return biosample.getId()<=0 || biosample.getInheritedStudy()!=null && (canRead(biosample.getInheritedStudy(), user) || canBlind(biosample.getInheritedStudy(), user));
		}

		//Return true by default if roles have not been defined
		return SpiritProperties.getInstance().getUserRoles().length<=1;

	}


	/**
	 * A user can edit a biosample if:
	 * - he is the creator, owner, or is in the department of the sample
	 * - or he is responsible of the biosample's study (if any)
	 * - or he has edit access on the biosample's location (if any)
	 *
	 * @param biosample
	 * @param user
	 * @return
	 */
	public static boolean canEdit(Biosample biosample, SpiritUser user) {
		if(user==null) return false;
		if(biosample==null) return true;
		if(biosample.getId()<=0) return true;

		//Study rights
		if(biosample.getInheritedStudy()!=null) {
			if(!canWork(biosample.getInheritedStudy(), user) && !canBlind(biosample.getInheritedStudy(), user)) return false;
		}

		//Check generic roles
		for (String role : user.getRoles()) {
			if(SpiritProperties.getInstance().isChecked(ActionType.EDIT_BIOSAMPLE, role)) return true;
		}

		//Check group/hierarchy rights (if needed)
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			for(String uid: user.getManagedUsers()) {
				if(uid.equals(biosample.getCreUser()) && SpiritProperties.getInstance().isChecked(ActionType.EDIT_BIOSAMPLE, UserType.CREATOR)) return true;
				if(uid.equals(biosample.getUpdUser()) && SpiritProperties.getInstance().isChecked(ActionType.EDIT_BIOSAMPLE, UserType.UPDATER)) return true;
			}
			if(biosample.getEmployeeGroup()!=null && user.isMember(biosample.getEmployeeGroup())) return true;

			//Check generic roles
			for (String role : user.getRoles()) {
				if(SpiritProperties.getInstance().isChecked(ActionType.EDIT_BIOSAMPLE, role)) return true;
			}
			return biosample.getId()<=0 || biosample.getInheritedStudy()!=null && (canRead(biosample.getInheritedStudy(), user) || canBlind(biosample.getInheritedStudy(), user));
		}

		//Return true by default if roles have not been defined
		return SpiritProperties.getInstance().getUserRoles().length<=1;

	}

	public static boolean canDelete(Biosample biosample, SpiritUser user) {
		if(user==null) return false;
		if(biosample==null) return false;

		//Study rights
		if(biosample.getInheritedStudy()!=null) {
			if(!canWork(biosample.getInheritedStudy(), user) && !canBlind(biosample.getInheritedStudy(), user)) return false;
		}

		//Check generic roles
		for (String role : user.getRoles()) {
			if(SpiritProperties.getInstance().isChecked(ActionType.DELETE_BIOSAMPLE, role)) return true;
		}

		//Check group/hierarchy rights (if needed)
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			for(String uid: user.getManagedUsers()) {
				if(uid.equals(biosample.getCreUser()) && SpiritProperties.getInstance().isChecked(ActionType.DELETE_BIOSAMPLE, UserType.CREATOR)) return true;
				if(uid.equals(biosample.getUpdUser()) && SpiritProperties.getInstance().isChecked(ActionType.DELETE_BIOSAMPLE, UserType.UPDATER)) return true;
			}
		}

		//Allow the study admin to delete a sample when the study design is changed
		if(biosample.getInheritedStudy()!=null && canEdit(biosample.getInheritedStudy(), user)) return true;

		return false;
	}

	/**
	 * The user can read any location except the protected and private that are not under their department
	 * @param location
	 * @return
	 */
	public static boolean canRead(Location location, SpiritUser user) {
		if(location==null) return true;
		if(user==null) return false;
		if(location.getInheritedPrivacy()==Privacy.PUBLIC) return true;

		//Check generic roles
		for (String role : user.getRoles()) {
			if(SpiritProperties.getInstance().isChecked(ActionType.READ_LOCATION, role)) return true;
		}

		//Check group/hierarchy rights (if needed)
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			if(user.isMember(location.getInheritedEmployeeGroup())) return true;
			for(String uid: user.getManagedUsers()) {
				if(uid.equals(location.getCreUser()) && SpiritProperties.getInstance().isChecked(ActionType.READ_LOCATION, UserType.CREATOR)) return true;
				if(uid.equals(location.getUpdUser()) && SpiritProperties.getInstance().isChecked(ActionType.READ_LOCATION, UserType.UPDATER)) return true;
			}
		}
		return false;
	}

	/**
	 * - for a new location, the user must have update rights on the container
	 * - for editing location, the user must have
	 * @param location
	 * @return
	 */
	public static boolean canEdit(Location location, SpiritUser user) {
		if(user==null) return false;
		if(location==null) return false;

		//Check generic roles
		for (String role : user.getRoles()) {
			if(SpiritProperties.getInstance().isChecked(ActionType.EDIT_LOCATION, role)) return true;
		}

		//Check group/hierarchy rights (if needed)
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			for(String uid: user.getManagedUsers()) {
				if(uid.equals(location.getCreUser()) && SpiritProperties.getInstance().isChecked(ActionType.EDIT_LOCATION, UserType.CREATOR)) return true;
				if(uid.equals(location.getUpdUser()) && SpiritProperties.getInstance().isChecked(ActionType.EDIT_LOCATION, UserType.UPDATER)) return true;
			}
			if(location.getInheritedEmployeeGroup()!=null && user.isMember(location.getInheritedEmployeeGroup())) return true;
			if(location.getInheritedPrivacy()==Privacy.PUBLIC) return true;
		}

		return false;
	}

	/**
	 * - for a new location, the user must have update rights on the container
	 * - for editing location, the user must have
	 * @param location
	 * @return
	 */
	public static boolean canDelete(Location location, SpiritUser user) {
		if(user==null) return false;
		if(location==null) return false;

		//Check generic roles
		for (String role : user.getRoles()) {
			if(SpiritProperties.getInstance().isChecked(ActionType.DELETE_LOCATION, role)) return true;
		}

		//Check group/hierarchy rights (if needed)
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			for(String uid: user.getManagedUsers()) {
				if(uid.equals(location.getCreUser()) && SpiritProperties.getInstance().isChecked(ActionType.DELETE_LOCATION, UserType.CREATOR)) return true;
				if(uid.equals(location.getUpdUser()) && SpiritProperties.getInstance().isChecked(ActionType.DELETE_LOCATION, UserType.UPDATER)) return true;
			}
			if(location.getInheritedEmployeeGroup()!=null && user.isMember(location.getInheritedEmployeeGroup())) return true;
			if(location.getInheritedPrivacy()==Privacy.PUBLIC) return true;
		}

		return false;
	}



	/**
	 * True if the user can edit all results
	 * @param results
	 * @param user
	 * @return
	 */
	public static boolean canEditResults(Collection<Result> results, SpiritUser user) {
		if(results==null) return true;
		for (Result result : results) {
			if(!canEdit(result, user)) return false;
		}
		return true;
	}



	/**
	 * True if the user can read the study
	 */
	public static boolean canRead(Result result, SpiritUser user) {
		if(user==null) return false;
		if(result.getBiosample()!=null) {
			Study study = result.getBiosample().getInheritedStudy();
			if(study!=null && !canRead(study, user)) return false;
		}

		//Check generic roles
		for (String role : user.getRoles()) {
			if(SpiritProperties.getInstance().isChecked(ActionType.READ_RESULT, role)) return true;
		}

		//Return true by default if roles have not been defined
		return SpiritProperties.getInstance().getUserRoles().length<=1;
	}

	/**
	 * To edit a result, the user need to have either:
	 * - rights on the biosample
	 * - rights on the study
	 * - have a specific role
	 * - be the owner/updater if given access
	 */
	public static boolean canEdit(Result result, SpiritUser user) {
		if(user==null || result==null) return false;
		if(result.getId()<=0) return true;


		//Check study rights first
		if(result.getBiosample()!=null) {
			if(!canRead(result.getBiosample(), user)) return false;
		}

		//Check generic roles
		for (String role : user.getRoles()) {
			if(SpiritProperties.getInstance().isChecked(ActionType.EDIT_RESULT, role)) return true;
		}

		//Check group/hierarchy rights (if needed)
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			for(String uid: user.getManagedUsers()) {
				if(uid.equals(result.getCreUser()) && SpiritProperties.getInstance().isChecked(ActionType.EDIT_RESULT, UserType.CREATOR)) return true;
				if(uid.equals(result.getUpdUser()) && SpiritProperties.getInstance().isChecked(ActionType.EDIT_RESULT, UserType.UPDATER)) return true;
			}
			if(canEdit(result.getStudy(), user)) return true;
		}


		return false;
	}

	public static boolean canDelete(Result result, SpiritUser user) {
		if(user==null) return false;

		if(result==null) return false;
		if(result.getId()<=0) return false;
		if(result.getCreUser()==null) return true;


		//Check biosample rights first
		if(result.getBiosample()!=null) {
			if(!canRead(result.getBiosample(), user)) return false;
		}

		//Check generic roles
		for (String role : user.getRoles()) {
			if(SpiritProperties.getInstance().isChecked(ActionType.DELETE_RESULT, role)) return true;
		}
		//Check group/hierarchy rights (if needed)
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			for(String uid: user.getManagedUsers()) {
				if(uid.equals(result.getCreUser()) && SpiritProperties.getInstance().isChecked(ActionType.DELETE_RESULT, UserType.CREATOR)) return true;
				if(uid.equals(result.getUpdUser()) && SpiritProperties.getInstance().isChecked(ActionType.DELETE_RESULT, UserType.UPDATER)) return true;
			}
		}
		return false;
	}

	public static boolean isSuperAdmin(SpiritUser user) {
		return user!=null && user.isSuperAdmin();
	}

	public static boolean canEdit(Order order, SpiritUser user) {
		if(user==null || order==null) return false;
		if(order.getId()<=0) return true;

		for(String uid: user.getManagedUsers()) {
			if(uid.equals(order.getCreUser()) || uid.equals(order.getUpdUser())) return true;
		}
		return false;
	}


}
