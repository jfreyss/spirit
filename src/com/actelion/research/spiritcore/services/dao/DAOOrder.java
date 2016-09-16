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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import com.actelion.research.spiritcore.business.order.Order;
import com.actelion.research.spiritcore.business.order.OrderStatus;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;

public class DAOOrder {

	public static Order getOrder(int id) {
		EntityManager session = JPAUtil.getManager();
		List<Order> res = (List<Order>) session.createQuery("SELECT o FROM Order o WHERE o.id = ?1")
				.setParameter(1, id)
				.getResultList();		
		return res.size()==1? res.get(0): null;
	}
	
	public static List<Order> getOrders(SpiritUser user) {
		EntityManager session = JPAUtil.getManager();
		List<Order> res = (List<Order>) session.createQuery("SELECT o FROM Order o WHERE o.creUser = ?1")
				.setParameter(1, user.getUsername())
				.getResultList();
		Collections.sort(res);
		return res;		
	}

	public static List<Order> getActiveOrders(int orMoreRecentThan) {
		EntityManager session = JPAUtil.getManager();
		List<Order> res = (List<Order>) session.createQuery(
				"SELECT o FROM Order o "
				+ " WHERE " 
				+ (orMoreRecentThan>=0? " o.updDate > current_date() - " +orMoreRecentThan + " or ": "")
				+ " (o.status <> '"+OrderStatus.CANCELED.name()+"' and o.status <> '"+OrderStatus.CLOSED.name()+"' and o.status <> '"+OrderStatus.REFUSED.name()+"')").getResultList();
		Collections.sort(res);
		return res;
	}
	
	/**
	 * Persist the orders 
	 * @param orders
	 * @param user
	 */
	public static void persistOrders(Collection<Order> orders, SpiritUser user) throws Exception {
		//Check that the user has the right to update the order.
		for (Order order : orders) {
			if(!SpiritRights.canEdit(order, user)) throw new Exception("You are not allowed to edit "+order);
			if(order.getContainerIds().isEmpty()) throw new Exception("The orders cannot be empty");
		}
		
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			Date now = JPAUtil.getCurrentDateFromDatabase();
			
			for (Order order : orders) {
				order.setUpdDate(now);
				order.setUpdUser(user.getUsername());
				
				if(order.getId()<=0) {
					order.setCreDate(order.getUpdDate());
					order.setCreUser(order.getUpdUser());
					session.persist(order);
				} else if(!session.contains(order)) {
					session.merge(order);
				}
			}
			txn.commit();
			txn = null;
		} finally {
			if (txn != null) try {txn.rollback();} catch (Exception e) {}
		}
		
		
	}
		
}
