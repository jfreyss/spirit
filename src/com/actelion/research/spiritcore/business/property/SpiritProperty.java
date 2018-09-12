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
import com.actelion.research.spiritcore.business.audit.DifferenceList;
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
	private String id;

	@Column(name="value", length=512)
	private String value;

	public SpiritProperty() {}

	public SpiritProperty(String key, String value) {
		this.id = key;
		this.value = value;
	}

	/**
	 * The Key of the property such as "format.date"
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * The configured Value
	 * @return
	 */
	public String getValue() {
		return value;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof SpiritProperty) && id.equals(((SpiritProperty) obj).getId());
	}

	@Override
	public int compareTo(SpiritProperty o) {
		return id==null? (o.id==null?0: -1): id.compareTo(o.id);
	}

	@Override
	public String toString() {
		return id + "=" + value;
	}

	@Override
	public DifferenceList getDifferenceList(IAuditable r) {
		DifferenceList res = new DifferenceList("Property", id, "", null);
		if(!(r instanceof SpiritProperty)) return res;
		SpiritProperty p = (SpiritProperty) r;
		if(!CompareUtils.equals(value, p.value)) {
			res.add(id, value, p.value);
		}
		return res;
	}

}
