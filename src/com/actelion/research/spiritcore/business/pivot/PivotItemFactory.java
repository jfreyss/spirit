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

package com.actelion.research.spiritcore.business.pivot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerMethod;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.PhaseFormat;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.FormatterUtils;

public class PivotItemFactory {

	public static final PivotItem STUDY_STUDYID = new PivotItem(PivotItemClassifier.STUDY_GROUP, "StudyId") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null && r.getBiosample().getInheritedStudy()!=null) {
				return r.getBiosample().getInheritedStudy().getStudyId();
			} else {
				return null;
			}			
		}
	};


	public static final PivotItem STUDY_GROUP = new PivotItem(PivotItemClassifier.STUDY_GROUP, "GroupName") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			Biosample b = r.getBiosample();
			if(b==null || b.getInheritedStudy()==null) return null;
			
			Group group = b.getInheritedGroup();			
			return group==null?"": "<r>" + group.getName();
		}
		@Override
		public boolean isHideForBlinds() {
			return true;
		}
	};
	
	public static final PivotItem STUDY_GROUP1 = new PivotItem(PivotItemClassifier.STUDY_GROUP, "Group1") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			Biosample b = r.getBiosample();
			if(b==null || b.getInheritedStudy()==null) return null;
			
			Group group = b.getInheritedGroup();
			if(group!=null) return "<r>" + Group.extractGroup123(group.getName());
			return null;

		}	
		@Override
		public boolean isHideForBlinds() {
			return true;
		}
	};
	
	public static final PivotItem STUDY_GROUP2 = new PivotItem(PivotItemClassifier.STUDY_GROUP, "GroupA") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			Biosample b = r.getBiosample();
			if(b==null || b.getInheritedStudy()==null) return null;
			
			Group group = b.getInheritedGroup();
			if(group!=null) return "<r>" + Group.extractGroupABC(group.getName());
			return null;
		}
		@Override
		public boolean isHideForBlinds() {
			return true;
		}
	};
	
	
	public static final PivotItem STUDY_SUBGROUP = new PivotItem(PivotItemClassifier.STUDY_GROUP, "GroupSt.") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			Biosample b = r.getBiosample();
			if(b==null || b.getInheritedStudy()==null) return null;
			
			Group group = b.getInheritedGroup();
			return group==null? null: group.getNSubgroups()>1? "<r>" + (b.getInheritedSubGroup()+1): null;
		}
		@Override
		public boolean isHideForBlinds() {
			return true;
		}
	};
	
	public static final PivotItem STUDY_TREATMENT = new PivotItem(PivotItemClassifier.STUDY_GROUP, "Treatment") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			Biosample b = r.getBiosample();
			if(b==null || b.getInheritedStudy()==null) return null;
			
			
			Group group = b.getInheritedGroup();
			if(group!=null) return "<r>" + group.getTreatmentDescription();
			return null;
		}
		@Override
		public boolean isHideForBlinds() {
			return true;
		}
	};
	



