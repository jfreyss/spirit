package com.actelion.research.spiritcore.test;

import java.util.Collections;
import java.util.Date;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.HSQLMemoryAdapter;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.biosample.ActionComments;
import com.actelion.research.spiritcore.business.biosample.ActionContainer;
import com.actelion.research.spiritcore.business.biosample.ActionLocation;
import com.actelion.research.spiritcore.business.biosample.ActionOwnership;
import com.actelion.research.spiritcore.business.biosample.ActionTreatment;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;

public class BiosampleTest {
	
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
	public void testBiotypes() throws Exception {
		int n = DAOBiotype.getBiotypes().size();
		Assert.assertTrue(n>0);
		
		//Save new biotype
		Biotype t = new Biotype();
		t.setCategory(BiotypeCategory.PURIFIED);
		t.setName("test");
		t.setSampleNameLabel(null);
		BiotypeMetadata mt1 = new BiotypeMetadata("m1", DataType.ALPHA);
		BiotypeMetadata mt2 = new BiotypeMetadata("m2", DataType.AUTO);
		BiotypeMetadata mt3 = new BiotypeMetadata("m3", DataType.NUMBER);
		BiotypeMetadata mt4 = new BiotypeMetadata("m4", DataType.D_FILE);
		BiotypeMetadata mt5 = new BiotypeMetadata("m5", DataType.BIOSAMPLE);
		t.getMetadata().add(mt1);
		t.getMetadata().add(mt2);
		t.getMetadata().add(mt3);
		t.getMetadata().add(mt4);
		t.getMetadata().add(mt5);
		DAOBiotype.persistBiotypes(Collections.singleton(t), user);
		
		Assert.assertEquals(n+1, DAOBiotype.getBiotypes().size());

		//Reload
		t = DAOBiotype.getBiotype("test");
		Assert.assertNotNull(t);
		Assert.assertNull(t.getSampleNameLabel());
		Assert.assertNotNull(t.getMetadata("m1"));
		
		//Move metadata to name
		DAOBiotype.moveMetadataToName(t.getMetadata("m1"), user);
		t = DAOBiotype.getBiotype("test");
		Assert.assertEquals("m1", t.getSampleNameLabel());
		Assert.assertNull(t.getMetadata("m1"));

		//Move name to metadata
		DAOBiotype.moveNameToMetadata(t, user);
		t = DAOBiotype.getBiotype("test");
		Assert.assertNull(t.getSampleNameLabel());
		Assert.assertNotNull(t.getMetadata("m1"));
		
		//Delete biotype
		DAOBiotype.deleteBiotype(t, user);
		Assert.assertEquals(n, DAOBiotype.getBiotypes().size());
		
		
		
	}
	@Test
	public void testQueries() throws Exception {
		BiosampleQuery q = new BiosampleQuery();
		q.setBiotype(DAOBiotype.getBiotype("Blood"));
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size()>0);
		
