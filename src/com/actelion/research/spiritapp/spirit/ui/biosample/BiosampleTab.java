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

package com.actelion.research.spiritapp.spirit.ui.biosample;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.ui.IBiosampleTab;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.SpiritTab;
import com.actelion.research.spiritapp.spirit.ui.icons.ImageFactory;
import com.actelion.research.spiritapp.spirit.ui.pivot.PivotPanel;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.pivot.InventoryPivotTemplate;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.iconbutton.IconType;

public class BiosampleTab extends SpiritTab implements IBiosampleTab {

	private PivotPanel pivotCardPanel;

	private BiosampleOrRackTab tableOrRackTab;

	private BiosampleSearchPane searchPane;
	private BiosampleTabbedPane biosampleDetailPanel;
	private JSplitPane westPane;
	private JSplitPane contentPane;
	private boolean first = true;

	public BiosampleTab(SpiritFrame frame) {
		this(frame, (Biotype[])null);
	}

	public BiosampleTab(SpiritFrame frame, String name) {
		this(frame, (Biotype[])null);
		setName(name);
	}

	/**
	 * Create a biosample search tab
	 * - if forcedBiotypes is null, studies can be selected
	 * - if forcedBiotypes is not null, only the given biotype can be searched and not in studies
	 * @param forcedBiotypes
	 */
	public BiosampleTab(SpiritFrame frame, Biotype[] forcedBiotypes) {
		super(frame,
				forcedBiotypes==null || forcedBiotypes.length==0? "Biosamples": forcedBiotypes[0].getName(),
						forcedBiotypes==null || forcedBiotypes.length==0? IconType.BIOSAMPLE.getIcon(): new ImageIcon(ImageFactory.getImageThumbnail(forcedBiotypes[0])));

		biosampleDetailPanel = new BiosampleTabbedPane();

		//TableTab
		tableOrRackTab = new BiosampleOrRackTab();
		tableOrRackTab.linkBiosamplePane(biosampleDetailPanel);
		tableOrRackTab.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				if(!biosampleDetailPanel.isVisible()) return;
				Collection<Biosample> sel = tableOrRackTab.getSelection(Biosample.class);
				if(sel.size()>0) {
					if(westPane.getDividerLocation()>westPane.getHeight()-20) westPane.setDividerLocation(500);
				} else {
					westPane.setDividerLocation(westPane.getHeight());
				}

			}
		});

		//PivotTab
		pivotCardPanel = new PivotPanel(true, tableOrRackTab, biosampleDetailPanel);
		pivotCardPanel.addPropertyChangeListener(PivotPanel.PROPERTY_PIVOT_CHANGED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				pivotBiosamples(pivotCardPanel.isPivotMode());
			}
		});
		BiosampleActions.attachPopup(pivotCardPanel.getPivotTable());

		//pivotTable Listeners
		ListSelectionListener listener = new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				if(!biosampleDetailPanel.isVisible()) return;

				Collection<Biosample> sel = pivotCardPanel.getPivotTable().getSelectedBiosamples();
				if(sel.size()==1) {
					if(westPane.getDividerLocation()>westPane.getHeight()-20) westPane.setDividerLocation(500);
					biosampleDetailPanel.setBiosamples(sel);
				} else {
					westPane.setDividerLocation(westPane.getHeight());
				}


			}
		};
		pivotCardPanel.getPivotTable().getSelectionModel().addListSelectionListener(listener);
		pivotCardPanel.getPivotTable().getColumnModel().getSelectionModel().addListSelectionListener(listener);


		//SearchPane
		searchPane = new BiosampleSearchPane(this, forcedBiotypes);

		JPanel eastPanel = new JPanel(new BorderLayout());
		eastPanel.add(BorderLayout.CENTER, pivotCardPanel);

		JPanel buttonsPanel = createButtonsPanel();
		if(buttonsPanel!=null) eastPanel.add(BorderLayout.SOUTH, buttonsPanel);

		westPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchPane, biosampleDetailPanel);
		contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, westPane, eastPanel);

		contentPane.setDividerLocation(300);
		contentPane.setOneTouchExpandable(true);
		westPane.setDividerLocation(1500);
		westPane.setOneTouchExpandable(true);


		searchPane.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				BiosampleTab.this.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
			}
		});

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, contentPane);

	}


	@SuppressWarnings("unchecked")
	@Override
	public<T> void fireModelChanged(SpiritChangeType action, Class<T> what, List<T> details) {
		if(!isShowing()) return;
		if(what==Biosample.class) {
			List<Biosample> biosamples = (List<Biosample>) details;
			Biosample.clearAuxInfos(biosamples);
			if(action==SpiritChangeType.MODEL_ADDED) {
				biosamples = JPAUtil.reattach(biosamples);
				tableOrRackTab.setBiosamples(biosamples);
				tableOrRackTab.setSelectedBiosamples(biosamples);
			} else if(action==SpiritChangeType.MODEL_UPDATED) {
				if(tableOrRackTab.getBiosamples().size()>200 || !tableOrRackTab.getBiosamples().containsAll(biosamples)) {
					//The table is too big or some edited or some samples are contained in the table, sets only the biosamples that were edited
					biosamples = JPAUtil.reattach(biosamples);
					tableOrRackTab.setBiosamples(biosamples);
				} else {
					//Refresh the table
					tableOrRackTab.setBiosamples(JPAUtil.reattach(tableOrRackTab.getBiosamples()));
				}
				tableOrRackTab.setSelectedBiosamples(biosamples);
			} else if(action==SpiritChangeType.MODEL_DELETED) {
				List<Biosample> rows = tableOrRackTab.getBiosamples();
				rows.removeAll(biosamples);
				tableOrRackTab.setBiosamples(JPAUtil.reattach(rows));
			}
			pivotBiosamples(false);
		}
		repaint();

	}


	protected BiosampleOrRackTab getBiosampleOrRackTab() {
		return tableOrRackTab;
	}

	@Override
	public List<Biosample> getBiosamples() {
		return tableOrRackTab.getBiosamples();
	}

	/**
	 * To be overriden by classes to get a custom button panel
	 * @return
	 */
	protected JPanel createButtonsPanel() {
		return null;
	}


	/**
	 * Can be overriden by classes to get a custom sort (or filtering)
	 * If the biosample belong to one type:
	 * - purified: sort by credate
	 * - Library: sort by name
	 * Otherwise
	 * - Use the normal sort: study, group, topId
	 * @return
	 */
	protected void sortBiosamples(List<Biosample> biosamples) {
		Biotype biotype = Biosample.getBiotype(biosamples);
		if(biotype!=null && biotype.getCategory()==BiotypeCategory.LIBRARY) {
			Collections.sort(biosamples, Biosample.COMPARATOR_NAME);
		} else if(biotype!=null && biotype.getCategory()==BiotypeCategory.PURIFIED) {
			Collections.sort(biosamples, Biosample.COMPARATOR_CREDATE);
		} else {
			Collections.sort(biosamples);
		}
	}

	public void query(BiosampleQuery q) {
		pivotBiosamples(false);
		getFrame().setStudyId(q.getStudyIds());
		searchPane.setQuery(q);
	}

	@Override
	public void setBiosamples(List<Biosample> biosamples) {
		first = false;
		tableOrRackTab.setBiosamples(biosamples);
		pivotBiosamples(pivotCardPanel.isPivotMode());
	}

	@Override
	public void setSelectedBiosamples(List<Biosample> biosamples) {
		tableOrRackTab.setSelectedBiosamples(biosamples);
	}

	public BiosampleOrRackTab getTableOrRackTab() {
		return tableOrRackTab;
	}

	@Override
	public void setRack(Location rack) {
		first = false;
		tableOrRackTab.setRack(rack);
	}


	public void pivotBiosamples(boolean enablePivot) {
		pivotCardPanel.clear();
		if(enablePivot) {

			new SwingWorkerExtended("Pivoting", pivotCardPanel) {
				final List<Result> toPivot = new ArrayList<>();
				@Override
				protected void doInBackground() throws Exception {
					List<Biosample> biosamples = tableOrRackTab.getBiosampleTable().getRows();

					Test test = new Test("Count");
					TestAttribute ta = new TestAttribute(test, "Number");
					test.getAttributes().add(ta);

					for (Biosample b : biosamples) {
						Result r = new Result();
						r.setBiosample(b);
						r.setPhase(b.getInheritedPhase());
						r.setTest(test);
						r.setValue(ta, "1");
						toPivot.add(r);
					}

				}
				@Override
				protected void done() {
					pivotCardPanel.setResults(toPivot, new InventoryPivotTemplate());

				}
			};
		}
		pivotCardPanel.setPivotMode(enablePivot);
	}

	public void queryMySamples() {
		searchPane.queryMySamples();
	}

	@Override
	public void onTabSelect() {
		if(getRootPane()!=null) {
			getRootPane().setDefaultButton(searchPane.getSearchButton());
		}
		//Load my samples in stockcare (forcedbiotype is not null)
		if(first && searchPane.getForcedBiotypes()!=null) {
			new SwingWorkerExtended("Loading my Samples", BiosampleTab.this, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
				@Override
				protected void done() {
					if(first) {
						first = false;
						searchPane.queryMySamples();
					}
				}
			};
		}
	}

	@Override
	public void onStudySelect() {
		if(getFrame()!=null && getFrame().getStudyId().length()>0) {
			query(searchPane.getSearchTree().getQuery());
		} else {
			tableOrRackTab.setBiosamples(new ArrayList<>());
		}
	}

}

