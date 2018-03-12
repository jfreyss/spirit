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

package com.actelion.research.spiritcore.services.dao;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import com.actelion.research.spiritcore.business.IAuditable;
import com.actelion.research.spiritcore.util.Pair;

/**
 * Class overriding the default RevisionEntity of Hibernate, to add the logged in userId
 *
 * @author Joel Freyss
 *
 */
@Entity(name="revinfo")
@Table(indexes = {
		@Index(name="study_id_index", columnList = "study_id")
})
@RevisionEntity(SpiritRevisionListener.class)
public class SpiritRevisionEntity {

	public static int MAX_DIFF_LENGTH = 512;

	@Id
	@Column(name="rev")
	@GeneratedValue(strategy=GenerationType.AUTO)
	@RevisionNumber
	private int id;

	@RevisionTimestamp
	@Column(name="revtstmp")
	private long timestamp;

	/**
	 * The study.Id impacted by this revision: it is equals to 0 if there are no study or more than 1 study
	 */
	@Column(name="study_id")
	private Integer sid;

	@Column(name="userId", length=20)
	private String userId;

	@Column(name="reason", length=256)
	private String reason;

	@Column(name="difference", length=512)
	private String difference;

	/**
	 * List used to store the change for each entity, and to build the difference message dynamically
	 */
	private transient List<Pair<IAuditable, String>> changes = new ArrayList<>();
	/**
	 * Map used to store the number of changes for each entity
	 */
	private transient Map<Class<?>, Integer> counter = new HashMap<>();


	public int getId() {
		return id;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getRevisionDate() {
		return new Date( timestamp );
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	//	public String getDifference() {
	//		return difference;
	//	}
	//
	//	public void setDifference(String difference) {
	//		this.difference = difference;
	//	}


	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SpiritRevisionEntity) ) {
			return false;
		}

		final SpiritRevisionEntity that = (SpiritRevisionEntity) o;
		return id == that.id;
	}

	@Override
	public int hashCode() {
		int result;
		result = id;
		result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "SpiritRevisionEntity(id = " + id + ", revisionDate = " + DateFormat.getDateTimeInstance().format( getRevisionDate() ) + ")";
	}

	public String getDifference() {
		return difference;
	}

	public void setDifference(String difference) {
		this.difference = difference;
	}

	/**
	 * Gets the study.Id impacted by this revision (if there is only one).
	 * Can be <=0 if there are no study or more than one study impacted
	 * @return
	 */
	public int getSid() {
		return sid==null?0:sid;
	}

	public void setSid(Integer sid) {
		this.sid = sid;
	}

	public List<Pair<IAuditable, String>> getChanges() {
		return changes;
	}

	public Map<Class<?>, Integer> getCounter() {
		return counter;
	}
}
