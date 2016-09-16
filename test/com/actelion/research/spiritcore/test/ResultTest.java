package com.actelion.research.spiritcore.test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.HSQLMemoryAdapter;
import com.actelion.research.spiritcore.adapter.SchemaCreator;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.pivot.ColumnPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.CompactPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.ExpandedPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.FlatPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.InventoryPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotDataTable;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.pivot.analyzer.PivotAnalyzer;
import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorExporter;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;

public class ResultTest {

	private static SpiritUser user;

	@BeforeClass
	public static void initDB() throws Exception {
		// Init user
		user = SpiritUser.getFakeAdmin();

		// Init DB
		DBAdapter.setAdapter(new HSQLMemoryAdapter());
		SchemaCreator.displayTables(DBAdapter.getAdapter());

		ExchangeTest.initDemoExamples(user);
	}

	@Test
	public void testTests() throws Exception {
		// Load Test
		int n = DAOTest.getTests().size();
		Assert.assertTrue(n > 0);

		// Create Test
		com.actelion.research.spiritcore.business.result.Test t = new com.actelion.research.spiritcore.business.result.Test("Test");
		t.setCategory("TEST");
		TestAttribute ta1 = new TestAttribute(t, "input");
		ta1.setOutputType(OutputType.INPUT);
		ta1.setDataType(DataType.AUTO);
		TestAttribute ta2 = new TestAttribute(t, "number");
		ta2.setOutputType(OutputType.OUTPUT);
		ta2.setDataType(DataType.NUMBER);
		TestAttribute ta3 = new TestAttribute(t, "file");
		ta3.setOutputType(OutputType.OUTPUT);
		ta3.setDataType(DataType.D_FILE);
		TestAttribute ta4 = new TestAttribute(t, "formula");
		ta4.setOutputType(OutputType.OUTPUT);
		ta4.setDataType(DataType.FORMULA);
		ta4.setParameters("O1*2");

		t.getAttributes().add(ta1);
		t.getAttributes().add(ta2);
		t.getAttributes().add(ta3);
		t.getAttributes().add(ta4);
		Assert.assertEquals(4, t.getAttributes().size());
		DAOTest.persistTests(Collections.singleton(t), user);

		// Reload tests
		Assert.assertEquals(n + 1, DAOTest.getTests().size());

		t = DAOTest.getTest("Test");
		Assert.assertNotNull(t);
		Assert.assertEquals(4, t.getAttributes().size());
		Assert.assertEquals(1, t.getInputAttributes().size());
		Assert.assertEquals(3, t.getOutputAttributes().size());
		Assert.assertEquals(0, t.getInfoAttributes().size());

		// Try update
		t.getAttribute("file").setOutputType(OutputType.INFO);
		DAOTest.persistTests(Collections.singleton(t), user);
		t = DAOTest.getTest("Test");
		Assert.assertEquals(1, t.getInputAttributes().size());
		Assert.assertEquals(2, t.getOutputAttributes().size());
		Assert.assertEquals(1, t.getInfoAttributes().size());

		// Save some results
		Study s = DAOStudy.getStudyByIvvOrStudyId("IVV2016-1").get(0);
		Biosample a1 = s.getTopAttachedBiosamples().iterator().next();
		Result r1 = new Result(t);
		r1.setElb("test");
		r1.setBiosample(a1);
		r1.setValue("input", "IL1");
		r1.setValue("number", "2.0");
		r1.getResultValue("file").setDocument(new Document(new File("c:/DBAR_Ver.txt")));
		DAOResult.persistResults(Collections.singleton(r1), user);
//		DAOResult.persistExperiment(true, "test", Collections.singleton(r1), user);
		try {
			DAOResult.persistExperiment(true, "test", Collections.singleton(r1), user);
			throw new AssertionError("Deletion should not be possible");
		} catch (Exception e) {
			// ok
		}
		

		// Retrieve result
		List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForElb("test"), user);
		Assert.assertEquals(1, results.size());
		r1 = results.get(0);
		Assert.assertEquals(a1, r1.getBiosample());
		Assert.assertEquals("IL1", r1.getInputResultValuesAsString());
		Assert.assertEquals("2.0", r1.getResultValue("number").getValue());
		Assert.assertEquals("4.0", r1.getResultValue("formula").getValue());
		Assert.assertEquals("DBAR_Ver.txt", r1.getResultValue("file").getDocument().getFileName());

		// test filters
		Assert.assertEquals(1, DAOTest.getAutoCompletionFields(t.getAttribute("input")).size());
		DAOTest.getInputFields(t.getId(), "S-000001");
		Assert.assertEquals(1, DAOTest.countRelations(t.getAttribute("number")));

		// Try Delete
		try {
			DAOTest.removeTest(t, user);
			throw new AssertionError("Deletion should not be possible");
		} catch (Exception e) {
			// ok
		}

		// Remove results
		DAOResult.deleteResults(results, user);

