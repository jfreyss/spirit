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

package com.actelion.research.spiritcore.services.exchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.Metadata;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Pair;

/**
 * ExchangeMapping serves as a mapping tool between the exchanged objects and the local objects.
 * This class encapsulates an Exchange class and is destructive, as it updates the objects to match the mapping
 * 
 * Functions that could be done on each object are either:
 *  - Skip (default, so the mapped objects are empty if no mapping was specified)
 *  - Map to...
 *  - Create
 *  
 *  Objects returned by the Mapper are ready to be saved. So Update/CreDate are reset.
 *  
 * @author freyssj
 */
public class ExchangeMapping {

	private static Logger logger = LoggerFactory.getLogger(ExchangeMapping.class);
	public static enum MappingAction {
		/*Ignore  or link if existing entity*/
		SKIP, 
		/*replace if existing entity*/
		MAP_REPLACE, 
		/*Create or duplicate if existing entity*/
		CREATE};

	////////////////Input Exchange
	private final Exchange exchange;

	//Biotype
	private final Map<String, MappingAction> biotype2action = new HashMap<>();
	private final Map<String, MappingAction> biotype2existingBiosampleAction = new HashMap<>();
	private final Map<String, Biotype> biotype2mappedBiotype = new HashMap<>();
	//TODO map name, comments, amount, ...	
	private final Map<Pair<String, String>, MappingAction> biotypeMetadata2action = new HashMap<>();
	private final Map<Pair<String, String>, BiotypeMetadata> biotypeMetadata2mappedBiotypeMetadata = new HashMap<>();	

	//Test
	private final Map<String, MappingAction> test2action = new HashMap<>();
	private final Map<String, MappingAction> test2existingResultAction = new HashMap<>();
	private final Map<String, Test> test2mappedTest = new HashMap<>();
	private final Map<Pair<String, String>, MappingAction> testAttribute2action = new HashMap<>();
	private final Map<Pair<String, String>, TestAttribute> testAttribute2mappedTestAttribute = new HashMap<>();	
	
	
	//Study
	private final Map<String, MappingAction> studyId2action = new HashMap<>();
	private final Map<String, Study> studyId2mappedStudy = new HashMap<>();


	//Location
	private final Map<String, MappingAction> location2action = new HashMap<>();
	private final Map<String, Location> location2mappedLocation = new HashMap<>();

	//Biosample
	private final Map<String, Biosample> sampleId2mappedBiosample = new HashMap<>();
		

	//Mapped entities, computed after a call to computeMapped
	private boolean hasExistingEntities;
	private boolean mapped = false;
	private List<Study> mappedStudies;
	private List<Biotype> mappedBiotypes;
	private List<Biosample> mappedBiosamples;
	private List<Location> mappedLocations;
	private List<Test> mappedTests;
	private List<Result> mappedResults;

	/**
	 * Create a default ExchangeMapping. If some data exists already, it is linked (IGNORE_LINK option)
	 * @param exchange
	 */
	public ExchangeMapping(Exchange exchange) {
		this(exchange, MappingAction.SKIP);
	}	
	
	/**
	 * Create a ExchangeMapping, while specifying how to import data.
	 * @param exchange
	 */
	public ExchangeMapping(Exchange exchange, MappingAction defaultActionWhenExistingEntity) {
		this.exchange = exchange;
		resetIds();

		initializeMappingFromDb(defaultActionWhenExistingEntity);
	}	
	
	public Map<String, MappingAction> getStudyId2action() {
		return studyId2action;
	}
	public Map<String, Study> getStudyId2mappedStudy() {
		return studyId2mappedStudy;
	}
	public Map<String, Biotype> getBiotype2mappedBiotype() {
		return biotype2mappedBiotype;
	}
	public Map<String, MappingAction> getBiotype2existingBiosampleAction() {
		return biotype2existingBiosampleAction;
	}	
	public Map<String, MappingAction> getBiotype2action() {
		return biotype2action;
	}
	public Map<Pair<String, String>, BiotypeMetadata> getBiotypeMetadata2mappedBiotypeMetadata() {
		return biotypeMetadata2mappedBiotypeMetadata;
	}
	public Map<Pair<String, String>, MappingAction> getBiotypeMetadata2action() {
		return biotypeMetadata2action;
	}
	
	public Map<String, Location> getLocation2mappedLocation() {
		return location2mappedLocation;
	}	
	public Map<String, MappingAction> getLocation2action() {
		return location2action;
	}
	
	public Map<String, Test> getTest2mappedTest() {
		return test2mappedTest;
	}	
	public Map<String, MappingAction> getTest2existingResultAction() {
		return test2existingResultAction;
	}
	public Map<String, MappingAction> getTest2action() {
		return test2action;
	}
	public Map<Pair<String, String>, TestAttribute> getTestAttribute2mappedTestAttribute() {
		return testAttribute2mappedTestAttribute;
	}
	public Map<Pair<String, String>, MappingAction> getTestAttribute2mappingAction() {
		return testAttribute2action;
	}

