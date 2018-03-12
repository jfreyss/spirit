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

package com.actelion.research.spiritapp.ui;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.ui.biosample.BiosampleActions.Action_Order;
import com.actelion.research.spiritapp.ui.biosample.BiosampleActions.Action_SelectWithDiscriminator;
import com.actelion.research.spiritapp.ui.biosample.BiosampleActions.Action_SetExpiryDate;
import com.actelion.research.spiritapp.ui.location.LocationActions;
import com.actelion.research.spiritapp.ui.result.ResultActions;
import com.actelion.research.spiritapp.ui.study.StudyActions;
import com.actelion.research.spiritapp.ui.study.StudyActions.Action_Report;
import com.actelion.research.spiritapp.ui.util.SpiritAction;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationFlag;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.iconbutton.IconType;

/**
 * Helper Class used to create the popup actions for each type of entity.
 * Can be overriden by each implementation of Spirit
 *
 * @author Joel Freyss
 */
public class PopupHelper {

	public JPopupMenu createStudyPopup(Study s) {
		//Reload the study to make sure the object is accurate
		final Study study = s==null? null: DAOStudy.getStudy(s.getId());

		JPopupMenu popupMenu = new JPopupMenu();
		if(study!=null) {
			popupMenu.add(new JCustomLabel("    Study: " + study.getStudyId(), Font.BOLD));
			popupMenu.add(new JSeparator());

			if(SpiritFrame.getUser()==null) {
				popupMenu.add(new SpiritAction.Action_Relogin(null, null));
				return popupMenu;
			}

			//New
			JMenu newMenu = new JMenu("New");
			newMenu.setIcon(IconType.NEW.getIcon());
			newMenu.setMnemonic('n');
			popupMenu.add(newMenu);
			{
				newMenu.add(new StudyActions.Action_New());
				newMenu.add(new StudyActions.Action_Duplicate(study));
			}

			//Edit
			JMenu editMenu = new JMenu("Edit");
			editMenu.setIcon(IconType.EDIT.getIcon());
			editMenu.setMnemonic('e');
			popupMenu.add(editMenu);
			if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_FEATURE_STUDYDESIGN)) {
				editMenu.add(new JMenuItem(new StudyActions.Action_EditInfos(study)));
				editMenu.add(new JMenuItem(new StudyActions.Action_EditDesign(study)));
			} else {
				editMenu.add(new JMenuItem(new StudyActions.Action_EditInfos(study)));
			}

