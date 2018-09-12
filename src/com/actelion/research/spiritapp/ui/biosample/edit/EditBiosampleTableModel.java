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

package com.actelion.research.spiritapp.ui.biosample.edit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.actelion.research.spiritapp.ui.biosample.column.BiosampleElbColumn;
import com.actelion.research.spiritapp.ui.biosample.column.BiosampleQualityColumn;
import com.actelion.research.spiritapp.ui.biosample.column.CombinedColumn;
import com.actelion.research.spiritapp.ui.biosample.column.ContainerAmountColumn;
import com.actelion.research.spiritapp.ui.biosample.column.ContainerFullColumn;
import com.actelion.research.spiritapp.ui.biosample.column.ContainerIdColumn;
import com.actelion.research.spiritapp.ui.biosample.column.ContainerLocationPosColumn;
import com.actelion.research.spiritapp.ui.biosample.column.ContainerTypeColumn;
import com.actelion.research.spiritapp.ui.biosample.column.CreationColumn;
import com.actelion.research.spiritapp.ui.biosample.column.ExpiryDateColumn;
import com.actelion.research.spiritapp.ui.biosample.column.ParentBiosampleColumn;
import com.actelion.research.spiritapp.ui.biosample.column.ScannedPosColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StatusColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudyGroupColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudyIdColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudyParticipantIdColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudyPhaseColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudySubGroupColumn;
import com.actelion.research.spiritapp.ui.biosample.linker.LinkerColumnFactory;
import com.actelion.research.spiritapp.ui.biosample.linker.SampleIdColumn;
import com.actelion.research.spiritapp.ui.util.component.SpiritExtendTableModel;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.biosample.BarcodeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.exceltable.Column;

/**
 * Table used to edit a list of Biosamples
 *
 * @author Joel Freyss
 */
public class EditBiosampleTableModel extends SpiritExtendTableModel<Biosample> {

	private Biotype type;
	private Study study;

	private boolean compactView = false;
	private Set<String> readOnlyColumns;

	public EditBiosampleTableModel() {
		initColumns();
	}

	public void setCompactView(boolean compactView) {
		this.compactView = compactView;
	}

	public boolean isCompactView() {
		return compactView;
	}

	/**
	 * Should be called after setrows
	 * @param type
	 */
	public void setBiotype(Biotype type) {
		this.type = type;
		initColumns();
	}

	public void setStudy(Study study) {
		this.study = study;
	}