	/**
	 * Computes and return the entities mapped by this mapping (assuming a mapping has been set, or initializeMapping has been called)
	 */
	public List<Study> getMappedStudies() throws Exception {
		computeMapped();
		return mappedStudies;
	}
	/**
	 * Computes and return the entities mapped by this mapping (assuming a mapping has been set, or initializeMapping has been called)
	 */
	public List<Biotype> getMappedBiotypes() throws Exception {
		computeMapped();
		return mappedBiotypes;
	}
	/**
	 * Computes and return the entities mapped by this mapping (assuming a mapping has been set, or initializeMapping has been called)
	 */
	public List<Biosample> getMappedBiosamples() throws Exception {
		computeMapped();
		return mappedBiosamples;
	}
	/**
	 * Computes and return the entities mapped by this mapping (assuming a mapping has been set, or initializeMapping has been called)
	 */
	public List<Location> getMappedLocations() throws Exception {
		computeMapped();
		return mappedLocations;
	}
	/**
	 * Computes and return the entities mapped by this mapping (assuming a mapping has been set, or initializeMapping has been called)
	 */
	public List<Test> getMappedTests() throws Exception {
		computeMapped();
		return mappedTests;
	}
	/**
	 * Computes and return the entities mapped by this mapping (assuming a mapping has been set, or initializeMapping has been called)
	 */
	public List<Result> getMappedResults() throws Exception {
		computeMapped();
		return mappedResults;
	}
	
	/**
	 * Initializes mapping from the DB status. 
	 * <li> For Biotypes, Tests, Locations: the mapping is always CREATE if the entity is new, or MAP if it exists already
	 * <li> For Biosample, Result: defaultActionWhenExistingEntity defines how the mapping should be done if the entity exists
	 * <li> For Study: defaultActionWhenExistingEntity defines how the mapping should be done if the entity exists, (REPLACE does not work)
	 * 
	 * 
	 */
	public void initializeMappingFromDb(MappingAction defaultActionWhenExistingEntity) {
		

		//Biotype, biosamples
		List<Biotype> existingBiotypes = DAOBiotype.getBiotypes();
		Map<String, Biotype> name2existingBiotype = Biotype.mapName(existingBiotypes);
		Map<String, Biosample> sampleId2existing = DAOBiosample.getBiosamplesBySampleIds(Biosample.getSampleIds(exchange.getBiosamples()));

		for(Biotype biotype: exchange.getBiotypes()) {
			Biotype existing = name2existingBiotype.get(biotype.getName());
			
			biotype2action.put(biotype.getName(), existing!=null? MappingAction.MAP_REPLACE: MappingAction.CREATE);
			biotype2mappedBiotype.put(biotype.getName(), existing!=null? existing: biotype);
			biotype2existingBiosampleAction.put(biotype.getName(), defaultActionWhenExistingEntity);

			for(BiotypeMetadata m: biotype.getMetadata()) {
				BiotypeMetadata existing2 = existing==null? null: existing.getMetadata(m.getName());
				biotypeMetadata2action.put(new Pair<String, String>(biotype.getName(), m.getName()), existing2!=null? MappingAction.SKIP: MappingAction.CREATE);
				biotypeMetadata2mappedBiotypeMetadata.put(new Pair<String, String>(biotype.getName(), m.getName()), existing2!=null? existing2: m);
			}
		}
		for(Biosample b: exchange.getBiosamples()) {
			Biosample existing = sampleId2existing.get(b.getSampleId()); 
			if(existing!=null) {
				hasExistingEntities = true;
			}
			sampleId2mappedBiosample.put(b.getSampleId(), existing!=null? existing: b);
		}
		
		
		//Tests, results
		List<Test> existingTests = DAOTest.getTests();
		Map<String, Test> name2existingTest = Test.mapName(existingTests);
		Map<String, Result> resultKey2result = DAOResult.findSimilarResults(exchange.getResults());
		for(Test test: exchange.getTests()) {
			Test existing = name2existingTest.get(test.getName());
			test2action.put(test.getName(), existing!=null? MappingAction.MAP_REPLACE: MappingAction.CREATE);
			test2mappedTest.put(test.getName(), existing!=null? existing: test);
			test2existingResultAction.put(test.getName(), defaultActionWhenExistingEntity);
			
			for(TestAttribute ta: test.getAttributes()) {
				TestAttribute existing2 = existing==null? null: existing.getAttribute(ta.getName());
				testAttribute2action.put(new Pair<String, String>(test.getName(), ta.getName()), existing2!=null? MappingAction.SKIP: MappingAction.CREATE);
				testAttribute2mappedTestAttribute.put(new Pair<String, String>(test.getName(), ta.getName()), existing2!=null? existing2: ta);
			}
		}
		for(Result r: exchange.getResults()) {
			if(resultKey2result.get(r.getTestBiosamplePhaseInputKey())!=null) {
				hasExistingEntities = true;
			}
		}

		//Study
		Set<String> ids = Study.getIvvOrStudyIds(exchange.getStudies());
		Map<String, List<Study>> id2existingStudy = Study.mapIvvAndStudyId(DAOStudy.getStudyByIvvOrStudyId(MiscUtils.flatten(ids, " ")));
		for (Study s : exchange.getStudies()) {
			List<Study> l = id2existingStudy.get(s.getIvv());
			if(l==null || l.size()!=1 || l.get(0).getGroups().size()!=s.getGroups().size() || l.get(0).getPhases().size()!=s.getPhases().size()) {
				l = id2existingStudy.get(s.getStudyId());
			}
			
			if(l==null || l.size()!=1 || l.get(0).getGroups().size()!=s.getGroups().size() || l.get(0).getPhases().size()!=s.getPhases().size()) {
				logger.debug("initializeMappingFromDb: Create study "+s.getStudyId());
				studyId2mappedStudy.put(s.getStudyId(), s);
				studyId2action.put(s.getStudyId(), MappingAction.CREATE);
			} else {
				logger.debug("initializeMappingFromDb: Match study "+s.getStudyId()+" to "+l+"> "+l.get(0));
				studyId2mappedStudy.put(s.getStudyId(), l.get(0));
				studyId2action.put(s.getStudyId(), defaultActionWhenExistingEntity);
			}			
		}
		for (Study s : exchange.getStudies()) {
			if(id2existingStudy.get(s.getStudyId())!=null || id2existingStudy.get(s.getIvv())!=null) {
				hasExistingEntities = true;
			}		
		}

		
		//Location
		for (Location l : exchange.getLocations()) {
			Location existing = null;
			try {
				existing = DAOLocation.getCompatibleLocation(l.getHierarchyFull(), null);
			} catch(Exception e) {
				existing = null;
			}
			location2action.put(l.getHierarchyFull(), existing!=null? MappingAction.MAP_REPLACE: MappingAction.CREATE);
			location2mappedLocation.put(l.getHierarchyFull(), existing!=null? existing: l);
		}
		
		
		logger.debug("initializeMappingFromDb("+defaultActionWhenExistingEntity+")");
		logger.debug("test2action="+test2action);
		logger.debug("test2mappedTest="+test2mappedTest);
		logger.debug("testAttribute2action="+testAttribute2action);
		logger.debug("testAttribute2mappedTestAttribute="+testAttribute2mappedTestAttribute);
		logger.debug("test2existingResultAction="+test2existingResultAction);
		logger.debug("biotype2action="+biotype2action);
		logger.debug("biotype2mappedBiotype="+biotype2mappedBiotype);
		logger.debug("biotypeMetadata2action="+biotypeMetadata2action);
		logger.debug("biotypeMetadata2mappedBiotypeMetadata="+biotypeMetadata2mappedBiotypeMetadata);
		logger.debug("biotype2existingBiosampleAction="+biotype2existingBiosampleAction);
		logger.debug("studyId2mappedStudy="+studyId2mappedStudy);
		logger.debug("studyId2action="+studyId2action);
		logger.debug("location2mappedLocation="+location2mappedLocation);
	}
	public boolean hasExistingEntities() {
		return hasExistingEntities;
	}
	
