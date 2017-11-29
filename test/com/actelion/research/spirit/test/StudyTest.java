package com.actelion.research.spirit.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Randomization;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAONamedSampling;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.services.helper.BiosampleCreationHelper;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;

import junit.framework.AssertionFailedError;

/**
 * Elaborate Tests on study (import demo)
 * @author Joel Freyss
 *
 */
public class StudyTest extends AbstractSpiritTest {


	@BeforeClass
	public static void init() throws Exception {
		initDemoExamples(user);
	}

	@Test
	public void testCount() throws Exception {
		Study s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		DAOStudy.countRecentSamplesByBiotype(new Date());
		DAOStudy.countBiosampleAndResultsByPhase(s);
		DAOStudy.countResults(Collections.singleton(s), null);
		DAOStudy.countResultsByStudyTest(Collections.singleton(s));
		DAOStudy.countSamplesByStudyBiotype(Collections.singleton(s));
	}

	@Test
	public void testDuplicate() throws Exception {
		Study s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		Study s2 = s.duplicate();
		DAOStudy.persistStudies(Collections.singleton(s2), user);
		System.out.println("StudyTest.testDuplicate() "+s.getGroups()+" "+s2.getGroups());

		Assert.assertEquals(s.getNotes(), s2.getNotes());
		Assert.assertEquals(s.getPhases().size(), s2.getPhases().size());
		Assert.assertEquals(s.getGroups().size(), s2.getGroups().size());
		Assert.assertEquals(s.getStudyActions().size(), s2.getStudyActions().size());
	}

	@Test
	public void testNamedSamplings() throws Exception {
		int n = DAONamedSampling.getNamedSamplings(user, null).size();

		//Create sampling
		NamedSampling ns = new NamedSampling("MyNecro");
		Sampling s1 = new Sampling();
		s1.setBiotype(DAOBiotype.getBiotype("Blood"));
		s1.setAmount(0.5);
		s1.setMetadata(DAOBiotype.getBiotype("Blood").getMetadata("Type"), "EDTA");
		s1.setComments("test");
		s1.setContainerType(ContainerType.CRYOTUBE);
		Sampling s2 = new Sampling();
		s2.setBiotype(DAOBiotype.getBiotype("Blood"));
		s2.setAmount(1.0);
		ns.getAllSamplings().add(s1);
		ns.getAllSamplings().add(s2);


		//Save outside study
		DAONamedSampling.persistNamedSampling(ns, user);

		//Retrieve
		List<NamedSampling> nss = DAONamedSampling.getNamedSamplings(user, null);
		Assert.assertEquals(n+1, nss.size());

	}

	@Test
	public void testRando() throws Exception {
		//Load rando
		Study s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		Randomization rnd = s.getPhase("d0").getRandomization();
		Assert.assertEquals(17, s.getParticipantsSorted().size());
		Assert.assertEquals(17, rnd.getSamples().size());

		//Add an animal and save
		AttachedBiosample b = new AttachedBiosample();
		b.setNo(18);
		b.setSampleId("MyTest");
		b.setSampleName("20");
		rnd.getSamples().add(b);
		DAOStudy.persistStudies(Collections.singleton(s), user);


		//Reload
		s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		rnd = s.getPhase("d0").getRandomization();
		Assert.assertEquals(17, s.getParticipantsSorted().size());
		Assert.assertEquals(18, rnd.getSamples().size());

		DAOStudy.loadBiosamplesFromStudyRandomization(rnd);
		for(AttachedBiosample a: rnd.getSamples()) {
			Assert.assertNotNull(a.getBiosample());
			Assert.assertEquals(a.getSampleId(), a.getBiosample().getSampleId());
		}

	}

