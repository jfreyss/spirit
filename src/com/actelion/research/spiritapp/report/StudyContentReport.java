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

import static net.sf.dynamicreports.report.builder.DynamicReports.cmp;
import static net.sf.dynamicreports.report.builder.DynamicReports.col;
import static net.sf.dynamicreports.report.builder.DynamicReports.type;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SampleListValidator;

import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.datasource.DRDataSource;


public class StudyContentReport extends AbstractDynamicReport {
	private Study study = null;
	private List<Biosample> biosamples = null;
	private Set<String> excludeSet = new HashSet<String>(Arrays.asList("SampleList", "Species", "Treatment", "Observations",
			"Type", "SubjectId", "Sampling date"));
	DRDataSource dataSource = null;

	public StudyContentReport(Study study, List<Biosample> biosamples) {
		super();
		this.study = study;
		this.biosamples = biosamples;


	}

	@Override
	public void buildReport() throws Exception {
		if ( study == null ) {
			throw new Exception("Cannot generate sample report. Invalid Study");
		}
		if ( biosamples == null ) {
			throw new Exception("Cannot generate sample report for this view");
		}
		String[] header = null;
		String[] row = null;
		String columnName = null;
		int columnCount = 0;
		boolean bProcessHeader = true;
		for ( Biosample b : biosamples) {
			if (b == null)
				continue;

			Set<BiotypeMetadata> keys = b.getMetadataValues().keySet();
			columnCount = keys.size() + 2;	// + 1 for the containerId, +1 for the sampleId
			String location = (b.getLocation() == null) ? "" : "\r\n" + b.getLocation().toString();
			String container = (b.getContainerId() == null) ? "-" : b.getContainerId();
			if (bProcessHeader) {
				header = new String[columnCount];
				int i=0;
				columnName = "SampleId";
				header[i++] = columnName;
				reportBuilder.addColumn(col
						.column(columnName,	columnName, type.stringType())
						.setFixedWidth(getReportColumnWidth(columnName)));
				columnName = SampleListValidator.ColumnLabel.BARCODE.getLabel() + "\r\nLocation";
				header[i++] = columnName;
				reportBuilder.addColumn(col
						.column(columnName,	columnName, type.stringType()));
				for (BiotypeMetadata k : keys) {
					columnName = k.getName();
					if ( !excludeSet.contains(columnName) ) {
						header[i++] = columnName;
						TextColumnBuilder newCol = col.column(columnName, columnName, type.stringType());
						int width = getReportColumnWidth(columnName);
						if ( width > 0 ) {
							newCol.setFixedWidth(width);
						}
						reportBuilder.addColumn(newCol);
					}
				}
				dataSource = new DRDataSource(header);
				bProcessHeader = false;
			}

			int i=0;
			row = new String[columnCount];
			row[i++] = b.getSampleId();
			row[i++] = container + location;
			for (BiotypeMetadata k : keys) {
				if (!excludeSet.contains(k.getName())) {
					row[i++] = b.getMetadataValue(k);
				}
			}
			// TODO : row is not exactly the right type. Maybe consider using a JRTableModelDataSource to be more compliant
			dataSource.add(row);
		}

		previewTitle = "Report for Study " + study.getStudyId();

		reportBuilder.setDataSource(dataSource);

		// add specific footer notes here
		reportBuilder.addPageFooter(cmp.text("Study ID : " + study.getStudyId() + " | Study Title : " + study.getTitle()));
	}
}
