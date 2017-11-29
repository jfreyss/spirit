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

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.biosample.AmountUnit;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationLabeling;
import com.actelion.research.spiritcore.business.location.LocationType;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.NamedTreatment.TreatmentUnit;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.PhaseFormat;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritlib.BiosamplePojo;
import com.actelion.research.spiritlib.BiosampleQueryPojo;
import com.actelion.research.spiritlib.BiotypeMetadataPojo;
import com.actelion.research.spiritlib.BiotypePojo;
import com.actelion.research.spiritlib.ExchangePojo;
import com.actelion.research.spiritlib.GroupPojo;
import com.actelion.research.spiritlib.LocationPojo;
import com.actelion.research.spiritlib.MeasurementPojo;
import com.actelion.research.spiritlib.NamedSamplingPojo;
import com.actelion.research.spiritlib.NamedTreatmentPojo;
import com.actelion.research.spiritlib.PhasePojo;
import com.actelion.research.spiritlib.ResultPojo;
import com.actelion.research.spiritlib.ResultQueryPojo;
import com.actelion.research.spiritlib.SamplingPojo;
import com.actelion.research.spiritlib.StudyActionPojo;
import com.actelion.research.spiritlib.StudyPojo;
import com.actelion.research.spiritlib.TestAttributePojo;
import com.actelion.research.spiritlib.TestPojo;
import com.owlike.genson.Genson;

/**
 * Read an Exchange file and converts it to native Spirit objects, while considering already imported objects.
 * This class read the objects as exported, without any mapping. Mapping can however be applied as shown in the following example.
 *
 * The order of import is super important, due to the dependancies (exception is thrown if order is invalid).
 * A call to convertExchange ensures the proper order.
 *
 *
 * A typical (simplified) execution is
 * <code>
 * /////////////////////////////////////////////////////////////////////
 * // Read the exchange file
 * SpiritImporter importer = new SpiritImporter();
 * Exchange exchange = importer.read(new FileReader(new File("c:/tmp/export.spirit")));
 *
 * /////////////////////////////////////////////////////////////////////
 * //Analyze the exchange file, and then apply some mapping (optional)
 * ImporterMapping mapping = new ImporterMapping();
 * mapping.getBiotype2MappedBiotype().put("Bacteria", myBiotype);
 * ...
 * List<Biotype> biotypes = mapping.getMappedBiotypes(exchange.getBiotypes());
 * List<Biosamples> biosamples = mapping.getMappedBiosamples(exchange.getBiosamples());
 *
 * /////////////////////////////////////////////////////////////////////
 * //Save
 * JPAUtil.getEntityManager().startTransaction();
 * DAOBiotype.saveBiotypes(biotypes);
 * DAOBiotype.saveBiosamples(biosamples);
 * JPAUtil.getEntityManager().commit();
 *
 * </code>
 *
 *
 *
 * @author freyssj
 *
 */
public class Importer {

	/**
	 * Internal maps to map the imported entities to the imported objects. This is used to allow references between imported objects.
	 * Reference to existing objects are not allowed, all objects need to be in the imported file
	 */
	private Map<String, Study> studyId2study = new HashMap<>();
	private Map<String, Group> studyIdGroupName2group = new HashMap<>();
	private Map<String, Phase> studyIdPhaseName2phase = new HashMap<>();
	private Map<String, NamedTreatment> studyIdNamedTreatment2namedTreatment = new HashMap<>();
	private Map<String, NamedSampling> studyIdNamedSampling2namedSampling = new HashMap<>();

	private Map<String, Biosample> sampleId2biosample = new HashMap<>();
	private Map<String, Biotype> name2biotype = new HashMap<>();
	private Map<String, Location> fullName2location = new HashMap<>();

	private Map<String, Test> name2test = new HashMap<>();



