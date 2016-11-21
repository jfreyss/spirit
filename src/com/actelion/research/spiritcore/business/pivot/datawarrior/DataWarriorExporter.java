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

package com.actelion.research.spiritcore.business.pivot.datawarrior;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.pivot.Computed;
import com.actelion.research.spiritcore.business.pivot.ExpandedPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotCell;
import com.actelion.research.spiritcore.business.pivot.PivotColumn;
import com.actelion.research.spiritcore.business.pivot.PivotDataTable;
import com.actelion.research.spiritcore.business.pivot.PivotItem;
import com.actelion.research.spiritcore.business.pivot.PivotItemFactory;
import com.actelion.research.spiritcore.business.pivot.PivotRow;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;

public class DataWarriorExporter {

	public static final int MAX_VIEWS = 10;
	private DataWarriorConfig config;
	private PivotTemplate tpl;
	private PivotDataTable pivotTable;
	private final boolean generateAllGraphs;
	
	/**
	 * Exports ala Excel
	 * @param pivotTable
	 * @param config
	 * @throws Exception
	 */
	private DataWarriorExporter(PivotDataTable pivotTable, DataWarriorConfig config) throws Exception {
		
		this.config = config;
		this.tpl = pivotTable.getTemplate();
		this.pivotTable = pivotTable;
		this.generateAllGraphs = false;
	}
	
	private DataWarriorExporter(List<Result> results, DataWarriorConfig config, SpiritUser user) throws Exception {
		this.config = config;
		
		if(config.getCustomTemplate()==null) {		
			// Smart template: everything is by column to start with
			//
			this.tpl = new ExpandedPivotTemplate();			
			((ExpandedPivotTemplate)tpl).init(results, config, user);

		} else {
			this.tpl = config.getCustomTemplate();
		}
		
		this.pivotTable = new PivotDataTable(results, config.getSkippedAttributes(), tpl);
		this.generateAllGraphs = true;
	}
	
	/**
	 * Simple exportation to DW without templates or graphs (ala Excel) 
	 * 
	 * @param pivotTable
	 * @return
	 * @throws Exception
	 */
	public static StringBuilder getDwar(PivotDataTable pivotTable) throws Exception {
		assert pivotTable!=null;
		assert pivotTable.getTemplate()!=null;
		if(pivotTable.getTemplate().getPivotItems(Where.ASCELL).size()>0) {
			for(PivotColumn c : pivotTable.getPivotColumns()) {
				for(PivotRow r : pivotTable.getPivotRows()) {
					PivotCell cc = r.getPivotCell(c);
					if(cc.getNestedKeys().size()>1) {
						throw new Exception("You cannot export to DW if your data contains nested tables. Please redefine your template");						
					}					
				}
			}			
		}
		//Export with a default DW Config: automatic
		DataWarriorExporter exporter = new DataWarriorExporter(pivotTable, new DataWarriorConfig());
		return exporter.buildDataWarrior();
	}

	/**
	 * 
	 * @param pivotTable
	 * @param model
	 * @return
	 */
	public static StringBuilder getDwar(List<Result> results, DataWarriorConfig model, SpiritUser user) throws Exception {		
		DataWarriorExporter exporter = new DataWarriorExporter(results, model, user);
		return exporter.buildDataWarrior();
	}
	
	/**
	 * 
	 * @param pivotTable
	 * @param model
	 * @return
	 */
	public static List<String> getViewNames(List<Result> results, DataWarriorConfig model, SpiritUser user) throws Exception {		
		DataWarriorExporter exporter = new DataWarriorExporter(results, model, user);
		return exporter.getViewNames();
	}
	
	private List<String> getViewNames() {
		List<String> res = new ArrayList<>();
		if(pivotTable.getPivotColumns().size()==0) {
			res.add("2D");
		} else {
			List<PivotColumn> pivotColumns = pivotTable.getPivotColumns();
			for(PivotColumn item: pivotColumns) {
				res.add(item.getTitle());
				if(!generateAllGraphs) break;
			}
		}
		return res;
	}
	
