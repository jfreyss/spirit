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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.util.SQLConverter;
import com.actelion.research.spiritcore.util.SQLConverter.SQLVendor;

/**
 * Move to Spirit 1.+ if version is below 1.9
 * @author freyssj
 *
 */
public class MigrationScript1_9 extends MigrationScript {

	private String SCRIPT1 =
			"ALTER TABLE SPIRIT.REVINFO ADD (USERID VARCHAR2(20 CHAR) );\n"
			+ "ALTER TABLE SPIRIT.BIOSAMPLE ADD (METADATA VARCHAR2(4000 CHAR) );\n"
			+ "ALTER TABLE SPIRIT.BIOSAMPLE_AUD ADD (METADATA VARCHAR2(4000 CHAR) );\n"

			+ "CREATE TABLE SPIRIT.BIOSAMPLE_BIOSAMPLE (BIOSAMPLE_ID NUMBER(19) NOT NULL, LINKEDBIOSAMPLE_ID NUMBER(19) NOT NULL, BIOTYPEMETADATA_ID NUMBER(19) NOT NULL);\n"
			+ "ALTER TABLE SPIRIT.BIOSAMPLE_BIOSAMPLE ADD CONSTRAINT BIOSAMPLE_BIOSAMPLE_FK1 FOREIGN KEY (BIOSAMPLE_ID) REFERENCES SPIRIT.BIOSAMPLE (ID) ENABLE;\n"
			+ "ALTER TABLE SPIRIT.BIOSAMPLE_BIOSAMPLE ADD CONSTRAINT BIOSAMPLE_BIOSAMPLE_FK2 FOREIGN KEY (LINKEDBIOSAMPLE_ID) REFERENCES SPIRIT.BIOSAMPLE (ID) ENABLE;\n"
			+ "ALTER TABLE SPIRIT.BIOSAMPLE_BIOSAMPLE ADD CONSTRAINT BIOSAMPLE_BIOSAMPLE_FK3 FOREIGN KEY (BIOTYPEMETADATA_ID) REFERENCES SPIRIT.BIOTYPE_METADATA (ID) ENABLE;\n"
			+ "CREATE TABLE SPIRIT.BIOSAMPLE_BIOSAMPLE_AUD (REV NUMBER(10) , REVTYPE NUMBER(3), BIOSAMPLE_ID NUMBER(19) NOT NULL, LINKEDBIOSAMPLE_ID NUMBER(19) NOT NULL, BIOTYPEMETADATA_ID NUMBER(19) NOT NULL);\n"
			+ "ALTER TABLE SPIRIT.BIOSAMPLE_BIOSAMPLE_AUD ADD CONSTRAINT BIOSAMPLE_BIOSAMPLE_AUD_FK1 FOREIGN KEY (REV) REFERENCES SPIRIT.REVINFO (REV);\n"

			+ "CREATE TABLE SPIRIT.BIOSAMPLE_DOCUMENT (BIOSAMPLE_ID NUMBER(19) NOT NULL, LINKEDDOCUMENT_ID NUMBER(19) NOT NULL, BIOTYPEMETADATA_ID NUMBER(19) NOT NULL);\n"
			+ "ALTER TABLE SPIRIT.BIOSAMPLE_DOCUMENT ADD CONSTRAINT BIOSAMPLE_DOCUMENT_FK1 FOREIGN KEY (BIOSAMPLE_ID) REFERENCES SPIRIT.BIOSAMPLE (ID) ENABLE;\n"
			+ "ALTER TABLE SPIRIT.BIOSAMPLE_DOCUMENT ADD CONSTRAINT BIOSAMPLE_DOCUMENT_FK2 FOREIGN KEY (LINKEDDOCUMENT_ID) REFERENCES SPIRIT.DOCUMENT (ID) ENABLE;\n"
			+ "ALTER TABLE SPIRIT.BIOSAMPLE_DOCUMENT ADD CONSTRAINT BIOSAMPLE_DOCUMENT_FK3 FOREIGN KEY (BIOTYPEMETADATA_ID) REFERENCES SPIRIT.BIOTYPE_METADATA (ID) ENABLE;\n"
			+ "CREATE TABLE SPIRIT.BIOSAMPLE_DOCUMENT_AUD (REV NUMBER(10) , REVTYPE NUMBER(3), BIOSAMPLE_ID NUMBER(19) NOT NULL, LINKEDDOCUMENT_ID NUMBER(19) NOT NULL, BIOTYPEMETADATA_ID NUMBER(19) NOT NULL);\n"
			+ "ALTER TABLE SPIRIT.BIOSAMPLE_DOCUMENT_AUD ADD CONSTRAINT BIOSAMPLE_DOCUMENT_AUD_FK1 FOREIGN KEY (REV) REFERENCES SPIRIT.REVINFO (REV);\n"
			
