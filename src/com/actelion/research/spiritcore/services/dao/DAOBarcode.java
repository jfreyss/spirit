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
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.property.PropertyKey;
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
	private static final int MAX_HOLE = 100; //To be increased with the number of users
	private static Map<String, List<String>> prefix2PrecomputedIds = new HashMap<>();
	private static Map<String, String> lastUsedIds = new HashMap<>();

	public static synchronized void reset() {
		prefix2PrecomputedIds.clear();
		lastUsedIds.clear();
	}

	/**
	 * Gets the next barcodeId for a containerType
	 * The barcode is generated using using the container's prefix
	 * @param locType
	 * @return
	 */
	public static String getNextId(ContainerType locType) {
		String pattern = (locType.getName()+"XX").substring(0, 2).toUpperCase() + "######";
		if(locType.isMultiple()) pattern += "-N";
		return getNextId(Category.CONTAINER, pattern, null);
	}

	/**
	 * Gets the next sampleid for a specific biosample.
	 * The barcode is generated based on the pattern specified in the biotype
	 *
	 * @param locType
	 * @throws Exception if the container has no prefix
	 * @return
	 */
	public static String getNextId(Biosample b) throws Exception {
		if(b==null) throw new Exception("You must give a biosample");
		Biotype biotype = b.getBiotype();
		if(biotype==null) throw new Exception("You must give a biotype");
		String prefix = biotype.getPrefix();
		if(prefix==null || prefix.length()==0) throw new Exception("SampleIds cannot be generated for " +biotype.getName()+" because the prefix is null");

		if(biotype.getPrefix().contains("{StudyId}") && b.getInheritedStudy()==null) throw new Exception("You nust select a study first");
		return DAOBarcode.getNextId(Category.BIOSAMPLE, prefix, b);
	}


	/**
	 * Formats the pattern by replacing:
	 *  - {StudyId} by the studyId
	 *  - {YY} by the Year
	 *  - {MM} by the Month
	 *  - {StudyId} by the studyId (of the given sample)
	 *  - adding ###### as suffix if the pattern does not contain #
	 * @param pattern
	 * @param context (can be null)
	 * @return
	 */
	public static String formatPattern(String pattern, Biosample context) {
		//Replace patterns in the prefix
		if(pattern.indexOf("{StudyId}")>=0) {
			pattern = pattern.replace("{StudyId}", context==null || context.getInheritedStudy()==null || context.getInheritedStudy().getStudyId().length()==0? "NoStudy": context.getInheritedStudy().getStudyId());
		}
		if(pattern.indexOf("{")>=0) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(JPAUtil.getCurrentDateFromDatabase());
			pattern = pattern.replace("{YY}", new DecimalFormat("00").format(cal.get(Calendar.YEAR) % 100));
			pattern = pattern.replace("{YYYY}", new DecimalFormat("0000").format(cal.get(Calendar.YEAR)));
			pattern = pattern.replace("{MM}", new DecimalFormat("00").format(cal.get(Calendar.MONTH)+1));
			pattern = pattern.replace("{DD}", new DecimalFormat("00").format(cal.get(Calendar.DAY_OF_MONTH)));
		}
		if(!pattern.contains("#")) pattern += "######";
		return pattern;
	}

	/**
	 * Generates an example of a barcode, using the given prefix and the id: 1
	 * @param prefix
	 * @return
	 */
	public static String getExample(String pattern) {
		String s = formatPattern(pattern, null);
		assert s.contains("#");
		int index = s.indexOf("#");
		int index2 = s.lastIndexOf("#");
		return s.substring(0, index) + new DecimalFormat(MiscUtils.repeat("0", index2-index+1)).format(1) + s.substring(index2+1);
	}

	private static String getLastBarcode(Category cat, String pattern) {
		String lastBarcode;
		EntityManager session = JPAUtil.getManager();
		if(cat==Category.BIOSAMPLE) {
			lastBarcode = (String) session.createQuery(
					"select max(sampleId) from Biosample b where sampleId like ?1"
							+ " and sampleId not like ?2 and sampleId not like ?3 and sampleId not like ?4"
							+ " and length(sampleId)>="+pattern.length())
					.setParameter(1, pattern.replaceAll("\\#+", "%"))
					.setParameter(2, pattern.replaceAll("\\#+", "%-%"))
					.setParameter(3, pattern.replaceAll("\\#+", "%.%"))
					.setParameter(4, pattern.replaceAll("\\#+", "%/%"))
					.getSingleResult();
		} else if(cat==Category.LOCATION) {
			lastBarcode = (String) session.createQuery(
					"select max(l.name) from Location l where l.name like ?1"
							+ " and length(l.name) = (select max(length(l.name)) from Location l where l.name like ?1))")
					.setParameter(1, pattern.replaceAll("\\#+", "%"))
					//					.setParameter(2, pattern.length())
					.getSingleResult();
		} else if(cat==Category.CONTAINER) {
			lastBarcode = (String) session.createQuery(
					"select max(b.container.containerId) from Biosample b where b.container.containerId like ?1 and length(b.container.containerId)>=?2")
					.setParameter(1, pattern.replaceAll("\\#+", "%").replaceAll("-N", ""))
					.setParameter(2, pattern.length())
					.getSingleResult();

		} else {
			throw new IllegalArgumentException("Invalid category: "+cat);
		}
		return lastBarcode;

	}

	private static String getNextId(Category cat, String pattern, Biosample context) {
		String formattedPattern = formatPattern(pattern, context);

		assert formattedPattern.contains("#");
		int firstIndex = formattedPattern.indexOf("#");
		int lastIndex = formattedPattern.lastIndexOf("#");
		int prefLength = firstIndex;
		int incrementLength = lastIndex-firstIndex+1;
		int suffLength = formattedPattern.length() - lastIndex-1;
		String suffix = formattedPattern.substring(formattedPattern.length()-suffLength);
		if(suffix.equals("-N")) suffix = "";

		//Retrieve next id
		List<String> list = prefix2PrecomputedIds.get(cat+"_"+formattedPattern);
		boolean newPrefix = list == null;

		//Generate next id
		if(list==null || list.size()==0) {


			list = new ArrayList<>();
			prefix2PrecomputedIds.put(cat+"_"+formattedPattern, list);

			int reserveN = cat==Category.BIOSAMPLE || cat==Category.CONTAINER? 10: 1;

			//Find the last used increment
			int startIncrement = 1;

			String lastDBBarcode = getLastBarcode(cat, formattedPattern);
			if(lastDBBarcode==null) {
				startIncrement = 1;
			} else {
				lastDBBarcode = MiscUtils.extractStartDigits(lastDBBarcode.substring(prefLength));
				try {
					startIncrement = lastDBBarcode.length()==0? 1: 1 + Integer.parseInt(MiscUtils.extractStartDigits(lastDBBarcode));
				} catch (Exception e) {
					System.err.println("Error in getting last barcode: "+e);
				}
			}
			System.out.println("DAOBarcode.getNextId() startIncrement="+startIncrement);

			String lastUsed = lastUsedIds.get(cat+"_"+formattedPattern);
			if(lastUsed!=null) {
				lastUsed = MiscUtils.extractStartDigits(lastUsed.substring(prefLength));
				try {
					int startIncrement2 = lastUsed.length()==0? 1: 1 + Integer.parseInt(lastUsed);
					System.out.println("DAOBarcode.getNextId() startIncrement2="+startIncrement2+"  --> startIncrement="+startIncrement);
					startIncrement = Math.max(startIncrement, startIncrement2);
				} catch (Exception e) {
					System.err.println("Error in getting last barcode: "+e);
				}
			}


			//Find the theoretical last barcode, and update it.
			//Be careful to create a new session, otherwise we may commit all other changes
			if(SpiritProperties.getInstance().isChecked(PropertyKey.SYSTEM_USEBARCODESEQUENCE)) {
				EntityTransaction txn = null;
				EntityManager session = null;
				try {
					session = JPAUtil.createManager();

					List<BarcodeSequence> barcodeSequences = session.createQuery("from BarcodeSequence bs where type = ?1 and category = ?2")
							.setParameter(1, formattedPattern)
							.setParameter(2, cat)
							.getResultList();
					txn = session.getTransaction();
					txn.begin();
					if(barcodeSequences.size()==0) {
						//Create a new sequence
						String nextBarcode =  formattedPattern.substring(0, prefLength) + new DecimalFormat(MiscUtils.repeat("0", incrementLength)).format(startIncrement+reserveN-1) + suffix;
						LoggerFactory.getLogger(DAOBarcode.class).debug("Create new sequence for " + formattedPattern + " = " + nextBarcode);
						BarcodeSequence sequence = new BarcodeSequence(cat, formattedPattern, nextBarcode);
						session.persist(sequence);
					} else {
						BarcodeSequence sequence = barcodeSequences.get(0);
						String lastBarcode = sequence.getLastBarcode();
						LoggerFactory.getLogger(DAOBarcode.class).debug("Last barcode from seq for " + formattedPattern+ " = " + lastBarcode);
						int startBarcodeSeq;
						try {
							startBarcodeSeq = lastBarcode==null? 1: 1 + Integer.parseInt(MiscUtils.extractStartDigits(lastBarcode));
						} catch (Exception e) {
							startBarcodeSeq = startIncrement;
						}
						if(newPrefix) {
							LoggerFactory.getLogger(DAOBarcode.class).debug("Last barcodes: seq="+startBarcodeSeq+" / db="+startIncrement);
							if(startBarcodeSeq<startIncrement) {
								//The sequence number is smaller than the actual sampleId
								LoggerFactory.getLogger(DAOBarcode.class).debug("Error in the naming for sequence prefix: "+formattedPattern+ ", restart at "+startIncrement+" instead of "+startBarcodeSeq);
								startBarcodeSeq = startIncrement;
							} else if(startIncrement>=0 && startBarcodeSeq> startIncrement + MAX_HOLE) {
								//Such a big hole in the sequence is very unlikely,
								LoggerFactory.getLogger(DAOBarcode.class).debug("Fix hole in NextId Sequence for prefix: "+formattedPattern+ ", restart at "+startIncrement+" instead of "+startBarcodeSeq);
								startBarcodeSeq = startIncrement;
							} else if(startBarcodeSeq<startIncrement) {

							} else {
								LoggerFactory.getLogger(DAOBarcode.class).debug("continue at "+startBarcodeSeq);
							}
						}
						//Start the sequence at ##1 if possible
						while(startBarcodeSeq%10!=1) startBarcodeSeq++;
						startIncrement = startBarcodeSeq;
						sequence.setLastBarcode(formattedPattern.substring(0, prefLength) + new DecimalFormat(MiscUtils.repeat("0", incrementLength)).format(startIncrement+reserveN-1) + suffix);
					}
					txn.commit();
					txn = null;

				} finally {
					if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
					if(session!=null) try{session.close();}catch (Exception e) {e.printStackTrace();}
				}
			}

			//Reserve the barcodes for future use
			LoggerFactory.getLogger(DAOBarcode.class).debug("Reserve Ids for "+formattedPattern+" : start at "+startIncrement+" lastUsed="+lastUsed+" lastDBBarcode="+lastDBBarcode);
			for (int i = 0; i < reserveN; i++) {
				int nextId = startIncrement+i;
				String nextBarcode =  formattedPattern.substring(0, prefLength) + new DecimalFormat(MiscUtils.repeat("0", incrementLength)).format(nextId) + suffix;
				list.add(nextBarcode);
			}

		}

		String res = list.remove(0);
		lastUsedIds.put(cat+"_"+formattedPattern, res);
		LoggerFactory.getLogger(DAOBarcode.class).debug("getNextId for "+formattedPattern+" = " + res + ", format="+formattedPattern);
		return res;
	}


}
