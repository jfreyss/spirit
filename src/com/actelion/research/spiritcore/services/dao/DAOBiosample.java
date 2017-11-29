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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.ValidationException;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.LocPos;
import com.actelion.research.spiritcore.business.biosample.LocationFormat;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationLabeling;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.helper.ExpressionHelper;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.QueryTokenizer;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.FormatterUtils;

import net.objecthunter.exp4j.Expression;

/**
 * DAO functions linked to biosmamples
 *
 * @author Joel Freyss
 */
public class DAOBiosample {

	private static Logger logger = LoggerFactory.getLogger(DAOBiosample.class);

	public static Biosample getBiosampleById(int id) {
		String hql = "select b from Biosample b where b.id = id";
		EntityManager session = JPAUtil.getManager();
		List<Biosample> l = session.createQuery(hql).getResultList();
		return l.size()==1? l.get(0): null;
	}

	/**
	 * Load a Biosample (fast, not fully loaded)
	 *
	 * @param sampleId
	 * @return
	 */
	public static Biosample getBiosample(String sampleId) {
		return getBiosamplesBySampleIds(Collections.singleton(sampleId)).get(sampleId);
	}

	public static Map<String, Biosample> getBiosamplesBySampleIds(Collection<String> sampleIds) {
		Map<String, Biosample> res = new HashMap<>();
		if(sampleIds.size()==0) return res;
		EntityManager session = JPAUtil.getManager();
		for (Biosample b : (List<Biosample>) session.createQuery("select b from Biosample b where " + QueryTokenizer.expandForIn("b.sampleId", sampleIds)).getResultList()) {
			res.put(b.getSampleId(), b);
		}
		return res;
	}

	public static Map<String, Integer> getIdFromSampleIds(Collection<String> sampleIds) {
		Map<String, Integer> res = new HashMap<>();
		if(sampleIds.size()==0) return res;
		EntityManager session = JPAUtil.getManager();
		for (Object[] objects : (List<Object[]>) session.createQuery("select b.sampleId, id from Biosample b where " + QueryTokenizer.expandForIn("b.sampleId", sampleIds)).getResultList()) {
			res.put((String)objects[0], (Integer) objects[1]);
		}
		return res;
	}

	public static Map<String, Biosample> getBiosampleByContainerIds(Collection<String> containerIds) {
		Map<String, Biosample> res = new HashMap<>();
		if(containerIds.size()==0) return res;

		Set<String> toSearch = new HashSet<>(containerIds);
		EntityManager session = JPAUtil.getManager();
		for (Biosample b : (List<Biosample>) session.createQuery("select b from Biosample b where " + QueryTokenizer.expandForIn("b.container.containerId", toSearch)).getResultList()) {
			res.put(b.getContainerId(), b);
			toSearch.remove(b.getContainerId());
		}
		if(toSearch.size()>0) {
			for (Biosample b : (List<Biosample>) session.createQuery("select b from Biosample b where " + QueryTokenizer.expandForIn("b.sampleId", toSearch)).getResultList()) {
				res.put(b.getSampleId(), b);
			}
		}
		return res;
	}

	/**
	 * Get Simple or multiple containers
	 * @param containerIds
	 * @return
	 */
	public static List<Container> getContainers(Collection<String> containerIds) {
		try {
			return Biosample.getContainers(queryBiosamples(BiosampleQuery.createQueryForContainerIds(containerIds), null), true);
		} catch(Exception e) {
			throw new RuntimeException(e);//should not happen
		}
	}

	public static Container getContainer(String containerId) {
		if(containerId==null) return null;
		List<Container> containers = getContainers(Collections.singletonList(containerId));
		return containers.size()==1? containers.get(0): null;
	}

	public static Biosample getBiosample(Study study, String animalIdOrNo) {
		List<Biosample> biosamples = getBiosamples(study, Collections.singletonList(animalIdOrNo));
		return biosamples.size()==1? biosamples.get(0): null;
	}

	public static List<Biosample> getBiosamples(Study study, List<String> animalIdOrNo) {
		EntityManager session = JPAUtil.getManager();
		Query query;
		List<Biosample> biosamples = new ArrayList<>();

		for (int offset = 0; offset < animalIdOrNo.size(); offset += 500) {
			StringBuilder sb = new StringBuilder();
			for (int i = offset; i < animalIdOrNo.size() && i < offset + 500; i++) {
				sb.append((sb.length() > 0 ? "," : "") + "'" + (animalIdOrNo.get(i).replace("'", "''")) + "'");
			}
			if (study == null) {
				query = session.createQuery("select b from Biosample b where b.sampleId in (" + sb + ")");
			} else {
				query = session.createQuery("select b from Biosample b where (b.sampleId in (" + sb + ") or b.name in (" + sb + ")) and b.inheritedStudy = ?1").setParameter(1, study);
			}
			//			query.setHint("org.hibernate.readOnly", !JPAUtil.isEditableContext());
			biosamples.addAll(query.getResultList());
		}
		return biosamples;
	}

	/**
	 * Query without sorting
	 *
	 * @param q
	 * @param user
	 * @return
	 */
	public static List<Biosample> queryBiosamples(BiosampleQuery q, SpiritUser user) throws Exception {
		return queryBiosamples(JPAUtil.getManager(), q, user);
	}

