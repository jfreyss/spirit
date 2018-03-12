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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.FormatterUtils;

public class Revision implements Comparable<Revision> {
	private int revId;
	private RevisionType type;
	private String reason;
	private String difference;
	private String user;
	private Study study;
	private List<IAuditable> auditable = new ArrayList<>();
	private Date date;


	public Revision() {
	}

	public Revision(int revId, RevisionType type, Study study, String reason, String difference, String user, Date date) {
		super();
		this.date = date;
		this.revId = revId;
		this.type = type;
		this.study = study;
		this.reason = reason;
		this.difference = difference;
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

	public String getWhat() {
		List<Biosample> biosamples = getBiosamples();
		List<Result> results = getResults();
		List<Study> studies = getStudies();
		List<Test> tests = getTests();
		List<Location> locations = getLocations();
		List<Biotype> biotypes = getBiotypes();
		List<SpiritProperty> properties = getSpiritProperties();
		List<Employee> employees = getEmployees();
		List<EmployeeGroup> employeeGroups = getEmployeeGroups();
		String t = (type==RevisionType.ADD?"Add": type==RevisionType.DEL?"Del": "Upd") + " ";

		List<String> desc = new ArrayList<>();
		desc.add(t);
		if(results.size()>0) desc.add(results.size() + " result" + (results.size()>1?"s":""));
		if(biosamples.size()>0) desc.add(biosamples.size() + " sample" + (biosamples.size()>1?"s":""));
		if(locations.size()>0) desc.add(locations.size() + " location" + (locations.size()>1?"s":""));
		if(studies.size()>0) desc.add(studies.size() + " stud" + (studies.size()>1?"ies":"y"));

		if(biotypes.size()>0) desc.add(biotypes.size() + " biotypes");
		if(tests.size()>0) desc.add(tests.size() + " tests");
		if(properties.size()>0) desc.add(properties.size() + " properties");
		if(employees.size()>0) desc.add(employees.size() + " employees");
		if(employeeGroups.size()>0) desc.add(employeeGroups.size() + " groups");

		return MiscUtils.flatten(desc, " ");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(revId + ". " + user + ": ");
		sb.append(getWhat());
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
		return reason;
	}

	public String getDifference() {
		return difference;
	}
}