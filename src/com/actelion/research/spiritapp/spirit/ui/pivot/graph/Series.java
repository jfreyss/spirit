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

package com.actelion.research.spiritapp.spirit.ui.pivot.graph;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.actelion.research.spiritcore.business.pivot.analyzer.SimpleResult;

public class Series {
	
	private Color color = Color.GRAY;
	private String name;
	private List<SimpleResult> values = new ArrayList<>();
	private boolean numeric;
	
	public Series(String name, Color color, List<SimpleResult> values) {
		this.name = name;
		this.color = color;
		this.values = values;
		
		numeric = values.stream().anyMatch(v->v.getDoubleValue()!=null);
	}
	
	public boolean isNumeric() {
		return numeric;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Color getColor() {
		return color;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
		
	public List<SimpleResult> getValues() {
		return values;
	}
	
	public double getMin() {
		return values.stream().filter(v->v.getDoubleValue()!=null).mapToDouble(v->v.getDoubleValue()).min().orElseGet(()->0);
	}
	
	public double getMax() {
		return values.stream().filter(v->v.getDoubleValue()!=null).mapToDouble(v->v.getDoubleValue()).max().orElseGet(()->0);
	}
	
	public void setValues(List<SimpleResult> values) {
		this.values = values;
	}

	public List<String> getLabels() {
		return values.stream().map(v->v.getPhaseString()).collect(Collectors.toList());
	}
	
	/**
	 * Return an double[3] with the .25,.5,.75 percentiles 
	 */
	public double[] getFences(String phaseName) {
		double[] a = values.stream()
				.filter(v->v.getDoubleValue()!=null && ((phaseName==null && v.getPhase()==null) || (phaseName!=null && phaseName.equals(v.getPhaseString()))))
				.mapToDouble(v->v.getDoubleValue())
				.sorted()
				.toArray();
		return getFences(a); 
	}
	
	public Map<String, List<SimpleResult>> countValues(String phaseName) {
		return values.stream()
				.filter(v->v.getValue()!=null && ((phaseName==null && v.getPhase()==null) || (phaseName!=null && phaseName.equals(v.getPhaseString()))))
				.collect(Collectors.groupingBy(v->v.getValue(), Collectors.toList()));
	}
	
	/**
	 * Return 1st quartile, median, 3rd quartile
	 * @param a
	 * @return
	 */
	public static double[] getFences(double[] a) {
		int n = a.length;
		if(n<2) return null;
		double[] res = new double[3];
		if(n==2) {
			res[0]=a[0];
			res[1]=(a[0]+a[1])/2;
			res[2]=a[1];
		} else if(n%2==0) {
			res[1] = (a[n/2-1] + a[n/2])/2;
			res[0] = (n/2-1)%2==1? a[((n/2-1)-1)/2]: (a[(n/2-1)/2-1] + a[(n/2-1)/2])/2;
			res[2] = (n-n/2)%2==1? a[n/2+((n-n/2)-1)/2]: (a[n/2+(n-n/2)/2-1] + a[n/2+(n-n/2)/2])/2;
		} else {
			int median1 = (n-1)/2;
			int median2 = (n+1)/2;
			res[1] = a[n/2];
			res[0] = median1%2==1? a[(median1-1)/2]: (a[median1/2-1] + a[median1/2])/2;
			res[2] = (n-median2)%2==1? a[median2+((n-median2)-1)/2]: (a[median2+(n-median2)/2-1] + a[median2+(n-median2)/2])/2;
		}
//		res[1] = n%2==1? a[(n-1)/2]: (a[n/2-1] + a[n/2])/2;
//		res[0] = a[(n-1)/4]; //Method1
//		res[2] = a[3*n/4];   //Method1
		return res;
		
	}
	public static void main(String[] args) {
		System.out.println("Series.main() "+Arrays.toString(getFences(new double[]{ 6, 7, 15, 36, 39, 40, 41, 42, 43, 47, 49})));//[20.25, 40.0, 42.75]
		System.out.println("Series.main() "+Arrays.toString(getFences(new double[]{ 7, 15, 36, 39, 40, 41})));//[15.0, 37.5, 40.0]
		System.out.println("Series.main() "+Arrays.toString(getFences(new double[]{ 1, 2, 3, 4, 5, 6, 7, 8})));// [2.5, 4.5, 6.5]
		System.out.println("Series.main() "+Arrays.toString(getFences(new double[]{ 1, 2, 3, 4, 5, 6, 7})));// [2.0, 4, 6.0]
		System.out.println("Series.main() "+Arrays.toString(getFences(new double[]{ 1, 2, 3, 4, 5, 6})));// [2.0, 3.5, 5.0]
		System.out.println("Series.main() "+Arrays.toString(getFences(new double[]{ 1, 2, 3, 4, 5})));// [1.5, 3.0, 4.5]
		System.out.println("Series.main() "+Arrays.toString(getFences(new double[]{ 1, 2, 3, 4})));// [1.5, 2.5, 3.5]
		System.out.println("Series.main() "+Arrays.toString(getFences(new double[]{ 1, 2, 3})));// [1.0, 2.0, 3.00]
		System.out.println("Series.main() "+Arrays.toString(getFences(new double[]{ 1, 2})));// [1.0, 1.5, 2.00]
		System.out.println("Series.main() "+Arrays.toString(getFences(new double[]{ 1})));// null
	}
	
	@Override
	public String toString() {
		return name;
	}
}