	/***************************
	 * Build the DW content
	 * @return
	 */
	private StringBuilder buildDataWarrior() {	
		
		List<String> views;
		
		if(config.getViewNames()==null) {
			views = getViewNames();
			if(views.size()>MAX_VIEWS) views = views.subList(0, MAX_VIEWS);
		} else {
			views = config.getViewNames();
		}

		// Orbit? if in Actelion domain
//		ListHashMap<Integer, RawDataFile> bid2raw = null;
		/*
		//TODO Fix confusion Container.id, Biosample.id in Orbit
		if (SpiritAdapter.getInstance().isInActelionDomain() && tpl.getWhere(PivotItem.BIOSAMPLE_TOPID) == Where.ASROW) {
			bid2raw = new ListHashMap<Integer, RawDataFile>();
			List<Integer> ids = new ArrayList<Integer>();
			for (int row = 0; row < pivotTable.getPivotRows().size(); row++) {
				for (Biosample b : pivotTable.getPivotRows().get(row).getBiosamples()) {
					ids.add((int) b.getTopParent().getId());
				}
			}
			try {
				List<RawDataFile> raws = DAODataFile.LoadRawDataFilesByBioSample(ids);
				for (RawDataFile rawDataFile : raws) {
					bid2raw.add(rawDataFile.getBioSampleId(), rawDataFile);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (bid2raw.size() == 0) {
				bid2raw = null;
			}
		}
		 */
		
		// DW header
		StringBuilder sb = new StringBuilder();
		sb.append("<datawarrior-fileinfo>\n");
		sb.append("<version=\"3.1\">\n");
		sb.append("<rowcount=\"" + pivotTable.getPivotRows().size() + "\">\n");
		sb.append("</datawarrior-fileinfo>\n");
//		if (bid2raw != null) {
//			sb.append("<column properties>\n");
//			sb.append("<columnName=\"" + PivotItem.BIOSAMPLE_TOPID.getName() + "\">\n");
//			sb.append("<columnProperty=\"detailSource0	orbit/preview\">\n");
//			sb.append("<columnProperty=\"detailName0	Orbit Preview\">\n");
//			sb.append("<columnProperty=\"detailType0	image/jpeg\">\n");
//			sb.append("<columnProperty=\"detailCount	1\">\n");
//			sb.append("</column properties>\n");
//		}

		int rows = pivotTable.getPivotRows().size();
		int columns = tpl.getPivotItems(Where.ASROW).size()
				+ (config.isExportAll()? pivotTable.getPivotColumns().size(): views.size()) * (tpl.getComputed() != null && tpl.getComputed() != Computed.NONE ? 2 : 1);
		String[][] table = new String[rows + 1][columns];

		// /////////////////////////////////////////////////////////////////////
		// First the headers
		int c = 0;
		for (PivotItem item : tpl.getPivotItems(Where.ASROW)) {
			table[0][c++] = MiscUtils.removeHtmlAndNewLines(item.getFullName());
		}

		for (int col = 0; col < pivotTable.getPivotColumns().size(); col++) {
			PivotColumn column = pivotTable.getPivotColumns().get(col);
			String cn = MiscUtils.removeHtmlAndNewLines(column.getTitle());
			if(!config.isExportAll() && !views.contains(column.getTitle())) continue;
			table[0][c++] = cn;

			if (tpl.getComputed() != null && tpl.getComputed() != Computed.NONE) {
				table[0][c++] = cn + " (" + tpl.getComputed() + ")";
			}
		}

		// /////////////////////////////////////////////////////////////////////
		// Then the data
		for (int r = 0; r < rows; r++) {
			PivotRow pivotRow = pivotTable.getPivotRows().get(r);

			// standard data
			c = 0;
			for (PivotItem item : tpl.getPivotItems(Where.ASROW)) {

				String cn = MiscUtils.removeHtmlAndNewLines(item.getTitle(pivotRow.getRepresentative()));
				if (cn == null)
					cn = "";
				table[r + 1][c] = cn;

				if (item == PivotItemFactory.BIOSAMPLE_TOPID) {
//					if (bid2raw != null) {
//						// Orbit attachment?
//						// Find topIds
//						List<Integer> ids = new ArrayList<Integer>();
//						for (Biosample b : pivotRow.getBiosamples())
//							ids.add((int) b.getTopParent().getId());
//						for (Integer id : ids) {
//							List<RawDataFile> raws = bid2raw.get(id);
//							if (raws != null) {
//								for (RawDataFile rawDataFile : raws) {
//									table[r + 1][c] += "|#|0:" + rawDataFile.getRawDataFileId();
//								}
//							}
//						}
//					}
				}
				c++;

			}

			for (int col = 0; col < pivotTable.getPivotColumns().size(); col++) {
				PivotColumn pivotColumn = pivotTable.getPivotColumns().get(col);			
				if(!config.isExportAll() && !views.contains(pivotColumn.getTitle())) continue;

				
				PivotCell pivotCell = pivotRow.getPivotCell(pivotColumn);				
				Object val = pivotCell.getValue();
				String cn = val==null? "": MiscUtils.removeHtmlAndNewLines(val.toString());
				
				
				if (c < table[r + 1].length)
					table[r + 1][c++] = cn;

				if (tpl.getComputed() != null && tpl.getComputed() != Computed.NONE) {
					cn = pivotCell.getComputed() == null ? "" : pivotCell.getComputed().toString();
					if (c < table[r + 1].length)
						table[r + 1][c++] = cn;
				}

			}
		}
		sb.append(MiscUtils.concatenate(table, true));

		// Write the template
		int n = tpl.getPivotItems(Where.ASROW).size();
		sb.append("<datawarrior properties>\n");

		// General
		String mainView = "2D View";
		sb.append("<filter0=\"#category#\t" + PivotItemFactory.RESULT_TEST.getFullName() + "\">\n");
		sb.append("<filter1=\"#category#\t" + PivotItemFactory.RESULT_INPUT.getFullName() + "\">\n");
		sb.append("<filter2=\"#category#\t" + PivotItemFactory.BIOSAMPLE_BIOTYPE.getFullName() + "\">\n");
		sb.append("<filter3=\"#category#\t" + PivotItemFactory.BIOSAMPLE_BIOTYPE.getFullName() + "\">\n");
		sb.append("<filter4=\"#category#\t" + PivotItemFactory.BIOSAMPLE_NAME.getFullName() + "\">\n");

		//Log scale
		if(config.isLogScale()) {
			StringBuilder sb2 = new StringBuilder();
			for (int i = 0; i < views.size(); i++) {
				sb2.append((i>0?"\t":"") + MiscUtils.removeHtmlAndNewLines(views.get(i)));
			}			
			sb.append("<logarithmicView=\"" + sb2 + "\">\n");
		}
		
		// Custom Order
		sb.append("<customOrderCount=\"" + n + "\">\n");
		for (int i = 0; i < n; i++) {
			PivotItem item = tpl.getPivotItems(Where.ASROW).get(i);

			Set<String> values = new HashSet<String>();
			for (int r = 1; r < table.length; r++) {
				values.add(table[r][i]);
			}

			// Order the values
			List<String> ordered = new ArrayList<String>(values);
			if (item == PivotItemFactory.STUDY_PHASE_DATE) {
				Collections.sort(ordered, PHASE_COMPARATOR);
			} else {
				Collections.sort(ordered, CompareUtils.STRING_COMPARATOR);
			}

			StringBuilder sb2 = new StringBuilder();
			for (String string : ordered) {
				if (string == null)
					continue;
				sb2.append((sb2.length() > 0 ? "\t" : "") + string);
			}
			sb.append("<customOrder_" + i + "=\"" + table[0][i] + "\t" + sb2 + "\">\n");
		}

		int viewNo = 0;

		if (views.size() == 0) views.add("2D");

		sb.append("<mainViewName0=\"Table\">\n");
		sb.append("<mainViewType0=\"tableView\">\n");
		sb.append("<mainView=\"" + mainView + "\">\n");
		sb.append("<mainViewCount=\"" + (1 + views.size()) + "\">\n");

		for (int i = 0; i < views.size(); i++) {
			viewNo++;
			String viewName = MiscUtils.removeHtmlAndNewLines(views.get(i));

			//X Axis
			System.out.println("DataWarriorExporter.buildDataWarrior() "+viewName+" "+config.getXAxis());
			if(config.getXAxis()==null) {
				//Auto (boxplot per group or by phase)				
				PivotColumn pivotColumn = pivotTable.getPivotColumn(viewName);
				boolean byPhase = /*pivotColumn==null? false:*/ Result.isPhaseDependant(pivotColumn.getResults());
				if(byPhase) {
					String xAxis = MiscUtils.removeHtmlAndNewLines(PivotDataType.PHASE.getColumnName(pivotTable));
					sb.append("<axisColumn_" + viewName + "_0=\"" + xAxis + "\">\n");
					sb.append("<connectionColumn_" + viewName + "=\"<connectCases>\">\n");
					String separate = MiscUtils.removeHtmlAndNewLines(PivotDataType.GROUP.getColumnName(pivotTable));
					if (separate != null) {
						sb.append("<caseSeparationColumn_" + viewName + "=\"" + separate + "\">\n");
						sb.append("<caseSeparationValue_" + viewName + "=\"0.5\">\n");
					}
					
				} else if(Biosample.getGroups(Result.getBiosamples(pivotColumn.getResults())).size()>1) {
					String xAxis = MiscUtils.removeHtmlAndNewLines(PivotDataType.GROUP.getColumnName(pivotTable));
					sb.append("<axisColumn_" + viewName + "_0=\"" + xAxis + "\">\n");					
				} else {
					String xAxis = MiscUtils.removeHtmlAndNewLines(PivotDataType.TOPSAMPLE.getColumnName(pivotTable));
					sb.append("<axisColumn_" + viewName + "_0=\"" + xAxis + "\">\n");					
				}
				
			} else {
				//XAxis
				String xAxis = MiscUtils.removeHtmlAndNewLines(config.getXAxis().getColumnName(pivotTable));
				if (xAxis != null) {
					sb.append("<axisColumn_" + viewName + "_0=\"" + xAxis + "\">\n");
				}
				if (config.getXAxis() == PivotDataType.PHASE) {
					sb.append("<connectionColumn_" + viewName + "=\"<connectCases>\">\n");
				}
				
				//Separate
				if (config.getSeparate() != null) {
					String separate = MiscUtils.removeHtmlAndNewLines(config.getSeparate().getColumnName(pivotTable));
					if (separate != null) {
						sb.append("<caseSeparationColumn_" + viewName + "=\"" + separate + "\">\n");
						sb.append("<caseSeparationValue_" + viewName + "=\"" + 0.5 + "\">\n");
					}
				}
			}
			
			//Y Axis
			String yAxis = MiscUtils.removeHtmlAndNewLines(views.get(i));					
			if (yAxis != null) {
				if(tpl.getComputed()!=null && tpl.getComputed()!=Computed.NONE) {
					 yAxis+= " (" + tpl.getComputed() + ")";
				}
				sb.append("<axisColumn_" + viewName + "_1=\"" + yAxis + "\">\n");
			}
			
			sb.append("<markersize_" + viewName + "=\"" + 0.5 + "\">\n");
			sb.append("<sizeAdaption_" + viewName + "=\"false\">\n");
			sb.append("<chartType_" + viewName + "=\"" + config.getType() + "\">\n");

			String color = PivotDataType.GROUP.getColumnName(pivotTable);
			if(color!=null) sb.append("<colorColumn_" + viewName + "=\"" + color + "\">\n");

			int count = 0;
			List<Group> allGroups = new ArrayList<>(Biosample.getGroups(Result.getBiosamples(pivotTable.getResults())));
			Collections.sort(allGroups, CompareUtils.OBJECT_COMPARATOR);
			sb.append("<colorCount_" + viewName + "=\"" + allGroups.size() + "\">\n");
			sb.append("<colorListMode_" + viewName + "=\"Categories\">\n");
			for (Group g : allGroups) {
				int rgb = g != null && g.getColorRgb() != null? g.getColorRgb().intValue(): Color.LIGHT_GRAY.getRGB();
				sb.append("<color_" + viewName + "_" + (count++) + "=\"" + rgb + "\">\n");
			}

			if (config.getSplit() != null) {
				String split = MiscUtils.removeHtmlAndNewLines(config.getSplit().getColumnName(pivotTable));
				if (split != null) {
					sb.append("<splitViewColumn1_" + viewName + "=\"" + split + "\">\n");
				}
			}

			sb.append("<filter0=\"#browser#\t<disabled>\">\n");
			sb.append("<boxplotMeanMode_" + viewName + "=\"median\">\n");
			sb.append("<mainViewName" + viewNo + "=\"" + viewName + "\">\n");
			sb.append("<mainViewType" + viewNo + "=\"2Dview\">\n");
		}
		sb.append("</datawarrior properties>\n");
		return sb;
	}