			if(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES).length>0) {
				editMenu.add(new JMenuItem(new StudyActions.Action_Promote(study)));
			}

			//Participants
			if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_FEATURE_ADVANCED)) {
				JMenu attachMenu = new JMenu("Participants");
				attachMenu.setIcon(IconType.LINK.getIcon());
				attachMenu.setMnemonic('a');
				popupMenu.add(attachMenu);
				if(SpiritRights.isBlindAll(study, Spirit.getUser())) {
					attachMenu.setEnabled(false);
				}
				{
					if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_FEATURE_ADVANCED)) {
						JMenu autoMenu = new JMenu("Automatic Assignment");
						autoMenu.setIcon(IconType.LINK.getIcon());
						autoMenu.setMnemonic('a');
						attachMenu.add(autoMenu);
						Set<Phase> randoPhases = study.getPhasesWithGroupAssignments();
						if(randoPhases.size()>0) {
							for (Phase phase : randoPhases) {
								assert phase.getStudy()!=null;
								autoMenu.add(new JMenuItem(new StudyActions.Action_GroupAssignment(phase)));
							}
						} else {
							autoMenu.add(new JMenuItem());
							autoMenu.setEnabled(false);
						}
						attachMenu.add(new JSeparator());
					}

					attachMenu.add(new JMenuItem(new BiosampleActions.Action_BatchEdit("Edit Participants", study, study.getParticipantsSorted().size()>0 && SpiritRights.canEditBiosamples(study, Spirit.getUser())) {
						@Override
						public List<Biosample> getBiosamples() {
							List<Biosample> res = new ArrayList<>(study.getParticipantsSorted());
							Collections.sort(res);
							return res;
						}
					}));
					attachMenu.add(new JMenuItem(new BiosampleActions.Action_BatchEdit("Add Participants", study, SpiritRights.canEditBiosamples(study, Spirit.getUser())) {
						@Override
						public List<Biosample> getBiosamples() {
							Study s = JPAUtil.reattach(study);
							//Creates a biosample (either a new animal or a sample similar to the existing participants)
							Biosample b = new Biosample();
							Biotype biotype = Biosample.getBiotype(s.getParticipants());
							b.setBiotype(biotype==null? DAOBiotype.getBiotype(Biotype.ANIMAL): biotype);
							b.setContainerType(Biosample.getContainerType(s.getParticipants()));
							if(b.getBiotype()!=null) {
								for (BiotypeMetadata bType : b.getBiotype().getMetadata()) {
									Set<String> vals = Biosample.getMetadata(bType, s.getParticipants());
									b.setMetadataValue(bType, vals.size()==1? vals.iterator().next(): null);
								}
							}

							//Creates the list of participants to edit
							List<Biosample> res = new ArrayList<>();
							if(s.getParticipants().size()==0) {
								//Create an empty template from the existing animals
								for (Group group : s.getGroups()) {
									for(int subgroup=0; subgroup<Math.max(1, group.getNSubgroups()); subgroup++) {
										int n = group.getSubgroupSize(subgroup);
										if(n==0 && group.getFromGroup()==null) n=1;
										for(int i=0; i<n; i++) {
											Biosample b2 = b.clone();
											b2.setAttached(s, group, subgroup);
											res.add(b2);
										}
									}
								}
							}
							if(res.isEmpty()) {
								b.setAttached(s, null, 0);
								res.add(b);
							}

							return res;
						}
					}));
				}
			}


			if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_FEATURE_LIVEMONITORING)) {
				popupMenu.add(new JSeparator());
				popupMenu.add(new StudyActions.Action_AnimalMonitoring(study));
				popupMenu.add(new StudyActions.Action_SetLivingStatus(study));
				popupMenu.add(new JSeparator());
				popupMenu.add(new StudyActions.Action_ManageSamples(study));
				popupMenu.add(new StudyActions.Action_MeasurementSamples(study));
			}

			popupMenu.add(new JSeparator());
			popupMenu.add(new Action_Report(study));
			popupMenu.add(new JSeparator());

			//Advanced
			popupMenu.add(new StudyActions.Action_History(study));
			JMenu systemMenu = new JMenu("Advanced");
			systemMenu.setIcon(IconType.ADMIN.getIcon());
			systemMenu.add(new StudyActions.Action_Delete(study));
			systemMenu.add(new JSeparator());
			systemMenu.add(new StudyActions.Action_AssignTo(study));
			popupMenu.add(systemMenu);


		} else {
			popupMenu.add(new JCustomLabel("   Study Menu", Font.BOLD));
			popupMenu.add(new JSeparator());
			popupMenu.add(new JMenuItem(new StudyActions.Action_New()));
		}
		return popupMenu;
	}


	/**
	 * Creates a popup of biosamples (to be overriden if needed)
	 * @param biosamples
	 * @param phase
	 * @return
	 */
	public JPopupMenu createBiosamplePopup(List<Biosample> biosamples) {

		JPopupMenu menu = new JPopupMenu();

		if(biosamples==null || biosamples.size()==0) {
			return menu;
		}
		if(SpiritFrame.getUser()==null) {
			menu.add(new SpiritAction.Action_Relogin(null, null));
			return menu;
		}

		Set<Biotype> types = Biosample.getBiotypes(biosamples);
		boolean hasLiving = false;
		boolean hasCompositeOrComponents= false;
		boolean hasUnknown = false;
		for (Biotype biotype : types) {
			if(Biotype.ANIMAL.equals(biotype.getName())) hasLiving = true;
			else if(!biotype.isAbstract() && biotype.getCategory()!=BiotypeCategory.LIVING) hasCompositeOrComponents = true;
			else hasUnknown = true;
		}


		String s = biosamples.size()==1? biosamples.get(0).getSampleIdName(): biosamples.size()+" selected";
		menu.add(new JCustomLabel("   Biosample: " + s, Font.BOLD));

		//New
		JMenu newMenu = new JMenu("New");
		newMenu.setIcon(IconType.NEW.getIcon());
		newMenu.setMnemonic('n');
		menu.add(newMenu);
		newMenu.add(new BiosampleActions.Action_NewBatch());
		newMenu.add(new BiosampleActions.Action_Duplicate(biosamples));
		newMenu.add(new JSeparator());
		newMenu.add(new BiosampleActions.Action_NewChildren(biosamples));
		if(SpiritProperties.getInstance().isChecked(PropertyKey.TAB_RESULT)) {
			newMenu.add(new BiosampleActions.Action_NewResults(biosamples));
		}

		//Edit
		JMenu editMenu = new JMenu("Edit");
		editMenu.setIcon(IconType.EDIT.getIcon());
		editMenu.setMnemonic('e');
		menu.add(editMenu);
		editMenu.add(new BiosampleActions.Action_BatchEdit(biosamples));
		editMenu.add(new JSeparator());
		editMenu.add(new BiosampleActions.Action_Amount(biosamples));

		//Status
		if(hasUnknown) {
			//SetStatus is disabled
			JMenu statusMenu = new JMenu("Trash / Set Status");
			statusMenu.setIcon(IconType.STATUS.getIcon());
			statusMenu.setEnabled(false);
			editMenu.add(statusMenu);
		} else if(hasLiving) {
			//SetStatus for living
			editMenu.add(new StudyActions.Action_SetLivingStatus(biosamples));
		} else if(hasCompositeOrComponents) {
			//SetStatus for samples
			JMenu statusMenu = new JMenu("Trash / Set Status");
			statusMenu.setIcon(IconType.STATUS.getIcon());
			statusMenu.add(new BiosampleActions.Action_SetStatus(biosamples, null));
			statusMenu.add(new BiosampleActions.Action_SetStatus(biosamples, Status.INLAB));
			statusMenu.add(new JSeparator());
			statusMenu.add(new BiosampleActions.Action_SetStatus(biosamples, Status.PLANNED));
			statusMenu.add(new BiosampleActions.Action_SetStatus(biosamples, Status.RECEIVED));
			statusMenu.add(new JSeparator());
			statusMenu.add(new BiosampleActions.Action_SetStatus(biosamples, Status.LOWVOL));
			statusMenu.add(new BiosampleActions.Action_SetStatus(biosamples, Status.USEDUP));
			statusMenu.add(new BiosampleActions.Action_SetStatus(biosamples, Status.TRASHED));
			editMenu.add(statusMenu);
		} else {
			JMenu statusMenu = new JMenu("Trash / Set Status");
			statusMenu.setIcon(IconType.STATUS.getIcon());
			statusMenu.setEnabled(false);
			editMenu.add(statusMenu);
		}


		JMenu qualityMenu = new JMenu("Set Quality");
		qualityMenu.setIcon(IconType.QUALITY.getIcon());
		for (Quality quality : Quality.values()) {
			qualityMenu.add(new BiosampleActions.Action_SetQuality(biosamples, quality));
		}
		editMenu.add(qualityMenu);
		JMenuItem expiryMenu = new JMenuItem(new Action_SetExpiryDate(biosamples));
		expiryMenu.setEnabled(hasCompositeOrComponents);
		editMenu.add(expiryMenu);

		//Checkin/Checkout
		menu.add(new JSeparator());
		menu.add(new JMenuItem(new BiosampleActions.Action_Checkin(biosamples)));
		menu.add(new JMenuItem(new BiosampleActions.Action_Checkout(biosamples)));


		//Print
		menu.add(new JSeparator());
		menu.add(new BiosampleActions.Action_Print(biosamples));
		menu.add(new JSeparator());

		//Order from storage??
		if(DBAdapter.getInstance().getAutomaticStores()!=null && DBAdapter.getInstance().getAutomaticStores().size()>0) {
			menu.add(new Action_Order(biosamples));
		}

		//Advanced
		menu.add(new BiosampleActions.Action_History(biosamples));
		JMenu systemMenu = new JMenu("Advanced");
		systemMenu.setIcon(IconType.ADMIN.getIcon());
		systemMenu.add(new BiosampleActions.Action_Delete(biosamples));
		systemMenu.add(new JSeparator());
		systemMenu.add(new Action_SelectWithDiscriminator(biosamples));
		systemMenu.add(new JSeparator());
		systemMenu.add(new BiosampleActions.Action_AssignTo(biosamples));
		menu.add(systemMenu);

		return menu;
	}



	public JPopupMenu createLocationPopup(List<Location> locations) {

		JPopupMenu menu = new JPopupMenu();

		String s = locations.size()==1? locations.get(0).getName(): locations.size()+" selected";
		menu.add(new JCustomLabel("   Location: "+s, Font.BOLD));

		JMenu newMenu = new JMenu("New");
		newMenu.setMnemonic('n');
		newMenu.setIcon(IconType.NEW.getIcon());
		menu.add(newMenu);
		newMenu.add(new JMenuItem(new LocationActions.Action_New(locations)));
		newMenu.add(new JMenuItem(new LocationActions.Action_Duplicate(locations)));


		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('e');
		editMenu.setIcon(IconType.EDIT.getIcon());
		menu.add(editMenu);
		editMenu.add(new JMenuItem(new LocationActions.Action_EditBatch(locations)));


		//SetStatus for samples
		JMenu statusMenu = new JMenu("Set Flag");
		statusMenu.setIcon(IconType.STATUS.getIcon());
		statusMenu.add(new LocationActions.Action_SetStatus(locations, null));
		for(LocationFlag flag: LocationFlag.values()) {
			statusMenu.add(new LocationActions.Action_SetStatus(locations, flag));
		}
		editMenu.add(statusMenu);

		menu.add(new JSeparator());
		menu.add(new JMenuItem(new LocationActions.Action_Print(locations)));
		menu.add(new JSeparator());

		menu.add(new JMenuItem(new LocationActions.Action_History(locations)));
		JMenu advancedMenu = new JMenu("Advanced");
		advancedMenu.setMnemonic('n');
		advancedMenu.setIcon(IconType.ADMIN.getIcon());
		menu.add(advancedMenu);
		advancedMenu.add(new JMenuItem(new LocationActions.Action_Delete(locations)));
		advancedMenu.add(new JSeparator());

		return menu;
	}


	public JPopupMenu createResultPopup(List<Result> results) {
		JPopupMenu menu = new JPopupMenu();
		if(results!=null && results.size()>0) {
			menu.add(new JCustomLabel("   Results: " + (results.size()>1?" "+results.size()+" selected":""), Font.BOLD));
			menu.add(new JSeparator());

			String elb = null;
			for (Result result : results) {
				if(elb==null) {
					elb = result.getElb();
				} else if(!elb.equals(result.getElb())) {
					elb = null;
					break;
				}
			}

			JMenu newMenu = new JMenu("New");
			newMenu.setIcon(IconType.NEW.getIcon());
			newMenu.setMnemonic('n');
			menu.add(newMenu);
			newMenu.add(new ResultActions.Action_New());

			JMenu editMenu = new JMenu("Edit");
			editMenu.setIcon(IconType.EDIT.getIcon());
			editMenu.setMnemonic('e');
			menu.add(editMenu);
			editMenu.add(new ResultActions.Action_Edit_ELB(elb, results.get(0)));
			editMenu.add(new ResultActions.Action_Edit_Results(results));
			editMenu.add(new JSeparator());
			JMenu markMenu = new JMenu("Set Quality");
			markMenu.setIcon(IconType.QUALITY.getIcon());
			for (Quality quality : Quality.values()) {
				markMenu.add(new ResultActions.Action_SetQuality(results, quality));
			}
			editMenu.add(markMenu);

			menu.add(new JSeparator());

			menu.add(new ResultActions.Action_History(results.size()==1? results.get(0): null));
			JMenu systemMenu = new JMenu("Advanced");
			systemMenu.setIcon(IconType.ADMIN.getIcon());
			systemMenu.add(new ResultActions.Action_Delete_Results(results));
			systemMenu.add(new JSeparator());
			systemMenu.add(new ResultActions.Action_AssignTo(results));
			systemMenu.add(new JSeparator());
			menu.add(systemMenu);

		}
		return menu;
	}
}
