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

package com.actelion.research.spiritapp.ui.study.randomize;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.swing.JOptionPane;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.biosample.ActionTreatment;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.CompareUtils;

public class AttachBiosamplesHelper {

	/**
	 * Reassign animals to the study at the given phase.
	 * - if phase is null, it is an assignment without considering phases, so the samples attached are exactly like the given input
	 * - if phase is not null, it is an assignment only for the considered phase, so animals from the groups without a randomization are not not touched
	 *
	 * If some of the groups require dividing into samples, this function will create/update the samples automatically
	 *
	 * @param study
	 * @param rndSamples
	 * @param phase - if null, unassign all animals not in the list. If not null, unassign only animals coming from a rando at that phase
	 * @param saveWeights
	 * @param user
	 * @throws Exception
	 */
	public static void attachSamples(Study study, List<AttachedBiosample> rndSamples, Phase phase, boolean saveWeights, SpiritUser user) throws Exception {

		//////////////////////////////////////////////////////////
		//Validate the input to remove empty samples and duplicates
		List<AttachedBiosample> list = new ArrayList<>();
		Set<String> sampleIds = new HashSet<>();
		for(AttachedBiosample rndSample: rndSamples) {
			if(rndSample.getSampleId()==null || rndSample.getSampleId().length()==0) {
				//skip empty sample (or throw an exception if some fields are prefilled)
				if(rndSample.getSampleName()!=null && rndSample.getSampleName().length()>0) throw new Exception("The sample named "+rndSample.getSampleName()+" has no sampleId");
				if(rndSample.getContainerId()!=null && rndSample.getContainerId().length()>0) throw new Exception("One sample in cage "+rndSample.getContainerId()+" has no sampleId");
				continue;
			}
			if(sampleIds.contains(rndSample.getSampleId())) throw new Exception("The SampleId "+rndSample.getSampleId()+" is duplicated");
			sampleIds.add(rndSample.getSampleId());
			list.add(rndSample);
		}

		/////////////////////////////
		//Load the former animals from the groups needing randomization
		List<Biosample> formerAnimals = new ArrayList<>();
		for(Biosample b: study.getParticipantsSorted()) {
			if(phase!=null) {
				if(b.getInheritedGroup()==null) continue; //always keep the reserve when we assign from a a rando on a specified phase
				if(!phase.equals(b.getInheritedGroup().getFromPhase())) continue; //the animals is attached from a different rando phase
			}
			formerAnimals.add(b);
		}

		//Compute the acceptedGroups
		List<Group> acceptedGroups = null;
		if(phase!=null) {
			acceptedGroups = new ArrayList<>();
			for (Group group : study.getGroups()) {
				if(!phase.equals(group.getFromPhase())) continue;
				if(!acceptedGroups.contains(group)) acceptedGroups.add(group);
				if(!acceptedGroups.contains(group.getFromGroup())) acceptedGroups.add(group.getFromGroup());
			}
		}

		//////////////////////////////////////////////
		//Check that animals don't belong to an other study/group
		//Because a biosample can only belong to one study, we solve this issue by duplicating the animals (ex: 881234 becomes 881234A in one study and 881234B in the other)
		List<Biosample> wrongStudySamples = new ArrayList<>();
		for (AttachedBiosample rndSample : list) {
			Biosample b = rndSample.getBiosample();
			boolean replacementNeeded = false;
			Biosample replacement = null;
			if(b==null) throw new Exception("The biosample of "+rndSample.getSampleId()+ " is null. Error?");

			if(b.getInheritedStudy()!=null && b.getInheritedStudy().equals(study) && CompareUtils.compare(b.getInheritedGroup(), rndSample.getGroup())==0) {
				//No Change -> OK
			} else if(b.getInheritedStudy()!=null && (!b.getInheritedStudy().equals(study) || (acceptedGroups!=null && b.getInheritedGroup()!=null && !acceptedGroups.contains(b.getInheritedGroup())))) {
				//The biosample belongs already to a different study/group
				wrongStudySamples.add(b);
			} else {
				//The biosample looks ok but it may have been cloned.
				//Check if one the child is compatible
				for (Biosample c : b.getHierarchy(HierarchyMode.CHILDREN)) {
					if (c.getAttachedStudy() != null) {
						replacementNeeded = true;
						if (c.getBiotype().equals(b.getBiotype()) && c.getAttachedStudy().equals(study) && CompareUtils.compare(c.getInheritedGroup(), rndSample.getGroup()) == 0) {
							replacement = c;
						}
					}
				}
				if(replacementNeeded) {
					if (replacement == null) {
						wrongStudySamples.add(b);
					} else {
						wrongStudySamples.remove(replacement);
						rndSample.setBiosample(replacement);
						rndSample.setSampleId(replacement.getSampleId());
					}
				}
			}
		}

		//Check existing animals
		Map<String, Biosample> toRemoveId2biosample = new HashMap<>();
		for (Biosample b : formerAnimals) toRemoveId2biosample.put(b.getSampleId(), b);
		for (AttachedBiosample r : list) toRemoveId2biosample.remove(r.getSampleId());

		if(formerAnimals.size()>0) {
			int res = JOptionPane.showConfirmDialog(null, formerAnimals.size() + " samples were already attached to "+study.getStudyId()+".\nAre you sure you want to replace those with " + list.size() + " samples?"
					+ (toRemoveId2biosample.size()>0? "\n"
							+ toRemoveId2biosample.size()+" samples " + (toRemoveId2biosample.size()<6? "(" + toRemoveId2biosample.keySet() + ")":"") + " will be unassigned!":""),
					"Randomization", JOptionPane.YES_NO_OPTION, toRemoveId2biosample.size()>0? JOptionPane.WARNING_MESSAGE: JOptionPane.QUESTION_MESSAGE);

			if(res!=JOptionPane.YES_OPTION) throw new Exception("Group Assignment canceled");
		}

		//Display Message for Cloning
		if(wrongStudySamples.size()>0) {
			//Propose duplication
			String msg = "";
			if(wrongStudySamples.size()<6) {
				msg = "The animals "+wrongStudySamples;
			} else {
				msg = wrongStudySamples.size() + " animals";
			}

			int res = JOptionPane.showConfirmDialog(null, msg + " were used in a different study or group.\nHaving an animal in 2 different groups requires (virtual) cloning.\nDo you want to clone?", "Reusing animals", JOptionPane.YES_NO_OPTION);
			if(res!=JOptionPane.YES_OPTION) throw new Exception("Group assignment canceled");
			Map<Biosample, Biosample> old2New = cloneParticipants(wrongStudySamples, study, user);
			for (AttachedBiosample rndSample : list) {
				Biosample newBio = old2New.get(rndSample.getBiosample());
				if(newBio!=null) {
					rndSample.setSampleId(newBio.getSampleId());
					rndSample.setBiosample(newBio);
				}
			}
		}


		//Update samples
		List<Biosample> toSave = new ArrayList<>();
		for (AttachedBiosample rndSample : list) {

			//SampleId cannot be null (should have been checked already)
			if(rndSample.getSampleId()==null || rndSample.getSampleId().length()==0) throw new Exception("You need to give a sampleId to each animal. Animal "+rndSample.getNo()+" has none.");

			//If the samplename is null but the no is not, use the no
			if(rndSample.getSampleName()==null && rndSample.getNo()>0) rndSample.setSampleName(""+rndSample.getNo());

			//Check corresponding biosample (should have been set already)
			Biosample b = rndSample.getBiosample();
			if(b==null) throw new Exception("Programming error? Contact an admin");

			if(b.getBiotype()==null) {
				Biotype animalBiotype = DAOBiotype.getBiotype(Biotype.ANIMAL);
				b.setBiotype(animalBiotype);
			}
			b.setSampleName(rndSample.getSampleName());

			if(rndSample.getWeight()!=null) {
				b.setLastAction(new ActionTreatment(null, phase, rndSample.getWeight(), null, null, null, null));
			}

			if(rndSample.getBiosample().getBiotype().getCategory()==BiotypeCategory.LIVING) {
				if(rndSample.getContainerId()!=null && rndSample.getContainerId().trim().length()>0) {
					if(b.getContainer()==null || b.getContainerType()!=ContainerType.CAGE || !rndSample.getContainerId().equals( b.getContainerId())) {
						b.setContainer(new Container(ContainerType.CAGE, rndSample.getContainerId()));
					}
				} else {
					b.setContainer(null);
				}
			}

			//Don't use setAttach(study, group, subgroup) to avoid checking, this should always be possible from there
			b.setAttached(study, rndSample.getGroup(), rndSample.getSubGroup());

			toSave.add(b);
		}

		//Remove samples not associated to the study anymore
		for (Biosample b : toRemoveId2biosample.values()) {

			if(b.getInheritedGroup()!=null || b.getInheritedSubGroup()!=0) {
				b.setInheritedGroup(null);
				b.setInheritedSubGroup(0);
			}
			if(b.getContainer()!=null) {
				b.setContainer(null);
			}

			b.setAttachedStudy(null);
			toSave.add(b);
		}


		//		List<Biosample> dividingBiosamplesToRemove = new ArrayList<>();
		//		toSave.addAll(BiosampleCreationHelper.processDividingSamples(study, dividingBiosamplesToRemove));


		//Open the transaction
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();

			//Save without checking the rights: rights have been checked already, and we may clone a rat from a previous study, where the user has no rights
			DAOBiosample.persistBiosamples(session, toSave, user);

			//Propose deletion?
			//			if(dividingBiosamplesToRemove.size()>0) {
			//				int res = JOptionPane.showConfirmDialog(UIUtils.getMainFrame(), "There are "+dividingBiosamplesToRemove.size()+" extra samples derived from those animals that need to be deleted. Do you confirm", "Extra samples", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
			//				if(res==JOptionPane.YES_OPTION) {
			//					DAOBiosample.deleteBiosamples(session, dividingBiosamplesToRemove, SpiritFrame.getUser());
			//				}
			//			}

			//Persist the weights into the result table
			if(saveWeights) {
				//Load existing results
				DAOResult.attachOrCreateStudyResultsToTops(study, toSave, phase, DAOResult.suggestElb(SpiritFrame.getUsername()));
				List<Result> weighings = new ArrayList<Result>();

				//Update results if the samples have a weight
				Test test = DAOTest.getTest(DAOTest.WEIGHING_TESTNAME);
				if(test==null) throw new Exception("The weights cannot be saved if the test "+DAOTest.WEIGHING_TESTNAME+" does not exist");
				for (AttachedBiosample s : list) {
					Result r = s.getBiosample().getAuxResult(test, phase);
					if(r==null) continue;
					if(s.getWeight()==null && r.getFirstAsDouble()==null) continue;
					if(s.getWeight()!=null && r.getFirstAsDouble()!=null && Math.abs(s.getWeight()-r.getFirstAsDouble())<.001) continue;

					r.getOutputResultValues().get(0).setValue(s.getWeight()==null?"": "" + s.getWeight());
					r.setUpdDate(new java.util.Date());
					r.setUpdUser(SpiritFrame.getUser().getUsername());
					weighings.add(r);
				}

				DAOResult.persistResults(session, weighings, SpiritFrame.getUser());
			}

			txn.commit();
			txn = null;
		} catch (Exception e) {
			if (txn != null)try {txn.rollback();} catch (Exception e2) {}
			throw e;
		}

	}


	/**
	 * To allow animals to belong to 2 studies, we perform the following trick:
	 *            881234
	 *            S-00001 (inherited only)
	 *          /         \
	 *  881234A             881234B
	 *  S-00001 (attached)  S-00002 (attached)
	 *  GrA (old)           GrB (new)
	 *
	 *  - We keep a topAnimal with no attached study
	 *  - From this topAnimal, we derive all the other animals belonging to studies
	 *
	 *
	 *  If an animal is reused more than twice, we continue the naming 'C', 'D', ...
	 *
	 *  Doing so, the results are still attached to the same topAnimal, and to the group matching the given study (what we want)
	 *
	 *
	 * This function is supposed to be used exceptionally
	 *
	 * @param samples
	 * @param study1
	 * @param study2
	 * @param user
	 * @return a map of animal (given as input) to a new animal (the duplicated)
	 */
	public static Map<Biosample, Biosample> cloneParticipants(List<Biosample> samples, Study newStudy, SpiritUser user) throws Exception {
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		Map<Biosample, Biosample> old2new = new HashMap<>();
		try {
			if(!SpiritRights.canEdit(newStudy, user)) throw new Exception("You are not allowed to edit "+newStudy);
			for (Biosample animal : samples) {
				if(animal.getId()<=0) throw new Exception("The animal "+animal+" is not persistant");
				if(animal.getParent()!=null) throw new Exception("The animal "+animal+" has already a parent");
			}

			txn = session.getTransaction();
			txn.begin();
			Date now = JPAUtil.getCurrentDateFromDatabase();

			for (Biosample animal : samples) {
				String sampleId = animal.getSampleId();
				if(animal.getAttachedStudy()!=null) {
					//
					//Creates a Top animal belonging to study1 but not attached(assigned) to it
					Biosample topAnimal = animal.clone();
					topAnimal.setId(0);
					topAnimal.setSampleId(sampleId+"_"); //new name to allow persistence
					topAnimal.setSampleName("");
					topAnimal.setAttachedStudy(null);
					topAnimal.setInheritedStudy(null);
					topAnimal.setInheritedGroup(null);
					topAnimal.setUpdDate(now);
					topAnimal.setUpdUser(user.getUsername());
					topAnimal.setContainer(null);
					topAnimal.setTopParent(topAnimal);
					session.persist(topAnimal);

					//The animal1 belongs to (as before)
					animal.setSampleId(sampleId+"A");
					animal.setParent(topAnimal);
					animal.setUpdDate(now);
					animal.setUpdUser(user.getUsername());
					animal.setTopParent(topAnimal);
					session.merge(topAnimal);

					//update the topparent from all animal1's children
					for (Biosample b : animal.getHierarchy(HierarchyMode.ALL)) {
						b.setTopParent(topAnimal);
					}

					//The animal2 belong to study2 / NoGroup
					Biosample animal2 = animal.clone();
					animal2.setId(0);
					animal2.setSampleId(sampleId+"B");
					animal2.setAttachedStudy(newStudy);
					animal2.setInheritedStudy(newStudy);
					animal2.setInheritedGroup(null); //to be given later by the programmer
					animal2.setParent(topAnimal);
					animal2.setUpdDate(now);
					animal2.setUpdUser(user.getUsername());
					animal2.setContainer(null);
					animal2.setTopParent(topAnimal);
					session.persist(animal2);

					//reupdate sampleid
					topAnimal.setSampleId(sampleId);
					old2new.put(animal, animal2);

				} else {
					//The TopAnimal was already cloned, check the next available letter
					Biosample topAnimal = animal.getTopParent();
					char availableLetter = 'A';
					for (Biosample b : topAnimal.getChildren()) {
						if(b.getSampleId().startsWith(sampleId) && b.getSampleId().length()==sampleId.length()+1) {
							availableLetter = (char) Math.max(availableLetter, b.getSampleId().charAt(b.getSampleId().length()-1)+1);
						}
					}



					Biosample animal2 = animal.clone();
					animal2.setId(0);
					animal2.setSampleId(sampleId+availableLetter);
					animal2.setAttachedStudy(newStudy);
					animal2.setInheritedStudy(newStudy);
					animal2.setInheritedGroup(null); //to be given later by the programmer
					animal2.setParent(topAnimal);
					animal2.setUpdDate(now);
					animal2.setUpdUser(user.getUsername());
					animal2.setContainer(null);
					animal2.setTopParent(topAnimal);
					session.persist(animal2);


					old2new.put(animal, animal2);
				}


			}




			txn.commit();
			txn = null;
		} finally {
			if(txn!=null && txn.isActive()) try{ txn.rollback();} catch(Exception e2) {}
		}
		return old2new;

	}



}
