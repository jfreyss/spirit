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

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.biosample.BiosampleTableModel.Mode;
import com.actelion.research.spiritapp.ui.biosample.dialog.BiosampleHistoryPanel;
import com.actelion.research.spiritapp.ui.result.ResultActions;
import com.actelion.research.spiritapp.ui.result.ResultTable;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.iconbutton.IconType;

/**
 * Component used to represent the info linked to biosamples/containers
 * The samples are always refreshed from the DB by default and the loading is done in threads
 * @author freyssj
 *
 */
public class BiosampleTabbedPane extends JPanel implements IBiosampleDetail {

	public static final String BIOSAMPLE_TITLE = "Biosample";
	public static final String HISTORY_TITLE = "History";
	//	public static final String HIERARCHY_TITLE = "Hierarchy";
	public static final String RESULT_TITLE = "Results";

	private JComponent biosampleTab;
	//	private JComponent hierarchyTab;
	private JComponent historyTab;
	private JComponent resultTab;
	//	private JComponent orbitTab;

	private boolean forRevision = false;


	private final JTabbedPane tabbedPane = new JCustomTabbedPane();

	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);

	private Collection<Biosample> biosamples;
	private boolean useTabPane = true;

	private final BiosampleMetadataPanel metadataPanel;
	private final BiosampleHistoryPanel historyPanel;
	private final BiosampleTable hierarchyTable;
	private final ResultTable resultTable;

	public BiosampleTabbedPane() {
		this(false);
	}

	public BiosampleTabbedPane(boolean forRevision) {

		this.forRevision = forRevision;

		setPreferredSize(new Dimension(370, 370));
		setMinimumSize(new Dimension(0, 0));

		metadataPanel = new BiosampleMetadataPanel();
		historyPanel = new BiosampleHistoryPanel();
		resultTable = new ResultTable(true);

		hierarchyTable = new BiosampleTable();
		hierarchyTable.getModel().setMode(Mode.SHORT);

		setBiosamples(null);

		resultTable.addMouseListener(new PopupAdapter(resultTable) {
			@Override
			protected void showPopup(MouseEvent e) {
				ResultActions.createPopup(resultTable.getSelection()).show(resultTable, e.getX(), e.getY());
			}
		});



		//Refresh panel when a tab is changed
		tabbedPane.addChangeListener(e-> refreshInThread());

		if(forRevision) {
			BiosampleActions.attachRevisionPopup(hierarchyTable);
		} else {
			BiosampleActions.attachPopup(hierarchyTable);
			BiosampleActions.attachPopup(metadataPanel);
			BiosampleActions.attachPopup(historyPanel);
		}

		//Init layout
		setLayout(new GridBagLayout());
		buildUI();


	}
	public void setTabPlacement(int placement) {
		tabbedPane.setTabPlacement(placement);
	}

	private void buildUI() {

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.gridx = 0;

		biosampleTab = new JScrollPane(metadataPanel);

		historyTab = new JScrollPane(historyPanel);

		resultTab = new JScrollPane(resultTable);

		if(useTabPane) {
			tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
			tabbedPane.add("", biosampleTab);
			tabbedPane.setToolTipTextAt(tabbedPane.getTabCount()-1, "Content");
			tabbedPane.setIconAt(tabbedPane.getTabCount()-1, IconType.BIOSAMPLE.getIcon());


			tabbedPane.add("", historyTab);
			tabbedPane.setToolTipTextAt(tabbedPane.getTabCount()-1, "History");
			tabbedPane.setIconAt(tabbedPane.getTabCount()-1, IconType.HISTORY.getIcon());

			if(SpiritProperties.getInstance().isChecked(PropertyKey.TAB_RESULT)) {
				if(!forRevision) {
					tabbedPane.add("", resultTab);
					tabbedPane.setToolTipTextAt(tabbedPane.getTabCount()-1, "Results");
					tabbedPane.setIconAt(tabbedPane.getTabCount()-1, IconType.RESULT.getIcon());
				}
			}

			tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
			c.weighty = 1; c.gridy = 1; add(tabbedPane, c);

		} else {
			cardPanel.add(BIOSAMPLE_TITLE, metadataPanel);
			cardPanel.add(HISTORY_TITLE, historyPanel);
			cardPanel.add(RESULT_TITLE, resultTab);
			c.weighty = 1; c.gridy = 1; add(cardPanel, c);
		}

	}

	@Override
	public Collection<Biosample> getBiosamples() {
		return biosamples;
	}

	@Override
	public void setBiosamples(final Collection<Biosample> biosamples) {
		setBiosamples(biosamples, true);
	}

	public void setBiosamples(final Collection<Biosample> biosamples, boolean checkRights) {
		if(biosamples==null || biosamples.size()==0 || (checkRights && !SpiritRights.canReadBiosamples(biosamples, SpiritFrame.getUser()))) {
			this.biosamples = null;
			refreshInThread();
		} else {
			if(Biosample.isInDifferentContainers(biosamples)) {
				this.biosamples = null;
				refreshInThread();
			} else {
				this.biosamples = biosamples;
				refreshInThread();
			}
		}
	}

	/**
	 * Refresh samples and refresh tabs in a separate thread
	 */
	private void refreshInThread() {
		if(forRevision) {
			refreshTab();
		} else if(biosamples==null) {
			refreshTab();
		} else {
			new SwingWorkerExtended(this, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
				@Override
				protected void done() {
					biosamples = JPAUtil.reattach(biosamples);
					if(isCancelled()) return;
					refreshTab();
				}
			};
		}
	}

	private void refreshTab() {
		if(tabbedPane.getSelectedIndex()<0) return;

		//Warning: don't check the rights here, must be done before as Bioviewer does not need rights
		if(biosamples!=null && biosamples.size()>0) {
			tabbedPane.setEnabled(true);
		} else {
			tabbedPane.setEnabled(false);
		}

		//MetadataTab
		if(tabbedPane.getSelectedComponent()==biosampleTab) {
			metadataPanel.setBiosamples(biosamples);
		}

		//HistoryTab
		if(tabbedPane.getSelectedComponent()==historyTab) {
			historyPanel.setBiosamples(biosamples);
		}

		//ResultTab
		if(tabbedPane.getSelectedComponent()==resultTab) {
			//Load Results for the parents and children
			Set<Integer> ids = new TreeSet<>();
			List<Result> results = new ArrayList<>();
			if(biosamples!=null) {
				for(Biosample b: biosamples) {
					Set<Biosample> children = b.getHierarchy(HierarchyMode.CHILDREN);
					children.add(b);
					for(Biosample b2: children) {
						ids.add(b2.getId());
					}
				}
			}
			if(ids.size()>0) {
				try {
					results = DAOResult.queryResults(ResultQuery.createQueryForBiosampleIds(ids), SpiritFrame.getUser());
				} catch(Exception e) {
					e.printStackTrace();
				}
			}

			Collections.sort(results, Result.COMPARATOR_UPDDATE);
			resultTable.setRows(results);
		}


		//		//Load Orbit Data
		//		if(tabbedPane.getSelectedComponent()==orbitTab && orbitPanel!=null) {
		//			orbitPanel.setBiosamples(biosamples);
		//		}

	}

	public void setSelectedTab(String tab) {
		if(useTabPane) {
			for (int i = 0; i < tabbedPane.getTabCount(); i++) {
				if(tabbedPane.getComponentAt(i)==biosampleTab && BIOSAMPLE_TITLE.equals(tab)) {
					tabbedPane.setSelectedIndex(i);
				} else if(tabbedPane.getComponentAt(i)==historyTab && HISTORY_TITLE.equals(tab)) {
					tabbedPane.setSelectedIndex(i);
					//				} else if(tabbedPane.getComponentAt(i)==orbitTab && ORBIT_TITLE.equals(tab)) {
					//					tabbedPane.setSelectedIndex(i);
				}
			}
		} else {
			cardLayout.show(cardPanel, tab);
		}
		refreshTab();
	}

	public void setVisibleTabs(boolean v) {
		this.useTabPane = v;
		buildUI();
	}

}
