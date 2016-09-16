package com.actelion.research.spiritcore.test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.envers.RevisionType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.HSQLMemoryAdapter;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;

public class RevisionTest {

	private static SpiritUser user;
	
	@BeforeClass
	public static void initDB() throws Exception {
		//Init user
		user = SpiritUser.getFakeAdmin();
		
		//Init DB
		DBAdapter.setAdapter(new HSQLMemoryAdapter());
		ExchangeTest.initDemoExamples(user);
		
	}
	
	@Test
	public void testRevertInsert() throws Exception {
		//Save a biosample
		Biosample b = new Biosample(DAOBiotype.getBiotype("Bacteria"));
		b.setMetadata("Resistances", "Ampicilin");
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
		
		//Load Revisions
		JPAUtil.clear();
		Assert.assertNull(DAOBiosample.getBiosample(sampleId));
		
	}
	
	@Test
	public void testRevertUpdate() throws Exception {
		//Save a biosample
		Biosample b = new Biosample(DAOBiotype.getBiotype("Bacteria"));
		b.setMetadata("Resistances", "Ampicilin");
		b.setComments("Old comments");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		String sampleId = b.getSampleId();
		Assert.assertNotNull(sampleId);
		Assert.assertNotNull(DAOBiosample.getBiosample(sampleId));
		
		//Update
		b.setComments("New comments");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		Assert.assertEquals("New comments", DAOBiosample.getBiosample(sampleId).getComments());
		
		//Load Revisions
		JPAUtil.clear();
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
		//Save a biosample
		Biosample b = new Biosample(DAOBiotype.getBiotype("Bacteria"));
		b.setMetadata("Resistances", "Ampicilin");
		b.setComments("Old comments");
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		String sampleId = b.getSampleId();
		Assert.assertNotNull(b.getSampleId());
		Assert.assertNotNull(DAOBiosample.getBiosample(sampleId));

		//Delete
		DAOBiosample.deleteBiosamples(Collections.singleton(b), user);
		Assert.assertNull(DAOBiosample.getBiosample(sampleId));
		
		//Load Revisions
		List<Revision> revisions = DAORevision.getRevisions(b);
		Assert.assertEquals(2, revisions.size());
		
		Revision rev = revisions.get(0);
		Assert.assertEquals(RevisionType.DEL, rev.getRevisionType());
		DAORevision.revert(rev, user, "Revert");
		b = DAOBiosample.getBiosample(sampleId);
		Assert.assertNotNull(sampleId+" not found", b);
		Assert.assertEquals("Old comments", b.getComments());
		
	}
	
	
	@Test
	public void testRevertCombo() throws Exception {
		//Save a biosample
		Biosample b = new Biosample(DAOBiotype.getBiotype("Bacteria"));
		b.setMetadata("Resistances", "Ampicilin");
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
		
		//Delete
		EntityManager session = JPAUtil.getManager();
		try {
			session.getTransaction().begin();
			DAOResult.deleteResults(session, Collections.singleton(r), user);
			DAOBiosample.deleteBiosamples(session, Collections.singleton(b), user);
			session.getTransaction().commit();
		} catch(Exception e) {
			session.getTransaction().rollback();
		}

		Assert.assertNull(DAOBiosample.getBiosample(sampleId));
		
		//Load Revisions
		JPAUtil.clear();
		List<Revision> revisions = DAORevision.getRevisions(b);
		Assert.assertEquals(2, revisions.size());
		
		Revision rev = revisions.get(0);
		Assert.assertEquals(RevisionType.DEL, rev.getRevisionType());
		DAORevision.revert(rev, user, "Revert");
		b = DAOBiosample.getBiosample(sampleId);
		Assert.assertNotNull(sampleId+" not found", b);
		Assert.assertEquals("Old comments", b.getComments());
		
	}
	
	@Test
	public void testRevertCombo2() throws Exception {
		ExchangeTest.initDemoExamples(user);
		
		Study study = DAOStudy.queryStudies(StudyQuery.createForLocalId("IVV2016-2"), user).get(0);
		List<Revision> revisions = DAORevision.getRevisions(study);
		Assert.assertTrue(revisions.size()>0);
		JPAUtil.pushEditableContext(user);

		study.setNotes("Test Combo2");
		DAOStudy.persistStudies(Collections.singleton(study), user);
		
		JPAUtil.close();
		//Load Revisions
		study = DAOStudy.queryStudies(StudyQuery.createForLocalId("IVV2016-2"), user).get(0);
		Assert.assertEquals("Test Combo2", study.getNotes());
		revisions = DAORevision.getRevisions(study);
		Assert.assertTrue(revisions.size()>0);
		
		
		Revision rev = revisions.get(0);
		Assert.assertEquals(RevisionType.MOD, rev.getRevisionType());
		DAORevision.revert(rev, user, "Revert");
		study = DAOStudy.queryStudies(StudyQuery.createForLocalId("IVV2016-2"), user).get(0);
		Assert.assertNotNull(study);
		
		//Delete results
		List<Result> res = DAOResult.queryResults(ResultQuery.createQueryForSids(Collections.singleton(study.getId())), user);
		Assert.assertTrue(res.size()>0);
		DAOResult.deleteResults(res, user);
		res = DAOResult.queryResults(ResultQuery.createQueryForSids(Collections.singleton(study.getId())), user);
		Assert.assertTrue("Loaded " + res, res.size()==0);
		
		//Delete biosamples
		List<Biosample> bios = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForSids(Collections.singleton(study.getId())), user);
		for (Biosample biosample : bios) {
			System.out.println("DAORevisionTest "+ biosample+" "+biosample.debugInfo());
		}
		Assert.assertTrue(bios.size()>0);
		DAOBiosample.deleteBiosamples(bios, user);
		
		bios = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForSids(Collections.singleton(study.getId())), user);
		Assert.assertTrue("Loaded " + bios, bios.size()==0);
		

		JPAUtil.close();
		revisions = DAORevision.getRevisions(null, new Date(), 1, true, true, true, true, true);
			
		rev = revisions.get(0);
		Assert.assertEquals(RevisionType.DEL, rev.getRevisionType());
		Assert.assertTrue(rev.getBiosamples().size()>0);
		DAORevision.revert(rev, user, "Revert");

		rev = revisions.get(1);
		Assert.assertEquals(RevisionType.DEL, rev.getRevisionType());
		Assert.assertTrue(rev.getResults().size()>0);
		DAORevision.revert(rev, user, "Revert");
		
	}
	
	
	
}
