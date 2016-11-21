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

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.helper.ExpressionHelper;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.QueryTokenizer;
import com.actelion.research.util.CompareUtils;

import net.objecthunter.exp4j.Expression;


@SuppressWarnings("unchecked")
public class DAOResult {
	
	private static Logger logger = LoggerFactory.getLogger(DAOResult.class); 
	
	public static String suggestElb(String user) {
		return "ELB-" + (user==null?"": user + "-") + new SimpleDateFormat("yyyyMMdd-HHmm").format(JPAUtil.getCurrentDateFromDatabase());
	}
	
	public static List<Result> queryResults(ResultQuery q, SpiritUser user) throws Exception  {
		return queryResults(JPAUtil.getManager(), q, user);
	}
	
	public static List<Result> getResults(Collection<Integer> ids) throws Exception  {
		EntityManager session = JPAUtil.getManager();
		List<Result> results = session.createQuery("SELECT r FROM Result r left join fetch r.biosample where " + QueryTokenizer.expandForIn("r.id", ids)).getResultList();
		postLoad(results);
		return results;				
	}
	
	public static List<Result> queryResults(EntityManager session, ResultQuery q, SpiritUser user) throws Exception  {
		//Create a new query per Test
		long s = System.currentTimeMillis();
		//New method for searching
		List<Result> results = getResults(session, q);
		
		//Check rights
		if(user!=null) {
			for (Iterator<Result> iterator = results.iterator(); iterator.hasNext();) {
				Result r = (Result) iterator.next();
				if(!SpiritRights.canRead(r, user)) {
					iterator.remove();
				}
			}
		}
		postLoad(results);
		logger.debug("Query done in "+(System.currentTimeMillis()-s)+"ms > unique results="+results.size());
		

		return results;
	}

	private static void postLoad(Collection<Result> results) {
		//Load linked biosamples
		ListHashMap<String,  ResultValue> sampleId2rvs = new ListHashMap<>();
		Map<Test, List<Result>> map = Result.mapTest(results);
		for (Test test : map.keySet()) {
			for(TestAttribute t: test.getAttributes()) {
				if(t.getDataType()==DataType.BIOSAMPLE) {
					for(Result result: map.get(test)) {
						ResultValue rv  = result.getResultValue(t);
						if(rv!=null && rv.getValue().length()>0) {
							sampleId2rvs.add(rv.getValue(), rv);
						}
					}
				}
			}
		}
		for(Biosample b: DAOBiosample.getBiosamplesBySampleIds(sampleId2rvs.keySet()).values()) {
			for(ResultValue rv : sampleId2rvs.get(b.getSampleId())) {
				rv.setLinkedBiosample(b);
			}
		}
	}
	
	/**
	 * Computes the formula for the given results
	 * 
	 * @param results
	 * @return true only if a value has been computed
	 */
	public static boolean computeFormula(Collection<Result> results) {
		Map<Test, List<Result>> map = Result.mapTest(results);
		boolean updated = false;
		for(Test test: map.keySet()) {
			
			//Loop though attributes of type formula
			for (TestAttribute ta : test.getAttributes()) {
				if(ta.getDataType()!=DataType.FORMULA) continue;
				String formula = ta.getParameters();
				Expression e;
				try {
					e = ExpressionHelper.createExpression(formula, test);;					
				} catch(Exception ex) {
					logger.warn("Could not evaluate "+formula+": "+ex);
					continue;
				}
				
				//Loop through each result to calculate the formula
				for (Result result : map.get(test)) {
					
					updated = true;
					try {
						double res = ExpressionHelper.evaluate(e, result);
						logger.info("Evaluate "+formula+": >"+res);
						result.setValue(ta, ""+res);
					} catch(Exception ex) {
						logger.info("ERROR "+formula+": "+ex);
						result.setValue(ta, "");						
					}
				}
				
			}
		}	
		return updated;
	}
	
