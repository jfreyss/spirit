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

package com.actelion.research.spiritcore.services.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.biosample.edit.EditBiosampleTable;
import com.actelion.research.spiritapp.ui.util.correction.Correction;
import com.actelion.research.spiritapp.ui.util.correction.CorrectionDlg;
import com.actelion.research.spiritapp.ui.util.correction.CorrectionMap;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.ValidationException;
import com.actelion.research.spiritcore.business.biosample.BarcodeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOBarcode;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;

public class BiosampleCreationHelper {

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

		LoggerFactory.getLogger(BiosampleCreationHelper.class).debug("processTemplateInStudy for "+study+" n="+study.getParticipants().size());

		for(Biosample animal: study.getParticipants()) {
			//filter by animals?
			if(animalFilters!=null && !animalFilters.contains(animal)) continue;

			for (Phase phase : phases) {

				//Find the applicable sampling
				StudyAction action = animal.getStudyAction(phase);
				if(action==null) continue;

				//Should we apply the sampling on this action
				if(action.getNamedSampling1()!=null && (ns==null || ns.equals(action.getNamedSampling1()))) {
					LoggerFactory.getLogger(BiosampleCreationHelper.class).debug("Apply " + animal +  " " + phase+" "+action.getNamedSampling1());
					for (Sampling topSampling : action.getNamedSampling1().getTopSamplings()) {
						retrieveOrCreateSamplesRec(phase, animal, topSampling, biosamples);
					}
				}
				if(action.getNamedSampling2()!=null && (ns==null || ns.equals(action.getNamedSampling2()))) {
					LoggerFactory.getLogger(BiosampleCreationHelper.class).debug("Apply " + animal +  " " + phase+" "+action.getNamedSampling2());
					for (Sampling topSampling : action.getNamedSampling2().getTopSamplings()) {
						retrieveOrCreateSamplesRec(phase, animal, topSampling, biosamples);
					}
				}
			}
		}

		//Filter Samples by container and sort them
		List<Biosample> sortedSamples = new ArrayList<>();
		for (Biosample biosample : biosamples) {
			if(containerFilter!=null && !containerFilter.equals(biosample.getContainerType())) continue;
			sortedSamples.add(biosample);
		}
		Collections.sort(sortedSamples);

		//Assign Containers
		List<Biosample> sampleToAssignContainers = new ArrayList<>();
		for (Biosample b : sortedSamples) {
			if(b.getContainerType()!=null) sampleToAssignContainers.add(b);
		}
		LoggerFactory.getLogger(BiosampleCreationHelper.class).debug("processTemplateInStudy for "+study+" n="+sortedSamples.size()+" sampleToAssignContainers="+sampleToAssignContainers.size());
		assignContainers(biosamples, sampleToAssignContainers);

