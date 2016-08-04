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
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import com.actelion.research.spiritcore.business.biosample.FoodWater;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;

public class DAOFoodWater {


	@SuppressWarnings("unchecked")
	public static List<FoodWater> getFoodWater(Study study, Phase phase) {
		if(study==null) throw new IllegalArgumentException("You must give a study");
		EntityManager session = JPAUtil.getManager();
		Query query = session.createQuery(
				" SELECT fw  " +
				" FROM FoodWater fw " +
				" WHERE fw.phase.study = :study"
				);
		
		query.setParameter("study", study); 
		List<FoodWater> fws = (List<FoodWater>) query.getResultList();
		
		List<FoodWater> res = new ArrayList<FoodWater>(); 
		for(FoodWater fw: fws) {
			if(phase!=null && !phase.equals(fw.getPhase())) continue; 
			res.add(fw);
		}
			
		
		
		Collections.sort(res);
		return res;
	}

	public static void persistFoodWater(FoodWater fw, SpiritUser user) throws Exception {
		assert fw.getContainerId()!=null;
		assert fw.getPhase()!=null;

		EntityManager session = JPAUtil.getManager();		
		//Start the transaction				
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			fw.setUpdDate(JPAUtil.getCurrentDateFromDatabase());
			fw.setUpdUser(user.getUsername());
			System.out.println("DAOFoodWater.persistFoodWater() "+fw.getContainerId()+" / "+fw.getPhase());
			if(fw.getId()<=0) {
				fw.setCreDate(fw.getUpdDate());
				fw.setCreUser(fw.getUpdUser());
				session.persist(fw);
			} else if(!session.contains(fw)) {
				session.merge(fw);
			}
			
			txn.commit();
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {}
		}	
	}

	
	
}
