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
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritapp.spirit.ui.util.RevisionList;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.UIUtils;

/**
 * Dialog used to show the previous versions of a biosample
 * @author Joel Freyss
 *
 */
public class BiosampleHistoryDlg extends JEscapeDialog {

	private JCheckBox onlyNoticeableChange = new JCheckBox("Only Noticeable changes");
	private RevisionList revisionList = new RevisionList();
	private List<Revision> revisions; 
	
	public BiosampleHistoryDlg(final Biosample biosample) {
		super(UIUtils.getMainFrame(), "Biosample History");
		
		try {
			this.revisions = DAORevision.getRevisions(biosample);

			if(revisions.size()==0) throw new Exception("There are no revisions saved");
	
			onlyNoticeableChange.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					refreshList();
				}
			});
			refreshList();
			
			final BiosampleTabbedPane detailPanel = new BiosampleTabbedPane(true);

			revisionList.addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					if(e.getValueIsAdjusting()) return;
					Revision r = revisionList.getSelectedValue();
					if(r==null) {
						detailPanel.setBiosamples(null);
						return;
					}
					detailPanel.setBiosamples(r.getBiosamples());
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
					UIUtils.createTitleBox("Revisions", UIUtils.createBox(new JScrollPane(revisionList), null, onlyNoticeableChange)), 
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
	
	private void refreshList() {
		Map<Revision, String> changeMap = new HashMap<>();
		List<Revision> revs = new ArrayList<>();
		for (int i = 0; i < revisions.size(); i++) {
			Biosample b1 = revisions.get(i).getBiosamples().get(0);
			String diff;
			if(i+1<revisions.size()) {
				Biosample b2 = revisions.get(i+1).getBiosamples().get(0);
				diff = b1.getDifference(b2, SpiritFrame.getUsername());
			} else {
				Biosample b2 = revisions.get(0).getBiosamples().get(0);
				diff = b1.getDifference(b2, SpiritFrame.getUsername());
				if(diff.length()==0) diff = "First version";
			}
			if(onlyNoticeableChange.isSelected() && diff.length()==0) continue;
			revs.add(revisions.get(i));
			changeMap.put(revisions.get(i), diff);
		}
		
		revisionList.setRevisions(revs);
		revisionList.setChangeMap(changeMap);
	}
	
	private class RestoreAction extends AbstractAction {
		private List<Biosample> biosamples;
		
		public RestoreAction(List<Biosample> biosamples) {
			super("Restore version");
			this.biosamples = biosamples;
			for (Biosample biosample : biosamples) {
				if(!SpiritRights.canEdit(biosample, SpiritFrame.getUser())) {
					setEnabled(false);
					break;
				}
			}
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
				int res = JOptionPane.showConfirmDialog(BiosampleHistoryDlg.this, "Are you sure you want to restore to the selected version?", "Restore", JOptionPane.YES_NO_OPTION);
				if(res!=JOptionPane.YES_OPTION) return;
			try {			
				JPAUtil.pushEditableContext(JPAUtil.getSpiritUser());
				for (Biosample biosample : biosamples) {
					biosample.setLastAction(new com.actelion.research.spiritcore.business.biosample.ActionComments("Restored from version "+FormatterUtils.formatDateTime(biosample.getUpdDate())));
					biosample.setUpdDate(null); 						
				}
				DAOBiosample.persistBiosamples(biosamples, SpiritFrame.getUser());
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, biosamples);
				dispose();
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			} finally {
				JPAUtil.popEditableContext();
			}
		}
	}

	@Override
	protected boolean mustAskForExit() {
		return false;
	}
	

	
}
