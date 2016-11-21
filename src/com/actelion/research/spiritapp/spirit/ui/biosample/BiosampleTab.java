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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.ui.pivot.PivotCardPanel;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritTab;
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
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.SwingWorkerExtended;

public class BiosampleTab extends JPanel implements ISpiritTab {
	
	private PivotCardPanel pivotCardPanel;
	
	private BiosampleOrRackTab tableOrRackTab;	
	
	private BiosampleSearchPane searchPane;
	private BiosampleTabbedPane biosampleDetailPanel;
	private JSplitPane westPane;
	private JSplitPane contentPane;
	private boolean first = true;

	/**
	 * Create a biosample search tab
	 * - if forcedBiotypes is null, studies can be selected
	 * - if forcedBiotypes is not null, only the given biotype can be searched and not in studies
	 * @param forcedBiotypes
	 */
	public BiosampleTab(Biotype[] forcedBiotypes) {
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
		pivotCardPanel = new PivotCardPanel(tableOrRackTab, false, biosampleDetailPanel);
		pivotCardPanel.addPropertyChangeListener(PivotCardPanel.PROPERTY_PIVOT_CHANGED, new PropertyChangeListener() {			
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
		
		JPanel westPanel = new JPanel(new BorderLayout());
		westPanel.add(BorderLayout.CENTER, pivotCardPanel);
		
		JPanel buttonsPanel = createButtonsPanel();
		if(buttonsPanel!=null) westPanel.add(BorderLayout.SOUTH, buttonsPanel);

		westPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchPane, biosampleDetailPanel);
		contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, westPane, westPanel);
		
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
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				searchPane.getSearchTree().eventStudyChanged();
				if(getRootPane()!=null) {
					getRootPane().setDefaultButton(searchPane.getSearchButton());
				}
				//Load my samples in stockcare (forcedbiotype is not null)
				if(first && searchPane.getForcedBiotypes()!=null) {
					new SwingWorkerExtended("Loading my Samples", BiosampleTab.this, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
						protected void done() {
							if(first) {
								first = false;
								searchPane.queryMySamples();
							}
						}
					};
				}
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
				tableOrRackTab.setBiosamples(biosamples);
			} else if(action==SpiritChangeType.MODEL_UPDATED) {
				Map<Integer, Biosample> sel = DAOBiosample.getBiosamplesByIds(JPAUtil.getIds(biosamples));
				
				if(tableOrRackTab.getBiosamples().size()<200) {
					tableOrRackTab.reload();
				} else {
					
					boolean changed = false;
					List<Biosample> rows = new ArrayList<Biosample>(tableOrRackTab.getBiosamples());
					for (int i = 0; i < rows.size(); i++) {
						Biosample newBio = sel.get(rows.get(i).getId());
						if(newBio!=null) {
							changed = true;
							rows.set(i, newBio);
						}
					}
					if(changed) tableOrRackTab.setBiosamples(rows);
				}
			} else if(action==SpiritChangeType.MODEL_DELETED) {
				List<Biosample> rows = tableOrRackTab.getBiosamples();
				rows.removeAll((List<Biosample>) details);
				if(rows.size()<200) rows = JPAUtil.reattach(rows);
				tableOrRackTab.setBiosamples(rows);
			}
			pivotBiosamples(false);
		} else {
			refreshFilters();
		}
		repaint();

	}
	
	
	@Override
	public void refreshFilters() {
//		searchPane.getSearchTree().eventStudyChanged();
	}

	
	@Override
	public String getStudyIds() {
		return searchPane.getSearchTree().getStudyId();
	}
	
	@Override
	public void setStudyIds(String studyIds) {
		if(studyIds==null) return;
		String currentStudy = searchPane.getSearchTree().getStudyId();
		if(currentStudy!=null && currentStudy.equals(studyIds)) return; //no need to refresh
		
		searchPane.getSearchTree().setStudyId(studyIds);	
	}
	
	
	protected BiosampleOrRackTab getBiosampleOrRackTab() {
		return tableOrRackTab;
	}
	
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
		searchPane.setQuery(q);
	}
		
	public void setBiosamples(List<Biosample> biosamples) {
		first = false;
		tableOrRackTab.setBiosamples(biosamples);
		pivotBiosamples(pivotCardPanel.isPivotMode());
	}
	
	public BiosampleOrRackTab getTableOrRackTab() {
		return tableOrRackTab;
	}
	
	public void setRack(Location rack) {
		first = false;
		tableOrRackTab.setRack(rack);
	}
	

	public void pivotBiosamples(boolean enablePivot) {
		pivotCardPanel.clear();
		if(enablePivot) {
			
			new SwingWorkerExtended("Pivoting", pivotCardPanel, false) {
				final List<Result> toPivot = new ArrayList<Result>();
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
					pivotCardPanel.setResults(toPivot, null, new InventoryPivotTemplate(), true);
					
				}
			};			
		}
		pivotCardPanel.setPivotMode(enablePivot);
	}

	public void queryMySamples() {		
		searchPane.queryMySamples();
	}
}

