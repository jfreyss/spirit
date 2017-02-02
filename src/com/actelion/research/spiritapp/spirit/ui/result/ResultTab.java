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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.spirit.ui.pivot.PivotCardPanel;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritTab;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;

public class ResultTab extends JPanel implements ISpiritTab {

	private final PivotCardPanel pivotCardPanel;
	private final ResultSearchPane searchPane;
	
	private final JPanel center = new JPanel(new BorderLayout());
	
	public ResultTab() {
		this(null);
	}
	public ResultTab(Biotype forcedBiotype) {
		this.pivotCardPanel = new PivotCardPanel(null, null);
		this.searchPane = new ResultSearchPane(pivotCardPanel, forcedBiotype);
		
		JPanel queryPanel = new JPanel(new BorderLayout());
		queryPanel.add(BorderLayout.CENTER, searchPane);
		
		JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, queryPanel, center);
		contentPane.setDividerLocation(300);
		contentPane.setOneTouchExpandable(true);


		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, contentPane);		
		center.add(BorderLayout.CENTER, pivotCardPanel);		

		JPanel buttonsPanel = createButtonsPanel();
		if(buttonsPanel!=null) center.add(BorderLayout.SOUTH, buttonsPanel);
				
		ResultActions.attachPopup(pivotCardPanel.getPivotTable());
		
		searchPane.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				ResultTab.this.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
			}
		});
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				searchPane.getSearchTree().repopulate();
				if(getRootPane()!=null) {
					getRootPane().setDefaultButton(searchPane.getSearchButton());
				}
			}
		});
	}
	
	
		
	
		
	@Override
	public void refreshFilters() {
		searchPane.getSearchTree().repopulate();
	}	
	
	@Override
	public<T> void fireModelChanged(final SpiritChangeType action, final Class<T> what, final List<T> details) {
		if(what==Test.class || what==Study.class || what==Result.class) {
			refreshFilters();
		}		
		if(what==Result.class) {
			searchPane.search(searchPane.getSearchTree().updateQuery());
		}
	}

	
	public void setResults(List<Result> results, PivotTemplate template) {
		pivotCardPanel.setResults(results, null, template, false);
	}
	
	public void query(ResultQuery q) {
		searchPane.query(q);
	}
	@Override
	public String getStudyIds() {
		return searchPane.getSearchTree().getStudyId();
	}
	
	@Override
	public void setStudyIds(String s) {
		if(s==null) return;
		String currentStudy = searchPane.getSearchTree().getStudyId();
		if(currentStudy!=null && currentStudy.equals(s)) return; //no need to refresh
		
		searchPane.getSearchTree().setQuery(ResultQuery.createQueryForStudyIds(s));
		setResults(new ArrayList<Result>(), null);
	}
	
	public void setCurrentPivotTemplate(PivotTemplate pivotTemplate) {
		pivotCardPanel.setCurrentPivotTemplate(pivotTemplate);
	}
	public void setAnalysisTemplate(PivotTemplate pivotTemplate) {
		pivotCardPanel.setAnalysisPivotTemplate(pivotTemplate);
	}
	public void setDefaultTemplates(PivotTemplate[] pivotTemplates) {
		pivotCardPanel.setDefaultTemplates(pivotTemplates);
	}
	
	/**
	 * To be overriden by classes to get a custom button panel
	 * @return
	 */
	protected JPanel createButtonsPanel() {
		return null;
	}
	
	public PivotCardPanel getPivotCardPanel() {
		return pivotCardPanel;
	}
	public List<Result> getSelection() {
		return pivotCardPanel.getPivotTable().getSelectedResults();
	}

	public List<Result> getResults() {
		return pivotCardPanel.getResults();
	}
	
}
