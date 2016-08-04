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

package com.actelion.research.spiritapp.spirit.ui.study;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.RevisionList;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.UIUtils;

public class StudyHistoryDlg extends JEscapeDialog {


	public StudyHistoryDlg(List<Revision> revisions) {
		super(UIUtils.getMainFrame(), "Study - History");
		
		final RevisionList revisionList = new RevisionList(revisions);
		final StudyDetailPanel detailPanel = new StudyDetailPanel(JSplitPane.VERTICAL_SPLIT, true);

		revisionList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				Revision s = revisionList.getSelectedValue();
				detailPanel.setStudy(s.getStudies().size()>0? s.getStudies().get(0): null);								
			}
		});
		revisionList.addMouseListener(new PopupAdapter() {			
			@Override
			protected void showPopup(MouseEvent e) {				
				Revision s = revisionList.getSelectedValue();
				if(s!=null && s.getStudies().size()==1) {
					JPopupMenu menu = new JPopupMenu();
					menu.add(new RestoreAction(s.getStudies().size()>0? s.getStudies().get(0): null));
					menu.show(revisionList, e.getX(), e.getY());
				}
			}
		});
		
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,  
				UIUtils.createTitleBox("Revisions", new JScrollPane(revisionList)), 
				UIUtils.createTitleBox("Study Revision", detailPanel));
		splitPane.setDividerLocation(400);		
		setContentPane(splitPane);
		
		
		UIUtils.adaptSize(this, 1124, 800);
		setVisible(true);
		
	}
	
	private class RestoreAction extends AbstractAction {
		private Study study;
		public RestoreAction(Study study) {
			super("Restore version");
			this.study = study;
			setEnabled(SpiritRights.canAdmin(study, Spirit.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {			
				JPAUtil.pushEditableContext(Spirit.getUser());
				int res = JOptionPane.showConfirmDialog(StudyHistoryDlg.this, "Are you sure you want to restore to the selected version?", "Restore", JOptionPane.YES_NO_OPTION);
				if(res!=JOptionPane.YES_OPTION) return;
				DAOStudy.persistStudy(study, Spirit.askForAuthentication());
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Study.class, study);
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			} finally {
				JPAUtil.popEditableContext();
			}
		}
	}
	
}
