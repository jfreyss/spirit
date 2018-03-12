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

package com.actelion.research.spiritapp.ui.result.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.spiritapp.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.spiritcore.util.Pair;

public class PivotHelper {

	public static class ResultMap extends LinkedHashMap<String, String> {
		public ResultMap() {super();}
		public ResultMap(Result r) {
			super();
			TestAttribute outAtt = r.getTest().getOutputAttributes().get(0);

			if(r.getPhase()!=null || (r.getBiosample()!=null && r.getBiosample().getInheritedStudy()!=null)) {
				String phase = r.getPhase()==null?null: r.getPhase().getShortName();
				if(phase!=null && phase.length()>0) put("Phase", phase);
				else put("Phase", "");
			}

			String sampleId = r.getBiosample()==null?null: r.getBiosample().getSampleId();
			if(sampleId!=null && sampleId.length()>0) put("SampleId", sampleId);

			for (TestAttribute inputAtt : r.getTest().getAttributes(OutputType.INPUT)) {
				String input = r.getResultValue(inputAtt).getValue()==null?"": r.getResultValue(inputAtt).getValue();
				put(inputAtt.getName(), input);
			}

			String output = r.getResultValue(outAtt).getValue();
			if(output!=null && output.length()>0) put("output", output);
		}
	}

	private static class RowElement extends ArrayList<String> implements Comparable<RowElement> {
		public RowElement(ResultMap item, List<String> headerKeyCols) {
			for (String k : headerKeyCols) {
				add(item.get(k));
			}
		}
		@Override
		public int compareTo(RowElement o) {
			for (int i = 0; i < size() && i<o.size(); i++) {
				int c = (get(i)==null?"": get(i)).compareTo( (o.get(i)==null?"": o.get(i)));
				if(c!=0) return c;
			}
			return 0;
		}
	}

	private List<ResultMap> items = new ArrayList<>();


	public PivotHelper(Collection<Result> results) {
		for (Result result : results) {
			items.add(new ResultMap(result));
		}
	}

	public List<String> getPivotableColumns() {
		Set<String> res = new LinkedHashSet<>();
		for (ResultMap item : items) {
			res.addAll(item.keySet());
		}
		res.remove("output");
		return new ArrayList<>(res);
	}

	/**
	 * Pivot the items like:
	 *
	 * <pre>
	 * headerKeyCols			headerPivotCols
	 * sampleId	input			phase1	phase2	phase3
	 * blah		blah			output	output	output
	 * blah		blah			output	output	output
	 * </pre>
	 *
	 * @param pivot
	 * @return
	 * @throws Exception
	 */
	public String[][] pivot(String pivot) throws Exception {

		List<String> headerKeyCols = getPivotableColumns();


		if(!headerKeyCols.contains(pivot)) throw new Exception("You can only pivot on "+headerKeyCols);
		headerKeyCols.remove(pivot);
		if(headerKeyCols.contains("SampleId")) {
			headerKeyCols.remove("SampleId");
			headerKeyCols.add(0, "SampleId");
		}

		//
		Set<String> headerPivotCols = new TreeSet<>();

		Set<RowElement> rowElts = new TreeSet<>();
		ListHashMap<Pair<RowElement, String>, String> vals = new ListHashMap<>();

		//Analyze the data
		for (ResultMap item : items) {

			String pivotValue = item.get(pivot);
			if(pivotValue==null) pivotValue = "";
			RowElement rowElement = new RowElement(item, headerKeyCols);

			rowElts.add(rowElement);
			headerPivotCols.add(pivotValue);

			vals.add(new Pair<RowElement, String>(rowElement, pivotValue), item.get("output"));
		}

		///////////////////////////////////////////////////////
		//Create the Headers for the pivot table
		///////////////////////////////////////////////////////
		List<List<String>> rows = new ArrayList<List<String>>();
		List<String> row = new ArrayList<String>();
		rows.add(row);
		//Add the items shown by row
		for (String s : headerKeyCols) row.add(s);
		//Add the items shown by col
		for (String s : headerPivotCols) row.add(s);

		///////////////////////////////////////////////////////
		//Data
		///////////////////////////////////////////////////////
		for (RowElement rowElt : rowElts) {
			boolean mustNext = true;
			for(int index = 0; mustNext; index++) {
				mustNext = false;

				row = new ArrayList<String>();
				rows.add(row);
				for (String s : rowElt) row.add(s);

				for (String s : headerPivotCols) {
					List<String> vs = vals.get(new Pair<RowElement, String>(rowElt, s));
					if(vs!=null && index<vs.size()) {
						row.add(vs.get(index));
						if(index+1<vs.size()) mustNext = true;
					} else {
						row.add("");
					}
				}
			}
		}

		return POIUtils.convertTable(rows);
	}

}
