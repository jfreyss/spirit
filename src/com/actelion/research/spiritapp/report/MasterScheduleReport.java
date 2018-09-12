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

package com.actelion.research.spiritapp.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.sf.dynamicreports.report.builder.DynamicReports.cmp;
import static net.sf.dynamicreports.report.builder.DynamicReports.col;
import static net.sf.dynamicreports.report.builder.DynamicReports.type;

import com.actelion.research.spiritapp.ui.study.StudyTable;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.ui.exceltable.Column;

import net.sf.dynamicreports.report.builder.column.ColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.datasource.DRDataSource;

public class MasterScheduleReport extends AbstractDynamicReport {
	private List<Study> studies = null;
	private StudyTable studyTable = null;
	private DRDataSource dataSource = null;
	
	public MasterScheduleReport(List<Study> studies, StudyTable studyTable) {
		super();
		this.studies = studies;
		this.studyTable = studyTable;
	}

	@Override
	public void buildReport() throws Exception {
		if ( studies == null || studyTable == null ) {
			throw new Exception("Cannot generate Master Schedule report. No studies given");
		}
		
		List<String> headerColumns = new ArrayList<String>();
		String[] row = null;
		String columnName = null;
		boolean bProcessHeader = true;
		int i = 0;
		for ( Study study : studies ) {
			if ( study == null ) {
				continue;
			}
			
			if (bProcessHeader) {
				// TODO : compute report width (with all columns) to check if it will not overflow the document width (A4 landscape) 
				for(Column<Study, ?> column: studyTable.getModel().getAllColumns()) {
					columnName = column.getShortName();
					if ( columnName.equals("#") 
							|| columnName.equalsIgnoreCase("Admins")
							|| columnName.equalsIgnoreCase("Group")
							|| columnName.equalsIgnoreCase("Status")
							|| columnName.equalsIgnoreCase("Type")
							|| columnName.startsWith("Test facility study")
							|| columnName.equalsIgnoreCase("CreatedBy")
							|| columnName.equalsIgnoreCase("UpdatedBy")
							|| columnName.equalsIgnoreCase("Biosamples") ) {
						continue;
					}
					if ( columnName.equals("Type") ) {
						columnName = "Study Type";
					}

					TextColumnBuilder<String> newCol = col.column(columnName, columnName, type.stringType());
					int width = getReportColumnWidth(columnName);
					if ( width > 0 ) {
						newCol.setFixedWidth(width);
					}
					reportBuilder.addColumn(newCol);
					headerColumns.add(columnName);
				}
				
				dataSource = new DRDataSource(headerColumns.toArray(new String[0]));
				bProcessHeader = false;
			}
			
			// add rows
			i = 0;
			row = new String[headerColumns.size()];
			for (String c : headerColumns) {
				if ( c.equalsIgnoreCase("Id") ) {
					row[i] = study.getStudyId(); 
				}/* else if ( c.equals("Status") ) {
					row[i] = study.getState();
				} else if ( c.equals("Study Type") ) {
					row[i] = study.getType();
				}*/ else if ( c.equalsIgnoreCase("Title") ) {
					row[i] = study.getTitle();
				}/* else if ( c.equalsIgnoreCase("CreatedBy") ) {
					row[i] = study.getCreUser();
				}*/ else if ( c.equalsIgnoreCase("Compound No") ) {
					row[i] = study.getMetadata("Compound");
				} else if ( c.equalsIgnoreCase("study initiation date") ) {
					row[i] = study.getMetadata("Initiation");
				} else if ( c.equalsIgnoreCase("Experimental starting date") ) {
					row[i] = study.getMetadata("ExpStart");
				} else if ( c.equalsIgnoreCase("Experimental completion date") ) {
					row[i] = study.getMetadata("ExpCompletion");
				} else if ( c.equalsIgnoreCase("planned draft report date") ) {
					row[i] = study.getMetadata("DraftReport");
				} else if ( c.equalsIgnoreCase("study archiving date") ) {
					row[i] = study.getMetadata("ArchivingDate");
				} else if ( c.equalsIgnoreCase("planned date for data transfer") ) {
					row[i] = study.getMetadata("PlannedTransfer");
				} else if ( c.equalsIgnoreCase("date for data transfer") ) {
					row[i] = study.getMetadata("DataTransfer");
				} else if ( c.equalsIgnoreCase("study/phase completion date") ) {
					row[i] = study.getMetadata("Completion");
				} else if ( c.equalsIgnoreCase("GLP Status") ) {
					row[i] = study.getMetadata("GLP");
				} else if ( c.equalsIgnoreCase("Study Director Name") ) {
					row[i] = study.getMetadata("Director");
				} else if ( c.equalsIgnoreCase("Principal Investigator Name") ) {
					row[i] = study.getMetadata("Investigator");
				} else if ( c.equalsIgnoreCase("test facility Name") ) {
					row[i] = study.getMetadata("TFacName");
				}/* else if ( c.equalsIgnoreCase("test facility study number") ) {
					row[i] = study.getMetadata("TFacNo");
				} else if ( c.equalsIgnoreCase("test facility study title") ) {
					row[i] = study.getMetadata("TFacTitle");
				}*/
				
				i++;
			}
			
			dataSource.add(row);
		}
		
		previewTitle = "Report for Master Schedule";
		reportBuilder.setDataSource(dataSource);
		
		reportBuilder.addPageFooter(cmp.text("Master Schedule Report"));
	}
}