	private void computeMapped() throws Exception {
		if(!mapped) {
			mapped = true;
			computeMappedBiotypes();
			computeMappedTests();

			computeMappedStudies();
			computeMappedLocations();
			computeMappedBiosamples();
			computeMappedResults();
			
		}
	}	
	
	private void resetIds() {
		
		//Reset biotype.ids
		for (Biotype inputBiotype : exchange.getBiotypes()) {
			inputBiotype.setId(0);
			for (BiotypeMetadata mt : inputBiotype.getMetadata()) {
				mt.setId(0);
			}
		}
		
		//Reset test.ids
		for (Test inputTest : exchange.getTests()) {
			inputTest.setId(0);
			for (TestAttribute ta : inputTest.getAttributes()) {
				ta.setId(0);
			}
		}
		

	}
	private void computeMappedBiotypes() throws Exception {
		logger.info("Map " +exchange.getBiotypes().size() + " biotypes" );
		
		
		mappedBiotypes = new ArrayList<>();
		for (Biotype inputBiotype : exchange.getBiotypes()) {
			MappingAction action = biotype2action.get(inputBiotype.getName());
			if(action==MappingAction.CREATE) {
				if(inputBiotype.getParent()!=null) {
					Biotype parentBiotype = biotype2mappedBiotype.get(inputBiotype.getParent().getName());
					if(parentBiotype!=null) inputBiotype.setParent(parentBiotype);
				}
				for (BiotypeMetadata m : new ArrayList<>(inputBiotype.getMetadata())) {
					MappingAction action2 = biotypeMetadata2action.get(new Pair<String, String>(inputBiotype.getName(), m.getName()));
					if(action2==null || action2==MappingAction.SKIP) {
						inputBiotype.getMetadata().remove(m);
					} else if(action2==MappingAction.CREATE) {
						//OK
					} else {
						assert action2==MappingAction.MAP_REPLACE;
						throw new Exception("Cannot combine create and then map");
					}
					biotypeMetadata2mappedBiotypeMetadata.put(new Pair<String, String>(inputBiotype.getName(), m.getName()), m);
				}
				mappedBiotypes.add(inputBiotype);
				biotype2mappedBiotype.put(inputBiotype.getName(), inputBiotype);
			} else if(action==MappingAction.MAP_REPLACE) {
				Biotype mappedBiotype = biotype2mappedBiotype.get(inputBiotype.getName());
				if(mappedBiotype==null) throw new Exception("You cannot map a biotype without specifying the mapped biotype");
				for (BiotypeMetadata m : inputBiotype.getMetadata()) {
					MappingAction action2 = biotypeMetadata2action.get(new Pair<String, String>(inputBiotype.getName(), m.getName()));
					if(action2==null || action2==MappingAction.SKIP || action2==MappingAction.MAP_REPLACE) {
						//Nothing
					} else if(action2==MappingAction.CREATE) {
						mappedBiotype.getMetadata().add(m);
						biotypeMetadata2mappedBiotypeMetadata.put(new Pair<String, String>(inputBiotype.getName(), m.getName()), m);
						mappedBiotypes.add(mappedBiotype);
						System.out.println("ExchangeMapping.computeMappedBiotypes() CREATE="+mappedBiotype+" "+m.getName());
					}
				}
			}
		}
	}
	

