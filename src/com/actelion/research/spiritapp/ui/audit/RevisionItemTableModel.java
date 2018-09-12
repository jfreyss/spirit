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

package com.actelion.research.spiritapp.ui.audit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;

import com.actelion.research.spiritcore.business.audit.RevisionItem;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.DateColumn;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;
import com.actelion.research.util.ui.exceltable.IntegerColumn;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;
import com.actelion.research.util.ui.exceltable.StringColumn;

public class RevisionItemTableModel extends ExtendTableModel<RevisionItem> {

	private IntegerColumn<RevisionItem> revColumn = new IntegerColumn<RevisionItem>("RevId") {
		@Override
		public Integer getValue(RevisionItem row) {
			return row.getRevId();
		}
	};

	private StringColumn<RevisionItem> userColumn = new StringColumn<RevisionItem>("User") {
		@Override
		public String getValue(RevisionItem row) {
			return row.getUser();
		}
	};

	private DateColumn<RevisionItem> dateColumn = new DateColumn<RevisionItem>("Date") {
		@Override
		public Date getValue(RevisionItem row) {
			return row.getDate();
		}
		@Override
		public void postProcess(AbstractExtendTable<RevisionItem> table, RevisionItem row, int rowNo, Object value, JComponent comp) {
			((JLabelNoRepaint)comp).setText(FormatterUtils.formatDateTime((Date)value));
		}
	};

	private StringColumn<RevisionItem> studyColumn = new StringColumn<RevisionItem>("StudyId") {
		@Override
		public String getValue(RevisionItem row) {
			return row.getStudy()==null?"": row.getStudy().getStudyId();
		}
	};

	private StringColumn<RevisionItem> reasonColumn = new StringColumn<RevisionItem>("Reason") {
		@Override
		public String getValue(RevisionItem row) {
			return row.getReason();
		}
	};

	private StringColumn<RevisionItem> entityTypeColumn = new StringColumn<RevisionItem>("EntityType") {
		@Override
		public String getValue(RevisionItem row) {
			return row.getEntityType();
		}
	};

	private StringColumn<RevisionItem> entityColumn = new StringColumn<RevisionItem>("Name") {
		@Override
		public String getValue(RevisionItem row) {
			return row.getEntityName();
		}
		@Override
		public void postProcess(AbstractExtendTable<RevisionItem> table, RevisionItem row, int rowNo, Object value, JComponent comp) {
			comp.setFont(FastFont.BOLD);
		}
	};

	private StringColumn<RevisionItem> typeColumn = new StringColumn<RevisionItem>("Type") {
		@Override
		public String getValue(RevisionItem row) {
			return row.getChangeType()==null?"": row.getChangeType().toString();
		}
		@Override
		public void postProcess(AbstractExtendTable<RevisionItem> table, RevisionItem row, int rowNo, Object value, JComponent comp) {
			comp.setFont(FastFont.BOLD);
		}
	};

	private StringColumn<RevisionItem> fieldColumn = new StringColumn<RevisionItem>("Field") {
		@Override
		public String getValue(RevisionItem row) {
			return row.getField();
		}
		@Override
		public boolean shouldMerge(RevisionItem r1, RevisionItem r2) {return false;}

		@Override
		public void postProcess(AbstractExtendTable<RevisionItem> table, RevisionItem row, int rowNo, Object value, JComponent comp) {
			comp.setFont(FastFont.BOLD);
		}
	};

	private StringColumn<RevisionItem> oldValueColumn = new StringColumn<RevisionItem>("Old Value") {
		@Override
		public String getValue(RevisionItem row) {
			return row.getOldValue();
		}
		@Override
		public boolean shouldMerge(RevisionItem r1, RevisionItem r2) {return false;}
	};

	private StringColumn<RevisionItem> newValueColumn = new StringColumn<RevisionItem>("New Value") {
		@Override
		public String getValue(RevisionItem row) {
			return row.getNewValue();
		}
		@Override
		public boolean shouldMerge(RevisionItem r1, RevisionItem r2) {return false;}
	};

	public RevisionItemTableModel() {
		initColumns(false);
	}

	public void initColumns(boolean addEntityType) {
		List<Column<RevisionItem, ?>> allColumns = new ArrayList<>();
		allColumns.add(COLUMN_ROWNO);
		allColumns.add(revColumn);
		allColumns.add(userColumn);
		allColumns.add(dateColumn);
		allColumns.add(studyColumn);
		allColumns.add(typeColumn);
		if(addEntityType) allColumns.add(entityTypeColumn);
		allColumns.add(entityColumn);
		allColumns.add(fieldColumn);
		allColumns.add(oldValueColumn);
		allColumns.add(newValueColumn);
		if(SpiritProperties.getInstance().isChecked(PropertyKey.SYSTEM_ASKREASON)) {
			allColumns.add(reasonColumn);
		}
		setColumns(allColumns);
	}

}
