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

package com.actelion.research.spiritapp.ui.biosample;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.biosample.linker.MetadataColumn;
import com.actelion.research.spiritapp.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.ui.util.formtree.FormTree;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.DBAdapter.UserManagedMode;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;
import com.actelion.research.util.ui.iconbutton.IconType;

public class BiosampleSearchPane extends JPanel {

	private final Biotype[] forcedBiotypes;

	private final BiosampleTab tab;
	private final BiosampleSearchTree tree;
	private final JButton resetButton = new JButton(new Action_Reset());
	private final JButton searchButton = new JButton(new Action_Search());


	public BiosampleSearchPane(final BiosampleTab tab, Biotype[] forcedBiotypes) {
		super(new BorderLayout(0, 0));
		this.tab = tab;
		this.forcedBiotypes = forcedBiotypes;


		tree = new BiosampleSearchTree(tab.getFrame(), forcedBiotypes, false);
		setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));


		add(BorderLayout.CENTER, new JScrollPane(tree));
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(/*new JButton(new Action_ViewMine()),*/ Box.createHorizontalGlue(), resetButton, searchButton));

		setPreferredSize(new Dimension(200, 200));
		tree.addPropertyChangeListener(FormTree.PROPERTY_SUBMIT_PERFORMED, evt-> {
			new Action_Search().actionPerformed(null);
		});
	}

	public void setQuery(final BiosampleQuery query) {
		tree.expandAll(false);
		tree.setQuery(query);
		query(query);

	}

	public void query(final BiosampleQuery query) {
		tab.setBiosamples(null);
		if(forcedBiotypes!=null) {
			query.setStudyIds("NONE");
		}

		try {
			final SpiritUser user = JPAUtil.getSpiritUser();
			new SwingWorkerExtended("Querying Biosamples", tab) {
				private List<Biosample> biosamples;
				private long s = System.currentTimeMillis();

				@Override
				protected void doInBackground() throws Exception {
					if(DBAdapter.getInstance().getUserManagedMode()!=UserManagedMode.UNIQUE_USER && query.isEmpty()) throw new Exception("You must enter more search criteria");

					//Query samples
					biosamples = DAOBiosample.queryBiosamples(query, user);
					LoggerFactory.getLogger(getClass()).debug("Query done in: "+(System.currentTimeMillis()-s)+"ms");


					//Add a not found for scanned items
					if(query.getSampleIdOrContainerIds()!=null && query.getSampleIdOrContainerIds().length()>0) {
						StringTokenizer st = new StringTokenizer(query.getSampleIdOrContainerIds(), "\t\n, ");
						Set<String> items = new HashSet<>();
						while(st.hasMoreTokens()) items.add(st.nextToken());

						Set<String> seen = new HashSet<>();
						for (Biosample b : biosamples) {
							seen.add(b.getSampleId());
							if(b.getContainerId()!=null) seen.add(b.getContainerId());
						}
						for(String item: items) {
							if(!seen.contains(item)) {
								Biosample b = new Biosample();
								b.setSampleId("Not Found");
								b.setContainerType(ContainerType.UNKNOWN);
								b.setContainerId(item);
								biosamples.add(b);
								assert b.getContainerId()==item;
							}
						}
					}
					tab.getBiosampleOrRackTab().getBiosampleTable().getModel().setFilterTrashed(query.isFilterTrashed());
					LoggerFactory.getLogger(getClass()).debug("Filtered in: "+(System.currentTimeMillis()-s)+"ms");
					tab.sortBiosamples(biosamples);
					LoggerFactory.getLogger(getClass()).debug("Sorted in: "+(System.currentTimeMillis()-s)+"ms");

				}
				@Override
				protected void done() {


					//If the query was done on hidden columns, unhide those
					Set<BiosampleLinker> linkers = new HashSet<>(query.getLinker2values().keySet());
					ExtendTableModel<Biosample> model = tab.getBiosampleOrRackTab().getBiosampleTable().getModel();

					for(Column<Biosample, ?> col: model.getAllColumns()) {
						if(col.isHideable() && (col instanceof MetadataColumn) && linkers.contains(((MetadataColumn) col).getLinker()) && !model.getColumns().contains(col)) {
							model.showHideable(col, true);
						}
					}

					//Set the samples
					tab.setBiosamples(biosamples);

					LoggerFactory.getLogger(getClass()).debug("Display done in: "+(System.currentTimeMillis()-s)+"ms");


					SpiritContextListener.setStatus(biosamples.size() + " Biosamples");

				}
			};

		} catch (Exception e) {
			JExceptionDialog.showError(e);
		}

	}


	public void reset() {
		BiosampleQuery q = new BiosampleQuery();
		q.setStudyIds("");
		tree.setQuery(q);
		tree.expandAll(false);
		tab.getBiosampleOrRackTab().clear();
		SpiritContextListener.setStatus("");

	}


	public void queryMySamples() {
		if(SpiritFrame.getUser()==null) return;
		BiosampleQuery q;
		if(forcedBiotypes!=null && forcedBiotypes.length==1  && forcedBiotypes[0].getCategory()==BiotypeCategory.LIBRARY) {
			q = BiosampleQuery.createQueryForBiotype(forcedBiotypes[0]);
		} else {
			q = new BiosampleQuery();
			if(SpiritFrame.getUser().getMainGroup()==null) {
				q.setCreUser(SpiritFrame.getUser().getUsername());
			} else {
				q.setDepartment(SpiritFrame.getUser().getMainGroup());
			}
			q.setBiotypes(forcedBiotypes);
			if(forcedBiotypes==null || forcedBiotypes.length==0) q.setCreDays(186);
		}
		query(q);
	}

	//	public class Action_ViewMine extends AbstractAction {
	//		public Action_ViewMine() {
	//			super("MySamples");
	//			setToolTipText("Query and display the biosamples of my department" + (forcedBiotypes==null || forcedBiotypes.length==0? "(Last 6 momths)":""));
	//		}
	//		@Override
	//		public void actionPerformed(ActionEvent e) {
	//			queryMySamples();
	//		}
	//	}

	public class Action_Search extends AbstractAction {
		public Action_Search() {
			super("Search");
			putValue(AbstractAction.SMALL_ICON, IconType.SEARCH.getIcon());
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			BiosampleQuery query = tree.getQuery();
			query(query);

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
			reset();
		}
	}

	public BiosampleSearchTree getSearchTree() {
		return tree;
	}
	public JButton getSearchButton() {
		return searchButton;
	}

	public Biotype[] getForcedBiotypes() {
		return forcedBiotypes;
	}


}
