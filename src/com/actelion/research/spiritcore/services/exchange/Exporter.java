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

package com.actelion.research.spiritcore.services.exchange;

import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.LocationFormat;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritlib.pojo.BiosamplePojo;
import com.actelion.research.spiritlib.pojo.BiosampleQueryPojo;
import com.actelion.research.spiritlib.pojo.BiotypeMetadataPojo;
import com.actelion.research.spiritlib.pojo.BiotypePojo;
import com.actelion.research.spiritlib.pojo.ExchangePojo;
import com.actelion.research.spiritlib.pojo.GroupPojo;
import com.actelion.research.spiritlib.pojo.LocationPojo;
import com.actelion.research.spiritlib.pojo.MeasurementPojo;
import com.actelion.research.spiritlib.pojo.NamedSamplingPojo;
import com.actelion.research.spiritlib.pojo.NamedTreatmentPojo;
import com.actelion.research.spiritlib.pojo.PhasePojo;
import com.actelion.research.spiritlib.pojo.ResultPojo;
import com.actelion.research.spiritlib.pojo.ResultQueryPojo;
import com.actelion.research.spiritlib.pojo.SamplingPojo;
import com.actelion.research.spiritlib.pojo.StudyActionPojo;
import com.actelion.research.spiritlib.pojo.StudyPojo;
import com.actelion.research.spiritlib.pojo.TestAttributePojo;
import com.actelion.research.spiritlib.pojo.TestPojo;
import com.owlike.genson.Genson;

/**
 * 
 * @author freyssj
 *
 */
public class Exporter {

	/**
	 * Export studies, biosamples, locations, and results mentioned in the exchange object.
	 * The file must contain all references to other entities. If the exchange object is invalid (ie. importing cannot be done after export), an exception will be thrown.
	 *  
	 * @param biosamples
	 * @param user
	 * @param writer
	 * @throws Exception
	 */
	public static void write(Exchange exchange, Writer writer) throws Exception {
		
		ExchangePojo res = convertExchange(exchange);		
		
		Genson gson = new Genson();
		writer.write(gson.serialize(res));
		writer.close();
	}

	public static ExchangePojo convertExchange(Exchange c) {		
		long s = System.currentTimeMillis();

		ExchangePojo res = new ExchangePojo();
		res.setName(c.getName());
		res.setVersion(c.getVersion());
		res.setBiotypes(convertBiotype(c.getBiotypes()));
		res.setBiosamples(convertBiosamples(c.getBiosamples()));
		res.setLocations(convertLocations(c.getLocations()));
		res.setTests(convertTests(c.getTests()));
		res.setResults(convertResults(c.getResults()));
		res.setStudies(convertStudies(c.getStudies()));

		System.out.println("Exporter.convertExchange() created in "+(System.currentTimeMillis()-s)+"ms");

		//Autotest, make sure the conversion is possible
		try {
			new Importer().convertExchange(res);
		} catch(Exception ex) {
			throw new RuntimeException("The exported exchange format is invalid", ex);
		}
		System.out.println("Exporter.convertExchange() tested in "+(System.currentTimeMillis()-s)+"ms");
		
		return res;
	}
	

	//////////////////////////////////////////////////////////////////////////////
	public static StudyPojo convertStudy(Study s) {
		if(s==null) return null;
		Set<StudyPojo> res = convertStudies(Collections.singleton(s));
		return res.size()==1? res.iterator().next(): null;
	}
	
