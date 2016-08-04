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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.hibernate.envers.Audited;

import com.actelion.research.spiritcore.business.study.Phase;

@Entity
@DiscriminatorValue("Status")
@Audited
public class ActionStatus extends ActionBiosample {
	
	@Enumerated(EnumType.STRING)
	private Status status;
	
	public ActionStatus() {
	}
	
	public ActionStatus(Biosample biosample, Phase phase, Status status) {
		super(biosample, phase);
		this.status = status;
		setComments((status==null?"N/A":status.getName()));
	}
	
	public ActionStatus(Biosample biosample, Phase phase, Status status, String comments) {
		super(biosample, phase);
		this.status = status;
		setComments(comments);
	}
	
	@Override
	public String getDetails() {
		return "Status=" + (status==null?"N/A":status.getName());
	}
	
	@Override
	public Color getColor() {
		return new Color(255,235,215);
	}
	
	public Status getStatus() {
		return status;
	}
}