			+ "ALTER TABLE SPIRIT.STUDY ADD (METADATA VARCHAR2(4000) );\n"
			+ "ALTER TABLE SPIRIT.STUDY_AUD ADD (METADATA VARCHAR2(4000) );\n"
			+ "ALTER TABLE SPIRIT.SPIRIT_PROPERTY MODIFY (ID VARCHAR2(64 CHAR) );\n"
			+ "update study set METADATA = concat(concat('TYPE=', replace(replace(type, '\\', '\\\\'), ';', '\\;'))"
			+ ", concat(concat(';CLINICAL=' , replace(replace(clinical, '\\', '\\\\'), ';', '\\;'))"
			+ ", concat(concat(';PROJECT=' , replace(replace(project, '\\', '\\\\'), ';', '\\;'))"
			+ ", concat(concat(';SITE=' , replace(replace(external_site, '\\', '\\\\'), ';', '\\;'))"
			+ ", concat(concat(';LICENCENO=' , replace(replace(licenceno, '\\', '\\\\'), ';', '\\;'))"
			+ ", concat(concat(';EXPERIMENTER=' , replace(replace(rnd_experimenter, '\\', '\\\\'), ';', '\\;'))"
			+ ", concat(';DISEASEAREA=' , replace(replace(diseases, '\\', '\\\\'), ';', '\\;'))))))))"
			+ "where METADATA is null;\n"
			+ "update study_aud set METADATA = concat(concat('TYPE=', replace(replace(type, '\\', '\\\\'), ';', '\\;'))"
			+ ", concat(concat(';CLINICAL=' , replace(replace(clinical, '\\', '\\\\'), ';', '\\;'))"
			+ ", concat(concat(';PROJECT=' , replace(replace(project, '\\', '\\\\'), ';', '\\;'))"
			+ ", concat(concat(';SITE=' , replace(replace(external_site, '\\', '\\\\'), ';', '\\;'))"
			+ ", concat(concat(';LICENCENO=' , replace(replace(licenceno, '\\', '\\\\'), ';', '\\;'))"
			+ ", concat(concat(';EXPERIMENTER=' , replace(replace(rnd_experimenter, '\\', '\\\\'), ';', '\\;'))"
			+ ", concat(';DISEASEAREA=' , replace(replace(diseases, '\\', '\\\\'), ';', '\\;'))))))))"
			+ "where METADATA is null;\n"
			+ "";
			
	
	
	
	public MigrationScript1_9() {
		super("1.9");
	}
	
