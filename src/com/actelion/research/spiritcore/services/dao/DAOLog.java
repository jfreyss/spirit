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

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import com.actelion.research.spiritcore.business.LogEntry;
import com.actelion.research.spiritcore.util.QueryTokenizer;

public class DAOLog {

	public static void log(String user, LogEntry.Action action) {
		log(user, action, "");
	}
	
	public static void log(String user, LogEntry.Action action, String comments) {
		assert user!=null;
		assert action!=null;
		
		String pcName = "";
		try { 
			java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
			pcName = localMachine==null? null: localMachine.getHostAddress();
		} catch(Exception e) {
			System.err.println(e);
		}
		

		EntityManager em = JPAUtil.getManager();

		
		
		LogEntry log = new LogEntry();
		log.setUser(user);
		log.setDate(JPAUtil.getCurrentDateFromDatabase());
		log.setAction(action);
		log.setComments(comments);
		log.setIpAddress(pcName);
		
		EntityTransaction txn = em.getTransaction();
		try {
			txn.begin();
			em.persist(log);
			txn.commit();
		} catch(Exception e) { 
			if(txn!=null && txn.isActive()) txn.rollback();
			throw e;
		}
	}
	
	public static List<LogEntry> getLogs(String user, LogEntry.Action action, int sinceDays) {
		EntityManager em = JPAUtil.getManager();
		try {
			Query query = em.createQuery("from LogEntry l where 1=1" + 
					(user!=null && user.length()>0? " and " + QueryTokenizer.expandOrQuery("l.user like ?", user) :"") +
					(sinceDays>=0? " and l.date >= ?1":"") + 
					(action!=null? " and l.action = '" + action.name() + "'":""));
			if(sinceDays>=0) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(JPAUtil.getCurrentDateFromDatabase());
				cal.add(Calendar.DAY_OF_YEAR, -sinceDays);
				query.setParameter(1, cal.getTime());
			}
			List<LogEntry> res = query.getResultList();
			Collections.sort(res);
			return res;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}
