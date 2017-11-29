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

package com.actelion.research.spiritapp.ui.util.lf;

import java.awt.Component;
import java.awt.Desktop;
import java.net.URI;
import java.util.Collections;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import com.actelion.research.spiritapp.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.ui.util.component.DocumentTextField;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAODocument;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;

public class SpiritHyperlinkListener implements HyperlinkListener {
	@Override
	public void hyperlinkUpdate(final HyperlinkEvent e) {
		if(e.getEventType()==EventType.ACTIVATED) {
			new SwingWorkerExtended("", (Component)e.getSource(), SwingWorkerExtended.FLAG_SYNCHRONOUS) {
				@Override
				protected void done() {
					if(e.getDescription().startsWith("doc:")) {
						String[] params = e.getDescription().substring(4).split(":");
						try {
							int id = Integer.parseInt(params[0]);
							Document doc = DAODocument.getDocument(id);

							if(params.length>1) {
								int entry = Integer.parseInt(params[1]);
								doc = doc.getZipEntry(entry);
							}

							DocumentTextField.open(doc);
						} catch (Exception ex) {
							JExceptionDialog.showError(ex);
						}
					} else if(e.getDescription().startsWith("loc:")) {
						int index = e.getDescription().lastIndexOf(':');
						try {
							if(index<4) throw new Exception("Invalid link:" + e.getDescription());
							String param1 = e.getDescription().substring(4, index);
							String param2 = e.getDescription().substring(index+1);
							int id = Integer.parseInt(param1);
							int pos = Integer.parseInt(param2);
							Location location = DAOLocation.getLocation(id);
							if(location!=null) {
								SpiritContextListener.setLocation(location, pos);
							}
						} catch (Exception ex) {
							JExceptionDialog.showError(ex);
						}
					} else if(e.getDescription().startsWith("stu:")) {
						//						SpiritContextListener.setStudy(null);
						String param = e.getDescription().substring(4);
						try {
							int id = Integer.parseInt(param);
							Study study = DAOStudy.getStudy(id);
							if(study!=null) {
								SpiritContextListener.setStudy(study);
							}
						} catch (Exception ex) {
							JExceptionDialog.showError(ex);
						}
					} else if(e.getDescription().startsWith("bio:")) {
						String param = e.getDescription().substring(4);
						try {
							int id = Integer.parseInt(param);
							Biosample b = DAOBiosample.getBiosampleById(id);

							if(b!=null) {
								SpiritContextListener.setBiosamples(Collections.singletonList(b));
							}
						} catch (Exception ex) {
							JExceptionDialog.showError(ex);
						}
					} else if(e.getDescription().startsWith("bios:")) { //studyId:typeName
						SpiritContextListener.query((BiosampleQuery)null);
						String[] s = e.getDescription().substring("bios:".length()).split(":");
						BiosampleQuery q = new BiosampleQuery();
						q.setStudyIds(s[0]);
						if(s.length>1) q.setBiotype(DAOBiotype.getBiotype(s[1]));
						if(s.length>2) q.setUpdDays(Integer.parseInt(s[2]));
						SpiritContextListener.query(q);
					} else if(e.getDescription().startsWith("test:")) {
						String[] s = e.getDescription().substring("test:".length()).split(":");
						ResultQuery q = new ResultQuery();
						q.setStudyIds(s[0]);
						if(s.length>1) q.getTestIds().add(Integer.parseInt(s[1]));
						SpiritContextListener.query(q, -1);
					} else if(e.getDescription().startsWith("elb:")) {
						String elb = e.getDescription().substring("elb:".length());
						SpiritContextListener.query(ResultQuery.createQueryForElb(elb), -1);
					} else if(e.getDescription().startsWith("http:") || e.getDescription().startsWith("https:")) {
						try {
							Desktop.getDesktop().browse(new URI(e.getDescription()));
						} catch(Exception ex) {
							JExceptionDialog.showError(ex);
						}
					}
				}
			};

		}
	}
}
