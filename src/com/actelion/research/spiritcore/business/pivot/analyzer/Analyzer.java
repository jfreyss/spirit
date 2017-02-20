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

package com.actelion.research.spiritcore.business.pivot.analyzer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.actelion.research.spiritcore.business.pivot.PerInputPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotCell;
import com.actelion.research.spiritcore.business.pivot.PivotCellKey;
import com.actelion.research.spiritcore.business.pivot.PivotColumn;
import com.actelion.research.spiritcore.business.pivot.PivotDataTable;
import com.actelion.research.spiritcore.business.pivot.PivotRow;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.Counter;
import com.actelion.research.util.PriorityQueue;

/**
 * Class used to analyze the results 
 * @author Joel Freyss
 */
public class Analyzer {
	
	public static enum Sort {NAME, GROUPS, N, KW, DISTRIB}

	private Map<PivotColumn, ColumnAnalyser> columnAnalysis;
	private PivotDataTable table;
	private Sort sort = Sort.KW;

	public Analyzer(List<Result> results, SpiritUser user) {
		PivotTemplate template = new PerInputPivotTemplate();
		template.init(results);
		template.expand(results, user);
		table = new PivotDataTable(results, template);
	}
	
	public ColumnAnalyser getColumnFromIndex(int index) {
		for (ColumnAnalyser ca : getColumns().values()) {
			if(ca.getIndex()==index) return ca;
		}
		return null;
	}
	

	public Map<PivotColumn, ColumnAnalyser> getColumns() {
		if(columnAnalysis==null) compute();
		return columnAnalysis;
	}
	
	public ColumnAnalyser getColumn(PivotColumn col) {
		if(columnAnalysis==null) compute();
		return columnAnalysis.get(col);
	}
	
	
	private void compute() {
		//Analyze each column
		columnAnalysis = new LinkedHashMap<>();
		int colNo = 0;
		for(PivotColumn col: table.getPivotColumns()) {
			List<SimpleResult> groupValues = new ArrayList<>();
			List<Result> results = col.getResults();
			
			//Retrieve results
			boolean byPhase = Result.isPhaseDependant(results);
			for(PivotRow row: table.getPivotRows()) {
				PivotCell cell = row.getPivotCell(col);
				for (PivotCellKey key : cell.getNestedKeys()) {					
					PivotCell subCell = cell.getNested(key);
					for(ResultValue rv: subCell.getValues()) {
						if(rv.getValue()!=null && rv.getValue().length()>0) {	
							groupValues.add(new SimpleResult(rv.getResult().getBiosample().getInheritedGroup(),
									byPhase? rv.getResult().getInheritedPhase(): null,
									rv.getResult().getBiosample(),
									rv.getDoubleValue(), rv.getValue()));
						}
					}
				}
			}
			if(groupValues.size()>0) {
				Collections.sort(groupValues);
				ColumnAnalyser analysis = new ColumnAnalyser(colNo, groupValues);  
				columnAnalysis.put(col, analysis);
			}
			colNo++;
		}
	}
	
	public void setSort(Sort sort) {
		this.sort = sort;
	}
	
