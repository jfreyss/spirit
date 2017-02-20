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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.business.pivot.PivotColumn;
import com.actelion.research.spiritcore.business.pivot.analyzer.Analyzer;
import com.actelion.research.spiritcore.business.pivot.analyzer.Analyzer.Sort;
import com.actelion.research.spiritcore.business.pivot.analyzer.ColumnAnalyser;
import com.actelion.research.spiritcore.business.pivot.analyzer.ColumnAnalyser.Distribution;
import com.actelion.research.spiritcore.business.pivot.analyzer.SimpleResult;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.UIUtils;

public class GraphPanel extends JScrollPane {
	
	private Analyzer analyzer;
		
	private ListPane<BoxPlot> listPane = new ListPane<>();
	private List<PivotColumn> sortedCols = new ArrayList<>();
	
	public GraphPanel() {
		super(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		setViewportView(listPane);
		getViewport().setBackground(Color.WHITE);
	}
	
	public void setListSelectionListener(ListSelectionListener listener) {
		listPane.setListSelectionListener(listener);
	}
	
	public void setResults(List<Result> results) {
		this.analyzer = new Analyzer(results, Spirit.getUser());
		refreshGraphs(); 		
	}
	
	public void refreshGraphs() {
		if(analyzer==null) return;
		
		sortedCols.clear();
		List<BoxPlot> panels = new ArrayList<>();
		for (PivotColumn col : analyzer.getSortedColumns(Sort.KW)) {
			ColumnAnalyser ca = analyzer.getColumn(col);
			sortedCols.add(col);
			
			String[] titles = col.getTitle().split("\n");
			String yLabel = titles[titles.length-1];
			String title = titles.length<=1? yLabel: MiscUtils.flatten(Arrays.asList(titles).subList(0, titles.length-1));
			
	        List<SimpleResult> simpleResults = ca.getSimpleResults();
	        
	        Map<Group, List<SimpleResult>> map = SimpleResult.groupingPerGroup(simpleResults);
	        List<Group> groups = new ArrayList<>(map.keySet());
	        Collections.sort(groups, CompareUtils.OBJECT_COMPARATOR);
	        List<Series> allSeries = new ArrayList<>();
	        for (Group group : groups) {
		        allSeries.add(new Series(
	        		group==null? "": group.getName(), 
	        		group==null? Color.GRAY: group.getColor(),
	        		map.get(group)));		        
			}
	        
			final BoxPlot boxPlot = new BoxPlot();
			boxPlot.setTitle(MiscUtils.removeHtmlAndNewLines(title));
			boxPlot.setyAxis(MiscUtils.removeHtmlAndNewLines(yLabel));			
	        boxPlot.setSeries(allSeries);
	        boxPlot.setLogScale(ca.getDistribution()==Distribution.LOGNORMAL);
	        
	        Double kw = ca.getKruskalWallis();
	        if(kw!=null) {
	        	boxPlot.setBackground(kw>.2? UIUtils.getColor(255, 220, 220): kw>.05? UIUtils.getColor(255, 242, 230): kw>.01? UIUtils.getColor(240, 255, 240): UIUtils.getColor(220, 255, 220));
	        }
	        
	        panels.add(boxPlot);
		}
		listPane.setItems(panels);
		listPane.setSelectedItems(panels);

	}
	
	public List<Result> getSelectedResults() {
		Set<Result> results = new LinkedHashSet<>();
		for (int index : listPane.getSelectedIndexes()) {
			results.addAll(sortedCols.get(index).getResults());				
		}
		return new ArrayList<>(results);
	}
	
	public static void main(String[] args) throws Exception {
		Spirit.initUI();
		ResultQuery q = new ResultQuery();
//		q.setStudyIds("S-00680");
//		q.setStudyIds("S-00600");
		q.setStudyIds("S-00629");
//		q.setBiotype("Organ");
//		q.getTestIds().add(DAOTest.getTest("Weighing").getId());
//		q.getTestIds().add(DAOTest.getTest("Tumor Volume").getId());
		List<Result> results = DAOResult.queryResults(q, null);
		System.out.println("AnalysisPanel.main() "+results);
		GraphPanel p = new GraphPanel();
		p.setResults(results);
		
		JFrame f = new JFrame("Test");		
		f.setContentPane(p);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		UIUtils.adaptSize(f, 900, 700);
		f.setVisible(true);
	}
}
