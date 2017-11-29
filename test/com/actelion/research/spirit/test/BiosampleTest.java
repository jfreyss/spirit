package com.actelion.research.spirit.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAODocument;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.IOUtils;
import com.actelion.research.spiritcore.util.MiscUtils;

import junit.framework.AssertionFailedError;

public class BiosampleTest extends AbstractSpiritTest {

	@BeforeClass
	public static void init() throws Exception {
		initDemoExamples(user);
	}

	@Test
	public void testMetadata() throws Exception {
		// Persist biotype
		Biotype biotype = new Biotype();
		biotype.setCategory(BiotypeCategory.PURIFIED);
		biotype.setName("BioTest");
		biotype.setPrefix("test-");
		biotype.setSampleNameLabel("Name");
		biotype.getMetadata().add(new BiotypeMetadata("meta1", DataType.ALPHA));
		biotype.getMetadata().add(new BiotypeMetadata("meta2", DataType.NUMBER));
		biotype.getMetadata().add(new BiotypeMetadata("meta3", DataType.D_FILE));
		biotype.getMetadata().add(new BiotypeMetadata("meta4", DataType.LARGE));
		biotype.getMetadata().add(new BiotypeMetadata("meta5", DataType.MULTI));
		DAOBiotype.persistBiotype(biotype, user);

		// Persist biosamples
		Biosample b1 = new Biosample(biotype);
		Biosample b2 = new Biosample(biotype);

		List<Biosample> list = new ArrayList<>();
		list.add(b1);
		list.add(b2);

		DAOBiosample.persistBiosamples(list, user);

		// Update
		b1.setSampleName("CD4");
		b1.setMetadataValue("meta1", "ALPHA1 ALPHA3");
		b1.setMetadataValue("meta2", "10");
		File f = File.createTempFile("test_", ".txt");
		f.getParentFile().mkdirs();
		IOUtils.bytesToFile("Some file content".getBytes(), f);
		b1.setMetadataDocument(biotype.getMetadata("meta3"), new Document(f));
		b1.setMetadataValue(biotype.getMetadata("meta4"), MiscUtils.repeat("large", 1000));
		b1.setMetadataValue(biotype.getMetadata("meta5"), "b; a;c");

		b2.setSampleName("CD6");
		b2.setMetadataValue("meta1", "ALPHA1 ALPHA2");

		DAOBiosample.persistBiosamples(list, user);


		JPAUtil.clearAll();

		// Query
		BiosampleQuery q = new BiosampleQuery();
		q.setKeywords("ALPHA1");
		Assert.assertEquals(2, DAOBiosample.queryBiosamples(q, user).size());

		q.setKeywords("ALPHA3 ALPHA1");
		Assert.assertEquals(1, DAOBiosample.queryBiosamples(q, user).size());
		Biosample b = DAOBiosample.queryBiosamples(q, user).get(0);
		Assert.assertEquals("ALPHA1 ALPHA3", b.getMetadataValue("meta1"));
		Assert.assertEquals("10", b.getMetadataValue("meta2"));
		Assert.assertNotNull(b.getMetadataDocument(biotype.getMetadata("meta3")).getFileName());
		Assert.assertNotNull(b.getMetadataDocument(biotype.getMetadata("meta3")).getBytes());
		Assert.assertNotNull(DAODocument.getDocument(b.getMetadataDocument(biotype.getMetadata("meta3")).getId()).getBytes());
		Assert.assertEquals(5 * 1000, b.getMetadataValue("meta4").length());
		Assert.assertEquals("a;b;c", b.getMetadataValue("meta5"));

		q.setKeywords("ALPHA2 or ALPHA3");
		Assert.assertEquals(2, DAOBiosample.queryBiosamples(q, user).size());

		q.setKeywords("ALPHA2 ALPHA3");
		Assert.assertEquals(0, DAOBiosample.queryBiosamples(q, user).size());

		q.setKeywords("CD4");
		Assert.assertEquals(1, DAOBiosample.queryBiosamples(q, user).size());

		q = new BiosampleQuery();
		q.getLinker2values().put(new BiosampleLinker(LinkerType.SAMPLENAME), "CD4");
		Assert.assertEquals(1, DAOBiosample.queryBiosamples(q, user).size());

		q = new BiosampleQuery();
		q.getLinker2values().put(new BiosampleLinker(LinkerType.SAMPLENAME), "cd6");
		Assert.assertEquals(1, DAOBiosample.queryBiosamples(q, user).size());


		// Delete biotype (not allowed)
		try {
			DAOBiotype.deleteBiotype(biotype, user);
			throw new AssertionError("Deletion biotype should not be allowed");
		} catch (Exception e) {
			//OK
		}

		// Delete biosamples
		DAOBiosample.deleteBiosamples(list, user);

		// Delete biotype
		DAOBiotype.deleteBiotype(biotype, user);

		// Retest query
		q.setKeywords("ALPHA1");
		Assert.assertEquals(0, DAOBiosample.queryBiosamples(q, user).size());

	}

