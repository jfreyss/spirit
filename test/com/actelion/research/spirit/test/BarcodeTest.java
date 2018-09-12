package com.actelion.research.spirit.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOBarcode;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.MiscUtils;

public class BarcodeTest extends AbstractSpiritTest {

	@Test
	public void testSampleId() throws Exception {

		//Test generation of sequence among mixed biotypes
		Biotype t1 = new Biotype("Animal");
		t1.setPrefix("ANL");

		Biotype t2 = new Biotype("Human");
		t2.setPrefix("HUM");

		String id1 = DAOBarcode.getNextId(new Biosample(t1));
		String id2 = DAOBarcode.getNextId(new Biosample(t2));
		String id3 = DAOBarcode.getNextId(new Biosample(t1));
		String id4 = DAOBarcode.getNextId(new Biosample(t1));
		String id5 = DAOBarcode.getNextId(new Biosample(t2));
		Assert.assertEquals(Integer.parseInt(id3.substring(3)), Integer.parseInt(id1.substring(3))+1);
		Assert.assertEquals(Integer.parseInt(id4.substring(3)), Integer.parseInt(id1.substring(3))+2);
		Assert.assertEquals(Integer.parseInt(id5.substring(3)), Integer.parseInt(id2.substring(3))+1);

		//Test increment with a studyId
		Biotype t3 = new Biotype("Human");
		t3.setPrefix("{StudyId}-###-A");
		Biosample b1 = new Biosample(t3);
		try {
			DAOBarcode.getNextId(new Biosample(t3));
			throw new AssertionError("Exception when no study");
		} catch (Exception e) {
			//OK
		}
		b1.setInheritedStudy(new Study("S01"));
		Assert.assertEquals("S01-001-A", DAOBarcode.getNextId(b1));


		Biosample b2 = new Biosample(t3);
		b2.setInheritedStudy(new Study("S01"));
		Assert.assertEquals("S01-002-A", DAOBarcode.getNextId(b2));

		Biosample b3 = new Biosample(t3);
		b3.setInheritedStudy(new Study("S02"));
		Assert.assertEquals("S02-001-A", DAOBarcode.getNextId(b3));

	}


	@Test
	public void testExamples() throws Exception {
		int year = Calendar.getInstance().get(Calendar.YEAR);
		int month = Calendar.getInstance().get(Calendar.MONTH)+1;
		int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

		Assert.assertEquals("AA-" + (year%100) + "-000001", DAOBarcode.getExample("AA-{YY}-"));
		Assert.assertEquals("AA-" + year + "-000001", DAOBarcode.getExample("AA-{YYYY}-"));
		Assert.assertEquals("AA-" + year + (month<10?"0": "") + month + "-000001", DAOBarcode.getExample("AA-{YYYY}{MM}-"));
		Assert.assertEquals("AA-" + year + (month<10?"0": "") + month + (day<10?"0": "") + day + "-000001", DAOBarcode.getExample("AA-{YYYY}{MM}{DD}-"));
		Assert.assertEquals(year + "-" + (month<10?"0": "") + month + "-" + (day<10?"0": "") + day + "-000001", DAOBarcode.getExample("{YYYY}-{MM}-{DD}-"));
		Assert.assertEquals((year%100) + ":000001",DAOBarcode.getExample("{YY}:"));

		Assert.assertEquals("ABC-001-" + (year%100) , DAOBarcode.getExample("ABC-###-{YY}"));
		Assert.assertEquals("ABC-00001-" + (year%100) , DAOBarcode.getExample("ABC-#####-{YY}"));



	}


