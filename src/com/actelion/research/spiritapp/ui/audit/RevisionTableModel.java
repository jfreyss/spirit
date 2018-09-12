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

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.hibernate.envers.RevisionType;

import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.DateColumn;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;
import com.actelion.research.util.ui.exceltable.IntegerColumn;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;
import com.actelion.research.util.ui.exceltable.StringColumn;

public class RevisionTableModel extends ExtendTableModel<Revision> {

	private String filterByType = null;
	private Serializable filterById = null;
	private Integer filterBySid = null;

	private IntegerColumn<Revision> revColumn = new IntegerColumn<Revision>("RevId") {
		@Override
		public Integer getValue(Revision row) {
			return row.getRevId();
		}
	};

	private StringColumn<Revision> userColumn = new StringColumn<Revision>("User") {
		@Override
		public String getValue(Revision row) {
			return row.getUser();
		}
	};

	private DateColumn<Revision> dateColumn = new DateColumn<Revision>("Date") {
		@Override
		public Date getValue(Revision row) {
			return row.getDate();
		}
		@Override
		public void postProcess(AbstractExtendTable<Revision> table, Revision row, int rowNo, Object value, JComponent comp) {
			((JLabelNoRepaint)comp).setText(FormatterUtils.formatDateTime((Date)value));
		}
	};

	private StringColumn<Revision> studyColumn = new StringColumn<Revision>("Study") {
		@Override
		public String getValue(Revision row) {
			return row.getStudy()==null?"": row.getStudy().getStudyId();
		}
	};

	private StringColumn<Revision> reasonColumn = new StringColumn<Revision>("Reason") {
		@Override
		public String getValue(Revision row) {
			return row.getReason();
		}
	};

	/**
	 * Column used to show the change between 2 revisions
	 */
	private StringColumn<Revision> changeColumn = new StringColumn<Revision>("Change") {

		private JLabel htmlLabel = new JLabel();
		/**
		 * The value shows the change filtered by the entity
		 */
		@Override
		public String getValue(Revision row) {
			return row.getDifferenceFormatted(filterByType, filterById, filterBySid);
		}

		/**
		 * The tooltip shows the global change
		 */
		@Override
		public String getToolTipText(Revision row) {
			if(!"true".equals(System.getProperty("test"))) return null;
			return row.getDifference().toString();
		}
		@Override
		public JComponent getCellComponent(AbstractExtendTable<Revision> table, Revision row, int rowNo, Object value) {
			htmlLabel.setText((String) value);
			return htmlLabel;
		}

		@Override
		public void postProcess(AbstractExtendTable<Revision> table, Revision rev, int rowNo, Object value, JComponent comp) {
			super.postProcess(table, rev, rowNo, value, comp);
			htmlLabel.setOpaque(true);
			if(rev.getRevisionType()==RevisionType.ADD) {
				comp.setForeground(new Color(0, 80, 0));
			} else if(rev.getRevisionType()==RevisionType.DEL) {
				comp.setForeground(new Color(170, 0, 0));
			} else {
				comp.setForeground(new Color(150, 100, 0));
			}
		}

		@Override
		public boolean isMultiline() {return true;}
	};

	public RevisionTableModel() {
		initColumns();
	}

	public void initColumns() {
		List<Column<Revision, ?>> allColumns = new ArrayList<>();
		allColumns.add(COLUMN_ROWNO);
		allColumns.add(revColumn.setHideable(true));
		allColumns.add(userColumn);
		allColumns.add(dateColumn);
		allColumns.add(studyColumn);
		allColumns.add(changeColumn);
		if(SpiritProperties.getInstance().isChecked(PropertyKey.SYSTEM_ASKREASON)) {
			allColumns.add(reasonColumn);
		}
		setColumns(allColumns);
		showAllHideable(true);
	}

	/**
	 * Sets filters to show only changes related to the entityType / entityId
	 * (sets to null to show all changes)
	 * @param filterByType
	 * @param filterById
	 */
	public void setFilters(String filterByType, Serializable filterById, Integer filterBySid) {
		this.filterByType = filterByType;
		this.filterById = filterById;
		this.filterBySid = filterBySid;
	}
}
