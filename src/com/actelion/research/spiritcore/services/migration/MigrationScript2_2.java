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

public class MigrationScript2_2 extends MigrationScript {

	private String SCRIPT =
			"alter table spirit.assay_result add study_id number(19);\n" +
					"alter table spirit.assay_result_aud add study_id number(19);\n" +
					"alter table spirit.assay_result add constraint result_study_fk foreign key (study_id) references spirit.study (id);\n" +
					"update spirit.assay_result set study_id = (select study_id from spirit.biosample where biosample.id = biosample_id) where study_id is null;\n" +
					"update spirit.assay_result_aud set study_id = (select max(study_id) from spirit.biosample_aud where biosample_aud.id = biosample_id and biosample_aud.rev <=assay_result_aud.rev) where study_id is null;\n" +
					"create index biosample_study_idx on spirit.biosample_aud (study_id);\n" +
					"create index result_study_idx on spirit.assay_result_aud (study_id);\n" +
					"alter table spirit.assay add disabled number(2);\n" +
					"alter table spirit.assay_aud add disabled number(2);\n" +
					"alter table spirit.biolocation ADD (flag varchar2(20 char));\n" +
					"alter table spirit.biolocation_aud ADD (flag varchar2(20 char));\n" +
					"alter table spirit.study ADD (studytype varchar2(32 char));\n" +
					"alter table spirit.study_aud ADD (studytype varchar2(32 char));\n"
					;
	public MigrationScript2_2() {
		super("2.2.0");
	}

	@Override
	public String getMigrationSql(SQLVendor vendor) throws Exception {
		return SQLConverter.convertScript(SCRIPT, vendor);
	}

}