	@Override
	public String getMigrationSql(SQLVendor vendor) throws Exception {
		Connection conn = DBAdapter.getAdapter().getConnection();
		
		try {			

			StringBuilder sb1 = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select biotype_id, id, datatype, idx from spirit.biotype_metadata order by biotype_id, idx");
			while(rs.next()) {
				int biotypeId = rs.getInt(1);
				int metaId = rs.getInt(2);
								
				sb1.append("update spirit.biosample"
						+ " set metadata = concat(metadata, concat(';" + metaId + "=', (select replace(replace(max(value), '\\', '\\\\'), ';', '\\;') from spirit.biosample_metadata where biosample_metadata.biosample_id = biosample.id and METADATA_TYPE_ID = " + metaId + ")))"
						+ " where biotype_id = "+biotypeId
						+ " and exists(select * from spirit.biosample_metadata where value is not null and biosample_metadata.biosample_id = biosample.id and METADATA_TYPE_ID = " + metaId + ")"
						+ " and (metadata is null or (metadata not like '" + metaId + "=%' and metadata not like '%;" + metaId + "=%'))" 
						+ ";\r\n");
				
				sb1.append("update spirit.biosample_aud"
						+ " set metadata = concat(metadata, concat(';" + metaId + "=', (select replace(replace(max(value), '\\', '\\\\'), ';', '\\;') from spirit.biosample_metadata where biosample_metadata.biosample_id = biosample.id and METADATA_TYPE_ID = " + metaId + ")))"
						+ " where biotype_id = "+biotypeId
						+ " and exists(select * from spirit.biosample_metadata_aud where value is not null and biosample_metadata_aud.rev<=biosample_aud.rev and biosample_metadata_aud.biosample_id = biosample_aud.id and METADATA_TYPE_ID = " + metaId + ")"
						+ " and (metadata is null or (metadata not like '" + metaId + "=%' and metadata not like '%;" + metaId + "=%'))" 
						+ ";\r\n");
				
				
			}
			sb1.append("update spirit.biosample set metadata = substr(metadata, 2) where metadata like ';%';\r\n");
			sb2.append("update spirit.biosample_aud set metadata = substr(metadata, 2) where metadata like ';%';\r\n");
			rs.close();
			stmt.close();
			
			sb2.append("insert into spirit.biosample_biosample (biosample_id, LINKEDBIOSAMPLE_id, biotypemetadata_id) "
					+ " (select biosample_id, linked_biosample_id, metadata_type_id from spirit.biosample_metadata where linked_biosample_id is not null"
					+ " and not exists(select * from spirit.biosample_biosample where biosample_metadata.biosample_id=biosample_biosample.biosample_id and biosample_metadata.metadata_type_id=biosample_biosample.biotypemetadata_id));\r\n");
			sb2.append("insert into spirit.biosample_biosample_aud (rev, revtype, biosample_id, LINKEDBIOSAMPLE_id, biotypemetadata_id) "
					+ " (select rev, revtype, biosample_id, linked_biosample_id, metadata_type_id from spirit.biosample_metadata_aud where linked_biosample_id is not null"
					+ " and not exists(select * from spirit.biosample_biosample_aud where biosample_metadata_aud.rev=biosample_biosample_aud.rev and biosample_metadata_aud.biosample_id=biosample_biosample_aud.biosample_id and biosample_metadata_aud.metadata_type_id=biosample_biosample_aud.biotypemetadata_id));\r\n");
			
			sb2.append("insert into spirit.biosample_document (biosample_id, linkeddocument_id, biotypemetadata_id) "
					+ " (select biosample_id, linked_document_id, metadata_type_id from spirit.biosample_metadata where linked_document_id is not null"
					+ " and not exists(select * from spirit.biosample_document where biosample_metadata.biosample_id=biosample_document.biosample_id and biosample_metadata.metadata_type_id=biosample_document.biotypemetadata_id));\r\n");
			sb2.append("insert into spirit.biosample_document_aud (rev, revtype, biosample_id, linkeddocument_id, biotypemetadata_id) "
					+ " (select rev, revtype, biosample_id, linked_document_id, metadata_type_id from spirit.biosample_metadata_aud where linked_document_id is not null"
					+ " and not exists(select * from spirit.biosample_document_aud where biosample_metadata_aud.rev=biosample_document_aud.rev and biosample_metadata_aud.biosample_id=biosample_document_aud.biosample_id and biosample_metadata_aud.metadata_type_id=biosample_document_aud.biotypemetadata_id));\r\n");
			
			StringBuilder sb = new StringBuilder();
			sb.append(SCRIPT1);
			sb.append(sb1);
			sb.append(sb2);
			System.out.println(SCRIPT1);
			System.out.println(sb.toString());
			
			return SQLConverter.convertScript(sb.toString(), vendor);
		} finally {
			conn.close();
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println(new MigrationScript1_9().getMigrationSql(SQLVendor.ORACLE));
	}
}
