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

public class MigrationScript2_1 extends MigrationScript {

	private String SCRIPT = 
			"alter table spirit.biosample add metadata2 varchar2(4000);\n" 
			+ "alter table spirit.biosample_aud add metadata2 varchar2(4000);\n" 
			+ "alter table spirit.biosample add lastaction varchar2(256);\n" 
			+ "alter table spirit.biosample_aud add lastaction varchar2(256);\n"
			+ "alter table spirit.biosample add endphase_id number(19);\n" 
			+ "alter table spirit.biosample_aud add endphase_id number(19);\n"
			+ "alter table spirit.biosample add constraint biosample_endphase_fk foreign key (endphase_id) references spirit.study_phase (id);\n"
			
			+ "alter table spirit.biotype add nameUnique number(1);\n" 
			+ "alter table spirit.biotype_aud add nameUnique number(1);\n"
	
	
			+ "update biosample set endphase_id = (select phase_id from biosample_action where type = 'Status' and biosample_action.biosample_id = biosample.id and id = (select max(id)  from biosample_action where type = 'Status' and biosample_action.biosample_id = biosample.id ))where (select phase_id from biosample_action where type = 'Status' and biosample_action.biosample_id = biosample.id and id = (select max(id)  from biosample_action where type = 'Status' and biosample_action.biosample_id = biosample.id )) is not null;\n"
			+ "update biosample_aud set ENDPHASE_ID =(select max(phase_id) from biosample_action_aud where type = 'Status' and biosample_action_aud.biosample_id = biosample_aud.id  and rev = (select max(rev) from biosample_action_aud where type = 'Status' and biosample_action_aud.biosample_id = biosample_aud.id  and biosample_action_aud.rev <= biosample_aud.rev)) where biosample_aud.ATTACHEDSTUDY_ID is not null and ENDPHASE_ID is null;\n"			
			+ "update biosample_aud set endphase_id = (select phase_id from biosample_action_aud where type = 'Status' and biosample_action_aud.biosample_id = biosample_aud.id and id = (select max(id)  from biosample_action_aud where type = 'Status' and biosample_action_aud.biosample_id = biosample_aud.id ) and biosample_action_aud.rev = biosample_aud.rev)where (select phase_id from biosample_action_aud where type = 'Status' and biosample_action_aud.biosample_id = biosample_aud.id and id = (select max(id)  from biosample_action_aud where type = 'Status' and biosample_action_aud.biosample_id = biosample_aud.id ) and biosample_action_aud.rev = biosample_aud.rev) is not null;\n"
			


//update biosample
//set lastaction = 
//(select distinct first_value('Treatment;' 
//|| (select max(name) from study_treatment where study_treatment.id = biosample_action.treatment_id) || ';'
//|| (select max(name) from study_phase where id = biosample_action.phase_id) || ';'
//|| treatment_weight || ';' 
//|| treatment_effdose || ';' 
//|| treatment_effdose2 || ';' 
//|| replace(treatment_formulation, ';', '_') || ';' 
//|| replace(comments, ';', '_')) over(order by id desc)
//from biosample_action
//where biosample_action.biosample_id=biosample.ID and type='Treatment')
//where lastaction is null and biosample.ATTACHEDSTUDY_ID is not null;
//
//
//
//update biosample_aud
//set lastaction = 
//(select distinct first_value('Treatment;' 
//|| (select max(name) from study_treatment_aud where study_treatment_aud.id = biosample_action_aud.treatment_id) || ';'
//|| (select max(name) from study_phase_aud where id = biosample_action_aud.phase_id) || ';'
//|| treatment_weight || ';' 
//|| treatment_effdose || ';' 
//|| treatment_effdose2 || ';' 
//|| replace(treatment_formulation, ';', '_') || ';' 
//|| replace(comments, ';', '_')) over(order by id desc)
//from biosample_action_aud
//where biosample_action_aud.biosample_id=biosample_aud.ID and type='Treatment' and biosample_aud.rev = biosample_action_aud.rev)
//where lastaction is null and biosample_aud.ATTACHEDSTUDY_ID is not null;
			;
			

	
	public MigrationScript2_1() {
		super("2.1");
	}
	
	@Override
	public String getMigrationSql(SQLVendor vendor) throws Exception {
		return SQLConverter.convertScript(SCRIPT, vendor);
	}
	
}
