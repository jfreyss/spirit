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

package com.actelion.research.spiritcore.business.study;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;

/**
 * Must be immutable (to be sure to call setMeasurement before making any changes)
 * @author freyssj
 *
 */
public class Measurement implements Comparable<Measurement> {
	
	/**
	 * testId>0 except when writing the exchange file and then test!=null 
	 */
	private int testId;
	
	
	private String[] parameters;

	/**
	 * The test is loaded at runtime. Could be empty in case of errors
	 */
	private transient Test test;
	
	public Measurement(int testId, String[] parameters) {		
		assert testId>0;
		this.testId = testId;
		this.parameters = parameters;
	}
	
	public Measurement(Test test, String[] parameters) {		
		assert test!=null;
		this.test = test;
		this.testId = test.getId();
		this.parameters = parameters;
	}
	
	public String getDescription() {
		return (test==null?"?": test.getName()) +  (getParametersString()!=null || getParametersString().length()==0? "": " (" + getParametersString() +")" );
	}
	
	public int getTestId() {
		return testId;
	}
	
	public String[] getParameters() {
		return parameters;
	}
	public String getParametersString() {
		return MiscUtils.unsplit(getParameters(), ", ");
	}
	
	public Test getTest() {
		return test;
	}
	public void setTest(Test test) {
		this.test = test;
	}

	@Override
	public int hashCode() {
		return testId;
	}
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		Measurement p = (Measurement) obj;
		if(testId>0 && testId!=p.testId) return false;
		if(testId<=0 && test!=null && !test.equals(p.test)) return false;		
		return CompareUtils.compare(parameters, p.parameters)==0;
	}
	
	@Override
	public int compareTo(Measurement o) {
		if(o==null) return -1;
		
		int c = CompareUtils.compare(testId, o.testId);
		if(c!=0) return c;
		return CompareUtils.compare(parameters, o.parameters);
	}
	
	@Override
	public String toString() {
		return ("EM"+getTestId()) + (getParameters()!=null && getParameters().length>0? ": "+MiscUtils.unsplit(getParameters(), ", "): "");
	}
	
	public static Set<Integer> getTestIds(Collection<Measurement> col) {
		Set<Integer> ids = new HashSet<>();
		for (Measurement m : col) {
			ids.add(m.getTestId());
		}
		return ids;		
	}
	
	public static Set<Test> getTests(Collection<Measurement> col) {
		Set<Test> ids = new HashSet<>();
		for (Measurement m : col) {
			if(m.getTest()!=null) ids.add(m.getTest());
		}
		return ids;		
	}
	
	public static List<Measurement> deserialize(String measurementString) {
		//Deserializes measurements
		List<Measurement> res = new ArrayList<>();
		if(measurementString!=null) {
			String[] measurements = MiscUtils.split(measurementString, ",");
			for(String s: measurements) {
				String params[] = MiscUtils.split(s, "#");
				try {
					int testId = Integer.parseInt(params[0]);
					String[] array = new String[params.length-1];
					System.arraycopy(params, 1, array, 0, params.length-1);
					res.add(new Measurement(testId, array));
				} catch(Exception e) {
					System.err.println("Invalid measurement: "+measurementString);
				}			
			}
		}
		
		return res;		
	}
	
	public static String serialize(List<Measurement> extraMeasurementList) {
		String extraMeasurement;
		if(extraMeasurementList==null) {
			extraMeasurement = null;
		} else {
			String[] measurements = new String[extraMeasurementList.size()];
			for (int i = 0; i < extraMeasurementList.size(); i++) {
				Measurement e = extraMeasurementList.get(i);
				int id = e.getTestId();
				if(id<=0 && e.getTest()!=null) id = e.getTest().getId();
//				assert id>0;
				measurements[i] = id + "#" + MiscUtils.unsplit(e.getParameters(), "#");
			}
			extraMeasurement = MiscUtils.unsplit(measurements, ",");
		}
		return extraMeasurement;
	}
	
}
