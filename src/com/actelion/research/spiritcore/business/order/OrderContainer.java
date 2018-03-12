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

package com.actelion.research.spiritcore.business.order;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name="bioorder_container")
public class OrderContainer implements Serializable {

	@Id	@GeneratedValue(strategy=GenerationType.AUTO) 
	private int id;

	
	@ManyToOne(fetch=FetchType.LAZY, cascade={}, optional = false)
	@JoinColumn(name="bioorder_id")
	@OnDelete(action=OnDeleteAction.CASCADE)
	private Order order;
	
	@Column(name="containerid")
	private String containerId;

	@Column(name="rackno", precision=4)
	private int rackNo;
	
	@Column(name="rackpos", length=6)
	private String position;
	
	public OrderContainer() {
	}

	public OrderContainer(String containerId) {
		this.containerId = containerId;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public String getContainerId() {
		return containerId;
	}

	public void setContainerId(String containerId) {
		this.containerId = containerId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((containerId == null) ? 0 : containerId.hashCode());
		result = prime * result + ((order == null) ? 0 : order.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		OrderContainer other = (OrderContainer) obj;
		if (containerId == null)  {
			if (other.containerId != null) return false;
		} else if (!containerId.equals(other.containerId)) {
			return false;
		}
		if (order == null) {
			if (other.order != null) return false;
		} else if (!order.equals(other.order)) {
			return false;
		}
		return true;
	}
	
	public int getRackNo() {
		return rackNo;
	}
	
	public void setRackNo(int rackNo) {
		this.rackNo = rackNo;
	}
	
	public String getPosition() {
		return position;
	}
	
	public void setPosition(String position) {
		this.position = position;
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public static void validatePosition(String position) throws Exception {
		int colNo;
		try {
			colNo = Integer.parseInt(position.substring(1));
		} catch(Exception e) {
			throw new Exception("The position "+position+" is not well formatted");
		}						
		if(position.charAt(0)<'A' || position.charAt(0)>'H' || colNo<1 || colNo>12) throw new Exception("The position "+position+" is not well formatted");
	}

}