	private void computeMappedTests() throws Exception {
		logger.info("Map " +exchange.getTests().size() + " tests" );

		mappedTests = new ArrayList<>();
		for (Test inputTest : exchange.getTests()) {
			MappingAction action = test2action.get(inputTest.getName());
			if(action==MappingAction.CREATE) {
				mappedTests.add(inputTest);
				for (TestAttribute ta : new ArrayList<>(inputTest.getAttributes())) {
					MappingAction action2 = testAttribute2action.get(new Pair<String, String>(inputTest.getName(), ta.getName()));
					if(action2==null || action2==MappingAction.SKIP) {
						inputTest.getAttributes().remove(ta);
					} else if(action2==MappingAction.CREATE) {
						//OK
						testAttribute2mappedTestAttribute.put(new Pair<String, String>(inputTest.getName(), ta.getName()), ta);
					} else {
						throw new Exception("Cannot combine create and then map");
					}
					
				}
				
			} else if(action==MappingAction.MAP_REPLACE) {
				Test mappedTest = test2mappedTest.get(inputTest.getName());
				if(mappedTest==null) throw new Exception("You cannot map a test without specifying the mapped test");
				for (TestAttribute ta : inputTest.getAttributes()) {
					MappingAction action2 = testAttribute2action.get(new Pair<String, String>(inputTest.getName(), ta.getName()));
					if(action2==null || action2==MappingAction.SKIP) {
						//Nothing
					} else if(action2==MappingAction.CREATE) {
						TestAttribute ta2 = ta.clone();
						ta2.setTest(mappedTest);
						mappedTest.getAttributes().add(ta2);
						mappedTests.add(mappedTest);
						testAttribute2mappedTestAttribute.put(new Pair<String, String>(inputTest.getName(), ta.getName()), ta);
					} else if(action2==MappingAction.MAP_REPLACE) {
						testAttribute2mappedTestAttribute.put(new Pair<String, String>(inputTest.getName(), ta.getName()), ta);
					}
				}
				
			}
		}
	}
	