	@Test
	public void testPhases() throws Exception {
		Study s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		List<Phase> phases = new ArrayList<>(s.getPhases());
		Assert.assertTrue(phases.size()>0);

		Phase p = phases.get(0);
		Assert.assertEquals("d0", p.getName());
		Assert.assertEquals("Weighing + Treatments", p.getDescription());

		Phase p1 = phases.get(1);
		Assert.assertEquals("d1", p1.getName());
		Assert.assertEquals("Weighing + Blood Sampling", p1.getDescription());

		Phase p2 = phases.get(2);
		Assert.assertEquals("d2", p2.getName());
		Assert.assertEquals("Weighing + Blood Sampling", p2.getDescription());

		Phase p3 = phases.get(3);
		Assert.assertEquals("d3", p3.getName());
		Assert.assertEquals("Weighing + Blood Sampling", p3.getDescription());
	}

	@Test
	public void testGroups() throws Exception {

		//Test accessor for normal study
		Study s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		List<Group> groups = new ArrayList<>(s.getGroups());
		Assert.assertEquals(3, groups.size());

		Group g = groups.get(0);
		System.out.println("StudyTest.testGroups() "+g.getName()+ " "+g.getDescription(-1)+" "+g.getTreatmentDescription());
		Assert.assertEquals("1 Non Treated", g.getName());
		Assert.assertEquals("1", g.getShortName());
		Assert.assertEquals("Vehicle: Vehicle", g.getTreatmentDescription());
		Assert.assertTrue(g.getDescription(-1).startsWith("8weigh."));
		Assert.assertEquals(1, g.getNSubgroups());


		//Test accessor for blind study
		s.setBlindUsers(Collections.singleton(user.getUsername()), null);
		Assert.assertTrue(s.getBlindAllUsersAsSet().contains(user.getUsername()));
		Assert.assertEquals("Blinded", g.getBlindedName(user.getUsername() ));

		s.setBlindUsers(null, Collections.singleton(user.getUsername()));
		Assert.assertTrue(s.getBlindDetailsUsersAsSet().contains(user.getUsername()));
		Assert.assertEquals("Gr. 1", g.getBlindedName(user.getUsername() ));


	}


