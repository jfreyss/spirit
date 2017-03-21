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

package com.actelion.research.spiritcore.services;

import java.util.Collection;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationType.LocationCategory;
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

	/**
	 * The user can view any location except the protected and private that are not under their department
	 * @param location
	 * @return
	 */
	public static boolean canRead(Location location, SpiritUser user) {
		if(location==null) return true;
		if(location.getInheritedPrivacy()==Privacy.PUBLIC) return true;
		if(user==null) return false;
		if(user.isSuperAdmin() || user.isReadall()) return true;
		return user.isMember(location.getInheritedEmployeeGroup());
	}

	public static boolean canReadBiosamples(Collection<Biosample> biosamples, SpiritUser user) {
		if(biosamples==null) return true;
		for (Biosample biosample : biosamples) {
			if(!canRead(biosample, user)) return false;
		}
		return true;
	}

	public static boolean canRead(Biosample biosample, SpiritUser user) {
		if(user==null) return false;
		if(biosample==null || biosample.getId()<=0) return true;
		if(user.isSuperAdmin() || user.isReadall()) return true;
		if(biosample.getCreUser()==null) return true;

		if(biosample.getInheritedStudy()!=null) {
			return canView(biosample.getInheritedStudy(), user);
		} else {
			//The creator or the manager of the creator/updater has the rights
			for(String uid: user.getManagedUsers()) {
				if(uid.equals(biosample.getCreUser()) || uid.equals(biosample.getUpdUser())) return true;
			}

			//Everybody in the group has the rights
			if(biosample.getEmployeeGroup()!=null && user.isMember(biosample.getEmployeeGroup())) return true;


			//Otherwise it depends of the location: public, protected (biosample shown without location), or member of private location
			Location location = biosample.getLocation();
			if(location==null) return SpiritProperties.getInstance().isOpen();
			else if(location.getInheritedPrivacy()==Privacy.PUBLIC) return true;
			else if(location.getInheritedPrivacy()==Privacy.PROTECTED) return SpiritProperties.getInstance().isOpen();
			else if(location.getInheritedPrivacy()==Privacy.PRIVATE && location.getInheritedEmployeeGroup()!=null && user.isMember(location.getInheritedEmployeeGroup())) return true;
		}
		return false;

	}

	public static boolean canEditBiosamples(Collection<Biosample> biosamples, SpiritUser user) {
		if(biosamples==null) return true;
		for (Biosample biosample : biosamples) {
			if(!canEdit(biosample, user)) return false;
		}
		return true;
	}

	public static boolean canEditResults(Collection<Result> results, SpiritUser user) {
		if(results==null) return true;
		for (Result result : results) {
			if(!canEdit(result, user)) return false;
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
	public static boolean canEdit(Biosample biosample, SpiritUser user) {
		if(user==null) return false;
		if(biosample==null || biosample.getId()<=0) return true;
		if(user.isSuperAdmin()) return true;
		if(biosample.getCreUser()==null) return true;

		for(String uid: user.getManagedUsers()) {
			if(uid.equalsIgnoreCase(biosample.getCreUser()) || uid.equalsIgnoreCase(biosample.getUpdUser())) return true;
		}
		if(biosample.getEmployeeGroup()!=null && user.isMember(biosample.getEmployeeGroup())) return true;

		if(biosample.getInheritedStudy()!=null) {
			return canAdmin(biosample.getInheritedStudy(), user)  || canBlind(biosample.getInheritedStudy(), user);
		} else {
			return false;
		}
	}

	public static boolean canDelete(Biosample biosample, SpiritUser user) {
		if(user==null) return false;
		if(user.isSuperAdmin()) return true;

		if(biosample==null) return false;
		if(biosample.getId()<=0) return false;
		if(biosample.getCreUser()==null) return true;
		if(user.getUsername().equalsIgnoreCase(biosample.getCreUser())) return true;


		//Allow the study admin to delete a sample when the design changes
		if(biosample.getInheritedStudy()!=null && canAdmin(biosample.getInheritedStudy(), user)) return true;

		return false;
	}

	public static boolean canDelete(Study study, SpiritUser user) {
		if(user==null) return false;
		if(user.isSuperAdmin()) return true;

		if(study==null) return false;
		if(study.getId()<=0) return false;
		if(study.getCreUser()==null) return true;

		//check for seal state?
		if("true".equals(SpiritProperties.getInstance().getValue(PropertyKey.STUDY_STATES_SEALED, study.getState()))) {
			return false;
		}
		if(user.getUsername().equalsIgnoreCase(study.getCreUser())) return true;
		return false;
	}

	public static boolean canEdit(Container container, SpiritUser user) {
		if(user==null) return false;
		if(user.isSuperAdmin()) return true;
		if(container==null) return true;
		if(container.getBiosamples().size()>0) {
			for (Biosample b : container.getBiosamples()) {
				if(!canEdit(b, user)) return false;
			}
			return true;
		} else {
			//An empty container can be edited by someone else... (important)
			return true;
		}
	}


	/**
	 * - for a new location, the user must have update rights on the container
	 * - for editing location, the user must have
	 * @param location
	 * @return
	 */
	public static boolean canEdit(Location location, SpiritUser user) {
		if(user==null) return false;
		if(user.isSuperAdmin()) return true;
		if(location==null) return false;
		if(location.getId()<=0) return true; //So that location without a type can be edited
		for(String uid: user.getManagedUsers()) {
			if(uid.equals(location.getCreUser())) return true;
			if(uid.equals(location.getUpdUser())) return true;
		}
		if(location.getLocationType().getCategory()==LocationCategory.ADMIN) return false;
		if(location.getInheritedPrivacy()==Privacy.PUBLIC) return true;
		return location.getInheritedEmployeeGroup()!=null && user.isMember(location.getInheritedEmployeeGroup());
	}

	public static boolean canAdmin(Study study, SpiritUser user) {
		if(study==null) return true;
		if(user==null) return false;
		if(user.isSuperAdmin()) return true;
		if(study.getId()<=0) return true;

		//check for seal state?
		if("true".equals(SpiritProperties.getInstance().getValue(PropertyKey.STUDY_STATES_SEALED, study.getState()))) {
			return false;
		}

		//Check roles?
		String[] roles = SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES_ADMIN, study.getState());
		if(MiscUtils.contains(roles, "NONE")) {
			return false;
		}
		if(MiscUtils.contains(roles, "ALL")) {
			return true;
		}
		if(MiscUtils.contains(roles, user.getRoles())) {
			return true;
		}

		for(String uid: user.getManagedUsers()) {
			if(uid.equalsIgnoreCase(study.getCreUser())) {
				return true;
			}
			if(study.getAdminUsersAsSet().contains(uid)) {
				return true;
			}
		}

		return false;
	}

	public static boolean canPromote(Study study, SpiritUser user) {
		if(study==null) return true;
		if(user==null) return false;
		if(user.isSuperAdmin()) return true;
		if(study.getId()<=0) return true;

		//check for seal state?
		if("true".equals(SpiritProperties.getInstance().getValue(PropertyKey.STUDY_STATES_SEALED, study.getState()))) {
			return false;
		}

		//Is there a workflow
		if(!SpiritProperties.getInstance().hasStudyWorkflow()) return false;

		//Check roles?
		String[] roles = SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES_PROMOTERS, study.getState());
		if(MiscUtils.contains(roles, "NONE")) {
			return false;
		}
		if(MiscUtils.contains(roles, "ALL")) {
			return true;
		}
		if(MiscUtils.contains(roles, user.getRoles())) {
			return true;
		}

		for(String uid: user.getManagedUsers()) {
			if(uid.equalsIgnoreCase(study.getCreUser())) return true;
			//			if(study.getOwner()!=null && study.getOwner().equals(uid)) return true;
			if(study.getAdminUsersAsSet().contains(uid)) return true;
		}

		return false;
	}

	/**
	 * Return true if the user can write or blind the study.
	 * All write users can also work as blind, but not read is not enought to work as blind
	 * @param study
	 * @param user
	 * @return
	 */
	public static boolean canBlind(Study study, SpiritUser user) {
		if(study==null) return true;
		if(user==null) return false;
		if(user.isSuperAdmin()) return true;
		if(study.getId()<=0) return true;

		//		if(study.getCreUser()!=null && study.getCreUser().equals(user.getUsername())) return true;
		//		if(study.getSetWriteUsers().contains(user.getUsername())) return true;
		if(study.getBlindAllUsersAsSet().contains(user.getUsername())) return true;
		if(study.getBlindDetailsUsersAsSet().contains(user.getUsername())) return true;

		if(canExpert(study, user)) return true;

		return false;
	}
	/**
	 * True if the user can read the study.
	 * Note: blind users CAN read the study, but the app has to restrict the group/treatment
	 * @param study
	 * @param user
	 * @return
	 */
	public static boolean canExpert(Study study, SpiritUser user) {
		if(user==null) return false;
		if(user.isSuperAdmin()) return true;
		if(study==null) return true;
		if(study.getId()<=0) return true;


		//Check seal?
		if("true".equals(SpiritProperties.getInstance().getValue(PropertyKey.STUDY_STATES_SEALED, study.getState()))) {
			return false;
		}

		//Check roles?
		String[] roles = SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES_EXPERT, study.getState());
		if(MiscUtils.contains(roles, "NONE")) {
			return false;
		}
		if(MiscUtils.contains(roles, "ALL")) {
			return true;
		}
		if(MiscUtils.contains(roles, user.getRoles())) {
			return true;
		}

		//Check groups
		for(EmployeeGroup eg: study.getEmployeeGroups()) {
			if(user.isMember(eg)) return true;
		}

		for(String uid: user.getManagedUsers()) {
			if(study.getExpertUsersAsSet().contains(uid)) return true;
		}

		if(canAdmin(study, user)) {
			return true;
		}

		return false;
	}

	/**
	 * All Osiris users can view all studies, except test studies from others
	 * @param study
	 * @param user
	 * @return
	 */
	public static boolean canRead(Study study, SpiritUser user) {
		if(user==null) return false;
		if(user.isReadall()) return true;
		if(study==null) return true;
		if(study.getId()<=0) return true;

		String[] roles = SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES_READ, study.getState());

		if(MiscUtils.contains(roles, "NONE")) {
			return false;
		}
		if(MiscUtils.contains(roles, "ALL")) {
			return true;
		}
		if(MiscUtils.contains(roles, user.getRoles())) {
			return true;
		}
		if(canExpert(study, user)) {
			return true;
		}
		if(canBlind(study, user)) {
			return true;
		}
		return false;
	}

	public static boolean canView(Study study, SpiritUser user) {
		return SpiritProperties.getInstance().isOpen() || canRead(study, user);
	}

	/**
	 * All users can read all results, except if the result is attached to a study without read rights
	 */
	public static boolean canRead(Result result, SpiritUser user) {
		if(user==null) return false;
		if(result.getBiosample()!=null) {
			Study study = result.getBiosample().getInheritedStudy();
			if(study!=null && !canRead(study, user)) return false;
		}
		return true;
	}

	/**
	 * To edit a result, you must either be the creator or you must have write access on a biosample
	 */
	public static boolean canEdit(Result result, SpiritUser user) {
		if(user==null || result==null) return false;
		if(user.isSuperAdmin()) return true;
		if(result.getId()<=0) return true;

		for(String uid: user.getManagedUsers()) {
			if(uid.equals(result.getCreUser()) || uid.equals(result.getUpdUser())) return true;
		}

		if(result.getBiosample()!=null && canEdit(result.getBiosample(), user)) return true;

		//		if(result.getBiosample()!=null && result.getBiosample().getStudy()!=null && canBlind(result.getBiosample().getStudy(), user)) return true;
		if(result.getBiosample()!=null && result.getBiosample().getInheritedStudy()!=null) {
			Study study  = result.getBiosample().getInheritedStudy();
			if(isBlind(study, user) || canAdmin(study, user)) return true;
		}

		return false;
	}

	public static boolean canDelete(Result result, SpiritUser user) {
		if(user==null) return false;
		if(user.isSuperAdmin()) return true;

		if(result==null) return false;
		if(result.getId()<=0) return false;
		if(result.getCreUser()==null) return true;
		for(String uid: user.getManagedUsers()) {
			if(uid.equals(result.getCreUser()) || uid.equals(result.getUpdUser())) return true;
		}
		return false;
	}

	public static boolean isSuperAdmin(SpiritUser user) {
		return user!=null && user.isSuperAdmin();
	}

	public static boolean canEdit(NamedSampling ns, SpiritUser user) {
		if(user!=null && (user.isSuperAdmin())) return true;
		if(user==null || ns==null) return false;
		for(String uid: user.getManagedUsers()) {
			if(uid.equals(ns.getCreUser())) return true;
		}
		if(ns.getStudy()!=null && canAdmin(ns.getStudy(), user)) return true;
		return false;
	}

	public static boolean canEdit(Order order, SpiritUser user) {
		if(user==null || order==null) return false;
		if(user.isSuperAdmin()) return true;
		if(order.getId()<=0) return true;

		for(String uid: user.getManagedUsers()) {
			if(uid.equals(order.getCreUser()) || uid.equals(order.getUpdUser())) return true;
		}
		return false;
	}

	public static boolean isBlindAll(Study study, SpiritUser user) {
		if(study==null || user==null) return false;
		if(user.isSuperAdmin()) return false;
		return study.getBlindAllUsersAsSet().contains(user.getUsername());
	}

	public static boolean isBlind(Study study, SpiritUser user) {
		if(study==null || user==null) return false;
		if(user.isSuperAdmin()) return false;
		return study.getBlindDetailsUsersAsSet().contains(user.getUsername()) || study.getBlindAllUsersAsSet().contains(user.getUsername());
	}

}