	/**
	 * Comparator for strings, comparing blocks separately so that the order becomes SLIDE-1, SLIDE-2, SLIDE-10, SLIDE-10-1, ...
	 */
	public static final Comparator<String> PHASE_COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			return comparePhases(o1, o2);
		}
	};

	public static int comparePhases(String s1, String s2) {
		if(s1==null && s2==null) return 0; 
		if(s1==null) return 1; //Null at the end
		if(s2==null) return -1;
		return convertPhaseIntoMinutes(s1) - convertPhaseIntoMinutes(s2);
	}
	
	private static int convertPhaseIntoMinutes(String dateString) {
		if(dateString==null || !dateString.startsWith("d")) return -1;
		int index = dateString.indexOf("_");
		int index2 = dateString.indexOf("h", index);
		int days;
		int hours;
		int minutes;
		if(index<0 || index2<0) {
			try {
				days = Integer.parseInt(dateString.substring(1));
				hours = 0;
				minutes = 0;
			} catch (Exception e) {
				return -1;
			}
		} else {
			try {
				days = Integer.parseInt(dateString.substring(1, index));
				hours = Integer.parseInt(dateString.substring(index+1, index2));
				minutes = dateString.length()<=index2+1?0: Integer.parseInt(dateString.substring(index2+1));				
			} catch (Exception e) {
				return -1;
			}					
		}
		return (days*24+hours)*60+minutes;
	}
	
}
