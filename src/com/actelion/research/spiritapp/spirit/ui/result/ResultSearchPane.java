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

package com.actelion.research.spiritapp.spirit.ui.result;

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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.pivot.PivotCardPanel;
import com.actelion.research.spiritapp.spirit.ui.study.ReportDlg;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.spirit.ui.util.formtree.FormTree;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.DBAdapter.UserAdministrationMode;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class ResultSearchPane extends JPanel {
	
	public static final String PROPERTY_SEARCH = "search_update";
	
	private final PivotCardPanel cardPanel;
	
	private final ResultSearchTree tree;		 
	private final JButton searchButton = new JButton(new Action_Search());
	
	public ResultSearchPane(final PivotCardPanel cardPanel, final Biotype forcedBiotype) {
		super(new BorderLayout(0, 0));
		this.cardPanel = cardPanel;
		this.tree = new ResultSearchTree(forcedBiotype);

		JButton reportButton = new JButton(new Action_Report());
		JButton resetButton = new JButton(new Action_Reset());	

		reportButton.setVisible(forcedBiotype==null);
		
		//Layout
		add(BorderLayout.CENTER, new JScrollPane(tree));
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(reportButton, Box.createHorizontalGlue(), resetButton, searchButton));		
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));		
		setPreferredSize(new Dimension(200, 200));
		
		tree.addPropertyChangeListener(FormTree.PROPERTY_SUBMIT_PERFORMED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				new Action_Search().actionPerformed(null);
			}
		});
	}
	
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(0,0);
	}
	
	public void query(ResultQuery query) {
		tree.expandAll(false);
		tree.setQuery(query);
		
		search(query);		
	}
	
	public void search(final ResultQuery query) {
		cardPanel.clear();

		final SpiritUser user = Spirit.getUser();
		if(user==null) return;
		
		new SwingWorkerExtended("Querying Results", cardPanel, true) {
			private final long s = System.currentTimeMillis();
			private List<Result> results;
			
			@Override
			protected void doInBackground() throws Exception {
				results = DAOResult.queryResults(query, user);
			}

			@Override
			protected void done() {
				LoggerFactory.getLogger(getClass()).debug("Query done in: "+(System.currentTimeMillis()-s)+"ms");
				cardPanel.setResults(results, query.getSkippedOutputAttribute(), null, false);
				LoggerFactory.getLogger(getClass()).debug("Display done in: "+(System.currentTimeMillis()-s)+"ms");
			}
			
		};
		
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
			final ResultQuery query = tree.updateQuery();
			if(DBAdapter.getAdapter().getUserManagedMode()!=UserAdministrationMode.UNIQUE_USER && query.isEmpty()) {
				JOptionPane.showMessageDialog(ResultSearchPane.this, "Please enter more criteria", "Search", JOptionPane.ERROR_MESSAGE);
			} else {
				search(query);
			}
			ResultSearchPane.this.firePropertyChange(PROPERTY_SEARCH, null, "");
		}
	}
	
	/**
	 * Reset Query
	 * @author freyssj
	 *
	 */
	public class Action_Reset extends AbstractAction {
		public Action_Reset() {
			super("");
			putValue(Action.SMALL_ICON, IconType.CLEAR.getIcon());
			setToolTipText("Reset all query fields");
		}
		@Override
		public void actionPerformed(ActionEvent e) {		
			ResultQuery query = new ResultQuery();
			tree.expandAll(false);
			tree.setQuery(query);
			cardPanel.setResults(new ArrayList<Result>(), null, null, false);
			SpiritContextListener.setStatus("");
			ResultSearchPane.this.firePropertyChange(PROPERTY_SEARCH, null, "");

		}
	}
	
	
	public class Action_Report extends AbstractAction {
		public Action_Report() {
			super("Report");
			putValue(Action.SMALL_ICON, IconType.EXCEL.getIcon());
			setToolTipText("Generate a predefined report");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			Study s = tree.getStudyId().length()==0? null: DAOStudy.getStudyByStudyId(tree.getStudyId());
			if(s==null) {
				JExceptionDialog.showError("You must select exactly one study");
				return;
			} else {
				new ReportDlg(s);
			}
			

		}
	}

	
//	public class Action_Export extends AbstractAction {
//		public Action_Export() {
//			super();
//			putValue(AbstractAction.SHORT_DESCRIPTION, "Save Query");
//			putValue(AbstractAction.SMALL_ICON, IconType.SAVE.getIcon());
//		}
//		@Override
//		public void actionPerformed(ActionEvent e) {		
//			ResultQuery query = tree.getQuery();
//			try {
//				ExportResultQueryModel model = new ExportResultQueryModel(query, cardPanel.getCurrentTemplate());
//				new ExportQueryDlg<ExportResultQueryModel>(ExportResultQueryModel.class, model, null);				
//			} catch (Exception ex) {
//				JExceptionDialog.show(ex);			
//			}
//			refreshCombobox();			
//		}
//	}
	
	
	public ResultSearchTree getSearchTree() {
		return tree;
	}

	public JButton getSearchButton() {
		return searchButton;
	}
	
}