	@Test
	public void testCRUD() throws Exception {
		ExchangeTest.initDemoExamples(user);
		JPAUtil.clearAll();

		Study s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		Assert.assertTrue(s.getId()>0);
		Assert.assertTrue(s.getStudyId().length()>0);
		Assert.assertEquals(1, s.getPhasesWithGroupAssignments().size());
		int np = s.getPhases().size();
		int ng = s.getGroups().size();

		System.out.println("StudyTest.testUpdates() init with "+s.getPhases());

		//Add Phase, Group
		Phase p = new Phase("d21");
		s.getPhases().add(p);
		s.getPhases().add(new Phase("d15_10h"));

		Group g = new Group("Test");
		g.setStudy(s);
		g.setFromPhase(p);
		g.setSubgroupSizes(new int[]{2,2});
		s.getGroups().add(g);
		s.getOrCreateStudyAction(g, 0, p).setNamedSampling1(s.getNamedSamplings().iterator().next());
		Assert.assertEquals(np+2, s.getPhases().size());
		Assert.assertEquals(ng+1, s.getGroups().size());

		//Set Metadata
		for(String metadata: SpiritProperties.getInstance().getValues(PropertyKey.STUDY_METADATA)) {
			s.getMetadataMap().put(metadata, "test");
		}

		//Persist
		DAOStudy.persistStudies(Collections.singleton(s), user);

		//Rename and then remove phase:
		System.out.println("StudyTest.testUpdates() load1 with "+s.getPhases());
		s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		p = s.getPhase("d15_10h");
		p.setName("");
		p.remove();


		//And load
		System.out.println("StudyTest.testUpdates() load2 with "+s.getPhases());
		s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		p = s.getPhase("d21");
		g = s.getGroup("Test");
		Assert.assertTrue(s.getPhases().contains(p));
		Assert.assertTrue(s.getGroups().contains(g));
		Assert.assertEquals(np+1, s.getPhases().size());
		Assert.assertEquals(ng+1, s.getGroups().size());
		Assert.assertEquals(2, s.getPhasesWithGroupAssignments().size());
		Assert.assertNotNull(s.getGroup("Test").getFromPhase());
		Assert.assertEquals(1, s.getStudyAction(g, 0, p).getNamedSamplings().size());
		Assert.assertTrue(s.getStudyAction(g, 1, p)==null || s.getStudyAction(g, 1, p).getNamedSamplings().size()==0);
		for(String metadata: SpiritProperties.getInstance().getValues(PropertyKey.STUDY_METADATA)) {
			Assert.assertEquals("test", s.getMetadataMap().get(metadata));
		}

		//Remove phase
		Assert.assertNotNull(p);
		p.remove();
		Assert.assertEquals(p+" was not deleted in " + s.getPhases(), np, s.getPhases().size());
		DAOStudy.persistStudies(Collections.singleton(s), user);

		s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		g = s.getGroup(g.getName());

		Assert.assertEquals(ng+1, s.getGroups().size());
		Assert.assertEquals(np, s.getPhases().size());
		Assert.assertNull(s.getGroup("Test").getFromPhase());

		//Add 2 phases, and a sampling
		NamedSampling ns = new NamedSampling("Necro2");
		ns.setNecropsy(true);
		Sampling s1 = new Sampling();
		s1.setBiotype(DAOBiotype.getBiotype("Blood"));
		s1.setAmount(0.5);
		s1.setMetadata(DAOBiotype.getBiotype("Blood").getMetadata("Type"), "EDTA");
		s1.setComments("test");
		s1.setContainerType(ContainerType.CRYOTUBE);
		Sampling s2 = new Sampling();
		s2.setBiotype(DAOBiotype.getBiotype("Blood"));
		s2.setAmount(1.0);
		ns.getAllSamplings().add(s1);
		ns.getAllSamplings().add(s2);
		Phase p1 = new Phase("d28");
		Phase p2 = new Phase("d29");
		p1.setStudy(s);
		s.getPhases().add(p1);
		p2.setStudy(s);
		s.getPhases().add(p2);
		s.getGroup("Test").setFromGroup(s.getGroups().iterator().next());
		s.getGroup("Test").setFromPhase(p1);
		s.getOrCreateStudyAction(g, 0, p2).setNamedSampling1(ns);
		s.getOrCreateStudyAction(g, 0, p2).setNamedTreatment(s.getNamedTreatments().iterator().next());

		DAOStudy.persistStudies(Collections.singleton(s), user);

		//Reload
		s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		p2 = s.getPhase(p2.getName());
		g = s.getGroup(g.getName());
		Assert.assertEquals(ng+1, s.getGroups().size());
		Assert.assertEquals(np+2, s.getPhases().size());
		Assert.assertNotNull(s.getStudyAction(g, 0, p2));
		Assert.assertEquals(1, s.getStudyAction(g, 0, p2).getNamedSamplings().size());
		Assert.assertEquals(2, s.getStudyAction(g, 0, p2).getNamedSamplings().iterator().next().getAllSamplings().size());
		Assert.assertEquals("EDTA", s.getStudyAction(g, 0, p2).getNamedSamplings().iterator().next().getAllSamplings().iterator().next().getMetadataValues());
		Assert.assertEquals("test", s.getStudyAction(g, 0, p2).getNamedSamplings().iterator().next().getAllSamplings().iterator().next().getComments());
		Assert.assertEquals(ContainerType.CRYOTUBE, s.getStudyAction(g, 0, p2).getNamedSamplings().iterator().next().getAllSamplings().iterator().next().getContainerType());
		Assert.assertEquals(0.5, s.getStudyAction(g, 0, p2).getNamedSamplings().iterator().next().getAllSamplings().iterator().next().getAmount(), 0.01);


		//Create samples before attaching (-> no new samples)
		List<Biosample> bios = BiosampleCreationHelper.processTemplateInStudy(s, null, null, null, null);
		int toCreate = 0; for (Biosample b : bios) {
			System.out.println("StudyTest.testUpdates() > "+b.getInfos());
			if(b.getId()<=0) toCreate++;
		}
		Assert.assertEquals(255, bios.size());
		Assert.assertEquals(0, toCreate);
		Assert.assertFalse(JPAUtil.getManager().getTransaction().isActive());

		//Move one animal and add one animal to g
		Biosample a1 = s.getParticipantsSorted().iterator().next();
		a1.setAttached(s, g, 0);

		Biosample a2 = new Biosample(DAOBiotype.getBiotype("Animal"));
		a2.setAttached(s, g, 0);
		DAOBiosample.persistBiosamples(Arrays.asList(new Biosample[]{a1,a2}), user);

		//Create samples after attaching: -> new samples
		bios = BiosampleCreationHelper.processTemplateInStudy(s, null, null, null, null);
		toCreate = 0; for (Biosample b : bios) {
			if(b.getId()<=0) {
				toCreate++;
				System.out.println("StudyTest.testUpdates() "+toCreate+" > "+b.getInfos());
			}
		}
		Assert.assertEquals(276, bios.size());
		Assert.assertEquals(19+7, toCreate);


		//Try rollback
		List<Revision> revisions = DAORevision.getLastRevisions(s);
		Revision rev = revisions.get(0);
		DAORevision.revert(rev, user, "test");

		//Try Restore
		rev = revisions.get(revisions.size()-1);
		DAORevision.restore(rev.getStudies(), user, "test");

		//Try reload
		s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		DAOStudy.fullLoad(s);
		Assert.assertEquals(ng, s.getGroups().size());
		Assert.assertEquals(np, s.getPhases().size());

		//Try delete
		try {
			DAOStudy.deleteStudies(Collections.singleton(s), false, user);
			throw new AssertionFailedError("Should throw an exception because of existing samples");
		} catch (Exception e) {
			//OK
		}

		DAOStudy.deleteStudies(Collections.singleton(s), true, user);
		Assert.assertEquals(0, DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").size());

		//Reset
		ExchangeTest.initDemoExamples(user);

	}

	@Test
	public void testQueries() throws Exception {
		StudyQuery q = new StudyQuery();
		q.setState("EXAMPLE");
		Assert.assertTrue(DAOStudy.queryStudies(q, user).size()>0);

		q = new StudyQuery();
		q.setKeywords("Treated");
		Assert.assertTrue(DAOStudy.queryStudies(q, user).size()>0);

		q = new StudyQuery();
		q.setUpdDays(1);
		Assert.assertTrue(DAOStudy.queryStudies(q, user).size()>0);

		q = new StudyQuery();
		q.setCreDays(5);
		q.setKeywords("toto");
		q.setLocalIds("dd");
		q.setRecentStartDays(5);
		q.setStudyIds("dd");
		q.setUpdDays(5);
		q.setUser("ff");
		Assert.assertTrue(DAOStudy.queryStudies(q, user).size()==0);

		//Test biotypes/containers
		Study s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		Assert.assertTrue(DAOStudy.getBiotypes(s).size()>0);
		Assert.assertTrue(DAOStudy.getContainerTypes(s).size()>0);


		//Test metadata
		for(String metadata: SpiritProperties.getInstance().getValues(PropertyKey.STUDY_METADATA)) {
			DAOStudy.getMetadataValues(metadata);
		}
	}

	@Test
	public void testCreateSamplesInStudy() throws Exception {
		//Check samples are already created
		Study s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		int nTop = s.getParticipantsSorted().size();
		List<Biosample> biosamples = BiosampleCreationHelper.processTemplateInStudy(s, null, null, null, null);
		int n = biosamples.size();
		int toCreate = 0;
		for (Biosample b : biosamples) {
			if(b.getId()<=0) toCreate++;
		}
		Assert.assertEquals(0, toCreate);

		//Delete results
		DAOResult.deleteResults(DAOResult.queryResults(ResultQuery.createQueryForSids(Collections.singleton(s.getId())), user), user);

		//Delete samples (except tops)
		biosamples = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForSids(Collections.singleton(s.getId())), user);
		biosamples.removeAll(s.getParticipantsSorted());
		Assert.assertTrue(biosamples.size()>0);
		DAOBiosample.deleteBiosamples(biosamples, user);

		//Check deletion
		biosamples = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForSids(Collections.singleton(s.getId())), user);
		Assert.assertEquals(nTop, biosamples.size());