	private static List<Result> getResults(EntityManager session, ResultQuery q) throws Exception {
		StringBuilder clause = new StringBuilder();
		List<Object> parameters = new ArrayList<>();

		if(q.getBids().size()>0) {
			clause.append(" and " + QueryTokenizer.expandForIn("b.id", q.getBids()));
		}
		if(q.getPhase()!=null) {
			clause.append(" and r.phase.id = " + q.getPhase().getId());					
		}
		if(q.getSid()>0) {
			clause.append(" and b.inheritedStudy.id = " + q.getSid());						
		}
		if(q.getStudyIds()!=null && q.getStudyIds().equalsIgnoreCase("NONE")) {
			clause.append(" and b.inheritedStudy is null");			
		} else if(q.getStudyIds()!=null && q.getStudyIds().length()>0) {
			clause.append(" and (" + QueryTokenizer.expandOrQuery("b.inheritedStudy.studyId = ?", q.getStudyIds()) + ")");						
		}
		
		if(q.getSids()!=null && q.getSids().size()>0) {			
			clause.append(" and " + QueryTokenizer.expandForIn("b.inheritedStudy.id", q.getSids()));
		}
		
		if(q.getGroups()!=null && q.getGroups().length()>0) {
			clause.append(" and (" + QueryTokenizer.expandOrQuery("b.inheritedGroup.name = ?", q.getGroups()) + ")");			
		}
		if(q.getSampleIds()!=null && q.getSampleIds().length()>0) {
			clause.append(" and (" + QueryTokenizer.expandOrQuery("b.sampleId = ?", q.getSampleIds()) + ")");			
		}

		if(q.getTopSampleIds()!=null && q.getTopSampleIds().length()>0) {
			clause.append(" and (" + QueryTokenizer.expandOrQuery("b.topParent.sampleId = ?", q.getTopSampleIds()) + ")");			
		}

		if(q.getContainerIds()!=null && q.getContainerIds().length()>0) {
			clause.append(" and (" + QueryTokenizer.expandOrQuery("b.container.containerId = ?", q.getContainerIds()) + ")");			
		}
		
		if(q.getContainerIds()!=null && q.getContainerIds().length()>0) {
			clause.append(" and (" + QueryTokenizer.expandOrQuery("b.container.containerId = ?", q.getContainerIds()) + ")");			
		}
		
		if(q.getElbs()!=null && q.getElbs().length()>0) {
			clause.append(" and (" + QueryTokenizer.expandOrQuery("r.elb = ?", q.getElbs()) + ")");	
		}

		if(q.getQuality()!=null) {
			if(q.getQuality().getId()<=Quality.VALID.getId()) {
				clause.append(" and (r.quality is null or r.quality >= " + q.getQuality().getId() + ")");
			} else {
				clause.append(" and r.quality >= " + q.getQuality().getId());
			}
		}
		
		if(q.getTestIds()!=null && q.getTestIds().size()>=1) {
			clause.append(" and (1=0");
			for (int testId : q.getTestIds()) {				
				clause.append(" or (r.test.id = " + testId);
				for (TestAttribute att : q.getAttribute2Values().keySet()) {
					if(att.getTest().getId()!=testId) continue;
					Set<String> attVal = q.getAttribute2Values().get(att);
					if(attVal==null || attVal.size()==0) continue;
				
					StringBuilder sb = new StringBuilder();
					for (String s : attVal) {
						if(sb.length()>0) sb.append(" or ");
						if(s==null || s.length()==0) {
							sb.append("v.value is null");						
						} else {
							sb.append("v.value = '" + (s.replace("'", "''")) + "'");
						}
					}					
					clause.append(" and (r IN (SELECT v.result FROM ResultValue v WHERE v.attribute.id = " + att.getId() + " and (" + sb + ")))");				
				}
				clause.append(")");
			}
			clause.append(")");
		}
		
		if(q.getBiotype()!=null && q.getBiotype().length()>0) {
			clause.append(" and " + QueryTokenizer.expandForIn("r.biosample.biotype.name", q.getBiotype()));
		}
		
		if(q.getUpdUser()!=null && q.getUpdUser().length()>0) {
			clause.append(" and r.updUser = ?");				
			parameters.add(q.getUpdUser());
		}
		if(q.getUpdDate()!=null && q.getUpdDate().length()>0) {
			String digits = MiscUtils.extractStartDigits(q.getUpdDate());
			if(digits.length()>0) { 
				clause.append(" and r.updDate > ?");				
				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date());
				cal.add(Calendar.DAY_OF_YEAR, -Integer.parseInt(digits));					
				parameters.add(cal.getTime());
			}		
		}
		if(q.getCreDays()!=null && q.getCreDays().length()>0) {
			String digits = MiscUtils.extractStartDigits(q.getCreDays());
			if(digits.length()>0) { 
				clause.append(" and r.creDate > ?");				
				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date());
				cal.add(Calendar.DAY_OF_YEAR, -Integer.parseInt(digits));					
				parameters.add(cal.getTime());
			}		
		}

		if(q.getBiotypes()!=null && q.getBiotypes().size()>0) {
			clause.append(" and (");
			boolean first = true;
			for (String s : q.getBiotypes()) {
				if(first) first = false; else clause.append(" or ");
				clause.append(" r.biosample.biotype.name  =  ?");
				parameters.add(s);
			}
			clause.append(")");			
		}

		if(q.getInputs()!=null && q.getInputs().size()>0) {
			clause.append(" and r IN (SELECT v.result FROM ResultValue v WHERE v.attribute.isOutput = false and (");
			boolean first = true;
			for (String s : q.getInputs()) {
				if(first) first = false; else clause.append(" or ");
				clause.append(" v.value = ?");
				parameters.add(s);
			}
			clause.append(")) ");			
		}

		if(q.getKeywords()!=null && q.getKeywords().length()>0) {
			
			StringBuilder expr = new StringBuilder();
			expr.append(" 0=1");
			expr.append(" or b.sampleId like ?");
			expr.append(" or replace(replace(replace(replace(replace(replace(replace(lower(b.name), '.', ''), ' ', ''), '-', ''), '_', ''), '/', ''), ':', ''), '#', '') like replace(replace(replace(replace(replace(replace(replace(lower(?), '.', ''), ' ', ''), '-', ''), '_', ''), '/', ''), ':', ''), '#', '')");
			expr.append(" or r IN (SELECT v.result FROM ResultValue v WHERE replace(replace(replace(replace(replace(replace(replace(lower(v.value), '.', ''), ' ', ''), '-', ''), '_', ''), '/', ''), ':', ''), '#', '') like replace(replace(replace(replace(replace(replace(replace(lower(?), '.', ''), ' ', ''), '-', ''), '_', ''), '/', ''), ':', ''), '#', ''))"); 
			expr.append(" or (r.test IN (SELECT t from Test t WHERE LOWER(t.name) like LOWER(?)))"); 
			expr.append(" or r.elb like ?");
			expr.append(" or LOWER(b.name) like LOWER(?)");
			expr.append(" or (b.id IN (SELECT b2.id FROM Biosample b2 WHERE LOWER(b2.inheritedStudy.studyId) like LOWER(?)))");
			expr.append(" or (b.id IN (SELECT b2.id FROM Biosample b2 WHERE LOWER(b2.inheritedStudy.ivv) like LOWER(?)))");
			expr.append(" or (b.id IN (SELECT b2.id FROM Biosample b2 WHERE LOWER(b2.biotype.name) like LOWER(?)))");
			expr.append(" or (b.id IN (SELECT b2.id FROM Biosample b2 WHERE LOWER(b2.inheritedGroup.name) like LOWER(?)))");
			expr.append(" or (b.id IN (SELECT b2.id FROM Biosample b2 WHERE LOWER(b2.inheritedPhase.name) like LOWER(?)))");
			expr.append(" or (b.id IN (SELECT b2.id FROM Biosample b2 WHERE LOWER(b2.topParent.sampleId) like LOWER(?)))");
			expr.append(" or (b.id IN (SELECT b2.id FROM Biosample b2 WHERE LOWER(b2.topParent.name) like LOWER(?)))");
			expr.append(" or (b.id IN (SELECT b2.id FROM Biosample b2 WHERE LOWER(b2.location.name) like LOWER(?)))");				
			expr.append(" or (b.id IN (SELECT b2.id FROM Biosample b2 WHERE LOWER(b2.serializedMetadata) like LOWER(?)))");
			expr.append(" or (b.id IN (SELECT b2.id FROM Biosample b2 WHERE LOWER(b2.parent.serializedMetadata) like LOWER(?)))");
			expr.append(" or (b.id IN (SELECT b2.id FROM Biosample b2 JOIN b2.linkedBiosamples b3 WHERE LOWER(b3.serializedMetadata) like LOWER(?)))");
			expr.append(" or LOWER(b.comments) like LOWER(?)");
			expr.append(" or LOWER(b.creUser) like LOWER(?)");
			expr.append(" or LOWER(b.updUser) like LOWER(?)");

			
			clause.append(" and (" + QueryTokenizer.expandQuery(expr.toString(), q.getKeywords(), true, true) + ")");
		}
		
		String jpql = "SELECT distinct(r) FROM Result r left join fetch r.biosample b ";
			// + "left join fetch r.values";
		
		if(clause.length()>0) {
			assert clause.subSequence(0,4).equals(" and"): "clause == '"+clause+"'";
			jpql += " where " + clause.substring(4);
		}
		
		jpql = JPAUtil.makeQueryJPLCompatible(jpql);
				
		Query jpaQuery = session.createQuery(jpql);		
		for (int i = 0; i < parameters.size(); i++) {
			jpaQuery.setParameter(i+1, parameters.get(i));
		}
		
		List<Result> results = jpaQuery.getResultList();
		
		//Post filters
		if(q.getPhases()!=null && q.getPhases().length()>0) {
		
			Set<String> set = new HashSet<String>(Arrays.asList(MiscUtils.split(q.getPhases(), MiscUtils.SPLIT_SEPARATORS_WITH_SPACE)));
			if(set.size()>0) {
				List<Result> filtered = new ArrayList<>();
				for (Result r : results) {
					if(r.getBiosample()==null) continue;
					if(r.getBiosample().getInheritedPhase()==null) continue;
					if(!set.contains(r.getBiosample().getInheritedPhase().getShortName())) continue;
					filtered.add(r);
				}
				results = filtered;
			}
		}
		
		
		
		return results;
		
	}
	
	public static void deleteResults(Collection<Result> results, SpiritUser user) throws Exception {
		if(results==null || results.size()==0) return;
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			deleteResults(session, results, user);
			txn.commit();
			txn = null;
		} finally {
			if(txn!=null) if(txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}
	}		
	
	public static void deleteResults(EntityManager session, Collection<Result> results, SpiritUser user) throws Exception {
		if(results==null || results.size()==0) return;
		assert session!=null;
		assert session.getTransaction().isActive();
		
		for (Result result : results) {
			if(!SpiritRights.canDelete(result, user)) throw new Exception(user+" is not allowed to delete "+result);
		}

		for (Result result : results) {
			if(!session.contains(result)) {
				result = session.merge(result);
			}
			session.remove(result);
		}			
	}
	
	
	/**
	 * Creates or replace an experiment.
	 * 
	 * @param elb
	 * @param results
	 * @param user - may be null, but then updUser and updDate have to be set
	 * @throws Exception
	 */
	public static void persistExperiment(boolean isNewExperiment, String experimentElb, Collection<Result> results, SpiritUser user) throws Exception {		
		if(results.size()==0) throw new Exception("The results are empty");
		if(experimentElb==null) throw new Exception("The elb is required");
		
		for (Result result : results) {
			if(result.getId()<=0 && result.isEmpty()) continue;
			if(result.getElb()!=null && !result.getElb().equals(experimentElb)) throw new Exception("All the ELBs must be equal");				
			if(user!=null && !SpiritRights.canEdit(result, user)) {
				throw new Exception("The elb '" + experimentElb + "' already exist and only "+result.getUpdUser()+" can add, edit, delete results during 7 days");
			}
			result.setElb(experimentElb);
		}
		
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			long s = System.currentTimeMillis();
			txn = session.getTransaction();
			txn.begin();
			persistResults(session, experimentElb, isNewExperiment, true, results, user);
			txn.commit();
			txn = null;		
			logger.debug("done in "+(System.currentTimeMillis()-s)+"ms");
			
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}	
			
		
	}
	
	
	/**
	 * Updates the results
	 * @param results
	 * @param user - the user updating the results, (if null, the upduser/upddate are not set 
	 * @throws Exception
	 */
	public static void persistResults(Collection<Result> results, SpiritUser user) throws Exception {
		if(results.size()==0) return;
		logger.info("Persist "+results.size()+" results");
		for (Result result : results) {
			if(result.getId()<=0 && result.isEmpty()) continue;
			if(user!=null && !SpiritRights.canEdit(result, user)) {
				throw new Exception("The result "+result+" cannot be edited by "+user);
			}
		}
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			long s = System.currentTimeMillis();
			txn = session.getTransaction();
			txn.begin();
			persistResults(session, null, false, false, results, user);

			txn.commit();
			txn = null;		
			logger.debug("done in "+(System.currentTimeMillis()-s)+"ms");
			
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}		
		
	}
	
	public static void persistResults(EntityManager session, Collection<Result> results, SpiritUser user) throws Exception {
		persistResults(session, null, false, false, results, user);
	}

	private static void persistResults(EntityManager session, String experimentElb, boolean isNewExperiment, boolean removeOlderResults, Collection<Result> results, SpiritUser user) throws Exception {
		assert user!=null;
		assert session!=null;
		assert session.getTransaction().isActive();
		
		Date now = JPAUtil.getCurrentDateFromDatabase();
	
		//Quick check of integrity constraints
		for (Result r : results) {
			
			//Check that if a phase is present, then there is a biosample without any phase. (or delete the phase, if there is a match) 
			if(r.getPhase()!=null) {
				if(r.getBiosample()==null) throw new Exception("The result "+r+" has a phase but no biosample");
				if(r.getBiosample().getInheritedPhase()!=null) {
					if(!r.getBiosample().getInheritedPhase().equals(r.getPhase())) throw new Exception("The phase of the result "+r+" does not match the phase of the biosample");
					r.setPhase(null);
				} else if(!r.getPhase().getStudy().equals(r.getBiosample().getInheritedStudy())) {
					throw new Exception("The phase.study of the result "+r+" does not match biosample.study");
				}
				
			}
		}

		if(experimentElb!=null) {
			

			Map<Integer, Result> id2result = JPAUtil.mapIds(results);
			List<Object[]> lastUpdates = (List<Object[]>) session.createQuery("select b.updDate, b.updUser, b.id from Result b where " + QueryTokenizer.expandForIn("b.id", id2result.keySet())).getResultList();
			for (Object[] lastUpdate : lastUpdates) {
				Date lastDate = (Date) lastUpdate[0];
				String lastUser = (String) lastUpdate[1];
				Result r = id2result.get(((Integer) lastUpdate[2]));
				if (r != null && r.getUpdDate() != null && lastDate != null) {
					int diffSeconds = (int) ((lastDate.getTime() - r.getUpdDate().getTime()) / 1000L);
					if (diffSeconds > 0)
						throw new Exception("The result (" + r + ") has just been updated by " + lastUser + " [" + diffSeconds + "seconds ago].\nYou cannot overwrite those changes unless you reopen the newest version.");
				}
			}
			
			
			List<Result> before = getResults(session, ResultQuery.createQueryForElb(experimentElb));	
			if(isNewExperiment && before.size()>0) {
				throw new Exception("The elb " +experimentElb+" is not new. You should edit an experiment to add results to an existing one");				
			} 
			
			Map<Integer, Result> id2after = JPAUtil.mapIds(results);
			if(removeOlderResults) {
				//Delete outdated results
				for (Result b : before) {						
					if(!id2after.containsKey(b.getId())) {
						b.setUpdUser(user.getUsername());
						b.setUpdDate(now);
						session.remove(b);
					}
				}		
			}
		}

		int count = 0;
		
		//Compute formula if needed
		computeFormula(results);
		
		
		for (Result result : results) {
			if(result.getId()<=0 && result.isEmpty()) continue;
			
			//Make sure the dual-links are there
			for (ResultValue v : result.getResultValues()) v.setResult(result);
			
			if(user!=null) {
				result.setUpdUser(user.getUsername());
				result.setUpdDate(now);					
			} else if(result.getUpdUser()==null || result.getUpdDate()==null) {
				throw new Exception("The updUser and updDate should have been set by the program");
			}
			if(result.getCreUser()==null) {
				result.setCreUser(result.getUpdUser());
				result.setCreDate(now);
			}

			if(result.getId()<=0) {
				logger.debug("Persist result: "+result+" / "+result.getId());
				session.persist(result);
			} else if(!session.contains(result)) {
				logger.debug("Merge result: "+result);
				session.merge(result);
			} else {
				logger.debug("Attached result: "+result);
			}
			
			if(++count%1000==0) {
				logger.debug(count+"/"+results.size()+" rows processed");
			}
		}
		
	}	

	public static int rename(TestAttribute att, String value, String newValue, SpiritUser user) throws Exception {
		if(user==null || !user.isSuperAdmin()) throw new Exception("You must be an admin to rename an attribute");
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			
			List<Result> results = (List<Result>) session.createQuery("select v.result from ResultValue v where v.value = ?1 and v.attribute.id = ?2")
				.setParameter(1, value)
				.setParameter(2, att.getId())
				.getResultList();
			
			Date now = JPAUtil.getCurrentDateFromDatabase();			
			for (Result result : results) {
				result.setUpdDate(now);
				result.setUpdUser(user.getUsername());
				result.setValue(att, newValue);
				session.merge(result);
			}
			
			txn.commit();
			return results.size();
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}		
	}
	
	public static int move(Test src, TestAttribute srcAtt, String srcVal, Test dest, SpiritUser user) throws Exception {
		ResultQuery q = new ResultQuery();
		
		if(!user.isSuperAdmin()) throw new Exception("You need to be a superadmin to perform this operation");
		q.getAttribute2Values().add(srcAtt, srcVal);
		List<Result> results = queryResults(q, user);
		
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			
			List<Integer> ids = new ArrayList<>();
			for (TestAttribute att : dest.getAttributes()) {
				ids.add(att.getId());
			}
			
			for (Result result : results) {
				ResultValue val = result.remove(srcAtt);
				if(val==null) throw new Exception("The result "+result.getId()+" does not have a value for att:"+srcAtt.getId());
				val.setResult(null);

				src.getAttributes();
				
				if(result.getResultValues().size()!=ids.size()) throw new Exception("Cannot map data from "+src+"("+result.getResultValues().size()+"attributes left) to "+dest+"("+ids.size()+"attributes)");
				
				int i = 0;				
				for (ResultValue v : result.getResultValues()) {
					v.setId(ids.get(i));
				}
				
				result.setTest(dest);
			}
			
			txn.commit();
			return results.size();
		} catch (Exception e) {
			if(txn!=null) txn.rollback();
			throw e;
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}		
	}
	
	
	public static List<String> getRecentElbs(SpiritUser user) {
		EntityManager session = JPAUtil.getManager();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, cal.get(Calendar.MONTH)-1);
		
		return (List<String>) session.createQuery("select distinct(r.elb) from Result r where r.creUser = ?1 and r.creDate > ?2")
				.setParameter(1, user.getUsername())
				.setParameter(2, cal.getTime())
			.getResultList();
	}
	
	public static enum FindDuplicateMethod {
		RETURN_ALL("Return ALL duplicated results"),
		RETURN_FIRST_ELB("Return the FIRST result (from the first elb or first id)"),
		RETURN_EXCEPT_FIRST_ELB("Return all except the FIRST result (from the first elb or first id)"),
		RETURN_OLDEST_2MNS("Return the OLDEST results (and at least 2mns older)"),
		RETURN_NEWEST_2MNS("Return the NEWEST results (and at least 2mns newer)");
		
		private String desc;
		private FindDuplicateMethod(String desc) {this.desc = desc;}
		@Override
		public String toString() {return desc;}
		
	}
	
	public static List<Result> findDuplicates(String elb1, String elb2, FindDuplicateMethod method, SpiritUser user) throws Exception {
		
		Set<Result> res = new HashSet<Result>();
		//Load Experiments
		List<Result> results1 = queryResults(ResultQuery.createQueryForElb(elb1), user);
		List<Result> results2;
		if(elb1.equals(elb2)) {
			results2 = results1;
		} else {
			results2 = queryResults(ResultQuery.createQueryForElb(elb2), user);
		}
		
		if(results1.size()==0) throw new Exception("The elb "+elb1+" is invalid or you don't have any rights");
		if(results2.size()==0) throw new Exception("The elb "+elb2+" is invalid or you don't have any rights");
		 
		Set<Integer> keepIds = new HashSet<>();
		//Find duplicates
		firstLoop: for (int i1 = 0; i1 < results1.size(); i1++) {
			Result r1 = results1.get(i1);
			if(keepIds.contains(r1.getId())) continue;
			nextResult: for (int i2 = (elb1.equals(elb2)? i1+1: 0); i2 < results2.size(); i2++) {
				Result r2 = results2.get(i2);
				if(keepIds.contains(r2.getId())) continue;
				if(r1==r2) continue;
				if(r1.getTest()!=r2.getTest()) continue;
				if(r1.getBiosample()!=r2.getBiosample()) continue;
				if(r1.getInheritedPhase()!=r2.getInheritedPhase()) continue;
				
				boolean differentOutput = false;
				for (ResultValue rv1 : r1.getResultValues()) {
					String v1 = rv1.getValue();
					ResultValue rv2 = r2.getResultValue(rv1.getAttribute());
					if(rv2==null) continue nextResult;
					
					String v2 = rv2.getValue();
					if(rv1.getAttribute().getOutputType()==OutputType.OUTPUT) { //Output
						if((v1==null && v2==null) || (v1!=null && v1.equals(v2))) {
							//Same output parameter
						} else {
							differentOutput = true;
						}
					} else if(rv1.getAttribute().getOutputType()==OutputType.INPUT) { //Input
						if((v1==null && v2==null) || (v1!=null && v1.equals(v2))) {
							//Same Input Parameter
						} else {
							continue nextResult;
						}
					}
					
				}
				
				if(differentOutput) {
					//Ok: duplicate measurements with different output
				} else {
					//same results
					long diffSec = (r1.getCreDate().getTime() - r2.getCreDate().getTime()) / 1000;
					if(method==FindDuplicateMethod.RETURN_ALL) {
						res.add(r1);
						res.add(r2);
					} else if(method==FindDuplicateMethod.RETURN_FIRST_ELB) {
						res.add(r1);	
						keepIds.add(r2.getId());
						continue firstLoop;
					} else if(method==FindDuplicateMethod.RETURN_EXCEPT_FIRST_ELB) {
						res.add(r2);	
						keepIds.add(r1.getId());
					} else if(method==FindDuplicateMethod.RETURN_NEWEST_2MNS) {
						if(diffSec>2*60) {
							res.add(r1);
							continue firstLoop;
						} else if(diffSec<-2*60) {
							res.add(r2);
							continue firstLoop;
						}
					} else if(method==FindDuplicateMethod.RETURN_OLDEST_2MNS) {
						if(diffSec>2*60) {
							res.add(r2);
							continue firstLoop;
						} else if(diffSec<-2*60) {
							res.add(r1);
							continue firstLoop;
						}
					} else {
						throw new IllegalArgumentException("Invalid method: "+method);
					}
					
				}
				
			}			
		}
		logger.debug("ELB1 has "+results1.size()+" results / ELB2 has "+results1.size()+" results / Duplicates = "+res.size());
		
		List<Result> list = new ArrayList<>(res);
		
		
		return list;
		
	}
	
	
	public static class ElbLink implements Comparable<ElbLink>{
		String elb;
		boolean inSpirit;		
		URL url;
		String title;
		String scientist;
		Date creDate;
		Date pubDate;
		
		
		public String getElb() {
			return elb;
		}
		public boolean isInSpirit() {
			return inSpirit;
		}
		public boolean isInNiobe() {
			return title!=null;
		}
		public URL getUrl() {
			return url;
		}
		public String getTitle() {
			return title;
		}
		public String getScientist() {
			return scientist;
		}
		public Date getCreDate() {
			return creDate;
		}
		public Date getPubDate() {
			return pubDate;
		}
		
		@Override
		public String toString() {
			return elb+": "+(title!=null?title:"") +(scientist!=null? " " + scientist:"") + (pubDate!=null?" Pub:"+pubDate: "");
		}
		
		@Override
		public int compareTo(ElbLink o) {
			return CompareUtils.compare(elb, o.elb);
		}
	}
	
	public static List<String> getElbsForStudy(String studyIds) {
		if(studyIds==null || studyIds.length()==0) return new ArrayList<String>();
		
		EntityManager session = JPAUtil.getManager();
		try {
			List<String> res = (List<String>) session.createQuery("select distinct(r.elb) from Result r where " + QueryTokenizer.expandOrQuery("r.biosample.inheritedStudy.studyId = ?", studyIds)).getResultList();
			Collections.sort(res);
			return res;
		} catch(Exception e) {
			e.printStackTrace();
			return new ArrayList<String>();
		}
	}
	
	public static List<ElbLink> getNiobeLinksForStudy(Study study) {
		if(!DBAdapter.getAdapter().isInActelionDomain()) return new ArrayList<>();
		List<ElbLink> res = (List<ElbLink>) Cache.getInstance().get("elb_links_"+study.getId());
		if(res==null) {
			Map<String, ElbLink> map = new HashMap<>();
			res = new ArrayList<>();
			
			//Load elbs accessible in Niobe
			Connection conn = null;
			try {
				conn = DBAdapter.getAdapter().getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select displayname, labjournal, title, scientist, createdate, sealdate" +
						" from niobe.references ref, niobe.documents doc" +
						" where path = '" + study.getStudyId() + "' and doc.id = ref.docid");
				while(rs.next()) {
					String elb = rs.getString("labjournal");
					ElbLink elbLink = map.get(elb);
					if(elbLink==null) {
						elbLink = new ElbLink();
						elbLink.elb = elb;
						res.add(elbLink);
						map.put(elb, elbLink);
					}
					
					elbLink.title = rs.getString("title");
					elbLink.scientist = rs.getString("scientist");
					elbLink.creDate = rs.getDate("createdate");
					elbLink.pubDate = rs.getDate("sealdate");
					
					if(elbLink.pubDate!=null) {
						elbLink.url = new URL("http://ares:8080/portal/jsp/displayniobepdf.jsp?labJournal="+elb);
					}
				}
				rs.close();
				stmt.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(conn!=null) try{conn.close();}catch (Exception e2) {}
			}			
		}
		Cache.getInstance().add("elb_links_"+study.getId(), res, 60);
		return res;
	}
	

	public static void attachOrCreateStudyResultsToTops(Study study) throws Exception {
		attachOrCreateStudyResultsToTops(study, null, null, null);
	}
			
	/**
	 * Load the results of Weighing, FoodWater, Observation, extraMesurements that could be performed on the top specimen
	 * @param study
	 * @param allBiosamples
	 * @param phaseFilter
	 * @param createMissingOnes
	 * @throws Exception
	 */
	public static void attachOrCreateStudyResultsToTops(Study study, Collection<Biosample> allBiosamples, Phase phaseFilter, String elbForCreatingMissingOnes) throws Exception  {
		if(allBiosamples==null) allBiosamples = study.getAttachedBiosamples();
		Test weighingTest = DAOTest.getTest(DAOTest.WEIGHING_TESTNAME);
		Test fwTest = DAOTest.getTest(DAOTest.FOODWATER_TESTNAME);
		Test obsTest = DAOTest.getTest(DAOTest.OBSERVATION_TESTNAME);
		if(weighingTest==null || fwTest==null || obsTest==null) throw new Exception("You must create the tests: " + DAOTest.WEIGHING_TESTNAME + "(output=weight [g]), " + DAOTest.FOODWATER_TESTNAME + "(output=[food [g], water [ml]]) , " + DAOTest.OBSERVATION_TESTNAME + "(output=observation)");
		
		List<Test> tests = new ArrayList<>();
		tests.add(weighingTest);
		tests.add(fwTest);
		tests.add(obsTest);
		tests.addAll(Measurement.getTests(study.getAllMeasurementsFromActions()));
		attachOrCreateStudyResults(study, false, allBiosamples, tests, phaseFilter, elbForCreatingMissingOnes);
	}
	
	
	public static void attachOrCreateStudyResultsToSamples(Study study) throws Exception {
		attachOrCreateStudyResultsToSamples(study, null, null, null);
	}
	
	/**
	 * Load the results of Weighing, Length, Observation that could be performed on the samples
	 * @param study
	 * @param samples
	 * @param phaseFilter
	 * @param createMissingOnes
	 * @throws Exception
	 */
	public static void attachOrCreateStudyResultsToSamples(Study study, Collection<Biosample> allBiosamples, Phase phaseFilter, String elbForCreatingMissingOnes) throws Exception  {
		if(allBiosamples==null) allBiosamples = study.getAttachedBiosamples();
		Test weighingTest = DAOTest.getTest(DAOTest.WEIGHING_TESTNAME);
		Test lengthTest = DAOTest.getTest(DAOTest.LENGTH_TESTNAME);
		Test obsTest = DAOTest.getTest(DAOTest.OBSERVATION_TESTNAME);		
		if(weighingTest==null || lengthTest==null || obsTest==null) throw new Exception("You must create the tests: " + DAOTest.WEIGHING_TESTNAME + "(output=weight [g]), " + DAOTest.LENGTH_TESTNAME + "(output=length) , " + DAOTest.OBSERVATION_TESTNAME + "(output=observation)");

		List<Test> tests = new ArrayList<>();
		tests.add(weighingTest);
		tests.add(lengthTest);
		tests.add(obsTest);		
		tests.addAll(Measurement.getTests(study.getAllMeasurementsFromSamplings()));		
		attachOrCreateStudyResults(study, true, allBiosamples, tests, phaseFilter, elbForCreatingMissingOnes);
	}
	
	
	/**
	 * Load result from the DB or create new ones
	 * @param study
	 * @param allBiosamples
	 * @param phaseFilter - not null to return only results for the specified phase + the weight of the animal at this phase
	 * @param user
	 * @throws Exception
	 */
	private static void attachOrCreateStudyResults(Study study, boolean skipEmptyPhase, Collection<Biosample> biosamples, Collection<Test> tests, Phase phaseFilter, String elbForCreatingMissingOnes) throws Exception  {
		Set<Biosample> allBiosamples = new HashSet<Biosample>(biosamples);
		//Clean previous data
		if(phaseFilter!=null) {
			for (Biosample biosample : allBiosamples) {
				biosample.clearAuxResults(phaseFilter);
			}
		}
		
		
		//Query all results associated to those samples		
		ResultQuery q = new ResultQuery();
		q.setQuality(null);
//		q.setElbs(study.getStudyId());
		q.setBids(JPAUtil.getIds(allBiosamples));
		q.getTestIds().addAll(JPAUtil.getIds(tests));

		if(phaseFilter!=null) {
			q.setPhase(phaseFilter);
		}
		List<Result> results = queryResults(q, null);
		
		//Map the result to the associated biosample
		Map<Integer,Biosample> map = JPAUtil.mapIds(allBiosamples);
		for (Result result : results) {
			Phase p = result.getInheritedPhase();
			
			if(p==null) continue;
			if(phaseFilter==null && skipEmptyPhase && result.getBiosample().getInheritedPhase()==null) continue; //if no phase filter -> returns only samples
			if(phaseFilter!=null && !phaseFilter.equals(p)) continue; //if phase filter -> returns only results with samples and animals at this phase
			
			Biosample b = map.get(result.getBiosample().getId());
			if(b!=null) b.addAuxResult(result);
		}
		
		//Create missing results		
		if(elbForCreatingMissingOnes!=null) {
			for (Biosample biosample : allBiosamples) {
				Phase p = biosample.getInheritedPhase()!=null? biosample.getInheritedPhase(): phaseFilter;
	
				if(phaseFilter==null && skipEmptyPhase && biosample.getInheritedPhase()==null) continue; //if no phase filter -> returns only samples
				if(phaseFilter!=null && !phaseFilter.equals(p)) continue; //if phase filter -> returns only results with samples and animals at this phase
				
				for(Test test: tests) {
					if(biosample.getAuxResult(test, p)==null) {
						Result r = new Result(test);
						r.setElb(elbForCreatingMissingOnes);
						r.setBiosample(biosample);
						r.setPhase(biosample.getInheritedPhase()!=null? null: phaseFilter);
						biosample.addAuxResult(r);
					}				
				}			
			}
		}
		
	}
	

	
	public static void changeOwnership(Collection<Result> results, SpiritUser toUser, SpiritUser updater) throws Exception {
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			
			Date now = JPAUtil.getCurrentDateFromDatabase();
			for (Result r : results) {
				if(r.getId()<=0) continue;
				if(!SpiritRights.canDelete(r, updater)) throw new Exception(updater+" is not allowed to change the ownership of "+r);
				
				r.setUpdUser(updater.getUsername());
				r.setUpdDate(now);
				r.setCreUser(toUser.getUsername());
				session.merge(r);
			}

			txn.commit();			
			txn = null;
		} finally {
			if(txn!=null) try{txn.rollback();}catch (Exception e) {}
		}				
	}
		
	public static Collection<Biotype> getBiotypes(String studyIds, Set<Integer> testIds) throws Exception {
		EntityManager session = JPAUtil.getManager();
		
		if(studyIds==null || studyIds.trim().length()==0) return new HashSet<Biotype>();
		
		StringBuilder jpql = new StringBuilder(); 
		jpql.append("SELECT distinct(r.biosample.biotype) FROM Result r " +
				" where (" + QueryTokenizer.expandOrQuery("r.biosample.inheritedStudy.studyId = ?", studyIds) + ")");
		
		if(testIds!=null && testIds.size()>0) {
			jpql.append(" and (");
			boolean first = true;
			for (int testId : testIds) {
				if(first) first = false; else jpql.append(" or "); 
				jpql.append(" r.test.id = "+testId);
			}
			jpql.append(")");
		}
		
		
		
		Query jpaQuery = session.createQuery(jpql.toString());		
		List<Biotype> res = jpaQuery.getResultList();
				
		return res;
	}	
	
	/**
	 * Find similar results in the system and map them, by:
	 *  result.getTestBiosamplePhaseInputKey -> Result
	 * This function is useful to find if the DB already contains similar results (which is allowed for duplicates measurement)
	 * @param results
	 * @return
	 */
	public static Map<String, Result> findSimilarResults(Collection<Result> results) {
		Map<String, Result> mapKey2result = new HashMap<>();
		Set<Biosample> biosamples = Result.getBiosamples(results);
		if(results.size()==0 || biosamples.size()==0) return mapKey2result;
		
		//Build a query
		ResultQuery q = new ResultQuery();
		q.setSampleIds(MiscUtils.flatten(Biosample.getSampleIds(biosamples), " "));
		//Note: don't test for test.ids because those can be different in different instances
		
		//Find existing keys
		Set<String> keys = new HashSet<>();
		for (Result r : results) {
			keys.add(r.getTestBiosamplePhaseInputKey());
		}

		//Query results matching sampleIds, testIds (we will get a lot of results)
		List<Result> list;
		try { 
			list = DAOResult.queryResults(q, null);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		//Map only the matching results
		for (Result result : list) {
			String key = result.getTestBiosamplePhaseInputKey();
			if(keys.contains(key)) {
				mapKey2result.put(key, result);
				logger.debug("Found "+key+" "+result);
			} else {
				logger.debug("Skip "+key+" "+result);
			}
		}

		return mapKey2result;
	}

	
}