//	public static final PivotItem COMPOUND_ACTNO = new PivotItem(PivotItemClassifier.COMPOUND, "ActNo") {
//		@Override
//		public String getTitle(ResultValue rv) {
//			Result r = rv.getResult();
//			if(r.getCompound()!=null) {
//				return r.getCompound().getActNo();
//			}  else {
//				return null;
//			}					
//		}
//	};
	
	public static final PivotItem RESULT_ELB = new PivotItem(PivotItemClassifier.RESULT, "ELB") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			return r.getElb();
		}
	};
	public static final PivotItem RESULT_TEST = new PivotItem(PivotItemClassifier.RESULT, "TestName") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			Test t = r.getTest();			
			return "<y>" + t.getName();
		}
	};

	
	public static final PivotItem BIOSAMPLE_CONTAINERTYPE = new PivotItem(PivotItemClassifier.LOCATION, "ContainerType") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null && r.getBiosample().getContainerType()!=null) {
				return "<c>" + r.getBiosample().getContainerType().getName();
			}  else {
				return null;			
			}
		}
	};
	public static final PivotItem BIOSAMPLE_CONTAINERID = new PivotItem(PivotItemClassifier.LOCATION, "ContainerId") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null && r.getBiosample().getContainerId()!=null) {
				return "<c>" + r.getBiosample().getContainerId();
			}  else {
				return null;			
			}
		}
	};
	public static final PivotItem BIOSAMPLE_FULLLOCATION = new PivotItem(PivotItemClassifier.LOCATION, "FullLocation") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null && r.getBiosample().getLocation()!=null) {
				return "<c>" + r.getBiosample().getLocation().getHierarchyFull();
			}  else {
				return null;
			}
		}
	};
	public static final PivotItem BIOSAMPLE_PARENTLOCATION = new PivotItem(PivotItemClassifier.LOCATION, "ParentLocation") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null && r.getBiosample().getLocation()!=null && r.getBiosample().getLocation().getParent()!=null) {
				return "<c>" + r.getBiosample().getLocation().getParent().getHierarchyFull();
			}  else {
				return null;
			}
		}
	};
	public static final PivotItem BIOSAMPLE_LOCATION = new PivotItem(PivotItemClassifier.LOCATION, "Location") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null && r.getBiosample().getLocation()!=null) {
				return "<c>" + r.getBiosample().getLocation().getName();
			}  else {
				return null;
			}
		}
	};
	public static final PivotItem BIOSAMPLE_POS = new PivotItem(PivotItemClassifier.LOCATION, "Pos") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null && r.getBiosample().getContainer()!=null && r.getBiosample().getContainer().getLocation()!=null) {
				return r.getBiosample().getPos()>=0? "<c>" + (r.getBiosample().getPos()+1): "";
			}  else {
				return null;			
			}
		}
	};


	public static final PivotItem BIOSAMPLE_TOPID = new PivotItem(PivotItemClassifier.TOP, "TopId") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()==null) return null;
			Biosample top = r.getBiosample().getTopParent();
			if(top==null || top.getBiotype()==null || top.getBiotype().isHideSampleId()) return null;
			return "<b>" + top.getSampleId();
		}
	};
	public static final PivotItem BIOSAMPLE_TOPNAME = new PivotItem(PivotItemClassifier.TOP, "TopName") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()==null) return null;
			Biosample top = r.getBiosample().getTopParent();
			Biosample topInStudy = r.getBiosample().getTopParentInSameStudy(); //parent in same study because the animal can have different id
			
			if(top==null || top.getBiotype()==null || top.getBiotype().getSampleNameLabel()==null) {
				return null;
			} else if(!top.equals(topInStudy) && (top.getSampleName()==null || top.getSampleName().length()==0) && (topInStudy.getSampleName()!=null && topInStudy.getSampleName().length()>0) && top.getBiotype().equals(topInStudy.getBiotype()) && topInStudy.getBiotype().getSampleNameLabel()!=null) {
				return "<b>" + topInStudy.getSampleName(); 
			} else {
				return (top.getSampleName()==null || top.getSampleName().length()==0)?"": "<b>" + top.getSampleName();
			}
		}
	};
	public static final PivotItem BIOSAMPLE_BIOTYPE = new PivotItem(PivotItemClassifier.BIOSAMPLE, "Biotype") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null && r.getBiosample().getBiotype()!=null) {
				String s = "<b>" + r.getBiosample().getBiotype().getName();				
				return s;
			} else {
				return null;
			}
		}
	};

	public static final PivotItem BIOSAMPLE_SAMPLEID = new PivotItem(PivotItemClassifier.BIOSAMPLE, "SampleId") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null && r.getBiosample().getBiotype()!=null) {
				return "<b>" + r.getBiosample().getSampleId();
			}
			return null;
		}
	};

	public static final PivotItem BIOSAMPLE_NAME = new PivotItem(PivotItemClassifier.BIOSAMPLE, "SampleName") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null && r.getBiosample().getBiotype()!=null) {
				if(r.getBiosample().getBiotype().getSampleNameLabel()!=null && r.getBiosample()!=r.getBiosample().getTopParentInSameStudy() &&  r.getBiosample().getSampleName()!=null) {				
					return "<b>" + r.getBiosample().getSampleName();
				} else {
					return "<b>" + r.getBiosample().getBiotype().getName();
				}		
			}
			return null;
		}
	};
	
	public static final PivotItem BIOSAMPLE_NAMEDSAMPLING = new PivotItem(PivotItemClassifier.BIOSAMPLE, "SamplingTemplate") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null) {
				return "<b>" + (r.getBiosample().getAttachedSampling()==null? "": r.getBiosample().getAttachedSampling().getNamedSampling().getName());
			}  else {
				return null;
			}
		}
	};
	
	public static final PivotItem BIOSAMPLE_SAMPLING = new PivotItem(PivotItemClassifier.BIOSAMPLE, "SamplingDescription") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null) {
				return "<b>" + (r.getBiosample().getAttachedSampling()==null? r.getBiosample().getBiotype().getName(): r.getBiosample().getAttachedSampling().getDetailsShort());
			}  else {
				return null;
			}
		}
	};
	
	public static final PivotItem BIOSAMPLE_METADATA = new PivotItem(PivotItemClassifier.BIOSAMPLE, "Metadata") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null && r.getBiosample().getBiotype()!=null) {
				String s = "<b>" + r.getBiosample().getMetadataAsString();				
				return s;

				
			} else {
				return null;
			}
		}
	};


	
	public static final PivotItem BIOSAMPLE_COMMENTS = new PivotItem(PivotItemClassifier.BIOSAMPLE, "Comments") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getBiosample()!=null) {
				return r.getBiosample().getComments()==null? "": "<b>" + r.getBiosample().getComments();
			}  else {
				return null;
			}
		}
	};
	

	public static class PivotItemBiosampleLinker extends PivotItem  {
		private BiosampleLinker linker;
		public PivotItemBiosampleLinker(BiosampleLinker linker) {
			super(PivotItemClassifier.BIOSAMPLE, "["+linker.getBiotypeForLabel()+"] "+linker.getLabelShort());
			this.linker = linker;
		}
		
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			Biosample b = r.getBiosample();
			String res;
			if(b==null) {
				res = null;
			} else {
				res = linker.getValue(b);
				if(res=="") res = null;
			}
			return res;			
		}
		
		public BiosampleLinker getLinker() {
			return linker;
		}
		@Override
		public String toString() {
			return super.toString()+"[-"+linker+"-]";
		}
	}
	
	
