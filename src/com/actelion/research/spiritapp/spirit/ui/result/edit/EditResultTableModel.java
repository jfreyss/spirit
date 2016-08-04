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

package com.actelion.research.spiritapp.spirit.ui.result.edit;

import java.util.Collections;

import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.util.ui.exceltable.ExcelTableModel;

public class EditResultTableModel extends ExcelTableModel<Result> {

	private Test test;
	private Study study;

	public EditResultTableModel() {
		super();
	}
		
	@Override
	public Result createRecord() {
		if(test==null) return null;
		Result result = new Result(test);
		return result;
	}
	
	public Test getTest() {
		return test;
	}
	
	public void setTest(Test test) {
		this.test = test;
	}

	public Study getStudy() {
		return study;
	}

	public void setStudy(Study study) {
		this.study = study;
	}
	
	@Override
	public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
		super.setValueAt(newValue, rowIndex, columnIndex);
		
		Result result = getRow(rowIndex);
		DAOResult.computeFormula(Collections.singleton(result));
		
		
	}
	
}
