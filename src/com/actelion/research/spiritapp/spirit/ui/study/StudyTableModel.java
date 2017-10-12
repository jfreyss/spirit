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

package com.actelion.research.spiritapp.spirit.ui.study;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.lf.CreationLabel;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Triple;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class StudyTableModel extends ExtendTableModel<Study> {

	private static final Date now = JPAUtil.getCurrentDateFromDatabase();

	public final Column<Study, String> COLUMN_STUDYID = new Column<Study, String>("Study\nId", String.class, 50, 80) {
		@Override
		public String getValue(Study row) {
			return row.getStudyId();
		}

		@Override
		public void postProcess(AbstractExtendTable<Study> table, Study row, int rowNo, Object value, JComponent comp) {
			comp.setFont(FastFont.BOLD);
		}
	};

	public final Column<Study, String> COLUMN_STATUS = new Column<Study, String>("Study\nState", String.class, 50, 80) {
		@Override
		public String getValue(Study row) {
			return row.getState();
		}

	};

	public final Column<Study, String> COLUMN_IVV = new Column<Study, String>("Study\nInternalId", String.class, 50, 80) {
		@Override
		public String getValue(Study row) {
			return row.getLocalId();
		}
	};

	public final Column<Study, String> COLUMN_TYPE = new Column<Study, String>("Study\nType", String.class, 50, 80) {
		@Override
		public String getValue(Study row) {
			return row.getType();
		}
	};

	public final Column<Study, String> COLUMN_TITLE = new Column<Study, String>("Study\nTitle", String.class, 100, 1000) {
		@Override
		public String getValue(Study row) {
			return row.getTitle();
		}
		@Override
		public boolean isAutoWrap() {return true;}
	};

	public final class MetadataColumn extends Column<Study, String> {
		private String metaKey;
		public MetadataColumn(String metaKey) {
			super("Metadata\n" + SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_NAME, metaKey), String.class, 40, 100);
			this.metaKey = metaKey;
		}
		@Override
		public String getValue(Study row) {
			if(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_TYPES).length>0 && !MiscUtils.contains(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_TYPES), row.getType())) {
				return null;
			}
			return row.getMetadataMap().get(metaKey);
		}

		//		@Override
		//		public boolean isHideable() {
		//			return true;
		//		}
	}

	public final Column<Study, String> COLUMN_RESPONSIBLES = new Column<Study, String>("Study\nAdmins", String.class, 40, 120) {
		@Override
		public String getValue(Study row) {
			Set<String> resps = row.getAdminUsersAsSet();
			StringBuilder sb = new StringBuilder();
			for (String s : resps) {
				if(sb.length()>0) sb.append(", ");
				sb.append(s);
			}
			return sb.toString();
		}

		@Override
		public boolean isAutoWrap() {
			return false;
		}
		@Override
		public boolean isHideable() {return true;}

	};

	public final Column<Study, Date> COLUMN_STARTING_DATE = new Column<Study, Date>("Study\nStart", Date.class, 40) {
		@Override
		public Date getValue(Study row) {
			return row.getFirstDate();
		}

		@Override
		public void postProcess(AbstractExtendTable<Study> table, Study row, int rowNo, Object value, JComponent comp) {
			Date startDate = row.getFirstDate();
			Date endDate = row.getLastDate();
			if(startDate==null) {
				return;
			} else if(!startDate.before(now)) {
				comp.setForeground(new Color(0x77, 0x33, 0));
			} else if(!now.before(endDate)) {
				comp.setForeground(new Color(0x88, 0, 0));
			} else{
				comp.setForeground(new Color(0x0, 0x88, 0));
			}
		}
		@Override
		public boolean isHideable() {return true;}
	};

	public static final Column<Study, Date> COLUMN_END_DATE = new Column<Study, Date>("Study\nEnd", Date.class, 40) {
		@Override
		public Date getValue(Study row) {
			return row.getLastDate();
		}
		@Override
		public void postProcess(AbstractExtendTable<Study> table, Study row, int rowNo, Object value, JComponent comp) {
			Date startDate = row.getFirstDate();
			Date endDate = row.getLastDate();
			if(startDate==null) {
				return;
			} else if(!startDate.before(now)) {
				comp.setForeground(UIUtils.getColor(60, 60, 0));
			} else if(!now.before(endDate)) {
				comp.setForeground(UIUtils.getColor(0, 0, 0));
			} else{
				comp.setForeground(UIUtils.getColor(0, 100, 0));
			}
		}
		@Override
		public boolean isHideable() {return true;}
	};

	public final Column<Study, String> COLUMN_DEPT = new Column<Study, String>("Study\nGroup", String.class) {
		@Override
		public String getValue(Study row) {
			return row.getEmployeeGroupsAsString();
		}
		@Override
		public boolean isHideable() {return true;}
	};

	public final Column<Study, String> COLUMN_BIOSAMPLES = new Column<Study, String>("Data\nBiosamples", String.class) {
		@Override
		public float getSortingKey() {return 10.1f;}
		@Override
		public String getValue(Study study) {
			StringBuilder sb = new StringBuilder();
			//Count Results
			Map<Study, Map<Biotype, Triple<Integer, String, Date>>> countRes = DAOStudy.countSamplesByStudyBiotype(Collections.singletonList(study));
			Map<Biotype, Triple<Integer, String, Date>> m2 = countRes.get(study);
			if(m2!=null && m2.size()>0) {
				for (Biotype t: m2.keySet()) {
					sb.append(t.getName() + " (" + m2.get(t).getFirst() + ")\n");
				}
			}
			return sb.toString();
		}
		@Override
		public void postProcess(AbstractExtendTable<Study> table, Study row, int rowNo, Object value, JComponent comp) {
			((JLabelNoRepaint) comp).setForeground(Color.BLUE);
			((JLabelNoRepaint) comp).setFont(FastFont.SMALL);
		}

		@Override
		public boolean isHideable() {return true;}

		@Override
		public boolean isMultiline() {return true;}
	};


	public final Column<Study, String> COLUMN_RESULTS = new Column<Study, String>("Data\nResults", String.class) {
		@Override
		public float getSortingKey() {return 10.2f;}
		@Override
		public String getValue(Study study) {
			StringBuilder sb = new StringBuilder();
			//Count Results
			Map<Study, Map<Test, Triple<Integer, String, Date>>> countRes = DAOStudy.countResultsByStudyTest(Collections.singletonList(study));
			Map<Test, Triple<Integer, String, Date>> m2 = countRes.get(study);
			if(m2!=null && m2.size()>0) {
				for (Test t: m2.keySet()) {
					sb.append(t.getName() + " (" + m2.get(t).getFirst() + ")\n");
				}
			}
			return sb.toString();
		}
		@Override
		public void postProcess(AbstractExtendTable<Study> table, Study row, int rowNo, Object value, JComponent comp) {
			((JLabelNoRepaint) comp).setForeground(Color.BLUE);
			((JLabelNoRepaint) comp).setFont(FastFont.SMALL);
		}

		@Override
		public boolean isHideable() {return true;}

		@Override
		public boolean isMultiline() {return true;}
	};

	public class StudyCreationColumn extends Column<Study, String> {

		private final boolean creation;

		public StudyCreationColumn(boolean creation) {
			super(creation?"Study\nOwner": "Study\nLastUpdate", String.class);
			this.creation = creation;
		}

		@Override
		public float getSortingKey() {return 10.1f;}

		@Override
		public String getValue(Study row) {
			return creation? row.getCreUser() + "\t" + FormatterUtils.formatDate(row.getCreDate()):
				row.getUpdUser()  + "\t" + FormatterUtils.formatDate(row.getUpdDate());
		}

		@Override
		public boolean isEditable(Study row) {return false;}

		private CreationLabel ownerLabel = new CreationLabel();

		@Override
		public JComponent getCellComponent(AbstractExtendTable<Study> table, Study row, int rowNo, Object value) {
			ownerLabel.setValue(creation? row.getCreUser(): row.getUpdUser(), null, creation? row.getCreDate(): row.getUpdDate(),
					SpiritRights.canAdmin(row, SpiritFrame.getUser())? RightLevel.ADMIN:
						SpiritRights.canExpert(row, SpiritFrame.getUser())? RightLevel.WRITE:
							RightLevel.READ);
			return ownerLabel;
		}

		@Override
		public boolean isHideable() {
			return !creation;
		}

		@Override
		public void populateHeaderPopup(final AbstractExtendTable<Study> table, JPopupMenu popupMenu) {
			popupMenu.add(new JSeparator());
			popupMenu.add(new JCustomLabel("Sort", Font.BOLD));

			popupMenu.add(new AbstractAction("Sort by " + (creation?"CreUser": "UpdUser")) {
				@Override
				public void actionPerformed(ActionEvent e) {
					table.sortBy(StudyCreationColumn.this, 1, new Comparator<Study>() {
						@Override
						public int compare(Study o1, Study o2) {
							return CompareUtils.compare(creation? o1.getCreUser(): o1.getUpdUser(), creation? o2.getCreUser(): o2.getUpdUser());
						}
					});
				}
			});
			popupMenu.add(new AbstractAction("Sort by " + (creation?"CreDate": "UpdDate")) {
				@Override
				public void actionPerformed(ActionEvent e) {
					table.sortBy(StudyCreationColumn.this, 1, new Comparator<Study>() {
						@Override
						public int compare(Study o1, Study o2) {
							return CompareUtils.compare(creation? o1.getCreDate(): o1.getUpdDate(), creation? o2.getCreDate(): o2.getUpdDate());
						}
					});
				}
			});
		}

		@Override
		public void postProcess(AbstractExtendTable<Study> table, Study row, int rowNo, Object value, JComponent comp) {
			comp.setBackground(COLOR_NONEDIT);
		}
	}

	public StudyTableModel() {
		initColumns();
	}

	public void initColumns() {
		List<Column<Study, ?>> defaultColumns = new ArrayList<>();
		defaultColumns.add(COLUMN_ROWNO);
		defaultColumns.add(COLUMN_STATUS);
		if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_AUTOGENERATEID)) {
			defaultColumns.add(COLUMN_IVV);
		}
		defaultColumns.add(COLUMN_STUDYID);
		if(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_TYPES).length>0) {
			defaultColumns.add(COLUMN_TYPE);
		}
		defaultColumns.add(COLUMN_TITLE);


		defaultColumns.add(COLUMN_RESPONSIBLES);
		defaultColumns.add(COLUMN_DEPT);
		if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_ADVANCEDMODE)) {
			defaultColumns.add(COLUMN_STARTING_DATE);
			defaultColumns.add(COLUMN_END_DATE);
		}
		for (String metaKey : SpiritProperties.getInstance().getValues(PropertyKey.STUDY_METADATA)) {
			MetadataColumn column = new MetadataColumn(metaKey);
			//			showHideable(column, SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_METADATA_REQUIRED, metaKey));
			defaultColumns.add(column);
		}
		defaultColumns.add(COLUMN_BIOSAMPLES);
		if(SpiritProperties.getInstance().isChecked(PropertyKey.TAB_RESULT)) {
			defaultColumns.add(COLUMN_RESULTS);
		}
		defaultColumns.add(new StudyCreationColumn(true));
		defaultColumns.add(new StudyCreationColumn(false));
		setColumns(defaultColumns);
	}


}
