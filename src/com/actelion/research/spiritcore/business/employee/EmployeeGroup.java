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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SortNatural;

import com.actelion.research.spiritcore.business.IObject;
import com.actelion.research.util.CompareUtils;

@Entity
@Table(name="employee_group", schema="spirit", indexes = {
		@Index(name="employeegroup_parent_index", columnList="group_parent"),
		@Index(name="employeegroup_name_index", columnList="group_name")})
@SequenceGenerator(name="employee_group_sequence", sequenceName="employee_group_sequence", allocationSize=1)
@BatchSize(size=64)
public class EmployeeGroup implements Comparable<EmployeeGroup>, IObject {

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="employee_group_sequence")
	@Column(name="group_id", scale=9)
	private int id = 0;

	@Column(name="group_name", unique=true)
	private String name;

	@ManyToOne(optional=true, fetch=FetchType.LAZY)
	@JoinColumn(name="group_parent")
	private EmployeeGroup parent;

	@OneToMany(mappedBy="parent", fetch=FetchType.LAZY)
	@SortNatural
	private Set<EmployeeGroup> children = new TreeSet<>();


	public EmployeeGroup() {}

	public EmployeeGroup(String name) {
		this.name = name;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Gets the parent or null
	 * @return
	 */
	public EmployeeGroup getParent() {
		return parent;
	}

	public void setParent(EmployeeGroup parent) {
		if(parent==this.parent) return;
		if(this.parent!=null) {
			this.parent.getChildren().remove(this);
		}

		this.parent = parent;

		if(this.parent!=null) {
			this.parent.getChildren().add(this);
		}

	}

	public String getName() {
		return name;
	}

	public String getNameShort() {
		final int newLength = 15;
		if(name==null || name.length()<newLength) return name;
		StringBuilder sb = new StringBuilder();
		String[] split = name.split(" ");
		int toBeCompressed = name.length()-newLength-split.length;
		for (int i = 0; i < split.length; i++) {
			String n = split[i];
			if(sb.length()>0) sb.append(" ");
			if(toBeCompressed>0 && n.length()>3) {
				sb.append(n.substring(0, Math.max(3, n.length()-toBeCompressed/(split.length-i))));
			} else {
				sb.append(n);
			}
		}
		return sb.toString();
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param children the children to set
	 */
	public void setChildren(Set<EmployeeGroup> children) {
		this.children = children;
	}

	/**
	 * @return the children
	 */
	public Set<EmployeeGroup> getChildren() {
		return children;
	}

	/**
	 * Return itself + children until given depth (depth=0 means no children)
	 * @param maxDepth
	 * @return
	 */
	public Set<EmployeeGroup> getChildrenRec(int maxDepth) {
		Set<EmployeeGroup> res = new LinkedHashSet<EmployeeGroup>();
		res.add(this);
		if(maxDepth<=0) return res;
		for (EmployeeGroup c : getChildren()) {
			res.addAll(c.getChildrenRec(maxDepth-1));
		}
		return res;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(EmployeeGroup o) {
		if(o==null) return -1;
		int c = CompareUtils.compare(getName(), o.getName());
		if(c!=0) return c;
		return id-o.id;
	}

	@Override
	public boolean equals(Object o) {
		if(this==o) return true;
		if(!(o instanceof EmployeeGroup)) return false;
		return id>0 && id==((EmployeeGroup)o).getId();
	}

	public int getDepth() {
		int depth = 0;
		EmployeeGroup eg = this;
		while(eg!=null && depth<6) {
			depth++;
			eg = eg.getParent();
		}
		return depth;
	}

	public boolean isFunctional() {
		return getParent()!=null && getParent().getName().toUpperCase().contains("FUNCTIONAL");
	}

	public static List<Integer> getIds(Collection<EmployeeGroup> groups) {
		List<Integer> res = new ArrayList<>();
		for (EmployeeGroup eg : groups) {
			res.add(eg.getId());
		}
		return res;
	}

	public static List<String> getNames(Collection<EmployeeGroup> groups) {
		List<String> res = new ArrayList<>();
		for (EmployeeGroup eg : groups) {
			res.add(eg.getNameShort());
		}
		return res;
	}
}
