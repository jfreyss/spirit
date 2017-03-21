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

import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.spirit.ui.admin.AdminActions;
import com.actelion.research.spiritapp.spirit.ui.util.RevisionList;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

/**
 * Dialog to show the audit history of a study.
 * 
 * @author Joel Freyss
 *
 */
public class StudyHistoryDlg extends JEscapeDialog {

	public StudyHistoryDlg(Study study) {
		super(UIUtils.getMainFrame(), "Study - History");
		
		//dialog events
		final RevisionList revisionList = new RevisionList();
		final StudyDetailPanel detailPanel = new StudyDetailPanel(JSplitPane.VERTICAL_SPLIT);
		detailPanel.setForRevision(true);
		
		revisionList.addListSelectionListener(e-> {
			if(e.getValueIsAdjusting()) return;
			Revision s = revisionList.getSelectedValue();
			detailPanel.setStudy(s.getStudies().size()>0? s.getStudies().get(0): null);								
		});
		revisionList.addMouseListener(new PopupAdapter() {			
			@Override
			protected void showPopup(MouseEvent e) {				
				Revision s = revisionList.getSelectedValue();
				if(s!=null && s.getStudies().size()==1) {
					JPopupMenu menu = new JPopupMenu();
					menu.add(new AdminActions.Action_Restore(s.getStudies()));
					menu.show(revisionList, e.getX(), e.getY());
				}
			}
		});
		
		//Create layout
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,  
				UIUtils.createTitleBox("Revisions", new JScrollPane(revisionList)), 
				UIUtils.createTitleBox("Study Revision", detailPanel));
		splitPane.setDividerLocation(400);		
		setContentPane(splitPane);
		
		//Load revisions in background
		new SwingWorkerExtended(revisionList, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			private List<Revision> revs;
			protected void doInBackground() throws Exception {
				revs = DAORevision.getRevisions(study);
			}
			
			protected void done() {
				revisionList.setRevisions(revs);
				revisionList.setSelectedIndex(0);
			}
		};
		

		//show dialog
		UIUtils.adaptSize(this, 1124, 800);
		setVisible(true);
		
	}
	
}
