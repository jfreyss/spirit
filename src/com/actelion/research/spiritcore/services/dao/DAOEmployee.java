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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.spiritcore.util.QueryTokenizer;

/**
 * DAO functions linked to employees
 *
 * @author Joel Freyss
 */
public class DAOEmployee {

	public static EmployeeGroup getEmployeeGroup(String name) {
		EntityManager session = JPAUtil.getManager();
		List<EmployeeGroup> groups = session.createQuery("from EmployeeGroup g where g.name = ?1").setParameter(1, name).getResultList();
		return groups.size()==1? groups.get(0): null;
	}

	/**
	 * Get the list of groups below the root, sorted by hierarchy:
	 * ex: return a list sorted like:
	 * Root1
	 * !-Child1
	 * !-Child2
	 *   !-SubChild
	 * Root2
	 * @param root
	 * @return
	 */
	public static List<EmployeeGroup> getEmployeeGroups(String root) {
		List<EmployeeGroup> groups = (List<EmployeeGroup>) Cache.getInstance().get("departments");
		if(groups==null) {
			EntityManager session = JPAUtil.getManager();
			Query query = session.createQuery("from EmployeeGroup" );
			groups = query.getResultList();

			//Sort alphabetically
			Collections.sort(groups);
			Cache.getInstance().add("departments", groups, 300);
		}

		List<EmployeeGroup> res = new ArrayList<>();
		ListHashMap<EmployeeGroup, EmployeeGroup> parent2children = new ListHashMap<>();
		List<EmployeeGroup> roots = new LinkedList<>();
		Set<EmployeeGroup> toProceed = new HashSet<>();
		for (EmployeeGroup gr : groups) {
			toProceed.add(gr);
			if((root==null && gr.getParent()==null) || (root!=null && root.equals(gr.getName()))) {
				roots.add(gr);
			} else {
				parent2children.add(gr.getParent(), gr);
			}

		}

		//Sort by hierarchy
		res = new ArrayList<>();
		while(!roots.isEmpty()) {
			EmployeeGroup eg = roots.remove(0);
			if(!toProceed.contains(eg)) continue;
			toProceed.remove(eg);
			res.add(eg);
			if(parent2children.get(eg)!=null) roots.addAll(0, parent2children.get(eg));
		}
		if(root==null) {
			res.addAll(toProceed);
		}
		return res;
	}

	public static List<EmployeeGroup> getEmployeeGroups() {
		return getEmployeeGroups(null);
	}


