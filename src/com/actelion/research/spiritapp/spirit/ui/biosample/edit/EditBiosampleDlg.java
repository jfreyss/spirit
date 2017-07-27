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

package com.actelion.research.spiritapp.spirit.ui.biosample.edit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils.ExportMode;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritapp.spirit.ui.util.correction.Correction;
import com.actelion.research.spiritapp.spirit.ui.util.correction.CorrectionDlg;
import com.actelion.research.spiritapp.spirit.ui.util.correction.CorrectionMap;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.ValidationException;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBarcode;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class EditBiosampleDlg extends JSpiritEscapeDialog {

	private final EditBiosamplePanel editPanel = new EditBiosamplePanel(this);
	private final List<Biosample> resSavedBiosamples = new ArrayList<>();
	private Study candidatesForStudy = null;
	private JButton excelButton = new JIconButton(IconType.EXCEL, "Export to Excel");
	private JButton okButton = new JIconButton(IconType.SAVE, "Save");

	/**
	 * Creates an edit dialog in a new session
	 * @param biosamples
	 * @return
	 * @throws Exception
	 */
	public static EditBiosampleDlg createDialogForEditInTransactionMode(List<Biosample> biosamples) throws Exception {
		return new EditBiosampleDlg(biosamples, true);
	}

	/**
	 * Create an edit dialog without creating a new session.
	 * the biosamples are still saved at the end
	 *
	 * @param biosamples
	 * @return
	 * @throws Exception
	 */
	public static EditBiosampleDlg createDialogForEditSameTransaction(List<Biosample> biosamples) throws Exception {
		return new EditBiosampleDlg(biosamples, false);
	}

	/**
	 *
	 * @param biosamples
	 */
	private EditBiosampleDlg(List<Biosample> biosamplesInput, boolean transactionMode) throws Exception {
		super(UIUtils.getMainFrame(), "Biosample - Batch Edit", transactionMode? EditBiosampleDlg.class.getName(): null);

		//Make sure the user is logged
		try {
			Spirit.askForAuthentication();
		} catch (Exception e) {
			JExceptionDialog.showError(e);
			return;
		}

		//Reload Objects
		List<Biosample> biosamples = transactionMode? JPAUtil.reattach(biosamplesInput): biosamplesInput;

		excelButton.addActionListener(ev-> {
			try {
				EditBiosampleTable table = editPanel.getTable();
				POIUtils.exportToExcel(table.getTabDelimitedTable(), ExportMode.HEADERS_TOP);
			} catch (Exception e) {
				JExceptionDialog.showError(EditBiosampleDlg.this, e);
			}
		});

		okButton.addActionListener(ev-> {
			new SwingWorkerExtended("Saving", editPanel) {
				private List<Biosample> toSave;
				private String selCol;
				private boolean isAddOp = false;

				@Override
				protected void doInBackground() throws Exception {
					try {
						SpiritUser user = Spirit.askForAuthentication();

						toSave = validate(EditBiosampleDlg.this, editPanel.getTable().getBiosamples(), editPanel.getTable(), getCandidatesForStudy()!=null, true);

						if(toSave==null) return;
						if(toSave.size()==0) throw new Exception("There are no samples to save");

						for (Biosample b : toSave) {
							if(b.getId()<=0) { isAddOp = true; break;}
						}

						DAOBiosample.persistBiosamples(toSave, user);
						resSavedBiosamples.addAll(toSave);

					} catch (ValidationException e) {
						e.printStackTrace();
						selCol = e.getCol();
						Biosample selBiosample = (e.getRow() instanceof Biosample)? (Biosample) e.getRow(): null;
						editPanel.getTable().setSelection(selBiosample, selCol);
						throw e;
					}

				}
				@Override
				protected void done() {
					if(toSave==null) return;
					Set<Study> studies = Biosample.getStudies(biosamples);
					SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Study.class, studies);
					SpiritChangeListener.fireModelChanged(isAddOp? SpiritChangeType.MODEL_ADDED : SpiritChangeType.MODEL_UPDATED, Biosample.class, toSave);
					dispose();

				}
			};
		});

		//Init Dialog
		setContentPane(UIUtils.createBox(editPanel, null, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), excelButton, Box.createHorizontalGlue(), okButton) ));

		UIUtils.adaptSize(this, 1920, 1080);
		setLocationRelativeTo(UIUtils.getMainFrame());

		//Init Data
		editPanel.setRows(biosamples);

	}

	public void setCandidatesForStudy(Study candidatesForStudy) {
		this.candidatesForStudy = candidatesForStudy;
		editPanel.getTable().getModel().setStudy(candidatesForStudy);

	}
	public Study getCandidatesForStudy() {
		return candidatesForStudy;
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
						String nextId = DAOBarcode.getNextId(b.getBiotype());
						b.setSampleId(nextId);
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
				String msg = "The following\n " + (groupsToBeCreated.size()>0? " groups: " + groupsToBeCreated + "\n": "")  + (phasesToBeCreated.size()>0? " phases: " + phasesToBeCreated + "\n": "") + " are new and will be added to the study";
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

			//Validate
			if(b.getSampleId()==null || b.getSampleId().length()==0) throw new ValidationException("The sampleId cannot be empty", b, "SampleId");
			if(b.getBiotype()==null) throw new ValidationException("Biotype cannot be empty", b, "SampleId");

			if(b.getBiotype().getSampleNameLabel()!=null && b.getBiotype().isNameRequired() && (b.getSampleName()==null || b.getSampleName().length()==0)){
				throw new ValidationException("The field '" + b.getBiotype().getSampleNameLabel() + "' is required", b, "Name");
			}

			for (BiotypeMetadata metadataType : b.getBiotype().getMetadata()) {
				String val = b.getMetadataValue(metadataType);
				if(metadataType.isRequired() && (val==null || val.length()==0)) {
					throw new ValidationException("The field '" +  metadataType.getName()+"' is required", b, metadataType.getName());
				}
				if(askValidChoice && metadataType.getDataType()==DataType.LIST && val!=null) {
					if(val!=null && val.length()>0 && !metadataType.extractChoices().contains(val)) {
						int res = JOptionPane.showConfirmDialog(opener, val + " is not a valid "+metadataType.getName()+".\nWould you like to proceed anyways?", "Invalid Choice", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if(res!=JOptionPane.YES_OPTION) {
							throw new ValidationException(val + " is not a valid "+metadataType.getName(), b, metadataType.getName());
						}
						askValidChoice = false;
					}
				}
			}

			//Check uniqueness
			Integer existingId = sampleId2Ids.get(b.getSampleId());
			if(allowCloning) {
				if(sampleId2sample.get(b.getSampleId())!=null ) {
					toClones.add(b);
					toCloneConflicts.add(sampleId2sample.get(b.getSampleId()));
				} else if(existingId!=null && existingId!=b.getId()) {
					toClones.add(b);
					toCloneConflicts.add(DAOBiosample.getBiosamplesById(existingId));
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


	@Override
	protected boolean mustAskForExit() {
		if(super.mustAskForExit() || editPanel.getTable().getUndoManager().hasChanges()) return true;
		return false;
	}

	public List<Biosample> getSaved() {
		return resSavedBiosamples;
	}

	public void setForcedBiotype(Biotype biotype) {
		editPanel.setForcedBiotype(biotype);

	}

}
