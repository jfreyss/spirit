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
import java.util.LinkedList;
import java.util.List;

/**
 * The randomization is only used in the randomization process.
 * It should not be considered after animals have been assigned to the study. 
 * (except for adding animals from reserve)
 *  
 * @author freyssj
 *
 */
public class Randomization {
	
	private final Phase phase;
	private List<AttachedBiosample> samples = null;

	public Randomization(Phase phase, String rndSamples) {
		this.phase = phase;
		deserialize(phase, rndSamples);
	}
	
	/**
	 * Deserializes the samples, must be called before any other calls
	 * @param study
	 */
	protected void deserialize(Phase phase, String rndSamples) {
		samples = new ArrayList<>();
		if(rndSamples!=null && rndSamples.length()>0) {
			if(rndSamples.startsWith("&1\n")) {
				//Version 1
				String[] split = rndSamples.split("\n");
				for (int i=1; i<split.length; i++) {
					AttachedBiosample s = new AttachedBiosample();
					s.deserialize(phase.getStudy(), split[i], 1);
					samples.add(s);
				}			
			} else {
				//Older version
				for (String item : rndSamples.split("#")) {
					AttachedBiosample s = new AttachedBiosample();
					s.deserialize(phase.getStudy(), item, 0);
					samples.add(s);
				}		
			}
		}
	}
		
	/**
	 * Serialize the samples (Version 1)
	 */
	protected void serialize() {
		if(samples!=null && samples.size()>0) {
			StringBuilder sb = new StringBuilder();
			sb.append("&1\n");
				for (int i = 0; i < samples.size(); i++) {
				
				if(i>0) sb.append("\n");
				sb.append(samples.get(i).serialize());
			}		
			phase.setSerializedRandomization(sb.toString());
		} else {		
			phase.setSerializedRandomization(null);
		}
	}

	
	
	public int getNAnimals() {
		return getSamples()==null || getSamples().size()==0? 0: getSamples().size();
	}
	
	/**
	 * Update the size of the list of animals to the  specified number. Throws an exception if the number is reduced and a sample is not empty
	 * @param n
	 * @throws Exception 
	 */
	public void setNAnimals(int n) throws Exception {
		List<AttachedBiosample> rndSamplesList = getSamples();
		if(n<rndSamplesList.size()) {
			//Remove the extra samples
			while(n<rndSamplesList.size()) {
				AttachedBiosample s = rndSamplesList.get(rndSamplesList.size()-1);
				if(s.getSampleId()!=null && s.getSampleId().length()>0) {
					throw new Exception("The Sample "+rndSamplesList.size()+" is not empty");
				}
				rndSamplesList.remove(rndSamplesList.size()-1);
			}
		} else if(n>rndSamplesList.size()) {
			List<Integer> availableNos = new LinkedList<Integer>();
			for (int i = 1; i <= n; i++) availableNos.add(i);
			for(AttachedBiosample s: rndSamplesList) {
				availableNos.remove((Integer) s.getNo());
			}
			//Add the missing samples
			while(n>rndSamplesList.size()) {
				AttachedBiosample sample = new AttachedBiosample();
				sample.setNo(availableNos.remove(0));
				rndSamplesList.add(sample);
			}
		}		
		
	}
	
	public void setNData(int n) throws Exception {
		for (AttachedBiosample sample : getSamples()) {
			while(sample.getDataList().size()>n) sample.getDataList().remove(n);
			while(sample.getDataList().size()<n) sample.getDataList().add(null);
		}
	}
	 
	public int getNData() {
		int n = 0;
		for (AttachedBiosample rndSample : samples) {
			n = Math.max(n, rndSample.getDataList().size());
		}
		return n;
	}
	
	public List<AttachedBiosample> getSamples() {
		return samples;
	}
	
	public void setSamples(List<AttachedBiosample> samples) {
		if(samples==null) throw new IllegalArgumentException("The list cannot be null");
		this.samples = samples;
	}

}