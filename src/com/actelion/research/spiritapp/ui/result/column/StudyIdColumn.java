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

package com.actelion.research.spiritapp.ui.result.column;

import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.util.ui.exceltable.Column;

public class StudyIdColumn extends Column<Result, String> {

	public StudyIdColumn() {
		super("StudyId", String.class);
	}

	@Override
	public float getSortingKey() {
		return 2.0f;
	}

	@Override
	public String getValue(Result row) {
		return row.getBiosample()==null || row.getBiosample().getInheritedStudy()==null? null: row.getBiosample().getInheritedStudy().getStudyId();
	}
	@Override
	public boolean isEditable(Result row) {return false;}


}
