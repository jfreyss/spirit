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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.actelion.research.spiritcore.business.audit.DifferenceItem;
import com.actelion.research.spiritcore.business.audit.DifferenceItem.ChangeType;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.audit.RevisionItem;

import net.sf.dynamicreports.report.datasource.DRDataSource;

public class EventsReport extends AbstractDynamicReport {

	private String filterByType;
	private Serializable filterById;
	private Integer filterBySid;
	private DRDataSource dataSource = null;
	private List<Revision> revisions = null;
	private List<String> footer = new ArrayList<String>();

	@Override
	public void buildReport() throws Exception {
		if ( revisions == null ) {
			throw new Exception("Cannot generate Events report. No revision given");
		}

		// create report columns
		String[] headerColumns = {"revId", "date", "study", "entity", "type", "field", "old_value", "new_value", "reason", "user"};
		dataSource = new DRDataSource(headerColumns);

		reportBuilder.columns(col.column("Rev. Id", "revId", type.stringType()).setFixedWidth(getReportColumnWidth("Rev. Id")),
				col.column("User", "user", type.stringType()).setFixedWidth(getReportColumnWidth("User")),
				col.column("Date", "date", type.stringType()).setFixedWidth(getReportColumnWidth("Date")),
				col.column("Study", "study", type.stringType()).setFixedWidth(getReportColumnWidth("Study")),
				//col.column("Difference", "difference", type.stringType()).setFixedWidth(getReportColumnWidth("Difference")),
				col.column("Entity", "entity", type.stringType()).setFixedWidth(80),
				col.column("Type", "type", type.stringType()).setFixedWidth(40),
				col.column("Field", "field", type.stringType()).setFixedWidth(80),
				col.column("Old Value", "old_value", type.stringType()),
				col.column("New Value", "new_value", type.stringType()),
				col.column("Reason", "reason", type.stringType()));

		String[] row = null;
		String revisionId = "", date = "", study = "", entity = "", type = "", field = "", oldValue = "", newValue = "", reason = "", user = "";
		for ( RevisionItem revisionItem : Revision.getRevisionItems(revisions, filterByType, filterById, filterBySid) ) {
			if ( revisionItem == null ) {
				continue;
			}

			revisionId = Integer.toString(revisionItem.getRevId());
			LocalDateTime revDateTime = revisionItem.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");
			date = revDateTime.format(formatter);

			study = revisionItem.getStudy()==null? "": revisionItem.getStudy().getStudyId();

			entity = revisionItem.getEntityName();
			type = getChangeLabel(revisionItem.getChangeType());
			field = revisionItem.getField();
			oldValue = revisionItem.getOldValue();
			newValue = revisionItem.getNewValue();


			reason = revisionItem.getReason()==null?"": revisionItem.getReason();
			user = revisionItem.getUser()==null? "":revisionItem.getUser();

			row = new String[] {revisionId, date, study, entity, type, field, oldValue, newValue, reason, user};
			dataSource.add(row);
		}

		reportBuilder.setDataSource(dataSource);

		for (String s : footer) {
			reportBuilder.addPageFooter(cmp.text(s));
		}
	}

	private String getChangeLabel(ChangeType changeType) {
		if ( changeType == DifferenceItem.ChangeType.ADD ) {
			return "ADD";
		} else if ( changeType == DifferenceItem.ChangeType.DEL ) {
			return "DEL";
		} else if ( changeType == DifferenceItem.ChangeType.MOD ) {
			return "MOD";
		}
		return "";
	}

	public void setRevisions(List<Revision> revisions) {
		this.revisions = revisions;
	}

	public void setPreviewTitle(String title) {
		this.previewTitle = title;
	}

	public void setFilters(String filterByType, Serializable filterById, Integer filterBySid) {
		this.filterByType = filterByType;
		this.filterById = filterById;
		this.filterBySid = filterBySid;
	}
}
