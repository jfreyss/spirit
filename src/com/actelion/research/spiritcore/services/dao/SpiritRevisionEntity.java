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
import java.util.Date;
import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import com.actelion.research.spiritcore.business.audit.DifferenceList;
import com.actelion.research.spiritcore.util.MiscUtils;

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

	public static final int MAX_DIFF_LENGTH = 4000;

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

	@Column(name="reason", length=1024)
	private String reason;

	@Basic(fetch=FetchType.LAZY)
	@Column(name="difference", length=MAX_DIFF_LENGTH)
	private String difference;

	@Transient
	private transient DifferenceList differenceList = new DifferenceList();

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

	public Map<String, String> getReason() {
		return MiscUtils.deserializeStringMap(reason);
	}

	public void setReason(Map<String, String> reason) {
		this.reason = MiscUtils.serializeStringMap(reason);
	}

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

	public DifferenceList getDifferenceList() {
		if(difference!=null && differenceList.isEmpty()) {
			try {
				differenceList = DifferenceList.deserialize(difference);
			} catch (Exception e) {
				System.err.println("Cannot deserialize "+difference+">"+e);
				e.printStackTrace();
				differenceList = new DifferenceList();
			}
		}
		return differenceList;
	}

	public void setDifferenceList(DifferenceList differenceList) {
		this.differenceList = differenceList;
		this.difference = differenceList==null? null: differenceList.serialize();
		if(this.difference!=null && this.difference.length()>MAX_DIFF_LENGTH) {
			this.difference = this.difference.substring(0, MAX_DIFF_LENGTH);
		}
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

}
