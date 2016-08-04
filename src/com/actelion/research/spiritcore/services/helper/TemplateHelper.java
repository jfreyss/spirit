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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.slide.ContainerTemplate;
import com.actelion.research.spiritcore.business.slide.SampleDescriptor;
import com.actelion.research.spiritcore.business.slide.Template;
import com.actelion.research.spiritcore.business.slide.ContainerTemplate.Duplicate;
import com.actelion.research.spiritcore.services.dao.DAOBarcode;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;

@SuppressWarnings("unchecked")
public class TemplateHelper {

	/**
	 * Class used to memorize the slide barcodes in one run, so we don't generate unnecessary codebars.
	 * <pre>
	 * 		//to generate id
	 * 		for(int i=0; i<10; i++) {
	 * 			String id = barcodePrefixMemo.getNextId();
	 * 		}
	 * 	
	 * 		//cancel the generated ones
	 * 		barcodePrefixMemo.init();
	 * 
	 * 		//regenerate id
	 * 		for(int i=0; i<10; i++) {
	 * 			String id = barcodePrefixMemo.getNextId(); //id = same as before
	 * 		}
	 * </pre>
	 * 
	 * @author freyssj
	 *
	 */
	public static class BarcodeMemo {

		private final ContainerType containerType;
		private List<String> slidePrefixes = new ArrayList<String>();
		private int index = 0;
		
		public BarcodeMemo(ContainerType containerType) {
			this.containerType = containerType;
		}
		
		public ContainerType getContainerType() {
			return containerType;
		}
		
		public void init() {
			index = 0;
		}
		
		public String getNextId() {
			if(index<slidePrefixes.size()) {
				return slidePrefixes.get(index++);			
			} else {
				String prefix = DAOBarcode.getNextId(containerType);
				slidePrefixes.add(prefix);
				index++;
				return prefix;
			}
		}
		
	}

	/**
	 * Apply a SlideTemplate to the given groups of biosamples.
	 * 
	 * The size of each group must not be higher that the number of animals used in the template.
	 * 
	 * 
	 * 
	 * @param tpl
	 * @param groups
	 * @param barcodePrefixMemo
	 * @param userName
	 * @param htmlMsgs
	 * @return
	 * @throws Exception
	 */
	public static List<Biosample> applyTemplate(Template tpl, List<List<Biosample>> groups, BarcodeMemo barcodePrefixMemo, String userName, StringBuilder htmlMsgs) throws Exception {
		
		List<Biosample> res = new ArrayList<>();
		ContainerType containerType = barcodePrefixMemo.getContainerType();
		
		int total = 0;
		
		htmlMsgs.append("<table>");
		for (List<Biosample> group : groups) {
			//Write animalNos in table
			htmlMsgs.append("<tr><td valign=top style='font-weight:bold'>");
			for (Biosample b : group) {
				htmlMsgs.append(b.getSampleIdName());
			}
			htmlMsgs.append("</td><td valign=top>");
			
			//Loop through each template and apply template
			String prefixId = barcodePrefixMemo.getNextId();
			for (int i=0; i<tpl.getContainerTemplates().size(); i++ ) {
				ContainerTemplate template = tpl.getContainerTemplates().get(i);							
				
				
				//Apply template
				List<Biosample> created;
				String containerId = prefixId + "-" + template.getBlocNo();
				if(tpl.getContainerType()==ContainerType.K7) {
					created = applyTemplateForCassette(template, group, containerId, userName, htmlMsgs);
				} else if(tpl.getContainerType()==ContainerType.SLIDE) {
					created = applyTemplateForSlide(template, group, containerId, userName, htmlMsgs, res);
				} else {
					throw new Exception("Cannot generate: "+tpl.getContainerType());
				}
				res.addAll(created);
				int newContainers = Biosample.getContainers(created).size();
				total += newContainers;
				htmlMsgs.append(containerType.getName() + " Bl." + template.getBlocNo() +" -> "+ newContainers + " <br>");
			}	
			htmlMsgs.append("</td></tr>");
		}	
		htmlMsgs.append("</table>");
		htmlMsgs.append("<b>Total of "+total+" new " + containerType.getName() + "s</b>");
		
		return res;
	}
	
