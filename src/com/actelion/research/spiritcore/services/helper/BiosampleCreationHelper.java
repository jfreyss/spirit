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

package com.actelion.research.spiritcore.services.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.biosample.BarcodeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.dao.DAOBarcode;

public class BiosampleCreationHelper {
	
	/**
	 * Return a list of samples that should be created to be consistent with the study design.
	 * In parallel, populates the list of samples to remove
	 * @param study
	 * @return
	 * @throws Exception
	 */
	public static List<Biosample> processDividingSamples(Study study, List<Biosample> dividingBiosamplesToRemove) throws Exception {

		List<Biosample> toSave = new ArrayList<>();
		
		///////////////////////////////////////////////
		//Create or update dividing samples
		List<Biosample> dividingBiosamplesToAdd = new ArrayList<>();
		List<Biosample> dividingBiosamplesToUpdate = new ArrayList<>();
		if(dividingBiosamplesToRemove==null) dividingBiosamplesToRemove = new ArrayList<>();
		
		Map<Group, int[]> group2left = new HashMap<Group, int[]>();
		for(Group g: study.getGroups()) {
			if(g.getDividingSampling()==null) continue;
			int[] left = new int[g.getNSubgroups()];
			for (int i = 0; i < left.length; i++) {
				left[i] = g.getSubgroupSize(i);
			}
			group2left.put(g, left);
		}
		
		for (Biosample b : study.getTopAttachedBiosamples()) {
			if(b.getInheritedGroup()==null || b.getInheritedGroup().getDividingGroups()==null) continue;
			for(Group dividingGroup: b.getInheritedGroup().getDividingGroups()) {
				Sampling sampling = dividingGroup.getDividingSampling();
				int[] left = group2left.get(dividingGroup);
				int subgroup = 0;
				while(subgroup<left.length && left[subgroup]==0) subgroup++;
				if(subgroup>=left.length) subgroup = 0;
				
				if(subgroup<left.length) left[subgroup]--;
				
				
				//find compatible one?
				Biosample found = null;
				for(Biosample c: b.getChildren()) {
					if(!study.equals(c.getAttachedStudy()) || !dividingGroup.equals(c.getInheritedGroup())) continue;
					
					if(sampling.getMatchingScore(c)==1) {
						found = c;
						break;
					} else if(sampling.getMatchingScore(c)>.8) {
						found = c;
					}
				}
				
				if(found!=null) {
					//we update the found biosample
					if(found.getInheritedSubGroup()!=subgroup) {
						found.setInheritedSubGroup(subgroup);
						dividingBiosamplesToUpdate.add(found);						
					}
					
					//we remove all extra compatible (should always be empty, but we never know if they are created by hand)
					for(Biosample c: b.getChildren()) {
						if(!study.equals(c.getAttachedStudy()) || !dividingGroup.equals(c.getInheritedGroup())) continue;
						if(!c.equals(found)) dividingBiosamplesToRemove.add(c);
					}
				} else {
					//we add the missing biosample
					Biosample child = sampling.createCompatibleBiosample();
					child.setParent(b);
					child.setInheritedStudy(study);
					child.setAttachedStudy(study);
					child.setAttached(study, dividingGroup, subgroup);
					child.setSampleId(DAOBarcode.getNextId(child.getBiotype()));
					dividingBiosamplesToAdd.add(child);
				}
				
				

			}
		}
		
		
		toSave.addAll(dividingBiosamplesToAdd);
		toSave.addAll(dividingBiosamplesToUpdate);
		
		return toSave;
	}

