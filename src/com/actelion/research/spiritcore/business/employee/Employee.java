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

package com.actelion.research.spiritcore.business.employee;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.actelion.research.spiritcore.business.IEntity;
import com.actelion.research.spiritcore.util.MiscUtils;

@Entity
@Table(name="employee", indexes= {@Index(name="employeegroup_username_index", columnList="user_name")})
@SequenceGenerator(name="employee_sequence", sequenceName="employee_sequence", allocationSize=1)
public class Employee implements Comparable<Employee>, IEntity {

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="employee_sequence")
	@Column(name="employee_id")
	private int id = 0;

	@Column(name="user_name", length=20, nullable=false, unique=true)
	private String userName;

	@Column(name="disabled")
	private Boolean disabled = false;

	/**
	 * Encrypted password
	 */
	@Column(name="password", length=64)
	private String password;

	@Column(name="roles")
	private String roles;

	@ManyToOne(cascade=CascadeType.REFRESH, optional=true, fetch=FetchType.LAZY)
	@JoinColumn(name="manager_id")
	private Employee manager;

	@OneToMany(cascade=CascadeType.REFRESH, mappedBy="manager", fetch=FetchType.LAZY)
	private Set<Employee> children = new HashSet<>();

	@ManyToMany(cascade=CascadeType.REFRESH, fetch=FetchType.LAZY)
	@JoinTable(name = "employee_group_link",
	joinColumns = {@JoinColumn(name="employee_id")},
	inverseJoinColumns = {@JoinColumn(name="group_id")})
	private Set<EmployeeGroup> employeeGroups = new HashSet<>();


	@Column(name="upd_user", length=20)
	private String updUser;

	@Column(name="upd_date")
	@Temporal(TemporalType.TIMESTAMP)
	private Date updDate;


	public Employee() {
	}

	public Employee(String name) {
		this.userName = name;
	}

	public Employee(String name, Set<String> roles) {
		this.userName = name;
		setRoles(roles);
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Employee getManager() {
		return manager;
	}

	public void setManager(Employee manager) {
		if(this.manager == manager) return;
		if(this.manager!=null) {
			this.manager.getChildren().remove(this);
		}

		this.manager = manager;

		if(this.manager!=null) {
			this.manager.getChildren().add(this);
		}

	}

	public Set<EmployeeGroup> getEmployeeGroups() {
		return employeeGroups;
	}

	/**
	 * Return the topmost group:
	 * - not a functional group: ie.  name or parent.name is not uppercase
	 * - if several groups matches, the result is not determined
	 * @return
	 */
	public EmployeeGroup getMainEmployeeGroup() {
		if(getEmployeeGroups().size()==0) {
			return null;
		} else {
			for (EmployeeGroup gr : getEmployeeGroups()) {

				//Skip groups, whose parent is FUNCTIONAL (all uppercase)
				if(gr.getParent()!=null && gr.getParent().getName().toUpperCase().equals(gr.getParent().getName())) continue;

				//Skip groups, whose parent is in the list.
				if(gr.getParent()!=null && getEmployeeGroups().contains(gr.getParent())) continue;
				return gr;
			}
			return getEmployeeGroups().iterator().next();
		}
	}

	public void setEmployeeGroups(Set<EmployeeGroup> employeeGroups) {
		this.employeeGroups = employeeGroups;
	}

	public String getPassword() {
		return password;
	}

	/**
	 * Sets the encrypted password
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String toString() {
		String res = userName + ": ";
		for (EmployeeGroup eg : getEmployeeGroups()) {
			res += eg.getName() + " ";
		}
		return res.trim();
	}

	@Override
	public int compareTo(Employee o) {
		if(o==null) return 1;
		return (userName==null?"":userName).compareToIgnoreCase(o.getUserName());
	}

	/**
	 * @param disabled the disabled to set
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * @return the disabled
	 */
	public boolean isDisabled() {
		return disabled==Boolean.TRUE;
	}


	@Override
	public String getUpdUser() {
		return updUser;
	}

	public void setUpdUser(String updUser) {
		this.updUser = updUser;
	}

	@Override
	public Date getUpdDate() {
		return updDate;
	}

	public void setUpdDate(Date updDate) {
		this.updDate = updDate;
	}


	@Override
	public boolean equals(Object o) {
		if(this==o) return true;
		if(!(o instanceof Employee)) return false;
		if(id>0 && id==((Employee)o).getId()) return true;
		return false;
	}

	@Override
	public int hashCode() {
		return id;
	}

	public Set<Employee> getChildren() {
		return children;
	}

	/**
	 * Return itself + children until given depth (depth=0 means no children)
	 * @param maxDepth
	 * @return
	 */
	public Set<Employee> getChildrenRec(int maxDepth) {
		Set<Employee> res = new LinkedHashSet<Employee>();
		res.add(this);
		if(maxDepth<=0) return res;
		for (Employee c : getChildren()) {
			res.addAll(c.getChildrenRec(maxDepth-1));
		}
		return res;
	}

	public Set<String> getRoles() {
		return new TreeSet<String>(Arrays.asList(MiscUtils.split(this.roles, ",")));
	}

	public void setRoles(Set<String> roles) {
		this.roles = MiscUtils.unsplit(roles.toArray(new String[roles.size()]), ",");
	}

	public boolean isRole(String role) {
		return Arrays.asList(getRoles()).contains(role);
	}

}
