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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.actelion.research.spiritcore.business.biosample.Biosample.InfoFormat;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoSize;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;

/**
 * Container class used when the biosample has a containerType or it contains more than 1 biosample or when containerId<>biosampleId
 *
 * The containerType gives some information about printing labels.
 * The biosamples shows the content of this container (all should share the same location)
 */
@Embeddable
public class Container implements Cloneable, Comparable<Container>, Serializable {

	public static final char BLOC_SEPARATOR = '-';

	/**ContainerId */
	@Column(length=25,name="containerid")
	private String containerId;

	/**ContainerType, must be equal to the container's containerType if container!=null*/
	@Enumerated(EnumType.STRING)
	@Column(name="containertype")
	private ContainerType containerType;

	private transient Biosample createdFor;

	/**Biosamples in this container*/
	private transient Set<Biosample> biosamples = null;

	public Container() {
	}

	public Container(ContainerType type) {
		this(type, null);
	}

	public Container(String containerId) {
		this(ContainerType.UNKNOWN, null);
	}

	public Container(ContainerType type, String containerId) {
		this.containerType = type;
		this.containerId = containerId;
	}

	/**
	 * The Container is formatter like: {prefix}{ID}[-BlocNo][-Suffix]
	 *
	 * @return
	 */
	public Integer getBlocNo() {
		return getBlocNo(null, containerId);
	}

	public static Integer getBlocNo(ContainerType containerType, String containerId) {
		if(containerId==null) return null;
		if(containerType!=null && containerType.getBlocNoPrefix()==null) return null;

		int ind1 = containerId.indexOf(Container.BLOC_SEPARATOR, 4);
		if(ind1<0) return null;

		int ind2 = containerId.indexOf(Container.BLOC_SEPARATOR, ind1+1);
		if(ind2<0) ind2 = containerId.length();

		String blocNo = containerId.substring(ind1+1, ind2);

		try {
			return Integer.parseInt(blocNo);
		} catch(Exception e) {
			return null;
		}
	}


	public int getPos() {
		for(Biosample b: getBiosamples()) {
			return b.getPos();
		}
		return -1;
	}

	public void setPos(int pos) {
		for(Biosample b: getBiosamples()) {
			b.setPos(pos);
			b.setScannedPosition(null);
		}
	}

	public Location getLocation() {
		Biosample b = getFirstBiosample();
		return b==null? null: b.getLocation();
	}

	public void setLocation(Location location) {
		if(this.getLocation()!=null) {
			this.getLocation().getBiosamples().removeAll(getBiosamples());
		}
		for(Biosample b: getBiosamples()) {
			b.setLocation(location);
		}
		if(this.getLocation()!=null) {
			this.getLocation().getBiosamples().addAll(getBiosamples());
		}
	}


	public String getContainerId() {
		return containerId;
	}

	public String getContainerOrBiosampleId() {
		if(getContainerId()!=null && getContainerId().length()>0) return getContainerId();
		if(getBiosamples().size()==0) return "";
		Biosample b = getBiosamples().iterator().next();
		return b.getSampleId()==null?"": b.getSampleId();
	}

	public void setContainerId(String containerId) {
		this.containerId = containerId;
	}

	public ContainerType getContainerType() {
		return containerType;
	}

	public void setContainerType(ContainerType containerType) {
		this.containerType = containerType;
	}

	public Set<Biosample> getBiosamples() {
		if(biosamples==null) {
			biosamples = new TreeSet<Biosample>((o1,o2)-> {
				if(o1==null && o2==null) return 0;
				if(o1==null) return 1; //Null at the end
				if(o2==null) return -1;
				int c = CompareUtils.compare(o1.getContainerIndex(), o2.getContainerIndex());
				if(c!=0) return c;
				c = CompareUtils.compare(o1.getSampleId(), o2.getSampleId());
				if(c!=0) return c;
				return o1.getId()-o2.getId();
			});
			//			if(containerId!=null && containerId.length()>0 && (containerType==null || containerType.isMultiple())) {
			if(containerId!=null && containerId.length()>0 && (containerType!=null && containerType.isMultiple())) {
				//TODO: should be removed
				//				Thread.dumpStack();
				biosamples.addAll(JPAUtil.getManager().createQuery("from Biosample b where b.container.containerId = ?1").setParameter(1, containerId).getResultList());
			}
			if(createdFor!=null) {
				biosamples.add(createdFor);
			}
		}
		return biosamples;
	}

