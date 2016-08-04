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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.FormTree;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class StudySearchPane extends JPanel {

	private final StudySearchTree studySearchTree = new StudySearchTree();
	
	private final JButton searchButton = new JButton(new Action_Search());
	private final JButton resetButton = new JButton(new Action_Reset());
	
	private final StudyTable table;
	
	public StudySearchPane(StudyTable table) {
		super(new BorderLayout(0, 0));
		this.table = table;
		
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));	
		
		add(BorderLayout.NORTH, UIUtils.createHorizontalBox(new JButton(new Action_ViewAll()), new JButton(new Action_ViewMine()), Box.createHorizontalGlue()));
		add(BorderLayout.CENTER, new JScrollPane(studySearchTree));
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), resetButton, searchButton));
		
		
		studySearchTree.addPropertyChangeListener(FormTree.PROPERTY_SUBMIT_PERFORMED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {				
				new Action_Search().actionPerformed(null);
			}
		});
		
		setPreferredSize(new Dimension(220, 200));
		
		
	}

	/**
	 * Query Biosamples
	 * @author freyssj
	 *
	 */
	public class Action_Search extends AbstractAction {
		public Action_Search() {
			super("Search");
			putValue(AbstractAction.SMALL_ICON, IconType.SEARCH.getIcon());
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			search(studySearchTree.getQuery());
		}
	}
	
	public class Action_Reset extends AbstractAction {
		public Action_Reset() {
			super("");
			putValue(Action.SMALL_ICON, IconType.CLEAR.getIcon());
			setToolTipText("Reset all query fields");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			studySearchTree.setQuery(new StudyQuery());
			table.setRows(new ArrayList<Study>());
			table.getSelectionModel().clearSelection();
		}
	}
	
	public void search(final StudyQuery query) {
		

		new SwingWorkerExtended("Querying Studies", table, SwingWorkerExtended.FLAG_ASYNCHRONOUS50MS) {
			List<Study> studies;
			@Override
			protected void doInBackground() throws Exception {
				studies = DAOStudy.queryStudies(query, Spirit.getUser());
			}

			@Override
			protected void done() {
				studySearchTree.setQuery(query);
				table.setRows(studies);
				if(studies.size()==1) {
					table.getSelectionModel().addSelectionInterval(0, 0);
				}
				SpiritContextListener.setStatus(studies.size() + " Studies");
			}
			
		};
		
		
		
	}
	
	
	public class Action_ViewAll extends AbstractAction {
		public Action_ViewAll() {
			super("View All");
			setToolTipText("Query and display all studies (provided I have READ-access)");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			search(new StudyQuery());
		}
	}

	public class Action_ViewMine extends AbstractAction {
		public Action_ViewMine() {
			super("MyStudies");
			setToolTipText("Query and display all studies (provided I have WRITE-access)");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			StudyQuery q = new StudyQuery();
			q.setUser(Spirit.getUser()==null? null: Spirit.getUser().getUsername());
			search(q);
		}
	}

	public JButton getSearchButton() {
		return searchButton;
	}

	public StudySearchTree getSearchTree() {
		return studySearchTree;
	}	
	
	public void setStudyIds(String studyIds) {
		studySearchTree.setStudyIds(studyIds);
	}
}
