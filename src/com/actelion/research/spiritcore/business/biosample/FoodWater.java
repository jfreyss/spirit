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

package com.actelion.research.spiritcore.business.biosample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.RevisionTimestamp;

import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.util.Formatter;
import com.actelion.research.util.CompareUtils;

@Entity
@Audited
@Table(name="food_water", uniqueConstraints= {@UniqueConstraint(columnNames= {"phase_id", "containerid"})}, indexes = {		
		@Index(name="fw_container_index", columnList = "containerid"),
		@Index(name="fw_phase_index", columnList = "phase_id")})
@SequenceGenerator(name="food_water_sequence", sequenceName="food_water_sequence", allocationSize=1)
public class FoodWater implements Comparable<FoodWater> {

	public static class Consumption {
		private final boolean water;
		public Phase fromPhase;
		public Phase toPhase;		
		public Double value;
		public String formula;
		
		public Consumption(boolean water) {
			this.water = water;
		}
		public boolean isWater() {
			return water;
		}
	}
	

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="food_water_sequence")
	private int id;
	
	@Column(name="containerid", length=25)
	private String containerId;
	
	@ManyToOne(cascade=CascadeType.REFRESH, optional=false, fetch=FetchType.LAZY)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)	
	private Phase phase;
		
	/** New Tare in g */
	private Double foodTare;

	/** Measured Weight in g */
	private Double foodWeight;
	
	private Double waterTare;
	
	private Double waterWeight;
	
	
	@Column(length=20, nullable=false)
	private String updUser;
	
	@RevisionTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable=false)
	private Date updDate;
	
	@Column(length=20, nullable=false)
	private String creUser;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable=false)
	private Date creDate;
		
	private Integer nAnimals;
	
	
	public FoodWater() {
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public String getContainerId() {
		return containerId;
	}
	public void setContainerId(String containerId) {
		this.containerId = containerId;
	}
	
//	private transient Container container;	
//	@Deprecated
//	public Container getContainer() {
//		if(containerId==null) return null;
//		if(container!=null) return container;
//		container = DAOBiosample.getContainer(containerId);
//		return container;
//	}
	
	public Phase getPhase() {
		return phase;
	}
	public void setPhase(Phase phase) {
		this.phase = phase;
	}
	
	/**
	 * Weight of food after refill
	 * @return
	 */
	public Double getFoodTare() {
		return foodTare;
	}
	/**
	 * Set Weight of food after refill
	 * @return
	 */
	public void setFoodTare(Double foodTare) {
		this.foodTare = foodTare;
	}
	/**
	 * Weight of food before refill
	 * @return
	 */
	public Double getFoodWeight() {
		return foodWeight;
	}
	public void setFoodWeight(Double foodWeight) {
		this.foodWeight = foodWeight;
	}
	public Double getWaterTare() {
		return waterTare;
	}
	public void setWaterTare(Double waterTare) {
		this.waterTare = waterTare;
	}
	public Double getWaterWeight() {
		return waterWeight;
	}
	public void setWaterWeight(Double waterWeight) {
		this.waterWeight = waterWeight;
	}
	public String getUpdUser() {
		return updUser;
	}
	public void setUpdUser(String updUser) {
		this.updUser = updUser;
	}
	public Date getUpdDate() {
		return updDate;
	}
	public void setUpdDate(Date updDate) {
		this.updDate = updDate;
	}
	public String getCreUser() {
		return creUser;
	}
	public void setCreUser(String creUser) {
		this.creUser = creUser;
	}
	public Date getCreDate() {
		return creDate;
	}
	public void setCreDate(Date creDate) {
		this.creDate = creDate;
	}
	
	@Override
	public int compareTo(FoodWater o) {
		int c = CompareUtils.compare(getContainerId(), o.getContainerId());
		if(c!=0) return c;
		c = CompareUtils.compare(getPhase(), o.getPhase());
		return c;
	}

	
	/**
	 * Util function to find the last measured taring from a list of FoodWater
	 * @param fws
	 * @param current
	 * @return
	 */		
	public FoodWater getPreviousFromList(List<FoodWater> fws, boolean water) {
		FoodWater sel = null;
		if(fws!=null) {
			for (FoodWater fw : fws) {
				if(!fw.getContainerId().equals(getContainerId())) continue;
				if(fw.getPhase().getTime()>=getPhase().getTime()) continue;				
				
				if(!water && (fw.getFoodTare()==null)) continue;
				if(water && (fw.getWaterTare()==null)) continue;

				if(sel==null || sel.getPhase().getTime()<fw.getPhase().getTime()) {
					sel = fw;
				}
			}
		}
		return sel;
	}
	
	public FoodWater getNextFromList(List<FoodWater> fws, boolean water) {
		FoodWater sel = null;
		if(fws!=null) {
			for (FoodWater fw : fws) {
				if(!fw.getContainerId().equals(getContainerId())) continue;
				if(fw.getPhase().getTime()<=getPhase().getTime()) continue;
				
				if(!water && (fw.getFoodWeight()==null)) continue;
				if(water && (fw.getWaterWeight()==null)) continue;
				
				if(sel==null || sel.getPhase().getTime()>fw.getPhase().getTime()) {
					sel = fw;
				}
			}
		}
		return sel;
	}
	
	
	@Override
	public String toString() {
		return "[FW:"+getContainerId()+" - " + getPhase() + "]";
	}
	
	public static List<Phase> getPhases(Collection<FoodWater> fws){
		Set<Phase> res = new TreeSet<Phase>();
		for (FoodWater fw : fws) {
			res.add(fw.getPhase());
		}
		
		return new ArrayList<>(res);
	}
	
	
	public static List<FoodWater> extract(Collection<FoodWater> fws, Phase phase) {
		assert phase!=null;
				
		List<FoodWater> res = new ArrayList<>();
		for (FoodWater fw : fws) {
			if(phase.equals(fw.getPhase())) res.add(fw);
		}
		return res;
	}
	
	public static FoodWater extract(Collection<FoodWater> fws, String containerId, Phase phase) {
		assert phase!=null;
		assert containerId!=null;
				
		for (FoodWater fw : fws) {
			if(phase.equals(fw.getPhase()) && containerId.equals(fw.getContainerId())) return fw;
		}
		return null;
	}

	/**
	 * @param nAnimals the nAnimals to set
	 */
	public void setNAnimals(Integer nAnimals) {
		this.nAnimals = nAnimals;
	}

	/**
	 * Returns the number of animals, either saved or calculated
	 * @return the nAnimals
	 */
	public Integer getNAnimals() {
		return nAnimals;
	}
//	
//	private int countNAnimals() {
//		if(getContainer()==null) return 0;
//		int nAnimals = 0;
//		for(Biosample b: getContainer().getBiosamples()) {
//			if(!b.isDeadAt(phase)) nAnimals++;
//		}
//		
//		return nAnimals;
//	}

	
	/**
	 * Util function to calculate the food/water consumption
	 * The no of rats used is the number of rats from the current FoodWater's location
	 * the time is calculated from the phases
	 * 
	 */
	public Consumption calculatePrevConsumptionFromList(List<FoodWater> fws, boolean water) {
		FoodWater prev = getPreviousFromList(fws, water);
		if(prev==null) return null;
		return calculateConsumption(prev, this, water);
	}

	public Consumption calculateNextConsumptionFromList(List<FoodWater> fws, boolean water) {
		FoodWater next = getNextFromList(fws, water);
		if(next==null) return null;
		FoodWater prev = next.getPreviousFromList(fws, water); //previous==this except if there is no measurement
		return calculateConsumption(prev, next, water);
	}

	
	
	
	/**
	 * Util function to calculate the food/water consumption from the previous measurement
	 * The no of rats used is the number of rats from the current FoodWater's location
	 * the time is calculated from the phases
	 */
	public static Consumption calculateConsumption(FoodWater prev, FoodWater fw, boolean water) {
		if(prev==null || fw==null) {
			return null;
		} else {
			Consumption res = new Consumption(water);
			res.fromPhase = prev.getPhase();
			res.toPhase = fw.getPhase();
			Integer nRats = fw.getNAnimals();
//			if(fw.getPhase().getDays()==null || prev.getPhase().getDays()==null) return null;
			double nDays = (fw.getPhase().getDays() - prev.getPhase().getDays());
				
			if(nRats==null) return res;
			
			if(water) {
				if(prev.getWaterTare()!=null && fw.getWaterWeight()!=null) {				
					if(nRats>0 && nDays>0 ) {
						res.value = (prev.getWaterTare() - fw.getWaterWeight())/(nRats*nDays);
						res.value = ((int)(res.value*10))/10.0;
					}
					res.formula = "("+Formatter.formatMax2(prev.getWaterTare()) + " - "+ Formatter.formatMax2(fw.getWaterWeight())+") / ("+nRats + " animal"+(nRats>1?"s":"") + " * "+Formatter.formatMax2(nDays)+" day"+(nDays>1?"s":"")+")";
				}
			} else {
				if(prev.getFoodTare()!=null && fw.getFoodWeight()!=null) {
					if(nRats>0 && nDays>0) {
						res.value = (prev.getFoodTare() - fw.getFoodWeight())/(nRats*nDays);
						res.value = ((int)(res.value*10))/10.0;
					}
					res.formula = "("+Formatter.formatMax2(prev.getFoodTare()) + " - "+ Formatter.formatMax2(fw.getFoodWeight())+") / ("+nRats+"animal"+(nRats>1?"s":"") + " * "+Formatter.formatMax2(nDays)+"day"+(nDays>1?"s":"")+")";
				}
			}

			return res;
		}
	}
	
	
	
	
	public boolean isEmpty() {
		return getFoodTare()==null && getFoodWeight()==null && 
				getWaterTare()==null && getWaterWeight()==null;
	}
	
}
