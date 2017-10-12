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

package com.actelion.research.spiritapp.slidecare.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.biosample.column.StudyGroupColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.column.StudySubGroupColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.column.StudyParticipantIdColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.linker.LinkerColumnFactory;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.util.ui.exceltable.Column;

public class AnimalsTable extends BiosampleTable {

	public AnimalsTable() {
		super();
	}


	@Override
	public void setRows(List<Biosample> biosamples) {
		//If the data is empty, set an empty table (to avoid too many useless events)
		if(biosamples==null || biosamples.size()==0) {
			getModel().clear();
			getModel().fireTableStructureChanged();
			return;
		}


		Collection<Biotype> biotypes = Biosample.getBiotypes(biosamples);
		Biotype biotype = biotypes.size()==1? biotypes.iterator().next(): null;


		boolean hasAnimal = false;
		boolean hasSubgroup = false;

		for (Biosample biosample : biosamples) {
			Biosample top = biosample.getTopParentInSameStudy();
			if(top!=biosample && !biosample.getSampleId().startsWith(top.getSampleId())) hasAnimal = true;
			if(biosample.getInheritedSubGroup()>0) hasSubgroup = true;
		}


		List<Column<Biosample, ?>> columns = new ArrayList<>();
		columns.add(getModel().COLUMN_ROWNO);
		columns.add(new StudyGroupColumn(null));
		if(hasSubgroup) columns.add(new StudySubGroupColumn());

		if(hasAnimal) columns.add(new StudyParticipantIdColumn());


		Column<Biosample, ?> sampleIdColumn = LinkerColumnFactory.create(new BiosampleLinker(LinkerType.SAMPLEID, biotype));
		columns.add(sampleIdColumn);
		getModel().setColumns(columns);
		getModel().setTreeColumn(sampleIdColumn);


		getModel().setRows(biosamples);

		resetPreferredColumnWidth();
	}

}