//	public static final PivotItem RESULT_BIOMARKER = new PivotItem(PivotItemClassifier.RESULT, "Biomarker") {
//		@Override
//		public String getTitle(ResultValue rv) {
//			Result r = rv.getResult();
//			StringBuilder sb = new StringBuilder();
//			for (TestAttribute att2 : r.getTest().getInputAttributes()) {
//				String val = r.getResultValue(att2).getValue();
//				if(val==null) continue;
//				if(sb.length()>0) sb.append(" ");
//				sb.append(val);
//			}
//			return sb.length()==0?"": "<y>"+sb.toString();
//		}
//	};

	public static final PivotItem RESULT_INPUT = new PivotItem(PivotItemClassifier.RESULT, "Input") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			StringBuilder sb = new StringBuilder();
			for (TestAttribute att2 : r.getTest().getInputAttributes()) {
				String val = r.getResultValue(att2).getValue();
				if(val==null) continue;
				if(sb.length()>0) sb.append(" ");
				sb.append(val);
			}
			return sb.length()==0?"": "<y>"+sb.toString();
		}
	};
	
	public static final PivotItem STUDY_PHASE_SINCEFIRST = new PivotItem(PivotItemClassifier.STUDY_PHASE, "Since 1st treatment") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getInheritedPhase()==null) return null;
			Phase phase = r.getStudy().getPhaseFirstTreatment(r.getGroup(), r.getSubGroup());
			if(phase==null) return null;
			
			
			if(r.getInheritedPhase().getStudy().getPhaseFormat()==PhaseFormat.NUMBER) {
				int diff = r.getInheritedPhase().getDays() - phase.getDays();
				return "<r>" + (diff>=0?"+":"") + diff;
			} else {			
				String diff = ( r.getInheritedPhase().getDays()-phase.getDays()>=0?"+":"") + (r.getInheritedPhase().getDays()-phase.getDays())
						+ (r.getInheritedPhase().getHours()-phase.getMinutes()==0 && r.getInheritedPhase().getMinutes()-phase.getMinutes()==0? "": "."+(r.getInheritedPhase().getHours()-phase.getHours()))
						+ (r.getInheritedPhase().getMinutes()-phase.getMinutes()==0? "": ":"+(r.getInheritedPhase().getMinutes()-phase.getMinutes()));
				return "<r>" + diff;
			}
			
		}
	};


	
	public static final PivotItem STUDY_PHASE_DATE = new PivotItem(PivotItemClassifier.STUDY_PHASE, "Phase") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getInheritedPhase()==null) return null;
			
			if(r.getInheritedPhase().getStudy().getPhaseFormat()==PhaseFormat.NUMBER) {
				return "<r>" + r.getInheritedPhase().getName();
			} else {			
				return "<r>" + r.getInheritedPhase().getShortName();
			}
			
		}
	};


	public static final PivotItem STUDY_PHASE_DAYS = new PivotItem(PivotItemClassifier.STUDY_PHASE, "Days") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getInheritedPhase()!=null) {
				String phase = new DecimalFormat("00").format(r.getInheritedPhase().getDays());
				return "<r>" + phase;
			} else {
				return null;
			}
		}
	};
	
	public static final PivotItem STUDY_PHASE_HOURS = new PivotItem(PivotItemClassifier.STUDY_PHASE, "Hours") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(r.getInheritedPhase()!=null) {
				String phase = new DecimalFormat("00").format(r.getInheritedPhase().getHours());
				return "<r>" + phase;
			} else {
				return null;
			}
		}
	};

	public static final PivotItem STUDY_PHASE_LABEL = new PivotItem(PivotItemClassifier.STUDY_PHASE, "PhaseLabel") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			return r.getInheritedPhase()==null?null: "<r>" + r.getInheritedPhase().getLabel();
		}
	};

	public static final PivotItem RESULT_OUTPUT = new PivotItem(PivotItemClassifier.RESULT, "Output") {
		@Override
		public String getTitle(ResultValue rv) {
			//Skip the output attribute if it is not discriminant
			if(rv.getAttribute().getTest().getOutputAttributes().size()==1) return null;
			return "<y>" + rv.getAttribute().getName();
		}		
	};
	
	public static final PivotItem RESULT_COMMENTS = new PivotItem(PivotItemClassifier.RESULT, "Comments") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			return r.getComments();
		}
	};
	
	public static final PivotItem RESULT_QUALITY = new PivotItem(PivotItemClassifier.RESULT, "Quality") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			return r.getQuality()==null? null: r.getQuality().getName();
		}
	};
	
	public static final PivotItem RESULT_CREDATE = new PivotItem(PivotItemClassifier.RESULT, "CreDate") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			return FormatterUtils.formatDate(r.getCreDate());
		}
	};
	
	public static final PivotItem RESULT_CRETIME = new PivotItem(PivotItemClassifier.RESULT, "CreTime") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			return FormatterUtils.formatTime(r.getCreDate());
		}
	};
	
	public static final PivotItem RESULT_CREUSER = new PivotItem(PivotItemClassifier.RESULT, "CreUser") {
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			return r.getCreUser();
		}
	};
	
	public static class PivotItemResultValue extends PivotItem  {
		private TestAttribute att;
		
		public PivotItemResultValue(TestAttribute att) {
			super(PivotItemClassifier.RESULT, "["+att.getTest().getName()+"] "+att.getName());
			this.att = att;
		}		
		
		@Override
		public String getTitle(ResultValue rv) {
			Result r = rv.getResult();
			if(!att.getTest().equals(r.getTest())) return null;
			if(r.getResultValue(att)==null) return null;
			return "<b>" + r.getResultValue(att).getValueWithoutDelegateUnit();
		}
	}	
	
	public static class PivotItemBiosampleFromResultValue extends PivotItem  {
		private String valueWithLinkedBiosample;
		private String metadata;
		
		public PivotItemBiosampleFromResultValue(String valueWithLinkedBiosample, String metadata) {
			super(PivotItemClassifier.RESULT, valueWithLinkedBiosample + " > " + metadata);
			this.valueWithLinkedBiosample = valueWithLinkedBiosample;
			this.metadata = metadata;
		}		
		
		@Override
		public String getTitle(ResultValue rv) {
			ResultValue rv2 = rv.getResult().getResultValue(valueWithLinkedBiosample);
			if(rv2==null) return null;
			Biosample b = rv2.getLinkedBiosample();
			if(b!=null) {

				//check the name
				if(metadata.equals(b.getBiotype().getSampleNameLabel())) return "<b>" + b.getSampleName();
				
				//otherwise check the metadata
				String m = b.getMetadataValue(metadata);
				if(m!=null) return "<b>" + m;
			}
			return null;
		}
	}
	
	
	/**
	 * 
	 */
	public static final PivotItem[] ALL = new PivotItem[] {
		STUDY_STUDYID,
		STUDY_GROUP,
		STUDY_SUBGROUP,		
		STUDY_GROUP1,
		STUDY_GROUP2,
		STUDY_TREATMENT,		
		RESULT_TEST,
		STUDY_PHASE_LABEL,
		STUDY_PHASE_DATE,
		STUDY_PHASE_DAYS,
		STUDY_PHASE_HOURS,
		STUDY_PHASE_SINCEFIRST,
		RESULT_INPUT,
		RESULT_OUTPUT,
		RESULT_ELB,
		RESULT_COMMENTS,
		RESULT_QUALITY,
		RESULT_CREDATE,
		RESULT_CRETIME,
		RESULT_CREUSER,
		BIOSAMPLE_TOPID,
		BIOSAMPLE_TOPNAME,
		BIOSAMPLE_BIOTYPE,
		BIOSAMPLE_SAMPLEID,
		BIOSAMPLE_NAME,
		BIOSAMPLE_NAMEDSAMPLING,
		BIOSAMPLE_SAMPLING,
		BIOSAMPLE_METADATA,
		BIOSAMPLE_CONTAINERTYPE,
		BIOSAMPLE_CONTAINERID,
		BIOSAMPLE_FULLLOCATION,
		BIOSAMPLE_PARENTLOCATION,
		BIOSAMPLE_LOCATION,
		BIOSAMPLE_POS,
		BIOSAMPLE_COMMENTS,
	};


	/**
	 * 
	 * @param results
	 * @return
	 */
	public static Set<PivotItem> getPossibleItems(List<Result> results, SpiritUser user) {
		if(results==null || results.size()==0) {			
			return new HashSet<>(Arrays.asList(ALL));
		} else {
			Set<PivotItem> pool = new HashSet<>();
			
			//Add standard items
			loop: for(PivotItem pv: ALL) {
				if(pv.isHideForBlinds()) {
					for(Result r: results) {
						if(r.getStudy()!=null && SpiritRights.isBlind(r.getStudy(), user)) {
							//skip this item because the user must be blinded
							continue loop;
						}
					}
				}
				
				pool.add(pv);
			}
			
			
			//Special, hide items that could be deduced from others
			boolean hasTime = false;
			for (Result r : results) {
				Phase p = r.getInheritedPhase();
				if(p!=null && (p.getHours()>0 || p.getMinutes()>0)) {
					hasTime = true;
					break;
				}
			}
			if(!hasTime) {
				pool.remove(STUDY_PHASE_DAYS);
				pool.remove(STUDY_PHASE_HOURS);
			}
			
			////////////////////////////////////////////////////////////////
			List<Biosample> biosamples = new ArrayList<>(Result.getBiosamples(results));
			if(biosamples.size()>50) biosamples = MiscUtils.subList(biosamples, 50);

			//Add possible links of result->biosample->metadata
			Set<BiosampleLinker> linkers = BiosampleLinker.getLinkers(biosamples, LinkerMethod.ALL_LINKS);			
			for (BiosampleLinker linker : linkers) {
				pool.add(new PivotItemBiosampleLinker(linker));
			}
				
			//Add possible links of result->resultValue->biosample->metadata
			Map<TestAttribute, Biotype> att2biotype = new TreeMap<>();
			for(Result result: results) {
				for (ResultValue rv: result.getResultValues()) {
					if(rv.getLinkedBiosample()!=null && rv.getAttribute().getParameters()!=null) {
						att2biotype.put(rv.getAttribute(), rv.getLinkedBiosample().getBiotype());
					}
				}
			}
			for(Entry<TestAttribute, Biotype> entry : att2biotype.entrySet()) {
				TestAttribute att = entry.getKey();
				Biotype biotype = entry.getValue();
				
				//name
				if(biotype.getSampleNameLabel()!=null) {
					PivotItem pv = new PivotItemBiosampleFromResultValue(att.getName(), biotype.getSampleNameLabel());
					if(!pool.contains(pv)) pool.add(pv);
				}
				//metadata
				for (BiotypeMetadata mt : biotype.getMetadata()) {
					PivotItem pv = new PivotItemBiosampleFromResultValue(att.getName(), mt.getName());
					if(!pool.contains(pv)) pool.add(pv);
				}				
			}
	
			//Add input pivotItem
			Set<Test> tests = Result.getTests(results);
			for(Test test: tests) {
				if(test.getAttributes().size()>1) {
					for (TestAttribute ta : test.getAttributes()) {
						if(ta.getOutputType()==OutputType.OUTPUT) continue;
						PivotItem pv = new PivotItemResultValue(ta);
						if(!pool.contains(pv)) pool.add(pv);
						
//						if(ta.getDataType()==DataType.ELN) {
//							pv = new PivotItemResultActNo(ta);
//							if(!pool.contains(pv)) pool.add(pv);
//							pv = new PivotItemResultEln(ta);
//							if(!pool.contains(pv)) pool.add(pv);
//						}
					}
				}
			}

			
			//////////////////////////////////////////////////////////
			//Keep only items having more than 2 possible values
			//Hide all items having less than 2 values (no discrimination possible)
			for (PivotItem pivotItem : new ArrayList<>(pool)) {
//				if(!pivotItem.isDiscriminating(results)) {
				if(!PivotTemplate.isPopulated(pivotItem, results)) {
					pool.remove(pivotItem);
				}								
			}
	
			return pool;
			
		}		
		
	}
	
	
}

