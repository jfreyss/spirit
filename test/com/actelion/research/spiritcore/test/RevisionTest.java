package com.actelion.research.spiritcore.test;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.envers.RevisionType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.IOUtils;

public class RevisionTest extends AbstractSpiritTest {

	@BeforeClass
	public static void init() throws Exception {
		initDemoExamples(user);
	}

	@Test
	public void testQueryRevision() throws Exception {
		List<Revision> revs = DAORevision.getRevisions(null, "S-00001", null, null, true, false, false, false, false);
		System.out.println("RevisionTest.testQueryRevision() "+revs);
		Assert.assertTrue(revs.size()>0);

		revs = DAORevision.getRevisions(null, "S-00001", null, null, false, true, true, true, true);
		Assert.assertTrue(revs.size()>0);

		revs = DAORevision.getRevisions(null, "S-00000", null, null, true, true, true, true, true);
		Assert.assertTrue(revs.size()==0);
	}

	@Test
	public void testRevertInsert() throws Exception {
		// Save a biosample
		Biosample b = new Biosample(DAOBiotype.getBiotype("Bacteria"));
		b.setMetadataValue("Resistances", "Ampicilin");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		String sampleId = b.getSampleId();
		Assert.assertNotNull(sampleId);
		Assert.assertNotNull(DAOBiosample.getBiosample(sampleId));

		List<Revision> revisions = DAORevision.getRevisions(b);
		Assert.assertEquals(1, revisions.size());

		Revision rev = revisions.get(0);
		Revision rev2 = DAORevision.getRevision(rev.getRevId());
		Assert.assertEquals(rev.toString(), rev2.toString());
		Assert.assertEquals(RevisionType.ADD, rev.getRevisionType());
		DAORevision.revert(rev, user, "Revert");

		// Load Revisions
		JPAUtil.clearAll();
		Assert.assertNull(DAOBiosample.getBiosample(sampleId));
	}

	@Test
	public void testRevertUpdate() throws Exception {
		// Save a biosample
		Biosample b = new Biosample(DAOBiotype.getBiotype("Bacteria"));
		b.setMetadataValue("Resistances", "Ampicilin");
		b.setComments("Old comments");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		String sampleId = b.getSampleId();
		Assert.assertNotNull(sampleId);
		Assert.assertNotNull(DAOBiosample.getBiosample(sampleId));

		// Update
		b.setComments("New comments");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		Assert.assertEquals("New comments", DAOBiosample.getBiosample(sampleId).getComments());

		// Load Revisions
		JPAUtil.clearAll();
		List<Revision> revisions = DAORevision.getRevisions(b);
		Assert.assertEquals(2, revisions.size());

		Revision rev = revisions.get(0);
		Assert.assertEquals(RevisionType.MOD, rev.getRevisionType());
		DAORevision.revert(rev, user, "Revert");
		b = DAOBiosample.getBiosample(sampleId);
		Assert.assertNotNull(b);
		Assert.assertEquals("Old comments", b.getComments());

	}

	@Test
	public void testRevertDelete() throws Exception {
		// Save a biosample
		Biosample b = new Biosample(DAOBiotype.getBiotype("Bacteria"));
		b.setMetadataValue("Resistances", "Ampicilin");
		b.setComments("Old comments");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		String sampleId = b.getSampleId();
		Assert.assertNotNull(b.getSampleId());
		Assert.assertNotNull(DAOBiosample.getBiosample(sampleId));

		// Delete
		DAOBiosample.deleteBiosamples(Collections.singleton(b), user);
		Assert.assertNull(DAOBiosample.getBiosample(sampleId));

		// Load Revisions
		List<Revision> revisions = DAORevision.getRevisions(b);
		Assert.assertEquals(2, revisions.size());

		Revision rev = revisions.get(0);
		Assert.assertEquals(RevisionType.DEL, rev.getRevisionType());
		DAORevision.revert(rev, user, "Revert");
		b = DAOBiosample.getBiosample(sampleId);
		Assert.assertNotNull(sampleId + " not found", b);
		Assert.assertEquals("Old comments", b.getComments());

	}

