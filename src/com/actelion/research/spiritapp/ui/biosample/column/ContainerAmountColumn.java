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

import javax.swing.JComponent;

import com.actelion.research.spiritapp.ui.util.component.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class ContainerAmountColumn extends Column<Biosample, Double> {
	public ContainerAmountColumn(Biotype biotype) {
		super("Container\n" + (biotype==null || biotype.getAmountUnit()==null?"Amount":biotype.getAmountUnit().getNameUnit()) , Double.class, 40, 60);
	}
	
	@Override
	public Double getValue(Biosample row) {
		if(row==null) return null;
		return row.getAmount();
	}
	
	@Override
	public float getSortingKey() {
		return 2.8f;
	}
	
	@Override
	public void setValue(Biosample row, Double value) {
		row.setAmount(value);
	}
	
	@Override
	public boolean isEditable(Biosample row) {
		return row!=null && row.getBiotype()!=null && row.getBiotype().getAmountUnit()!=null;
	}
	
	@Override
	public String getToolTipText() {return "Amount";}		
	
	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		comp.setBackground(LF.BGCOLOR_LOCATION);
		if(row!=null && row.getAmount()!=null && row.getBiotype()!=null && row.getBiotype().getAmountUnit()!=null) {
			((JLabelNoRepaint) comp).setText(row.getAmount()+row.getBiotype().getAmountUnit().getUnit());
		}
	}
}