	/**
	 * Tests that the system continue the sequence if there is a gap between the last recorded sampleId in the sample table and in the barcode table
	 * @throws Exception
	 */
	@Test
	public void testFixHole() throws Exception {
		//Persist a biotype
		Biotype biotype = new Biotype("TestSampleId");
		biotype.setCategory(BiotypeCategory.LIQUID);
		biotype.setPrefix("TBa");
		DAOBiotype.persistBiotype(biotype, user);


		//Save 100 samples
		List<Biosample> biosamples = new ArrayList<>();
		for (int i = 0; i < 200; i++) {
			Biosample b = new Biosample(biotype);
			biosamples.add(b);
		}
		DAOBiosample.persistBiosamples(biosamples, user);
		Assert.assertEquals("TBa000001", biosamples.get(0).getSampleId());
		Assert.assertEquals("TBa000200", biosamples.get(199).getSampleId());


		//Create a large sampleId, with a wrong pattern
		Biosample b1 = new Biosample(biotype);
		b1.setSampleId("TBa9999");
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b1), user);

		//The auto generated id should not be affected
		Biosample b2 = new Biosample(biotype);
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b2), user);
		Assert.assertEquals("TBa000201", b2.getSampleId());


		//Create a correct larger sampleId, the auto generated id should continue from there (creating a sequence hole)
		b1 = new Biosample(biotype);
		b1.setSampleId("TBa009999");
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b1), user);

		b2 = new Biosample(biotype);
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b2), user);
		Assert.assertEquals("TBa010001", b2.getSampleId());

		//Delete the 2 larger sampleIds
		DAOBiosample.deleteBiosamples(MiscUtils.listOf(b1, b2), user);

		//,The next generated Id should fix the sequence hole
		b2 = new Biosample(biotype);
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b2), user);
		Assert.assertEquals("TBa000211", b2.getSampleId());


		//Create a overflow. The next Id should continue
		b1 = new Biosample(biotype);
		b1.setSampleId("TBa999999");
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b1), user);

		b2 = new Biosample(biotype);
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b2), user);
		Assert.assertEquals("TBa1000001", b2.getSampleId());
	}


	@Test
	public void testIgnoreUseBarcodeSequence() throws Exception {
		SpiritProperties.getInstance().setValue(PropertyKey.SYSTEM_USEBARCODESEQUENCE, "false");
		try {
			//Persist a biotype
			Biotype biotype = new Biotype("TestSampleId2");
			biotype.setCategory(BiotypeCategory.LIQUID);
			biotype.setPrefix("TBB");
			DAOBiotype.persistBiotype(biotype, user);


			//Save 45 samples
			List<Biosample> biosamples = new ArrayList<>();
			for (int i = 0; i < 45; i++) {
				Biosample b = new Biosample(biotype);
				biosamples.add(b);
			}
			DAOBiosample.persistBiosamples(biosamples, user);
			Assert.assertEquals("TBB000001", biosamples.get(0).getSampleId());
			Assert.assertEquals("TBB000045", biosamples.get(44).getSampleId());

			//Delete those samples
			DAOBiosample.deleteBiosamples(biosamples, user);

			JPAUtil.clear();
			//Recreate them
			biosamples = new ArrayList<>();
			for (int i = 0; i < 45; i++) {
				Biosample b = new Biosample(biotype);
				biosamples.add(b);
			}
			DAOBiosample.persistBiosamples(biosamples, user);


			//Test ids stats from 1 again (not use of the sequence)
			Assert.assertEquals("TBB000001", biosamples.get(0).getSampleId());
			Assert.assertEquals("TBB000045", biosamples.get(44).getSampleId());
		} finally {
			SpiritProperties.getInstance().setValue(PropertyKey.SYSTEM_USEBARCODESEQUENCE, "false");
		}



	}


	/**
	 * Tests that the system uses more digits, if the number of digits cannot hold all the samples
	 * @throws Exception
	 */
	@Test
	public void testOverflow() throws Exception {
		//Persist a biotype
		Biotype biotype = new Biotype("TestOverflow");
		biotype.setCategory(BiotypeCategory.LIQUID);
		biotype.setPrefix("TC-##C");
		DAOBiotype.persistBiotype(biotype, user);


		//Save 100 samples and check the overflow
		List<Biosample> biosamples = new ArrayList<>();
		for (int i = 0; i < 101; i++) {
			Biosample b = new Biosample(biotype);
			biosamples.add(b);
		}
		DAOBiosample.persistBiosamples(biosamples, user);
		Assert.assertEquals("TC-01C", biosamples.get(0).getSampleId());
		Assert.assertEquals("TC-99C", biosamples.get(98).getSampleId());
		Assert.assertEquals("TC-100C", biosamples.get(99).getSampleId());
		Assert.assertEquals("TC-101C", biosamples.get(100).getSampleId());

	}

}
