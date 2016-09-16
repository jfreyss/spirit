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

package com.actelion.research.spiritcore.services.migration;

import com.actelion.research.spiritcore.util.SQLConverter;
import com.actelion.research.spiritcore.util.SQLConverter.SQLVendor;

public class MigrationScript2_0 extends MigrationScript {

	private String SCRIPT = 
			"alter table spirit.assay_result_value add document_id number(19);\n" 
			+ "alter table spirit.assay_result_value_aud add document_id number(19);\n"
			+ "alter table spirit.assay_result_value add constraint assay_result_fk foreign key (document_id) references spirit.document (ID);\n";
	
	
	public MigrationScript2_0() {
		super("2.0");
	}
	
	@Override
	public String getMigrationSql(SQLVendor vendor) throws Exception {
		return SQLConverter.convertScript(SCRIPT, vendor);
	}

	
}
