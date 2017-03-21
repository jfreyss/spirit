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

package com.actelion.research.spiritapp.spirit.ui.pivot.graph;

import java.awt.GridLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.pivot.PivotPanel;
import com.actelion.research.spiritapp.spirit.ui.pivot.PivotTable;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.util.ui.UIUtils;

public class GraphPanelWithResults extends JPanel {

	private GraphPanel graphPanel = new GraphPanel();
	private PivotPanel pivotCardPanel = new PivotPanel(false, null, null);

	public GraphPanelWithResults() {
		super(new GridLayout());
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, graphPanel, pivotCardPanel);
		add(splitPane);

		splitPane.setDividerLocation(250);
		graphPanel.setListSelectionListener(e->{
			pivotCardPanel.setResults(graphPanel.getSelectedResults());
		});
	}

	public GraphPanel getGraphPanel() {
		return graphPanel;
	}

	public PivotTable getPivotTable() {
		return pivotCardPanel.getPivotTable();
	}

	public void setPivotTemplate(PivotTemplate tpl) {
		pivotCardPanel.setCurrentPivotTemplate(tpl);
	}

	/**
	 * Analyzes the results based on a standard analyzer.
	 * Can be slow.
	 * Upon completion, select all graphs (if less than 10), or select the first one
	 * @param results
	 */
	public void setResults(List<Result> results) {
		graphPanel.setResults(results).afterDone(() -> {
			if(graphPanel.getItemSize()>10) {
				graphPanel.setSelectedIndex(0);
			} else {
				graphPanel.selectAll();
			}
		});
	}

	public static void main(String[] args) throws Exception {
		Spirit.initUI();
		ResultQuery q = new ResultQuery();
		q.setStudyIds("S-00629");
		List<Result> results = DAOResult.queryResults(q, null);
		System.out.println("AnalysisPanel.main() "+results);
		GraphPanelWithResults p = new GraphPanelWithResults();
		p.setResults(results);

		JFrame f = new JFrame("Test");
		f.setContentPane(p);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		UIUtils.adaptSize(f, 900, 700);
		f.setVisible(true);
	}
}