	public static List<Biosample> queryBiosamples(EntityManager session, BiosampleQuery q, SpiritUser user) throws Exception {
		assert q!=null;


		StringBuilder clause = new StringBuilder();
		List<Object> parameters = new ArrayList<>();

		long start = System.currentTimeMillis();
		List<Entry<BiosampleLinker, String>> postprocessFilters = new ArrayList<>();

		if ((q.getBiotypes() == null || q.getBiotypes().length == 0) && user != null && !user.isSuperAdmin()) {
			clause.append(" and b.biotype.isHidden = false");
		}

		if (q.getSampleIdOrContainerIds() != null && q.getSampleIdOrContainerIds().length() > 0) {
			clause.append(" and (" + QueryTokenizer.expandForIn("b.sampleId", q.getSampleIdOrContainerIds()));
			clause.append(" or " + QueryTokenizer.expandForIn("b.container.containerId", q.getSampleIdOrContainerIds()));
			clause.append(")");
		} else {

			if (q.getBids() != null && q.getBids().size() > 0) {
				clause.append(" and " + QueryTokenizer.expandForIn("b.id", q.getBids()));
			}
			if (q.getSids() != null && q.getSids().size() > 0) {
				clause.append(" and " + QueryTokenizer.expandForIn("b.inheritedStudy.id", q.getSids())) ;
			}
			if (q.getSampleIds() != null && q.getSampleIds().length() > 0) {
				clause.append(" and " + QueryTokenizer.expandForIn("b.sampleId", q.getSampleIds()));
			}
			if (q.getSampleId() != null && q.getSampleId().length() > 0) {
				clause.append(" and b.sampleId = ?");
				parameters.add(q.getSampleId());
			}
			if (q.getParentSampleIds() != null && q.getParentSampleIds().length() > 0) {
				clause.append(" and (" + QueryTokenizer.expandOrQuery("b.parent.sampleId = ?", q.getParentSampleIds()) + ")");
			}
			if (q.getTopSampleIds() != null && q.getTopSampleIds().length() > 0) {
				clause.append(" and (" + QueryTokenizer.expandOrQuery("b.topParent.sampleId = ?", q.getTopSampleIds()) + ")");
			}
			if (q.getContainerIds() != null && q.getContainerIds().length() > 0) {
				clause.append(" and " + QueryTokenizer.expandForIn("b.container.containerId", q.getContainerIds()));
			}

			if (q.getElbs() != null && q.getElbs().length() > 0) {
				clause.append(" and (" + QueryTokenizer.expandOrQuery("lower(b.elb) like lower(?)", q.getElbs()) + ")");
			}

			if (q.getSampleNames() != null && q.getSampleNames().length() > 0) {
				clause.append(" and (" + QueryTokenizer.expandOrQuery("lower(b.name) like lower(?)", q.getSampleNames()) + ")");
			}

			if (q.getKeywords() != null && q.getKeywords().length() > 0) {
				StringBuilder expr = new StringBuilder();
				expr.append(" \n(");
				expr.append(" (b.id in (select b2.id from Biosample b2 where lower(b2.inheritedStudy.studyId) like lower(?) or lower(b2.inheritedStudy.localId) like lower(?)))");
				expr.append(" or (b.id in (select b2.id from Biosample b2 where lower(b2.inheritedGroup.name) like lower(?)))");
				expr.append(" or (b.id in (select b2.id from Biosample b2 where lower(b2.inheritedPhase.name) like lower(?)))");
				expr.append(" or (b.id in (select b2.id from Biosample b2 where lower(b.parent.biotype.name) like lower(?)))");
				expr.append(" or (b.id in (select b2.id from Biosample b2 where lower(b.topParent.biotype.name) like lower(?)))");
				expr.append(" or lower(b.biotype.name) like lower(?)");
				expr.append(" or b.sampleId like ?");
				expr.append(" or b.container.containerId like ?");
				expr.append(" or replace(replace(replace(replace(replace(replace(replace(lower(b.name), '.', ''), ' ', ''), '-', ''), '_', ''), '/', ''), ':', ''), '#', '') like replace(replace(replace(replace(replace(replace(replace(lower(?), '.', ''), ' ', ''), '-', ''), '_', ''), '/', ''), ':', ''), '#', '')");
				expr.append(" or (b.id in (select b2.id from Biosample b2 where replace(replace(replace(replace(replace(replace(replace(lower(b2.parent.name), '.', ''), ' ', ''), '-', ''), '_', ''), '/', ''), ':', ''), '#', '') like replace(replace(replace(replace(replace(replace(replace(lower(?), '.', ''), ' ', ''), '-', ''), '_', ''), '/', ''), ':', ''), '#', '')))");
				expr.append(" or (b.id in (select b2.id from Biosample b2 where replace(replace(replace(replace(replace(replace(replace(lower(b2.topParent.name), '.', ''), ' ', ''), '-', ''), '_', ''), '/', ''), ':', ''), '#', '') like replace(replace(replace(replace(replace(replace(replace(lower(?), '.', ''), ' ', ''), '-', ''), '_', ''), '/', ''), ':', ''), '#', '')))");
				expr.append(" or lower(b.serializedMetadata) like lower(?) or lower(b.serializedMetadata2) like lower(?)");
				//				expr.append(" or (b.id in (select b2.id from Biosample b2 left outer join b2.linkedDocuments as d where d.bytes.bytes like lower(?)))");
				expr.append(" or (b.id in (select b2.id from Biosample b2 where lower(b2.parent.serializedMetadata) like lower(?)))");
				expr.append(" or (b.id in (select b2.id from Biosample b2 where lower(b2.topParent.serializedMetadata) like lower(?)))");
				expr.append(" or lower(b.comments) like lower(?)");
				expr.append(" or lower(b.creUser) like lower(?)");
				expr.append(" or lower(b.updUser) like lower(?)");
				expr.append(" or lower(b.elb) like lower(?)");
				expr.append(" or (b.id in (select b2.id from Biosample b2 where lower(b2.location.name) like lower(?)))");
				expr.append(" )\n");
				clause.append(" and (" + QueryTokenizer.expandQuery(expr.toString(), q.getKeywords(), true, true) + ")");
			}
			if (q.getStudyIds() != null && q.getStudyIds().equalsIgnoreCase("NONE")) {
				clause.append(" and b.inheritedStudy is null");
			} else if (q.getStudyIds() != null && q.getStudyIds().length() > 0) {
				clause.append(" and (" + QueryTokenizer.expandOrQuery("b.inheritedStudy.studyId = ?", q.getStudyIds()) + ")");
			}
			if (q.getGroup() != null && q.getGroup().length() > 0) {
				clause.append(" and ( b.id in (select b2.id from Biosample b2 where b2.inheritedGroup.name = ?) " + " or b.id in (select b2.id from Biosample b2 where b2.inheritedGroup.fromGroup.name = ?))");
				parameters.add(q.getGroup());
				parameters.add(q.getGroup());
			}
			if (q.getPhase()!=null) {
				clause.append(" and (b.id in (select b2.id from Biosample b2 where b2.inheritedPhase.id = " + q.getPhase().getId() + "))");
			}
			if (q.getBiotypes() != null && q.getBiotypes().length > 0) {
				clause.append(" and (");
				boolean first = true;
				for (Biotype biotype : q.getBiotypes()) {
					if (first)
						first = false;
					else
						clause.append(" or ");
					clause.append(" b.biotype.id = " + biotype.getId());

				}
				clause.append(") ");
			}
			if (q.getContainerType() != null) {
				clause.append(" and (b.container.containerType = ?)");
				parameters.add(q.getContainerType());
			}
			if(q.getLocationRoot()!=null) {
				Location l = session.merge(q.getLocationRoot());
				List<Location> locs = new ArrayList<Location>(l.getChildrenRec(8));
				clause.append(" and " + QueryTokenizer.expandForIn("b.location.id", JPAUtil.getIds(locs)));
			}
			if(q.getLocations()!=null && q.getLocations().size()>0) {
				clause.append(" and " + QueryTokenizer.expandForIn("b.location.id", JPAUtil.getIds(q.getLocations())));
			}
			if(q.getLocPoses()!=null && q.getLocPoses().size()>0) {
				clause.append(" and (");
				boolean first = true;
				for (LocPos lp : q.getLocPoses()) {
					if(first) first = false; else clause.append(" or ");
					clause.append("(b.location.id= "+lp.getLocation().getId()+" and b.pos = " + lp.getPos() + ")");

				}
				clause.append(")");
			}
			if (q.getComments() != null && q.getComments().length() > 0) {
				clause.append(" and (" + QueryTokenizer.expandQuery("lower(b.comments) like lower(?)", q.getComments(), true, true) + ")");
			}

			if (q.getDepartment() != null && q.getDepartment()!=null) {
				clause.append(" and b.group = ?");
				parameters.add(q.getDepartment());
			}

			if (q.getMinQuality() != null) {
				if (q.getMinQuality().getId() <= Quality.VALID.getId()) {
					clause.append(" and (b.quality is null or b.quality >= " + q.getMinQuality().getId() + ")");
				} else {
					clause.append(" and b.quality >= " + q.getMinQuality().getId());
				}
			}
			if (q.getMaxQuality() != null) {
				if (q.getMaxQuality().getId() >= Quality.VALID.getId()) {
					clause.append(" and (b.quality is null or b.quality <= " + q.getMaxQuality().getId() + ")");
				} else {
					clause.append(" and b.quality <= " + q.getMaxQuality().getId());
				}
			}
			if (q.getExpiryDateMin() != null) {
				clause.append(" and (b.expiryDate > ?)");
				parameters.add(q.getExpiryDateMin());
			}
			if (q.getExpiryDateMax() != null) {
				clause.append(" and (b.expiryDate <= ?)");
				parameters.add(q.getExpiryDateMax());
			}
			if (q.getUpdUser() != null && q.getUpdUser().length() > 0) {
				clause.append(" and b.updUser = ?");
				parameters.add(q.getUpdUser());
			}
			if (q.getUpdDate() != null && q.getUpdDate().length() > 0) {

				String modifier = MiscUtils.extractModifier(q.getUpdDate());
				Date date = MiscUtils.extractDate(q.getUpdDate());
				if (date != null && modifier.equals("=")) {
					clause.append(" and b.updDate between ? and ?");
					parameters.add(date);
					parameters.add(MiscUtils.addDays(date, 1));
				} else if (date != null && modifier.equals("<")) {
					clause.append(" and b.updDate < ?");
					parameters.add(MiscUtils.addDays(date, 0));
				} else if (date != null && modifier.equals("<=")) {
					clause.append(" and b.updDate < ?");
					parameters.add(MiscUtils.addDays(date, 1));
				} else if (date != null && modifier.equals(">=")) {
					clause.append(" and b.updDate > ?");
					parameters.add(MiscUtils.addDays(date, 0));
				} else if (date != null && modifier.equals(">")) {
					clause.append(" and b.updDate > ?");
					parameters.add(MiscUtils.addDays(date, 1));
				}
			}
			if (q.getCreUser() != null && q.getCreUser().length() > 0) {
				clause.append(" and b.creUser = ?");
				parameters.add(q.getCreUser());
			}
			if (q.getCreDays() != null && q.getCreDays().length() > 0) {
				String digits = MiscUtils.extractStartDigits(q.getCreDays());
				if (digits.length() > 0) {
					try {
						clause.append(" and b.creDate > ?");
						Calendar cal = Calendar.getInstance();
						cal.setTime(new Date());
						cal.add(Calendar.DAY_OF_YEAR, -Integer.parseInt(digits));
						parameters.add(cal.getTime());
					} catch (Exception e) {
					}
				}
			}
			if (q.getUpdDays() != null && q.getUpdDays().length() > 0) {
				String digits = MiscUtils.extractStartDigits(q.getUpdDays());
				if (digits.length() > 0) {
					try {
						clause.append(" and b.updDate > ?");
						Calendar cal = Calendar.getInstance();
						cal.setTime(new Date());
						cal.add(Calendar.DAY_OF_YEAR, -Integer.parseInt(digits));
						parameters.add(cal.getTime());
					} catch (Exception e) {
					}
				}
			}
			if (q.isFilterNotInContainer()) {
				clause.append(" and b.container.containerType is not null ");
			}
			if (q.isFilterNotInLocation()) {
				clause.append(" and b.location is not null ");
			}

			if (q.isFilterInStudy()) {
				clause.append(" and b.inheritedStudy is null ");
			}

			if (q.isFilterTrashed()) {
				clause.append(" and b.status <> ? and b.status <> ? ");
				parameters.add(Status.TRASHED);
				parameters.add(Status.USEDUP);
			}

			if (q.isSearchMySamples() && user != null) {
				clause.append(" and b.employeeGroup = ?");
				parameters.add(user.getMainGroup());
			}

			// Query based on the linkers
			for (Entry<BiosampleLinker, String> entry : q.getLinker2values().entrySet()) {
				BiosampleLinker linker = entry.getKey();
				String val = entry.getValue();
				if (val == null)
					continue;
				if (linker.getAggregatedMetadata() != null) {
					// Linker for aggregated biosamples
					long idAgg = linker.getAggregatedMetadata().getId();
					if (linker.getType() == LinkerType.SAMPLEID) {
						clause.append(" and (b.id in (select b2.id from Biosample b2 JOIN b2.linkedBiosamples b3 where key(b3) = " + idAgg + " and b3.sampleId = '" + QueryTokenizer.escapeForSQL(val) + "'))");
					} else if (linker.getType() == LinkerType.SAMPLENAME) {
						clause.append(" and (b.id in (select b2.id from Biosample b2 JOIN b2.linkedBiosamples b3 where key(b3) = " + idAgg + " and lower(b3.name) like lower('" + QueryTokenizer.escapeForSQL(val) + "')))");
					} else if (linker.getType() == LinkerType.COMMENTS) {
						clause.append(" and (b.id in (select b2.id from Biosample b2 JOIN b2.linkedBiosamples b3 where key(b3) = " + idAgg + " and  lower(b3.comments) like lower('" + QueryTokenizer.escapeForSQL(val) + "')))");
					} else if (linker.getType() == LinkerType.METADATA && linker.getBiotypeMetadata() != null && linker.getBiotypeMetadata().getDataType()==DataType.MULTI) {
						clause.append(" and (b.id in (select b2.id from Biosample b2 JOIN b2.linkedBiosamples b3 where key(b3) = " + idAgg + " and " + QueryTokenizer.expandQuery("concat(';', lower(b3.serializedMetadata), ';') like lower(?)", val, true, true) + "))");
					} else if (linker.getType() == LinkerType.METADATA && linker.getBiotypeMetadata() != null) {
						clause.append(" and (b.id in (select b2.id from Biosample b2 JOIN b2.linkedBiosamples b3 where key(b3) = " + idAgg + " and concat(';', lower(b3.serializedMetadata), ';') like lower('%" + QueryTokenizer.escapeForSQL(val) + "%')))");
					} else {
						postprocessFilters.add(entry);
						System.err.println("LinkerAgg " + linker + " not managed = "+val);
					}
				} else if (linker.getHierarchyBiotype() != null) {
					long tId = linker.getHierarchyBiotype().getId();
					if (linker.getType() == LinkerType.SAMPLEID) {
						clause.append(" and (b.topParent.id in (select b2.topParent.id from Biosample b2 where b2.biotype.id = " + tId + " and b2.sampleId = '" + QueryTokenizer.escapeForSQL(val) + "'))");
					} else if (linker.getType() == LinkerType.SAMPLENAME) {
						clause.append(" and (b.topParent.id in (select b2.topParent.id from Biosample b2 where b2.biotype.id = " + tId + " and lower(b2.name) like lower('" + QueryTokenizer.escapeForSQL(val) + "')))");
					} else if (linker.getType() == LinkerType.COMMENTS) {
						clause.append(" and (b.topParent.id in (select b2.topParent.id from Biosample b2 where b2.biotype.id = " + tId + " and lower(b2.comments) like lower('" + QueryTokenizer.escapeForSQL(val) + "')))");
					} else if (linker.getType() == LinkerType.METADATA && linker.getBiotypeMetadata() != null && linker.getBiotypeMetadata().getDataType()==DataType.MULTI) {
						//						clause.append(" and (b.topParent.id in (select b2.topParent.id from Biosample b2, IN(b2.metadataMap) m2 where b2.biotype.id = " + tId + " and m2.biotypeMetadata.id = " + linker.getBiotypeMetadata().getId() + " and " + QueryTokenizer.expandQuery("lower(m2.value) like lower(?)", val, true, true) + "))");
						clause.append(" and (b.topParent.id in (select b2.id from Biosample b2 where " + QueryTokenizer.expandQuery("concat(';', lower(b2.serializedMetadata), ';') like lower(?)", val, true, true) + "))");
					} else if (linker.getType() == LinkerType.METADATA && linker.getBiotypeMetadata() != null) {
						//						clause.append(" and (b.topParent.id in (select b2.topParent.id from Biosample b2, IN(b2.metadataMap) m2 where b2.biotype.id = " + tId + " and m2.biotypeMetadata.id = " + linker.getBiotypeMetadata().getId() + " and lower(m2.value) like lower('" + QueryTokenizer.escapeForSQL(val) + "')))");
						clause.append(" and (b.topParent.id in (select b2.id from Biosample b2 where concat(';', lower(b2.serializedMetadata), ';') like lower('%" + QueryTokenizer.escapeForSQL(val) + "%')))");
					} else {
						postprocessFilters.add(entry);
						System.err.println("LinkerHie " + linker + " not managed");
					}
				} else {
					if (linker.getType() == LinkerType.SAMPLEID) {
						clause.append(" and " + QueryTokenizer.expandOrQuery("b.sampleId = ?", val) + "");
					} else if (linker.getType() == LinkerType.SAMPLENAME) {
						clause.append(" and lower(b.name) like lower('" + QueryTokenizer.escapeForSQL(val) + "')");
					} else if (linker.getType() == LinkerType.COMMENTS) {
						clause.append(" and lower(b.comments) like lower('" + QueryTokenizer.escapeForSQL(val) + "')");
					} else if (linker.getType() == LinkerType.METADATA && linker.getBiotypeMetadata() != null && linker.getBiotypeMetadata().getDataType()==DataType.MULTI) {
						clause.append(" and (b.id in (select b2.id from Biosample b2 where " + QueryTokenizer.expandQuery("concat(';', lower(b2.serializedMetadata), ';') like lower(?)", val, true, true) + "))");
					} else if (linker.getType() == LinkerType.METADATA && linker.getBiotypeMetadata() != null) {
						clause.append(" and (b.id in (select b2.id from Biosample b2 where concat(';', lower(b2.serializedMetadata), ';') like lower('%" + QueryTokenizer.escapeForSQL(val) + "%')))");
					} else {
						postprocessFilters.add(entry);
						System.err.println("LinkerReg " + linker + " not managed = "+val);
					}
				}

			}
		}


		String jpql = "from Biosample b";
		if (clause.length() > 0) {
			assert clause.substring(0, 4).equals(" and");
			jpql += " where " + clause.substring(4);
		}
		jpql = JPAUtil.makeQueryJPLCompatible(jpql);

		//Execute query
		Query query = session.createQuery(jpql);
		for (int i = 0; i < parameters.size(); i++) {
			query.setParameter(1 + i, parameters.get(i));
		}
		List<Biosample> biosamples = query.getResultList();

		logger.debug("BiosampleQuery: queried in " + (System.currentTimeMillis() - start) + "ms : n="+biosamples.size());

		//Verify the metadata, as the search didn't check the exact fields
		loop: for (Iterator<Biosample> iterator = biosamples.iterator(); iterator.hasNext();) {
			Biosample biosample = iterator.next();
			for (Entry<BiosampleLinker, String> entry : q.getLinker2values().entrySet()) {
				if(!QueryTokenizer.matchQuery(entry.getKey().getValue(biosample), entry.getValue())) {
					iterator.remove();
					continue loop;
				}
			}
		}
		logger.debug("BiosampleQuery: filter1 in " + (System.currentTimeMillis() - start) + "ms : n="+biosamples.size());

		// Filter samples that should not be searchable (location = private )
		if (user != null) {
			for (Iterator<Biosample> iterator = biosamples.iterator(); iterator.hasNext();) {
				Biosample biosample = iterator.next();
				if (!SpiritRights.canRead(biosample, user)) {
					iterator.remove();
					continue;
				}
				if (q.isSearchMySamples() && !SpiritRights.canEdit(biosample, user)) {
					iterator.remove();
					continue;
				}
			}
		}

		if(q.getPhases()!=null && q.getPhases().length()>0) {
			Set<String> set = new HashSet<>(Arrays.asList(MiscUtils.split(q.getPhases(), MiscUtils.SPLIT_SEPARATORS_WITH_SPACE)));
			if(set.size()>0) {
				for (Iterator<Biosample> iterator = biosamples.iterator(); iterator.hasNext();) {
					Biosample b = iterator.next();
					if(b.getInheritedPhase()==null || !set.contains(b.getInheritedPhase().getShortName())) {
						iterator.remove();
					}
				}
			}
		}

		// Apply the Select-One query
		if (q.getSelectOneMode() == BiosampleQuery.SELECT_MOST_RIGHT) {
			Map<Biosample, Biosample> top2Sel = new HashMap<>();
			for (Biosample b : biosamples) {
				Container c = b.getContainer();
				if (c == null || c.getPos() < 0)
					continue;
				Biosample top = b.getTopParent();
				Biosample sel = top2Sel.get(top);

				if (sel == null || (b.getCol() > sel.getCol())) {
					top2Sel.put(top, b);
				}
			}
			biosamples = new ArrayList<>(top2Sel.values());
		} else if (q.getSelectOneMode() == BiosampleQuery.SELECT_MOST_LEFT) {
			Map<Biosample, Biosample> top2Sel = new HashMap<>();
			for (Biosample b : biosamples) {
				Container c = b.getContainer();
				if (c == null || c.getPos() < 0)
					continue;
				Biosample top = b.getTopParent();
				Biosample sel = top2Sel.get(top);

				if (sel == null || (b.getCol() < sel.getCol())) {
					top2Sel.put(top, b);
				}
			}
			biosamples = new ArrayList<>(top2Sel.values());
		}


		LoggerFactory.getLogger(DAOBiosample.class).debug("filtered in " + (System.currentTimeMillis() - start) + "ms: n="+biosamples.size());
		return biosamples;
	}

