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

package com.actelion.research.spiritapp.ui.biosample.linker;


import javax.swing.JComponent;

import com.actelion.research.spiritapp.ui.util.component.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public abstract class AbstractLinkerColumn<T> extends Column<Biosample, T> {

	protected final BiosampleLinker linker;


	protected AbstractLinkerColumn(BiosampleLinker linker, Class<T> claz, int minWidth) {
		this(linker, claz, minWidth, 300);
	}
	protected AbstractLinkerColumn(BiosampleLinker linker, Class<T> claz, int minWidth, int maxWidth) {
		super(	"", claz, minWidth, maxWidth);
		assert linker!=null;
		this.linker = linker;
		setName(linker.getLabel());
	}

	@Override
	public boolean isHideable() {
		return linker.getBiotypeMetadata()!=null && linker.getBiotypeMetadata().isSecundary();
	}

	@Override
	public float getSortingKey() {
		//		float res = linker.getHierarchyBiotype()!=null? 4f: linker.getAggregatedMetadata()!=null? 5f: 6f;
		float res = linker.getHierarchyBiotype()!=null? 4f: 6f;
		BiotypeMetadata nextMetadata = linker.getAggregatedMetadata()!=null? linker.getAggregatedMetadata(): linker.getBiotypeMetadata();

		float subIndex;
		if(nextMetadata!=null) {
			subIndex = .6f + nextMetadata.getIndex()/1000f;
		} else if(linker.getType()==LinkerType.SAMPLEID) {
			subIndex = .4f;
		} else if(linker.getType()==LinkerType.SAMPLENAME) {
			subIndex = .5f;
		} else if(linker.getType()==LinkerType.COMMENTS) {
			subIndex = .9f;
		} else {
			System.err.println("getSortingKey: Invalid type "+linker.getType());
			subIndex = 0;
		}

		if(linker.getHierarchyBiotype()!=null) {
			int index = DAOBiotype.getBiotypes().indexOf(linker.getHierarchyBiotype());
			res+=index/10000.0;
			subIndex/=10000;
		}
		return res+subIndex;
	}

	@Override
	public boolean isEditable(Biosample row) {
		if(row==null) return true;
		if(linker.isLinked()) return false;

		switch(linker.getType()) {
		case SAMPLEID: return true;
		case SAMPLENAME: return row.getBiotype().getSampleNameLabel()!=null;
		case METADATA: return row.getBiotype().getMetadata().contains(linker.getBiotypeMetadata());
		case COMMENTS: return true;
		}
		return false;
	}

	public Biotype getBiotype() {
		return linker.getBiotypeForLabel();
	}

	public BiotypeMetadata getType() {
		return linker.getBiotypeMetadata();
	}

	public BiosampleLinker getLinker() {
		return linker;
	}

	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		if(linker.isLinked()) {
			comp.setBackground(LF.BGCOLOR_LINKED);
			comp.setForeground(LF.FGCOLOR_LINKED);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==null || !(obj instanceof AbstractLinkerColumn)) return false;
		return getLinker().equals(((AbstractLinkerColumn<?>) obj).getLinker());
	}
}