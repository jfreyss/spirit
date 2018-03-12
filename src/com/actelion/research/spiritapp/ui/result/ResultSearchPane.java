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

package com.actelion.research.spiritapp.ui.result;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
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

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.ui.util.formtree.FormTree;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.DBAdapter.UserManagedMode;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;

public class ResultSearchPane extends JPanel {

	public static final String PROPERTY_SEARCH = "search_update";

	private final ResultTab tab;
	private final ResultSearchTree tree;
	private final JButton searchButton = new JButton(new Action_Search());

	public ResultSearchPane(final ResultTab resultTab, final Biotype forcedBiotype) {
		super(new BorderLayout());
		this.tab = resultTab;
		this.tree = new ResultSearchTree(resultTab.getFrame(), forcedBiotype);

		JButton resetButton = new JButton(new Action_Reset());

		//Layout
		add(BorderLayout.CENTER, new JScrollPane(tree));
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), resetButton, searchButton));
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		setPreferredSize(new Dimension(200, 200));

		tree.addPropertyChangeListener(FormTree.PROPERTY_SUBMIT_PERFORMED, evt -> {
			new Action_Search().actionPerformed(null);
		});
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(0,0);
	}

	/**
	 * Sets the query by updating the filter, and retrieves the results
	 * @param query
	 * @return
	 */
	public SwingWorkerExtended setQuery(ResultQuery query) {
		tree.expandAll(false);
		tree.setQuery(query);
		return query(query);
	}

	/**
	 * Retrieves the results without updating the filters
	 * @param query
	 * @return
	 */
	public SwingWorkerExtended query(final ResultQuery query) {
		tab.setResults(new ArrayList<>());

		final SpiritUser user = SpiritFrame.getUser();
		if(user==null) return new SwingWorkerExtended() {};

		return new SwingWorkerExtended("Querying Results", tab, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			private long s;
			private List<Result> results;

			@Override
			protected void doInBackground() throws Exception {
				s = System.currentTimeMillis();
				LoggerFactory.getLogger(getClass()).debug("ResultQuery results: " + query);
				results = query.isEmpty()? new ArrayList<>(): DAOResult.queryResults(query, user);
				LoggerFactory.getLogger(getClass()).debug("ResultQuery queried in: "+(System.currentTimeMillis()-s)+"ms");
				DAOResult.fullLoad(results);
				LoggerFactory.getLogger(getClass()).debug("ResultQuery loaded in: "+(System.currentTimeMillis()-s)+"ms");
			}

			@Override
			protected void done() {
				if(query.getMaxResults()>0 && results.size()>=query.getMaxResults()) {
					tab.setErrorText("There are more than "+query.getMaxResults()+" results. Please use the filters");
				} else {
					tab.setResults(results);
				}
				LoggerFactory.getLogger(getClass()).debug("ResultQuery done in: "+(System.currentTimeMillis()-s)+"ms");
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
			final ResultQuery query = tree.getQuery();
			query.setMaxResults(100000);
			if(DBAdapter.getInstance().getUserManagedMode()!=UserManagedMode.UNIQUE_USER && query.isEmpty()) {
				JOptionPane.showMessageDialog(ResultSearchPane.this, "Please enter more criteria", "Search", JOptionPane.ERROR_MESSAGE);
			} else {
				query(query);
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
			ResultQuery q = new ResultQuery();
			q.setStudyIds("");
			tree.expandAll(false);
			tree.setQuery(q);
			tab.setResults(new ArrayList<Result>());
			SpiritContextListener.setStatus("");
			ResultSearchPane.this.firePropertyChange(PROPERTY_SEARCH, null, "");

		}
	}

	public ResultSearchTree getSearchTree() {
		return tree;
	}

	public JButton getSearchButton() {
		return searchButton;
	}

}
