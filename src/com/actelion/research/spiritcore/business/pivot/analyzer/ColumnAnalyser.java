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

package com.actelion.research.spiritcore.business.pivot.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.util.StatUtils;

public class ColumnAnalyser {
	
	public enum Distribution {
		NORMAL("Normal"),
		LOGNORMAL("Log-Normal"),
		UNIFORM("Uniform");
		
		private String name;
		
		private Distribution(String name) {
			this.name = name;
		}
		@Override
		public String toString() {
			return name;
		}
	}
	
	private final int index;
	
	private List<SimpleResult> simpleResults;
	private final List<Double> doubles;
	private int[] norBins;
	private int[] logBins;
	private Distribution distribution;
	private Double K;
	private int nGroups;
	
	/**
	 * Creates an analyzer and calculates statistics
	 * @param index
	 * @param keyValues a list of (key, value) typically (groupName, value)
	 */
	public ColumnAnalyser(int index, List<SimpleResult> simpleResults) {
		this.index = index;
		this.simpleResults = simpleResults;
		
		//Extract doubles and sort them
		doubles = SimpleResult.getValues(simpleResults);
		Collections.sort(this.doubles);
		 
		//Calculate Bins and distribution
		norBins = getBins(false, 5, 10);
		logBins = getBins(true, 5, 10);
		
		if(norBins[0]<norBins[2] && norBins[1]<=norBins[2] && norBins[3]<=norBins[2] && norBins[4]<norBins[2]) {
			distribution = Distribution.NORMAL; //centered at 2
		} else if(norBins[0]<norBins[1] && norBins[2]<=norBins[1] && norBins[3]<norBins[2] && norBins[4]<norBins[2]) {
			distribution = Distribution.NORMAL; //centered at 1
		} else if(norBins[0]<norBins[3] && norBins[1]<norBins[3] && norBins[2]<=norBins[3] && norBins[4]<=norBins[3]) {
			distribution = Distribution.NORMAL; //centered at 3
		} else if(Math.abs(norBins[0]-norBins[1])<=1 && Math.abs(norBins[0]-norBins[2])<=1 && Math.abs(norBins[0]-norBins[3])<=1 && Math.abs(norBins[0]-norBins[4])<=1 &&
				Math.abs(norBins[1]-norBins[2])<=1 && Math.abs(norBins[1]-norBins[3])<=1 && Math.abs(norBins[1]-norBins[4])<=1 && 
				Math.abs(norBins[2]-norBins[3])<=1 && Math.abs(norBins[2]-norBins[4])<=1 && Math.abs(norBins[3]-norBins[4])<=1) {
			distribution = Distribution.UNIFORM; 
		} else if(logBins[1]<=logBins[0] && logBins[2]<=logBins[0] && logBins[3]<=logBins[0] && logBins[4]<=logBins[0]) {
			distribution = Distribution.LOGNORMAL; //centered at 0
		} else if(logBins[0]<=logBins[1] && logBins[2]<=logBins[1] && logBins[3]<=logBins[1] && logBins[4]<=logBins[1]) {
			distribution = Distribution.LOGNORMAL; //centered at 1
		} else if(logBins[0]<=logBins[2] && logBins[1]<=logBins[2] && logBins[3]<=logBins[2] && logBins[4]<=logBins[2]) {
			distribution = Distribution.LOGNORMAL; //centered at 2
		} else if(logBins[0]<=logBins[3] && logBins[1]<=logBins[3] && logBins[2]<=logBins[3] && logBins[4]<=logBins[3]) {
			distribution = Distribution.LOGNORMAL; //centered at 3
		} else {
			distribution = null;
		}
		
		//Calculate KW. Group all values with the same key into a map
		Map<Group, List<Double>> map = SimpleResult.groupingValuesPerGroup(simpleResults); 
		
		//Transform the map->list<Double> to a List<double[]> to calculate KW
		int maxSize = 0;
		List<double[]> doublesList = new ArrayList<>();		
		for (Group key : map.keySet()) {
			List<Double> doubles = map.get(key);
			if(doubles.size()==0) continue;
			double[] a = new double[doubles.size()];
			for (int i = 0; i < a.length; i++) a[i] = doubles.get(i);			
			doublesList.add(a);
			maxSize = Math.max(maxSize, a.length);
		}		
		nGroups = doublesList.size();
		K = nGroups<=1 || maxSize<=4? null: StatUtils.getKruskalWallis(doublesList);
	}
	
	public List<SimpleResult> getSimpleResults() {
		return simpleResults;
	}
	
	public int getNGroups() {
		return nGroups;
	}
	
	public int getIndex() {
		return index;
	}
	
	public Double getKruskalWallis() {
		return K;
	}
	
	public int getN() {
		return doubles.size();
	}
	
	public Double getMin() {
		return doubles.get(0);
	}
	public Double getMax() {
		return doubles.get(doubles.size()-1);
	}
	
	public Double getMed() {
		return doubles.size()==0? null: doubles.size()%2==0? (doubles.get(doubles.size()/2-1) + doubles.get(doubles.size()/2)) / 2 : doubles.get((doubles.size()-1)/2);
	}
	
	public Double getAvg() {
		return StatUtils.getMean(doubles);
	}

	public String getBinsHisto(boolean log) {
		int[] b = log? logBins: norBins;
		String s = "";
		for(int i: b) s+= (char)('0'+i);
		return s;
	}
	
	public Distribution getDistribution() {
		return distribution;
	}

	
	/**
	 * @return
	 */
	private int[] getBins(boolean log, int nBins, int maxRange) {		
		int[] bins = new int[nBins];
		if(doubles.size()==0) return bins;
		if(getMax()==getMin()) return bins;
		double min = getMin();
		double max = getMax();

		if(log) {
			if(min<=0) return bins;
			min = Math.log(min);
			max = Math.log(max);
		}
		for (double d : doubles) {
			if(log) d = Math.log(d);
			int bin = (int)((d-min)/(max-min)*bins.length);
			if(bin>=bins.length) bin = bins.length-1;
			bins[bin]++;			
		}
		
		//normalize to [0-9[
		for (int i = 0; i < bins.length; i++) {
			bins[i] = (int)( .5 + bins[i] * (double)maxRange / getN());
			if(bins[i]>=maxRange) bins[i] = maxRange-1;
		}
		
		return bins;
	}
	
}
