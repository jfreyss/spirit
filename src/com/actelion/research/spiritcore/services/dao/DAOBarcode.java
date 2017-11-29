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

/**
 * DAO functions linked to barcode generation
 *
 * @author Joel Freyss
 */
public class DAOBarcode {

	/**
	 * Maximum tolerated sequence hole in the barcode sequences
	 */
	private static final int MAX_HOLE = 50; //To be increased with the number of users
	private static Map<String, List<String>> prefix2PrecomputedIds = new HashMap<>();
	private static final String SUFFIX_FORMAT = "000000";

	public static synchronized void reset() {
		prefix2PrecomputedIds.clear();
	}

	/**
	 * Gets the next barcodeId for a specific container (using the container's prefix: 2 first characters)
	 * @param locType
	 * @return
	 */
	public static String getNextId(ContainerType locType) {
		String prefix = (locType.getName()+"XX").substring(0, 2).toUpperCase();
		return getNextId(Category.CONTAINER, prefix);
	}

	/**
	 * Gets the next barcodeId for a specific biotype (using the biotype's prefix)
	 * @param locType
	 * @throws Exception if the container has no prefix
	 * @return
	 */
	public static String getNextId(Biotype biotype) throws Exception {
		if(biotype==null) throw new Exception("You must give a biotype");
		String prefix = biotype.getPrefix();
		if(prefix==null || prefix.length()==0) throw new Exception("SampleIds cannot be generated for " +biotype.getName()+" because the prefix is null");
		return DAOBarcode.getNextId(Category.BIOSAMPLE, prefix);
	}