	private static List<Biosample> applyTemplateForSlide(ContainerTemplate template, List<Biosample> animalsFor1Generation, String prefixContainerId, String userName, StringBuilder errors, List<Biosample> alreadyCreated) throws Exception  {
		
		Biotype sliceType = DAOBiotype.getBiotype(Biotype.SLICE);
		if(sliceType==null) throw new Exception("The type '"+Biotype.SLICE+"' must be created");
		
		Set<String> createdSampledIds = Biosample.getSampleIds(alreadyCreated);
		Set<String> createdContainerIds = Biosample.getContainerIds(alreadyCreated);		
		for (SampleDescriptor s : template.getSampleDescriptors()) {
			if(s.getAnimalNo()>=animalsFor1Generation.size()) {
				errors.append("<span style='color:red'>The slide requires at least " + s.getAnimalNo()+"</span></br>");
				return new ArrayList<>();
			}
		}

		//Find biosamples on which a template can be applied (no slides)
		Set<Biosample> inSlides = new TreeSet<>(); 
		Set<Biosample>[] terminals = new Set[animalsFor1Generation.size()];
		for (int i = 0; i < terminals.length; i++) {
			
			//Find all the children of this animals (except slides)
			terminals[i] = animalsFor1Generation.get(i).getHierarchy(HierarchyMode.CHILDREN);

			for (Iterator<Biosample> iterator = terminals[i].iterator(); iterator.hasNext();) {
				Biosample b = iterator.next();
				if(b.getId()<=0) {
					iterator.remove();
				} else if(b.getContainerType()==ContainerType.SLIDE) {
					inSlides.add(b);
					iterator.remove();
				}				
			}
			
		}


		////////////
		//check if a compatible slide was already generated, if so makes a warning and reuse the prefix
		existing: for (Container s : Biosample.getContainers(inSlides)) {
			if(s.getBiosamples().size()!=template.getSampleDescriptors().size()) continue;

			List<Biosample> content = new ArrayList<Biosample>( s.getBiosamples());
			for (int cIndex = 0; cIndex < template.getSampleDescriptors().size(); cIndex++) {
				if(!template.getSampleDescriptors().get(cIndex).isBiosampleCompatible(content.get(cIndex))) continue existing;
			}
			
			//This slide is compatible, adds a warning and reuse the prefix
			prefixContainerId = s.getContainerPrefix();
			errors.append("<span style='color:#AA5500'>The same template has already been applied: reuse the existing prefix " + prefixContainerId + "</span><br>");
			break;
		}
	
		errors.append("prefixContainerId="+prefixContainerId+"<br>");
		errors.append("prefixContainerId="+prefixContainerId+" ");

		int offset = 0;
		List<Biosample> res = new ArrayList<>();
		for (Duplicate duplicate : template.getDuplicates()) {
			int nDuplicates = duplicate.getNDuplicates();
			String staining = duplicate.getStaining();
			String sectionNo = duplicate.getSectionNo();
			
			for (int i = 0; i < nDuplicates; i++) {				
				//Find containerId
				String containerId;	
				do {
					offset++;
					containerId = prefixContainerId + Container.BLOC_SEPARATOR + offset;//new DecimalFormat("000").format(offset);					
				}  while (createdContainerIds.contains(containerId) || DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForContainerIds(Collections.singletonList(containerId)), null).size()>0);
				createdContainerIds.add(containerId);
				
				
				//Create the container
				Container container = new Container(ContainerType.SLIDE, containerId);
				List<Biosample> biosamples = new ArrayList<>();
				
				//Add the samples to the container
				for (int cIndex = 0; cIndex < template.getSampleDescriptors().size(); cIndex++) {			
					SampleDescriptor s = template.getSampleDescriptors().get(cIndex);
					
					//Find compatible sample
					Biosample compatible = null;
					findCompatible: for (Biosample b : new ArrayList<Biosample>(terminals[s.getAnimalNo()])) {
						if(s.isBiosampleCompatible(b)) {
							compatible = b;
							break findCompatible;
						}
					}
					if(compatible==null) {
						//No sample found, skip this container's creation
						errors.append("<span style='color:red'><b>"+animalsFor1Generation.get(s.getAnimalNo()).getSampleIdName()+"</b> does not have a: <b>" + s + "</b></span><br>");
						break;
						
					} else {
						//We create slides, so new slices have to be created (children of the organ)
						String sampleId;
						int n = i;
						do {
							n++;
							sampleId = compatible.getSampleId() + "-" + n;
						} while(createdSampledIds.contains(sampleId) || DAOBiosample.getBiosample(sampleId)!=null);
						createdSampledIds.add(sampleId);
						
						//Create the sample
						Biosample slice = new Biosample(sliceType);
						slice.setParent(compatible, false);
						slice.setSampleId(sampleId);
						if(slice.getMetadata("Staining")==null) throw new Exception("Staining is not a valid metadata");
						if(slice.getMetadata("SectionNo")==null) throw new Exception("SectionNo is not a valid metadata");
						slice.setMetadata("Staining", staining);
						slice.setMetadata("SectionNo", sectionNo);
						slice.setInheritedStudy(compatible.getInheritedStudy());
						slice.setInheritedGroup(compatible.getInheritedGroup());
						slice.setInheritedPhase(compatible.getInheritedPhase());
						slice.setTopParent(compatible.getTopParent());
						slice.setContainer(container);
						slice.setContainerIndex(template.getSampleDescriptors().size()==1? null: cIndex);
						biosamples.add(slice);
					}
				}
				res.addAll(biosamples);
			}
			

		}
		return res;
		
	}
	
	/**
	 * 
	 * @param template
	 * @param group
	 * @param containerId
	 * @param userName
	 * @param errors
	 * @param res
	 * @throws Exception
	 */
	private static List<Biosample> applyTemplateForCassette(ContainerTemplate template, List<Biosample> group, String containerId, String userName, StringBuilder errors) throws Exception  {
		
		for (SampleDescriptor s : template.getSampleDescriptors()) {
			if(s.getAnimalNo()>=group.size()) {
				//Incomplete Cassette: warning but no error
				errors.append("<span style='color:#FFAA00'>There are only " + group.size()+" samples for this cassette</span><br>");
			} 
		}

		//Find biosamples on which a template can be applied (no slides)
		Set<Biosample> inCassettes = new TreeSet<>(); 
		Set<Biosample>[] children = new Set[group.size()];
		for (int i = 0; i < children.length; i++) {
			
			//Find all the children of this animals (except slides)
			children[i] = new TreeSet<>(group.get(i).getHierarchy(HierarchyMode.CHILDREN));

			for (Iterator<Biosample> iterator = children[i].iterator(); iterator.hasNext();) {
				Biosample b = iterator.next();
				if(b.getId()<=0) {
					iterator.remove();
				} else if(b.getContainerType()==ContainerType.SLIDE) {
					iterator.remove();
				} else if(b.getContainerType()==ContainerType.K7) {
					inCassettes.add(b);
				}				
			}
			
		}

		
		//Create the cassette
		Container container = new Container(ContainerType.K7, containerId);

		//Add the samples to the container
		List<Biosample> biosamples = new ArrayList<>();
		for (int cIndex = 0; cIndex < template.getSampleDescriptors().size(); cIndex++) {			
			SampleDescriptor s = template.getSampleDescriptors().get(cIndex);
			
			//Skip this sample if the related animal was not given
			if(s.getAnimalNo()>=children.length) continue;
			
			//Find compatible sample
			Biosample compatible = null;
			for (Biosample b : new ArrayList<>(children[s.getAnimalNo()])) {
				if(s.isBiosampleCompatible(b)) {
					compatible = b;
					break;
				}
			}
			if(compatible==null) {
				//No sample found, skip this container's creation
				errors.append("<span style='color:red'><b>"+group.get(s.getAnimalNo()).getSampleIdName()+"</b> does not have a: <b>" + s + "</b></span><br>");
				return new ArrayList<>();
			}
			//We create Cassettes. No new samples but we move organs to the cassette
			compatible.setContainer(container);
			compatible.setContainerIndex(cIndex);
			biosamples.add(compatible);
		}
			
		return biosamples;
		
	}
	
	
}
