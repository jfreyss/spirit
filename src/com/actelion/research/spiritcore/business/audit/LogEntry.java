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

package com.actelion.research.spiritcore.business.audit;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="logentry", indexes = {
		@Index(name="logentry_user_index", columnList = "creUser"), @Index(name="logEntry_date_index", columnList = "creDate")})
public class LogEntry implements Serializable, Comparable<LogEntry> {

	public static enum Action {
		/**Register successful logon attempts*/
		LOGON_SUCCESS,
		/**Register unsuccessful logon attempts*/
		LOGON_FAILED,
		/**when the account has been unlocked */
		UNLOCK,
		//		/**when the account has been disabled after n failed attempts */
		//		DISABLED,
	}

	@Column(name="creUser", length=20, nullable=false)
	@Id
	private String user;

	@Temporal(value=TemporalType.TIMESTAMP)
	@Column(name="creDate", nullable=false)
	@Id
	private Date date;

	@Enumerated(EnumType.STRING)
	@Column(name="action", length=20, nullable=false)
	@Id
	private Action action;

	@Column(name="ipAddress", length=20)
	private String ipAddress;

	@Column(name="comments", length=128)
	private String comments;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}


	@Override
	public int hashCode() {
		return user.hashCode() + date.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==null) return false;
		if(obj==this) return true;
		LogEntry l = (LogEntry) obj;
		return user.equals(l.getUser()) && date.equals(l.getDate()) && action==l.getAction();
	}

	@Override
	public int compareTo(LogEntry o) {
		return -getDate().compareTo(o.getDate());
	}

}
