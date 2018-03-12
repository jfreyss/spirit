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

import com.actelion.research.spiritcore.util.SQLConverter;
import com.actelion.research.spiritcore.util.SQLConverter.SQLVendor;

public class MigrationScript2_3 extends MigrationScript {

	private String SCRIPT = ""
			+ "alter table spirit.employee_group add (disabled number(3), upduser varchar2(20), upddate time);\n"

			+ "create table spirit.employee_group_aud (group_id number(9,0), REV number(10,0), REVTYPE number(3), group_name varchar(255), group_parent number(9,0), disabled number(3), upduser varchar2(20), upddate time);\n"
			+ "alter table spirit.employee_group_aud add constraint employee_group_aud_pk primary key (group_id, REV);\n"
			+ "alter table spirit.employee_group_aud add constraint employee_group_aud_fk1 foreign key (REV) references spirit.revinfo (REV);\n"

			+ "create table spirit.employee_aud (employee_id number(9,0) not null, REV number(10,0), REVTYPE number(3), disabled number(3), password varchar(64), roles varchar(255), upd_date date, upd_user varchar(20), user_name varchar(20), manager_id integer, primary key (employee_id, REV));\n"
			+ "alter table spirit.employee_aud add constraint employee_group_aud_pk primary key (employee_id, REV);\n"
			+ "alter table spirit.employee_aud add constraint employee_aud_fk1 foreign key (REV) references spirit.revinfo (REV);\n"

			+ "create table spirit.employee_group_link_aud (employee_id number(9,0), REV number(10,0), REVTYPE number(3), group_id number(9,0) not null, primary key (REV, employee_id, group_id));\n"
			+ "alter table spirit.employee_group_link_aud add constraint employee_group_link_aud_fk1 foreign key (REV) references spirit.revinfo (REV);\n"
			+ "alter table spirit.revinfo add study_id number(9,0);\n"
			+ "alter table spirit.revinfo add reason varchar2(256);\n"
			+ "alter table spirit.revinfo add difference varchar2(256);\n";

	public MigrationScript2_3() {
		super("2.3.0");
	}

	@Override
	public String getMigrationSql(SQLVendor vendor) throws Exception {
		return SQLConverter.convertScript(SCRIPT, vendor);
	}

}