	private void computeMappedStudies() throws Exception {
		logger.info("Map " +exchange.getStudies().size() + " studies" );
		

		//Reset study.ids
		for (Study inputStudy : exchange.getStudies()) {
			inputStudy.setId(0);
			for (Group g : inputStudy.getGroups()) {
				g.setId(0);
				if(g.getDividingSampling()!=null) {
					g.getDividingSampling().setId(0);
				}
			}
			for (Phase g : inputStudy.getPhases()) {
				g.setId(0);
			}
			for (NamedTreatment g : inputStudy.getNamedTreatments()) {
				g.setId(0);
			}
			for (NamedSampling g : inputStudy.getNamedSamplings()) {				
				g.setId(0);
				for(Sampling s: g.getAllSamplings()) {
					s.setId(0);
				}
			}
			for (StudyAction g : inputStudy.getStudyActions()) {
				g.setId(0);
			}
		}
				
		
		//Map studies
		mappedStudies = new ArrayList<>();
		for (Study inputStudy : exchange.getStudies()) {
			
			Study existingStudy = studyId2mappedStudy.get(inputStudy.getStudyId());
			MappingAction existingAction = studyId2action.get(inputStudy.getStudyId());
			if(existingStudy==null || existingAction==MappingAction.CREATE) {

				//Create the studies, while fixing links to existing relations: namedSampling.sampling.biotype, action.measurement.test
				mappedStudies.add(inputStudy);
				studyId2mappedStudy.put(inputStudy.getStudyId(), inputStudy);

				//Map sampling.biotypes and metadata
				for (NamedSampling ns : inputStudy.getNamedSamplings()) {
					for(Sampling s: ns.getAllSamplings()) {
						MappingAction action = biotype2action.get(s.getBiotype().getName());
						if(action==MappingAction.SKIP) {
							throw new Exception("Cannot export "+inputStudy+" because the sampling is linked to "+s.getBiotype().getName()+" which is not exported");
						} else { 
							if(s.getBiotype()==null) throw new Exception("The sampling has no biotype");
							Biotype biotype = biotype2mappedBiotype.get(s.getBiotype().getName());
							if(biotype==null) throw new Exception("Cannot export "+inputStudy+" because a sampling refers to "+s.getBiotype()+", which is not mapped");
							Map<BiotypeMetadata, String> map2 = new HashMap<>();
							for (Map.Entry<BiotypeMetadata, String> e : s.getMetadataMap().entrySet()) {
								BiotypeMetadata bm = e.getKey();
								BiotypeMetadata mappedBm = biotypeMetadata2mappedBiotypeMetadata.get(new Pair<String, String>(biotype.getName(), bm.getName()));
								if(mappedBm==null) throw new Exception(s.getBiotype().getName() + " not found in " + biotypeMetadata2mappedBiotypeMetadata);
								map2.put(mappedBm, e.getValue());
								System.out.println("ExchangeMapping.computeMappedStudies() map "+inputStudy+" "+ns+" "+s+" to "+mappedBm+" "+mappedBm.getId() );
							}
							
							s.setBiotype(biotype);
							s.setMetadataMap(map2);
						}
					}
				}
				
				
				//Map measurements.tests
				for(Measurement m: inputStudy.getAllMeasurementsFromActions()) {
					assert m.getTest()!=null && m.getTest().getName()!=null;
					Test t = test2mappedTest.get(m.getTest().getName());
					if(t==null) t = Test.mapName(mappedTests).get(m.getTest().getName());
					assert t!=null: m.getTest().getName() + " was not present in "+test2mappedTest+ " nor "+mappedTests;
					m.setTest(t);
				}
				for(Measurement m: inputStudy.getAllMeasurementsFromSamplings()) {
					assert m.getTest()!=null && m.getTest().getName()!=null;
					Test t = test2mappedTest.get(m.getTest().getName());
					if(t==null) t = Test.mapName(mappedTests).get(m.getTest().getName());
					assert t!=null: m.getTest().getName() + " was not present in "+test2mappedTest+ " nor "+mappedTests;
					m.setTest(t);
				}
			} else if(existingAction==MappingAction.MAP_REPLACE) {
				throw new Exception("The action replace is not implemented for studies");
			} else {
				logger.debug("Link Study "+inputStudy);
				//Ignore, just keep the link
				System.out.println("ExchangeMapping.computeMappedStudies() MAP S TO "+existingStudy+" "+JPAUtil.getManager().contains(existingStudy));
//				int id = existingStudy.getId();
//				if(existingStudy.getId()<=0) throw new Exception("Cannot map "+inputStudy.getStudyId()+" to "+existingStudy+" "+existingStudy.getId());
				existingStudy = JPAUtil.reattach(existingStudy);
				studyId2mappedStudy.put(inputStudy.getStudyId(), existingStudy);					
			}
			
			

		}
		
	}
	