		return sortedSamples;
	}


	/**
	 * Process NamedSampling for a list of parent biosamples
	 * @param ns
	 * @param parents
	 * @return
	 * @throws Exception
	 */
	public static List<Biosample> processTemplate(Phase phase, NamedSampling ns, Collection<Biosample> parents, boolean generateContainers) throws Exception {

		LoggerFactory.getLogger(BiosampleCreationHelper.class).debug("processTemplateOutsideStudy for "+ns+" n="+parents.size());

		//Create samples
		List<Biosample> biosamples = new ArrayList<>();
		for (Biosample parent : parents) {
			for (Sampling topSampling : ns.getTopSamplings()) {
				retrieveOrCreateSamplesRec(phase, parent, topSampling, biosamples);
			}
		}

		//generateContainers
		if(generateContainers) {
			assignContainers(biosamples, biosamples);
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
				String key = b.getContainerType()+"_"+b.getInheritedPhase() + "_" + b.getTopParentInSameStudy().getSampleId() + "_"+ s.getContainerType() + "_" + s.getBlocNo();
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
				if(b.getContainerType()!=s.getContainerType()) {
					b.setContainer(new Container(s.getContainerType()));
				}
			} else if(b.getContainer()!=null && b.getContainerType()==s.getContainerType() && CompareUtils.equals(b.getContainer().getBlocNo(), s.getBlocNo())) {
				//Already done
			} else {
				//The container is a new multiple container, check if we have a container already to add it into.
				//If there is a container, we add it
				//If not, we create it, and add it into

				String key = b.getContainerType()+"_"+b.getInheritedPhase() + "_" + b.getTopParentInSameStudy().getSampleId() + "_"+ s.getContainerType() + "_" + s.getBlocNo();
				Container container = key2Containers.get(key);
				if(container==null) {
					String prefix;
					if(s.getContainerType().getBarcodeType()==BarcodeType.GENERATE) {
						String key2 = b.getContainerType()+"_"+b.getTopParentInSameStudy().getSampleId() + "_" + s.getContainerType();
						prefix = map2prefix.get(key2);
						if(prefix==null) {
							prefix = DAOBarcode.getNextId(s.getContainerType());
							map2prefix.put(key2, prefix);
						}
					} else {
						prefix = b.getSampleId();
					}
					String containerId = prefix + "-" + s.getBlocNo();
					container = new Container(s.getContainerType(), containerId);
					key2Containers.put(key, container);
				}
				LoggerFactory.getLogger(BiosampleCreationHelper.class).debug("Assign " + container + " to "+b + "("+s+" : bl."+s.getBlocNo()+")");
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

		Phase phaseOfSample;
		if(phase==null) {
			phaseOfSample = parent.getInheritedPhase();
		} else {
			Phase endPhase = isNecropsy? parent.getTopParentInSameStudy().getExpectedEndPhase(): null;
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


	public static List<Biosample> validate(JDialog opener, List<Biosample> biosamples) throws Exception {
		return validate(opener, biosamples, null, false, false);
	}

	/**
	 *
	 * @param opener
	 * @param biosamples -not null
	 * @param editor -nullable
	 * @param allowDialogs
	 * @return
	 * @throws Exception
	 */
	public static List<Biosample> validate(JDialog opener, List<Biosample> biosamples, EditBiosampleTable editor, boolean allowCloning, boolean allowDialogs) throws Exception {

		if(allowDialogs) {

			//
			//Check generation of sampleIds
			List<Biosample> toGenerateSampleId = new ArrayList<>();
			for (Biosample b : biosamples) {
				if(b==null) continue;
				if(b.getSampleId()==null || b.getSampleId().length()==0) {
					toGenerateSampleId.add(b);
				}
			}
			if(toGenerateSampleId.size()>0) {
				//Confirm generation of sampleIds
				int res = JOptionPane.showConfirmDialog(opener, toGenerateSampleId.size() + " samples don't have a sampleId.\nDo you want SPIRIT to generate them?", "Generate SampleIds", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(res!=JOptionPane.YES_OPTION) throw new ValidationException("The sampleId is required", toGenerateSampleId.get(0), "SampleId");

				for (Biosample b : toGenerateSampleId) {
					if(editor!=null) {
						editor.generateSampleId(b);
					} else {
						b.setSampleId(DAOBarcode.getNextId(b));
					}
				}
				opener.repaint();
			}


			//Check that all groups/phase exists
			Set<Group> groupsToBeCreated = new TreeSet<>();
			Set<Phase> phasesToBeCreated = new TreeSet<>();
			for (Biosample b : biosamples) {
				if(b==null) continue;
				if(b.getInheritedGroup()!=null && b.getInheritedGroup().getId()<=0) {
					groupsToBeCreated.add(b.getInheritedGroup());
				}
				if(b.getInheritedPhase()!=null && b.getInheritedPhase().getId()<=0) {
					phasesToBeCreated.add(b.getInheritedPhase());
				}
			}
			if(groupsToBeCreated.size()>0 || phasesToBeCreated.size()>0) {
				//Confirm new groups to be created
				String msg = "The following" + (groupsToBeCreated.size()>0? " groups: " + MiscUtils.flatten(groupsToBeCreated) + "\n": "")  + (phasesToBeCreated.size()>0? (groupsToBeCreated.size()>0? " and ":"") + " phases: " + MiscUtils.flatten(phasesToBeCreated) + "\n": "") + " will be added to the study";
				int res = JOptionPane.showConfirmDialog(opener, msg, "New Groups/Phases", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(res!=JOptionPane.YES_OPTION) throw new ValidationException("Please correct the groups", null, "SampleId");

			}
		}



		//Validation
		Map<String, Biosample> sampleId2sample = new HashMap<>();
		boolean askValidChoice = true;
		List<Biosample> toSave = new ArrayList<>();
		Map<String, Integer> sampleId2Ids = DAOBiosample.getIdFromSampleIds(Biosample.getSampleIds(biosamples));
		List<Biosample> toClones = new ArrayList<>();
		List<Biosample> toCloneConflicts = new ArrayList<>();
		for (Biosample b : biosamples) {

			if(b==null || b.isEmpty()) continue;
			if(editor!=null && editor.getModel().getReadOnlyRows().contains(b)) continue;

			//Check that we have edit rights
			boolean canEdit = SpiritRights.canEdit(b, SpiritFrame.getUser());
			if(!canEdit) throw new ValidationException("You cannot allowed to update " + b, b, "SampleId");

			//SampleId required if prefix is null, as it cannot be generated automatically
			if((b.getBiotype().getPrefix()==null || b.getBiotype().getPrefix().length()==0) && (b.getSampleId()==null || b.getSampleId().length()==0)) throw new ValidationException("The sampleId cannot be empty", b, "SampleId");

			//Biotype is required
			if(b.getBiotype()==null) throw new ValidationException("Biotype cannot be empty", b, "SampleId");

			if(b.getBiotype().getSampleNameLabel()!=null && b.getBiotype().isNameRequired() && (b.getSampleName()==null || b.getSampleName().length()==0)){
				throw new ValidationException("The field '" + b.getBiotype().getSampleNameLabel() + "' is required", b, "Name");
			}

			for (BiotypeMetadata metadataType : b.getBiotype().getMetadata()) {
				String val = b.getMetadataValue(metadataType);
				if(metadataType.isRequired() && (val==null || val.length()==0)) {
					throw new ValidationException("The field '" +  metadataType.getName()+"' is required", b, metadataType.getName());
				}
				if(askValidChoice && metadataType.getDataType()==DataType.LIST) {
					if(val!=null && val.length()>0 && !metadataType.extractChoices().contains(val)) {
						if(!allowDialogs) {
							throw new ValidationException(val + " is not a valid "+metadataType.getName(), b, metadataType.getName());
						} else {
							int res = JOptionPane.showConfirmDialog(opener, val + " is not a valid "+metadataType.getName()+".\nWould you like to proceed anyways?", "Invalid Choice", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if(res!=JOptionPane.YES_OPTION) {
								throw new ValidationException(val + " is not a valid "+metadataType.getName(), b, metadataType.getName());
							}
							askValidChoice = false;
						}
					}
				}
			}

			//Check uniqueness, when a sampleId is given
			Integer existingId = sampleId2Ids.get(b.getSampleId());
			if(b.getSampleId()!=null && b.getSampleId().length()>0) {
				if(allowCloning) {
					if(sampleId2sample.get(b.getSampleId())!=null ) {
						//There is a conflict among samples in the list
						toClones.add(b);
						toCloneConflicts.add(sampleId2sample.get(b.getSampleId()));
					} else if(existingId!=null && existingId!=b.getId()) {
						//There is a conflict among samples in the db
						toClones.add(b);
						toCloneConflicts.add(DAOBiosample.getBiosampleById(existingId));
					}
				} else {
					assert !allowCloning;
					if(sampleId2sample.get(b.getSampleId())!=null) {
						throw new ValidationException("The sample with the id '"+b.getSampleId()+"' is duplicated", b, "SampleId");
					}
					if(existingId!=null && existingId!=b.getId()) {
						throw new ValidationException("The sampleId "+b.getSampleId()+" exists already in Spirit", b, "SampleId");
					}
				}
			}


			if(!sampleId2sample.containsKey(b.getSampleId())) {
				sampleId2sample.put(b.getSampleId(), b);
			}
			toSave.add(b);
		}


		//Relink parents (must be donebefore cloning)
		for (Biosample b : toSave) {
			if(b.getParent()!=null) {
				Biosample parent = b.getParent();
				if(parent.getId()<=0) {
					Biosample ref = sampleId2sample.get(parent.getSampleId());
					if(ref!=null) {
						b.setParent(ref);
					}
				}
			}
		}

		//Error message for duplicate samples or cloning?
		if(toClones.size()>0) {
			String msg = toClones.size()<6? "The participants "+ Biosample.getSampleIds(toClones): toClones.size() + " participants";
			if(allowDialogs) {
				int res = JOptionPane.showConfirmDialog(null, msg + " were used in a different study or group. Reusing them across study or groups requires (virtual) cloning.\nCloning means creating 2 identical vitual samples coming from the same physival sample.\n\nTo continue, you need to confirm the cloning of those samples?", "Reusing participants", JOptionPane.YES_NO_OPTION);
				if(res!=JOptionPane.YES_OPTION) throw new Exception("Please fix the duplicate sampleIds");

				//Clone the participants after confirmation from the user
				List<Biosample> updated = DAOStudy.cloneParticipants(toClones, toCloneConflicts);
				toSave.removeAll(updated);
				toSave.addAll(0, updated);
			} else {
				throw new ValidationException("The participants are duplicated", toClones.get(0), "SampleId");
			}
		}


		///////////////
		//AutoCorrection: Check the autocompletion fields for approximate spelling
		if(allowDialogs) {
			CorrectionMap<Biotype, Biosample> correctionMap1 = new CorrectionMap<>();
			for (Biosample b : toSave) {
				if(b.getBiotype().getSampleNameLabel()!=null && b.getBiotype().isNameAutocomplete()) {
					String value = b.getSampleName();
					if(value==null || value.length()==0) continue;

					Set<String> possibleValues = new TreeSet<>(DAOBiotype.getAutoCompletionFieldsForName(b.getBiotype(), null));
					if(possibleValues.contains(value)) continue;

					Correction<Biotype, Biosample> correction = correctionMap1.getCorrection(b.getBiotype(), value);
					if(correction==null) correction = correctionMap1.addCorrection(b.getBiotype(), value, new ArrayList<>(possibleValues), false);
					correction.getAffectedData().add(b);
				}
			}
			//BiotypeName: Display Correction Dlg
			if(correctionMap1.getItemsWithSuggestions()>0) {
				CorrectionDlg<Biotype, Biosample> dlg = new CorrectionDlg<Biotype, Biosample>(opener, correctionMap1) {
					@Override
					public String getSuperCategory(Biotype att) {
						return "";
					}
					@Override
					protected String getName(Biotype att) {
						return att.getName();
					}
					@Override
					protected void performCorrection(Correction<Biotype, Biosample> correction, String newValue) {
						for (Biosample b : correction.getAffectedData()) {
							b.setSampleName(newValue);
						}
					}
				};
				if(dlg.getReturnCode()!=CorrectionDlg.OK) return null;
			}
		}

		//BiotypeMetadata: Check the dictionary and autocompletion fields for approximate spelling
		if(allowDialogs) {
			CorrectionMap<BiotypeMetadata, Biosample> correctionMap2 = new CorrectionMap<>();
			for (Biosample b : toSave) {
				for (BiotypeMetadata att : b.getBiotype().getMetadata()) {
					if(att.getDataType()==DataType.AUTO) {
						String value = b.getMetadataValue(att);
						if(value==null || value.length()==0) continue;

						Set<String> possibleValues = DAOBiotype.getAutoCompletionFields(att, null);
						if(possibleValues.contains(value)) continue;

						Correction<BiotypeMetadata, Biosample> correction = correctionMap2.getCorrection(att, value);
						if(correction==null) correction = correctionMap2.addCorrection(att, value, new ArrayList<String>( possibleValues), false);
						correction.getAffectedData().add(b);
					}
				}
			}

			//BiotypeMetadata: Display Correction Dlg
			if(correctionMap2.getItemsWithSuggestions()>0) {
				CorrectionDlg<BiotypeMetadata, Biosample> dlg = new CorrectionDlg<BiotypeMetadata, Biosample>(opener, correctionMap2) {
					@Override
					public String getSuperCategory(BiotypeMetadata att) {
						return att.getBiotype().getName() + " - " + att.getName();
					}
					@Override
					protected String getName(BiotypeMetadata att) {
						return att.getName();
					}
					@Override
					protected void performCorrection(Correction<BiotypeMetadata, Biosample> correction, String newValue) {
						for (Biosample b : correction.getAffectedData()) {
							b.setMetadataValue(correction.getAttribute(), newValue);
						}
					}
				};
				if(dlg.getReturnCode()!=CorrectionDlg.OK) return null;
			}
		}
		return toSave;
	}

}
