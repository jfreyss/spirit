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

package com.actelion.research.spiritcore.test;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.actelion.research.spiritcore.services.StringEncrypter;
import com.actelion.research.spiritcore.util.MiscUtils;

import junit.framework.AssertionFailedError;

public class UtilsTest {
	
	
	@Test
	public void testMiscUtilsSerializeDeserializeIntegerMap() {
		//Simple case
		String s = "1=abc;2=defgh;20=ijkl";
		Map<Integer, String> map = MiscUtils.deserializeIntegerMap(s);		
		Assert.assertEquals(3, map.size());
		Assert.assertEquals(s, MiscUtils.serializeIntegerMap(map));
		
		
		//Check order
		String s2 = "1=abc;20=ijkl;2=defgh";
		Assert.assertEquals(s2, MiscUtils.serializeIntegerMap(MiscUtils.deserializeIntegerMap(s2)));
		
		//Check alternate tab
		String s3 = "1=abc\t20=ijkl\t2=defgh";
		Assert.assertEquals(s2, MiscUtils.serializeIntegerMap(MiscUtils.deserializeIntegerMap(s3)));
		
		//Check special chars tab
		String s4 = "1=a\\;\\\tbc;20=\\;ijkl\\;;2=defgh";
		Assert.assertEquals(3, MiscUtils.deserializeIntegerMap(s4).size());
		Assert.assertEquals(s4, MiscUtils.serializeIntegerMap(MiscUtils.deserializeIntegerMap(s4)));
	
		
		//ending ;
		Assert.assertEquals(1, MiscUtils.deserializeIntegerMap("1=a;").size());
		
		//error checks
		try {
			MiscUtils.deserializeIntegerMap("a=b;");
			throw new AssertionFailedError();
		} catch(Exception e) {			
		}
		
		try {
			MiscUtils.deserializeIntegerMap("1=b;;");
			throw new AssertionFailedError();
		} catch(Exception e) {			
		}
		
	}
	
	@Test
	public void testMiscUtilsSerializeDeserializeStringMap() {
		//Simple case
		String s = "meta1=abc;meta2=defgh;meta3=ijkl";
		Map<String, String> map = MiscUtils.deserializeStringMap(s);		
		Assert.assertEquals(3, map.size());
		Assert.assertEquals(s, MiscUtils.serializeStringMap(map));
		
		
		//Check order
		String s2 = "meta1=abc;meta3=ijkl;meta2=defgh";
		Assert.assertEquals(s2, MiscUtils.serializeStringMap(MiscUtils.deserializeStringMap(s2)));
		
		//Check special chars
		String s4 = "meta\\=1=abc;meta\\\\2=E=MC2;meta\\;3=\\;";
		Assert.assertEquals(3, MiscUtils.deserializeStringMap(s4).size());
		Assert.assertEquals("abc", MiscUtils.deserializeStringMap(s4).get("meta=1"));
		Assert.assertEquals("E=MC2", MiscUtils.deserializeStringMap(s4).get("meta\\2"));
		Assert.assertEquals(";", MiscUtils.deserializeStringMap(s4).get("meta;3"));
	
		
		//ending ;
		Assert.assertEquals(1, MiscUtils.deserializeStringMap("meta1=a;").size());
		
		//error checks
		try {
			MiscUtils.deserializeIntegerMap("a=b;");
			throw new AssertionFailedError();
		} catch(Exception e) {			
		}
		
		try {
			MiscUtils.deserializeIntegerMap("1=b;;");
			throw new AssertionFailedError();
		} catch(Exception e) {			
		}
		
	}
	
	@Test
	public void testEncryption() throws Exception {
		String crypted = new StringEncrypter("key").encrypt("my password".toCharArray());
		char[] decrypted = new StringEncrypter("key").decrypt(crypted);
		
		Assert.assertEquals("my password", new String(decrypted));
		
	}
	
	@Test
	public void testHtml() throws Exception {
		String s;
		s = MiscUtils.removeHtml("<h1>Header</h1><br><p>Body</b>");
		Assert.assertEquals("Header\nBody", s);
		
		s = MiscUtils.removeHtmlAndNewLines("<h1>Header</h1><br><p>Body</b>");
		Assert.assertEquals("Header Body", s);
		
		s = MiscUtils.removeHtml("<b att='test<>test'>Test</b>");
		Assert.assertEquals("Test", s);

		s = MiscUtils.removeHtml("<b att='test<>test' att2='test'>Test</b>");
		Assert.assertEquals("Test", s);

		s = MiscUtils.removeHtml("<b att=\"Test<>test\" att2='test'>Test</b>");
		Assert.assertEquals("Test", s);

		s = MiscUtils.removeHtml("<b att='test<>'>Some\n HTML</b><br>2");
		Assert.assertEquals("Some\n HTML\n2", s);
		
		s = MiscUtils.removeHtmlAndNewLines("<b att='test<>'>Some HTML</b><br>2");
		Assert.assertEquals("Some HTML 2", s);
		
		
	}
	
	
	
}
