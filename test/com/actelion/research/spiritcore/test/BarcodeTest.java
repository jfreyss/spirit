package com.actelion.research.spiritcore.test;

import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;

import com.actelion.research.spiritcore.business.biosample.BarcodeSequence.Category;
import com.actelion.research.spiritcore.services.dao.DAOBarcode;

public class BarcodeTest extends AbstractSpiritTest {

	@Test
	public void testBarcode() throws Exception {
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
}
