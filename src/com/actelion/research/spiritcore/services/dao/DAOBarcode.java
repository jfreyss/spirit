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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.biosample.BarcodeSequence;
import com.actelion.research.spiritcore.business.biosample.BarcodeSequence.Category;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.util.MiscUtils;

public class DAOBarcode {

	/**
	 * Maximum tolerated sequence hole in the barcode sequences
	 */
	private static final int MAX_HOLE = 50; //To be increased with the number of users
	private static Map<String, List<String>> prefix2PrecomputedIds = new HashMap<>();
	private static DecimalFormat idFormat = new DecimalFormat("000000");

	public static synchronized void reset() {
		prefix2PrecomputedIds.clear();
	}
	
	public static String getNextId(ContainerType locType) {
		String prefix = locType.getName().substring(0, 2).toUpperCase();
		return getNextId(Category.CONTAINER, prefix);
	}
	
	public static String getNextId(Biotype biotype) throws Exception {
		if(biotype==null) throw new Exception("You must give a type");
		String prefix = biotype.getPrefix();
		if(prefix==null || prefix.length()==0) throw new Exception("SampleIds cannot be generated for " +biotype.getName()+" because the prefix is null");
		return DAOBarcode.getNextId(Category.BIOSAMPLE, prefix);
	}

	
	public static String formatPattern(String pattern) {
		//Replace patterns in the prefix
		if(pattern.indexOf("{")>0) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(JPAUtil.getCurrentDateFromDatabase());
			pattern = pattern.replace("{YY}", new DecimalFormat("00").format(cal.get(Calendar.YEAR) % 100));
			pattern = pattern.replace("{YYYY}", new DecimalFormat("0000").format(cal.get(Calendar.YEAR)));
			pattern = pattern.replace("{MM}", new DecimalFormat("00").format(cal.get(Calendar.MONTH) % 100));
			pattern = pattern.replace("{DD}", new DecimalFormat("00").format(cal.get(Calendar.DAY_OF_MONTH) % 100));
		}		
		return pattern;
	}
	
	public static String getExample(String prefix) {		
		return formatPattern(prefix) + idFormat.format(1);
	}
	
	@SuppressWarnings("unchecked")
	public static synchronized String getNextId(Category cat, String prefixPattern) {
		String prefix = formatPattern(prefixPattern);

		//Retrieve next id
		List<String> list = prefix2PrecomputedIds.get(cat+"_"+prefix);
		LoggerFactory.getLogger(DAOBarcode.class).debug("getNextId for "+prefix+" list.n="+(list==null?"":list.size())+" next="+(list==null || list.size()==0?"":list.get(0)));
		boolean newPrefix = list == null;
		
		//Generate next id
		if(list==null || list.size()==0) {
			
			
			list = new ArrayList<>();
			prefix2PrecomputedIds.put(cat+"_"+prefix, list);
			
			int reserveN = cat==Category.BIOSAMPLE || cat==Category.CONTAINER? 20: 1;
			
			/**Find the last used barcode (security check if BarcodeSequence is invalid)*/
			int lastBarcodeN1 = -1;
			{
				EntityManager session = JPAUtil.getManager();
				if(newPrefix) {
					String lastBarcode; 
				
					if(cat==Category.BIOSAMPLE) {
						lastBarcode = (String) session.createQuery(
							"SELECT max(sampleId) FROM Biosample b WHERE sampleId like ?1")
							.setParameter(1, prefix+"%")
							.getSingleResult();
					} else if(cat==Category.LOCATION) {
						lastBarcode = (String) session.createQuery(
							"SELECT max(l.name) FROM Location l WHERE l.name like ?1")
							.setParameter(1, prefix+"0%")
							.getSingleResult();
					} else if(cat==Category.CONTAINER) {
						lastBarcode = (String) session.createQuery(
						"SELECT max(b.container.containerId) FROM Biosample b WHERE b.container.containerId like ?1")
						.setParameter(1, prefix+"0%")
						.getSingleResult();
					
					} else {
						throw new IllegalArgumentException("Invalid category: "+cat);
					}
					if(lastBarcode==null) {
						lastBarcodeN1 = 0; 
					} else {
						lastBarcode = lastBarcode.substring(prefix.length());
						if(lastBarcode.lastIndexOf('-')>0) lastBarcode = lastBarcode.substring(0, lastBarcode.lastIndexOf('-'));
						try {
							lastBarcodeN1 = lastBarcode==null? 0: Integer.parseInt(MiscUtils.extractStartDigits(lastBarcode));
						} catch (Exception e) {
							System.err.println("Error in getting last barcode: "+e);
						}
					}
				}
			} 
			
			//Find the theoretical last barcode, and update it.
			//Be careful to create a new session, or we may commit all other changes (open request must be followed by JPAUtil.closerequest in the finally close)
			EntityTransaction txn = null;
			EntityManager session = null;
			try {
				session = JPAUtil.createManager();

				List<BarcodeSequence> barcodeSequences = (List<BarcodeSequence>) session.createQuery(
						"SELECT bs FROM BarcodeSequence bs WHERE type = ?1 and category = ?2")
						.setParameter(1, prefix)
						.setParameter(2, cat)
						.getResultList();


				txn = session.getTransaction();
				txn.begin();
				String nextBarcode = "";
				if(barcodeSequences.size()==0) {
					//Create a new sequence
					for (int i = 0; i < reserveN; i++) {
						nextBarcode = prefix + idFormat.format(lastBarcodeN1+i+1);
						list.add(nextBarcode);
					}
					BarcodeSequence sequence = new BarcodeSequence(cat, prefix, nextBarcode);
					session.persist(sequence);									
				} else {
					BarcodeSequence sequence = barcodeSequences.get(0);
					String lastBarcode = sequence.getLastBarcode();
					int lastBarcodeN2 = lastBarcode==null? 0: Integer.parseInt(lastBarcode.substring(prefix.length()));
					if(newPrefix) {
						if(lastBarcodeN2<lastBarcodeN1) {
							//The sequence number is smaller than the actual sampleId
							System.err.println("Error in the naming for sequence prefix: "+prefix+ ", restart at "+lastBarcodeN1+" instead of "+lastBarcodeN2);
							lastBarcodeN2 = lastBarcodeN1;
						} else if(lastBarcodeN1>=0 && lastBarcodeN2> lastBarcodeN1 + MAX_HOLE) { 
							//Such a big hole in the sequence is very unlikely, 
							System.err.println("Fix hole in NextId Sequence for prefix: "+prefix+ ", restart at "+lastBarcodeN1+" instead of "+lastBarcodeN2);
							lastBarcodeN2 = lastBarcodeN1;
						}
					}
					
					
					for (int i = 0; i < reserveN; i++) {
						nextBarcode = prefix + idFormat.format(lastBarcodeN2+i+1);
						list.add(nextBarcode);
					}
					sequence.setLastBarcode(nextBarcode);
				}
				txn.commit();
				txn = null;
				
			} finally {			
				if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {}
				if(session!=null) try{session.close();}catch (Exception e) {}
			}
		}
		
		String res = list.remove(0);
		return res;
	}


	/*
	public static boolean reserveNextId(String prefix, int maxId) {
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		List<BarcodeSequence> barcodeSequences = session.createQuery("SELECT bs FROM BarcodeSequence bs WHERE type = ?1")
			.setParameter(1, prefix)
			.getResultList();
		
		String barcode = prefix + idFormat.format(maxId);
		BarcodeSequence sequence = null;
		if(barcodeSequences.size()>0) {
			sequence = barcodeSequences.get(0);
			String lastBarcode = sequence.getLastBarcode();
			int v = lastBarcode==null? 0: Integer.parseInt(lastBarcode.substring(prefix.length()));
			if(v>=maxId) return false;
			sequence.setLastBarcode(barcode);
		} else {
			sequence = new BarcodeSequence(Category.BIOSAMPLE, prefix, barcode);
		}
	
		try {
			txn = session.getTransaction();
			txn.begin();
			session.merge(sequence);
			txn.commit();
			txn = null;			
			return true;
		} finally {
			if(txn!=null) txn.rollback();
		}	
	}
	*/
//
//	public static String getNextBarcode(String prefix) throws Exception {
//		EntityManager session = JPAUtil.getManager();
//		//Load all
//		Query query = session.createQuery(
//				" SELECT max(l.name)  " +
//				" FROM Location l " +
//				" WHERE l.name like '" + prefix + "0%'");
//		try {
//			String last = (String) query.getSingleResult();
//			int lastId;
//			if(last==null) {
//				lastId = 0;
//			} else {
//				lastId = Integer.parseInt(last.substring(prefix.length()));
//			}
//			
//			
//			return prefix + new DecimalFormat("0000000").format(lastId+1);
//		} catch (Exception e) {
//			throw e;
//		}
//	}


}
