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
	 * The format is expected to be &{versi
	 */
	protected void deserialize(Phase phase, String serializedRando) {
		samples = new ArrayList<>();
		if(serializedRando!=null && serializedRando.length()>0) {
			if(serializedRando.startsWith("&1\n")) {
				//Version 1
				String[] split = serializedRando.split("\n");
				for (int i=1; i<split.length; i++) {
					AttachedBiosample s = deserialize(phase.getStudy(), split[i]);
					samples.add(s);
				}
			} else {
				System.err.println("Cannot deserializes rando for phase "+phase+" in "+ phase.getStudy());
				return;
			}
		}
	}


	public AttachedBiosample deserialize(Study study, String s) {
		AttachedBiosample b = new AttachedBiosample();
		String[] split = s.split(":", -1); //split's limit <0 to return also empty strings
		//		if(versionNo==1) {
		int i = -1;
		//Version 1 of serialization
		try {
			b.setNo(Integer.parseInt(split[++i]));
			b.setSampleName(split[++i].length()==0? null: split[i]);
			b.setWeight(split[++i].length()==0? null: Double.parseDouble(split[i]));
			b.setSampleId(split[++i]);
			if(split[++i].length()==0) {
				b.setGroup(null);
			} else {
				b.setGroup(study.getGroup(Integer.parseInt(split[i])));
			}
			b.setContainerId(split[++i].length()==0? null: split[i]);
			b.setSubGroup(split[++i].length()==0? 0: Integer.parseInt(split[i]));

			for(int j = ++i; j<split.length; j++) {
				try {
					b.getDataList().add(Double.parseDouble(split[j]));
				} catch(Exception e) {
					b.getDataList().add(null);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		//		} else {
		//			//Older version of serialization (subgroup at the end, only one data)
		//			try {
		//				no = Integer.parseInt(split[0]);
		//				sampleName = split[1].length()==0? null: split[1];
		//				weight = split[2].length()==0? null: Double.parseDouble(split[2]);
		//				sampleId = split[3];
		//				if(split[4].length()==0) {
		//					group = null;
		//				} else {
		//					group = getGroup(study, Integer.parseInt(split[4]));
		//				}
		//				containerId = split[5].length()==0? null: split[5];
		//
		//				try {
		//					dataList.add(Double.parseDouble(split[6]));
		//				} catch(Exception e) {
		//
		//				}
		//
		//				subGroup = split.length<8 || split[7].length()==0? 0: Integer.parseInt(split[7]);
		//			} catch (Exception e) {
		//				e.printStackTrace();
		//			}
		//		}

		return b;
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
		assert samples!=null;
		this.samples = samples;
	}

	public void reset() {
		setSamples(new ArrayList<AttachedBiosample>());
	}

}