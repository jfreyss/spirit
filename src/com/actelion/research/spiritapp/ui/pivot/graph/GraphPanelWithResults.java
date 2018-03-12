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

package com.actelion.research.spiritapp.ui.pivot.graph;

import java.awt.GridLayout;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.ui.pivot.PivotPanel;
import com.actelion.research.spiritapp.ui.pivot.PivotTable;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;

/**
 * The GraphPanelWithResults is a UI component displaying the overview of the results on top and the data below.
 *
 * @author Joel Freyss
 *
 */
public class GraphPanelWithResults extends JPanel {

	private GraphPanel graphPanel = new GraphPanel();
	private PivotPanel pivotPanel = new PivotPanel(null, null);

	public GraphPanelWithResults() {
		super(new GridLayout());
		JSplitPane splitPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.VERTICAL_SPLIT, graphPanel, pivotPanel);
		splitPane.setDividerLocation(150);
		add(splitPane);

		graphPanel.setListSelectionListener(e->{
			pivotPanel.setResults(graphPanel.getSelectedResults());
		});
	}


	public GraphPanel getGraphPanel() {
		return graphPanel;
	}

	public PivotPanel getPivotPanel() {
		return pivotPanel;
	}

	public void setSelectedIndex(int index) {
		graphPanel.setSelectedIndex(index);
	}

	public PivotTable getPivotTable() {
		return pivotPanel.getPivotTable();
	}

	public void setPivotTemplate(PivotTemplate tpl) {
		pivotPanel.setPivotTemplate(tpl);
	}

	public void setDefaultTemplates(PivotTemplate[] tpls) {
		pivotPanel.setDefaultTemplates(tpls);
	}

	/**
	 * Analyzes the results based on a standard analyzer.
	 * Can be slow.
	 * Upon completion, select all graphs (if less than 10), or select the first one
	 * @param results
	 */
	public void setResults(List<Result> results, boolean selectAll) {
		graphPanel.setResults(results).afterDone(() -> {
			if(selectAll) {
				graphPanel.selectAll();
			} else {
				graphPanel.setSelectedIndex(0);
			}
		});
	}
}