	public List<PivotColumn> getSortedColumns(Sort sort) {
		if(columnAnalysis==null) compute();
		List<PivotColumn> cols = new ArrayList<>(columnAnalysis.keySet());
		if(sort!=null && sort!=Sort.NAME) {
			Collections.sort(cols, new Comparator<PivotColumn>() {
				@Override
				public int compare(PivotColumn o1, PivotColumn o2) {
					ColumnAnalyser a1 = columnAnalysis.get(o1);
					ColumnAnalyser a2 = columnAnalysis.get(o2);
					switch(sort) {
					case KW: return CompareUtils.compare(a1.getKruskalWallis(), a2.getKruskalWallis());
					case GROUPS: return -CompareUtils.compare(a1.getNGroups(), a2.getNGroups());
					case N: return -CompareUtils.compare(a1.getN(), a2.getN());
					case DISTRIB: return CompareUtils.compare(a1.getDistribution(), a2.getDistribution());
					default:
						throw new IllegalArgumentException("Not implemented: "+sort);
					}
				}
			});
		}
		return cols;
	}

	
	public String getReport() {
		if(columnAnalysis==null) compute();
		
		List<PivotColumn> cols = getSortedColumns(sort);
		if(cols.size()==0) {
			return "You need to have numerical values to analyze your data";
		}
		
		Counter<Integer> counter = new Counter<Integer>();
		for (PivotColumn col : cols) {			
			ColumnAnalyser a = columnAnalysis.get(col);
			counter.increaseCounter(a.getN());
		}
		int nComplete = counter.getKeys().size()>0? counter.getKeySorted().get(0): 0;

		int count = 0;		
		PriorityQueue<Integer> indexes = new PriorityQueue<>();
		List<Integer> indexesPca = new ArrayList<>();
		StringBuilder sbRows = new StringBuilder();
		for (PivotColumn col : cols) {
						
			ColumnAnalyser a = columnAnalysis.get(col);
			boolean complete = a.getN() == nComplete;
			
			sbRows.append("<tr style='background:" + (a.getKruskalWallis()==null?"": a.getKruskalWallis()>.2?"#FFCCCC": a.getKruskalWallis()>.05?"#FFEEDD": a.getKruskalWallis()>.01?"#DDFFDD": "#AAFFAA") + "'>");
			sbRows.append("<td style='white:space:nowrap;text-align:right'>"+(++count)+".</td>");			
			sbRows.append("<td style='white:space:nowrap'>"+MiscUtils.convertLabel(col.getTitle())+"</td>");
			sbRows.append("<td>"+a.getNGroups()+"</td>");
			sbRows.append("<td style='color:" + (complete?"blue":"black") + "'>"+a.getN()+"</td>");
			sbRows.append("<td>"+(a.getDistribution()==null?"?": "<b>"+a.getDistribution().toString())+"</b>"+"</td>");
			sbRows.append("<td align=center><img src='histo://"+a.getBinsHisto(false)+"'></td>");
			sbRows.append("<td><b>"+fc(a.getKruskalWallis())+"</b></td>");			
			sbRows.append("<td style='white-space:nowrap'><a href='graphs:" + a.getIndex() + "'>Show Graph in DW</a></td>");			
			sbRows.append("</tr>");
			if(a.getKruskalWallis()!=null) {
				indexes.add(a.getIndex(), a.getKruskalWallis());
				if(complete) {
					indexesPca.add(a.getIndex());
				}
			}
		}
		
		
		StringBuilder sb = new StringBuilder();
		sb.append("<h2>Column Analysis</h2>");

		sb.append("The following statistics shows the columns (biomarkers, measured data) sorted according to the <a href='http://en.wikipedia.org/wiki/Kruskal-Wallis_one-way_analysis_of_variance'>Kruskal-Wallis</a> value (KW). KW is a non-parametrics, which compare the ranks of each measured value for each groups.<br>"
				+ "The value given show the ssignificance level to the null hypothesis: <b>'the samples come from groups with non-equal medians'</b><br>"
				+ "<br>"
				+ "<u>Careful:</u> a low value shows there is something to see on the graph, but it does not prove that a biomarker will actually signifant in all similar studies. Those values should be adjusted by the number of columns or measured genes. For more information, look at <a href='http://en.wikipedia.org/wiki/Multiple_comparisons_problem'>Wikipedia:MultipleComparisons</a><br>");
		sb.append("<h2>Export Graphs to DataWarrior:</h2>");
		
		sb.append("<table width=100%><tr><td width=60% height=100%>");		
		sb.append("<div style='width:100%; height:60px; background:#FFFFCC;padding:2px; border: solid 1px #CCCCCC'>");
		if(indexes.size()>0) {
			if(indexes.size()<10) {
				sb.append("<a href='graphs:" + (MiscUtils.flatten(indexes.sublist(0, indexes.size()), ",")) + "'> Export all " + indexes.size() + " graphs</a><br>");
			} else {
				sb.append("<a href='graphs:" + (MiscUtils.flatten(indexes.sublist(0, 10), ",")) + "'> Export the 10 most significant graphs</a><br>");
			}
		}
		
		sb.append("</div>");		
		sb.append("</td>");
		
		sb.append("<td width=40% height=100%>");		
		sb.append("<div style='width:100%; height:60px; background:#DDDDCC;padding:2px; border: solid 1px #CCCCCC'><b>Principal Component Analysis:</b><br> ");
		sb.append("<span style='font-size:90%'>(Computes the maximum complete dataset)</span><br>");
		sb.append("<a href='pca:" + (MiscUtils.flatten(indexesPca, ",")) + "'> PCA (" + indexesPca.size() + " Graphs)</a><br>");
		sb.append("</div>");
		sb.append("</td></tr></table>");
		
		sb.append("<h2>Data</h2>");		
		sb.append("<table style='font-size:95%; border: solid 1px #DDDDDD'>");
		sb.append("<tr style='background:#DDDDDD'>");
		sb.append("<td></td>");
		sb.append("<td><a href='sort:name'>Column</a></td>");
		sb.append("<td><a href='sort:groups'>N.Groups.</a></td>");
		sb.append("<td><a href='sort:N'>N.Values.</a></td>");
		sb.append("<td><a href='sort:distrib'>Distrib.</a></td>");
		sb.append("<td>Hist.</td>");
		sb.append("<td><a href='sort:K'>KW</a></td>");
		sb.append("</tr>");
		sb.append(sbRows);
		sb.append("</table>");
		return sb.toString();
	}

	private String fc(Double v) {
		if(v==null) return "";
		if(v==Double.NaN) return "<span style='color:gray'>NaN</span>";
//		int nDecimals = Integer.toString(Math.abs((int) (double) v)).length();
		String format = Math.abs(v)>=0.001? "0.000": "0.#E0";
		String s = new DecimalFormat(format).format(v);
		if(v<.01) return "<span style='color:#46C646'>" + s + "</span>";
		else if(v<.05) return "<span style='color:#008000'>" + s + "</span>";
		else if(v<.2) return "<span style='color:#558000'>" + s + "</span>";
		else if(v<.5) return "<span style='color:#804000'>" + s + "</span>";
		else return "<span style='color:#800000'>" + s + "</span>";
	}
	
}
