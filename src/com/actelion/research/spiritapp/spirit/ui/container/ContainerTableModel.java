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

package com.actelion.research.spiritapp.spirit.ui.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritapp.spirit.ui.container.ContainerLabel.ContainerDisplayMode;
import com.actelion.research.spiritapp.spirit.ui.study.GroupLabel;
import com.actelion.research.spiritapp.spirit.ui.study.PhaseLabel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.LocationFormat;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;

public class ContainerTableModel extends ExtendTableModel<Container> {

	public static enum ContainerTableModelType {
		PRINT, CHECKIN, EXPANDED
	}

	private final ContainerTableModelType type;

	public static class ScannedPosColumn extends Column<Container, String> {
		public ScannedPosColumn() {
			super("Pos", String.class, 50);
		}

		@Override
		public String getValue(Container row) {
			return row == null ? null : row.getScannedPosition();
		}

		@Override
		public boolean isEditable(Container row) {
			return false;
		}

		@Override
		public String getToolTipText() {
			return "Scanned Position";
		}
	}

	public static class StudyIdColumn extends Column<Container, String> {
		public StudyIdColumn() {
			super("StudyId", String.class);
		}

		@Override
		public String getValue(Container row) {
			return row.getStudy() == null ? null : row.getStudy().getStudyId();
		}

		@Override
		public boolean isEditable(Container row) {
			return false;
		}

		private GroupLabel groupLabel = new GroupLabel();

		@Override
		public JComponent getCellComponent(AbstractExtendTable<Container> table, Container row, int rowNo, Object value) {
			groupLabel.setText(value == null ? "" : value.toString(), row.getFirstGroup());
			return groupLabel;
		}
	}

	public static class StudyGroupColumn extends Column<Container, String> {
		private GroupLabel groupLabel = new GroupLabel();

		public StudyGroupColumn() {
			super("Group", String.class);
		}

		@Override
		public String getValue(Container row) {
			StringBuilder sb = new StringBuilder();
			for (Group gr : row.getGroups()) {
				sb.append((sb.length() > 0 ? ", " : "") + gr.getBlindedName(Spirit.getUsername()));
			}
			return sb.toString();
		}

		@Override
		public boolean isEditable(Container row) {
			return false;
		}

		@Override
		public JComponent getCellComponent(AbstractExtendTable<Container> table, Container row, int rowNo, Object value) {
			groupLabel.setText(value == null ? "" : value.toString(), row.getFirstGroup());
			return groupLabel;
		}
	}

	public static class StudyPhaseColumn extends Column<Container, Phase> {
		public StudyPhaseColumn() {
			super("Phase", Phase.class);
		}

		@Override
		public Phase getValue(Container row) {
			return row.getPhase();
		}

		@Override
		public boolean isEditable(Container row) {
			return false;
		}

		private static PhaseLabel phaseLabel = new PhaseLabel();

		@Override
		public JComponent getCellComponent(AbstractExtendTable<Container> table, Container row, int rowNo, Object value) {
			phaseLabel.setPhase(row.getPhase(), row.getFirstGroup());
			return phaseLabel;
		}
	}

	public static class ContainerIdColumn extends Column<Container, String> {
		private static ContainerLabel containerLabel = new ContainerLabel(ContainerDisplayMode.CONTAINERID_OR_BIOSAMPLEID);

		public ContainerIdColumn() {
			super("ContainerId", String.class, 55);
		}

		@Override
		public String getValue(Container row) {
			return row.getContainerOrBiosampleId();
		}

		@Override
		public JComponent getCellComponent(AbstractExtendTable<Container> table, Container row, int rowNo, Object value) {
			containerLabel.setContainer(row);
			return containerLabel;
		}
		@Override
		public boolean shouldMerge(Container r1, Container r2) {
			return false;
		}
	}

	public static class ContainerFullColumn extends Column<Container, String> {
		private static ContainerLabel containerLabel = new ContainerLabel(ContainerDisplayMode.FULL);

		public ContainerFullColumn() {
			super("Container", String.class, 55);
		}

		@Override
		public String getValue(Container row) {
			return row == null ? null : row.getContainerOrBiosampleId();
		}

		@Override
		public JComponent getCellComponent(AbstractExtendTable<Container> table, Container row, int rowNo, Object value) {
			containerLabel.setContainer(row);
			return containerLabel;
		}
		@Override
		public boolean shouldMerge(Container r1, Container r2) {
			return false;
		}

	}

	public static class LocationColumn extends Column<Container, String> {
		private static ContainerLabel containerLabel = new ContainerLabel(ContainerDisplayMode.FULL);

		public LocationColumn() {
			super("Location", String.class, 95);
		}

		@Override
		public String getValue(Container row) {
			return row == null || row.getFirstBiosample() == null ? null : row.getFirstBiosample().getLocationString(LocationFormat.MEDIUM_POS, Spirit.getUser());
		}

		@Override
		public JComponent getCellComponent(AbstractExtendTable<Container> table, Container row, int rowNo, Object value) {
			containerLabel.setContainer(row);
			return containerLabel;
		}
	}

	public static class TopParentColumn extends Column<Container, String> {
		public TopParentColumn() {
			super("TopSampleId", String.class);
		}

