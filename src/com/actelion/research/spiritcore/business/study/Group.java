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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.util.Counter;
import com.actelion.research.util.CompareUtils;

/**
 * 
 */
@Entity
@Audited
@Table(name="study_group", indexes = {@Index(name="group_study_index", columnList = "study_id")})
@SequenceGenerator(name="group_sequence", sequenceName="group_sequence", allocationSize=1)
@BatchSize(size=16)
public class Group implements Comparable<Group>, Cloneable {

	public static final String DISEASE_NAIVE = "Naive";
	public static final String DISEASE_SHAM =  "Sham";
	public static final String DISEASE_DISEASED = "Diseased";
	
	public static final String TREATMENT_NONTREATED = "Non Treated";
	public static final String TREATMENT_VEHICLE =  "Vehicle";
	public static final String TREATMENT_COMPOUND = "Compound";
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="group_sequence")
	private int id = 0;
	
	/**
	 * Unique within each study
	 */
	@Column(nullable = false)
	private String name = "";
	

	@ManyToOne(fetch=FetchType.LAZY)
	private Study study = null;
		
	/**
	 * Color of the group used for display (rgb color)
	 */
	@Column(name="color")	
	private Integer colorRgb = null;
	
	/**
	 * Uses when this group comes from another group, either when:
	 * - a "sick" group is splitted into "sick treated" and "sick untreated"
	 * - samples are created from a group and assigned to this new subgroup  
	 */
	@ManyToOne(cascade=CascadeType.PERSIST, fetch=FetchType.LAZY, optional=true)
	@JoinColumn(name="randofromgroup_id")
	private Group fromGroup;
	
	/**
	 * Used when this group comes from another group, and when the animal is divided into samples.
	 * For example, at d7, we treat 4 fragments of skin and extract cells over several days
	 */
	@OneToOne(cascade=CascadeType.ALL, fetch=FetchType.LAZY, optional=true) //bug with orphanRemoval = true (http://stackoverflow.com/questions/15167911/orphanremoval-true-bidirectional-onetoone)
	@JoinColumn(name="dividingsample_id")
	private Sampling dividingSampling;

	@ManyToOne(cascade=CascadeType.PERSIST, fetch=FetchType.LAZY, optional=true)
	@BatchSize(size=100)
	@JoinColumn(name="randophase_id")	
	private Phase fromPhase;
	
	/**
	 * The subgroups are stored flat. Ex: 5,5,5 means we have 3 subgroups  of size 5.
	 * This column gives therefore the number of subgroup and the number of specimen per subgroup (0=not defined)*/
	@Column(name="stratificationgroups", length=64)
	private String subgroupSizeFlat;	
	
	
	/**
	 * Naive, Sham, Disease:###
	 */
	@Column(name="diseaseModel", length=128)
	private String diseaseModel;  

	/**
	 * Non Treated, Vehicle, Compound:###
	 */
	@Column(name="treatmentModel", length=128)
	private String treatmentModel;  
	
	/**Cache: EndPhase indicates the last operation on this group, if there is a necropsy*/	
	private transient Phase[] endPhases = null;
	
	/**Cache: subgroupSizeArray indicates the size of each group (corresponding to subgroupSizeFlat), 0 means undefined number of animals*/	
	private transient int[] subgroupSizeArray = null;
	

	
	public Group() {}
	
	public Group(String name) {this.name = name;}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public String getBlindedName(String user) {
		if(study==null) {
			return getName()==null?"":getName();
		} else if(user==null || study.getBlindAllUsers().contains(user)) {
			return "Blinded";
		} else if(study.getBlindDetailsUsers().contains(user)) {
			return "Gr. " + getShortName();
		} else {
			return getName()==null?"":getName();
		}
	}
	
	public Color getBlindedColor(String user) {		
		if(study==null) {
			return getColor();
		} else if(user==null || study.getBlindAllUsers().contains(user)) {
			return Color.LIGHT_GRAY;
		} else if(study.getBlindDetailsUsers().contains(user)) {
			return getColor();
		} else {
			return getColor();
		}
		
	}



	/**
	 * Get the part before the first space
	 * @return
	 */
	public String getShortName() {
		if(name==null) return "";
		int index = name.indexOf(" ");
		if(index<=0) index = name.length();
		if(index>4) index = 4;
		return name.substring(0, index).trim();
	}
	
	public String getNameWithoutShortName() {
		if(name==null) return "";
		int index = name.indexOf(" ");
		if(index<=0) index = name.length();
		if(index>4) index = 4;
		return index<name.length()? name.substring(index).trim(): "";
	}
	
	/**
	 * Set the name
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * Set the name
	 * @param name
	 */
	public void setName(String shortName, String nameWithoutShortName) {
		if(shortName==null) shortName = "";
		if(nameWithoutShortName==null) nameWithoutShortName = "";
		this.name = (shortName.trim() + " " + nameWithoutShortName).trim();
	}


	public Study getStudy() {
		return study;
	}

	public void setId(int id) {
		this.id = id;
	}


	public void setStudy(Study study) {
		if(this.study==study) return;

		if(this.study!=null) {
			this.study.getGroups().remove(this);
		}
		
		this.study = study;
		
		if(this.study!=null) {
			this.study.getGroups().add(this);
		}
	}

	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this==obj) return true;
		if(!(obj instanceof Group)) return false;
		if(getId()>0 && getId()==((Group)obj).getId()) return true;
		if(getId()<=0 && getShortName().equals(((Group)obj).getShortName())) return true;
		return false;
	}
	
	@Override
	public int hashCode() {
		return (int)(getId()%Integer.MAX_VALUE);
	}

	
	@Override
	public int compareTo(Group o) {
		if(o==null) return 1;
		if(o==this) return 0;
		
		int c = CompareUtils.compare(getStudy(), o.getStudy());
		if(c!=0) return c;
		
		c = getShortName().compareToIgnoreCase(o.getShortName());
		if(c!=0) return c;
		
		return (int) ((getId()-o.getId()) % Integer.MAX_VALUE);
	}
	
	
	@Override
	protected Group clone() {
		try {
			return (Group) super.clone();	
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Return the initial number of animals
	 * @return
	 */
	public int getNAnimals() {
		int total = 0;
		for (int s : getSubgroupSizes()) {
			total+=s;
		}
		return total;
	}
	
	/**
	 * Return the number of animals expected at the given phase.
	 * This function is equal to getNAnimals, except if the group splits and some animals go to an other group
	 * @param phase
	 * @return
	 */
	public int getNAnimals(Phase phase) {
		if(phase==null) return getNAnimals();
		int n = getNAnimals(); 
		for(Group gr: getToGroups()) {
			if(gr.getFromPhase()!=null && gr.getFromPhase().compareTo(phase)<=0) {
				n-=gr.getNAnimals();
			}
		}
		return n;
	}
	

	public void remove() {	
		if(getStudy()==null || getStudy().getGroups()==null) return;
		
		this.fromGroup = null;
		this.fromPhase = null;

		//Remove actions of this group
		for (StudyAction a: new ArrayList<StudyAction>(getStudy().getStudyActions())) {
			if(this.equals(a.getGroup())) {
				a.remove();
			}
		}
		setStudy(null);
	}
		
	public String getTreatmentDescription() {
		return getTreatmentDescription(-1, false);
	}
	
	public String getTreatmentDescription(int subgroup) {
		return getTreatmentDescription(subgroup, false);
	}
	
	public Set<NamedTreatment> getAllTreatments(int subgroup) {
		Set<NamedTreatment> res = new LinkedHashSet<>();
		for (StudyAction a : subgroup<0? study.getStudyActions(this): study.getStudyActions(this, subgroup)) {
			if(a.getNamedTreatment()!=null) {
				res.add(a.getNamedTreatment());
			}
		}
		return res;
	}
	
	/**
	 * Get the description of the treatment for the given subgroup (if >=0)
	 * @return
	 */
	public String getTreatmentDescription(int subgroup, boolean fullTreatments) {
		List<NamedTreatment> treatments = new ArrayList<>(getAllTreatments(subgroup));		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < treatments.size(); i++) {
			if(!fullTreatments && treatments.size()>3 && i>=2) {
				sb.append(" +..."); 
				break;
			}
			if(sb.length()>0) {
				sb.append(" +");
			}
			
			sb.append( treatments.get(i).getNameWithCompoundAndUnit());
		}
		return sb.toString();
	}
	
	
	public static String extractGroupABC(String s) {
		if(s==null) return null;
		if(s.indexOf(' ')>=1) s = s.substring(0, s.indexOf(' '));
		int index = 0;
		for (;index<s.length() && !Character.isLetter(s.charAt(index)); index++) {}
		
		int index2 = index;
		for (;index2<s.length() && Character.isLetter(s.charAt(index2)); index2++) {}
		return s.substring(index, index2);
	}

	public static String extractGroup123(String s) {
		if(s==null) return null;
		if(s.indexOf(' ')>=1) s = s.substring(0, s.indexOf(' '));

		int index = 0;
		for (;index<s.length() && !Character.isDigit(s.charAt(index)); index++) {}
		int index2 = index;
		for (;index2<s.length() && Character.isDigit(s.charAt(index2)); index2++) {}

		return s.substring(index, index2);
	}
	
	
	public static Color generateColor(String name) {
		if(name==null || name.length()==0) return Color.WHITE;
		
		String shortGroup = Group.extractGroup123(name); 
		
		float h = 0;
		for (int i = shortGroup.length()-1; i >=0 ; i--) {
			int v = 1 + shortGroup.charAt(i);
			h = (h + v/8f)%1;
		}
		
		float s = .25f;
		float b = .85f;
		Color col = Color.getHSBColor(h, s, b);
		
		return col;
	}
	
	private transient Color color = null;
	
	/**
	 * Background color of this group. It cannot be null
	 * @return
	 */
	public Color getColor() {
		if(color==null) {
			if(colorRgb==null) {
				return Color.LIGHT_GRAY;
			}
			
			//Make the color lighter if needed
			int value = colorRgb;
			int r = (value & 0xFF0000)>>16;
			int g = (value & 0xFF00)>>8;
			int b = (value & 0xFF);
			
			if(r+g+b<500) {
				int inc = (500- (r+g+b))/3;
				r = Math.min(255, r+inc);
				g = Math.min(255, g+inc);
				b = Math.min(255, b+inc);
			}
			color = new Color(r, g, b);
		}
		return color;
	}
	
	public Integer getColorRgb() {
		return colorRgb;
	}
	
	public void setColorRgb(Integer colorRgb) {
		this.color = null;
		this.colorRgb = colorRgb;
	}
	
	public String getDiseaseModel() {
		return diseaseModel;
	}

	public void setDiseaseModel(String diseaseModel) {
		this.diseaseModel = diseaseModel;
	}

	public String getTreatmentModel() {
		return treatmentModel;
	}

	public void setTreatmentModel(String treatmentModel) {
		this.treatmentModel = treatmentModel;
	}

	/**
	 * If this group will be later split into new groups, this function will return all phases where this occurs
	 * @return
	 */
	public List<Phase> getNewGroupingPhases() {
		List<Phase> res = new ArrayList<Phase>();
		for (Group gr : getToGroups()) {
			res.add(gr.getFromPhase());
		}
		return res;
	}

	public Group getFromGroup() {
		return fromGroup;
	}

	public void setFromGroup(Group fromGroup) {
		this.fromGroup = fromGroup;
	}

	public Phase getFromPhase() {
		return fromPhase;
	}

	public void setFromPhase(Phase randoPhase) {
		this.fromPhase = randoPhase;
	}

	
	
	public Phase getEndPhase(int subGroupNo) {
		if(getStudy()==null) return null;

		if(endPhases!=null && subGroupNo>=0 && subGroupNo<endPhases.length) {
			return endPhases[subGroupNo];
		} else {
			Phase[] endPhases = new Phase[getNSubgroups()];
			for(int i=0; i<getNSubgroups(); i++) {
				
				for(StudyAction a: study.getStudyActions(this, i)) {
					if((a.getNamedSampling1()!=null && a.getNamedSampling1().isNecropsy()) || (a.getNamedSampling2()!=null && a.getNamedSampling2().isNecropsy())) {
						endPhases[i] = a.getPhase();
						break;
					}
				}
			}
			this.endPhases = endPhases;
			return subGroupNo>=0 && subGroupNo<endPhases.length? endPhases[subGroupNo]: null;
		}
	
	}

	public int getNSubgroups() {
		return getSubgroupSizes().length;
	}
	
	public void addSubgroup() {
		
		int[] sizes = getSubgroupSizes();

		//Update subgroup
		int[] newSizes = new int[sizes.length+1];
		System.arraycopy(sizes, 0, newSizes, 0, sizes.length);
		setSubgroupSizes(newSizes);
			
		//duplicate actions from the last group
		List<StudyAction> actions = new ArrayList<>();
		for(StudyAction a : study.getStudyActions(this, sizes.length-1)) {
			StudyAction a2 = new StudyAction(a);
			a2.setSubGroup(sizes.length);					
			actions.add(a2);
		}
		study.addStudyActions(actions);		
	}
	
	/**
	 * The subgroups are stored in the db in GROUP.subgroupSizeFlat as csv (Ex: 5,5,5)
	 * this function returns the corresponding array: subgroup 0 -> size of subgroup, ...
	 * @return
	 */
	public int[] getSubgroupSizes() {
		if(subgroupSizeArray!=null) {
			return subgroupSizeArray;
		} else {
			//Check if we have no subgroup -> array with the nAnimals
			if(subgroupSizeFlat==null || subgroupSizeFlat.length()==0) {
				return new int[] {0}; //no animals by default for new groups
			} else {		
				try {
					String s[] = subgroupSizeFlat.split(",",-1);
					int[] res = new int[s.length];
					for (int i = 0; i < res.length; i++) {
						try {
							res[i] = s[i].length()==0? 0: Integer.parseInt(s[i]);
						} catch(Exception e) {
							res[i] = 0;
						}
					}
					subgroupSizeArray = res;
					return res;
				} catch(Exception e) {
					e.printStackTrace();
					return new int[] {0};
				}
			}
		}
	}
	
	/**
	 * The subgroups are stored in the db in GROUP.subgroupSizeFlat as csv (Ex: 5,5,5)
	 * the size of the array is equal to the number of subgroups.
	 * @return
	 */
	public void setSubgroupSizes(int[] a) {
		subgroupSizeArray = null;
		if(a==null || a.length==0) {
			subgroupSizeFlat = null;
		} else {
			String s = "";
			for (int i : a) s += (s.length()>0?",":"") + i;
			subgroupSizeFlat = s;
		}
	}
	
	public int getSubgroupSize(int subgroupno) {
		int[] sizes = getSubgroupSizes();
		if(subgroupno<0 || subgroupno>=sizes.length) throw new IllegalArgumentException("Invalid subgroupNo: "+subgroupno+" for "+this);
		return sizes[subgroupno];
	}

	public Set<Biosample> getTopAttachedBiosamples(){
		return getStudy()==null? new HashSet<Biosample>(): Collections.unmodifiableSet(getStudy().getTopAttachedBiosamples(this));
	}
	
	public Sampling getDividingSampling() {
		return dividingSampling;
	}
	
	public void setDividingSampling(Sampling dividingSample) {
		this.dividingSampling = dividingSample;
	}
	
	/**
	 * Gets all splitting SubGroups g such as g.getFromGroup()==this and g.getDividingSampling()==null
	 * @return
	 */
	public List<Group> getToGroups() {
		List<Group> res = new ArrayList<Group>();
		if(getStudy()==null) return res;
		for (Group g : getStudy().getGroups()) {
			if(this.equals(g.getFromGroup())) {
				if(g.getDividingSampling()==null) res.add(g);
			}
		}
		return res;
	}
	
	/**
	 * Gets all dividing SubGroups g such as g.getFromGroup()==this and g.getDividingSampling()!=null
	 * @return
	 */
	public List<Group> getDividingGroups() {
		List<Group> res = new ArrayList<Group>();
		if(getStudy()==null) return res;
		for (Group g : getStudy().getGroups()) {
			if(this.equals(g.getFromGroup())) {
				if(g.getDividingSampling()!=null) res.add(g);
			}
		}
		return res;
	}
	
	/**
	 * Copy the animals, the fromgroup/phase, subgroups, color, dividingsample from the given group.
	 * The developer still needs to set the study to do something useful with this.
	 * @param sel
	 */
	public void copyFrom(Group sel) {
		setName(sel.getName());
		setDividingSampling(sel.getDividingSampling()==null? null: sel.getDividingSampling().clone());
		setFromGroup(sel.getFromGroup());
		setFromPhase(sel.getFromPhase());
		setSubgroupSizes(sel.getSubgroupSizes());
		setColorRgb(sel.getColorRgb());		
	}
	
	public void resetCache() {
		endPhases = null;
		subgroupSizeArray = null;
	}
	
	/**
	 * Generates a description based on the number of treatment and samplings
	 * @param subgroup (-1 to get a description for the group in general, subgroupNo for the given subgroup)
	 * @return
	 */
	public String getDescription(int subgroup) {
		String[] desc;
		if(subgroup<0) {
			desc = getDescriptionLines(0);				
			for(int i=1; i<getNSubgroups(); i++) {
				String[] desc2 = getDescriptionLines(i);
				for (int j = 0; j < desc2.length; j++) {
					if(!desc[j].equals(desc2[j])) desc[j] = "";
				}
				
			}
		} else {
			desc = getDescriptionLines(subgroup);
		}
		
		//concatenate the description
		StringBuilder sb = new StringBuilder();  					
		for (String s : desc) {
			if(s!=null && s.length()>0) sb.append(s + "\n");
		}
		return sb.toString();

		
	}

	/**
	 * Util function to get a description of the treatment
	 * @param subgroup
	 * @return
	 */
	public String[] getDescriptionLines(int subgroup) {
		if(getStudy()==null) return new String[]{"","",""};
		Counter<NamedTreatment> treatmentCounter = new Counter<>();
		Counter<NamedSampling> samplingCounter = new Counter<>();
		Counter<Measurement> measurementCounter = new Counter<>();
		int nWeighings = 0;
		int nFoods = 0;
		int nWaters = 0;
		StringBuilder sMeasurements = new StringBuilder();
		String sTreatments = "";
		String sSamplings = "";
		
		//Find the actions, including the actions from the parent
		List<StudyAction> actions = new ArrayList<StudyAction>();
		actions.addAll(getStudy().getStudyActions(this, subgroup));
		
		Group g = getFromGroup();
		while(g!=null && g.getNSubgroups()<=1) {
			actions.addAll(study.getStudyActions(g, 0));
			g = g.getFromGroup();			
		}
		
		
		for(StudyAction a: actions) {
			if(a.isMeasureWeight()) nWeighings++;
			if(a.isMeasureFood()) nFoods++;
			if(a.isMeasureWater()) nWaters++;
			
			if(a.getNamedTreatment()!=null) treatmentCounter.increaseCounter(a.getNamedTreatment());
			if(a.getNamedSampling1()!=null) samplingCounter.increaseCounter(a.getNamedSampling1());
			if(a.getNamedSampling2()!=null) samplingCounter.increaseCounter(a.getNamedSampling2());
			for(Measurement m: a.getMeasurements()) {
				measurementCounter.increaseCounter(m);
			}
		}

		if(nWeighings>0) sMeasurements.append((sMeasurements.length()>0?", ":"") + nWeighings + "weigh.");
		if(nFoods>0) sMeasurements.append((sMeasurements.length()>0?", ":"") +  + nFoods + "Food");
		if(nWaters>0) sMeasurements.append((sMeasurements.length()>0?", ":"") +  + nWaters + "Water");
		
		for(Measurement t: measurementCounter.getKeys()) {
			sTreatments = "+ " + (measurementCounter.getCount(t)>1? measurementCounter.getCount(t)+"x ":"") + t.getDescription();
		}
		for(NamedTreatment t: treatmentCounter.getKeys()) {
			sTreatments = "+ " + (treatmentCounter.getCount(t)>1? treatmentCounter.getCount(t)+"x ":"") + t.getName();
		}
		for(NamedSampling s: samplingCounter.getKeys()) {
			sSamplings = "- " + (samplingCounter.getCount(s)>1? samplingCounter.getCount(s)+"x ":"") + s.getName();
		}
		
		return new String[] {sMeasurements.toString(), sTreatments, sSamplings};
	}	

	public void removeSubgroup(int subgroup) throws Exception {
		
		//Change subgroupsizes
		int[] sizes = getSubgroupSizes();
		if(subgroup<0 || subgroup>=sizes.length) throw new Exception("Invalid subgroup: "+subgroup);
		
		if(study.getTopAttachedBiosamples(this, subgroup).size()>0) throw new Exception("The group " + this +"'"+(1+subgroup) + " has "+study.getTopAttachedBiosamples(this, subgroup).size()+" attached samples");
		
		int[] newSizes = new int[sizes.length-1];
		for (int i = 0; i < newSizes.length; i++) {
			newSizes[i] = sizes[i + (i>=subgroup?1:0)];
		}
		setSubgroupSizes(newSizes);
		
		//Delete actions
		study.removeStudyActions(study.getStudyActions(this, subgroup));
		
		//Move actions		
		for(StudyAction a: study.getStudyActions(this)) {
			if(a.getSubGroup()>subgroup) {
				a.setSubGroup(a.getSubGroup()-1);
			}
		}
		
		
		//Move biosamples
		for (Biosample top : study.getTopAttachedBiosamples(this)) {
			if(top.getInheritedSubGroup()<subgroup) continue;
			for(Biosample b: top.getHierarchy(HierarchyMode.ATTACHED_SAMPLES)) {
				b.setInheritedSubGroup(b.getInheritedSubGroup()-1);
			}
				
		}		
		
	}
	
	public void moveUp(int subgroup) {
		int[] sizes = getSubgroupSizes();
		if(subgroup<1 || subgroup>=sizes.length) throw new IllegalArgumentException("Invalid subgroup: "+subgroup);
		
		//switch subgroupsizes
		int tmp = sizes[subgroup-1];
		sizes[subgroup-1] = sizes[subgroup];
		sizes[subgroup] = tmp;				
		setSubgroupSizes(sizes);
		
		
		//switch actions		
		for(StudyAction a: study.getStudyActions(this)) {
			if(a.getSubGroup()==subgroup) {
				a.setSubGroup(subgroup-1);
			} else if(a.getSubGroup()==subgroup-1) {
				a.setSubGroup(subgroup);
			}
		}
		
		
		//Move biosamples
		for (Biosample top : study.getTopAttachedBiosamples(this)) {
			if(top.getInheritedSubGroup()==subgroup) {
				for(Biosample b: top.getHierarchy(HierarchyMode.ATTACHED_SAMPLES)) {
					b.setInheritedSubGroup(subgroup-1);
				}
			} else if(top.getInheritedSubGroup()==subgroup-1) {
				for(Biosample b: top.getHierarchy(HierarchyMode.ATTACHED_SAMPLES)) {
					b.setInheritedSubGroup(subgroup);
				}
			} 
				
		}	
		
	}

}
