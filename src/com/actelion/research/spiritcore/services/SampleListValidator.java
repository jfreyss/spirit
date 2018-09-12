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

package com.actelion.research.spiritcore.services;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.CSVUtils;

/**
 * SampleListValidator biosample data usually imported from a csv file.
 * It maintains a list of sample labels as it should be in the DB (i.e. metadata case sensitive).
 * Nevertheless, validation is case insensitive
 */
public class SampleListValidator {

	public SampleListValidator() throws Exception {
		dtfBuilder.parseCaseInsensitive();
		for (String pattern : dtPatterns)
			dtfBuilder.appendOptional(DateTimeFormatter.ofPattern(pattern));
	}

	/**
	 *  Looks for existence and uniqueness of barcode metadata and also for study id existence.
	 *
	 * @param csvFile. The chosen file by the user
	 * @param sampleList
	 * @return csv data structure
	 * @throws Exception if csv data contains invalid or missing mandatory values
	 */
	public String[][] validate(File csvFile, Biosample sampleList) throws Exception {

		String[][] data = CSVUtils.importCsv(csvFile);

		// check if already imported
		SpiritUser user = Spirit.askForAuthentication();
		InputStream is = Files.newInputStream(Paths.get(csvFile.getAbsolutePath()));
		String checksum = DigestUtils.md5Hex(is);
		BiosampleLinker linker = new BiosampleLinker(sampleList.getBiotype().getMetadata("Checksum"));
		BiosampleQuery biosampleQuery = BiosampleQuery.createQueryForBiotype(sampleList.getBiotype());
		List<Biosample> bios = DAOBiosample.queryBiosamples(biosampleQuery, user);
		for (Biosample sample : bios) {
			String metadata = sample.getMetadataValue(linker.getBiotypeMetadata());
			if (metadata != null && metadata.equals(checksum))
				throw new Exception("This sample list has already been uploaded.");
		}
		sampleList.setMetadataValue(linker.getBiotypeMetadata(), checksum);

		// detect at which index are each column type from header line. Assuming the first line is the header
		String colName = null;
		for (int i=0; i<data[0].length; i++) {
			colName = data[0][i].toLowerCase();
			addColumnMapping(colName, i);
		}

		// no barcode column found
		if ( !columnMapping.containsKey(ColumnLabel.BARCODE.getLabel()) )
			throw new Exception("No barcode column found");

		// no barcode column found
		if ( columnMapping.containsKey(ColumnLabel.TYPE.getLabel())
				&& !checkTypeData(data) )
			throw new Exception("Invalid type value. " + specificErrorMessage);

		// look for barcode duplicates
		if ( columnHasDuplicates(data, getColumnIndex(ColumnLabel.BARCODE.getLabel())) )
			throw new Exception("Data contains duplicate barcodes. " + specificErrorMessage);

		// check for date format dd-MMM-yyyy
		if ( getColumnIndex(ColumnLabel.COLLECTION_DATE.getLabel()) >= 0 ) {
			checkDateFormat(data, getColumnIndex(ColumnLabel.COLLECTION_DATE.getLabel()));
		}

		// check for study existence
		if ( !validateStudyIds(data) )
			throw new Exception("Study id error. " + specificErrorMessage);

		return data;
	}

	/**
	 * returns the column position for a given column name ({@link ColumnLabel} Enum)
	 *
	 * @param metadataName
	 * @return column index in the user file
	 */
	public int getColumnIndex(String metadataName) {
		String userColumnName = enumToUserColumnName.get(metadataName);
		if (userColumnName != null) {
			ColumnMapper colMapper = columnMapping.get(userColumnName);
			if (colMapper != null) {
				return colMapper.getColPos();
			}
		}


		return -1;
	}