	public static Set<StudyPojo> convertStudies(Collection<Study> list) {
		if(list==null) return null;
		Set<StudyPojo> res = new HashSet<>();
		for (Study s : list) {
			if(s==null) continue;
			StudyPojo r = new StudyPojo();
			r.setId(s.getId());
			r.setStudyId(s.getStudyId());
			r.setIvv(s.getIvv());
			r.setTitle(s.getTitle());
			r.setAttachedSampleIds(Biosample.getSampleIds(s.getAttachedBiosamples()));
			r.setBlindAllUsers(s.getBlindAllUsers());
			r.setBlindDetailsUsers(s.getBlindDetailsUsers());
			r.setCreDate(s.getCreDate());
			r.setCreUser(s.getCreUser());
			r.setDay1(s.getDayOneDate());
			r.setGroups(convertGroups(s.getGroups()));
			r.setNamedSamplings(convertNamedSamplings(s.getNamedSamplings()));
			r.setNamedTreatments(convertNamedTreatments(s.getNamedTreatments()));
			r.setNotes(s.getNotes());
			r.setPhaseFormat(s.getPhaseFormat()==null?"": s.getPhaseFormat().name());
			r.setPhases(convertPhases(s.getPhases()));
			r.setExpertUsers(s.getExpertUsers());
			r.setState(s.getState());
			r.setMetadata(s.getMetadata());
			r.setStudyActions(convertStudyActions(s.getStudyActions()));
			r.setSynchronizeSamples(s.isSynchronizeSamples());
			r.setUpdDate(s.getUpdDate());
			r.setUpdUser(s.getUpdUser());
			r.setAdminUsers(s.getAdminUsers());			
			res.add(r);
		}
		return res;
	}
	
	public static List<GroupPojo> convertGroups(Collection<Group> list) {
		List<GroupPojo> res = new ArrayList<>();
		for (Group g : list) {
			GroupPojo p = new GroupPojo();			
			p.setId(g.getId());
			p.setName(g.getName());
			p.setColorRgb(g.getColorRgb());
			p.setDividingSampling(g.getDividingSampling()==null? null: convertSamplings(Collections.singleton(g.getDividingSampling())).iterator().next());
			p.setFromGroup(g.getFromGroup()==null?"": g.getFromGroup().getName());
			p.setFromPhase(g.getFromPhase()==null?"": g.getFromPhase().getShortName());
			p.setSubgroupSizes(g.getSubgroupSizes());
			res.add(p);
		}
		return res;
	}
	
	public static List<PhasePojo> convertPhases(Collection<Phase> list) {
		List<PhasePojo> res = new ArrayList<>();
		for (Phase g : list) {
			PhasePojo p = new PhasePojo();			
			p.setId(g.getId());
			p.setName(g.getName());
			p.setRando(g.getSerializedRandomization());
//			p.setLabel(g.getLabel());
//			p.setSerializedRandomization(g.getSerializedRandomization());
			res.add(p);
		}
		
		return res;
	}

	public static List<NamedTreatmentPojo> convertNamedTreatments(Collection<NamedTreatment> list) {
		List<NamedTreatmentPojo> res = new ArrayList<>();
		for (NamedTreatment g : list) {
			NamedTreatmentPojo p = new NamedTreatmentPojo();			
			p.setId(g.getId());
			p.setName(g.getName());
			p.setApplication1(g.getApplication1());
			p.setApplication2(g.getApplication2());
			p.setColorRgb(g.getColorRgb());
			p.setCompoundName1(g.getCompoundName1());
			p.setCompoundName2(g.getCompoundName2());
			p.setDose1(g.getDose1());
			p.setDose2(g.getDose2());
			p.setUnit1(g.getUnit1()==null?"":g.getUnit1().name());
			p.setUnit2(g.getUnit2()==null?"":g.getUnit2().name());
			res.add(p);
		}
		
		return res;
	}


	public static List<NamedSamplingPojo> convertNamedSamplings(Collection<NamedSampling> list) {
		List<NamedSamplingPojo> res = new ArrayList<>();
		for (NamedSampling g : list) {
			NamedSamplingPojo p = new NamedSamplingPojo();			
			p.setId(g.getId());
			p.setName(g.getName());
			p.setNecropsy(g.isNecropsy());
			p.setSamplings(convertSamplings(g.getTopSamplings()));
			res.add(p);
		}
		
		return res;
	}
	
