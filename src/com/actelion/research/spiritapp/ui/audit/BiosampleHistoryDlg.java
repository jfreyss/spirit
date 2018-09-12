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

package com.actelion.research.spiritapp.ui.audit;

import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.biosample.BiosampleActions.Action_ExportBiosampleEvents;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

/**
 * Dialog used to show the previous versions of a biosample
 *
 * @author Joel Freyss
 */
public class BiosampleHistoryDlg extends JEscapeDialog {

	private RevisionPanel revisionPanel = new RevisionPanel();
	private JCheckBox byFieldCheckbox = new JCheckBox("Changes per field", true);

	public BiosampleHistoryDlg(final Biosample biosample) {
		super(UIUtils.getMainFrame(), "Biosample - Audit Trail - " + biosample.getSampleId());

		if(biosample.getInheritedStudy()!=null && SpiritRights.isBlind(biosample.getInheritedStudy(), Spirit.getUser())) {
			JExceptionDialog.showError("The history is not visible for blind users");
			return;
		}

		revisionPanel.setFilters("Sample", biosample.getId(), null);
		byFieldCheckbox.addActionListener(e->revisionPanel.setSingular(byFieldCheckbox.isSelected()));
		byFieldCheckbox.setVisible(SpiritProperties.getInstance().isAdvancedMode());

		Action_ExportBiosampleEvents exportBiosampleEventsAction = new Action_ExportBiosampleEvents();
		exportBiosampleEventsAction.setParentDlg(this);
		exportBiosampleEventsAction.setFilters("Sample", biosample.getId(), null);
		JIconButton exportBiosampleEventsButton = new JIconButton(IconType.PDF, "Export Sample Events...", exportBiosampleEventsAction);
		JPanel actionPanel = UIUtils.createHorizontalBox(Box.createHorizontalGlue(), exportBiosampleEventsButton);

		JPanel topPanel = UIUtils.createHorizontalBox(Box.createHorizontalGlue(), byFieldCheckbox);
		setContentPane(UIUtils.createBox(revisionPanel, topPanel, actionPanel));

		//Load revisions in background
		new SwingWorkerExtended(revisionPanel, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			private List<Revision> revisions;
			@Override
			protected void doInBackground() throws Exception {
				revisions = DAORevision.getLastRevisions(biosample);
			}

			@Override
			protected void done() {
				if(revisions.size()==0) {
					JExceptionDialog.showError("There are no revisions saved");
				}
				revisionPanel.setSingular(byFieldCheckbox.isSelected());
				revisionPanel.setRows(revisions);
				exportBiosampleEventsAction.setRevisions(revisions);
				exportBiosampleEventsAction.setBiosample(biosample);
			}
		};



		UIUtils.adaptSize(this, 1200, 800);
		setVisible(true);

	}

	//	private Map<Revision, String> getChangeMap(List<Revision> revisions ) {
	//		Map<Revision, String> changeMap = new HashMap<>();
	//		List<Revision> revs = new ArrayList<>();
	//		for (int i = 0; i < revisions.size(); i++) {
	//			Biosample b1 = revisions.get(i).getBiosamples().get(0);
	//			String diff;
	//			if(i+1<revisions.size()) {
	//				Biosample b2 = revisions.get(i+1).getBiosamples().get(0);
	//				diff = b1.getDifference(b2);
	//			} else {
	//				Biosample b2 = revisions.get(0).getBiosamples().get(0);
	//				diff = b1.getDifference(b2);
	//				if(diff.length()==0) diff = "First version";
	//			}
	//
	//			revs.add(revisions.get(i));
	//			changeMap.put(revisions.get(i), diff);
	//		}
	//		return changeMap;
	//
	//	}
	//
	//	private class RestoreAction extends AbstractAction {
	//		private List<Biosample> biosamples;
	//
	//		public RestoreAction(List<Biosample> biosamples) {
	//			super("Restore version");
	//			this.biosamples = biosamples;
	//			for (Biosample biosample : biosamples) {
	//				if(!SpiritRights.canEdit(biosample, SpiritFrame.getUser())) {
	//					setEnabled(false);
	//					break;
	//				}
	//			}
	//		}
	//
	//		@Override
	//		public void actionPerformed(ActionEvent e) {
	//			int res = JOptionPane.showConfirmDialog(BiosampleHistoryDlg.this, "Are you sure you want to restore to the selected version?", "Restore", JOptionPane.YES_NO_OPTION);
	//			if(res!=JOptionPane.YES_OPTION) return;
	//			try {
	//				JPAUtil.pushEditableContext(JPAUtil.getSpiritUser());
	//				for (Biosample biosample : biosamples) {
	//					biosample.setLastAction(new com.actelion.research.spiritcore.business.biosample.ActionComments("Restored from version "+FormatterUtils.formatDateTime(biosample.getUpdDate())));
	//					biosample.setUpdDate(null);
	//				}
	//				DAOBiosample.persistBiosamples(biosamples, SpiritFrame.getUser());
	//				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, biosamples);
	//				dispose();
	//			} catch (Exception ex) {
	//				JExceptionDialog.showError(ex);
	//			} finally {
	//				JPAUtil.popEditableContext();
	//			}
	//		}
	//	}


}