	public static String formatPattern(String pattern) {
		//Replace patterns in the prefix
		if(pattern.indexOf("{")>=0) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(JPAUtil.getCurrentDateFromDatabase());
			pattern = pattern.replace("{YY}", new DecimalFormat("00").format(cal.get(Calendar.YEAR) % 100));
			pattern = pattern.replace("{YYYY}", new DecimalFormat("0000").format(cal.get(Calendar.YEAR)));
			pattern = pattern.replace("{MM}", new DecimalFormat("00").format(cal.get(Calendar.MONTH)));
			pattern = pattern.replace("{DD}", new DecimalFormat("00").format(cal.get(Calendar.DAY_OF_MONTH)));
		}
		return pattern;
	}

	/**
	 * Generates an example of a barcode, using the given prefix and the id: 1
	 * @param prefix
	 * @return
	 */
	public static String getExample(String prefix) {
		return formatPattern(prefix) + new DecimalFormat(SUFFIX_FORMAT).format(1);
	}

	private static String getLastBarcode(Category cat, String prefix, String format) {
		String lastBarcode;
		EntityManager session = JPAUtil.getManager();
		if(cat==Category.BIOSAMPLE) {
			lastBarcode = (String) session.createQuery(
					"select max(sampleId) from Biosample b WHERE sampleId like ?1 and length(sampleId) = ?2")
					.setParameter(1, prefix+"%")
					.setParameter(2, format.length())
					.getSingleResult();
		} else if(cat==Category.LOCATION) {
			lastBarcode = (String) session.createQuery(
					"select max(l.name) from Location l WHERE l.name like ?1 and length(l.name) = ?2")
					.setParameter(1, prefix+"0%")
					.setParameter(2, format.length())
					.getSingleResult();
		} else if(cat==Category.CONTAINER) {
			lastBarcode = (String) session.createQuery(
					"select max(b.container.containerId) from Biosample b where b.container.containerId like ?1")
					.setParameter(1, prefix+"0%")
					.getSingleResult();

		} else {
			throw new IllegalArgumentException("Invalid category: "+cat);
		}
		LoggerFactory.getLogger(DAOBarcode.class).debug("getLastBarcode for "+cat+"."+prefix+" = " + lastBarcode);
		return lastBarcode;

	}

	public static String getNextId(Category cat, String prefixPattern) {
		//		return getNextId(cat, prefixPattern, true);
		//	}
		//
		//	public static String getNextId(Category cat, String prefixPattern, boolean updateBarcodeTable) {
		String prefix = formatPattern(prefixPattern);

		//Retrieve next id
		List<String> list = prefix2PrecomputedIds.get(cat+"_"+prefix);
		boolean newPrefix = list == null;

		//Generate next id
		String idFormat = SUFFIX_FORMAT;
		if(list==null || list.size()==0) {


			list = new ArrayList<>();
			prefix2PrecomputedIds.put(cat+"_"+prefix, list);

			int reserveN = cat==Category.BIOSAMPLE || cat==Category.CONTAINER? 20: 1;

			/**Find the last used barcode (security check if BarcodeSequence is invalid)*/
			int lastBarcodeId = -1;
			{
				if(newPrefix) {
					String lastBarcode = getLastBarcode(cat, prefix, prefix + SUFFIX_FORMAT);

					if(lastBarcode==null) {
						lastBarcodeId = 0;
					} else {
						lastBarcode = lastBarcode.substring(prefix.length());
						if(lastBarcode.lastIndexOf('-')>0) {
							lastBarcode = lastBarcode.substring(0, lastBarcode.lastIndexOf('-'));
						}
						idFormat = MiscUtils.repeat("0", lastBarcode.length());
						try {
							lastBarcodeId = lastBarcode==null? 0: Integer.parseInt(MiscUtils.extractStartDigits(lastBarcode));
						} catch (Exception e) {
							System.err.println("Error in getting last barcode: "+e);
						}
					}
				}
			}

			//			if(updateBarcodeTable) {
			//Find the theoretical last barcode, and update it.
			//Be careful to create a new session, or we may commit all other changes (open request must be followed by JPAUtil.closerequest in the finally close)
			EntityTransaction txn = null;
			EntityManager session = null;
			try {
				session = JPAUtil.createManager();

				List<BarcodeSequence> barcodeSequences = session.createQuery(
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
						nextBarcode = prefix + new DecimalFormat(idFormat).format(lastBarcodeId+i+1);
						list.add(nextBarcode);
					}
					BarcodeSequence sequence = new BarcodeSequence(cat, prefix, nextBarcode);
					session.persist(sequence);
				} else {
					BarcodeSequence sequence = barcodeSequences.get(0);
					String lastBarcode = sequence.getLastBarcode();
					int lastBarcodeN2 = lastBarcode==null? 0: Integer.parseInt(lastBarcode.substring(prefix.length()));
					if(newPrefix) {
						if(lastBarcodeN2<lastBarcodeId) {
							//The sequence number is smaller than the actual sampleId
							LoggerFactory.getLogger(DAOBarcode.class).debug("Error in the naming for sequence prefix: "+prefix+ ", restart at "+lastBarcodeId+" instead of "+lastBarcodeN2);
							lastBarcodeN2 = lastBarcodeId;
						} else if(lastBarcodeId>=0 && lastBarcodeN2> lastBarcodeId + MAX_HOLE) {
							//Such a big hole in the sequence is very unlikely,
							LoggerFactory.getLogger(DAOBarcode.class).debug("Fix hole in NextId Sequence for prefix: "+prefix+ ", restart at "+lastBarcodeId+" instead of "+lastBarcodeN2);
							lastBarcodeN2 = lastBarcodeId;
						}
					}

					for (int i = 0; i < reserveN; i++) {
						nextBarcode = prefix + new DecimalFormat(idFormat).format(lastBarcodeN2+i+1);
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
			//			} else if(lastBarcodeId>0) {
			//				//Don't use the BarcodeId table. This can create an error if 2 users come with the same barcodeId
			//				for (int i = 0; i < 1000; i++) {
			//					String nextBarcode = prefix + new DecimalFormat(idFormat).format(lastBarcodeId+i+1);
			//					list.add(nextBarcode);
			//				}
			//			} else {
			//				System.err.println("Prefix "+prefix+" not used yet");
			//				return "";
			//			}
		}

		String res = list.remove(0);
		LoggerFactory.getLogger(DAOBarcode.class).debug("getNextId for "+prefix+" = " + res + ", format="+idFormat);
		return res;
	}


}
