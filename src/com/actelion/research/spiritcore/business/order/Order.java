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

package com.actelion.research.spiritcore.business.order;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.actelion.research.spiritcore.business.IEntity;


/**
 * 
 *
 */
@Entity
@Audited
@Table(name="bioorder")
@SequenceGenerator(name="bioorder_sequence", sequenceName="bioorder_sequence", allocationSize=1)
public class Order implements IEntity, Comparable<Order>, Serializable {

	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="bioorder_sequence")
	@Audited(targetAuditMode = RelationTargetAuditMode.AUDITED)
	private int id = -1;
	
	@Enumerated(EnumType.STRING)
	private OrderStatus status = OrderStatus.PLANNED;
		
	
	@OneToMany(fetch=FetchType.LAZY, cascade=CascadeType.ALL, mappedBy="order", orphanRemoval=true)
	@MapKey(name="containerId")
	@BatchSize(size=100)
	@Audited(targetAuditMode = RelationTargetAuditMode.AUDITED)
	private Map<String, OrderContainer> containerMap = new HashMap<String, OrderContainer>();
	

	@Audited(targetAuditMode = RelationTargetAuditMode.AUDITED)
	private String updUser;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Audited(targetAuditMode = RelationTargetAuditMode.AUDITED)
	private Date updDate;
	
	@Audited(targetAuditMode = RelationTargetAuditMode.AUDITED)
	private String creUser;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Audited(targetAuditMode = RelationTargetAuditMode.AUDITED)
	private Date creDate;

	@Override
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/**
	 * RackNo starting at 1 
	 * @param containerId
	 * @return
	 */
	public int getRackNo(String containerId) {
		OrderContainer oc = containerMap.get(containerId);
		return oc==null? 0: oc.getRackNo();
	}

	/**
	 * Return the position formatted A1
	 * @param containerId
	 * @return
	 */
	public String getRackPosition(String containerId) {
		OrderContainer oc = containerMap.get(containerId);
		return oc==null? null: oc.getPosition();
	}

	public void add(OrderContainer orderContainer) {
		orderContainer.setOrder(this);
		containerMap.put(orderContainer.getContainerId(), orderContainer);
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

	public OrderStatus getStatus() {
		return status;
	}
	
	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;
		return (obj instanceof Order) && ((Order)obj).getId()==getId();
	}
	
	
	@Override
	public int hashCode() {
		return (int)(getId()%Integer.MAX_VALUE);
	}
	
	@Override
	public int compareTo(Order o) {
		int c = -creDate.compareTo(o.getCreDate());
		if(c!=0) return c;
		
		return (int) -(getId()-o.getId());
	}
	
	@Override
	public String toString() {
		return "#Order-"+id;
	}
	
	public boolean hasPositions() {
		for (OrderContainer oc : containerMap.values()) {
			if(oc.getRackNo()>0 || oc.getPosition()!=null) return true;
		}
		return false;
	}
	
	public Map<String, OrderContainer> getContainerMap() {
		return containerMap;
	}

	public void setContainerMap(Map<String, OrderContainer> containerMap) {
		this.containerMap = containerMap;
	}
	
	public Set<String> getContainerIds() {
		return containerMap.keySet();
	}
//	public List<Container> getContainers() {
//		return DAOBiosample.getContainers(getContainerIds());
//	}
	


}