	public static Set<SamplingPojo> convertSamplings(Collection<Sampling> list) {
		Map<Integer, Test> id2test = JPAUtil.mapIds(DAOTest.getTests(Measurement.getTestIds(Sampling.getMeasurements(list))));
		
		Set<SamplingPojo> res = new LinkedHashSet<>();
		for (Sampling g : list) {
			SamplingPojo p = new SamplingPojo();			
			p.setAmount(g.getAmount());
			p.setBiotype(g.getBiotype()==null?"": g.getBiotype().getName());
			p.setBlocNo(g.getBlocNo()==null?null: g.getBlocNo());
			p.setChildren(convertSamplings(g.getChildren()));
			p.setComments(g.getComments());
			p.setCommentsRequired(g.isCommentsRequired());
			p.setContainerType(g.getContainerType()==null?"": g.getContainerType().name());
			p.setId(g.getId());
			p.setLengthRequired(g.isLengthRequired());
			
			p.setSampleName(g.getSampleName());
			p.setWeighingRequired(g.isWeighingRequired());
			
			//Metadata
			Map<String, String> map = new HashMap<>();
			for(Map.Entry<BiotypeMetadata, String> e: g.getMetadataMap().entrySet()) {
				assert e.getKey().getName()!=null; 
				map.put(e.getKey().getName(), e.getValue());
			}
			p.setMetadata(map);
			
			//Measurements
			List<MeasurementPojo> mps = new ArrayList<>();			
			for(Measurement m: g.getMeasurements()) {
				Test t = id2test.get(m.getTestId());
				if(t==null) continue;
				MeasurementPojo mp = new MeasurementPojo();
				mp.setTest(t.getName());
				mp.setParameters(m.getParameters());
				mps.add(mp);
			}
			p.setMeasurements(mps.toArray(new MeasurementPojo[mps.size()]));			
			
			res.add(p);
		}
		
		return res;
	}
	
	public static List<StudyActionPojo> convertStudyActions(Collection<StudyAction> list) {
		Map<Integer, Test> id2test = JPAUtil.mapIds(DAOTest.getTests(Measurement.getTestIds(StudyAction.getMeasurements(list))));
		List<StudyActionPojo> res = new ArrayList<>();
		for (StudyAction g : list) {
			StudyActionPojo p = new StudyActionPojo();
			p.setGroup(g.getGroup().getName());
			p.setLabel(g.getLabel());
			p.setNamedSampling1(g.getNamedSampling1()==null?"": g.getNamedSampling1().getName());
			p.setNamedSampling2(g.getNamedSampling2()==null?"": g.getNamedSampling2().getName());
			p.setNamedTreatment(g.getNamedTreatment()==null?"": g.getNamedTreatment().getName());
			p.setPhase(g.getPhase().getShortName());
			p.setSubGroup(g.getSubGroup());

			p.setMeasureFood(g.isMeasureFood());
			p.setMeasureWater(g.isMeasureWater());
			p.setMeasureWeight(g.isMeasureWeight());
			
			//Measurements
			List<MeasurementPojo> mps = new ArrayList<>();			
			for(Measurement m: g.getMeasurements()) {
				Test t = id2test.get(m.getTestId());
				if(t==null) continue;
				MeasurementPojo mp = new MeasurementPojo();
				mp.setTest(t.getName());
				mp.setParameters(m.getParameters());
				mps.add(mp);
			}
			p.setMeasurements(mps.toArray(new MeasurementPojo[mps.size()]));
			
			res.add(p);
		}
		
		return res;
	}

	
	//////////////////////////////////////////////////////////////////////////////
	public static Set<BiotypePojo> convertBiotype(Collection<Biotype> list) {
		if(list==null) return null;
		Set<BiotypePojo> res = new HashSet<>();
		for (Biotype b : list) {
			if(b==null) continue;
			BiotypePojo biotype = new BiotypePojo();
			biotype.setId(b.getId());
			biotype.setName(b.getName());
			biotype.setSampleNameLabel(b.getSampleNameLabel());
			biotype.setCategory(b.getCategory().name());
			biotype.setPrefix(b.getPrefix());
			biotype.setAmountUnit(b.getAmountUnit()==null?"":b.getAmountUnit().name());
			
			for(BiotypeMetadata m: b.getMetadata()) {
				biotype.getMetadata().add(convertBiotypeMetadata(m));
			}
			biotype.setContainerType(b.getContainerType()==null?"":b.getContainerType().name());
			biotype.setParentBiotype(b.getParent()==null?"":b.getParent().getName());
//			biotype.setDescription(b.getDescription());
			
			biotype.setAbstract(b.isAbstract());
			biotype.setHidden(b.isHidden());
			biotype.setHideContainer(b.isHideContainer());		
			biotype.setHideSampleId(b.isHideSampleId());
			biotype.setNameAutocomplete(b.isNameAutocomplete());
			biotype.setNameRequired(b.isNameRequired());
			res.add(biotype);
		}
		return res;
	}
	

