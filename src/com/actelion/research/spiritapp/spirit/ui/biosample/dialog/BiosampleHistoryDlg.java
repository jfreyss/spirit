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

package com.actelion.research.spiritapp.spirit.ui.biosample.dialog;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritapp.spirit.ui.util.RevisionList;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.ActionComments;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.spiritcore.util.Formatter;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.UIUtils;

public class BiosampleHistoryDlg extends JSpiritEscapeDialog {

		@Override
	protected boolean mustAskForExit() {
		return false;
	}
	
	public BiosampleHistoryDlg(final List<Revision> revisions) {
		super(UIUtils.getMainFrame(), "Biosample History", BiosampleHistoryDlg.class.getName());
		
		try {
			if(revisions.size()==0) throw new Exception("There are no revisions saved");
	
			final RevisionList revisionList = new RevisionList(revisions);
			final BiosampleTabbedPane detailPanel = new BiosampleTabbedPane(true);

			
			JScrollPane sp = new JScrollPane(revisionList);
			
			revisionList.addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					if(e.getValueIsAdjusting()) return;
					int i = revisionList.getSelectedIndex();
					if(i<0) {
						detailPanel.setBiosamples(null);
						return;
					}
					detailPanel.setBiosamples(revisions.get(i).getBiosamples());
				}
			});
			
			
			revisionList.addMouseListener(new PopupAdapter() {			
				@Override
				protected void showPopup(MouseEvent e) {				
					Revision r = (Revision) revisionList.getSelectedValue();
					if(r!=null) {
						JPopupMenu menu = new JPopupMenu();
						menu.add(new RestoreAction(r.getBiosamples()));
						menu.show(revisionList, e.getX(), e.getY());
					}
				}
			});

			
			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
					UIUtils.createTitleBox("Revisions", sp), 
					UIUtils.createTitleBox("Biosample Revision", new JScrollPane(detailPanel)));
			
			JPanel contentPanel = new JPanel(new BorderLayout());
			contentPanel.add(BorderLayout.CENTER, splitPane);
			setContentPane(contentPanel);
			
			
			UIUtils.adaptSize(this, 850, 600);
			setVisible(true);
		} catch (Exception e) {
			JExceptionDialog.showError(e);
			dispose();
		}		
	}
	
	private class RestoreAction extends AbstractAction {
		private List<Biosample> biosamples;
		
		public RestoreAction(List<Biosample> biosamples) {
			super("Restore version");
			this.biosamples = biosamples;
			for (Biosample biosample : biosamples) {
				if(!SpiritRights.canEdit(biosample, Spirit.getUser())) {
					setEnabled(false);
					break;
				}
			}
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {			
				int res = JOptionPane.showConfirmDialog(BiosampleHistoryDlg.this, "Are you sure you want to restore to the selected version?", "Restore", JOptionPane.YES_NO_OPTION);
				if(res!=JOptionPane.YES_OPTION) return;
				for (Biosample biosample : biosamples) {
					biosample.addAction(new ActionComments(biosample, "Restore from version "+Formatter.formatDateTime(biosample.getUpdDate())));
					biosample.setUpdDate(null); 						
				}
				DAOBiosample.persistBiosamples(biosamples, Spirit.getUser());
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, biosamples);
				dispose();
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}
	
	
}
