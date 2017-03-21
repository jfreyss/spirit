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

package com.actelion.research.spiritapp.spirit.ui.location;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.LocationDepictor;
import com.actelion.research.spiritapp.spirit.ui.util.RevisionList;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.UIUtils;

public class LocationHistoryDlg extends JEscapeDialog {

	
	public LocationHistoryDlg(final List<Revision> revisions) {
		super(UIUtils.getMainFrame(), "Location History", true);
		
		try {
			if(revisions.size()==0) throw new Exception("There are no revisions saved");
	
			final RevisionList revisionList = new RevisionList(revisions);
			final LocationTable locationTable = new LocationTable();
			final LocationDepictor locationDepictor = new LocationDepictor();
			
			locationDepictor.setForRevisions(true);
			locationTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				
				@Override
				public void valueChanged(ListSelectionEvent e) {
					if(e.getValueIsAdjusting()) return;
					locationDepictor.setBioLocation(locationTable.getSelection().size()==1? locationTable.getSelection().get(0): null);
					
				}
			});
			revisionList.addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					if(e.getValueIsAdjusting()) return;
					Revision rev = revisionList.getSelectedValue();
					if(rev!=null) {
						locationTable.setRows(rev.getLocations());
						locationDepictor.setBioLocation(rev.getLocations().size()==1? rev.getLocations().get(0): null);
					}
				}
			});
			
			
			revisionList.addMouseListener(new PopupAdapter() {			
				@Override
				protected void showPopup(MouseEvent e) {				
					Revision r = (Revision) revisionList.getSelectedValue();
					if(r!=null) {
						JPopupMenu menu = new JPopupMenu();
						menu.add(new RestoreAction(r.getResults()));
						menu.show(revisionList, e.getX(), e.getY());
					}
				}
			});

			JScrollPane sp = new JScrollPane(locationTable);
			sp.setPreferredSize(new Dimension(400, 180));
			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
					UIUtils.createTitleBox("Revisions", new JScrollPane(revisionList)),
					UIUtils.createTitleBox("Location Revision", UIUtils.createBox(locationDepictor, sp)));
			
			JPanel contentPanel = new JPanel(new BorderLayout());
			contentPanel.add(BorderLayout.CENTER, splitPane);
			setContentPane(contentPanel);
			
			
			UIUtils.adaptSize(this, 850, 600);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setLocationRelativeTo(UIUtils.getMainFrame());
			setVisible(true);
		} catch (Exception e) {
			JExceptionDialog.showError(e);
			dispose();
		}		
		
	}
	
	private class RestoreAction extends AbstractAction {
		private List<Result> results;
		
		public RestoreAction(List<Result> results) {
			super("Restore version");
			this.results = results;
			for (Result r : results) {
				if(!SpiritRights.canEdit(r, SpiritFrame.getUser())) {
					setEnabled(false);
					break;
				}
			}
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {			
				int res = JOptionPane.showConfirmDialog(LocationHistoryDlg.this, "Are you sure you want to restore to the selected version?", "Restore", JOptionPane.YES_NO_OPTION);
				if(res!=JOptionPane.YES_OPTION) return;
				DAOResult.persistResults(results, SpiritFrame.getUser());
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Result.class, results);
				dispose();
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}
}
