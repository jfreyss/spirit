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
import static net.sf.dynamicreports.report.builder.DynamicReports.report;
import static net.sf.dynamicreports.report.builder.DynamicReports.stl;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.JFrame;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.SpiritFrame;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JasperViewer;

/**
 * This abstract class provides the default behavior for creating reports
 * It contains also some constants related to Selenium manipulated objects (study, samples...)
 *
 * It is the responsibility of each subclass to use its own data source (default, table model...)
 *
 * @author mankoka1
 *
 */
public abstract class AbstractDynamicReport {

	protected static int DEFAULT_REPORT_COLUMN_WIDTH = 30;
	protected static int REPORT_CONTAINER_ID_COLUMN_WIDTH = 70;
	protected static int REPORT_TYPE_COLUMN_WIDTH = 40;
	protected static int REPORT_SUBJECT_ID_COLUMN_WIDTH = 35;
	protected static int REPORT_GENDER_COLUMN_WIDTH = 35;
	protected static int REPORT_DOSE_COLUMN_WIDTH = 25;
	protected static int REPORT_DOSE_GROUP_COLUMN_WIDTH = 40;
	protected static int REPORT_DATE_COLUMN_WIDTH = 60;
	protected static int REPORT_TREATMENT_COLUMN_WIDTH = 40;
	protected static int REPORT_SAMPLE_LIST_COLUMN_WIDTH = 100;
	protected static int REPORT_SPECIES_COLUMN_WIDTH = 40;
	protected static int REPORT_VISIT_COLUMN_WIDTH = 25;
	protected static int REPORT_PERIOD_COLUMN_WIDTH = 30;
	protected static int REPORT_SEX_COLUMN_WIDTH = 25;
	protected static int REPORT_ROW_NUMBER_WIDTH = 5;
	protected static int REPORT_STUDY_ID_WIDTH = 55;
	protected static int REPORT_STUDY_STATUS_WIDTH = 45;
	protected static int REPORT_STUDY_TITLE_WIDTH = 100;
	protected static int REPORT_STUDY_TYPE_WIDTH = 60;
	protected static int REPORT_STUDY_COMPOUND_WIDTH = 75;
	protected static int REPORT_STUDY_CREATED_BY_WIDTH = 50;
	protected static int REPORT_STUDY_DIRECTOR_WIDTH = 70;
	protected static int REPORT_STUDY_INVESTIGATOR_WIDTH = 70;
	protected static int REPORT_REVISION_ID_WIDTH = 30;
	protected static int REPORT_REVISION_DIFFERENCE_WIDTH = 400;
	protected static int REPORT_SAMPLE_ID_LIST = 75;
	protected static int REPORT_TIMEPOINT_COLUMN_WIDTH = 25;

	protected static int REPORT_DEFAULT_FONT_SIZE = 6;

	protected JasperReportBuilder reportBuilder = null;
	protected JasperPrint reportPrint = null;
	protected JasperViewer reportViewer = null;

	protected String previewTitle = "Selenium Report";
	protected String[] footerData = null;