	public void removeBiosample(Biosample b) {
		if(biosamples==null) return;
		biosamples.remove(b);
	}

	public void addBiosample(Biosample b) {
		if(biosamples==null) return;
		biosamples.add(b);
	}

	public Biosample getFirstBiosample() {
		if(createdFor!=null) return createdFor;
		return getBiosamples().size()>0? getBiosamples().iterator().next(): null;
	}

	@Override
	protected Container clone()  {
		try {
			return (Container) super.clone();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode() {
		return (containerId==null? 0: containerId.hashCode());
	}

	/**
	 * Compare per containerType, ContainerId, SampleId
	 */
	@Override
	public int compareTo(Container c) {
		if(c==null) return -1;
		int cmp = CompareUtils.compare(getContainerType(), c.getContainerType());
		if(cmp!=0) return cmp;

		cmp = CompareUtils.compare(getContainerId(), c.getContainerId());
		if(cmp!=0) return cmp;

		cmp = CompareUtils.compare(getFirstBiosample(), c.getFirstBiosample());
		return cmp;

	}

	/**
	 * 2 containers are equals if they have the same type and the same not-null id.
	 * Ie. 2 containers without Id will always be different
	 */
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Container)) return false;
		if(this==obj) return true;

		Container c2 = (Container) obj;
		int cmp = CompareUtils.compare(getContainerType(), c2.getContainerType());
		if(cmp!=0) return false;