	@Test
	public void testRevertCombo() throws Exception {
		// Save a biosample
		Biosample b = new Biosample(DAOBiotype.getBiotype("Bacteria"));
		b.setMetadataValue("Resistances", "Ampicilin");
		b.setComments("Old comments");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		String sampleId = b.getSampleId();
		Assert.assertNotNull(b.getSampleId());
		Assert.assertNotNull(DAOBiosample.getBiosample(sampleId));

		Result r = new Result(DAOTest.getTest("Weighing"));
		r.setBiosample(b);
		r.setElb("MyTestElb");
		r.setValue(DAOTest.getTest("Weighing").getOutputAttributes().get(0), "10");
		DAOResult.persistResults(Collections.singletonList(r), user);

		// Delete
		EntityManager session = JPAUtil.getManager();
		try {
			session.getTransaction().begin();
			DAOResult.deleteResults(session, Collections.singleton(r), user);
			DAOBiosample.deleteBiosamples(session, Collections.singleton(b), user);
			session.getTransaction().commit();
		} catch (Exception e) {
			session.getTransaction().rollback();
		}

		Assert.assertNull(DAOBiosample.getBiosample(sampleId));

		// Load Revisions
		JPAUtil.clearAll();
		List<Revision> revisions = DAORevision.getRevisions(b);
		Assert.assertEquals(2, revisions.size());

		Revision rev = revisions.get(0);
		Assert.assertEquals(RevisionType.DEL, rev.getRevisionType());
		DAORevision.revert(rev, user, "Revert");
		b = DAOBiosample.getBiosample(sampleId);
		Assert.assertNotNull(sampleId + " not found", b);
		Assert.assertEquals("Old comments", b.getComments());

	}

	@Test
	public void testRevertCombo2() throws Exception {
		ExchangeTest.initDemoExamples(user);

		Study study = DAOStudy.queryStudies(StudyQuery.createForLocalId("IVV2016-2"), user).get(0);
		List<Revision> revisions = DAORevision.getRevisions(study);
		Assert.assertTrue(revisions.size() > 0);
		JPAUtil.pushEditableContext(user);

		study.setNotes("Test Combo2");
		DAOStudy.persistStudies(Collections.singleton(study), user);

		JPAUtil.closeFactory();
		// Load Revisions
		study = DAOStudy.queryStudies(StudyQuery.createForLocalId("IVV2016-2"), user).get(0);
		Assert.assertEquals("Test Combo2", study.getNotes());
		revisions = DAORevision.getRevisions(study);
		Assert.assertTrue(revisions.size() > 0);

		Revision rev = revisions.get(0);
		Assert.assertEquals(RevisionType.MOD, rev.getRevisionType());
		DAORevision.revert(rev, user, "Revert");
		study = DAOStudy.queryStudies(StudyQuery.createForLocalId("IVV2016-2"), user).get(0);
		Assert.assertNotNull(study);

		// Delete results
		List<Result> res = DAOResult.queryResults(ResultQuery.createQueryForSids(Collections.singleton(study.getId())), user);
		Assert.assertTrue(res.size() > 0);
		DAOResult.deleteResults(res, user);
		res = DAOResult.queryResults(ResultQuery.createQueryForSids(Collections.singleton(study.getId())), user);
		Assert.assertTrue("Loaded " + res, res.size() == 0);

		// Delete biosamples
		List<Biosample> bios = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForSids(Collections.singleton(study.getId())), user);
		Assert.assertTrue(bios.size() > 0);
		DAOBiosample.deleteBiosamples(bios, user);

