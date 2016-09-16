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

package com.actelion.research.spiritapp.spirit.services.report;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.LocationFormat;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.FormatterUtils;

public class SamplesLocationReport extends AbstractReport {
	
	
	private static ReportParameter SHOW_WITHOUT_LOCATION_PARAMETER = new ReportParameter("Show samples with an empty location", Boolean.TRUE);
	private static ReportParameter SHOW_RESULTS_PARAMETER = new ReportParameter("Show Results", Boolean.TRUE);
	
	public SamplesLocationReport() {		
		super(ReportCategory.SAMPLES, 
				"Inventory", 
				"Show the location of each sample in the study: " + MiscUtils.convert2Html(
						"\tLocation\tContainer\tGroup\tPhase\tTopId\tBiotype\tMetadata\n"
						+ "SampleId1\t\n"
						+ "SampleId2\t\n"), 
				new ReportParameter[]{SHOW_WITHOUT_LOCATION_PARAMETER, SHOW_RESULTS_PARAMETER});
	}


	@Override
	protected void populateWorkBook() throws Exception {		
		boolean showWithoutLocation = getParameter(SHOW_WITHOUT_LOCATION_PARAMETER)==Boolean.TRUE;
		boolean showResults = getParameter(SHOW_RESULTS_PARAMETER)==Boolean.TRUE;
		SpiritUser user = Spirit.getUser();
		
		//Load the samples and their results
		List<Biosample> allSamples = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForStudyIds(study.getStudyId()), user);		
		Map<Biosample, List<Result>> sample2results = new HashMap<Biosample, List<Result>>();
		if(showResults){
			List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForBiosampleIds(JPAUtil.getIds(allSamples)), user);
			sample2results = Result.mapBiosample(results);
			System.out.println("SamplesLocationReport.populateWorkBook() FOUND "+results.size());
		}
		
		Collections.sort(allSamples, Biosample.HIERARCHY_COMPARATOR);
		
		if(allSamples.size()==0) throw new Exception("There are no samples to be reported. Make sure you have a sampling template with some required weighings.");
		
		//Loop through each sample and display the data
		Sheet sheet = createSheet(wb, "Locations");
		sheet.setFitToPage(true);
		createHeadersWithTitle(sheet, study, "Sample's locations");
		
		///////////////
		//		0		1		2		3		4		5		6		7		8		9		10
		//5		Locat.	CType	CId		Group	Phase	TopId	SId		Biotype	Meta.	Comment	Owner	
		
		//Write headers
		
		int y = 3;
		int x = 0;
		set(sheet, y, x++, "Location", Style.S_TH_LEFT);
		set(sheet, y, x++, "ContainerType", Style.S_TH_LEFT);
		set(sheet, y, x++, "ContainerId", Style.S_TH_LEFT);
		set(sheet, y, x++, "Group", Style.S_TH_LEFT);
		set(sheet, y, x++, "Phase", Style.S_TH_LEFT);
		set(sheet, y, x++, "TopId", Style.S_TH_LEFT);
		set(sheet, y, x++, "SampleId", Style.S_TH_LEFT);
		set(sheet, y, x++, "Biotype", Style.S_TH_LEFT);
		set(sheet, y, x++, "SampleName", Style.S_TH_LEFT);
		set(sheet, y, x++, "Metadata", Style.S_TH_LEFT);
		set(sheet, y, x++, "Comments", Style.S_TH_LEFT);
		set(sheet, y, x++, "Owner", Style.S_TH_LEFT);
		set(sheet, y, x++, "Date", Style.S_TH_LEFT);
		if(showResults) {
			set(sheet, y, x++, "Results", Style.S_TH_LEFT);
		}
		int nCols = x-1;
		
		Biosample previous = null;
		
		drawLineAbove(sheet, y, 0, nCols, (short)1);
		for (Biosample b : allSamples) {
			if(!showWithoutLocation && b.getLocation()==null) continue;
			
			if(previous==null) {
				drawLineUnder(sheet, y, 0, nCols, (short)1);
			} else if(b.getTopParent()!=previous.getTopParent()) {
				drawLineUnder(sheet, y, 0, nCols, previous.getTopParent()==null || previous.getTopParent().getInheritedGroup()!=b.getInheritedGroup()? (short)5: (short)1);
			} else if(b.getInheritedPhase()!=previous.getInheritedPhase()) {
				drawLineUnder(sheet, y, 0, nCols, (short)4);
			}
			previous = b;
			y++;
			x = 0;
			set(sheet, y, x++, b.getLocationString(LocationFormat.FULL_POS, user), Style.S_TD_LEFT);
			set(sheet, y, x++, b.getContainerType()==null?"": b.getContainerType().getName(), Style.S_TD_LEFT);
			set(sheet, y, x++, b.getContainerId(), Style.S_TD_LEFT);
			set(sheet, y, x++, b.getInheritedGroupString(user.getUsername()), Style.S_TD_LEFT);
			set(sheet, y, x++, b.getInheritedPhase()==null?"": b.getInheritedPhase().getShortName(), Style.S_TD_LEFT);
			set(sheet, y, x++, b.getTopParent().getSampleId(), b.getTopParent()==b? Style.S_TD_BOLD_LEFT: Style.S_TD_LEFT);
			set(sheet, y, x++, b.getSampleId(), Style.S_TD_BOLD_LEFT);
			set(sheet, y, x++, b.getBiotype().getName(), Style.S_TD_LEFT);
			set(sheet, y, x++, b.getSampleName(), Style.S_TD_LEFT);
			set(sheet, y, x++, b.getMetadataAsString(), Style.S_TD_LEFT);
			set(sheet, y, x++, b.getComments(), Style.S_TD_LEFT);
			set(sheet, y, x++, b.getCreUser(), Style.S_TD_LEFT);
			set(sheet, y, x++, FormatterUtils.formatDate(b.getCreDate()), Style.S_TD_LEFT);
			
			if(showResults) {
				List<Result> results = sample2results.get(b);
				if(results==null || results.size()==0) {
					set(sheet, y, x++, "", Style.S_TD_SMALL);
				} else {
					Collections.sort(results);
					StringBuilder sb = new StringBuilder();					
					for(Result r: results) {
						sb.append((sb.length()>0?"\n":"") + r.getDetailsWithoutSampleId());
					}
					set(sheet, y, x++, sb.toString(), Style.S_TD_SMALL);
				}
			}

		}
			
		POIUtils.autoSizeColumns(sheet, 15000, false);
		if(wb.getNumberOfSheets()==0) throw new Exception("There are no samplings to be reported");

	}
	
	public static void main(String[] args)  {
		try {
			Spirit.setUser(DAOSpiritUser.loadUser("freyssj"));
			SamplesLocationReport wg = new SamplesLocationReport();
			wg.populateReport(DAOStudy.getStudyByStudyId("S-00511"));
			wg.export(null);
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.exit(1);
	}
	
}