	private static BiotypeMetadataPojo convertBiotypeMetadata(BiotypeMetadata m) {
		BiotypeMetadataPojo res = new BiotypeMetadataPojo();
		res.setId(m.getId());
		res.setName(m.getName());
		res.setDataType(m.getDataType().name());
		res.setParameters(m.getParameters());
		res.setRequired(m.isRequired());
		res.setSecundary(m.isSecundary());
		return res;
	}

	public static Set<BiosamplePojo> convertBiosamples(Collection<Biosample> list) {
		if(list==null) return null;
		Set<BiosamplePojo> res = new HashSet<>();
		for (Biosample b : list) {
			if(b==null) continue;
			BiosamplePojo biosample = new BiosamplePojo();
			biosample.setId(b.getId());
			biosample.setSampleId(b.getSampleId());
			biosample.setSampleName(b.getSampleName());
			
			biosample.setParentSampleId(b.getParent()==null?"": b.getParent().getSampleId());
			biosample.setTopSampleId(b.getTopParent()==null?"": b.getTopParent().getSampleId());
			
			biosample.setContainerId(b.getContainerId());
			biosample.setContainerType(b.getContainerType()==null?"": b.getContainerType().name());
			biosample.setBiotype(b.getBiotype().getName());
			
			biosample.setAttached(b.getAttachedStudy()!=null);
			biosample.setStudyId(b.getInheritedStudy()==null? "": b.getInheritedStudy().getStudyId());
			biosample.setStudyGroup(b.getInheritedGroup()==null? "": b.getInheritedGroup().getName());
			biosample.setStudySubGroup(b.getInheritedSubGroup());
			biosample.setStudyPhase(b.getInheritedPhase()==null? "": b.getInheritedPhase().getShortName());
			biosample.setAttachedSamplingId(b.getAttachedSampling()==null? 0: b.getAttachedSampling().getId());
			
			for(BiotypeMetadata bm: b.getBiotype().getMetadata()) {
				String s = b.getMetadataValue(bm);
				biosample.getMetadata().put(bm.getName(), s==null? "": s);
			}
			
			biosample.setFullLocation(b.getLocationString(LocationFormat.FULL_POS, null));
			biosample.setAmount(b.getAmount()==null?"":""+b.getAmount());
			biosample.setStatus(b.getStatus()==null?"":b.getStatus().name());
			biosample.setQuality(b.getQuality()==null?"":b.getQuality().name());
			biosample.setComments(b.getComments());
			biosample.setUpdDate(b.getUpdDate());
			biosample.setUpdUser(b.getUpdUser());
			biosample.setCreDate(b.getCreDate());
			biosample.setCreUser(b.getCreUser());
			res.add(biosample);
		}
		return res;
	}


	//////////////////////////////////////////////////////////////////////////////
	public static Set<TestPojo> convertTests(Collection<Test> list) {
		if(list==null) return null;
		Set<TestPojo> res = new HashSet<>();
		for (Test t : list) {
			if(t==null) continue;
			TestPojo test = new TestPojo();
			test.setId(t.getId());
			test.setName(t.getName());
			test.setCategory(t.getCategory());
			
			for(TestAttribute ta: t.getAttributes()) {
				test.getAttributes().add(convertTestAttribute(ta));
			}
			res.add(test);
		}
		return res;
	}
	