		q = new BiosampleQuery();
		q.setKeywords("Femur or Tibia");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size()>0);

		q = new BiosampleQuery();
		q.setKeywords("Femur Tibia");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size()==0);
		
		q = new BiosampleQuery();
		q.setKeywords("Femur 1");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size()>0);

		q = new BiosampleQuery();
		q.setKeywords("EDTA Rat");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size()>0);

		q = new BiosampleQuery();
		q.setBids(Collections.singleton(1));
		q.setComments("test");
		q.setComments("test");
		q.setContainerIds("toto");
		q.setContainerType(ContainerType.BOTTLE);
		q.setCreDays(5);
		q.setCreUser("test");
		q.setDepartment("test");
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
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size()==0);
			
		q = new BiosampleQuery();
		q.getLinker2values().put(new BiosampleLinker(DAOBiotype.getBiotype("Organ"), LinkerType.SAMPLENAME), "Lung");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size()>0);
		
		q = new BiosampleQuery();
		q.getLinker2values().put(new BiosampleLinker(DAOBiotype.getBiotype("Blood").getMetadata("Type")), "Heparin");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size()>0);
		
		q = new BiosampleQuery();
		q.getLinker2values().put(new BiosampleLinker(DAOBiotype.getBiotype("Animal"), DAOBiotype.getBiotype("Animal").getMetadata("Sex")), "M");
		q.getLinker2values().put(new BiosampleLinker(DAOBiotype.getBiotype("Blood").getMetadata("Type")), "Heparin");
		Assert.assertTrue(DAOBiosample.queryBiosamples(q, user).size()>0);
		
	}
	
	@Test
	public void testActions() throws Exception {
		
		
		Biosample a = DAOBiosample.getBiosample("ANL000036");
		
		//Test default status, (if none is set)
		Assert.assertNotNull(a);
		Assert.assertTrue(a.getStatus().isAvailable());
		Assert.assertTrue(!a.getLastActionStatus().getStatus().isAvailable());
		Assert.assertNotNull(a.getLastActionStatus().getStatus()==Status.NECROPSY);
		Assert.assertNotNull(a.getLastActionStatus().getBiosample()==a);
		Assert.assertNotNull("Planned necropsy".equals(a.getLastActionStatus().getComments()));
		
		//Test status
		a.setStatus(Status.DEAD, a.getInheritedStudy().getPhase("d0"));		
		DAOBiosample.persistBiosamples(Collections.singleton(a), user);
		
		//Reload
		a = DAOBiosample.getBiosample("ANL000036");		
		Assert.assertTrue(!a.getStatus().isAvailable());
		Assert.assertEquals(Status.DEAD, a.getLastActionStatus().getStatus());
		Assert.assertEquals("d0", a.getLastActionStatus().getPhase().getName());
		

		//Test with Plasma now
		Biosample b = DAOBiosample.getBiosample("PLA000003");
		Assert.assertNotNull(b);
		Assert.assertEquals(1, b.getActions(ActionLocation.class).size());
		int n = b.getActions().size();
		
		//Change status (only 1 recorded)
		b.setStatus(Status.USEDUP);
		b.setStatus(Status.TRASHED);
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		
		b = DAOBiosample.getBiosample("PLA000003");
		Assert.assertTrue(!b.getStatus().isAvailable());
		Assert.assertTrue(b.getStatus().getForeground()!=null);
		Assert.assertTrue(b.getStatus().getBackground()!=null);
		Assert.assertEquals(n+1, b.getActions().size());

		//An other status change
		b.setStatus(Status.LOWVOL);
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		b = DAOBiosample.getBiosample("PLA000003");
		Assert.assertTrue(b.getStatus().isAvailable());
		Assert.assertEquals(n+2, b.getActions().size());
				
		//Test location change
		Location loc = DAOLocation.getLocation(null, "InTransfer");
		Assert.assertNotNull(loc);
		b.setContainer(new Container(ContainerType.CRYOTUBE, "11"));
		b.setLocation(loc);
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		b = DAOBiosample.getBiosample("PLA000003");
		Assert.assertEquals(loc, b.getLocation());
		Assert.assertEquals(ContainerType.CRYOTUBE, b.getContainerType());
		Assert.assertEquals("11", b.getContainerId());
		Assert.assertEquals(2, b.getActions(ActionLocation.class).size());
		Assert.assertEquals(2, b.getActions(ActionContainer.class).size());
		
		//Test ownership
		DAOBiosample.changeOwnership(Collections.singleton(b), DAOSpiritUser.loadUser("admin"), user);
		b = DAOBiosample.getBiosample("PLA000003");
		Assert.assertEquals("admin", b.getCreUser());
		Assert.assertEquals(1, b.getActions(ActionOwnership.class).size());
		
		//Test Comment
		b.addAction(new ActionComments(b, "Test"));
		DAOBiosample.persistBiosamples(Collections.singleton(b), user);
		b = DAOBiosample.getBiosample("PLA000003");
		Assert.assertEquals(1, b.getActions(ActionComments.class).size());
		Assert.assertEquals(user.getUsername(), b.getActions(ActionComments.class).get(0).getUpdUser());
		
		//Test Treatment
		a = DAOBiosample.getBiosample("ANL000036");		
		a.addAction(new ActionTreatment(a, a.getInheritedStudy().getPhase("d0"), 100.0, null, null, null, null, "test"));
		DAOBiosample.persistBiosamples(Collections.singleton(a), user);
		a = DAOBiosample.getBiosample("ANL000036");
		Assert.assertEquals(1, a.getActions(ActionTreatment.class).size());
		Assert.assertEquals(100, a.getActions(ActionTreatment.class).get(0).getWeight(), 0.01);
		Assert.assertEquals("d0", a.getActions(ActionTreatment.class).get(0).getPhase().getName());
		
	}

	
}