	@Test
	public void testLinkedBiosamples() throws Exception {
		Biotype antibody = DAOBiotype.getBiotype("Antibody");
		Biotype fluorophore = DAOBiotype.getBiotype("Fluorophore");
		Assert.assertNotNull(antibody);
		Assert.assertNotNull(fluorophore);

		System.out.println("BiosampleTest.testLinkedBiosamples() "+antibody.getMetadata());
		System.out.println("BiosampleTest.testLinkedBiosamples() "+fluorophore.getMetadata());


		//Persist an antibody linked to a fluorophore
		Biosample f1 = new Biosample(fluorophore);
		f1.setMetadataValue("Type", "Primary");

		Biosample b1 = new Biosample(antibody);
		b1.setMetadataBiosample("Fluorophore", f1);

		DAOBiosample.persistBiosamples(MiscUtils.listOf(f1, b1), user);


		//Check the links
		b1 = DAOBiosample.getBiosample(b1.getSampleId());
		Assert.assertNotNull(b1);
		Assert.assertNotNull(b1.getMetadataBiosample("Fluorophore"));
		System.out.println("BiosampleTest.testLinkedBiosamples() "+f1+">"+f1.getMetadataAsString());
		System.out.println("BiosampleTest.testLinkedBiosamples() "+b1+">"+b1.getMetadataBiosample("Fluorophore")+" "+b1.getMetadataBiosample("Fluorophore").getMetadataAsString());
		Assert.assertEquals("Primary", b1.getMetadataBiosample("Fluorophore").getMetadataValue("Type"));


		//
		//Test1 update fluorophore's type
		f1.setMetadataValue("Type", "Secundary");
		DAOBiosample.persistBiosamples(MiscUtils.listOf(f1), user);

		//Check the linked biosample
		b1 = DAOBiosample.getBiosample(b1.getSampleId());
		Assert.assertEquals("Secundary", b1.getMetadataBiosample("Fluorophore").getMetadataValue("Type"));

		//
		//Test2 update fluorophore's id
		f1.setSampleId("MyOwnFluo");
		DAOBiosample.persistBiosamples(MiscUtils.listOf(f1), user);

		//Check the linked biosample
		b1 = DAOBiosample.getBiosample(b1.getSampleId());
		Assert.assertEquals("MyOwnFluo", b1.getMetadataBiosample("Fluorophore").getSampleId());

	}

	@Test
	public void testFilters() throws Exception {
		Biotype organ = DAOBiotype.getBiotype("Organ");
		Assert.assertNotNull(organ);
		DAOBiotype.getAutoCompletionFieldsForComments(organ, null);
		DAOBiotype.getAutoCompletionFieldsForName(organ, null);
		DAOBiotype.getAutoCompletionFieldsForSampleId(organ);
		for (BiotypeMetadata mt : organ.getMetadata()) {
			DAOBiotype.getAutoCompletionFields(mt, null);
		}
	}