		//Create samples
		biosamples = BiosampleCreationHelper.processTemplateInStudy(s, null, null, null, null);
		Assert.assertEquals(n, biosamples.size());
		toCreate = 0;
		for (Biosample b : biosamples) {
			if(b.getId()<=0) toCreate++;
		}
		Assert.assertEquals(n, toCreate);
		DAOBiosample.persistBiosamples(biosamples, user);

		//Check creation
		biosamples = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForSids(Collections.singleton(s.getId())), user);
		Assert.assertEquals(n+nTop, biosamples.size());

		//Recheck template
		biosamples = BiosampleCreationHelper.processTemplateInStudy(s, null, null, null, null);
		Assert.assertEquals(n, biosamples.size());
		toCreate = 0;
		for (Biosample b : biosamples) {
			if(b.getId()<=0) toCreate++;
		}
		Assert.assertEquals(0, toCreate);

		for (Biosample b : biosamples) {
			Assert.assertTrue(b.getAttachedSampling()!=null);
		}

	}

	@Test
	public void testCreateSamplesOutStudy() throws Exception {
		Study s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		NamedSampling ns = s.getNamedSamplings().iterator().next();

		List<Biosample> biosamples = s.getParticipants(s.getGroups().iterator().next());

		List<Biosample> samples = BiosampleCreationHelper.processTemplate(null, ns, biosamples, true);
		Assert.assertEquals(ns.getAllSamplings().size()*biosamples.size(), samples.size());
		int toCreate = 0;
		for (Biosample b : samples) {
			if(b.getId()<=0) toCreate++;
		}
		Assert.assertEquals(samples.size(), toCreate);

		DAOBiosample.persistBiosamples(samples, user);
		for (Biosample b : samples) {
			Assert.assertTrue(b.getAttachedSampling()==null);
		}

	}

	@Test
	public void testAttachedSamples() throws Exception {
		Study s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		Biosample previous = null;

		//Check correctess of order
		for(Biosample b: s.getParticipantsSorted()) {
			System.out.println("StudyTest.testAttachedSamples() "+b+" "+b.getInheritedGroupString(null)+" "+b.getInheritedSubGroup()+" "+ b.getSampleName());
			if(previous!=null) {
				Assert.assertTrue(b.getInheritedGroup()==null
						|| b.getInheritedGroup().compareTo(previous.getInheritedGroup())>0
						|| (b.getInheritedGroup().compareTo(previous.getInheritedGroup())==0 && CompareUtils.compare(b.getSampleName(), previous.getSampleName())>=0));
			}
			previous = b;
		}
	}

	@Test
	public void testAttachedResults() throws Exception {

		Study s = DAOStudy.getStudyByLocalIdOrStudyIds("IVV2016-1").get(0);
		List<Biosample> samples = new ArrayList<>();
		for (Biosample b : s.getParticipantsSorted()) {
			samples.addAll(b.getHierarchy(HierarchyMode.ATTACHED_SAMPLES));
		}
		DAOResult.attachOrCreateStudyResultsToTops(s, s.getParticipantsSorted(), null, null);
		DAOResult.attachOrCreateStudyResultsToSamples(s, samples, null, null);

		for (Biosample b : s.getParticipantsSorted()) {
			Assert.assertTrue(b.getAuxResults().size()>0);
		}
		int nRes = 0;
		for (Biosample b : samples) {
			nRes += b.getAuxResults().size();
		}
		Assert.assertTrue(nRes>0);
	}



	@Test
	public void testCloningParticipantsInSameStudy() throws Exception {
		Study s = new Study();
		DAOStudy.persistStudies(Collections.singleton(s), user);

		//Create 2 samples with same ID: error expected
		Biosample b1 = new Biosample(DAOBiotype.getBiotype("Animal"));
		b1.setSampleId("Cl1");
		b1.setAttachedStudy(s);
		Biosample b2 = new Biosample(DAOBiotype.getBiotype("Animal"));
		b2.setSampleId("Cl1");
		b2.setAttachedStudy(s);

		try {
			DAOBiosample.persistBiosamples(MiscUtils.listOf(b1,b2), user);
			throw new AssertionFailedError("Error of duplicated sampleId should have been thrown");
		} catch (Exception e) {
			//OK
		}

		//Clone and save: OK
		List<Biosample> toSave = DAOStudy.cloneParticipants(Collections.singletonList(b2), Collections.singletonList(b1));
		Assert.assertEquals("Cl1A", b1.getSampleId());
		Assert.assertEquals(s, b1.getAttachedStudy());

		Assert.assertEquals("Cl1B", b2.getSampleId());
		Assert.assertEquals(s, b2.getAttachedStudy());

		Assert.assertEquals(b2.getParent(), b1.getParent());
		Assert.assertEquals(null, b2.getParent().getAttachedStudy());
		DAOBiosample.persistBiosamples(toSave, user);

	}

	@Test
	public void testCloningParticipantsInOtherStudy() throws Exception {
		Study s1 = new Study();
		Study s2 = new Study();
		DAOStudy.persistStudies(MiscUtils.listOf(s1, s2), user);

		//Create 2 samples with same ID in 2 different studies: error expected
		Biosample b1 = new Biosample(DAOBiotype.getBiotype("Animal"));
		b1.setSampleId("Cl2");
		b1.setSampleName("Name1");
		b1.setAttachedStudy(s1);
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b1), user);

		Biosample b2 = new Biosample(DAOBiotype.getBiotype("Animal"));
		b2.setSampleId("Cl2");
		b2.setAttachedStudy(s2);

		try {
			DAOBiosample.persistBiosamples(MiscUtils.listOf(b2), user);
			throw new AssertionFailedError("Error of duplicated sampleId should have been thrown");
		} catch (Exception e) {
			//OK
		}

		//Clone
		List<Biosample> toSave = DAOStudy.cloneParticipants(Collections.singletonList(b2), Collections.singletonList(b1));
		Assert.assertEquals("Cl2A", b1.getSampleId());
		Assert.assertEquals(s1, b1.getAttachedStudy());

		Assert.assertEquals("Cl2B", b2.getSampleId());
		Assert.assertEquals(s2, b2.getAttachedStudy());

		Assert.assertEquals(b2.getParent(), b1.getParent());
		Assert.assertEquals("Name1", b1.getParent().getSampleName());
		Assert.assertEquals(null, b2.getParent().getAttachedStudy());

		for (Biosample biosample : toSave) {
			System.out.println("StudyTest.testCloningParticipantsInOtherStudy() "+biosample.getId()+"> "+biosample.getSampleId());
		}

		DAOBiosample.persistBiosamples(toSave, user);

	}


	@Test
	public void testGroupPhaseCreationFromBiosample() throws Exception {
		Study s = new Study();
		DAOStudy.persistStudies(MiscUtils.listOf(s), user);

		Biosample b1 = new Biosample(DAOBiotype.getBiotype("Animal"));
		b1.setSampleId("A01");
		b1.setAttachedStudy(s);
		b1.setInheritedGroup(new Group("A"));
		b1.setInheritedPhase(new Phase("Begin"));

		Biosample b2 = new Biosample(DAOBiotype.getBiotype("Animal"));
		b2.setSampleId("A02");
		b2.setAttachedStudy(s);
		b2.setInheritedGroup(new Group("B"));
		b2.setInheritedPhase(new Phase("Begin"));

		Biosample b3 = new Biosample(DAOBiotype.getBiotype("Animal"));
		b3.setSampleId("A03");
		b3.setAttachedStudy(s);
		b3.setInheritedGroup(new Group("B"));
		b3.setInheritedPhase(new Phase("End"));


		//Saving biosamples should be refused, without saving first the study
		try {
			//			DAOBiosample.persistBiosamples(MiscUtils.listOf(b1, b2), user);
			//			throw new AssertionFailedError("Study should be saved first");
		} catch (Exception e) {
			//OK
		}

		//This should work
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b1, b2, b3), user);
		Assert.assertEquals(2, s.getGroups().size());
		Assert.assertNotNull(s.getGroup("A"));
		Assert.assertTrue(s.getGroup("A").getId()>0);

		Assert.assertEquals(2, s.getPhases().size());
		Assert.assertNotNull(s.getPhase("Begin"));
		Assert.assertTrue(s.getPhase("Begin").getId()>0);

	}


	@Test
	public void testDifferences() throws Exception {

		//Test infos
		Study s1 = new Study();
		s1.setTitle("My Title 1");
		s1.getMetadataMap().put("Site", "onsite");
		s1.getMetadataMap().put("Experimenter", "Employee2");

		Study s2 = new Study();
		s2.setTitle("My Title 2");
		s2.getMetadataMap().put("Site", "onsite");
		s2.getMetadataMap().put("Experimenter", "Employee1");
		s2.getMetadataMap().put("Old", "previous");

		Assert.assertEquals("Title=My Title 1; Experimenter=Employee2; Old=", s1.getDifference(s2));



		//Test Group Name change
		s1.getGroups().add(new Group("G1"));
		s1.getGroups().add(new Group("G2"));
		s1.getPhases().add(new Phase("P1"));
		s1 = DAOStudy.persistStudies(Collections.singleton(s1), user).get(0);

		s2 = s1.clone();
		s2.getGroups().iterator().next().setName("G3");
		Assert.assertEquals("Groups=updated", s2.getDifference(s1));


		s1 = DAOStudy.persistStudies(Collections.singleton(s1), user).get(0);
		s2 = s1.clone();
		s2.getPhases().iterator().next().getRandomization().setNAnimals(3);
		s2.getPhases().iterator().next().serializeRandomization();
		Assert.assertEquals("Phases=modified P1", s2.getDifference(s1));

	}

	@Test
	public void testOwnership() throws Exception {
		Study s = DAOStudy.getStudies().get(0);
		DAOStudy.changeOwnership(s, new SpiritUser("TEST"), user);
		s = DAOStudy.getStudy(s.getId());

		Assert.assertEquals("TEST", s.getCreUser());
	}

	@Test
	public void testUniqueId() throws Exception {
		String sid = DAOStudy.getNextStudyId();
		Study s1 = new Study();
		s1.setStudyId(sid);

		Study s2 = new Study();
		s2.setStudyId(sid);

		//Persisting 2 studies with same id is impossible
		try {
			DAOStudy.persistStudies(MiscUtils.listOf(s1, s2), user);

			throw new AssertionFailedError("Should not be possible");
		} catch (Exception e) {
			//OK: Note: always clear session after an exception.
			JPAUtil.clear();

		}
		s1 = JPAUtil.reattach(s1);
		s2 = JPAUtil.reattach(s2);
		System.out.println("StudyTest.testUniqueId() "+s1.getId());
		Assert.assertTrue(s1.getId()<=0);
		Assert.assertTrue(s2.getId()<=0);

		s2.setStudyId(null);
		DAOStudy.persistStudies(MiscUtils.listOf(s1, s2), user);



	}


}
