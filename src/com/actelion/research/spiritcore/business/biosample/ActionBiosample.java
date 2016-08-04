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

import java.awt.Color;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import com.actelion.research.spiritcore.business.study.Phase;

/**
 * Action can be treatments, assigned/move to group, volume modification, moved location 
 * It is redundant to the audit but it is used to save 'worthwhile actions'.
 * As such, actions are never updated and can only be deleted when the underlying sample is deleted
 * 
 * @author freyssj
 *
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="type", discriminatorType=DiscriminatorType.STRING)
@Table(name="biosample_action")
@Audited
public abstract class ActionBiosample implements Comparable<ActionBiosample> {
	@Id
	@SequenceGenerator(name="biosample_action_sequence", sequenceName="biosample_action_sequence", allocationSize=1)
	@GeneratedValue(generator="biosample_action_sequence")
	@RevisionNumber
	private int id = 0;

	@ManyToOne(cascade=CascadeType.REFRESH, fetch=FetchType.LAZY, optional=false)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@JoinColumn(name="biosample_id")
	protected Biosample biosample;
	
	@ManyToOne(cascade=CascadeType.REFRESH, fetch=FetchType.LAZY, optional=true)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@JoinColumn(name="phase_id")
	protected Phase phase;
	
	@Column(length=256)
	protected String comments;

		
	@RevisionTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable=false)
	protected Date updDate = new Date();
	
	public ActionBiosample() {
	}
	
	public ActionBiosample(Biosample biosample, Phase phase) {
		setBiosample(biosample);
		setPhase(phase);		
	}
	
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Biosample getBiosample() {
		return biosample;
	}

	public void setBiosample(Biosample biosample) {
		this.biosample = biosample;
	}

	public Phase getPhase() {
		return phase;
	}

	public void setPhase(Phase phase) {
		this.phase = phase;
	}

	public Date getUpdDate() {
		return updDate;
	}

	public void setUpdDate(Date updDate) {
		this.updDate = updDate;
	}

	@Override
	public int compareTo(ActionBiosample o) {
		int c = getBiosample()==null? (o.getBiosample()==null?0 : 1): getBiosample().compareTo(o.getBiosample());
		if(c!=0) return c;

		c = getPhase()==null? (o.getPhase()==null?0 : 1): -getPhase().compareTo(o.getPhase());
		if(c!=0) return c;
		
		if(getId()>0 || o.getId()>0) {
			c = getUpdDate()==null? (o.getUpdDate()==null?0 : 1): -getUpdDate().compareTo(o.getUpdDate());
			if(c!=0) return c;
		}
		
		return getClass().getName().compareTo(o.getClass().getName());
	}

	
	public String getDetails() {
		return getComments();
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}
	
	public abstract Color getColor();
	
	
	@Override
	public String toString() {
		return getBiosample() + " > " + getDetails();
	}
}
