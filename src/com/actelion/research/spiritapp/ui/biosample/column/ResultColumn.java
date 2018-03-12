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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class ResultColumn extends Column<Biosample, String> {

	public ResultColumn() {
		super("Data\nResults", String.class, 80);
	}
	@Override
	public float getSortingKey() {return 20.2f;}


	@Override
	public String getValue(Biosample row) {
		//TODO: For faster Load, load a bunch of data from the model and assign to the aux values.

		List<Result> results;
		try {

			results = DAOResult.queryResults(ResultQuery.createQueryForBiosampleId(row.getId()), null);
			if(results.size()==0) return null;
		} catch(Exception e) {
			e.printStackTrace();
			return e.toString();
		}

		Set<Test> tests = Result.getTests(results);
		if(tests.size()>1) {
			return tests.size()+" Tests / " + results.size()+" Results";
		} else if(tests.size()==1) {
			StringBuilder sb = new StringBuilder();
			for (Test test : tests) {
				List<Result> sub = new ArrayList<Result>();
				for (Result r : results) {
					if(r.getTest()==test) sub.add(r);
				}
				sb.append(test.getName()+": ");
				if(sub.size()==1 && sub.get(0).getOutputResultValues().size()==1) {
					String s1 = sub.get(0).getInputResultValuesAsString();
					String s2 = sub.get(0).getOutputResultValues().get(0).getValue();
					if(s1!=null && s1.length()>0) {
						sb.append(s1 + ": ");
					}
					sb.append(s2==null?"":s2);
				} else {
					sb.append(sub.size()+" Results");
				}
			}
			return sb.toString();
		} else {
			return null;
		}


	}

	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		comp.setForeground(Color.BLUE);
	}

	@Override
	public boolean isMultiline() {
		return true;
	}

	@Override
	public boolean isHideable() {
		return true;
	}

}
