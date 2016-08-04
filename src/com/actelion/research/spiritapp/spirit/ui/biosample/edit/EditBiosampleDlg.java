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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.help.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils.ExportMode;
import com.actelion.research.spiritapp.spirit.ui.util.component.JHeaderLabel;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritapp.spirit.ui.util.correction.Correction;
import com.actelion.research.spiritapp.spirit.ui.util.correction.CorrectionDlg;
import com.actelion.research.spiritapp.spirit.ui.util.correction.CorrectionMap;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.ValidationException;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.Metadata;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBarcode;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class EditBiosampleDlg extends JSpiritEscapeDialog {

	private final EditBiosamplePanel editPanel = new EditBiosamplePanel(this);
	private final List<Biosample> resSavedBiosamples = new ArrayList<>();
	private final JHeaderLabel infoLabel = new JHeaderLabel("");

	private JButton excelButton = new JIconButton(JIconButton.IconType.EXCEL, "Export to Excel");
	private JButton okButton = new JIconButton(JIconButton.IconType.SAVE, "Save");

	/**
	 * Creates an edit dialog in a new session
	 * @param biosamples
	 * @return
	 * @throws Exception
	 */
	public static EditBiosampleDlg createDialogForEditInTransactionMode(String title, List<Biosample> biosamples) throws Exception {
		return new EditBiosampleDlg(title, biosamples, true);			
	}
	
	/**
	 * Create an edit dialog without creating a new session.
	 * the biosamples are still saved at the end
	 * 
	 * @param biosamples
	 * @return
	 * @throws Exception
	 */
	public static EditBiosampleDlg createDialogForEditSameTransaction(String title, List<Biosample> biosamples) throws Exception {
		return new EditBiosampleDlg(title, biosamples, false);
	}
		
	/**
	 * 
	 * @param biosamples
	 */
	private EditBiosampleDlg(String title, List<Biosample> biosamplesInput, boolean transactionMode) throws Exception {
		super(UIUtils.getMainFrame(), title==null? "Biosample - Batch Edit": title, transactionMode? EditBiosampleDlg.class.getName(): null);
		
		//Make sure the user is logged
		try {
			Spirit.askForAuthentication();
		} catch (Exception e) {
			JExceptionDialog.showError(e);
			return;
		}
		
		//Reload Objects		
		List<Biosample> biosamples = transactionMode? JPAUtil.reattach(biosamplesInput): biosamplesInput;		
		
		excelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					EditBiosampleTable table = editPanel.getTable();
					POIUtils.exportToExcel(table.getTabDelimitedTable(), ExportMode.HEADERS_TOP);
				} catch (Exception e) {
					JExceptionDialog.showError(EditBiosampleDlg.this, e);
				}					
			}
		});

		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				new SwingWorkerExtended("Saving", editPanel, false) {
					private List<Biosample> toSave;
					private String selCol;	
					private boolean isAddOp = false;
					
					@Override
					protected void doInBackground() throws Exception {
						try {
							SpiritUser user = Spirit.askForAuthentication();

//							long s = System.currentTimeMillis();
							toSave = validate(EditBiosampleDlg.this, editPanel.getTable().getBiosamples(), editPanel.getTable(), true);

							if(toSave==null) return;		
								
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
						SpiritChangeListener.fireModelChanged(isAddOp? SpiritChangeType.MODEL_ADDED : SpiritChangeType.MODEL_UPDATED, Biosample.class, toSave);
						dispose();

					}
				};

				
			}
		});
		
		//
		//Init Dialog
		infoLabel.setVisible(false);
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.NORTH, infoLabel);
		contentPanel.add(BorderLayout.CENTER, editPanel);
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), excelButton, Box.createHorizontalGlue(), /*validateButton,*/ okButton));
		setContentPane(contentPanel);
		
		UIUtils.adaptSize(this, 1400, 800);
		setLocationRelativeTo(UIUtils.getMainFrame());
		
		//Init Data
		editPanel.setRows(biosamples);

	}
	
	
	public void setTopParentReadOnly(boolean makeTopParentReadOnly) {

		//If there is no type specified, make the highest available parent read only (ie the animal used in the sampling) 
		if(makeTopParentReadOnly && editPanel.getBiotype()==null) {
			Set<Biosample> all = new HashSet<Biosample>(editPanel.getTable().getBiosamples());
			for (Biosample b : editPanel.getTable().getBiosamples()) {
				if(!all.contains(b.getParent())) {					
					editPanel.getTable().getModel().setReadOnlyRow(b, true);
				}
			}
		}
		
//		//Start autosave when sampling
//		autosaveDecorator.startAutosave();
		
		//Refresh the table
		try {
			editPanel.setRows(editPanel.getTable().getBiosamples());
		} catch(Exception e) {
			//Should not happen
			e.printStackTrace();
		}
		
		
		
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
	public static List<Biosample> validate(JDialog opener, List<Biosample> biosamples, EditBiosampleTable editor, boolean allowDialogs) throws Exception {
		
		long s = System.currentTimeMillis();
		System.out.println("EditBiosampleDlg.validateAndSave()1" +" > "+(System.currentTimeMillis()-s)+"ms");
		//Check sampleId to be generated
		if(allowDialogs) {
			List<Biosample> toGenerateSampleId = new ArrayList<Biosample>();
			for (Biosample b : biosamples) {		
				if(b==null) continue;
				
				if(b.getSampleId()==null || b.getSampleId().length()==0) {
					toGenerateSampleId.add(b);
				}
			}
			if(toGenerateSampleId.size()>0) {				
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
		}
		
		System.out.println("EditBiosampleDlg.validateAndSave()2" +" > "+(System.currentTimeMillis()-s)+"ms");
		//Validation
		Map<String, Biosample> sampleId2sample = new HashMap<>();
		boolean askValidChoice = true;
		List<Biosample> validated = new ArrayList<>();
		
		Map<String, Integer> sampleId2Ids = DAOBiosample.getIdFromSampleIds(Biosample.getSampleIds(biosamples));
		
		for (Biosample b : biosamples) {
			
			if(b==null || b.isEmpty()) continue;
			try {
				if(editor!=null && editor.getModel().getReadOnlyRows().contains(b)) continue;
	
				//Check that we have edit rights
				boolean canEdit = SpiritRights.canEdit(b, Spirit.getUser());
				if(!canEdit) throw new ValidationException("You cannot allowed to update " + b, b, "SampleId");
				
				//Validate
				if(b.getSampleId()==null || b.getSampleId().length()==0) throw new ValidationException("The sampleId cannot be empty", b, "SampleId");			
				if(b.getBiotype()==null) throw new ValidationException("Biotype cannot be empty", b, "SampleId");			
				
				if(b.getBiotype().getSampleNameLabel()!=null && b.getBiotype().isNameRequired() && (b.getSampleName()==null || b.getSampleName().length()==0)){
					throw new ValidationException(b.getBiotype().getName() + "." + b.getBiotype().getSampleNameLabel() + " is required", b, "Name");
				}
	
				for (BiotypeMetadata metadataType : b.getBiotype().getMetadata()) {
					Metadata m = b.getMetadata(metadataType);
					if(metadataType.isRequired() && (m.getValue()==null || m.getValue().length()==0)) {
						throw new ValidationException(b.getBiotype().getName() + "." + metadataType.getName()+" is required", b, metadataType.getName());
					}
					if(askValidChoice && metadataType.getDataType()==DataType.LIST && m.getValue()!=null) {
						String v = m.getValue();
						if(v!=null && v.length()>0 && !metadataType.extractChoices().contains(v)) {
							int res = JOptionPane.showConfirmDialog(opener, v + " is not a valid "+metadataType.getName()+".\nWould you like to proceed anyways?", "Invalid Choice", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if(res!=JOptionPane.YES_OPTION) {
								throw new ValidationException(v + " is not a valid "+metadataType.getName(), b, metadataType.getName());
							}
							askValidChoice = false;
						}
					}
				}
				
				
				//Check unicity
				if(sampleId2sample.get(b.getSampleId())!=null) throw new ValidationException("The sampleId "+b.getSampleId()+" is duplicated", b, "SampleId");
				
	
				Integer existingId = sampleId2Ids.get(b.getSampleId());
				if(existingId!=null && existingId!=b.getId()) throw new ValidationException("The sampleId "+b.getSampleId()+" exists already in Spirit", b, "SampleId");
	 			
				validated.add(b);
				sampleId2sample.put(b.getSampleId(), b);
			} catch(ValidationException e) {
				if(allowDialogs) throw e;
				//else ignore
			}
		}
		System.out.println("EditBiosampleDlg.validateAndSave()3" +" > "+(System.currentTimeMillis()-s)+"ms");


		//Secondary checks and updates:
		List<Biosample> parentsToSave = new ArrayList<Biosample>();
		Set<Biosample> samplesWithoutParents = new TreeSet<Biosample>();
		List<Biosample> toSave = new ArrayList<Biosample>();

		for (Biosample b : validated) {
			
			//Check that the parent exists or are referenced in an other tab
			if(b.getParent()!=null) {
				Biosample parent = b.getParent();
				if(parent.getId()<=0) {
					Biosample ref = sampleId2sample.get(parent.getSampleId());
					if(ref==null) {
						if(!parentsToSave.contains(parent)) parentsToSave.add(parent);
					} else {
						b.setParent(ref);
					}
				}
			}				
			
			//Check that it is not a composite type without a parent
			if((b.getBiotype().getCategory()==BiotypeCategory.SOLID || b.getBiotype().getCategory()==BiotypeCategory.LIQUID) && b.getParent()==null) {
				samplesWithoutParents.add(b);
			}
			
			toSave.add(b);
		}
		System.out.println("EditBiosampleDlg.validateAndSave()4" +" > "+(System.currentTimeMillis()-s)+"ms");

		//BiotypeName: Check the autocompletion fields for approximate spelling
		if(allowDialogs) {
			CorrectionMap<Biotype, Biosample> correctionMap1 = new CorrectionMap<Biotype, Biosample>();
			for (Biosample b : toSave) {
				if(b.getBiotype().getSampleNameLabel()!=null && b.getBiotype().isNameAutocomplete()) {
					String value = b.getSampleName();
					if(value==null || value.length()==0) continue;
					
					Set<String> possibleValues = new TreeSet<String>(DAOBiotype.getAutoCompletionFieldsForName(b.getBiotype(), null));					
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
		System.out.println("EditBiosampleDlg.validateAndSave()5a" +" > "+(System.currentTimeMillis()-s)+"ms");

		//BiotypeMetadata: Check the dictionary and autocompletion fields for approximate spelling
		if(allowDialogs) {
			CorrectionMap<BiotypeMetadata, Biosample> correctionMap2 = new CorrectionMap<BiotypeMetadata, Biosample>();
			for (Biosample b : toSave) {
				for (BiotypeMetadata att : b.getBiotype().getMetadata()) {
					if(att.getDataType()==DataType.AUTO) {
						Metadata rv = b.getMetadata(att);
						if(rv==null || rv.getValue()==null || rv.getValue().length()==0) continue;
						String value = rv.getValue();
						
						Set<String> possibleValues = DAOBiotype.getAutoCompletionFields(att, null);					
						if(possibleValues.contains(value)) continue;
						
						Correction<BiotypeMetadata, Biosample> correction = correctionMap2.getCorrection(att, value);
						if(correction==null) correction = correctionMap2.addCorrection(att, value, new ArrayList<String>( possibleValues), false);					
						correction.getAffectedData().add(b);
						
//					} else if(att.getDataType()==DataType.DICO) {
//						Metadata rv = b.getMetadata(att);
//						if(rv==null || rv.getValue()==null || rv.getValue().length()==0) continue;
//						String value = rv.getValue();
//						
//						Set<NomenclatureTuple> set = NomenclatureClientImp.instanceOfDefault().searchForMatchingNomenclature(value, 10);
//						Set<String> possibleValues = new HashSet<String>();
//						boolean contains = false;
//						for (NomenclatureTuple tuple : set) {
//							if(value.equals(tuple.controlledTerm)) {contains = true; break;} 
//							possibleValues.add(tuple.controlledTerm);
//						}					
//						if(contains) continue;
//											
//						Correction<BiotypeMetadata, Biosample> correction = correctionMap2.getCorrection(att, value);
//						if(correction==null) correction = correctionMap2.addCorrection(att, value, new ArrayList<String>(possibleValues), true);					
//						correction.getAffectedData().add(b);
//						
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
							b.getMetadata(correction.getAttribute()).setValue(newValue);							
						}						
					}
				};
				if(dlg.getReturnCode()!=CorrectionDlg.OK) return null;
			}
		}
		System.out.println("EditBiosampleDlg.validateAndSave()5b" +" > "+(System.currentTimeMillis()-s)+"ms");
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
	
	public void setLabel(String html) {
		infoLabel.setVisible(html!=null);
		infoLabel.setText(html);
	}
	
	
	public void setModelForNewRecords(Biosample b) {
		editPanel.getTable().getModel().setModelForNewRecords(b);
	}
	
}
