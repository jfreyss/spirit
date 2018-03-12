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

package com.actelion.research.spiritcore.business.property;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import com.actelion.research.spiritcore.business.IAuditable;
import com.actelion.research.util.CompareUtils;

/**
 * Property saved in the DB used to customize Spirit. All those properties are optional, but are used by the system to add extra functionalities.
 *
 * Those properties are designed to be extensible and to be evolved more often than the rest of the DB.
 *
 * @author freyssj
 *
 */
@Entity
@Table(name="spirit_property")
@Audited
public class SpiritProperty implements Comparable<SpiritProperty>, IAuditable {

	@Id
	@Column(name="id", length=128)
	private String key;

	@Column(name="value", length=512)
	private String value;

	public SpiritProperty() {}

	public SpiritProperty(String key, String value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * The Key of the property such as "format.date"
	 * @return
	 */
	public String getKey() {
		return key;
	}

	/**
	 * The configured Value
	 * @return
	 */
	public String getValue() {
		return value;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof SpiritProperty) && key.equals(((SpiritProperty) obj).getKey());
	}

	@Override
	public int compareTo(SpiritProperty o) {
		return key==null? (o.key==null?0: -1): key.compareTo(o.key);
	}

	@Override
	public String toString() {
		return key + "=" + value;
	}

	@Override
	public String getDifference(IAuditable b) {
		if(!(b instanceof SpiritProperty)) return "Class";
		SpiritProperty p = (SpiritProperty) b;
		if(!CompareUtils.equals(value, p.value)) return key+"="+value;
		return null;
	}

}