	public static Exchange read(Reader reader) throws Exception {
		Genson gson = new Genson();
		ExchangePojo pojo = gson.deserialize(reader, ExchangePojo.class);
		if(pojo==null) throw new Exception("It seems that this is not a valid file");
		Exchange res = new Importer().convertExchange(pojo);
		return res;
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	public Exchange convertExchange(ExchangePojo c) throws Exception {
		Exchange res = new Exchange();
		res.setName(c.getName());
		res.setVersion(c.getVersion());

		res.setBiotypes(convertBiotypes(c.getBiotypes()));
		res.setTests(convertTests(c.getTests()));

		res.setStudies(convertStudies(c.getStudies()));
		res.setLocations(convertLocations(c.getLocations()));
		res.setBiosamples(convertBiosamples(c.getBiosamples()));
		res.setResults(convertResults(c.getResults()));
		return res;
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	public Set<Study> convertStudies(Collection<StudyPojo> list) throws Exception {
		if(list==null) return null;
		Set<Study> res = new HashSet<>();
		for (StudyPojo s : list) {
			if(s==null) continue;
			Study study = new Study();
			study.setId(s.getId());
			study.setStudyId(s.getStudyId());
			study.setLocalId(s.getLocalId());
			study.setTitle(s.getTitle());
			study.setBlindAllUsers(Arrays.asList(MiscUtils.split(s.getBlindAllUsers())));
			study.setBlindDetailsUsers(Arrays.asList(MiscUtils.split(s.getBlindDetailsUsers())));

			study.setCreDate(s.getCreDate());
			study.setCreUser(s.getCreUser());
			study.setDayOneDate(s.getDay1());

			study.setNotes(s.getNotes());
			PhaseFormat phaseFormat = s.getPhaseFormat()==null || s.getPhaseFormat().length()==0? null: PhaseFormat.valueOf(s.getPhaseFormat());
			study.setPhaseFormat(phaseFormat);
			study.setExpertUsers(s.getExpertUsers());

			study.setState(s.getState());
			study.setMetadataMap(s.getMetadata());
			study.setSynchronizeSamples(s.isSynchronizeSamples());
			study.setUpdDate(s.getUpdDate());
			study.setUpdUser(s.getUpdUser());
			study.setAdminUsers(s.getAdminUsers());

			study.setPhases(convertPhases(s.getPhases(), study));
			study.setGroups(convertGroups(s.getGroups(), study));
			study.setNamedSamplings(convertNamedSamplings(s.getNamedSamplings(), study));
			study.setNamedTreatments(convertNamedTreatments(s.getNamedTreatments(), study));
			study.setStudyActions(convertStudyActions(s.getStudyActions(), study));

			studyId2study.put(study.getStudyId(), study);
			res.add(study);

		}

		return res;
	}

	public Set<Group> convertGroups(Collection<GroupPojo> list, Study forStudy) throws Exception {
		Set<Group> res = new TreeSet<>();

		//Create the groups
		for (GroupPojo g : list) {
			Group group = new Group();
			group.setStudy(forStudy);
			group.setId(g.getId());
			group.setName(g.getName());
			group.setColorRgb(g.getColorRgb());
			group.setSubgroupSizes(g.getSubgroupSizes());
			//			group.setDividingSampling(g.getDividingSampling()==null? null: convertSamplingAndItsChildren(g.getDividingSampling(), null));
			studyIdGroupName2group.put(forStudy.getStudyId()+"_"+group.getName(), group);
		}

		//Recreate the linkage, be sure to call convertPhases before convertGroups
		for (GroupPojo g : list) {
			Group group = studyIdGroupName2group.get(forStudy.getStudyId()+"_"+g.getName());
			assert group!=null;

			if(g.getFromPhase()!=null && g.getFromPhase().length()>0) {
				Phase fromPhase = studyIdPhaseName2phase.get(forStudy.getStudyId()+"_"+g.getFromPhase());
				if(fromPhase==null) throw new Exception("The fromPhase "+forStudy.getStudyId()+"_"+g.getFromPhase()+" was not exported (studyIdPhaseName2phase="+studyIdPhaseName2phase+")");
				group.setFromPhase(fromPhase);
			}

			if(g.getFromGroup()!=null && g.getFromGroup().length()>0) {
				Group fromGroup = studyIdGroupName2group.get(forStudy.getStudyId()+"_"+g.getFromGroup());
				if(fromGroup==null) throw new Exception("The fromGroup "+forStudy.getStudyId()+"_"+g.getFromGroup()+" was not exported (studyIdGroupName2group="+studyIdGroupName2group+")");
				group.setFromGroup(fromGroup);
			}
			res.add(group);
		}

		return res;
	}

	public Set<Phase> convertPhases(Collection<PhasePojo> list, Study forStudy) throws Exception {
		Set<Phase> res = new TreeSet<>();
		for (PhasePojo g : list) {
			Phase p = new Phase();
			p.setStudy(forStudy);
			p.setId(g.getId());
			p.setName(g.getName());
			p.setSerializedRandomization(g.getRando());
			studyIdPhaseName2phase.put(forStudy.getStudyId()+"_"+p.getShortName(), p);
			res.add(p);
		}

		return res;
	}

	public Set<NamedTreatment> convertNamedTreatments(Collection<NamedTreatmentPojo> list, Study forStudy) throws Exception {

		Set<NamedTreatment> res = new TreeSet<>();
		for (NamedTreatmentPojo pojo : list) {
			NamedTreatment ns = new NamedTreatment();
			ns.setStudy(forStudy);
			ns.setId(pojo.getId());
			ns.setName(pojo.getName());
			ns.setApplication1(pojo.getApplication1());
			ns.setApplication2(pojo.getApplication2());
			ns.setColorRgb(pojo.getColorRgb());
			ns.setCompoundName1(pojo.getCompoundName1());
			ns.setCompoundName2(pojo.getCompoundName2());
			ns.setDose1(pojo.getDose1());
			ns.setDose2(pojo.getDose2());
			TreatmentUnit unit1 = pojo.getUnit1()==null || pojo.getUnit1().length()==0? null: TreatmentUnit.valueOf(pojo.getUnit1());
			ns.setUnit1(unit1);
			TreatmentUnit unit2 = pojo.getUnit2()==null || pojo.getUnit2().length()==0? null: TreatmentUnit.valueOf(pojo.getUnit2());
			ns.setUnit2(unit2);
			studyIdNamedTreatment2namedTreatment.put(forStudy.getStudyId()+"_"+ns.getName(), ns);
			res.add(ns);
		}
		return res;
	}


	public Set<NamedSampling> convertNamedSamplings(Collection<NamedSamplingPojo> list, Study forStudy) throws Exception {
		Set<NamedSampling> res = new TreeSet<>();
		for (NamedSamplingPojo g : list) {
			NamedSampling p = new NamedSampling();
			p.setStudy(forStudy);
			p.setId(g.getId());
			p.setName(g.getName());
			p.setNecropsy(g.isNecropsy());

			//Do recursion on children
			List<Sampling> allSamplings = new ArrayList<>();
			for (SamplingPojo sampling : g.getSamplings()) {
				convertSamplingAndItsChildren(sampling, allSamplings);
			}
			for (Sampling sampling : allSamplings) {
				sampling.setNamedSampling(p);
			}
			p.setAllSamplings(allSamplings);

			studyIdNamedSampling2namedSampling.put(forStudy.getStudyId()+"_"+p.getName(), p);
			res.add(p);
		}
		return res;
	}

	public Sampling convertSamplingAndItsChildren(SamplingPojo g, Collection<Sampling> outAllSamplings) throws Exception  {

		Sampling p = new Sampling();
		p.setAmount(g.getAmount());

		if(g.getBiotype()==null || g.getBiotype().length()==0) throw new Exception("The biotype cannot be null");
		Biotype biotype = name2biotype.get(g.getBiotype());
		if(biotype==null) throw new Exception("The biotype " + g.getBiotype() +" was not exported");

		p.setBiotype(biotype);
		p.setBlocNo(g.getBlocNo()==null?0: g.getBlocNo());
		p.setComments(g.getComments());
		p.setCommentsRequired(g.isCommentsRequired());

		if(g.getContainerType()!=null && g.getContainerType().length()>0) {
			ContainerType containerType = ContainerType.valueOf(g.getContainerType());
			if(containerType==null) throw new Exception("The containerType " + g.getContainerType() +" is invalid");
			p.setContainerType(containerType);
		}

		p.setId(g.getId());
		p.setLengthRequired(g.isLengthRequired());
		p.setSampleName(g.getSampleName());
		p.setWeighingRequired(g.isWeighingRequired());

		//Convert metadata
		for (BiotypeMetadata bm : biotype.getMetadata()) {
			String value = g.getMetadata()==null? null: g.getMetadata().get(bm.getName());
			if(value!=null && value.length()>0) p.setMetadata(bm, value);
		}

		//Convert measurements
		List<Measurement> measurements = new ArrayList<>();
		if(g.getMeasurements()!=null) {
			for(MeasurementPojo mp: g.getMeasurements()) {
				Measurement m = new Measurement(new Test(mp.getTest()), mp.getParameters());
				measurements.add(m);
			}
		}
		p.setMeasurements(measurements);
		if(outAllSamplings!=null) outAllSamplings.add(p);

		//convert children
		Set<Sampling> children = new HashSet<>();
		for (SamplingPojo childPj : g.getChildren()) {
			Sampling child = convertSamplingAndItsChildren(childPj, outAllSamplings);
			child.setParent(p);
			children.add(child);
		}
		p.setChildren(children);

		return p;
	}

	public Set<StudyAction> convertStudyActions(Collection<StudyActionPojo> list, Study forStudy) throws Exception {
		Set<StudyAction> res = new HashSet<>();
		for (StudyActionPojo g : list) {
			StudyAction action = new StudyAction();
			action.setLabel(g.getLabel());
			action.setStudy(forStudy);

			Group group = studyIdGroupName2group.get(forStudy.getStudyId()+"_"+g.getGroup());
			if(group==null) throw new Exception("The group " +forStudy.getStudyId()+"_"+g.getGroup()+" was not exported");
			action.setGroup(group);
			action.setSubGroup(g.getSubGroup());

			Phase phase = studyIdPhaseName2phase.get(forStudy.getStudyId()+"_"+g.getPhase());
			if(phase==null) throw new Exception("The phase " +forStudy.getStudyId()+"_"+g.getPhase()+" was not exported");
			action.setPhase(phase);

			if(g.getNamedSampling1()!=null && g.getNamedSampling1().length()>0) {
				NamedSampling ns1 = studyIdNamedSampling2namedSampling.get(forStudy.getStudyId()+"_"+g.getNamedSampling1());
				if(ns1==null) throw new Exception("The sampling " +forStudy.getStudyId()+"_"+g.getNamedSampling1()+" was not exported");
				action.setNamedSampling1(ns1);
			}

			if(g.getNamedSampling2()!=null && g.getNamedSampling2().length()>0) {
				NamedSampling ns2 = studyIdNamedSampling2namedSampling.get(forStudy.getStudyId()+"_"+g.getNamedSampling2());
				if(ns2==null) throw new Exception("The sampling " +forStudy.getStudyId()+"_"+g.getNamedSampling2()+" was not exported");
				action.setNamedSampling2(ns2);
			}

			if(g.getNamedTreatment()!=null && g.getNamedTreatment().length()>0) {
				NamedTreatment nt = studyIdNamedTreatment2namedTreatment.get(forStudy.getStudyId()+"_"+g.getNamedTreatment());
				if(nt==null) throw new Exception("The treatment " +forStudy.getStudyId()+"_"+g.getNamedTreatment()+" was not exported");
				action.setNamedTreatment(nt);
			}


			action.setMeasureFood(g.isMeasureFood());
			action.setMeasureWater(g.isMeasureWater());
			action.setMeasureWeight(g.isMeasureWeight());


			List<Measurement> measurements = new ArrayList<>();
			if(g.getMeasurements()!=null) {
				for(MeasurementPojo mp: g.getMeasurements()) {
					Measurement m = new Measurement(new Test(mp.getTest()), mp.getParameters());
					measurements.add(m);
				}
			}
			action.setMeasurements(measurements);

			res.add(action);
		}

		return res;
	}


	///////////////////////////////////////////////////////////////////////////////////////////
	public Set<Biotype> convertBiotypes(Collection<BiotypePojo> list) throws Exception {
		if(list==null) return null;
		if(sampleId2biosample.size()>0) throw new RuntimeException("You must call convertLocations before convertBiotypes");

		Set<Biotype> biotypes = new HashSet<>();
		for (BiotypePojo b : list) {
			Biotype biotype = new Biotype();
			biotype.setId(b.getId());
			biotype.setName(b.getName());
			biotype.setSampleNameLabel(b.getSampleNameLabel());

			BiotypeCategory cat = BiotypeCategory.valueOf(b.getCategory());
			biotype.setCategory(cat);
			biotype.setPrefix(b.getPrefix());
			biotype.setAmountUnit(b.getAmountUnit()==null || b.getAmountUnit().length()==0? null: AmountUnit.valueOf(b.getAmountUnit()));

			int index = 0;
			for(BiotypeMetadataPojo m: b.getMetadata()) {
				BiotypeMetadata m2 = convertBiotypeMetadata(m);
				m2.setIndex(index++);
				m2.setBiotype(biotype);
				biotype.getMetadata().add(m2);
			}
			biotype.setContainerType(b.getContainerType()==null || b.getContainerType().length()==0? null: ContainerType.valueOf(b.getContainerType()));
			//			biotype.setDescription(b.getDescription());

			biotype.setAbstract(b.isAbstract());
			biotype.setHidden(b.isHidden());
			biotype.setHideContainer(b.isHideContainer());
			biotype.setHideSampleId(b.isHideSampleId());
			biotype.setNameAutocomplete(b.isNameAutocomplete());
			biotype.setNameRequired(b.isNameRequired());
			biotypes.add(biotype);
			if(name2biotype.get(biotype.getName())!=null) throw new Exception("The biotype "+b.getName()+" was exported 2 times");
			name2biotype.put(biotype.getName(), biotype);
		}

		//Reestablish links between biotypes
		for (BiotypePojo b : list) {
			if(b.getParentBiotype()==null || b.getParentBiotype().length()==0) continue;
			Biotype biotype = name2biotype.get(b.getName());
			Biotype parent = name2biotype.get(b.getParentBiotype());
			if(parent==null) throw new Exception("The biotype "+b.getParentBiotype()+" was not exported");
			biotype.setParent(parent);
		}

		return biotypes;
	}

	private BiotypeMetadata convertBiotypeMetadata(BiotypeMetadataPojo m) throws Exception {
		BiotypeMetadata res = new BiotypeMetadata();
		res.setId(m.getId());
		res.setName(m.getName());
		try {
			DataType dataType = DataType.valueOf(m.getDataType());
			res.setDataType(dataType);
		} catch(Exception e) {
			throw new Exception("Invalid DataType: "+m.getDataType());
		}

		res.setParameters(m.getParameters());
		res.setRequired(m.isRequired());
		res.setSecundary(m.isSecundary());
		return res;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////
	public Set<Biosample> convertBiosamples(Collection<BiosamplePojo> list) throws Exception {
		if(list==null) return null;
		Set<Biosample> biosamples = new HashSet<>();
		for (BiosamplePojo b : list) {
			Biosample biosample = new Biosample();
			biosample.setId(b.getId());
			biosample.setSampleId(b.getSampleId());
			biosample.setSampleName(b.getSampleName());

			biosample.setContainerId(b.getContainerId());

			if(b.getContainerType()!=null && b.getContainerType().length()>0) {
				ContainerType containerType = ContainerType.valueOf(b.getContainerType());
				if(containerType==null) throw new Exception("The containerType " + b.getContainerType() +" is invalid");
				biosample.setContainerType(containerType);
			}


			//Set the biotype
			Biotype biotype = name2biotype.get(b.getBiotype());
			if(biotype==null) throw new Exception("The biotype "+b.getBiotype()+" was not exported");
			biosample.setBiotype(biotype);

			//Set the study
			if(b.getStudyId()!=null && b.getStudyId().length()>0) {
				Study study = studyId2study.get(b.getStudyId());
				if(study==null) throw new Exception("The study "+b.getStudyId()+" from "+b+" was not exported");
				biosample.setInheritedStudy(study);

				if(b.isAttached()) {
					biosample.setAttachedStudy(study);
				}
				if(b.getStudyGroup()!=null && b.getStudyGroup().length()>0) {
					Group group = studyIdGroupName2group.get(b.getStudyId()+"_"+b.getStudyGroup());
					if(group==null) throw new Exception("The group "+b.getStudyId()+"_"+b.getStudyGroup()+" from "+b+" was not exported");
					biosample.setInheritedGroup(group);
					biosample.setInheritedSubGroup(b.getStudySubGroup());
				}
				if(b.getStudyPhase()!=null && b.getStudyPhase().length()>0) {
					Phase phase = studyIdPhaseName2phase.get(b.getStudyId()+"_"+b.getStudyPhase());
					if(phase==null) throw new Exception("The phase "+b.getStudyId()+"_"+b.getStudyPhase()+" from "+b+" was not exported");
					biosample.setInheritedPhase(phase);
				}
				if(b.getAttachedSamplingId()>0) {
					Sampling s = study.getSampling(b.getAttachedSamplingId());
					if(s==null) {
						throw new Exception("The sampling of " + biosample + ": " + b.getAttachedSamplingId() + " was not exported");
					}
					biosample.setAttachedSampling(s);
				}
			}

			for(String key: b.getMetadata().keySet()) {
				biosample.setMetadataValue(key, b.getMetadata().get(key));
			}

			String fullLocation = b.getFullLocation();
			if(fullLocation!=null && fullLocation.length()>0) {
				String posString = "";
				if(fullLocation.lastIndexOf(':')>=0) {
					posString = fullLocation.substring(fullLocation.lastIndexOf(':')+1);
					fullLocation = fullLocation.substring(0, fullLocation.lastIndexOf(':'));
				}
				Location loc = fullName2location.get(fullLocation);
				if(loc==null) throw new Exception("The location "+fullLocation+" is referenced by "+b.getSampleId()+" but was not exported");
				biosample.setLocPos(loc, loc.parsePosition(posString));
			}


			biosample.setComments(b.getComments());
			if(b.getStatus()!=null && b.getStatus().length()>0) {
				try {
					Status status = Status.valueOf(b.getStatus());
					biosample.setStatus(status);
				} catch(Exception e) {
					throw new Exception("The status "+b.getStatus()+" is invalid");
				}
			}
			if(b.getQuality()!=null && b.getQuality().length()>0) {
				try {
					Quality quality = Quality.valueOf(b.getQuality());
					biosample.setQuality(quality);
				} catch(Exception e) {
					throw new Exception("The quality "+b.getStatus()+" is invalid");
				}
			}
			if(b.getAmount()!=null && b.getAmount().length()>0) {
				try {
					biosample.setAmount(Double.parseDouble(b.getAmount()));
				} catch(Exception e) {
					throw new Exception("The amount "+b.getAmount()+" is invalid");
				}
			}
			biosample.setUpdDate(b.getUpdDate());
			biosample.setUpdUser(b.getUpdUser());
			biosample.setCreDate(b.getCreDate());
			biosample.setCreUser(b.getCreUser());

			biosamples.add(biosample);
			if(sampleId2biosample.get(biosample.getSampleId())!=null) throw new Exception("The biosample "+biosample.getSampleId()+" was exported 2 times");
			sampleId2biosample.put(biosample.getSampleId(), biosample);

		}

		//Reestablish links between biosamples
		for (BiosamplePojo b : list) {
			if(b.getParentSampleId()==null || b.getParentSampleId().length()==0) continue;
			Biosample biosample = sampleId2biosample.get(b.getSampleId());
			Biosample parent = sampleId2biosample.get(b.getParentSampleId());
			if(parent==null) {
				throw new Exception("The biosample "+b.getParentSampleId()+" was not exported (sample="+b.getSampleId()+")");
			}
			biosample.setParent(parent);
		}
		return biosamples;
	}


	public Set<Location> convertLocations(Collection<LocationPojo> list) throws Exception {
		if(list==null) return null;
		if(sampleId2biosample.size()>0) throw new RuntimeException("You must call convertLocations before convertBiosamples");
		Set<Location> res = new HashSet<>();
		for (LocationPojo l : list) {
			Location r = new Location();
			r.setId(l.getId());
			r.setName(l.getName());
			r.setDescription(l.getDescription());
			r.setCols(l.getCols());
			r.setRows(l.getRows());
			r.setLocationType(l.getLocationType()==null || l.getLocationType().length()==0? null: LocationType.valueOf(l.getLocationType()));
			r.setLabeling(l.getLabeling()==null || l.getLabeling().length()==0? null: LocationLabeling.valueOf(l.getLabeling()));
			r.setUpdDate(l.getUpdDate());
			r.setUpdUser(l.getUpdUser());
			r.setCreDate(l.getCreDate());
			r.setCreUser(l.getCreUser());
			res.add(r);
			if(fullName2location.get(l.getFullName())!=null) throw new Exception("The location "+l.getFullName()+" was exported 2 times");
			fullName2location.put(l.getFullName(), r);
		}

		//Reestablish links between locations
		for (LocationPojo l : list) {
			if(l.getParent()==null) continue;
			Location location = fullName2location.get(l.getFullName());
			Location parent = fullName2location.get(l.getParent());
			if(parent!=null) {
				location.setParent(parent);
			} else {
				//Accept that location don't have their parent
			}
		}


		return res;
	}


	public BiosampleQuery convertBiosampleQuery(BiosampleQueryPojo q) {
		BiosampleQuery res = new BiosampleQuery();
		res.setContainerIds(q.getContainerIds());
		res.setSampleIds(q.getSampleIds());
		res.setStudyIds(q.getStudyIds());
		res.setParentSampleIds(q.getParentSampleIds());
		res.setTopSampleIds(q.getTopSampleIds());
		res.setKeywords(q.getKeywords());
		return res;
	}

	public ResultQuery convertResultQuery(ResultQueryPojo q) {
		ResultQuery res = new ResultQuery();
		res.setContainerIds(q.getContainerIds());
		res.setSampleIds(q.getSampleIds());
		res.setStudyIds(q.getStudyIds());
		res.setTopSampleIds(q.getTopSampleIds());
		res.setKeywords(q.getKeywords());
		return res;
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	public Set<Test> convertTests(Collection<TestPojo> list) throws Exception {
		if(list==null) return null;

		Set<Test> tests = new HashSet<>();
		for (TestPojo t : list) {
			Test test = new Test();
			test.setId(t.getId());
			test.setName(t.getName());
			test.setCategory(t.getCategory());

			int index = 0;
			for(TestAttributePojo ta: t.getAttributes()) {
				TestAttribute ta2 = convertTestAttribute(ta);
				ta2.setIndex(index++);
				ta2.setTest(test);
				test.getAttributes().add(ta2);
			}

			tests.add(test);
			if(name2test.get(test.getName())!=null) throw new Exception("The test "+t.getName()+" was exported 2 times");
			name2test.put(test.getName(), test);
		}

		return tests;
	}

	private TestAttribute convertTestAttribute(TestAttributePojo ta) throws Exception {
		TestAttribute res = new TestAttribute();
		res.setId(ta.getId());
		res.setName(ta.getName());
		try {
			DataType dataType = DataType.valueOf(ta.getDataType());
			res.setDataType(dataType);
		} catch(Exception e) {
			throw new Exception("Invalid DataType: "+ta.getDataType());
		}

		try {
			OutputType outputType = OutputType.valueOf(ta.getOutputType());
			res.setOutputType(outputType);
		} catch(Exception e) {
			throw new Exception("Invalid OutputType: "+ta.getDataType());
		}
		res.setParameters(ta.getParameters());
		res.setRequired(ta.isRequired());
		return res;
	}

	public Set<Result> convertResults(Collection<ResultPojo> pojos) throws Exception {
		Set<Result> results = new HashSet<>();
		for (ResultPojo r : pojos) {

			Result res = new Result();
			res.setId(r.getId());
			res.setElb(r.getElb());

			//Set the test
			Test test = name2test.get(r.getTestName());
			if(test==null) throw new Exception("The Test "+r.getTestName()+" was not exported");
			res.setTest(test);

			//Convert the values
			for(Map.Entry<String, String> e : r.getValues().entrySet()) {
				res.setValue(e.getKey(), e.getValue());
			}

			//Set the biosample
			if(r.getSampleId()!=null && r.getSampleId().length()>0) {
				Biosample biosample = sampleId2biosample.get(r.getSampleId()); // r.getSampleId()==null? null: DAOBiosample.getBiosample(r.getSampleId());
				if(biosample==null) throw new Exception("The sampleId " + r.getSampleId() + " is invalid (result="+res+")");
				res.setBiosample(biosample);
			}


			//Set the phase
			if(r.getPhase()!=null && r.getPhase().length()>0) {
				if(res.getBiosample()==null) throw new Exception("Result.Phase is not null but Result.Biosample is null (result="+res+")");
				if(res.getBiosample().getInheritedStudy()==null) throw new Exception("Result.Phase is not null but Result.Biosample.Study is null (sampleid="+res.getBiosample().getSampleId()+", phase="+r.getPhase()+")");
				Phase p = studyIdPhaseName2phase.get(res.getBiosample().getInheritedStudy().getStudyId()+"_"+r.getPhase());
				if(p==null) throw new Exception("Result.Phase is invalid as "+res.getBiosample().getInheritedStudy().getStudyId()+"_"+r.getPhase()+" was not exported");
				res.setPhase(p);
			}

			//Set the generic fields
			res.setQuality(Quality.get(r.getQuality()));
			res.setComments(r.getComments());
			res.setUpdDate(r.getUpdDate());
			res.setUpdUser(r.getUpdUser());
			res.setCreDate(r.getCreDate());
			res.setCreUser(r.getCreUser());

			results.add(res);
		}
		return results;
	}
}