	@SuppressWarnings("unchecked")
	public static List<Employee> getEmployees() {
		List<Employee> res = (List<Employee>) Cache.getInstance().get("employees_all");
		if(res==null) {
			EntityManager session = JPAUtil.getManager();
			res = session.createQuery("SELECT distinct(e) FROM Employee as e left join fetch e.employeeGroups g").getResultList();
			Collections.sort(res);
			Cache.getInstance().add("employees_all", res, 180);
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	public static List<Employee> getEmployees(String root) {
		List<Employee> res = (List<Employee>) Cache.getInstance().get("employees_"+root);
		if(res==null) {
			List<Integer> ids = EmployeeGroup.getIds(getEmployeeGroups(root));
			if(ids.isEmpty()) {
				res = getEmployees();
			} else {
				EntityManager session = JPAUtil.getManager();
				res = session.createQuery("from Employee e left join fetch e.employeeGroups g where " + QueryTokenizer.expandForIn("g.id", ids)+")").getResultList();
				Collections.sort(res);
			}
			Cache.getInstance().add("employees_"+root, res, 180);
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	public static Employee getEmployee(String username) {
		EntityManager session = JPAUtil.getManager();
		List<Employee> res = session.createQuery("from Employee e where e.userName = ?1").setParameter(1, username).getResultList();

		return res.size()==1? res.get(0): null;
	}

	public static void persistEmployees(Collection<Employee> employees, SpiritUser user) throws Exception {
		if(user!=null && !user.isSuperAdmin()) throw new Exception("You must be a superadmin");

		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			Date now = JPAUtil.getCurrentDateFromDatabase();

			txn = session.getTransaction();
			txn.begin();

			for (Employee employee : employees) {

				if(user!=null) {
					employee.setUpdUser(user.getUsername());
					employee.setUpdDate(now);
				}

				//Validate username
				if(employee.getUserName()==null || employee.getUserName().length()==0 || employee.getUserName().contains(" ")) throw new Exception(employee.getUserName()+" is not valid");
				int n = session.createQuery("select count(e) from Employee e where lower(e.userName) = lower(?1) and e.id <> " + employee.getId()).setParameter(1, employee.getUserName()).getFirstResult();
				if(n>0) throw new Exception(employee.getUserName()+" must be unique");

				//validate manager
				if(employee.getChildrenRec(7).contains(employee.getManager())) throw new Exception(employee.getManager()+" cannot be the manager of "+employee.getUserName());

				if(employee.getId()>0) {
					if(!session.contains(employee)) employee = session.merge(employee);
				} else {
					session.persist(employee);
				}
			}

			txn.commit();
			txn = null;

			Cache.getInstance().remove("employees_all");
		} catch (Exception e) {
			if(txn!=null && txn.isActive()) try{ txn.rollback();} catch(Exception e2) {}
			throw e;
		}
	}

	public static void removeEmployee(Employee employee, SpiritUser user) throws Exception {
		if(user==null || !user.isSuperAdmin()) throw new Exception("The user must be a superadmin");

		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {

			txn = session.getTransaction();
			txn.begin();
			employee = session.find(Employee.class, employee.getId());
			employee.getEmployeeGroups().clear();
			session.remove(employee);

			txn.commit();
			txn = null;

			Cache.getInstance().remove("employees_all");
		} catch (Exception e) {
			if(txn!=null && txn.isActive()) try{ txn.rollback();} catch(Exception e2) {}
			throw e;
		}
	}

	public static void persistEmployeeGroups(Collection<EmployeeGroup> groups, SpiritUser user) throws Exception {
		if(user==null || !user.isSuperAdmin()) throw new Exception("The user must be a superadmin");
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {

			txn = session.getTransaction();
			txn.begin();

			for (EmployeeGroup group : groups) {
				//Validate name
				if(group.getName()==null || group.getName().length()==0) throw new Exception(group.getName()+" is not valid");
				int n = session.createQuery("select count(g) from EmployeeGroup g where lower(g.name) = lower(?1) and g.id <> " + group.getId()).setParameter(1, group.getName()).getFirstResult();
				if(n>0) throw new Exception(group.getName()+" must be unique");

				//validate manager
				if(group.getChildrenRec(7).contains(group.getParent())) throw new Exception(group.getParent()+" cannot be the parent of "+group.getName());


				if(group.getId()>0) {
					if(!session.contains(group)) group = session.merge(group);
				} else {
					session.persist(group);
				}
			}

			txn.commit();
			txn = null;

			Cache.getInstance().remove("departments");
		} catch (Exception e) {
			if(txn!=null && txn.isActive()) try{ txn.rollback();} catch(Exception e2) {}
			throw e;
		}
	}

	private static List<Employee> getEmployees(EmployeeGroup group) {
		assert group!=null && group.getId()>0;
		EntityManager session = JPAUtil.getManager();
		return session.createQuery("select e from Employee e, IN(e.employeeGroups) g where g.id = " + group.getId()).getResultList();
	}

	public static void removeEmployeeGroup(EmployeeGroup group, SpiritUser user) throws Exception {
		if(user==null || !user.isSuperAdmin()) throw new Exception("The user must be a superadmin");

		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {

			txn = session.getTransaction();
			txn.begin();
			group = session.find(EmployeeGroup.class, group.getId());
			if(group==null) throw new Exception("The group is invalid");
			List<Employee> employees = getEmployees(group);
			if(employees.size()>0) throw new Exception("The group "+group+" is not empty. Members: "+employees);
			session.remove(group);

			txn.commit();
			txn = null;

			Cache.getInstance().remove("departments");
		} catch (Exception e) {
			if(txn!=null && txn.isActive()) try{ txn.rollback();} catch(Exception e2) {}
			throw e;
		}
	}

}
