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

package com.actelion.research.spiritapp.ui.biosample.column;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.exceltable.Column;

public class BiotypeColumn extends Column<Biosample, Biotype> {
	public BiotypeColumn() {
		super("Sample\nBiotype", Biotype.class);
	}

	@Override
	public float getSortingKey() {return 6.00f;}

	@Override
	public Biotype getValue(Biosample row) {
		return row.getBiotype();
	}

	@Override
	public void setValue(Biosample row, Biotype value) {
		row.setBiotype(value);
	}

	@Override
	public void paste(Biosample row, String value) throws Exception {
		if(value==null || value.length()==0) setValue(row, null);
		else {
			Biotype biotype = DAOBiotype.getBiotype(value);
			if( biotype==null) throw new Exception("The biotype " +  value + " is invalid");
			setValue(row, biotype);
		}
	}

	@Override
	public boolean isEditable(Biosample row) {
		return row.getBiotype()==null || (row.getBiotype().getSampleNameLabel()==null && row.getMetadataAsString().length()==0);
	}

	@Override
	public String getToolTipText() {return "Biotype";}

	@Override
	public boolean isHideable() {
		return true;
	}
}