	private static TestAttributePojo convertTestAttribute(TestAttribute m) {
		TestAttributePojo res = new TestAttributePojo();
		res.setId(m.getId());
		res.setName(m.getName());
		res.setOutputType(m.getOutputType().name());
		res.setDataType(m.getDataType().name());
		res.setRequired(m.isRequired());
		res.setParameters(m.getParameters());
		return res;
	}

	
	public static Set<ResultPojo> convertResults(Collection<Result> list) {
		if(list==null) return null;
		Set<ResultPojo> res = new HashSet<>();
		for (Result r : list) {
			if(r==null) continue;
			ResultPojo result = new ResultPojo();
			result.setId(r.getId());
			result.setElb(r.getElb());
			result.setTestName(r.getTest()==null? null: r.getTest().getName());
			if(r.getBiosample()!=null) {
				result.setSampleId(r.getBiosample().getSampleId());
			}
			for(ResultValue rv : r.getResultValues()) {
				result.getValues().put(rv.getAttribute().getName(), rv.getValue()==null?"": rv.getValue());
			}
			result.setPhase(r.getPhase()==null? "": r.getPhase().getShortName());
			result.setComments(r.getComments());
			result.setQuality(r.getQuality()==null?"":r.getQuality().name());
			result.setUpdDate(r.getUpdDate());
			result.setUpdUser(r.getUpdUser());
			result.setCreDate(r.getCreDate());
			result.setCreUser(r.getCreUser());
			res.add(result);
			
		}
		return res;
	}

	//////////////////////////////////////////////////////////////////////////////
	public static Set<LocationPojo> convertLocations(Collection<Location> list) {
		if(list==null) return null;
		Set<LocationPojo> res = new HashSet<>();
		for (Location l : list) {
			if(l==null) continue;
			LocationPojo r = new LocationPojo();
			r.setId(l.getId());
			r.setFullName(l.getHierarchyFull());
			r.setDescription(l.getDescription());
			r.setCols(l.getCols());
			r.setRows(l.getRows());
			r.setLocationType(l.getLocationType()==null?"": l.getLocationType().name());
			r.setLabeling(l.getLabeling()==null?"": l.getLabeling().name());
			r.setUpdDate(l.getUpdDate());
			r.setUpdUser(l.getUpdUser());
			r.setCreDate(l.getCreDate());
			r.setCreUser(l.getCreUser());
			res.add(r);
		}
		return res;
	}
	
	//////////////////////////////////////////////////////////////////////////////////
	public static BiosampleQueryPojo convertBiosampleQuery(BiosampleQuery q) {
		BiosampleQueryPojo res = new BiosampleQueryPojo();
		res.setContainerIds(q.getContainerIds());
		res.setSampleIds(q.getSampleIds());
		res.setStudyIds(q.getStudyIds());
		res.setParentSampleIds(q.getParentSampleIds());
		res.setTopSampleIds(q.getTopSampleIds());
		res.setKeywords(q.getKeywords());
		return res;
	}

	public static ResultQueryPojo convertResultQuery(ResultQuery q) {
		ResultQueryPojo res = new ResultQueryPojo();
		res.setContainerIds(q.getContainerIds());
		res.setSampleIds(q.getSampleIds());
		res.setStudyIds(q.getStudyIds());
		res.setTopSampleIds(q.getTopSampleIds());
		res.setKeywords(q.getKeywords());
		return res;
	}

	public static void main(String[] args) throws Exception {
		SpiritUser user = DAOSpiritUser.loadUser("freyssj");
//		List<Biosample> biosamples = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForStudyIds("S-00085"), user);
		List<Biosample> biosamples = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForBiotype(DAOBiotype.getBiotype("Bacteria")), user);
		Exchange exchange = new Exchange("actelion.bacteria");
		exchange.addBiosamples(biosamples);
		Exporter.write(exchange, new FileWriter("c:/tmp/bacteria.spirit"));
		
		
		Study study = DAOStudy.getStudyByStudyId("S-00085");
		exchange = new Exchange("actelion.s-00085");
		exchange.addStudies(Collections.singletonList(study));
		Exporter.write(exchange, new FileWriter("c:/tmp/s85.spirit"));

	}
}