		// Try Delete
		DAOTest.removeTest(t, user);
		Assert.assertNull(DAOTest.getTest("Test"));
	}

	@Test
	public void testFilters() throws Exception {
		Study s = DAOStudy.getStudyByIvvOrStudyId("IVV2016-1").get(0);
		Assert.assertTrue(DAOTest.getTestCategories().size() > 0);
		Assert.assertTrue(DAOTest.getTestsFromElbs("ELB-2016-1").size() > 0);
		Assert.assertTrue(DAOTest.getTestsFromStudies(Collections.singleton(s)).size() > 0);
		Assert.assertTrue(DAOResult.getElbsForStudy(s.getStudyId()).size() > 0);
		Assert.assertTrue(DAOResult.getBiotypes(s.getStudyId(), null).size() > 0);
	}

	@Test
	public void testRecentElbs() throws Exception {
		Assert.assertTrue(DAOResult.getRecentElbs(user).size() > 0);
	}

	@Test
	public void testSaveResults() throws Exception {
		// Retrieve some results
		ResultQuery q = new ResultQuery();
		q.setKeywords("LCMS lung");
		List<Result> results = DAOResult.queryResults(q, user);
		Assert.assertTrue(results.size() > 0);

		
		//Change ownership
		DAOResult.changeOwnership(results, DAOSpiritUser.loadUser("admin"), user);

		//Similar results (we should find all of them)
		Map<String, Result> similar = DAOResult.findSimilarResults(results);
		Assert.assertEquals(results.size(), similar.size());

		//Update result
		Result r = results.get(0);
		com.actelion.research.spiritcore.business.result.Test t = r.getTest();
		Assert.assertEquals("LCMS (metabolics)", t.getName());
		r.getResultValue(t.getInputAttributes().get(0)).setValue("Test");
		r.setFirstOutputValue("3000");
		DAOResult.persistResults(Collections.singleton(r), user);
		
		r = DAOResult.getResults(Collections.singleton(r.getId())).get(0);
		Assert.assertEquals("Test", r.getInputResultValuesAsString());
		Assert.assertEquals("3000", r.getFirstValue());
		
		//Rename inputs
		int n = DAOResult.rename(t.getInputAttributes().get(0), "Test", "Test2", user);
		Assert.assertEquals(1, n);
		r = DAOResult.getResults(Collections.singleton(r.getId())).get(0);
		Assert.assertEquals("Test2", r.getInputResultValuesAsString());
		Assert.assertEquals("3000", r.getFirstValue());
		
		
	}

	@Test
	public void testQueries() throws Exception {
		// Query all fields
		ResultQuery q = new ResultQuery();
		q.setBiotype("test");
		q.setBids(Collections.singleton(5));
		q.setBid(5);
		q.setContainerIds("test");
		q.setCreDays(5);
		q.setElbs("test");
		q.setGroups("test");
		q.setPhases("test");
		q.setQuality(Quality.BOGUS);
		q.setSid(5);
		q.setStudyIds("test");
		q.setTopSampleIds("test");
		q.setUpdDays(4);
		q.setKeywords("test");
		Assert.assertEquals(0, DAOResult.queryResults(q, user).size());

		// Query some parameters
		q = new ResultQuery();
		q.setKeywords("LCMS lung");
		Assert.assertTrue(DAOResult.queryResults(q, user).size() > 0);

		q = new ResultQuery();
		q.setKeywords("organ sick");
		Assert.assertTrue(DAOResult.queryResults(q, user).size() > 0);
	}
	
	@Test
	public void testPivot() throws Exception {
		SchemaCreator.clearExamples(user);
		SchemaCreator.createExamples(user);
		
		// Retrieve some results
		ResultQuery q = new ResultQuery();
		q.setKeywords("IVV2016-1");
		List<Result> results = DAOResult.queryResults(q, user);
		System.out.println("ResultTest.testPivot() "+results);
		Assert.assertTrue(results.size() > 0);

		//Test standard pivot
		PivotTemplate tpl = new CompactPivotTemplate();
		tpl.init(results);		
		PivotDataTable table = new PivotDataTable(results, null, tpl);
		Assert.assertEquals(2, table.getPivotColumns().size());
		Assert.assertEquals(17, table.getPivotRows().size());
		
		tpl = new ColumnPivotTemplate();
		tpl.init(results);		
		table = new PivotDataTable(results, null, tpl);
		Assert.assertEquals(12, table.getPivotColumns().size());
		Assert.assertEquals(17, table.getPivotRows().size());
		
		tpl = new FlatPivotTemplate();
		tpl.init(results);		
		table = new PivotDataTable(results, null, tpl);
		Assert.assertEquals(1, table.getPivotColumns().size());
		
		tpl = new ExpandedPivotTemplate();
		tpl.init(results);		
		table = new PivotDataTable(results, null, tpl);
		Assert.assertTrue(table.getPivotColumns().size()>1);
		Assert.assertEquals(17, table.getPivotRows().size());
		
		tpl = new InventoryPivotTemplate();
		tpl.init(results);		
		table = new PivotDataTable(results, null, tpl);
		Assert.assertEquals(8, table.getPivotColumns().size());
		Assert.assertEquals(4, table.getPivotRows().size());
	}
	
	@Test
	public void testAnalyzer() throws Exception {
		ResultQuery q = new ResultQuery();
		q.setKeywords("IVV2016-1");
		List<Result> results = DAOResult.queryResults(q, user);
		System.out.println("ResultTest.testPivot() "+results);
		Assert.assertTrue(results.size() > 0);

		PivotTemplate tpl = new ColumnPivotTemplate();
		tpl.init(results);		
		PivotDataTable table = new PivotDataTable(results, null, tpl);
		
		PivotAnalyzer analyzer = new PivotAnalyzer(table);
		analyzer.getReport();
		
		DataWarriorExporter.getDwar(table);
	}



}