	@Test
	public void testHierarchy() throws Exception {
		Biosample b = DAOBiosample.getBiosample("ORG000082");
		Assert.assertNotNull(b);
		Assert.assertEquals(1, b.getHierarchy(HierarchyMode.PARENTS).size());
		Assert.assertEquals(2, b.getParentHierarchy().size());
		Assert.assertEquals(2, b.getChildren().size());
		Assert.assertEquals(4, b.getHierarchy(HierarchyMode.ALL).size());
		Assert.assertEquals(3, b.getHierarchy(HierarchyMode.ATTACHED_SAMPLES).size());
		Assert.assertEquals(2, b.getHierarchy(HierarchyMode.CHILDREN).size());

		Biosample a = b.getTopParent();
		Assert.assertEquals(6, a.getHierarchy(HierarchyMode.ALL).size());
		Assert.assertEquals(6, a.getHierarchy(HierarchyMode.ATTACHED_SAMPLES).size());
		Assert.assertEquals(5, a.getHierarchy(HierarchyMode.CHILDREN).size());
		Assert.assertEquals(6, a.getHierarchy(HierarchyMode.ALL_MAX2).size());
		Assert.assertEquals(0, a.getHierarchy(HierarchyMode.PARENTS).size());

	}

	@Test
	public void testDatatypes() throws Exception {
		int n = DAOBiotype.getBiotypes().size();
		Assert.assertTrue(n > 0);

		// Save new biotype
		Biotype t = new Biotype();
		t.setCategory(BiotypeCategory.PURIFIED);
		t.setName("test");
		t.setSampleNameLabel(null);
		BiotypeMetadata mt1 = new BiotypeMetadata("m1", DataType.ALPHA);
		BiotypeMetadata mt2 = new BiotypeMetadata("m2", DataType.AUTO);
		BiotypeMetadata mt3 = new BiotypeMetadata("m3", DataType.NUMBER);
		BiotypeMetadata mt4 = new BiotypeMetadata("m4", DataType.D_FILE);
		BiotypeMetadata mt5 = new BiotypeMetadata("m5", DataType.BIOSAMPLE);
		BiotypeMetadata mt6 = new BiotypeMetadata("m6", DataType.FORMULA);
		mt6.setParameters("2*M3");
		t.getMetadata().add(mt1);
		t.getMetadata().add(mt2);
		t.getMetadata().add(mt3);
		t.getMetadata().add(mt4);
		t.getMetadata().add(mt5);
		t.getMetadata().add(mt6);
		DAOBiotype.persistBiotypes(Collections.singleton(t), user);
		Assert.assertEquals(n + 1, DAOBiotype.getBiotypes().size());
		Assert.assertTrue(t.getPrefix().length() > 0);

		// Reload
		t = DAOBiotype.getBiotype("test");
		Assert.assertNotNull(t);
		Assert.assertNull(t.getSampleNameLabel());
		Assert.assertNotNull(t.getMetadata("m1"));

		// Create sample
		Biosample b = new Biosample(t);
		b.setMetadataValue("m3", "5");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);

		// Reload sample and check formula
		b = DAOBiosample.getBiosample(b.getSampleId());
		Assert.assertNotNull(b);
		Assert.assertEquals("5", b.getMetadataValue("m3"));
		Assert.assertEquals("10.0", b.getMetadataValue("m6"));

		DAOBiosample.deleteBiosamples(Collections.singleton(b), user);

		// Move metadata to name
		DAOBiotype.moveMetadataToName(t.getMetadata("m1"), user);
		t = DAOBiotype.getBiotype("test");
		Assert.assertEquals("m1", t.getSampleNameLabel());
		Assert.assertNull(t.getMetadata("m1"));

		// Move name to metadata
		DAOBiotype.moveNameToMetadata(t, user);
		t = DAOBiotype.getBiotype("test");
		Assert.assertNull(t.getSampleNameLabel());
		Assert.assertNotNull(t.getMetadata("m1"));

