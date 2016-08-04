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

package com.actelion.research.spiritcore.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;

public class StatUtils {

	
	/**
	 * Calculate Kruskal-Walis (http://en.wikipedia.org/wiki/Kruskal%E2%80%93Wallis_one-way_analysis_of_variance)
	 * @param values
	 * @return
	 */
	public static double getKruskalWallis(List<double[]> values) {
		
		
		//Assign ranks
		assert values.size()>1;
		
		List<Double> allDoubles = new ArrayList<Double>();
		for (double[] a : values) {
			assert a.length>0;
			for (double d : a) {
				allDoubles.add(d);
			}
		}
		int N = allDoubles.size();
		Collections.sort(allDoubles);
		double[] allDoublesArray = new double[allDoubles.size()];
		for (int i = 0; i < allDoubles.size(); i++) allDoublesArray[i] = allDoubles.get(i);
		List<double[]> ranks = new ArrayList<double[]>();
		for (double[] a : values) {
			double[] rankArray = new double[a.length];
			ranks.add(rankArray);
			for (int i = 0; i < a.length; i++) {
				int r = Arrays.binarySearch(allDoublesArray, a[i]);
				assert r>=0;
				int r1 = r, r2 = r;
				while(r1>0 && allDoublesArray[r1-1]==a[i]) r1--;
				while(r2<allDoublesArray.length-1 && allDoublesArray[r2+1]==a[i]) r2++;
				rankArray[i] = (r1+r2)/2.0 + 1; 
			}
		}
		
		//Calculate rank average per group
		List<Double> rankSum = new ArrayList<Double>();
		for (double[] a : ranks) {
			double sum = 0;
			for (double d: a) sum+=d;
			rankSum.add(sum);
		}
		
		
		
		double sum = 0;
		for (int i = 0; i < ranks.size(); i++) {
			sum += rankSum.get(i) * rankSum.get(i) / ranks.get(i).length;
		}
		double H = 12.0/(N*(N+1)) * sum - 3 * (N+1);
		
		ChiSquaredDistribution chi = new ChiSquaredDistribution(values.size()-1);
		double K = 1-chi.cumulativeProbability(H);

		return K;
	}
	
	
	public static Double getMedian(List<Double> doubles) {
		Collections.sort(doubles);
		if(doubles.size()==0) {
			return null;
		} else if(doubles.size()%2==0) {
			return (doubles.get(doubles.size()/2-1) + doubles.get(doubles.size()/2))/2;
		} else {
			return doubles.get(doubles.size()/2);
		}		
	}
	public static Double getMean(List<Double> doubles) {
		if(doubles.size()==0) {
			return null;
		} else {
			int i = 0;
			double sum = 0;
			for (Double d : doubles) {
				if(d==null) continue;
				sum += d;
				i++;
			}
			return i==0? null: sum/i;
		}		
	}
	public static double[] getFences(List<Double> doubles) {
		if(doubles.size()==0) return null;
		if(doubles.size()<=4) return null;
		
		//double mean = getMean(doubles);
		Collections.sort(doubles);
		double indexQ1 = (doubles.size())*.25;
		double indexQ3 = (doubles.size())*.75;
		
		
		double q1 =  doubles.get((int)indexQ1) + (indexQ1-(int)indexQ1) * (doubles.get((int)indexQ1+1)-doubles.get((int)indexQ1));
		double q3 =  doubles.get((int)indexQ3) + (indexQ3-(int)indexQ3) * (doubles.get((int)indexQ3+1)-doubles.get((int)indexQ3));
		double interquartile = q3-q1;
		double l3 = q1 - 3 * interquartile; 
		double l2 = q1 - 2 * interquartile;
		double l1 = q1 - 1 * interquartile;
		double h1 = q3 + 1 * interquartile;
		double h2 = q3 + 2 * interquartile;
		double h3 = q3 + 3 * interquartile; 
		
		return new double[] {l3, l2, l1, h1, h2, h3};
	}
	
	
	public static double getStandardDeviationOfMean(List<Double> doubles) {
		return getStandardDeviation(doubles, getMean(doubles));
	}
	public static Double getStandardDeviation(List<Double> doubles, Double mean) {
		double sum = 0;
		int i = 0;
		for (Double d : doubles) {
			if(d==null) continue;
			sum += (d-mean)*(d-mean);
			i++;
		}
		return i==0? null: Math.sqrt(sum/i);
	}
	
	public static void main(String[] args) {
//		List<Double> list = new ArrayList<Double>();
////		list.add(30d); list.add(171d); list.add(184d); list.add(201d); list.add(212d); list.add(250d); list.add(265d); list.add(270d); list.add(272d); list.add(289d); list.add(305d); list.add(306d); list.add(322d); list.add(322d); list.add(336d); list.add(346d); list.add(351d); list.add(370d); list.add(390d); list.add(404d); list.add(409d); list.add(411d); list.add(436d); list.add(437d); list.add(439d); list.add(441d); list.add(444d); list.add(448d); list.add(451d); list.add(453d); list.add(470d); list.add(480d); list.add(482d); list.add(487d); list.add(494d); list.add(495d); list.add(499d); list.add(503d); list.add(514d); list.add(521d); list.add(522d); list.add(527d); list.add(548d); list.add(550d); list.add(559d); list.add(560d); list.add(570d); list.add(572d); list.add(574d); list.add(578d); list.add(585d); list.add(592d); list.add(592d); list.add(607d); list.add(616d); list.add(618d); list.add(621d); list.add(629d); list.add(637d); list.add(638d); list.add(640d); list.add(656d); list.add(668d); list.add(707d); list.add(709d); list.add(719d); list.add(737d); list.add(739d); list.add(752d); list.add(758d); list.add(766d); list.add(792d); list.add(792d); list.add(794d); list.add(802d); list.add(818d); list.add(830d); list.add(832d); list.add(843d); list.add(858d); list.add(860d); list.add(869d); list.add(918d); list.add(925d); list.add(953d); list.add(991d); list.add(1000d); list.add(1005d); list.add(1068d); list.add(1441d); 
//		list.add(0.0); list.add(156.0); list.add(156.0); list.add(161.0); list.add(176.0); 
//		
//		double[] fences = getFences(list);
//		System.out.println("median="+getMedian(list));
//		System.out.println("std="+getStandardDeviationOfMean(list));
//		System.out.println("fences="+Arrays.toString(fences));
		
		


		int count = 0;
		int N = 1000;
		for(int t=0; t<N; t++) {
			List<double[]> doubles = new ArrayList<double[]>();
			for (int i = 0; i < 3; i++) {
				double[] array = new double[10];
				doubles.add(array);
				for (int j = 0; j < array.length; j++) {
					array[j] = Math.random()*10;// i<2 || j<10? j: j+100; 
				} 			
			} 
			double r = getKruskalWallis(doubles);
			if(r<0.05) count++;
			System.out.println("["+t+"] KW="+r);
		}
		System.out.println("==> percentage of false positive "+(1.0*count/N)+" ~ 0.05");

	}
	
}
