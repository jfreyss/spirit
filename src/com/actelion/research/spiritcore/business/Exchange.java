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

package com.actelion.research.spiritcore.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOTest;

/**
 * Exchange is used to represent a Spirit exchange file.
 * 
 * Constraints:
 * - If results are linked to tests, those tests should be present
 * - If results are linked to biosamples, those biosamples should be present
 * - If results are linked to biosamples, those biosamples should be present
 * - If biosamples are linked to locations, those locations should be present
 * - If biosamples are linked to studies, those studies should be present
 * - If biosamples are present, their biotypes should be present
 * - If result are present, their tests should be present
 * - If studies contains samplings, their biotypes should be present
 * 
 * 
 * 
 * Here is the constraint schema showing the relation: 
 *   "If entity is present -> then this related entity must also be present"
 *   
 * ie: 
 * - if exchange contains a result, the test, its related biosample, parent, study, location, biotype must be present
 * - if exchange contains a location, the related biosample don't need to be present
 *      
 * <pre>
 *
 *  result 
 *  
 *    \->  test
 *    
 *    \->  biosample   -> parent, topBiosamples
 *            
 *                    \ -> biotype
 *            
 *                    \ -> location
 *            
 *                    \ -> study
 *   
 * </pre>
 * 
 * 
 * @author freyssj
 *
 *
 *
 */
public class Exchange {
	
	private String name;
	private String version;
	
	private final Set<Biotype> biotypes = new TreeSet<>();
	private final Set<Test> tests = new TreeSet<>();
	
	private final Set<Study> studies = new TreeSet<>(Collections.reverseOrder());
	private final Set<Biosample> biosamples = new TreeSet<>();
	private final Set<Location> locations = new TreeSet<>();
	private final Set<Result> results = new TreeSet<>();
	
	
	public Exchange() {
		this.version = getClass().getPackage().getImplementationVersion();
	}
	
	public Exchange(String name) {
		this.name = name;
	}
	
	/**
	 * Adds the biosamples and its dependencies (parents, studies, locations)
	 * Does not load transitively (biosamples of the study or of the locations are not added)
	 */
	public void addBiosamples(Collection<Biosample> biosamples) {
		if(this.biosamples.containsAll(biosamples)) return;
		
		//Add the dependent parents
		Set<Biosample> hierarchy = Biosample.getParentRecursively(biosamples);
		this.biosamples.addAll(hierarchy);
		
		//Add the dependent biotypes
		this.biotypes.addAll(Biosample.getBiotypes(hierarchy));

		//Add the dependent locations
		this.locations.addAll(Biosample.getLocations(hierarchy));

		//Add the dependent studies
		addStudies(Biosample.getStudies(hierarchy));				

	}
	
	public void addBiotypes(Collection<Biotype> biotypes) {
		this.biotypes.addAll(biotypes);
	}
	
	public void addTests(Collection<Test> tests) {
		this.tests.addAll(tests);
	}
	
	
	/**
	 * Adds the locations and its biosamples plus dependancies (studies, parents).
	 * Note: the biosamples are not normally included in the exchange file
	 */
	public void addLocations(Collection<Location> locations) {
		if(this.locations.containsAll(locations)) return;
				
		this.locations.addAll(locations);
		
		addBiosamples(Location.getBiosamples(locations));		
	}
	
	/**
	 * Adds the results and their dependencies (biosamples)
	 */
	public void addResults(Collection<Result> results) {
		if(this.results.containsAll(results)) return;
		
		this.results.addAll(results);
		this.tests.addAll(Result.getTests(results));

		addBiosamples(Result.getBiosamples(results));		
	}
	
	/**
	 * Adds the study designs and dependencies (biotypes referred by the samplings)
	 * @param biosamples
	 */
	public void addStudies(Collection<Study> collection) {
		List<Study> studies = new ArrayList<>(collection);
		//Sort studies
		Collections.sort(studies, Collections.reverseOrder());
		for (Study study : studies) {
			if(study==null || this.studies.contains(study)) continue;
			
			this.studies.add(study);
			
			//Add the dependent biotypes
			for(NamedSampling ns: study.getNamedSamplings()) {
				for(Sampling s: ns.getAllSamplings()) {
					biotypes.add(s.getBiotype());
				}
			}
			
			//Add the dependent tests
			tests.addAll(Measurement.getTests(study.getAllMeasurementsFromActions()));
			tests.addAll(Measurement.getTests(study.getAllMeasurementsFromSamplings()));
			
			//Temporary fix, add Weighing/FoodWater
//			for(StudyAction a: study.getStudyActions()) {
//				if(a.isMeasureWeight()) {
					Test t = DAOTest.getTest(DAOTest.WEIGHING_TESTNAME);
					if(t!=null) tests.add(t);
//					break;
//				}
//			}
//			for(StudyAction a: study.getStudyActions()) {
//				if(a.isMeasureFood() || a.isMeasureWater()) {
					Test t2 = DAOTest.getTest(DAOTest.FOODWATER_TESTNAME);
					if(t2!=null) tests.add(t2);
//					break;
//				}
//			}
//			for(NamedSampling ns: study.getNamedSamplings()) {
//				for(Sampling s: ns.getAllSamplings()) {
//					if(s.isCommentsRequired()) {
						Test t3 = DAOTest.getTest(DAOTest.OBSERVATION_TESTNAME);
						if(t3!=null) tests.add(t3);
//						break;
//					}
//				}
//			}
		}
		
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getName() {
		return name;
	}	
	public void setName(String name) {
		this.name = name;
	}
	
	public Set<Biotype> getBiotypes() {
		return biotypes;
	}
	public void setBiotypes(Set<Biotype> biotypes) {
		this.biotypes.clear();
		this.biotypes.addAll(biotypes);
	}
	public Set<Test> getTests() {
		return tests;
	}
	public void setTests(Set<Test> tests) {
		this.tests.clear();
		this.tests.addAll(tests);
	}
	public Set<Study> getStudies() {
		return studies;
	}
	public void setStudies(Set<Study> studies) {
		this.studies.clear();
		this.studies.addAll(studies);
	}
	public Set<Biosample> getBiosamples() {
		return biosamples;
	}
	public void setBiosamples(Set<Biosample> biosamples) {
		this.biosamples.clear();
		this.biosamples.addAll(biosamples);
	}
	public Set<Location> getLocations() {
		return locations;
	}
	public void setLocations(Set<Location> locations) {
		this.locations.clear();
		this.locations.addAll(locations);
	}
	public Set<Result> getResults() {
		return results;
	}
	public void setResults(Set<Result> results) {
		this.results.clear();
		this.results.addAll(results);
	}
	
	public boolean isEmpty() {
		return studies.size()==0 && biosamples.size()==0 && results.size()==0 && locations.size()==0 && biotypes.size()==0 && tests.size()==0;
	}
	
	@Override
	public String toString() {
		return "name: " + name + ", biotypes: " + biotypes + ", biosamples: "+biosamples;
	}

	
}