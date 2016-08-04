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

package com.actelion.research.spiritcore.business.study;

public enum PhaseFormat {

	DAY_MINUTES("d0, d2, d3"),
	NUMBER("1. Baseline, 2., 3. EOT");
	

	private final String description;
	
	private PhaseFormat(String description) {
		this.description = description;
	} 
	
	public String getDescription() {
		return description;
	}
	
	@Override
	public String toString() {
		return description;
	}
	
	
	
	/*
	public void setPhaseName(Phase phase, String txt) throws Exception {
		txt = txt.trim();
		switch(this) {
		case DAY_MINUTES:
		{
			int indexLabel = txt.indexOf(" ");
			String dateString;
			String label = null;
			if(indexLabel>0) {
				dateString = txt.substring(0, indexLabel).toLowerCase();
				label = txt.substring(indexLabel+1).trim();
			} else {
				dateString = txt.toLowerCase();
			}
	
			if(!dateString.startsWith("d")) throw new Exception("The phase "+txt+" is not well formatted (Expected: d0_0h0 Label)");
			int index = dateString.indexOf("_");
			int index2 = dateString.indexOf("h", index);
			int days;
			int hours;
			int minutes;
			if(index<0 || index2<0) {
				try {
					days = Integer.parseInt(dateString.substring(1));
					hours = 0;
					minutes = 0;
				} catch (Exception e) {
					throw new Exception("The phase "+txt+" is not well formatted (Expected: d0_0h0 Label)");
				}
			} else {
				try {
					days = Integer.parseInt(dateString.substring(1, index));
					hours = Integer.parseInt(dateString.substring(index+1, index2));
					minutes = dateString.length()<=index2+1?0: Integer.parseInt(dateString.substring(index2+1));
					
				} catch (Exception e) {
					throw new Exception("The phase "+txt+" is not well formatted (Expected: d0_0h0 Label)");
				}					
			}
			phase.setDays(days);
			phase.setHours(hours);
			phase.setMinutes(minutes);
			phase.setLabel(label);
			break;
		}
		case NUMBER:
		{
			int indexLabel = txt.indexOf(".");
			String dateString;
			String label;
			if(indexLabel>0) {
				dateString = txt.substring(0, indexLabel).trim();
				label = txt.substring(indexLabel+1).trim();
			} else {
				dateString = txt.trim();
				label = null;
			}
	
			int days;
			try {
				days = Integer.parseInt(dateString);
			} catch (Exception e) {
				throw new Exception("The phase "+txt+" is not well formatted (Expected: "+this+")");
			}
			phase.setDays(days);
			phase.setHours(0);
			phase.setMinutes(0);
			phase.setLabel(label);
			break;
		}
		default:
			throw new Exception("The format "+this+" is invalid");
		}
	}


	public String getName(Phase phase) {
		switch(this) {
		case DAY_MINUTES:
			return "d" + phase.getDays()
					+ (phase.getHours()!=0 || phase.getMinutes()!=0? "_" + phase.getHours() + "h": "")		
					+ (phase.getMinutes()!=0? (phase.getMinutes()<10?"0":"") + phase.getMinutes(): "");
		case NUMBER:
			return phase.getDays()+".";
		default:
			throw new RuntimeException("The format "+this+" is invalid");
		}
	}
	*/
	
	public String getName(int days, int hours, int minutes, String label) {
		switch(this) {
		case DAY_MINUTES:
			return "d" + days
					+ (hours!=0 || minutes!=0? "_" + hours + "h": "")		
					+ (minutes!=0? (minutes<10?"0":"") + minutes: "")
					+ (label!=null && label.length()>0? " " + label: "");
		case NUMBER:
			return days+"." + (label!=null && label.length()>0? " " + label: "");
		default:
			throw new RuntimeException("The format "+this+" is invalid");
		}
		
	}


}
