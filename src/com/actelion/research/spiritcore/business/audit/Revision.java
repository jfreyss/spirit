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

package com.actelion.research.spiritcore.business.audit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.RevisionType;

import com.actelion.research.spiritcore.business.IAuditable;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.property.SpiritProperty;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.FormatterUtils;

/**
 * Revision is used to display the audit trail to the user.
 * The Revisions are transaction based: for each transaction, there is one revision with
 * @author Joel Freyss
 *
 */
public class Revision implements Comparable<Revision> {
	private int revId;
	private RevisionType type;
	private String user;
	private Date date;
	private Study study;
	private DifferenceList differenceList;
	private Map<String, String> reasonsOfChange;
	private List<IAuditable> auditable = new ArrayList<>();


	public Revision() {
	}

	public Revision(int revId, RevisionType type, Study study, Map<String, String> reasonsOfChange, DifferenceList difference, String user, Date date) {
		super();
		this.date = date;
		this.revId = revId;
		this.type = type;
		this.study = study;
		this.reasonsOfChange = reasonsOfChange;
		this.differenceList = difference;
		this.user = user;
	}

	public Study getStudy() {
		return study;
	}

	@Override
	public int hashCode() {
		return revId;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Revision)) return false;
		return revId == ((Revision)obj).revId && type == ((Revision)obj).type;
	}

	@Override
	public int compareTo(Revision o) {
		return -(revId-o.revId);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(revId + ". " + user);
		sb.append(" (" + FormatterUtils.formatDateTime(date) + ")");
		return sb.toString();
	}

	public Date getDate() {
		return date;
	}

	public int getRevId() {
		return revId;
	}

	public String getUser() {
		return user==null? "": user;
	}

	@SuppressWarnings("unchecked")
	private<T> List<T> extract(Class<T> claz) {
		List<T> res = new ArrayList<>();
		for (Object t : auditable) {
			if(claz.isInstance(t)) res.add((T)t);
		}
		return res;
	}

	public List<SpiritProperty> getSpiritProperties() {
		return extract(SpiritProperty.class);
	}

	public List<Biosample> getBiosamples() {
		return extract(Biosample.class);
	}

	public List<Employee> getEmployees() {
		return extract(Employee.class);
	}

	public List<EmployeeGroup> getEmployeeGroups() {
		return extract(EmployeeGroup.class);
	}

	public List<Result> getResults() {
		return extract(Result.class);
	}

	public List<Study> getStudies() {
		return extract(Study.class);
	}

	public List<Location> getLocations() {
		return extract(Location.class);
	}

	public List<Biotype> getBiotypes() {
		return extract(Biotype.class);
	}

	public List<Test> getTests() {
		return extract(Test.class);
	}

	public RevisionType getRevisionType() {
		return type;
	}

	public List<IAuditable> getAuditables() {
		return auditable;
	}

	public void setAuditables(List<IAuditable> entities) {
		this.auditable = entities;
	}

	public void setType(RevisionType type) {
		this.type = type;
	}

	public RevisionType getType() {
		return type;
	}

	public String getReason() {
		if(getReasonsOfChange().size()==1) return getReasonsOfChange().values().iterator().next();

		StringBuilder sb = new StringBuilder();
		for(Map.Entry<String, String> e: getReasonsOfChange().entrySet()) {
			sb.append(e.getKey()+": "+e.getValue() + "\n");
		}
		return sb.toString();
	}

	public Map<String, String> getReasonsOfChange() {
		return reasonsOfChange;
	}

	public String getReasonsOfChange(String field) {
		String res = reasonsOfChange.get(field);
		return res;
	}

	public DifferenceList getDifference() {
		return differenceList;
	}

	/**
	 * Gets the difference formatted in HTML, without the ids of the entities being modified
	 * If filterById==0, then all changes are returned
	 * If filterById>0, then only the changes concerning this id are returned
	 * @param filterById
	 * @return
	 */
	public String getDifferenceFormatted(String entityType, Serializable entityId, Integer sid) {
		if(getDifference()==null) return null;
		return getDifference().filter(entityType, entityId, sid).toHtmlString(true);
	}

	public static List<RevisionItem> getRevisionItems(List<Revision> revisions, String entityType, Serializable entityId, Integer sid) {
		//Explode revision into revisionItem
		List<RevisionItem> res = new ArrayList<>();
		if(revisions!=null) {
			for (Revision revision : revisions) {
				for (DifferenceItem di : revision.getDifference().filter(entityType, entityId, sid)) {
					res.add(new RevisionItem(revision, di));
				}
			}
		}

		return res;
	}

}