		//Make sure deletion worked
		bios = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForSids(Collections.singleton(study.getId())), user);
		Assert.assertTrue("Loaded " + bios, bios.size() == 0);

		JPAUtil.closeFactory();

		//Load revisions and revert
		revisions = DAORevision.getRevisions(null, null, null, null, true, true, true, true, true);

		rev = revisions.get(0);
		Assert.assertEquals(RevisionType.DEL, rev.getRevisionType());
		Assert.assertTrue(rev.getBiosamples().size() > 0);
		DAORevision.revert(rev, user, "Revert");

		rev = revisions.get(1);
		Assert.assertEquals(RevisionType.DEL, rev.getRevisionType());
		Assert.assertTrue(rev.getResults().size() > 0);
		DAORevision.revert(rev, user, "Revert");

	}

	@Test
	public void testDocuments() throws Exception {
		// System.setProperty("show_sql", "true");
		File f = File.createTempFile("test_", ".txt");
		f.getParentFile().mkdirs();
		IOUtils.bytesToFile("Some file content".getBytes(), f);

		// Create Biotype
		Biotype biotype = new Biotype();
		biotype.setName("TestDocRev");
		biotype.setCategory(BiotypeCategory.LIBRARY);
		biotype.getMetadata().add(new BiotypeMetadata("file", DataType.D_FILE));
		biotype.getMetadata().add(new BiotypeMetadata("large", DataType.LARGE));
		DAOBiotype.persistBiotype(biotype, user);

		// Create Biosample
		Biosample b1 = new Biosample(biotype);
		b1.setMetadataDocument(biotype.getMetadata("file"), new Document(f));
		b1.setMetadataValue("large", "test1");
		DAOBiosample.persistBiosamples(Collections.singleton(b1), user);

		// Update Biosample
		b1.setMetadataValue("large", "test2");
		// b1.setDoc(Collections.singletonMap(biotype.getMetadata("file"), new
		// Document(f)));
		DAOBiosample.persistBiosamples(Collections.singleton(b1), user);

		// Retrieve Sample
		// JPAUtil.clear();
		b1 = DAOBiosample.getBiosample(b1.getSampleId());
		// Assert.assertNotNull(b1.getDoc());
		Assert.assertNotNull(b1.getMetadataDocument(biotype.getMetadata("file")));
		Assert.assertEquals("test2", b1.getMetadataValue(biotype.getMetadata("large")));

		// Retrieve versions
		List<Revision> revs = DAORevision.getRevisions(b1);
		Assert.assertEquals(2, revs.size());

		Biosample r1 = revs.get(0).getBiosamples().get(0);
		Biosample r2 = revs.get(1).getBiosamples().get(0);
		Assert.assertNotNull("File not retrievable in revision", r1.getMetadataDocument(biotype.getMetadata("file")));
		Assert.assertEquals("Invalid text", "test2", r1.getMetadataValue("large"));
		Assert.assertEquals("Invalid text", "test1", r2.getMetadataValue("large"));

	}

	@Test
	public void testRevertDocs() throws Exception {
		Biotype biotype = new Biotype("TestRevertDoc");
		biotype.setCategory(BiotypeCategory.PURIFIED);
		biotype.getMetadata().add(new BiotypeMetadata("doc", DataType.D_FILE));
		DAOBiotype.persistBiotype(biotype, user);

		//Create a sample with a doc and delete it
		Biosample b = new Biosample(biotype);
		b.setMetadataDocument(biotype.getMetadata("doc"), new Document("Test", "bytes".getBytes()));
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		DAOBiosample.deleteBiosamples(Collections.singleton(b), user);



		//Test revision
		List<Revision> revs = DAORevision.getRevisions(b);
		Assert.assertEquals(2, revs.size());
		Assert.assertEquals(RevisionType.DEL, revs.get(0).getRevisionType());

		Biosample r1 = revs.get(0).getBiosamples().get(0);
		Assert.assertEquals(biotype, r1.getBiotype());
		Assert.assertEquals("Test", new String(r1.getMetadataDocument(biotype.getMetadata("doc")).getFileName()));
		Assert.assertEquals("bytes", new String(r1.getMetadataDocument(biotype.getMetadata("doc")).getBytes()));

		//Restore
		DAORevision.restore(Collections.singleton(r1), user, "restored");
		List<Biosample> biosamples = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForSampleIdOrContainerIds(r1.getSampleId()), user);
		Assert.assertEquals(biotype, biosamples.get(0).getBiotype());
		Assert.assertEquals("Test", new String(biosamples.get(0).getMetadataDocument(biotype.getMetadata("doc")).getFileName()));
		Assert.assertEquals("bytes", new String(biosamples.get(0).getMetadataDocument(biotype.getMetadata("doc")).getBytes()));


	}

}