		cmp = CompareUtils.compare(getContainerId(), c2.getContainerId());
		return cmp==0 && getContainerId()!=null && getContainerId().length()>0;
	}

	@Override
	public String toString() {
		return getContainerOrBiosampleId();
	}

	public String getType() {
		Container c = this;

		Set<String> types = Biosample.getTypes(c.getBiosamples());
		if(types.size()==1) return types.iterator().next();
		return MiscUtils.extractCommonPrefix(types);
	}

	public String getBlocDescription() {
		Container c = this;
		Set<String> lines = new LinkedHashSet<>();


		int count=0;
		for (Biosample b : c.getBiosamples()) {
			lines.add(b.getInfos(EnumSet.of(InfoFormat.SAMPLENAME, InfoFormat.BIOTYPE, InfoFormat.METATADATA, InfoFormat.COMMENTS), InfoSize.ONELINE));
		}
		StringBuilder sb = new StringBuilder();
		sb.append(c.getContainerType().getBlocNoPrefix()==null?"": c.getContainerType().getBlocNoPrefix() + c.getBlocNo() + "\n");
		sb.append(MiscUtils.flatten(lines, "\n"));
		return sb.toString();
	}

	/**
	 * Return a generic print Label (used in SpiritWeb)
	 * If the study is blinded, the group is shown as blinded
	 * @return
	 */
	public String getPrintLabel() {
		return getPrintStudyLabel(null) + "\n" + getPrintMetadataLabel();
	}


	public String getPrintStudyLabel(String user) {
		StringBuilder sb = new StringBuilder();
		Study study = Biosample.getStudy(getBiosamples());
		if(study!=null) {
			//Add the study
			sb.append(study.getStudyId() + "\n");

			//Add the group
			if(user==null || study.isBlind()) {
				sb.append("");
			} else {
				Group g = Biosample.getGroup(getBiosamples());
				if(g!=null) {
					sb.append("Gr." + g.getShortName());
					//Add the phase
					Phase phase = Biosample.getPhase(getBiosamples());
					if(phase!=null) {
						sb.append(" / " +phase.getShortName());
					}
					sb.append("\n");
				}
			}

		}

		return sb.toString();
	}

	public String getPrintMetadataLabel() {
		Study study = Biosample.getStudy(getBiosamples());
		return getPrintMetadataLabel(study==null? InfoSize.EXPANDED: InfoSize.COMPACT);
	}
	/**
	 * Infos how they will be printed on the label (without the study)
	 * @return
	 */
	public String getPrintMetadataLabel(InfoSize infoSize) {
		String res;
		ContainerType containerType = getContainerType();
		if(containerType==ContainerType.BOTTLE) {
			//Bottle
			res = Biosample.getInfos(getBiosamples(), EnumSet.of(InfoFormat.TOPIDNAMES), infoSize)+"\n";
			Integer blocNo = getBlocNo();
			if(blocNo!=null && containerType.getBlocNoPrefix()!=null) res += containerType.getBlocNoPrefix() + blocNo;
		} else if(containerType==ContainerType.CAGE) {
			//Cage
			res = Biosample.getInfos(getBiosamples(), EnumSet.of(InfoFormat.SAMPLEID, InfoFormat.SAMPLENAME), infoSize)+"\n";
		} else if(containerType!=null && containerType.isMultiple()) {
			//Slide, Cassette
			res = Biosample.getInfos(getBiosamples(), EnumSet.of(InfoFormat.TOPIDNAMES), infoSize)+"\n";

			String info = Biosample.getInfos(getBiosamples(), EnumSet.of(InfoFormat.SAMPLENAME, InfoFormat.PARENT_SAMPLENAME), infoSize);
			if(info.length()>0) {
				res+= info + "\n";
			}

			Integer blocNo = getBlocNo();
			if(blocNo!=null && containerType.getBlocNoPrefix()!=null) {
				res += containerType.getBlocNoPrefix() + blocNo + "\n";
			}

		} else {
			//Default Label
			res = Biosample.getInfos(getBiosamples(), EnumSet.of(
					InfoFormat.SAMPLEID,
					InfoFormat.TOPIDNAMES,
					InfoFormat.SAMPLENAME,
					InfoFormat.METATADATA,
					InfoFormat.PARENT_METATADATA,
					InfoFormat.PARENT_SAMPLENAME,
					InfoFormat.COMMENTS,
					InfoFormat.AMOUNT), infoSize);
		}
		return res;
	}

	/**
	 * Returns the groups of the contained biosamples
	 * @return
	 */
	public Set<Group> getGroups() {
		return Biosample.getGroups(getBiosamples());
	}

	/**
	 * Returns the group (if all samples share the same group) or null
	 * @return
	 */
	public Group getGroup() {
		return Biosample.getGroup(getBiosamples());
	}

	public Phase getPhase() {
		return Biosample.getPhase(getBiosamples());
	}

	public Study getStudy() {
		return Biosample.getStudy(getBiosamples());
	}

	public Status getStatus() {
		return Biosample.getStatus(getBiosamples());
	}

	public String getTopParents() {
		StringBuilder sb = new StringBuilder();
		Set<Biosample> tops = Biosample.getTopParentsInSameStudy(getBiosamples());
		for(Biosample top: tops) {
			if(sb.length()>0) sb.append(" ");
			sb.append(top.getSampleIdName());
		}
		return sb.toString();
	}

	public Biosample getTopParent() {
		return Biosample.getTopParentInSameStudy(getBiosamples());
	}

	public String getContainerPrefix() {
		int index = containerId.lastIndexOf('-');
		if(index>5) return containerId.substring(0, index);
		else return containerId;
	}


	public static Set<Location> getLocations(Collection<Container> containers) {
		if(containers==null) return null;
		Set<Location> res = new java.util.HashSet<>();
		for (Container c : containers) {
			if(c!=null) res.add(c.getLocation());
		}
		return res;
	}

	public static Set<Integer> getPoses(Collection<Container> containers) {
		if(containers==null) return null;
		Set<Integer> res = new java.util.HashSet<>();
		for (Container c : containers) {
			if(c.getLocation()!=null && c.getPos()>=0) res.add(c.getPos());
		}
		return res;
	}

	public static Set<String> getScannedPoses(Collection<Container> containers) {
		if(containers==null) return null;
		Set<String> res = new java.util.HashSet<>();
		for (Container c : containers) {
			if(c.getScannedPosition()!=null) res.add(c.getScannedPosition());
		}
		return res;
	}

	public static Set<String> getContainerIds(Collection<Container> containers) {
		if(containers==null) return null;
		Set<String> res = new java.util.HashSet<>();
		for (Container c : containers) {
			res.add(c.getContainerId());
		}
		return res;
	}

	public static Set<ContainerType> getContainerTypes(Collection<Container> containers) {
		if(containers==null) return null;
		Set<ContainerType> res = new java.util.HashSet<>();
		for (Container c : containers) {
			res.add(c.getContainerType());
		}
		return res;
	}

	public static List<Biosample> getBiosamples(Collection<Container> containers) {
		return getBiosamples(containers, false);
	}

	public static List<Biosample> getBiosamples(Collection<Container> containers, boolean createFakeBiosamplesForEmptyContainer) {
		if(containers==null) return null;
		List<Biosample> res = new ArrayList<Biosample>();
		for (Container c : containers) {
			if(c==null) continue;
			if(c.getBiosamples().size()==0) {
				if(createFakeBiosamplesForEmptyContainer && c.getBiosamples().size()==0) {
					Biosample b = new Biosample();
					b.setContainer(c);
					res.add(b);
				}
			} else {
				res.addAll(c.getBiosamples());
			}
		}
		return res;
	}


	public static String suggestNameForCage(Study s, int cageNo) {
		if(s==null) return "S##-"+cageNo;

		String res = s.getStudyId();
		if(s.getStudyId().startsWith("S-")) {
			try {
				res = "S"+ Integer.parseInt(s.getStudyId().substring(2));
			} catch (Exception e) {
			}
		}

		res+="-";
		if(cageNo<10) res+="0";
		res+=cageNo;

		return res;

	}

	public Amount getAmount() {
		Biosample b = getFirstBiosample();
		return b==null? null: b.getAmountAndUnit();
	}

	public void setAmount(Double volume) {
		for (Biosample b : getBiosamples()) {
			b.setAmount(volume);
		}
	}

	public static Map<String, Container> mapContainerId(Collection<Container> containers){
		Map<String, Container> res = new HashMap<>();

		for (Container container : containers) {
			if(container.getContainerId()!=null) res.put(container.getContainerId(), container);
		}
		return res;
	}

	public static boolean isAllWithScannedPositions(Collection<Container> containers) {
		for (Container c : containers) {
			if(c.getScannedPosition()==null) return false;
		}
		return true;
	}

	public String getMetadata(String name) {
		Set<String> res = new HashSet<>();
		for (Biosample b : getBiosamples()) {
			String metadata = b.getMetadataValue(name);
			if(metadata!=null && metadata.length()>0) res.add(metadata);
		}
		if(res.size()==1) return res.iterator().next();
		return "";
	}



	public boolean isEmpty() {
		for (Biosample b : getBiosamples()) {
			if(b.getBiotype()!=null) return false;
		}
		return true;
	}


	public int getRow() {
		if(getLocation()==null) return 0;
		return getLocation().getLabeling().getRow(getLocation(), getPos());
	}

	public int getCol() {
		if(getLocation()==null) return 0;
		return getLocation().getLabeling().getCol(getLocation(), getPos());
	}

	public String getScannedPosition() {
		Biosample b = getFirstBiosample();
		return b==null? null: b.getScannedPosition();
	}


	/**
	 * Utility function to be able to retrieve later the samples of a multiple container
	 * @param createdFor
	 */
	protected void setCreatedFor(Biosample createdFor) {
		this.createdFor = createdFor;
	}

}