	/**
	 * Retrieve or create biosamples matching the given criteria
	 * Notes:
	 * - The newly created samples have no sampleIds yet
	 * 
	 * @param ns - null for all sampling
	 * @param study
	 * @param phases - null for all phases
	 * @param groups - null for all group
	 * @param typeFilter - null for all types
	 * @param containerFilter - null for all containers
	 * @param onlyWithRequiredAction
	 * @param animalFilters - null for all animals
	 * @param generateContainers
	 * @return
	 * @throws Exception
	 */
	public static List<Biosample> processTemplateInStudy(Study study, NamedSampling ns, Collection<Phase> phases, ContainerType containerFilter, List<Biosample> animalFilters) {
		assert study!=null;
		List<Biosample> biosamples = new ArrayList<>();		
		if(phases==null) phases = new ArrayList<>(study.getPhases());
				
		LoggerFactory.getLogger(BiosampleCreationHelper.class).debug("processTemplateInStudy for "+study+" n="+study.getAttachedBiosamples().size());

		for(Biosample animal: study.getAttachedBiosamples()) {			
			//filter by animals?
			if(animalFilters!=null && !animalFilters.contains(animal)) continue;

			for (Phase phase : phases) {
				
				//Find the applicable sampling
				StudyAction action = animal.getStudyAction(phase);
				if(action==null) continue;
				
				
				//Should we apply the sampling on this action
				if(action.getNamedSampling1()!=null && (ns==null || ns.equals(action.getNamedSampling1()))) {
					LoggerFactory.getLogger(BiosampleCreationHelper.class).debug("Apply " +phase+" "+action.getNamedSampling1());
					for (Sampling topSampling : action.getNamedSampling1().getTopSamplings()) {
						retrieveOrCreateSamplesRec(phase, animal, topSampling, biosamples);
					}
				}
				if(action.getNamedSampling2()!=null && (ns==null || ns.equals(action.getNamedSampling2()))) {
					for (Sampling topSampling : action.getNamedSampling2().getTopSamplings()) {
						retrieveOrCreateSamplesRec(phase, animal, topSampling, biosamples);
					}
				}
			}
		}
		
		//Filter Samples
		List<Biosample> res = new ArrayList<>();
		for (Biosample biosample : biosamples) {
			if(containerFilter!=null && !containerFilter.equals(biosample.getContainerType())) {
				//Don't add
			} else {
				//Add this biosample and its parents
				Biosample b = biosample;//
				while(b!=null && biosamples.contains(b)) {
					if(!res.contains(b)) res.add(b);
					b = b.getParent();
				}
			}
		}
		//generateContainers
		List<Biosample> filtered = new ArrayList<>();
		for (Biosample b : res) {
			if(b.getContainerType()==null) continue; 
			filtered.add(b);
		}
		LoggerFactory.getLogger(BiosampleCreationHelper.class).debug("processTemplateInStudy for "+study+" n="+res.size()+" containers="+filtered.size());
		assignContainers(biosamples, filtered);
		
		return res;
	}
	
	
	/**
	 * Process NamedSampling for a list of parent biosamples
	 * @param ns
	 * @param parents
	 * @return
	 * @throws Exception
	 */
	public static List<Biosample> processTemplateOutsideStudy(NamedSampling ns, Collection<Biosample> parents, boolean generateContainers) throws Exception {
		
		//Create samples
		List<Biosample> biosamples = new ArrayList<>();	
		for (Biosample parent : parents) {
//			biosamples.add(parent);		
			for (Sampling topSampling : ns.getTopSamplings()) {
				retrieveOrCreateSamplesRec(null, parent, topSampling, biosamples);
			}			
		}
		
		//generateContainers
		if(generateContainers) {
			List<Biosample> filtered = new ArrayList<>();
			for (Biosample b : biosamples) {
				if(b.getContainer()!=null/* && b.getContainer().getId()>0*/) continue; 
				filtered.add(b);
			}
			assignContainers(biosamples, filtered);
		}

		//Remove link to Sampling (we don't want the linkage outside study)
		for (Biosample biosample : biosamples) {
			biosample.setAttachedSampling(null);
		}
		return biosamples;
	}
	
