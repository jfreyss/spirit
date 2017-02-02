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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;

@SuppressWarnings("unchecked")
/**
 * Important: don't use Cache, as it may be not be thread safe
 */
public class DAOBiotype {

	private static Logger logger = LoggerFactory.getLogger(DAOBiotype.class);

	public static void deleteBiotype(Biotype biotype, SpiritUser user) throws Exception {
		if(user==null || !user.isSuperAdmin()) throw new Exception("You must be am admin");
		if(biotype==null) throw new Exception("Biotype is null");
		if(biotype.getChildren().size()>0) throw new Exception("You must delete the children first: "+biotype.getChildren());
		EntityManager session = JPAUtil.getManager();
		BiosampleQuery q = new BiosampleQuery();
		q.setBiotype(biotype);
		int n = DAOBiosample.queryBiosamples(session, q, null).size();
		if(n>0) {
			throw new Exception("You cannot delete a biotype if you have some biosamples");
		}
		
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			if(!session.contains(biotype)) {
				biotype = session.merge(biotype);
			}
			session.remove(biotype);
			
			txn.commit();
			txn = null;
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {}
			Cache.getInstance().removeAllWithPrefix("id2biotype_"+JPAUtil.getManager());
		}
		
	}

	public static Map<Integer, Biotype> getId2Biotype() {
		Map<Integer, Biotype> id2biotype = (Map<Integer, Biotype>) Cache.getInstance().get("id2biotype_"+JPAUtil.getManager());
		if(id2biotype==null) {			
			EntityManager session = JPAUtil.getManager();
			//Load all
			Query query = session.createQuery("select distinct(t) from Biotype t left join fetch t.metadata");		
			
			List<Biotype> biotypes = query.getResultList();
			id2biotype = JPAUtil.mapIds(biotypes);
			Cache.getInstance().add("id2biotype_"+JPAUtil.getManager(), id2biotype, 180);
		}
		return id2biotype;
	}
	
	public static List<Biotype> getBiotypes() {
		return getBiotypes(false);
	}
	
	public static List<Biotype> getBiotypes(boolean showHidden) {
		Map<Integer, Biotype> id2biotype = getId2Biotype();
		List<Biotype> todo = new ArrayList<>(id2biotype.values());
		Collections.sort(todo);
		
		//Order according to the hierarchy
		List<Biotype> types = new ArrayList<>();
		while(todo.size()>0) {
			for (Iterator<Biotype> iterator = todo.iterator(); iterator.hasNext();) {
				Biotype biotype = iterator.next();
				if(biotype.getParent()==null) {
					biotype.setDepth(0);
					types.add(biotype);
					iterator.remove();
				} else {
					int index = types.indexOf(biotype.getParent());
					if(index>=0) {
						int depth = types.get(index).getDepth() + 1;
						index++;
						while(index<types.size() && biotype.getParent().equals(types.get(index).getParent())) {
							index++;
						}
						biotype.setDepth(depth);
						types.add(index, biotype);
						iterator.remove();
					}
				}
			}
		}
		if(!showHidden) {
			types = Biotype.removeHidden(types);
		}		
		return types;
	}	

	public static Biotype getBiotype(String name) {
		if(name==null) return null;
		
		Map<Integer, Biotype> id2biotype = getId2Biotype();
		for (Biotype t : id2biotype.values()) {
			if(t.getName().equals(name)) return t;
			
		}
		return null;
	}

	public static Set<String> getAutoCompletionFields(BiotypeMetadata metadataType, Study study) {
		if(metadataType==null || metadataType.getId()<=0) return new TreeSet<String>();
	
		//Use Cache
		String key = "biotype_autocompletion_"+metadataType.getId()+"_"+(study==null?"": study.getId());
		Set<String> res = (Set<String>) Cache.getInstance().get(key);
		if(res==null) {
			res = new TreeSet<String>(CompareUtils.STRING_COMPARATOR);
			try { 
				EntityManager session = JPAUtil.getManager();
				int id = metadataType.getId();
				//Select first 500 non empty rows
				Query query = session.createQuery("select distinct(b.serializedMetadata) from Biosample b "
						+ " WHERE concat(';', b.serializedMetadata, '%') like '%;"+id+"=%'"
						+ " AND NOT concat(';', b.serializedMetadata, '%') like '%;"+id+"=;%'"
						+ (study!=null? " AND b.inheritedStudy = ?2 ":""));
				if(study!=null) query.setParameter(2, study);
				query.setMaxResults(3000);
				for (String s : (List<String>) query.getResultList()) {
					String tok = MiscUtils.deserializeIntegerMap(s).get(id);
					if(tok!=null) res.add(tok);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			Cache.getInstance().add(key, res, 60);
		}
		return res;
	}

	public static Set<String> getAutoCompletionFieldsForSampleId(Biotype biotype) {
		if(biotype==null || biotype.getId()<=0) return new TreeSet<String>();
		
		//Use Cache
		String key = "biotype_autocompletion_sampleid_"+biotype.getId();
		Set<String> res = (Set<String>) Cache.getInstance().get(key);
		if(res==null) {			
			EntityManager session = JPAUtil.getManager();
			Query query = session.createQuery("SELECT distinct(b.sampleId) FROM Biosample b WHERE b.biotype = ?1");
			query.setParameter(1, biotype);
			query.setMaxResults(3000);
			res = new TreeSet<String>(CompareUtils.STRING_COMPARATOR);
			res.addAll(query.getResultList());
			
			Cache.getInstance().add(key, res, Cache.FAST);
		}
		return res;
	}

	public static Set<String> getAutoCompletionFieldsForName(Biotype biotype, Study study) {
		
		if(biotype==null || biotype.getId()<=0) return new TreeSet<String>();
		
		//Use Cache
		String key = "biotype_autocompletion_name_"+biotype.getId()+"_"+(study==null?"": study.getId());
		Set<String> res = (Set<String>) Cache.getInstance().get(key);
		if(res==null) {
			
			EntityManager session = JPAUtil.getManager();
			Query query = session.createQuery(
					"SELECT distinct(b.name) FROM Biosample b WHERE b.biotype = ?1 AND length(b.name)>0  " +
					(study!=null? " AND b.inheritedStudy = ?2 ":""));
			query.setParameter(1, biotype);
			if(study!=null) query.setParameter(2, study);
			query.setMaxResults(3000);
			res = new TreeSet<String>(CompareUtils.STRING_COMPARATOR);
			res.addAll(query.getResultList());
			
			
			Cache.getInstance().add(key, res, Cache.FAST);
		}
		return res;
	}

	public static Set<String> getAutoCompletionFieldsForComments(Biotype biotype, Study study) {
		
		if(biotype==null || biotype.getId()<=0) return new TreeSet<String>();
		
		//Use Cache
		String key = "biotype_autocompletion_comments_"+biotype.getId()+"_"+(study==null?"": study.getId());
		Set<String> res = (Set<String>) Cache.getInstance().get(key);
		if(res==null) {
			
			EntityManager session = JPAUtil.getManager();
			Query query = session.createQuery(
					"SELECT distinct(b.comments) FROM Biosample b WHERE b.biotype = ?1 AND length(b.comments)>0 " +
					(study!=null? " AND b.inheritedStudy = ?2 ":""));
			query.setParameter(1, biotype);
			if(study!=null) query.setParameter(2, study);
			query.setMaxResults(3000);
			res = new TreeSet<String>(CompareUtils.STRING_COMPARATOR);
			res.addAll(query.getResultList());
			
			Cache.getInstance().add(key, res, Cache.FAST);
		}
		return res;
	}

	public static void persistBiotype(Biotype biotype, SpiritUser user) throws Exception {
		if(biotype==null) return;
		persistBiotypes(Collections.singletonList(biotype), user);
	}
	

	public static void persistBiotypes(Collection<Biotype> biotypes, SpiritUser user) throws Exception {
		if(biotypes==null || biotypes.size()==0) return;
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			persistBiotypes(session, biotypes, user);
			txn.commit();
			txn = null;
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {}
		}		
	}
	
	public static void persistBiotypes(EntityManager session, Collection<Biotype> biotypes, SpiritUser user) throws Exception {
		logger.info("Persist "+biotypes.size()+" biotypes");
		
		if(biotypes==null || biotypes.size()==0) return;
		if(user==null || !user.isSuperAdmin()) throw new Exception("Only an admin can save a biotype");
		

		Date now = JPAUtil.getCurrentDateFromDatabase();
		for (Biotype biotype : biotypes) {
				
			if(biotype.getCategory()==null) throw new Exception("The category is required");
			if(biotype.getName()==null || biotype.getName().length()==0) throw new Exception("The name is required");
			
			//Update the bidirectional link
			for (BiotypeMetadata m : biotype.getMetadata()) {
				if(m.getName().trim().length()==0) throw new Exception("The Metadata name cannot be empty");
				if(m.getDataType()==null) throw new Exception("The Metadata datatype cannot be empty");
				m.setBiotype(biotype);
			}
			
			//Set a prefic if it is null or non empty
			if(biotype.getPrefix()==null || biotype.getPrefix().length()==0) {
				biotype.setPrefix(biotype.getName().substring(0, Math.min(3, biotype.getName().length())));
			}
						
			biotype.setUpdDate(now);
			biotype.setUpdUser(user.getUsername());
			
			if(biotype.getId()<=0) {
				logger.debug("Persist "+biotype+": Insert");
				session.persist(biotype);
				biotype.setCreDate(biotype.getUpdDate());
				biotype.setCreUser(biotype.getUpdUser());
			} else {
				logger.debug("Persist "+biotype+": Update");
				if(!session.contains(biotype)) {
					biotype = session.merge(biotype);
				}
			}
		}
		Cache.getInstance().remove("id2biotype_"+JPAUtil.getManager());

	}

	public static int renameNames(Biotype biotype, String value, String newValue, SpiritUser user) throws Exception {
		if(user==null || !user.isSuperAdmin()) throw new Exception("You must be an admin to rename a name");
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			
			List<Biosample> biosamples = (List<Biosample>) session.createQuery("select b from Biosample b where b.name = ?1")
				.setParameter(1, value)
				.getResultList();
			Date now = JPAUtil.getCurrentDateFromDatabase();
			for (Biosample b : biosamples) {
				b.setUpdDate(now);
				b.setUpdUser(user.getUsername());
				b.setSampleName(newValue);
				session.merge(b);
			}
			
			txn.commit();
			return biosamples.size();
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
			Cache.getInstance().remove("id2biotype_"+JPAUtil.getManager());
		}		
	}
	
	public static int renameMetadata(BiotypeMetadata att, String value, String newValue, SpiritUser user) throws Exception {
		if(user==null || !user.isSuperAdmin()) throw new Exception("You must be an admin to rename a metadata");
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			
			List<Biosample> biosamples = (List<Biosample>) session.createQuery("from Biosample b"
					+ " WHERE concat(';', b.serializedMetadata, '%') like '%;"+att.getId()+"=" + value.replace("'", "''") + ";%'").getResultList();
			Date now = JPAUtil.getCurrentDateFromDatabase();
			for (Biosample b : biosamples) {
				if(b.getMetadataValue(att).equals(value)) {
					b.setUpdDate(now);
					b.setUpdUser(user.getUsername());
					b.setMetadataValue(att, newValue);
				}
			}
			
			Cache.getInstance().remove("id2biotype_"+JPAUtil.getManager());
			txn.commit();
			return biosamples.size();
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}		
	}
	
	public static void moveNameToMetadata(Biotype biotype, SpiritUser user) throws Exception {
		if(user==null || !user.isSuperAdmin()) throw new Exception("You must be an admin to rename a metadata");
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			Date now = JPAUtil.getCurrentDateFromDatabase();

			//Add the metadata
			String newMetadata = biotype.getSampleNameLabel();
			if(biotype.getMetadata(newMetadata)!=null) throw new Exception(newMetadata+" is already a metadata");
			BiotypeMetadata bm = new BiotypeMetadata(newMetadata, biotype.isNameAutocomplete()? DataType.AUTO: DataType.ALPHA);
			bm.setBiotype(biotype);
			bm.setRequired(biotype.isNameRequired());
			biotype.getMetadata().add(bm);
			biotype = session.merge(biotype);
			biotype.setUpdDate(now);
			biotype.setUpdUser(user.getUsername());
			
			//Query and Update the biosamples
			BiosampleQuery q = new BiosampleQuery();
			q.setBiotype(biotype);
			List<Biosample> biosamples = DAOBiosample.queryBiosamples(session, q, null);
			for(Biosample b: biosamples) {
				b = session.merge(b);
				b.setMetadataValue(newMetadata, b.getSampleName());
				b.setUpdUser(user.getUsername());
				b.setUpdDate(now);
			}
			
			///Remove the name
			biotype.setSampleNameLabel(null);
			
			Cache.getInstance().remove("id2biotype_"+JPAUtil.getManager());
			txn.commit();
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}	
		
	}
	
	public static void moveMetadataToName(BiotypeMetadata biotypeMetadata, SpiritUser user) throws Exception {
		if(user==null || !user.isSuperAdmin()) throw new Exception("You must be an admin to rename a metadata");
		
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			Biotype biotype = biotypeMetadata.getBiotype();
//			if(!session.contains(biotype)) {
//				biotype = session.merge(biotype);
//				biotypeMetadata = biotype.getMetadata(biotypeMetadata.getName());
//			}
			if(biotype.getSampleNameLabel()!=null) throw new Exception(biotype+" has already a MainField");


			txn = session.getTransaction();
			txn.begin();
			Date now = JPAUtil.getCurrentDateFromDatabase();

			//Add the name
			biotype.setSampleNameLabel(biotypeMetadata.getName());
			biotype.setNameAutocomplete(biotypeMetadata.getDataType()==DataType.AUTO);
			biotype.setNameRequired(biotypeMetadata.isRequired());
			biotype.setUpdDate(now);
			biotype.setUpdUser(user.getUsername());
			
			
			//Query and Update the biosamples
			BiosampleQuery q = new BiosampleQuery();
			q.setBiotype(biotype);
			List<Biosample> biosamples = DAOBiosample.queryBiosamples(session, q, null);
			for(Biosample b: biosamples) {
				String m = b.getMetadataValue(biotypeMetadata);
				if(m!=null) {
					b = session.merge(b);
					b.setSampleName(m);
					b.setUpdUser(user.getUsername());
					b.setUpdDate(now);
				}
			}
			
			///Remove the metadata
			biotype.getMetadata().remove(biotype.getMetadata(biotypeMetadata.getName()));
			biotype = session.merge(biotype);
			Cache.getInstance().remove("id2biotype_"+JPAUtil.getManager());

			txn.commit();
			
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}	
		
	}
	
}