	/**
	 * returns the real DB metadata name based on a given column name ({@link ColumnLabel} Enum)
	 *
	 * @param metadataName
	 * @return DB metadata name
	 */
	public String getColumnMetadataName(String metadataName) {
		String userColumnName = enumToUserColumnName.get(metadataName);
		if (userColumnName != null) {
			ColumnMapper colMapper = columnMapping.get(userColumnName);
			if (colMapper != null) {
				return colMapper.getMetadataName();
			}
		}

		return null;
	}


	public DateTimeFormatterBuilder getDateTimeFormatterBuilder() {
		return dtfBuilder;
	}


	public List<String> getHeaderCols() {
		return headerCols;
	}


	private void addColumnMapping(String colName, int position) throws Exception {
		if ( studyIdSet.contains(colName) ) {
			enumToUserColumnName.put(ColumnLabel.STUDY_ID.getLabel(), colName);
		} else if ( barcodeSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.BARCODE.getLabel(), colName);
		} else if ( subjectIdSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.SUBJECT.getLabel(), colName);
		} else if ( animalIdSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.ANIMAL.getLabel(), colName);
		} else if ( typeSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.TYPE.getLabel(), colName);
		} else if ( sexSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.SEX.getLabel(), colName);
		} else if ( doseGroupSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.DOSE_GROUP.getLabel(), colName);
		} else if ( doseSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.DOSE.getLabel(), colName);
		} else if ( timePointSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.TIMEPOINT.getLabel(), colName);
		} else if ( samplingOccasionSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.SAMPLING_OCCASION.getLabel(), colName);
		} else if ( collectionDateSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.COLLECTION_DATE.getLabel(), colName);
		} else if ( collectionTimeSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.COLLECTION_TIME.getLabel(), colName);
		} else if ( periodSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.PERIOD.getLabel(), colName);
		} else if ( visitSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.VISIT.getLabel(), colName);
		} else if ( aliquotSet.contains(colName) )  {
			enumToUserColumnName.put(ColumnLabel.ALIQUOT.getLabel(), colName);
		} else {
			return;
			//throw new Exception("Invalid sample list column name '" + colName + "'");
		}

		columnMapping.put(colName, createMetadataMapper(colName, position));
	}

	private ColumnMapper createMetadataMapper(String colName, int position) throws Exception {
		if ( barcodeSet.contains(colName) ) {
			headerCols.add(position, ColumnLabel.BARCODE.getLabel());
			return new ColumnMapper(ColumnLabel.BARCODE.getLabel(), position);
		} else if ( studyIdSet.contains(colName) ) {
			headerCols.add(position, ColumnLabel.STUDY_ID.getLabel());
			return new ColumnMapper(ColumnLabel.STUDY_ID.getLabel(), position);
		}

		Set<BiotypeMetadata> metadata = DAOBiotype.getBiotype("Sample").getMetadata();
		String metadataName = null;
		for (BiotypeMetadata m : metadata) {
			metadataName = m.getName().toLowerCase();
			if ( animalIdSet.contains(colName) && animalIdSet.contains(metadataName)
					|| subjectIdSet.contains(colName) && subjectIdSet.contains(metadataName)
					|| typeSet.contains(colName) && typeSet.contains(metadataName)
					|| sexSet.contains(colName) && sexSet.contains(metadataName)
					|| doseGroupSet.contains(colName) && doseGroupSet.contains(metadataName)
					|| doseSet.contains(colName) && doseSet.contains(metadataName)
					|| timePointSet.contains(colName) && timePointSet.contains(metadataName)
					|| samplingOccasionSet.contains(colName) && samplingOccasionSet.contains(metadataName)
					|| collectionDateSet.contains(colName) && collectionDateSet.contains(metadataName)
					|| collectionTimeSet.contains(colName) && collectionTimeSet.contains(metadataName)
					|| periodSet.contains(colName) && periodSet.contains(metadataName)
					|| visitSet.contains(colName) && visitSet.contains(metadataName)
					|| aliquotSet.contains(colName) && aliquotSet.contains(metadataName)) {

				headerCols.add(position, m.getName());
				return new ColumnMapper(m.getName(), position);
			}
		}

		return null;
	}

	private boolean validateStudyIds(String[][] data) {
		int studyIdIdx = getColumnIndex(ColumnLabel.STUDY_ID.getLabel());
		for (int i=1; i<data.length; i++) {
			if (DAOStudy.getStudyByStudyId(data[i][studyIdIdx]) == null) {
				specificErrorMessage = "Invalid study id '" + data[i][studyIdIdx] + "'";
				return false;
			}
		}

		return true;
	}


	private boolean checkTypeData(String[][] data) {
		int typeIdx = getColumnIndex(ColumnLabel.TYPE.getLabel());
		String type = null;
		for (int i=1; i<data.length; i++) {
			type = data[i][typeIdx];
			if (type == null || type.isEmpty()) {
				continue;
			}
			if (!typeDataSet.contains(type.toLowerCase())) {
				specificErrorMessage = "Invalid value '" + type + "' at line " + (i+1);
				return false;
			}
		}

		return true;
	}


	private boolean checkDateFormat(String[][] data, int col) throws Exception {
		DateTimeFormatter dtf = dtfBuilder.toFormatter();
		DateTimeFormatter outDateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
		for (int i=1; i<data.length; i++) {
			try {
				if ( col >= data[i].length || data[i][col] == null || data[i][col].isEmpty() ) {
					continue;
				}
				LocalDate inDate = LocalDate.parse(data[i][col], dtf);
				data[i][col] = inDate.format(outDateFormatter);
			} catch (DateTimeParseException dtpe) {
				specificErrorMessage = "Found value '" + data[i][col] + "' at line " + (i+1);
				throw new Exception("Some dates are not in the format '" + Arrays.toString(dtPatterns) + "'.\r\n" + specificErrorMessage);
			}
		}

		return true;
	}

	private boolean columnHasDuplicates(String[][] data, int col) {
		HashMap<String, Integer> values = new HashMap<String, Integer>();
		for (int i=1; i<data.length; i++) {
			String val = data[i][col]; 
			if ( val == null || val.isEmpty() ) {
				continue;
			}
			if (values.containsKey(data[i][col])) {
				specificErrorMessage = "Value '" + val + "' has been found at lines "+ (values.get(val) + 1) + " and " + (i+1);
				return true;
			} else {
				values.put(val, i);
			}
		}

		return false;
	}

	class ColumnMapper {
		public ColumnMapper(String metadataName, int colPos) {
			this.metadataName = metadataName;
			this.colPos = colPos;
		}

		public String getMetadataName() {
			return metadataName;
		}
		public void setMetadataName(String metadataName) {
			this.metadataName = metadataName;
		}
		public int getColPos() {
			return colPos;
		}
		public void setColPos(int colPos) {
			this.colPos = colPos;
		}

		private String metadataName;
		private int colPos;
	}

	public enum ColumnLabel {
		STUDY_ID("studyid"),
		BARCODE("barcode"),
		ANIMAL("animalid"),
		SUBJECT("subjectid"),
		DOSE_GROUP("dosegroup"),
		DOSE("dose"),
		SEX("sex"),
		SAMPLING_OCCASION("samplingoccasion"),
		COLLECTION_DATE("collectiondate"),
		COLLECTION_TIME("collectiontime"),
		TIMEPOINT("timepoint"),
		PERIOD("period"),
		VISIT("visit"),
		ALIQUOT("aliquot"),
		TYPE("type");

		ColumnLabel(String label) {
			this.label = label;
		}

		public String getLabel() {
			return this.label;
		}

		private String label;
	}

	// String is col name from file. Column mapper is metadata name and column index
	private HashMap<String, ColumnMapper> columnMapping = new HashMap<String, ColumnMapper>(30);
	private HashMap<String, String> enumToUserColumnName = new HashMap<String, String>(30);
	private List<String> headerCols = new ArrayList<String>(Collections.nCopies(50, null));
	private String[] dtPatterns = {"dd.MM.yyyy", "dd-MMM-yyyy", "dd MMM yyyy", "dd/MMM/yyyy"};
	DateTimeFormatterBuilder dtfBuilder = new DateTimeFormatterBuilder();
	private String specificErrorMessage = "";

	// study id values
	private String[] studyIdAcceptedValues = new String[] {"idorsia study id", "idorsia study number", "study number", "studynumber", "study id", "studyid"};
	private Set<String> studyIdSet = new HashSet<String>(Arrays.asList(studyIdAcceptedValues));

	// barcode
	private String[] barcodeAcceptedValues = new String[] {"barcode", "bar code"};
	private Set<String> barcodeSet = new HashSet<String>(Arrays.asList(barcodeAcceptedValues));

	// animal id
	private String[] animalIdAcceptedValues = new String[] {"animal", "animal id", "animalid"};
	private Set<String> animalIdSet = new HashSet<String>(Arrays.asList(animalIdAcceptedValues));

	// biosample type (column name values and data values)
	private String[] typeAcceptedValues = new String[] {"type"};
	private Set<String> typeSet = new HashSet<String>(Arrays.asList(typeAcceptedValues));
	private String[] typeAcceptedData = new String[] {"plasma", "blood", "urine", "saliva"};
	private Set<String> typeDataSet = new HashSet<String>(Arrays.asList(typeAcceptedData));

	// sex
	private String[] sexAcceptedValues = new String[] {"gender", "sex"};
	private Set<String> sexSet = new HashSet<String>(Arrays.asList(sexAcceptedValues));

	// dose group
	private String[] doseGroupAcceptedValues = new String[] {"dose group", "dosegroup"};
	private Set<String> doseGroupSet = new HashSet<String>(Arrays.asList(doseGroupAcceptedValues));

	// dose
	private String[] doseAcceptedValues = new String[] {"dose"};
	private Set<String> doseSet = new HashSet<String>(Arrays.asList(doseAcceptedValues));

	// sampling occasion
	private String[] samplingOccasionAcceptedValues = new String[] {"sample occasion", "sampleoccasion", "sampling occasion", "samplingoccasion"};
	private Set<String> samplingOccasionSet = new HashSet<String>(Arrays.asList(samplingOccasionAcceptedValues));

	// time point
	private String[] timePointAcceptedValues = new String[] {"time point", "timepoint"};
	private Set<String> timePointSet = new HashSet<String>(Arrays.asList(timePointAcceptedValues));

	// aliquot
	private String[] aliquotAcceptedValues = new String[] {"aliquot"};
	private Set<String> aliquotSet = new HashSet<String>(Arrays.asList(aliquotAcceptedValues));

	/* SPECIFIC TO CLINICAL STUDY */
	// collection date
	private String[] collectionDateAcceptedValues = new String[] {"collection date", "collectionDate"};
	private Set<String> collectionDateSet = new HashSet<String>(Arrays.asList(collectionDateAcceptedValues));

	// collection time
	private String[] collectionTimeAcceptedValues = new String[] {"collection time", "collectiontime"};
	private Set<String> collectionTimeSet = new HashSet<String>(Arrays.asList(collectionTimeAcceptedValues));

	// period
	private String[] periodAcceptedValues = new String[] {"period"};
	private Set<String> periodSet = new HashSet<String>(Arrays.asList(periodAcceptedValues));

	// visit
	private String[] visitAcceptedValues = new String[] {"visit"};
	private Set<String> visitSet = new HashSet<String>(Arrays.asList(visitAcceptedValues));

	// subject id
	private String[] subjectIdAcceptedValues = new String[] {"subjectid", "subject id", "subject"};
	private Set<String> subjectIdSet = new HashSet<String>(Arrays.asList(subjectIdAcceptedValues));
}