	public static void assignContainers(Collection<Biosample> biosamplesWithExistingContainers, Collection<Biosample> biosamplesToAssign) {
		
		Map<String, Container> key2Containers = new HashMap<>();
		Map<String, String> map2prefix = new HashMap<>();
		
		//Map existing multiple containers
		for (Biosample b : biosamplesWithExistingContainers) {
			Sampling s = b.getAttachedSampling();
			if(s==null || s.getContainerType()==null) continue;
			
			if(b.getContainer()!=null && b.getContainerId()!=null && b.getContainerType()!=null && b.getContainerType().isMultiple() /*&& b.getContainer().getId()>0*/) {
				//The container is already saved, map it in order to potentially reuse it
				String key = b.getInheritedPhase() + "_" + b.getTopParent().getSampleId() + "_"+ s.getContainerType() + "_" + s.getBlocNo();				
				key2Containers.put(key, b.getContainer());
			}
		}
	
		//Create new containers
		for (Biosample b : biosamplesToAssign) {
			Sampling s = b.getAttachedSampling();
			if(s==null) continue;
			
			if(s.getContainerType()==null) {
				//No container -> unset
				b.setContainer(null);
			} else if(!s.getContainerType().isMultiple() || s.getBlocNo()==null) {
				//Container but no bloc, assign a new container type
				if(b.getContainerType()==s.getContainerType()) {
					continue; //Already done
				} else {
					b.setContainer(new Container(s.getContainerType()));
				}
			} else {
				//The container is a new multiple container, check if we have a container already to add it into.					
				//If there is a container, we add it
				//If not, we create it, and add it into
				
				if(b.getContainer()!=null && b.getContainerType()==s.getContainerType() && b.getContainer().getBlocNo()==s.getBlocNo()) {
					continue; //Already done
				} 
				
				
				String key = b.getInheritedPhase() + "_" + b.getTopParent().getSampleId() + "_"+ s.getContainerType() + "_" + s.getBlocNo();				
				Container container = key2Containers.get(key);
				if(container==null) {
					String prefix;
					if(s.getContainerType().getBarcodeType()==BarcodeType.GENERATE) {
						String key2 = b.getTopParent().getSampleId() + "_" + s.getContainerType();
						prefix = map2prefix.get(key2);
						if(prefix==null) {
							prefix = DAOBarcode.getNextId(s.getContainerType());
							map2prefix.put(key2, prefix);
						}
						
					} else {
						prefix = b.getSampleId();
					}
					String containerId = prefix + "-" + (s.getBlocNo());
					container = new Container(s.getContainerType(), containerId);						
					key2Containers.put(key, container);
				}
				LoggerFactory.getLogger(BiosampleCreationHelper.class).debug("Assign " + container + " to "+b);
				b.setContainer(container);	
			}				

		}
	}

	
	/**
	 * Adds the existing sample (or create new one), matching the given phase, parent and sampling definition
	 * @param phase - null if outside a study, not null if within a study
	 * @param parent - the parent from which we create the samples
	 * @param sampling - the current sampling to be applied
	 * @param res - list of retrieved or created samples 
	 */
	private static void retrieveOrCreateSamplesRec(Phase phase, Biosample parent, Sampling sampling, List<Biosample> res) {
		boolean isNecropsy = sampling.getNamedSampling()!=null && sampling.getNamedSampling().isNecropsy();
		
		if(isNecropsy && (parent.getTopParentInSameStudy().getStatus()==Status.DEAD || parent.getTopParentInSameStudy().getStatus()==Status.KILLED)) {
			return;
		}
		
		Phase phaseOfSample;
		if(phase==null) {
			phaseOfSample = parent.getInheritedPhase();
		} else{
			Phase endPhase = parent.getTopParentInSameStudy().getEndPhase();
			phaseOfSample = isNecropsy && endPhase!=null? endPhase: phase;
		}
		
		//Find compatible biosamples
		List<Biosample> compatibles = new ArrayList<>();
		if(phase!=null) {
			for (Biosample biosample : parent.getChildren()) {
				if(res.contains(biosample)) continue;				
				if(!phaseOfSample.equals(biosample.getInheritedPhase())) continue;
				if(!sampling.equals(biosample.getAttachedSampling())) continue;
					
				//We found a compatible one
				compatibles.add(biosample);
			}
		}

		if(compatibles.size()==0) {
			//Create a compatible biosample
			Biosample created = sampling.createCompatibleBiosample();			
			created.setAttachedSampling(sampling);
			created.setParent(parent);
			parent.getChildren().add(created);
			created.setInheritedStudy(parent.getInheritedStudy());
			created.setInheritedGroup(parent.getInheritedGroup());
			created.setInheritedSubGroup(parent.getInheritedSubGroup());
			created.setInheritedPhase(phaseOfSample);
			compatibles.add(created);
		} 
		LoggerFactory.getLogger(BiosampleCreationHelper.class).debug("Create sample for "+phase+" "+parent+" "+sampling+" found="+compatibles.size());

		Biosample b = compatibles.get(0);
		res.add(b);
		for (Sampling s : sampling.getChildren()) {
			retrieveOrCreateSamplesRec(phase, b, s, res);
		}
		
	}
	
}