		// Delete biotype
		DAOBiotype.deleteBiotype(t, user);
		Assert.assertEquals(n, DAOBiotype.getBiotypes().size());

	}

	@Test
	public void testNameUnique() throws Exception {
		// Save new biotype
		Biotype t = new Biotype();
		t.setCategory(BiotypeCategory.PURIFIED);
		t.setName("testNameUnique");
		t.setSampleNameLabel("Name");
		t.setNameRequired(true);
		t.setNameUnique(true);
		DAOBiotype.persistBiotypes(Collections.singleton(t), user);

		//Reload biotype
		t = DAOBiotype.getBiotype("testNameUnique");
		Assert.assertNotNull(t);
		Assert.assertEquals("Name", t.getSampleNameLabel());
		Assert.assertTrue(t.isNameUnique());
		Assert.assertTrue(t.isNameRequired());

		// Create sample without required name ->error
		Biosample b = new Biosample(t);
		try {
			DAOBiosample.persistBiosamples(Collections.singleton(b), user);
			throw new AssertionFailedError("Name was required, should fail");
		} catch(Exception e) {
			//No name-> ok to have an error
		}


		// Create sample with required name->Ok
		b.setSampleName("Test");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);


		// Create samples with duplicated name->error
		Biosample b2 = new Biosample(t);
		Biosample b3 = new Biosample(t);
		b2.setSampleName("Test2");
		b3.setSampleName("Test2");

		try {
			DAOBiosample.persistBiosamples(Arrays.asList(new Biosample[]{b2, b3}), user);
			throw new AssertionFailedError("Name is duplicated, should fail");
		} catch(Exception e) {
			//No name-> ok to have an error
		}

		// Create samples with duplicated name->error
		Biosample b4 = new Biosample(t);
		b4.setSampleName("Test");
		try {
			DAOBiosample.persistBiosamples(Collections.singleton(b4), user);
			throw new AssertionFailedError("Name is duplicated, should fail");
		} catch(Exception e) {
			//No name-> ok to have an error
		}



	}
	@Test
	public void testQueries() throws Exception {
		BiosampleQuery q = new BiosampleQuery();
		q.setBiotype(DAOBiotype.getBiotype("Blood"));
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size() > 0);

		q = new BiosampleQuery();
		q.setKeywords("Femur or Tibia");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size() > 0);

		q = new BiosampleQuery();
		q.setKeywords("Femur Tibia");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size() == 0);

		q = new BiosampleQuery();
		q.setKeywords("Femur 1");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size() > 0);

		q = new BiosampleQuery();
		q.setKeywords("EDTA Rat");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size() > 0);

		q = new BiosampleQuery();
		q.setBids(Collections.singleton(1));
		q.setComments("test");
		q.setComments("test");
		q.setContainerIds("toto");
		q.setContainerType(ContainerType.BOTTLE);
		q.setCreDays(5);
		q.setCreUser("test");
		q.setDepartment(DAOEmployee.getEmployeeGroup("test"));
		q.setElbs("test");
		q.setElbs("test");
		q.setExpiryDateMax(new Date());
		q.setExpiryDateMin(new Date());
		q.setFilterInStudy(true);
		q.setFilterNotInContainer(true);
		q.setFilterNotInLocation(true);
		q.setFilterTrashed(true);
		q.setGroup("ds");
		q.setMaxQuality(Quality.BOGUS);
		q.setParentSampleIds("test");
		q.setPhases("fds");
		q.setPhases("fds");
		q.setSampleId("dss");
		q.setSampleIdOrContainerIds("dss");
		q.setSampleNames("dsd");
		q.setSearchMySamples(true);
		q.setSelectOneMode(1);
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size() == 0);

		q = new BiosampleQuery();
		q.getLinker2values().put(new BiosampleLinker(DAOBiotype.getBiotype("Organ"), LinkerType.SAMPLENAME), "Lung");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size() > 0);

		q = new BiosampleQuery();
		q.getLinker2values().put(new BiosampleLinker(DAOBiotype.getBiotype("Blood").getMetadata("Type")), "Heparin");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size() > 0);

		q = new BiosampleQuery();
		q.getLinker2values().put(new BiosampleLinker(DAOBiotype.getBiotype("Animal"), DAOBiotype.getBiotype("Animal").getMetadata("Sex")), "M");
		q.getLinker2values().put(new BiosampleLinker(DAOBiotype.getBiotype("Blood").getMetadata("Type")), "Heparin");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size() > 0);

	}

	@Test
	public void testActions() throws Exception {

		Biosample a = DAOBiosample.getBiosample("ANL000036");

		// Test default status, (if none is set)
		Assert.assertNotNull(a);
		Assert.assertTrue(a.getStatus().isAvailable());
		Assert.assertEquals(Status.INLAB, a.getLastActionStatus().getFirst());
		Assert.assertNotNull(a.getLastActionStatus().getSecond());
		//		Assert.assertNotNull("Planned necropsy".equals(a.getLastActionStatus().getComments()));

		// Test status
		a.setStatus(Status.DEAD, a.getInheritedStudy().getPhase("d0"));
		DAOBiosample.persistBiosamples(Collections.singleton(a), user);

		// Reload
		a = DAOBiosample.getBiosample("ANL000036");
		Assert.assertTrue(!a.getStatus().isAvailable());
		Assert.assertEquals(Status.DEAD, a.getLastActionStatus().getFirst());
		Assert.assertEquals("d12", a.getInheritedGroup().getEndPhase(a.getInheritedSubGroup()).getName());
		Assert.assertEquals("d0", a.getEndPhase().getName());
		Assert.assertEquals("d0", a.getLastActionStatus().getSecond().getName());

		// Test with Plasma now
		Biosample b = DAOBiosample.getBiosample("PLA000003");
		Assert.assertNotNull(b);
		//		Assert.assertEquals(1, b.getActions(ActionLocation.class).size());
		//		int n = b.getActions().size();

		//		// Change status (only 1 recorded)
		//		b.setStatus(Status.USEDUP);
		//		b.setStatus(Status.TRASHED);
		//		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		//
		//		b = DAOBiosample.getBiosample("PLA000003");
		//		Assert.assertTrue(!b.getStatus().isAvailable());
		//		Assert.assertTrue(b.getStatus().getForeground() != null);
		//		Assert.assertTrue(b.getStatus().getBackground() != null);
		//		Assert.assertEquals(n + 1, b.getActions().size());
		//
		//		// An other status change
		//		b.setStatus(Status.LOWVOL);
		//		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		//		b = DAOBiosample.getBiosample("PLA000003");
		//		Assert.assertTrue(b.getStatus().isAvailable());
		//		Assert.assertEquals(n + 2, b.getActions().size());

		// Test location change
		Location loc = DAOLocation.getLocation(null, "InTransfer");
		Assert.assertNotNull(loc);
		b.setContainer(new Container(ContainerType.CRYOTUBE, "11"));
		b.setLocation(loc);
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		b = DAOBiosample.getBiosample("PLA000003");
		Assert.assertEquals(loc, b.getLocation());
		Assert.assertEquals(ContainerType.CRYOTUBE, b.getContainerType());
		Assert.assertEquals("11", b.getContainerId());
		//		Assert.assertEquals(2, b.getActions(ActionLocation.class).size());
		//		Assert.assertEquals(2, b.getActions(ActionContainer.class).size());

		//		// Test ownership
		//		DAOBiosample.changeOwnership(Collections.singleton(b), DAOSpiritUser.loadUser("admin"), user);
		//		b = DAOBiosample.getBiosample("PLA000003");
		//		Assert.assertEquals("admin", b.getCreUser());
		//		Assert.assertEquals(1, b.getActions(ActionOwnership.class).size());

		//		// Test Comment
		//		b.addAction(new ActionComments(b, "Test"));
		//		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		//		b = DAOBiosample.getBiosample("PLA000003");
		//		Assert.assertEquals(1, b.getActions(ActionComments.class).size());
		//		Assert.assertEquals(user.getUsername(), b.getActions(ActionComments.class).get(0).getUpdUser());

		//		// Test Treatment
		//		a = DAOBiosample.getBiosample("ANL000036");
		//		a.addAction(new ActionTreatment(a, a.getInheritedStudy().getPhase("d0"), 100.0, null, null, null, null, "test"));
		//		DAOBiosample.persistBiosamples(Collections.singleton(a), user);
		//		a = DAOBiosample.getBiosample("ANL000036");
		//		Assert.assertEquals(1, a.getActions(ActionTreatment.class).size());
		//		Assert.assertEquals(100, a.getActions(ActionTreatment.class).get(0).getWeight(), 0.01);
		//		Assert.assertEquals("d0", a.getActions(ActionTreatment.class).get(0).getPhase().getName());

	}

	/**
	 * Compare the differences between 2 samples
	 */
	@Test
	public void testDifferencesBiosample() {
		Biosample b1 = new Biosample(DAOBiotype.getBiotype("Human"));
		b1.setContainerType(null);
		b1.setSampleId("Hum1");

		Biosample b2 = new Biosample(DAOBiotype.getBiotype("Animal"));
		b2.setContainerType(ContainerType.CAGE);
		b2.setContainerId("Cage1");
		b2.setSampleId("Hum1");

		Map<String, String> map = b1.getDifferenceMap(b2);
		Assert.assertEquals(2, map.size());
		Assert.assertNotNull(map.get("Container"));
		Assert.assertNotNull(map.get("Biotype"));
		Assert.assertEquals("SampleId=Hum1; Biotype=Human", b1.getDifference(null));
		Assert.assertEquals("Container=NA; Biotype=Human", b1.getDifference(b2));


		b1 = new Biosample(DAOBiotype.getBiotype("Animal"));
		b1.setContainerType(ContainerType.CAGE);
		b1.setContainerId("Cage1");
		b1.setSampleName("No1");
		b1.setMetadataValue("Type", "Rat");
		b1.setMetadataValue("Sex", "M");
		b1.setComments("Animal1");

		b2 = new Biosample(DAOBiotype.getBiotype("Animal"));
		b2.setContainerType(ContainerType.CAGE);
		b2.setContainerId("Cage1");
		b2.setSampleName("No2");
		b2.setMetadataValue("Type", "Mice");
		b2.setMetadataValue("Sex", "M");
		b2.setComments("Animal2");

		Assert.assertEquals("No=No1; Type=Rat; Comments=Animal1", b1.getDifference(b2));
		Assert.assertEquals("No=No2; Type=Mice; Comments=Animal2", b2.getDifference(b1));

		b1 = new Biosample(DAOBiotype.getBiotype("Animal"), "ANL1");
		b2 = new Biosample("ANL2");
		Assert.assertEquals("SampleId=ANL1; Biotype=Animal", b1.getDifference(b2));
		Assert.assertEquals("SampleId=ANL2; Biotype=", b2.getDifference(b1));
	}


	/**
	 * Compare the differences between 2 samples
	 */
	@Test
	public void testDifferencesBiotypes() {
		Biotype b1 = new Biotype("B1");
		b1.setSampleNameLabel("Main");
		b1.getMetadata().add(new BiotypeMetadata("M1", DataType.ALPHA));

		Biotype b2 = b1.clone();
		b2.setSampleNameLabel("Main2");
		b2.setContainerType(ContainerType.BOTTLE);
		b2.getMetadata().add(new BiotypeMetadata("M2", DataType.ALPHA));


		Assert.assertEquals("ContainerType=Bottle; SampleName=B1; Metadata=added M2", b2.getDifference(b1));
	}

	/**
	 * Compare the sorting: samples should be sorted by
	 * - study desc, groups, sampleNames
	 * - samplesnames should be sorted in a numerical orders
	 */
	@Test
	public void testCompareBiosample1() {
		Biosample b1 = new Biosample(DAOBiotype.getBiotype("Animal"));
		Biosample b2 = new Biosample(DAOBiotype.getBiotype("Animal"));
		Biosample b3 = new Biosample(DAOBiotype.getBiotype("Animal"));
		Biosample b4 = new Biosample(DAOBiotype.getBiotype("Animal"));

		b1.setInheritedStudy(DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-2").get(0));
		b2.setInheritedStudy(DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0));
		b3.setInheritedStudy(DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0));
		b4.setInheritedStudy(DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0));

		b2.setInheritedGroup(DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0).getGroups().iterator().next());
		b3.setInheritedGroup(DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0).getGroups().iterator().next());
		b4.setInheritedGroup(null);

		b1.setSampleName("99");
		b2.setSampleName("8");
		b3.setSampleName("10");
		b4.setSampleName("0");

		Assert.assertTrue(b1.compareTo(b2)<0);
		Assert.assertTrue(b2.compareTo(b3)<0);
		Assert.assertTrue(b3.compareTo(b4)<0);

	}

}