	private void computeMappedLocations() throws Exception {
		
		logger.info("Map " +exchange.getLocations().size() + " locations" );
		//Reset ids
		for (Location inputLocation : exchange.getLocations()) {
			inputLocation.setId(0);
		}
				
		mappedLocations = new ArrayList<>();
		for (Location inputLocation : exchange.getLocations()) {
			Location existing;
			try {
				existing = DAOLocation.getCompatibleLocation(inputLocation.getHierarchyFull(), null);
			} catch(Exception e) {
				existing = null;
			}
			MappingAction action = location2action.get(inputLocation.getHierarchyFull());
			System.out.println("ExchangeMapping.computeMappedLocations() "+inputLocation.getHierarchyFull()+" "+action+" "+existing);
			if(action==null || action==MappingAction.SKIP) {
				//Ignore
			} else if(action==MappingAction.CREATE) {
				
				if(existing!=null) {
					//Unique copy
					Location l2 = new Location();
					l2.setParent(inputLocation.getParent());
					l2.setName(inputLocation.getName()+"."+System.currentTimeMillis());
					inputLocation = l2;
				} 
				
				//Remap parent
				if(inputLocation.getParent()!=null) {
					MappingAction actionParent = location2action.get(inputLocation.getParent().getHierarchyFull());
					if(actionParent==MappingAction.SKIP) {
						inputLocation.setParent(null);
					} else {
						Location mappedLocation = location2mappedLocation.get(inputLocation.getParent().getHierarchyFull());
						if(mappedLocation==null) throw new Exception("You cannot map a location without specifying the mapped location");
						inputLocation.setParent(mappedLocation);						
					}
				}
				mappedLocations.add(inputLocation);
				location2mappedLocation.put(inputLocation.getHierarchyFull(), inputLocation);
			} else if(action==MappingAction.MAP_REPLACE) {
				Location mappedLocation = location2mappedLocation.get(inputLocation.getHierarchyFull());
				if(mappedLocation==null) throw new Exception("You cannot map a location without specifying the mapped location");
				location2mappedLocation.put(inputLocation.getHierarchyFull(), mappedLocation);
			}
				
		}
	}
	
	
	private void computeMappedBiosamples() throws Exception {
		logger.info("Map " +exchange.getBiosamples().size() + " biosamples" );
		
		mappedBiosamples = new ArrayList<>();
		if(exchange.getBiosamples()==null || exchange.getBiosamples().size()==0) return;
		
		//reset ids		
		for (Biosample inputBiosample : exchange.getBiosamples()) {
			inputBiosample.setId(0);
			if(inputBiosample.getAttachedSampling()!=null) inputBiosample.getAttachedSampling().setId(0);
		}
		
		Map<String, Biosample> sampleId2existing = DAOBiosample.getBiosamplesBySampleIds(Biosample.getSampleIds(exchange.getBiosamples()));
		biosampleLoop: for (Biosample inputBiosample : exchange.getBiosamples()) {
			String inputSampleId = inputBiosample.getSampleId();
			Biotype inputBiotype = inputBiosample.getBiotype();
			MappingAction mappingaction = biotype2action.get(inputBiotype.getName());			
			MappingAction existingAction = biotype2existingBiosampleAction.get(inputBiotype.getName());

			if(mappingaction==null || mappingaction==MappingAction.SKIP) {
				//Nothing
				continue biosampleLoop;
			} 
		

			Biotype biotype = biotype2mappedBiotype.get(inputBiotype.getName())!=null? biotype2mappedBiotype.get(inputBiotype.getName()): inputBiotype;
			if(biotype==null) throw new Exception("You cannot map a biotype without specifying the mapped biotype");
			
			
			//Remap the Study/Group/Phase/Sampling
			if(inputBiosample.getInheritedStudy()!=null) {
				Study mappedStudy = studyId2mappedStudy.get(inputBiosample.getInheritedStudy().getStudyId());
				if(mappedStudy==null) {
					throw new Exception("You cannot import the sample "+inputBiosample+" without mapping the study for: "+inputBiosample.getInheritedStudy().getStudyId()+ " studyId2mappedStudy="+studyId2mappedStudy);
				}
				logger.debug("Map "+inputBiosample+" to study "+mappedStudy);
				if(inputBiosample.getAttachedStudy()!=null) {
					if( !inputBiosample.getAttachedStudy().equals(inputBiosample.getInheritedStudy()) ) {
						throw new Exception("The file has a biosample (" + inputBiosample + "), whose attached study is different from the the inherited study!!");
					}
					inputBiosample.setAttachedStudy(mappedStudy);
				}	
				inputBiosample.setInheritedStudy(mappedStudy);

				if(inputBiosample.getInheritedGroup()!=null) {
					Group mappedGroup = mappedStudy.getGroup(inputBiosample.getInheritedGroup().getName());
					if(mappedGroup==null) throw new Exception("Could not find group "+inputBiosample.getInheritedGroup().getName()+" in "+mappedStudy+" for "+inputBiosample+" (available:"+mappedStudy.getGroups()+") studyId2mappedStudy="+studyId2mappedStudy);
					inputBiosample.setInheritedGroup(mappedGroup);
					if(inputBiosample.getInheritedSubGroup()<0 || inputBiosample.getInheritedSubGroup()>=mappedGroup.getNSubgroups()) {
						throw new Exception("The subgroup is invalid for "+inputBiosample);
					}
					inputBiosample.setInheritedSubGroup(inputBiosample.getInheritedSubGroup());
				}
				if(inputBiosample.getInheritedPhase()!=null) {
					Phase mappedPhase = mappedStudy.getPhase(inputBiosample.getInheritedPhase().getShortName());
					if(mappedPhase==null) {
						throw new Exception("The phase "+inputBiosample.getInheritedPhase().getShortName()+" does not exist for the study "+mappedStudy);
					}
					logger.debug("Map "+inputBiosample+" to phase "+mappedPhase+" "+JPAUtil.getManager().contains(mappedPhase));

					inputBiosample.setInheritedPhase(mappedPhase);
				}
				if(inputBiosample.getAttachedSampling()!=null) {
					Sampling mappedSampling = mappedStudy.getSampling(inputBiosample.getAttachedSampling().getNamedSampling().getName(), inputBiosample.getAttachedSampling().getDetailsLong());
					//if the mappedSampling is null, we continue without setting it, no need to throw an exception
					inputBiosample.setAttachedSampling(mappedSampling);
				}					
			}
			
			//Remap the parent (! after the study)
			if(inputBiosample.getParent()!=null) {
				Biosample b = sampleId2mappedBiosample.get(inputBiosample.getParent().getSampleId());
				if(b!=null) {
					logger.debug("Map "+inputBiosample+" to parent "+b);
					inputBiosample.setParent(b, false);
				}					
			}

			//Remap the metadata
			Map<BiotypeMetadata, Metadata> inputMap = new HashMap<>(inputBiosample.getMetadataMap());
			inputBiosample.setMetadataMap(new HashMap<BiotypeMetadata, Metadata>());			
			inputBiosample.setBiotype(biotype);
			for (BiotypeMetadata mt : inputBiotype.getMetadata()) {
				Metadata m = inputMap.get(mt);
				
				MappingAction action2 = biotypeMetadata2action.get(new Pair<String, String>(inputBiotype.getName(), mt.getName()));

				if(action2==MappingAction.SKIP) {
					//ignore this metadata
				} else if(action2==null || action2==MappingAction.CREATE) {
					inputBiosample.setMetadata(mt.getName(), m.getValue());
				} else if(action2==MappingAction.MAP_REPLACE) {
					BiotypeMetadata mappedMt = biotypeMetadata2mappedBiotypeMetadata.get(new Pair<String, String>(inputBiotype.getName(), mt.getName()));
//					if(mappedMt!=null) inputBiosample.setMetadata(mappedMt.getName(), m.getValue());
					if(mappedMt==null) throw new Exception(inputBiotype.getName()+"."+ mt.getName()+" is not mapped");
					inputBiosample.setMetadata(mappedMt, m.getValue());
				}
			}	
			
			
			//SampleId overlap?
			Biosample existing = sampleId2existing.get(inputSampleId);				
			if(existing==null) {
				inputBiosample.setCreDate(null);
				inputBiosample.setCreUser(null);
				inputBiosample.setUpdDate(null);
				inputBiosample.setCreDate(null);
				sampleId2mappedBiosample.put(inputSampleId, inputBiosample);
				logger.debug("Save "+inputSampleId);
			} else {
				if(existingAction==MappingAction.SKIP) {
					//Ignore
					sampleId2mappedBiosample.put(inputSampleId, existing);
					logger.debug("Skip "+inputSampleId);
					continue;
				} else if(existingAction==MappingAction.MAP_REPLACE) {
					if(!existing.getBiotype().equals(inputBiosample.getBiotype())) {
						throw new Exception("the " + inputBiosample.getBiotype().getName() + " " + inputBiosample.getSampleId()+" cannot replace the existing "+existing.getBiotype().getName());
					}
					inputBiosample.setId(existing.getId());
					inputBiosample.setUpdDate(null);//Force replacing without looking at existing date
					sampleId2mappedBiosample.put(inputSampleId, inputBiosample);
					logger.debug("Replace "+inputSampleId+" replacedId="+existing.getId()+" study="+existing.getInheritedStudy()+" sid="+(existing.getInheritedStudy()==null?"NA":existing.getInheritedStudy().getId())
							+" attachedstudy="+existing.getInheritedStudy()+" attachedSid="+(existing.getInheritedStudy()==null?"NA":existing.getInheritedStudy().getId()));

				} else if(existingAction==MappingAction.CREATE) {
					while(DAOBiosample.getBiosample(inputBiosample.getSampleId())!=null) {
						inputBiosample.setSampleId(Biosample.incrementSampleId(inputBiosample.getSampleId()));
					}
					inputBiosample.setUpdDate(null);//Force replacing without looking at existing date
					logger.debug("Create copy "+inputSampleId+"->"+inputBiosample);
					sampleId2mappedBiosample.put(inputSampleId, inputBiosample);
					sampleId2mappedBiosample.put(inputBiosample.getSampleId(), inputBiosample);
				} else {
					throw new Exception("Invalid existingAction "+existingAction);
				}

			}
			mappedBiosamples.add(inputBiosample);
			
			
			//Relink Location
			Location l = inputBiosample.getLocation();
			if(l!=null) {
				Location mappedLoc = location2mappedLocation.get(l.getHierarchyFull()); 
				if(mappedLoc==null) throw new Exception("Invalid Mapped Location for "+l.getHierarchyFull()); 
				inputBiosample.setLocation(mappedLoc);				
			}
		}
	}
	
	

