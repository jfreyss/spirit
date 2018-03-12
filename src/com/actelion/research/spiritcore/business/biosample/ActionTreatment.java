/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.spiritcore.business.biosample;

import java.util.Arrays;
import java.util.List;

import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.util.MiscUtils;

/**
 * This is used to save the treatment applied to an animal
 * This class evolves from the beginning as it it not used anymore to "store" the weight but only to track what has been done.
 * @author freyssj
 *
 */
public class ActionTreatment extends ActionBiosample {

	private String formulation;
	private String treatmentName;
	private String phaseName;
	private String weightString;
	private String eff1, eff2;

	public ActionTreatment() {
	}

	public ActionTreatment(NamedTreatment treatment, Phase phase, Double weight, Double eff1, Double eff2, String formulation, String comments) {
		this(treatment==null? null: treatment.getName(), phase==null? null: phase.getShortName(), weight==null? null: weight.toString(), eff1==null? null: eff1.toString(), eff2==null? null: eff2.toString(), formulation, comments);
	}

	public ActionTreatment(String treatment, String phase, String weight, String eff1, String eff2, String formulation, String comments) {
		this.treatmentName = treatment;
		this.phaseName = phase;
		this.weightString = weight;
		this.eff1 = eff1;
		this.eff2 = eff2;
		this.formulation = formulation;
		this.comments = comments;
	}



	public String getFormulation() {
		return formulation;
	}

	public String getTreatmentName() {
		return treatmentName;
	}

	public String getPhaseName() {
		return phaseName;
	}

	public String getWeight() {
		return weightString;
	}

	public String getEff1() {
		return eff1;
	}

	public String getEff2() {
		return eff2;
	}

	@Override
	public String getDetails() {
		String s = (treatmentName!=null && treatmentName.length()>0? "Treatment: " + treatmentName : "Weighing:") +
				(weightString==null || weightString.length()==0?"": " " + weightString+"g ") +
				(phaseName==null? "": " at "+phaseName) +
				(treatmentName==null || treatmentName.length()==0? "":
					(eff1==null? "": " "+ eff1) + (eff2==null? "": " " + eff2) + (formulation==null? "": " " +  formulation)) +
				(getComments()!=null && getComments().length()>0? ": " + getComments(): "");
		return s;

	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof ActionTreatment)) return false;
		return getDetails().equals(((ActionTreatment) obj).getDetails());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String serialize() {
		return MiscUtils.serializeStrings(Arrays.asList(new String[]{
				"Treatment",
				treatmentName==null? "": treatmentName,
						phaseName==null?"": ""+phaseName,
								weightString==null? "": ""+weightString,
										eff1==null? "": ""+eff1,
												eff2==null? "": ""+eff2,
														formulation==null? "": ""+formulation,
																comments==null? "": comments}));
	}

	public static ActionTreatment deserialize(List<String> strings) {
		assert strings.size()>=8: "Expected size of 8: found"+strings.size();
		assert strings.get(0).equals("Treatment");
		return new ActionTreatment(strings.get(1), strings.get(2), strings.get(3), strings.get(4), strings.get(5), strings.get(6), strings.get(7));
	}

	@Override
	public String toString() {
		return "[ActionTreatment:" + getDetails() + "]";
	}
}
