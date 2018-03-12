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
import com.actelion.research.spiritapp.ui.biosample.column.StudyGroupColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudyIdColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudyParticipantIdColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudyPhaseColumn;
import com.actelion.research.spiritapp.ui.biosample.column.StudySubGroupColumn;
import com.actelion.research.spiritapp.ui.biosample.linker.LinkerColumnFactory;
import com.actelion.research.spiritapp.ui.biosample.linker.SampleIdColumn;
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
import com.actelion.research.util.ui.exceltable.ExtendTableModel;

/**
 * Table used to edit a list of Biosamples
 *
 * @author Joel Freyss
 */
public class EditBiosampleTableModel extends ExtendTableModel<Biosample> {

	private Biotype type;
	private Study study;

	private boolean compactView = false;

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

		List<Column<Biosample, ?>> defaultColumns = new ArrayList<>();

		//Generic Elements
		defaultColumns.add(COLUMN_ROWNO);

		if(hasScanPos) {
			defaultColumns.add(new ScannedPosColumn());
		}

		//Container Elements
		if(type!=null && type.isAbstract()) {
			//No container
		} else if(type!=null && type.isHideContainer()) {
			defaultColumns.add(new ContainerLocationPosColumn());
			if(type.getAmountUnit()!=null) defaultColumns.add(new ContainerAmountColumn(type));
		} else if(type!=null) {
			if(SpiritProperties.getInstance().isChecked(PropertyKey.BIOSAMPLE_CONTAINERTYPES)) {
				if(type.getContainerType()==null) defaultColumns.add(new ContainerTypeColumn());
			}
			if(type.getContainerType()==null || type.getContainerType().getBarcodeType()!=BarcodeType.NOBARCODE) defaultColumns.add(new ContainerIdColumn());
			defaultColumns.add(new ContainerLocationPosColumn());
			if(type.getAmountUnit()!=null) defaultColumns.add(new ContainerAmountColumn(type));
		} else {
			//When sampling or other type, we condensed the container's infos in one column
			defaultColumns.add(new ContainerFullColumn());
		}

		//Study Elements
		if(type!=null && (type.getCategory()==BiotypeCategory.LIVING || type.getCategory()==BiotypeCategory.SOLID || type.getCategory()==BiotypeCategory.LIQUID) || hasAttachedSamples) {
			defaultColumns.add(new StudyIdColumn());
			if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_FEATURE_STUDYDESIGN)) {
				defaultColumns.add(new StudyGroupColumn(this));
				defaultColumns.add(new StudySubGroupColumn());
				defaultColumns.add(new StudyPhaseColumn(this));
			}
		}

		if(SpiritProperties.getInstance().isAdvancedMode()) {
			if(type!=null && type.getCategory()==BiotypeCategory.PURIFIED && type.getParent()==null) {
				//Don't add parents columns
			} else if(!compactView) {
				//Top
				if(hasParents2) {
					defaultColumns.add(new StudyParticipantIdColumn());
				}
			}

			//Parent (must always be displayed except for living)
			if(type==null || type.getCategory()!=BiotypeCategory.LIVING) {
				defaultColumns.add(new ParentBiosampleColumn(this));
			}
		}


		Column<Biosample, String> sampleIdColumn = new SampleIdColumn(new BiosampleLinker(LinkerType.SAMPLEID, type), false, true);
		defaultColumns.add(sampleIdColumn);
		setTreeColumn(sampleIdColumn);

		if(type!=null && type.getSampleNameLabel()!=null && !compactView) {
			defaultColumns.add(LinkerColumnFactory.create(new BiosampleLinker(LinkerType.SAMPLENAME, type)));
		}

		if(type!=null && !compactView) {

			for (BiotypeMetadata t : type.getMetadata()) {
				defaultColumns.add(LinkerColumnFactory.create(new BiosampleLinker(t)));
			}

			//Comments
			defaultColumns.add(LinkerColumnFactory.create(new BiosampleLinker(LinkerType.COMMENTS, type)));

		} else {
			defaultColumns.add(new CombinedColumn());
		}
		setColumns(defaultColumns);
	}

	@Override
	public List<Column<Biosample, ?>> getPossibleColumns() {
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
		res.setInheritedSubGroup(model==null? null: model.getInheritedSubGroup());
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
			Column<Biosample, ?> column = getColumn(columnIndex);
			Biosample row = getRows().get(rowIndex);
			if(column instanceof SampleIdColumn && row.getBiotype()==null) return false;

			if(getReadOnlyColumns().contains(column)) return false;

			return getColumn(columnIndex).isEditable(rows.get(rowIndex));
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


}