	/**
	 * Delete the biosamples (but not the container) -use deletecontainers
	 * instead
	 *
	 * @param biosamples
	 * @throws Exception
	 */
	public static void deleteBiosamples(Collection<Biosample> biosamples, SpiritUser user) throws Exception {
		if(biosamples==null || biosamples.size()==0) return;
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			deleteBiosamples(session, biosamples, user);
			txn.commit();
			txn = null;
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}
	}

	public static void deleteBiosamples(EntityManager session, Collection<Biosample> biosamples, SpiritUser user) throws Exception {
		assert session!=null;
		assert session.getTransaction().isActive();
		logger.info("Remove "+biosamples.size()+" biosamples");

		for (Biosample biosample : biosamples) {
			if (!SpiritRights.canDelete(biosample, user)) throw new Exception("You are not allowed to delete " + biosample);
		}

		List<Integer> ids = JPAUtil.getIds(biosamples);
		if (ids.size()>0 && DAOResult.queryResults(session, ResultQuery.createQueryForBiosampleIds(ids), null).size() > 0) {
			throw new Exception("Some biosamples already contains results. Please delete the results first");
		}
		//Sort to delete first the children
		List<Biosample> list = new ArrayList<>(biosamples);
		Collections.sort(list, Collections.reverseOrder());

		//Delete
		for (Biosample biosample : list) {
			if(!session.contains(biosample)) {
				biosample = session.merge(biosample);
			}
			session.remove(biosample);
		}
	}

	/**
	 * Persists the biosamples.
	 *
	 *
	 *  If the biosample is linked to new groups, phases, locations, those are also saved
	 *
	 *
	 * @param biosamples
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public static List<Biosample> persistBiosamples(Collection<Biosample> biosamples, SpiritUser user) throws Exception {
		List<Biosample> res = new ArrayList<>();
		if(biosamples==null || biosamples.size()==0) return res;

		long start = System.currentTimeMillis();

		logger.info("Persist "+biosamples.size()+" biosamples");

		for (Biosample biosample : biosamples) {

			if (!SpiritRights.canEdit(biosample, user)) {
				throw new Exception("You are not allowed to edit the biosample "+biosample.getSampleId());
			}

			if (!SpiritRights.canEdit(biosample.getParent(), user)) {
				throw new Exception("You are not allowed to edit the biosample's parent of "+biosample.getSampleId());
			}
		}


		//Open the transaction
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;

		testConcurrentModification(session, biosamples);

		try {

			/////////////////////////////////////////////////
			//Find dependent studues that must be saved first
			/////////////////////////////////////////////////
			Set<Study> studyToSave = new HashSet<>();
			Set<Group> groupToSave = new HashSet<>();
			Set<Phase> phaseToSave = new HashSet<>();
			for (Biosample b : biosamples) {
				Study s = b.getInheritedStudy();
				Group g = b.getInheritedGroup();
				Phase p = b.getInheritedPhase();

				//Do we need to save the group?
				if(g!=null && g.getId()<=0) {
					g.setStudy(s);
					groupToSave.add(g);
					studyToSave.add(s);
				}

				//Do we need to save the phase?
				if(p!=null && p.getId()<=0) {
					p.setStudy(s);
					phaseToSave.add(p);
					studyToSave.add(s);
				}
			}

			//Remove orphan groups and phases, which may have been created in the process
			for (Study study : studyToSave) {
				for (Phase phase : new ArrayList<>(study.getPhases())) {
					if(phase.getId()<=0 && !phaseToSave.contains(phase)) {
						phase.setStudy(null);
					}
				}
				for (Group group : new ArrayList<>(study.getGroups())) {
					if(group.getId()<=0 && !groupToSave.contains(group)) {
						group.setStudy(null);
					}
				}
			}


			txn = session.getTransaction();
			txn.begin();
			if(studyToSave.size()>0) {
				logger.info("Persist dependant studies: "+studyToSave);
				Map<Integer, Study> id2study = JPAUtil.mapIds(DAOStudy.persistStudies(session, studyToSave, user));

				//Flush to update the entity ids
				session.flush();

				//Relink to avoid object confusion
				//(when 2 objects (group/phase) represents the same entity: only the first entity is saved and the other must be linked)
				for (Biosample b : biosamples) {
					if(b.getInheritedStudy()==null) continue;
					Study s = id2study.get(b.getInheritedStudy().getId());
					assert s!=null;
					Group g = b.getInheritedGroup();
					Phase p = b.getInheritedPhase();
					if(g!=null && g.getId()<=0) {
						g = s.getGroup(g.getName());
						assert g!=null: b.getInheritedGroup() + " not found in "+s.getGroups();
						b.setInheritedGroup(g);
						logger.info("Relink "+b+" to "+g+" "+g.getId());
					}
					if(p!=null && p.getId()<=0) {
						p = s.getPhase(p.getName());
						assert p!=null;
						b.setInheritedPhase(p);
						logger.info("Relink "+b+" to "+p+" "+p.getId());
					}
				}
			}

			//Open the transaction
			res = persistBiosamples(session, biosamples, user);

			txn.commit();
			txn = null;
		} finally {
			if (txn != null && txn.isActive())try {txn.rollback();} catch (Exception e2) {e2.printStackTrace();}
		}
		logger.info("Persist "+biosamples.size()+" biosamples: done in "+(System.currentTimeMillis()-start));
		return res;
	}


	private static void testConcurrentModification(EntityManager session, Collection<Biosample> biosamples) throws Exception {
		Map<Integer, Biosample> id2biosample = JPAUtil.mapIds(biosamples);
		if(id2biosample.size()==0) return;
		List<Object[]> lastUpdates = null;

		// Test that nobody else modified the samples
		String jpql = "select b.updDate, b.updUser, b.id from Biosample b where " + QueryTokenizer.expandForIn("b.id", id2biosample.keySet());
		lastUpdates = session.createQuery(jpql).getResultList();

		for (Object[] lastUpdate : lastUpdates) {
			Date lastDate = (Date) lastUpdate[0];
			String lastUser = (String) lastUpdate[1];
			Biosample b = id2biosample.get(lastUpdate[2]);
			logger.info("last update of " + b + " was " + lastDate + " by " + lastUser + " attached object: "+b.getUpdDate());
			if (b != null && b.getUpdDate() != null && lastDate != null) {
				int diffSeconds = (int) (lastDate.getTime() - b.getUpdDate().getTime());
				if (diffSeconds > 0)
					throw new Exception("The biosample (" + b + ") has just been updated by " + lastUser + " [at " + FormatterUtils.formatDateOrTime(lastDate) + ", current version is from "+FormatterUtils.formatDateOrTime(b.getUpdDate()) + "].\nYou cannot overwrite those changes unless you reopen the newest version.");
			}
		}
	}

	/**
	 * Note: if the user calls directly this function, he should call testConcurrentModification before starting the transaction
	 * @param session
	 * @param biosamples
	 * @param user
	 * @throws Exception
	 */
	public static List<Biosample> persistBiosamples(EntityManager session, Collection<Biosample> biosamples, SpiritUser user) throws Exception {
		assert session!=null;
		assert session.getTransaction().isActive();


		List<Biosample> res = new ArrayList<>();

		//Create a map of actual locationId_pos to biosample, to make sure positions are unique
		Map<String, String> locIdPos2container = new HashMap<>();
		Set<Location> locations = Biosample.getLocations(biosamples);
		if(locations.size()>0) {
			EntityManager newEm = null;
			try {
				newEm = JPAUtil.createManager();
				Set<Integer> biosampleIdsToSave = new HashSet<>(JPAUtil.getIds(biosamples));
				List<Object[]> l = session.createQuery("select b.id, b.container.containerId, b.location.id, b.pos from Biosample b where b.pos>=0 and " + QueryTokenizer.expandForIn("b.location.id", JPAUtil.getIds(locations))).getResultList();
				for (Object[] object : l) {
					int biosampleId = (Integer) object[0];
					String containerId = (String) object[1];
					int locationId = (Integer) object[2];
					int pos = (Integer) object[3];
					if(biosampleIdsToSave.contains(biosampleId)) continue;
					String key =  locationId + "_" + pos;
					assert !locIdPos2container.containsKey(key) || locIdPos2container.get(key).equals(containerId);
					locIdPos2container.put(key, containerId);
				}
			} finally {
				if(newEm!=null) newEm.close();
			}
		}

		/////////////////////////
		// Validation
		for (Biosample biosample : biosamples) {

			// Generate missing sampleId (if possible)
			if (biosample.getSampleId() == null || biosample.getSampleId().length() == 0) {
				biosample.setSampleId(DAOBarcode.getNextId(biosample.getBiotype()));
				if (biosample.getSampleId() == null || biosample.getSampleId().length() == 0) {
					throw new Exception("The biosample must have a sampleId");
				}
			}
			if (biosample.getSampleId().indexOf(' ') >= 0) {
				throw new Exception("The sampleId '" + biosample.getSampleId() + "' cannot have spaces");
			}

			// Test that the biosample has biotype
			if (biosample.getBiotype() == null) {
				throw new Exception("The sampleId '" + biosample.getSampleId() + "' must have a biotype");
			}

			// Check consistency: the study must be saved before
			if(biosample.getAttachedStudy()!=null && biosample.getAttachedStudy().getId()<=0) {
				throw new Exception("The sampleId '" + biosample.getSampleId() + "' is a study, which was not saved: "+biosample.getAttachedStudy());
			}

			if(biosample.getInheritedGroup()!=null && biosample.getInheritedGroup().getId()<=0) {
				throw new Exception("The sampleId '" + biosample.getSampleId() + "' is a group, which was not saved: "+biosample.getInheritedGroup());
			}

			if(biosample.getInheritedPhase()!=null && biosample.getInheritedPhase().getId()<=0) {
				throw new Exception("The sampleId '" + biosample.getSampleId() + "' is a phase, which was not saved: "+biosample.getInheritedPhase());
			}
		}

		//Test samplename (required, and uniqueness)
		Map<Biotype, Map<String, Biosample>> biotype2name2sample = new HashMap<>();
		for (Biosample b : biosamples) {
			if(b.getBiotype().getSampleNameLabel()==null) continue;
			if(b.getSampleName()==null || b.getSampleName().length()==0) {
				//No name, throw an exception if it is required
				if(b.getBiotype().isNameRequired()) {
					throw new Exception("The field '"+b.getBiotype().getSampleNameLabel()+"' of "+b.getSampleId()+" is required "+b.getInfos());
				}
			} else if(b.getBiotype().isNameUnique()) {
				//Name present: check uniqueness
				Map<String, Biosample> map = biotype2name2sample.get(b.getBiotype());
				if(map==null) {
					biotype2name2sample.put(b.getBiotype(), map = new HashMap<>());
				}
				if(map.get(b.getSampleName())!=null && map.get(b.getSampleName())!=b) throw new Exception("The "+b.getBiotype().getSampleNameLabel()+" of "+b.getSampleId()+": " +b.getSampleName()+"  is not unique (used by: "+map.get(b.getSampleName())+")");
				map.put(b.getSampleName(), b);
			}
		}
		if(biotype2name2sample.size()>0) {
			for(Biosample b: (List<Biosample>) session.createQuery("from Biosample b where  " + QueryTokenizer.expandForIn("b.name", Biosample.getSampleNames(biosamples))).getResultList()) {
				Map<String, Biosample> map = biotype2name2sample.get(b.getBiotype());
				if(map==null) continue;
				Biosample b2 = map.get(b.getSampleName());
				if(b2==null) continue;
				if(b2.getId()!=b.getId()) {
					throw new Exception("The field '"+b.getBiotype().getSampleNameLabel()+"' of "+b.getSampleId()+" is not unique");
				}
			}
		}

		//Test aggregated samples
		for (Biosample biosample : biosamples) {
			for(BiotypeMetadata bType: biosample.getBiotype().getMetadata()) {
				if(bType.getDataType()!=DataType.BIOSAMPLE) continue;
				Biosample agg = biosample.getMetadataBiosample(bType);
				if(agg!=null && agg.getId()<=0 && !biosamples.contains(agg)) throw new Exception("The linked biosample "+agg+" does not exist");
			}
		}

		//Test that the biosample location is valid and unique
		for (Biosample biosample : biosamples) {
			if (biosample.getLocation() != null) {
				Location loc = biosample.getLocation();
				if (biosample.isAbstract()) {
					throw new ValidationException("The biosample is abstract and cannot have a location", biosample, "Container\nLocation");
				} else {

					// Validate the location's position
					String key = (loc.getId()>0? loc.getId(): loc.getHierarchyFull()) + "_" + biosample.getPos();
					if (biosample.getPos() < 0 || loc.getLabeling() == LocationLabeling.NONE) {
						biosample.setPos(-1);
					} else if (locIdPos2container.containsKey(key) && ((biosample.getContainerId()==null && locIdPos2container.get(key)==null) || (biosample.getContainerId()!=null && !biosample.getContainerId().equals(locIdPos2container.get(key))) ) ) {
						throw new ValidationException("The location " + biosample.getLocationString(LocationFormat.FULL_POS, null) + " is already taken by "+locIdPos2container.get(key), biosample, "Container\nLocation");
					}
					locIdPos2container.put(key, biosample.getContainerId());

					if (loc.getSize() > 0 && biosample.getPos() >= loc.getSize()) {
						throw new ValidationException("The location " + biosample.getLocationString(LocationFormat.FULL_POS, null) + " is invalid (size=" + loc.getSize() + ")", biosample, "Container\nLocation");
					}
				}
			}
		}



		//////////////////////////////////////
		for (Biosample biosample : biosamples) {
			// Update the top
			biosample.setTopParent(biosample.getParent() == null ? biosample : biosample.getParent().getTopParent());

			//Check the coherence of the inherited study and the attached study
			if (biosample.getAttachedStudy() != null) {
				if(biosample.getInheritedStudy()!=null && !biosample.getInheritedStudy().equals(biosample.getAttachedStudy())) throw new Exception("Invalid inherited study="+biosample.getInheritedStudy()+" expected="+biosample.getAttachedStudy()+" for "+biosample);
				biosample.setInheritedStudy(biosample.getAttachedStudy());
			}
			// Update the group if the parent has one
			if (biosample.getAttachedStudy() != null) {
				// Attached Study -> no update
			} else if (biosample.getParent() == null) {
				biosample.setInheritedGroup(null);
				biosample.setInheritedPhase(null);
			} else {
				if (biosample.getInheritedGroup() == null && biosample.getParent().getInheritedGroup()!=null) {
					biosample.setInheritedGroup(biosample.getParent().getInheritedGroup());
					biosample.setInheritedSubGroup(biosample.getParent().getInheritedSubGroup());
				}
				if (biosample.getInheritedPhase() == null && biosample.getParent().getInheritedPhase()!=null) {
					biosample.setInheritedPhase(biosample.getParent().getInheritedPhase());
				}
			}


			// Test we have no cycle
			LinkedList<Biosample> gen = new LinkedList<>();
			if (biosample.getParent() != null)
				gen.add(biosample.getParent());
			while (!gen.isEmpty()) {
				Biosample b = gen.remove(0);
				if (b == biosample)
					throw new Exception("You just created a cycle from " + biosample + ". This is not allowed.");
				if (b.getParent() != null)
					gen.add(b.getParent());
			}

			// Check the parents are valid
			if (biosample.getParent() != null && biosample.getParent().getId() <= 0 && !biosamples.contains(biosample.getParent())) {
				throw new Exception("The parent of " + biosample + " id:" + biosample.getId() + " ->" + biosample.getParent() + " id:"+(biosample.getParent().getId())+ " is invalid, 2nd parent:"+biosample.getParent().getParent());
			}
		}

		// ////////////////////////////////////////////////////
		// Save linked locations if they are new
		for (Biosample biosample : biosamples) {
			Location location = biosample.getLocation();
			if (location != null) {
				if(location.getLocationType()==null) throw new Exception("The location's type cannot be null");
				if(location.getName()==null || location.getName().length()==0) throw new Exception("The location's name is required");
				//to save the location when the location was created or edited in the setlocation dlg
				if (location.getId() <= 0) {
					logger.debug("Persist linked location: "+location );
					session.persist(location);
				} else if(!session.contains(location) && location.wasUpdated() ) {
					logger.debug("Merge linked location: "+location );
					session.merge(location);
				}
			}

		}

		//Compute formula (when needed)
		computeFormula(biosamples);

		// /////////////////
		//Update the upddate/upduser
		Date now = JPAUtil.getCurrentDateFromDatabase();
		for (Biosample b : biosamples) {
			b.preSave();
			if(user==null) {
				//Make sure the date and user were set by the programmer (import mode?)
				if(b.getUpdUser()==null || b.getUpdDate()==null) {
					throw new Exception("The updUser and updDate must be set");
				}
			} else {
				// Set the updDate/user/group
				b.setUpdDate(now);
				b.setUpdUser(user.getUsername());
				if (b.getEmployeeGroup() == null) {
					if(user.getMainGroup()!=null) {
						b.setEmployeeGroup(user.getMainGroup());
					} else {
						Biosample parent = b.getParent();
						while(parent!=null) {
							if(parent.getEmployeeGroup()!=null) {
								b.setEmployeeGroup(parent.getEmployeeGroup());
								break;
							}
							parent = parent.getParent();
						}
					}
				}
			}
		}

		//Persist or merge
		for (Biosample b : biosamples) {
			assert b.getBiotype()!=null && b.getBiotype().getId()>0: "The biotype of "+b+" ("+b.getBiotype()+") is not persistent";
			if (b.getId() <= 0) {
				b.setCreUser(b.getUpdUser());
				b.setCreDate(b.getUpdDate());
				session.persist(b);
			} else if (!session.contains(b)) {
				b = session.merge(b);
			}
			res.add(b);
		}


		//////////////////////////////////////////////////////
		// Update Containers
		Set<String> containerIds = Biosample.getContainerIds(res);
		Map<String, Container> cid2container = new HashMap<>();// Container.mapContainerId(getContainers(containerIds));
		if(containerIds.size()>0) {
			List<Biosample> inBiosamples = session.createQuery("from Biosample b where " + QueryTokenizer.expandForIn("b.container.containerId", containerIds)).getResultList();
			cid2container.putAll(Container.mapContainerId(Biosample.getContainers(inBiosamples, true)));
		}

		for (Biosample biosample : res) {
			if(biosample.getContainerType()==null) {
				//If there is no containerType, there should be no containerId
				biosample.setContainerId(null);
			} else /*containerType!=null*/ {
				String containerId = biosample.getContainerId();
				if (containerId!=null && containerId.length()>0) {
					Container c = cid2container.get(containerId);
					if(c!=null) {
						//If the containerType is multiple and not unique, make sure the user can add to it
						if(c.getContainerType().isMultiple() && !SpiritRights.canEdit(c, user)) throw new Exception("You are not allowed to edit the container " + containerId);
						if(c.getContainerType().isMultiple() && c.getContainerType()!=biosample.getContainerType()) throw new Exception("The container's type of " + biosample.getSampleId() + ": " + containerId+" is " + c.getContainerType());
						if(c.getContainerType().isMultiple() && c.getStudy()!=biosample.getInheritedStudy()) throw new Exception("You cannot mix studies on the same container "+containerId+" ( "+c.getBiosamples()+" on "+c.getStudy()+")<>"+biosample.getInheritedStudy());
						//If the containerType is not multiple, make sure it is unique
						if(!c.getContainerType().isMultiple() && c.getBiosamples().size()>0 && !c.getBiosamples().contains(biosample)) throw new Exception("The container " + containerId + " of " + biosample + " is already used by "+c.getBiosamples());
					} else {
						cid2container.put(containerId, biosample.getContainer());
					}
				}

				if(biosample.getContainerType().isMultiple() &&(biosample.getContainerIndex()==null || biosample.getContainerIndex()<1)) {
					biosample.setContainerIndex(1);
				}

				//Remove container of abstract samples
				if(biosample.getBiotype().isAbstract()) {
					biosample.setContainer(null);
				}
			}
		}

		// Propagate the study information to children
		for (Biosample b : res) {
			List<Biosample> toCheck = new LinkedList<>();
			toCheck.addAll(b.getChildren());
			while(!toCheck.isEmpty()) {
				Biosample tmp = toCheck.remove(0);

				// Stop at samples attached to study
				if(tmp.getAttachedStudy()!=null) continue;

				// Update the study of child
				tmp.setInheritedStudy(b.getInheritedStudy());

				// Update the group/phase of child
				boolean modified = false;
				if (CompareUtils.compare(b.getInheritedGroup(), tmp.getInheritedGroup())!=0 || b.getInheritedSubGroup() != tmp.getInheritedSubGroup()) {
					tmp.setInheritedGroup(b.getInheritedGroup());
					tmp.setInheritedSubGroup(b.getInheritedSubGroup());
					modified = true;
				}
				// Update the phase of child (only if the parent has one)
				if (b.getInheritedPhase()!=null && CompareUtils.compare(b.getInheritedPhase(), tmp.getInheritedPhase())!=0) {
					tmp.setInheritedPhase(b.getInheritedPhase());
					modified = true;
				}
				if (CompareUtils.compare(b.getTopParent(), tmp.getTopParent())!=0) {
					tmp.setTopParent(b.getTopParent());
					modified = true;
				}
				if(modified) {
					toCheck.addAll(tmp.getChildren());
				}
			}
		}

		session.flush();


		// ///////////////////////////////////////////
		// Update the values of the linked biosamples if needed
		{
			String query = "select b from Biosample b, IN(b.linkedBiosamples) lb where " +QueryTokenizer.expandForIn("lb.id", JPAUtil.getIds(biosamples));
			List<Biosample> linked = session.createQuery(query).getResultList();
			for (Biosample b : linked) {
				for (BiotypeMetadata bm : b.getBiotype().getMetadata()) {
					if (bm.getDataType()==DataType.BIOSAMPLE && b.getMetadataBiosample(bm) != null) {
						b.setMetadataBiosample(bm, b.getMetadataBiosample(bm));
					}
				}
			}
		}
		return res;
	}

	public static enum AmountOp {
		ADD, SUBSTRACT, SET
	}


	public static enum BiosampleDuplicateMethod {
		RETURN_ALL("Returns all having duplicate"), RETURN_OLDEST("Returns the oldests"), RETURN_NEWEST("Returns the newests"), RETURN_OLDEST_WITHOUT_RESULT("Returns The oldest, but keep all with results");

		private String name;

		private BiosampleDuplicateMethod(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static List<Biosample> findDuplicates(Study study, Biotype biotype, BiosampleDuplicateMethod duplicateMethod, SpiritUser user) throws Exception {
		if (study == null && biotype == null)
			throw new Exception("You need to specifiy a filter");
		BiosampleQuery q = new BiosampleQuery();
		q.setStudyIds(study == null ? "" : study.getStudyId());
		q.setBiotype(biotype);
		List<Biosample> all = queryBiosamples(q, user);
		List<Biosample> res = new ArrayList<>();
		ListHashMap<String, Biosample> key2Sample = new ListHashMap<>();

		for (Biosample b : all) {
			if (b.getTopParent() == b)
				continue;

			String key = nvl(b.getInheritedStudy()) + "_" + nvl(b.getBiotype()) + "_" + nvl(b.getTopParent()) + "_" + nvl(b.getSampleName()) + "_" + nvl(b.getMetadataAsString()) + "_" + nvl(b.getContainerType()) + "_"
					+ (b.getAttachedSampling() == null ? "" : b.getAttachedSampling().getId());
			key2Sample.add(key, b);
		}

		if (duplicateMethod == BiosampleDuplicateMethod.RETURN_OLDEST_WITHOUT_RESULT) {
			for (Result r : DAOResult.queryResults(ResultQuery.createQueryForBiosampleIds(JPAUtil.getIds(all)), null)) {
				r.getBiosample().getAuxiliaryInfos().put("result", "true");
			}
		}

		for (List<Biosample> list : key2Sample.values()) {
			if (list.size() <= 1)
				continue;

			Biosample sel = null;
			if (duplicateMethod == BiosampleDuplicateMethod.RETURN_NEWEST) {
				for (Biosample b : list) {
					if (sel == null || b.getCreDate().after(sel.getCreDate()))
						sel = b;
				}
			} else if (duplicateMethod == BiosampleDuplicateMethod.RETURN_OLDEST) {
				for (Biosample b : list) {
					if (sel == null || b.getCreDate().before(sel.getCreDate()))
						sel = b;
				}
			} else if (duplicateMethod == BiosampleDuplicateMethod.RETURN_OLDEST_WITHOUT_RESULT) {
				for (Biosample b : list) {
					if (b.getAuxiliaryInfos().get("result") != null)
						continue;
					if (sel == null || b.getCreDate().before(sel.getCreDate()))
						sel = b;
				}
			} else if (duplicateMethod == BiosampleDuplicateMethod.RETURN_ALL) {
				// OK
			} else {
				throw new IllegalArgumentException("Invalid method: " + duplicateMethod);
			}

			if (duplicateMethod == BiosampleDuplicateMethod.RETURN_ALL) {
				res.addAll(list);
			} else {
				if (sel == null) {
					System.err.println("Could not decide between " + list);
				} else {
					list.remove(sel);
					res.addAll(list);
				}
			}
		}

		return res;
	}

	private static String nvl(Object o) {
		return o == null ? "" : o.toString();
	}

	public static void changeOwnership(Collection<Biosample> biosamples, SpiritUser toUser, SpiritUser user) throws Exception {
		assert toUser!=null;
		if(!SpiritRights.canEditBiosamples(biosamples, user)) throw new Exception("You don't have sufficient rights to change the ownership");

		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();

			Date now = JPAUtil.getCurrentDateFromDatabase();

			for (Biosample b : biosamples) {
				if (b.getId() <= 0) continue;
				b.setUpdUser(user.getUsername());
				b.setUpdDate(now);
				b.setCreUser(toUser.getUsername());
				b.setEmployeeGroup(toUser.getMainGroup());
				if(!session.contains(b)) {
					session.merge(b);
				}
			}

			txn.commit();
			txn = null;
		} finally {
			if (txn != null && txn.isActive()) try { txn.rollback();} catch (Exception e) {}
		}
	}

	/**
	 * Computes the formula for the given results
	 *
	 * @param results
	 * @return true only if a value has been computed
	 */
	public static boolean computeFormula(Collection<Biosample> biosamples) {
		Map<Biotype, List<Biosample>> map = Biosample.mapBiotype(biosamples);
		boolean updated = false;
		for(Biotype biotype: map.keySet()) {

			//Loop though attributes of type formula
			for (BiotypeMetadata bm : biotype.getMetadata()) {
				if(bm.getDataType()!=DataType.FORMULA) continue;
				String formula = bm.getParameters();
				Expression e;
				try {
					e = ExpressionHelper.createExpression(formula, biotype);;
				} catch(Exception ex) {
					logger.warn("Could not evaluate "+formula+": "+ex);
					continue;
				}

				//Loop through each result to calculate the formula
				for (Biosample biosample : map.get(biotype)) {

					updated = true;
					try {
						double res = ExpressionHelper.evaluate(e, biosample);
						logger.info("Evaluate "+formula+": >"+res);
						biosample.setMetadataValue(bm, ""+res);
					} catch(Exception ex) {
						logger.info("ERROR "+formula+": "+ex);
						biosample.setMetadataValue(bm, "");
					}
				}

			}
		}
		return updated;
	}


}