	private void computeMappedResults() throws Exception {
		logger.info("Map " +exchange.getResults().size() + " results" );

		mappedResults = new ArrayList<>();
		if(exchange.getResults()==null || exchange.getResults().size()==0) return;
		
		//reset ids		
		for (Result inputResult : exchange.getResults()) {
			inputResult.setId(0);
		}
		
		Map<String, Result> key2existing = DAOResult.findSimilarResults(exchange.getResults());
		logger.debug("Found similar: "+key2existing);
		loop: for (Result inputResult : exchange.getResults()) {
			Test inputTest = inputResult.getTest();
			MappingAction testAction = test2action.get(inputTest.getName());			

			//Metadata handling
			if(testAction==null || testAction==MappingAction.SKIP) {
				//Nothing
				continue loop;
			} else if(testAction==MappingAction.CREATE || testAction==MappingAction.MAP_REPLACE) {
				//Find existing result
				
				Test test = test2mappedTest.get(inputTest.getName())!=null? test2mappedTest.get(inputTest.getName()): inputTest;
				if(test==null) throw new Exception("You cannot map a test without specifying the mapped test");

				//Map the biosample
				if(inputResult.getBiosample()!=null) {
					Biosample biosample = sampleId2mappedBiosample.get(inputResult.getBiosample().getSampleId());
					if(biosample==null) throw new Exception("The biosample "+inputResult.getBiosample().getSampleId()+" was not exported. It was referred by result: "+inputResult);
					inputResult.setBiosample(biosample);
				}
				
				//Map the Phase
				if(inputResult.getPhase()!=null) {
					if(inputResult.getBiosample()==null || inputResult.getBiosample().getInheritedStudy()==null) throw new Exception("The result "+inputResult+" has a phase, but the biosample has no study");
					Phase phase = inputResult.getBiosample().getInheritedStudy().getPhase(inputResult.getPhase().getShortName());
					if(phase==null) throw new Exception("The phase "+inputResult.getPhase().getShortName()+" is invalid for "+inputResult.getBiosample().getInheritedStudy());
					inputResult.setPhase(phase);
				}
				
				
				//Reset the metadata before possible mapping
				Map<TestAttribute, ResultValue> inputMap = new HashMap<>(inputResult.getResultValueMap());
				inputResult.setResultValueMap(new HashMap<TestAttribute, ResultValue>());			
				inputResult.setTest(test);
				
				//Map the resultvalues
				for (TestAttribute ta : inputTest.getAttributes()) {
					ResultValue rv = inputMap.get(ta);
					MappingAction action2 = testAttribute2action.get(new Pair<String, String>(inputTest.getName(), ta.getName()));
					if(action2==null || action2==MappingAction.SKIP) {
						//ignore this metadata
						inputResult.getResultValueMap().remove(ta);
					} else if(action2==MappingAction.CREATE) {
						//will be done in persistBiosample
						TestAttribute mappedTa = test.getAttribute(ta.getName());
						if(mappedTa==null) throw new RuntimeException(inputTest.getName()+"."+ ta.getName()+" could not be created");
						inputResult.setValue(mappedTa, rv==null?"": rv.getValue());
					} else if(action2==MappingAction.MAP_REPLACE) {
						TestAttribute mappedTa = testAttribute2mappedTestAttribute.get(new Pair<String, String>(inputTest.getName(), ta.getName()));
						if(mappedTa==null) throw new Exception(inputTest.getName()+"."+ ta.getName()+" is not mapped");
						inputResult.setValue(mappedTa, rv==null?"": rv.getValue());
					}
				}
			}
			
			//Result overlap?
			MappingAction existingResultAction = test2existingResultAction.get(inputTest.getName());
			Result existing = key2existing.get(inputResult.getTestBiosamplePhaseInputKey());				
			if(existing==null) {
				inputResult.setCreDate(null);
				inputResult.setCreUser(null);
				inputResult.setUpdDate(null);
				inputResult.setCreDate(null);
				logger.info("new result "+inputResult);
			} else {
				if(existingResultAction==null || existingResultAction==MappingAction.SKIP) {
					//Ignore
					logger.info("skip "+inputResult);
					continue;
				} else if(existingResultAction==MappingAction.MAP_REPLACE) {
					inputResult.setId(existing.getId());
					inputResult.setUpdDate(null);//Force replacing without looking at existing date
					logger.info("replace "+inputResult+" id="+existing.getId());
				} else if(existingResultAction==MappingAction.CREATE) {
					//Duplicate result
					inputResult.setCreDate(null);
					inputResult.setCreUser(null);
					inputResult.setUpdDate(null);
					inputResult.setCreDate(null);
					logger.info("create duplicate "+inputResult);
				} else {
					throw new Exception("Invalid existingAction "+existingResultAction);
				}
			}
			mappedResults.add(inputResult);
			
		}
	}
	
	@Override
	public String toString() {
		return "[ExchangeMapping: "+mappedStudies.size()+" studies, "+mappedBiosamples.size()+" biosamples, "+mappedLocations.size()+" locations, "+mappedResults.size()+" results]";
	}
}
