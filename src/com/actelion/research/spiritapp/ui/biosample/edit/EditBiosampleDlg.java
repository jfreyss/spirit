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

package com.actelion.research.spiritapp.ui.biosample.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.util.HelpBinder;
import com.actelion.research.spiritapp.ui.util.POIUtils;
import com.actelion.research.spiritapp.ui.util.POIUtils.ExportMode;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.ValidationException;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.helper.BiosampleCreationHelper;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class EditBiosampleDlg extends JSpiritEscapeDialog {

	private final EditBiosamplePanel editPanel = new EditBiosamplePanel(this);
	private final List<Biosample> resSavedBiosamples = new ArrayList<>();
	private Study participatingStudy = null;
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

						toSave = BiosampleCreationHelper.validate(EditBiosampleDlg.this, editPanel.getTable().getBiosamples(), editPanel.getTable(), getParticipitatingStudy()!=null, true);

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

	public void setParticipatingStudy(Study participatingStudy) {
		this.participatingStudy = participatingStudy;
		editPanel.getTable().getModel().setStudy(participatingStudy);
	}

	public Study getParticipitatingStudy() {
		return participatingStudy;
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