		@Override
		public String getValue(Container row) {
			return row.getTopParents();
		}

		@Override
		public boolean isEditable(Container row) {
			return false;
		}

		private static SampleIdLabel sampleIdLabel = new SampleIdLabel();

		@Override
		public JComponent getCellComponent(AbstractExtendTable<Container> table, Container row, int rowNo, Object value) {
			Biosample top = Biosample.getTopParentInSameStudy(row.getBiosamples());
			if (top != null) {
				sampleIdLabel.setBiosample(top);
				return sampleIdLabel;
			}
			return super.getCellComponent(table, row, rowNo, value);
		}
	}

	public static class PrintLabelColumn extends Column<Container, String> {
		public PrintLabelColumn() {
			super("Content", String.class, 80, 320);
		}

		@Override
		public String getValue(Container row) {
			String sLabel = row.getPrintStudyLabel(Spirit.getUsername());
			return (sLabel.length() > 0 ? sLabel + (sLabel.endsWith("\n") ? "" : "\n") : "") + "<B>" + row.getPrintMetadataLabel();
		}

		@Override
		public boolean isEditable(Container row) {
			return false;
		}

		@Override
		public void postProcess(AbstractExtendTable<Container> table, Container row, int rowNo, Object value, JComponent comp) {
			comp.setFont(FastFont.MEDIUM);
			if (row != null) {
				Group g = row.getFirstGroup();
				if (g != null)
					comp.setBackground(UIUtils.getDilutedColor(g.getBlindedColor(Spirit.getUsername()), comp.getBackground()));
			}

		}

		@Override
		public boolean isMultiline() {
			return true;
		}

		@Override
		public boolean isAutoWrap() {
			return false;
		}
	}

	public static class BlocNoColumn extends Column<Container, String> {
		public BlocNoColumn() {
			super("BlocNo", String.class, 100, 500);
		}

		@Override
		public String getValue(Container row) {
			return row.getBlocDescription();
		}
	}

	public static class SamplesColumn extends Column<Container, String> {
		public SamplesColumn() {
			super("Samples", String.class, 100, 500);
		}

		@Override
		public String getValue(Container row) {
			StringBuilder sb = new StringBuilder();
			for (Biosample b : row.getBiosamples()) {
				sb.append(b.getSampleIdName()+"\n");
			} 
			return sb.toString();
		}
		
		@Override
		public boolean isMultiline() {
			return true;
		}

		@Override
		public boolean isAutoWrap() {
			return false;
		}
	}

	public static class StainingColumn extends LinkColumn {
		public StainingColumn() {
			super("Staining", false, "Staining");
		}
	}

	public static class LinkColumn extends Column<Container, String> {

		private final boolean fromTopParent;
		private final String metadata;

		public LinkColumn(final String title, boolean fromTopParent, final String metadata) {
			super(title, String.class, 20, 100);
			this.fromTopParent = fromTopParent;
			this.metadata = metadata;
		}

		public boolean isMultiLine() {
			return metadata == null;
		}

		@Override
		public String getValue(Container row) {

			// Go the the parents...
			Collection<Biosample> biosamples;
			if (fromTopParent) {
				Biosample top = row.getTopParent();
				biosamples = top == null ? new ArrayList<Biosample>() : Collections.singletonList(row.getTopParent());
			} else {
				biosamples = row.getBiosamples();
			}

			if (biosamples.size() == 0) {
				return "";
			} else {
				Set<String> set = Biosample.getMetadata(metadata, biosamples);
				if (set.size() == 0) {
					return null;
				} else if (set.size() == 1) {
					return set.iterator().next();
				} else {
					return "N/A";
				}
			}
		}

	}

	public ContainerTableModel(ContainerTableModelType type) {
		this.type = type;
		initColumns();
	}

	public void initColumns() {
		List<Column<Container, ?>> cols = new ArrayList<>();
		cols.add(COLUMN_ROWNO);
		cols.add(new ScannedPosColumn());

		if (type == ContainerTableModelType.EXPANDED) {
			cols.add(new StudyIdColumn());
			cols.add(new StudyGroupColumn());
			cols.add(new TopParentColumn());
			cols.add(new LocationColumn());
			cols.add(new PrintLabelColumn());
			cols.add(new SamplesColumn());
		} else if (type == ContainerTableModelType.CHECKIN) {
			cols.add(new LocationColumn());
			cols.add(new PrintLabelColumn());
		} else if (type == ContainerTableModelType.PRINT) {
			cols.add(new ContainerFullColumn());
			cols.add(new PrintLabelColumn());
			cols.add(new SamplesColumn());
		}
		setColumns(cols);
	}

	@Override
	public List<Column<Container, ?>> getPossibleColumns() {
		List<Column<Container, ?>> cols = new ArrayList<Column<Container, ?>>();
		cols.add(new StudyIdColumn());
		cols.add(new StudyGroupColumn());
		cols.add(new TopParentColumn());
		cols.add(new StudyPhaseColumn());
		cols.add(new SamplesColumn());
		cols.add(new BlocNoColumn());
		cols.add(new StainingColumn());
		cols.add(new LocationColumn());
		return cols;

	}

}