	// Font styles
	protected StyleBuilder boldStyle = stl.style().bold();
	protected StyleBuilder boldCenteredStyle = stl.style(boldStyle).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER);
	protected StyleBuilder columnTitleStyle = stl.style(boldCenteredStyle)
			.setBorder(stl.pen1Point())
			.setBackgroundColor(Color.LIGHT_GRAY);
	protected StyleBuilder footerStyle = stl.style().setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT);

	public AbstractDynamicReport() {
		reportBuilder = report();

		// footer information
		String username = Spirit.getUsername();
		String versionLabel = Spirit.class.getPackage().getImplementationVersion() == null ? "0.4" : Spirit.class.getPackage().getImplementationVersion();
		LocalDate now = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
		String currentDate = now.format(formatter);

		reportBuilder
		.setColumnTitleStyle(columnTitleStyle)
		.highlightDetailEvenRows()
		.setPageFormat(PageType.A4, PageOrientation.LANDSCAPE)
		.setDefaultFont(stl.fontCourierNew().setFontSize(REPORT_DEFAULT_FONT_SIZE))
		.pageFooter(
				cmp.text("Printed by " + username + " the " + currentDate),
				cmp.text("Selenium version " + versionLabel));
	}

	public abstract void buildReport() throws Exception;

	public void addFooterNote(String footerNote) {
		reportBuilder.addPageFooter(cmp.text(footerNote));
	}

	public void showPreview() throws DRException {
		// add pages display to footer (i.e. : 1 / 4)
		reportBuilder.addPageFooter(cmp.horizontalList().add(
				cmp.image(Spirit.class.getResource("idorsia_logo.png")).setFixedHeight(20),
				cmp.pageXofY().setStyle(footerStyle)));

		reportPrint = reportBuilder.toJasperPrint();
		reportViewer = new JasperViewer(reportPrint, false);
		reportViewer.setTitle(previewTitle);
		reportViewer.setIconImage(SpiritFrame.getApplicationIcon());
		reportViewer.setExtendedState(JFrame.MAXIMIZED_BOTH);
		reportViewer.setVisible(true);
	}

	protected int getReportColumnWidth(String columnName) {
		if ( columnName.equals("Sampling date")
				|| columnName.equals("Sampling occasion")
				|| columnName.equals("Collection date")
				|| columnName.equals("Collection time")
				|| columnName.equals("Archiving date")
				|| columnName.equals("Disposal date")
				|| columnName.equals("Experimental starting date")
				|| columnName.equals("Experiment completion date")
				|| columnName.equals("Date") )
			return REPORT_DATE_COLUMN_WIDTH;
		if ( columnName.equals("SampleList") )
			return REPORT_SAMPLE_LIST_COLUMN_WIDTH;
		if ( columnName.equals("Species") )
			return REPORT_SPECIES_COLUMN_WIDTH;
		if ( columnName.equals("Dose") )
			return REPORT_DOSE_COLUMN_WIDTH;
		if ( columnName.equals("Type") )
			return REPORT_TYPE_COLUMN_WIDTH;
		if ( columnName.equals("SubjectId")
				|| columnName.equals("AnimalId") )
			return REPORT_SUBJECT_ID_COLUMN_WIDTH;
		if ( columnName.equals("Type") )
			return REPORT_TYPE_COLUMN_WIDTH;
		if ( columnName.equals("Gender") )
			return REPORT_GENDER_COLUMN_WIDTH;
		if ( columnName.equals("Id")
				|| columnName.equals("Study") )
			return REPORT_STUDY_ID_WIDTH;
		if ( columnName.equals("SampleId") )
			return REPORT_SAMPLE_ID_LIST;
		if ( columnName.equals("Status") )
			return REPORT_STUDY_STATUS_WIDTH;
		if ( columnName.equals("Title") )
			return REPORT_STUDY_TITLE_WIDTH;
		if ( columnName.equals("Study Type") )
			return REPORT_STUDY_TYPE_WIDTH;
		if ( columnName.equals("Compound No") )
			return REPORT_STUDY_COMPOUND_WIDTH;
		if ( columnName.equals("CreatedBy")
				|| columnName.equals("UpdatedBy")
				|| columnName.equals("User") )
			return REPORT_STUDY_CREATED_BY_WIDTH;
		if ( columnName.equals("Study Director Name") )
			return REPORT_STUDY_DIRECTOR_WIDTH;
		if ( columnName.equals("Principal Investigator Name") )
			return REPORT_STUDY_INVESTIGATOR_WIDTH;
		if ( columnName.equals("Rev. Id") )
			return REPORT_REVISION_ID_WIDTH;
		if ( columnName.equals("Difference") )
			return REPORT_REVISION_DIFFERENCE_WIDTH;
		if ( columnName.equals("Dose group") )
			return REPORT_DOSE_GROUP_COLUMN_WIDTH;
		if ( columnName.equals("Visit") )
			return REPORT_VISIT_COLUMN_WIDTH;
		if ( columnName.equals("Period") )
			return REPORT_PERIOD_COLUMN_WIDTH;
		if ( columnName.equals("Sex") )
			return REPORT_SEX_COLUMN_WIDTH;
		if ( columnName.equals("Time point") )
			return REPORT_TIMEPOINT_COLUMN_WIDTH;

		return -1;
	}
}
