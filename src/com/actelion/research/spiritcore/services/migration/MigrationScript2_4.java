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

package com.actelion.research.spiritcore.services.migration;

import com.actelion.research.spiritcore.services.dao.SpiritRevisionEntity;
import com.actelion.research.spiritcore.util.SQLConverter;
import com.actelion.research.spiritcore.util.SQLConverter.SQLVendor;

public class MigrationScript2_4 extends MigrationScript {

	private String SCRIPT = ""
			+ "alter table spirit.revinfo modify (difference varchar2(" + SpiritRevisionEntity.MAX_DIFF_LENGTH + ") );\n"
			+ "alter table spirit.employee add failedconnections number(7);\n"
			+ "alter table spirit.employee_aud add failedconnections number(7);\n"
			;

	public MigrationScript2_4() {
		super("2.4");
	}

	@Override
	public String getMigrationSql(SQLVendor vendor) throws Exception {
		return SQLConverter.convertScript(SCRIPT, vendor);
	}

}
