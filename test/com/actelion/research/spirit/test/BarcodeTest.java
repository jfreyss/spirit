package com.actelion.research.spirit.test;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.actelion.research.spiritcore.business.biosample.BarcodeSequence.Category;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.services.dao.DAOBarcode;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.util.MiscUtils;

public class BarcodeTest extends AbstractSpiritTest {

	@Test
	public void testSampleId() throws Exception {
		int year = Calendar.getInstance().get(Calendar.YEAR);
		int month = Calendar.getInstance().get(Calendar.MONTH);
		int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

		Assert.assertEquals("AA-" + (year%100) + "-", DAOBarcode.formatPattern("AA-{YY}-"));
		Assert.assertEquals("AA-" + year + "-", DAOBarcode.formatPattern("AA-{YYYY}-"));
		Assert.assertEquals("AA-" + year + (month<10?"0": "") + month + "-", DAOBarcode.formatPattern("AA-{YYYY}{MM}-"));
		Assert.assertEquals("AA-" + year + (month<10?"0": "") + month + (day<10?"0": "") + day + "-", DAOBarcode.formatPattern("AA-{YYYY}{MM}{DD}-"));

		String id1 = DAOBarcode.getNextId(Category.BIOSAMPLE, "ANL");
		String id2 = DAOBarcode.getNextId(Category.BIOSAMPLE, "HUM");
		String id3 = DAOBarcode.getNextId(Category.BIOSAMPLE, "ANL");
		String id4 = DAOBarcode.getNextId(Category.BIOSAMPLE, "ANL");
		String id5 = DAOBarcode.getNextId(Category.BIOSAMPLE, "HUM");
		Assert.assertEquals(Integer.parseInt(id3.substring(3)), Integer.parseInt(id1.substring(3))+1);
		Assert.assertEquals(Integer.parseInt(id4.substring(3)), Integer.parseInt(id1.substring(3))+2);
		Assert.assertEquals(Integer.parseInt(id5.substring(3)), Integer.parseInt(id2.substring(3))+1);
	}


	@Test
	public void testExamples() throws Exception {
		Calendar cal = Calendar.getInstance();
		String prefix = new DecimalFormat("0000").format(cal.get(Calendar.YEAR)) + "-" + new DecimalFormat("00").format(cal.get(Calendar.MONTH)) + "-" + new DecimalFormat("00").format(cal.get(Calendar.DAY_OF_MONTH));
		Assert.assertEquals(prefix + "-000001",DAOBarcode.getExample("{YYYY}-{MM}-{DD}-"));

		prefix = new DecimalFormat("00").format(cal.get(Calendar.YEAR)%100);
		Assert.assertEquals(prefix + ":000001",DAOBarcode.getExample("{YY}:"));

	}
	@Test
	public void testFixHole() throws Exception {
		//Persist a biotype
		Biotype biotype = new Biotype("TestBarcode");
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
		System.out.println("BarcodeTest.testFixHole() "+biosamples);

		Assert.assertEquals("TBa000001", biosamples.get(0).getSampleId());
		Assert.assertEquals("TBa000200", biosamples.get(199).getSampleId());



		//Create a misleading large sampleId, the auto generated id should not be affected
		Biosample b1 = new Biosample(biotype);
		b1.setSampleId("TBa9999");
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b1), user);

		Biosample b2 = new Biosample(biotype);
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b2), user);
		Assert.assertEquals("TBa000201", b2.getSampleId());


		//Create a correct larger sampleId, the auto generated id should continue from there (creating a sequence hole)
		b1 = new Biosample(biotype);
		b1.setSampleId("TBa009999");
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b1), user);

		b2 = new Biosample(biotype);
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b2), user);
		Assert.assertEquals("TBa010000", b2.getSampleId());

		//Delete the 2 large sampleIds, the next generated Id should fix the sequence hole
		DAOBiosample.deleteBiosamples(MiscUtils.listOf(b1, b2), user);
		b2 = new Biosample(biotype);
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b2), user);
		Assert.assertEquals("TBa000202", b2.getSampleId());


		//Create a overflow. The next Id should continue
		b1 = new Biosample(biotype);
		b1.setSampleId("TBa999999");
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b1), user);

		b2 = new Biosample(biotype);
		DAOBiosample.persistBiosamples(MiscUtils.listOf(b2), user);
		Assert.assertEquals("TBa1000000", b2.getSampleId());
	}


	/**
	 * Count the number of samples per biotype, or metadata
	 */
	@Test
	public void testCountRelations() {
		Biotype biotype = DAOBiotype.getBiotype("Animal");
		Assert.assertNotNull(biotype);
		Assert.assertNotNull(biotype.getMetadata("Type"));
		Assert.assertTrue(DAOBiotype.countRelations(biotype)>0);
		Assert.assertTrue(DAOBiotype.countRelations(biotype.getMetadata("Type"))>0);
	}

}
