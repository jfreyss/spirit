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

package com.actelion.research.spiritcore.services.helper;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.spiritcore.adapter.PropertyKey;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.ConfigProperties;
import com.actelion.research.spiritcore.util.MiscUtils;

public class WorkflowHelper {

	
	public static String getWorkflowDescription(String currentState) {
		boolean hasWorkflow = ConfigProperties.getInstance().hasStudyWorkflow();
		String[] states = ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES);
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		if(currentState!=null && currentState.length()>0) {
			sb.append("<b>"+currentState+"</b><br>");
		}
		boolean hasNext = false;
		for (String state : states) {
			String[] from = ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES_FROM, state);
			if(!hasWorkflow || ((currentState==null || currentState.length()==0) && from.length==0) || MiscUtils.contains(from, currentState)) {
				sb.append("&nbsp;&nbsp;&nbsp;--&gt;&nbsp;&nbsp;" + state + (hasWorkflow?" (" + ConfigProperties.getInstance().getValue(PropertyKey.STUDY_STATES_PROMOTERS, state) + ")":"") + "<br>");
				hasNext = true;
			}
		}
		if(!hasNext) {
			sb.append("&nbsp;&nbsp;&nbsp;--&gt;&nbsp;&nbsp;End state<br>");			
		}
			
		return sb.toString();
	}
	
	public static String getStateDescriptions() {
		String[] states = ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES);
		boolean hasWorkflow = ConfigProperties.getInstance().hasStudyWorkflow();
		StringBuilder sb = new StringBuilder();
		sb.append("<html><table border=0 padding=0 margin=0>");
		sb.append("<tr><td></td>");
		sb.append("<td width=120 style='background:#DDDDDDAA; white-space:nowrap'><u>Admin Rights</u><br>(edit design/users)</td>"
				+ "<td width=120 style='background:#DDDDDDAA; white-space:nowrap'><u>Experimenter</u><br>(add samples/results)</td>"
				+ "<td width=90 style='background:#DDDDDDAA; white-space:nowrap'><u>View Rights</u><br>(only read access)</td>");
		if(hasWorkflow) {sb.append("<td style='background:#DDDDDDAA'><u>Promoters</u></td>"
				+ "<td style='background:#DDDDDDAA'><u>From</u></td>");}
		sb.append("</tr>");
		for (String state : states) {
			String view = ConfigProperties.getInstance().getValue(PropertyKey.STUDY_STATES_READ, state);
			String expert = ConfigProperties.getInstance().getValue(PropertyKey.STUDY_STATES_EXPERT, state);
			String admin = ConfigProperties.getInstance().getValue(PropertyKey.STUDY_STATES_ADMIN, state);
			String[] from = ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES_FROM, state);
			String[] promoters = ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES_PROMOTERS, state);
			
			sb.append("<tr><td style='background:#DDDDDDAA'><b>&nbsp;"+state+"&nbsp;</b></td>");
			sb.append("<td style='background:#DDDDDDAA'>&nbsp;"+admin+"&nbsp;</td>");
			sb.append("<td style='background:#DDDDDDAA'>&nbsp;"+expert+"&nbsp;</td>");
			sb.append("<td style='background:#DDDDDDAA'>&nbsp;"+view+"&nbsp;</td>");
			if(hasWorkflow) {
				sb.append("<td style='background:#DDDDDDAA'>&nbsp;"+MiscUtils.flatten(promoters, ", ")+"&nbsp;</td>");
				sb.append("<td style='background:#DDDDDDAA'>&nbsp;"+MiscUtils.flatten(from, ", ")+"&nbsp;</td>");
			}
			sb.append("</tr>");					
		}
		return sb.toString();
	}

	/**
	 * 
	 * @param study
	 * @param user
	 * @return
	 */
	public static List<String> getNextStates(Study study, SpiritUser user) {
		String[] states = ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES);
		List<String> possibleStates = new ArrayList<>();
		boolean hasWorkflow = ConfigProperties.getInstance().hasStudyWorkflow();
		for (String state : states) {
			String[] from = ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES_FROM, state);
			String[] promoters = ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES_PROMOTERS, state);
			
			if(!hasWorkflow || user.isSuperAdmin()) {
				possibleStates.add(state);		
			} else if(state.equals(study.getState())) {
				possibleStates.add(state);		
			} else if(((study.getState()==null || study.getState().length()==0) && from.length==0 ) || MiscUtils.contains(from, study.getState())) {
				if(MiscUtils.contains(promoters, "ALL")) {
					possibleStates.add(state);
				} else if((MiscUtils.contains(promoters, user.getRoles()))) {
					possibleStates.add(state);
				}
			}
		}
		return possibleStates;
	}
}