	public void initColumns() {
		List<Column<Biosample, ?>> columns = new ArrayList<>();

		//Analyze the data
		boolean hasParents2 = false;
		boolean hasScanPos = false;
		boolean hasAttachedSamples = false;
		Set<Biosample> myRows = new HashSet<>(getRows());
		for (Biosample b : getRows()) {
			if(b.getParent()!=null && b.getParent().getParent()!=null && !myRows.contains(b.getParent().getParent())) {
				hasParents2 = true;
			}
			if(b.getScannedPosition()!=null) {
				hasScanPos = true;
			}
			if(b.getAttachedStudy()!=null) {
				hasAttachedSamples = true;
			}
		}

		//RowNo
		columns.add(COLUMN_ROWNO);

		//Scan
		if(hasScanPos) {
			columns.add(new ScannedPosColumn());
		}

		//Container/Location Elements
		if(type!=null) {
			if(type.isAbstract()) {
				//No container
			} else if(type.isHideContainer()) {
				columns.add(new ContainerLocationPosColumn());
				if(type.getAmountUnit()!=null) columns.add(new ContainerAmountColumn(type));
			} else {
				if(SpiritProperties.getInstance().isChecked(PropertyKey.BIOSAMPLE_CONTAINERTYPES)) {
					if(type.getContainerType()==null) columns.add(new ContainerTypeColumn());
				}
				if(type.getContainerType()==null || type.getContainerType().getBarcodeType()!=BarcodeType.NOBARCODE) columns.add(new ContainerIdColumn());
				columns.add(new ContainerLocationPosColumn());
				if(type.getAmountUnit()!=null) columns.add(new ContainerAmountColumn(type));
			}
		} else if(SpiritProperties.getInstance().isAdvancedMode()) {
			//When sampling or other type, we condensed the container's infos in one column
			columns.add(new ContainerFullColumn());
		}

		//Study
		if(type!=null && (type.getCategory()==BiotypeCategory.LIVING || type.getCategory()==BiotypeCategory.SOLID || type.getCategory()==BiotypeCategory.LIQUID) || hasAttachedSamples) {
			columns.add(new StudyIdColumn());
			if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_FEATURE_STUDYDESIGN)) {
				columns.add(new StudyGroupColumn(this));
				columns.add(new StudySubGroupColumn());
				columns.add(new StudyPhaseColumn(this));
			}
		}

		//Parent
		if(SpiritProperties.getInstance().isAdvancedMode()) {
			if(type!=null && type.getCategory()==BiotypeCategory.PURIFIED && type.getParent()==null) {
				//Don't add parents columns
			} else if(!compactView) {
				//Top
				if(hasParents2) {
					columns.add(new StudyParticipantIdColumn());
				}
			}

			//Parent (must always be displayed except for living)
			if(type==null || type.getCategory()!=BiotypeCategory.LIVING) {
				columns.add(new ParentBiosampleColumn(this));
			}
		}


		//SampleId/SampleName
		Column<Biosample, String> sampleIdColumn = new SampleIdColumn(new BiosampleLinker(LinkerType.SAMPLEID, type), false, true);
		columns.add(sampleIdColumn);
		setTreeColumn(sampleIdColumn);

		if(type!=null && type.getSampleNameLabel()!=null && !compactView) {
			columns.add(LinkerColumnFactory.create(new BiosampleLinker(LinkerType.SAMPLENAME, type)));
		}

		//Metatadata
		if(type!=null && !compactView) {
			for (BiotypeMetadata t : type.getMetadata()) {
				columns.add(LinkerColumnFactory.create(new BiosampleLinker(t)));
			}

			//Comments
			columns.add(LinkerColumnFactory.create(new BiosampleLinker(LinkerType.COMMENTS, type)));

		} else {
			columns.add(new CombinedColumn());
		}
		if(SpiritProperties.getInstance().isAdvancedMode()) {
			columns.add(new StatusColumn());
		}

		//Remove empty columns that are not editable
		Set<String> readOnlyColumns = getReadOnlyColumns();
		if(readOnlyColumns!=null) {
			List<Column<Biosample, ?>> toRemove = new ArrayList<>();
			for (Column<Biosample, ?> column : columns) {
				if(readOnlyColumns.contains(column.getShortName()) || readOnlyColumns.contains(column.getName()))
					toRemove.add(column);
			}
			toRemove.removeAll(getNonEmptyColumns(columns));

			Set<Column<Biosample, ?>> alwaysVisible = getAlwaysVisibleColumns();
			if(alwaysVisible!=null) toRemove.removeAll(alwaysVisible);
			columns.removeAll(toRemove);
		}




		setColumns(columns);
	}

	@Override
	public List<Column<Biosample, ?>> getPossibleColumns() {
		if(!SpiritProperties.getInstance().isAdvancedMode()) return super.getPossibleColumns();
		List<Column<Biosample, ?>> res = new ArrayList<>();
		res.add(new BiosampleElbColumn());
		if(SpiritProperties.getInstance().isChecked(PropertyKey.BIOSAMPLE_CONTAINERTYPES)) {
			res.add(new ContainerTypeColumn());
		}
		res.add(new ContainerIdColumn());
		res.add(new ContainerLocationPosColumn());
		res.add(new ContainerAmountColumn(null));
		res.add(new StudyIdColumn());
		if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_FEATURE_STUDYDESIGN)) {
			res.add(new StudyGroupColumn(this));
			res.add(new StudySubGroupColumn());
			res.add(new StudyPhaseColumn(this));
		}
		if(SpiritProperties.getInstance().isAdvancedMode()) {
			res.add(new StudyParticipantIdColumn());
			res.add(new ParentBiosampleColumn(this));
		}
		res.add(new BiosampleQualityColumn());
		res.add(new ExpiryDateColumn());
		res.add(new CreationColumn(false));
		res.add(new StatusColumn());
		return res;
	}

	public Biotype getBiotype() {
		return type;
	}

	@Override
	public Biosample createRecord(Biosample model) {
		if(type==null) return null;
		Biosample res = new Biosample(type);
		res.setBiotype(model==null? null: model.getBiotype());
		res.setAttachedStudy(model==null? study: model.getAttachedStudy());
		res.setInheritedGroup(model==null? null: model.getInheritedGroup());
		res.setInheritedPhase(model==null? null: model.getInheritedPhase());
		res.setInheritedSubGroup(model==null? 0: model.getInheritedSubGroup());
		res.setContainerType(model==null? null: model.getContainerType());
		return res;
	}

	@Override
	public List<Biosample> getTreeChildren(Biosample row) {
		return new ArrayList<Biosample>(row.getChildren());
	}


	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {

		try {
			if(!super.isCellEditable(rowIndex, columnIndex)) return false;

			Column<Biosample, ?> column = getColumn(columnIndex);
			Biosample row = getRows().get(rowIndex);
			if(column instanceof SampleIdColumn && row.getBiotype()==null) return false;

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public Column<Biosample, ?> getTreeColumn() {
		if(!SpiritProperties.getInstance().isAdvancedMode()) return null;

		Column<Biosample, ?> col = super.getTreeColumn();
		if(col==null || !getColumns().contains(col)) {
			for (Column<Biosample, ?> c : getColumns()) {
				if(c instanceof SampleIdColumn) {
					col = c;
					super.setTreeColumn(col);
					break;
				}
			}
		}
		return col;
	}

	@Override
	public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
		super.setValueAt(newValue, rowIndex, columnIndex);
		Biosample b = getRow(rowIndex);
		DAOBiosample.computeFormula(Collections.singleton(b));
	}


	/**
	 * Returns the column shortName of columns that are read only
	 */
	@Override
	public Set<String> getReadOnlyColumns() {
		if(readOnlyColumns==null) {
			return DBAdapter.getInstance().getReadOnlyColumns();
		} else {
			return readOnlyColumns;
		}
	}

	/**
	 * Sets the read only columns
	 * @param readOnlyColumns
	 */
	public void setReadOnlyColumns(Set<String> readOnlyColumns) {
		this.readOnlyColumns = readOnlyColumns;
	}

	/**
	 * Sets the editable columns.
	 * Returns true if some columns are found in the model
	 * @param editableColumns
	 * @return
	 */
	public boolean setEditableColumns(Set<String> editableColumns) {
		boolean found = false;
		readOnlyColumns = new HashSet<>();
		for (Column<Biosample, ?> column : getAllColumns()) {
			if(editableColumns.contains(column.getShortName()) || editableColumns.contains(column.getName())) {
				found = true;
			} else {
				readOnlyColumns.add(column.getShortName());
			}
		}
		if(found) {
			initColumns();
			fireTableStructureChanged();
		}
		return found;
	}

}
