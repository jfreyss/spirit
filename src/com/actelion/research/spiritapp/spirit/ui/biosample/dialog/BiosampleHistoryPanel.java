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

package com.actelion.research.spiritapp.spirit.ui.biosample.dialog;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.IBiosampleDetail;
import com.actelion.research.spiritapp.spirit.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritapp.spirit.ui.util.lf.SpiritHyperlinkListener;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.util.FormatterUtils;

public class BiosampleHistoryPanel extends ImageEditorPane implements IBiosampleDetail {

	private Collection<Biosample> biosamples;
	public BiosampleHistoryPanel() {
		super();

		setOpaque(false);
		setEditable(false);
		setBackground(Color.white);

		BiosampleActions.attachPopup(this);
		addHyperlinkListener(new SpiritHyperlinkListener());
	}

	@Override
	public Collection<Biosample> getBiosamples() {
		return biosamples;
	}

	@Override
	public void setBiosamples(Collection<Biosample> biosamples) {
		Collection<Container> containers = Biosample.getContainers(biosamples, true);
		if(containers!=null && containers.size()==1) {
			this.biosamples = biosamples==null? new ArrayList<Biosample>(): biosamples;
		} else {
			this.biosamples = null;
		}
		refresh();
	}

	private void refresh() {

		if(biosamples==null || biosamples.size()==0) {
			setText("");
			return;
		}

		StringBuilder txt = new StringBuilder();
		txt.append("<html><body style='background:white'>");
		txt.append("<b>Change history</b><table>");
		try {

			for(Biosample b: biosamples) {

				if(b.getInheritedStudy()!=null && SpiritRights.isBlind(b.getInheritedStudy(), Spirit.getUser())) {
					txt.append("<tr><td>Details are not visible for blind users</td></tr>");

				} else {

					try {
						List<Revision> revisions = DAORevision.getLastRevisions(b);
						for (int i = 0; i < revisions.size(); i++) {
							Revision rev = revisions.get(i);

							String diff;
							Biosample b1 = revisions.get(i).getBiosamples().get(0);
							if(i+1<revisions.size()) {
								Biosample b2 = revisions.get(i+1).getBiosamples().get(0);
								diff = b1.getDifference(b2);
							} else {
								Biosample b2 = revisions.get(0).getBiosamples().get(0);
								diff = b1.getDifference(b2);
								if(diff.length()==0) diff = "First version";
							}

							if(diff.length()==0) continue;
							txt.append("<tr>");
							if(biosamples.size()>1) {
								txt.append("<th style='white-space:nowrap' valign=top>&nbsp;" + b.getSampleId() + "</th>");
							}
							txt.append("<th style='white-space:nowrap' valign=top>&nbsp;" + FormatterUtils.formatDateTimeShort(rev.getDate()) + "</th>");
							txt.append("<th style='white-space:nowrap' valign=top>&nbsp;" + rev.getUser() + "&nbsp;</th>");
							txt.append("<td style='white-space:nowrap' valign=top>" + diff.replace(";", "<br>") +"</td>");
							txt.append("</tr>");
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		txt.append("</table>");
		txt.append("</body></html>");

		setText(txt.toString());
		setCaretPosition(0);

	}


}
