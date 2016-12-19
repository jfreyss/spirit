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

package com.actelion.research.spiritapp.spirit.ui.biosample;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.icons.ImageFactory;
import com.actelion.research.spiritapp.spirit.ui.lf.SpiritHyperlinkListener;
import com.actelion.research.spiritapp.spirit.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.biosample.ActionBiosample;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoFormat;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoSize;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.biosample.LocationFormat;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.location.Privacy;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.UIUtils;

public class BiosampleMetadataPanel extends ImageEditorPane implements IBiosampleDetail {
	
	private int display;
	private Collection<Biosample> biosamples;
	private final static Hashtable<String, Image> imageCache = new Hashtable<>();
	private Date now = JPAUtil.getCurrentDateFromDatabase();
	
	public BiosampleMetadataPanel() {
		super(imageCache);
		setBorder(BorderFactory.createEmptyBorder());
		setMinimumSize(new Dimension(0, 0));
		setPreferredSize(new Dimension(300, 200));
		setOpaque(false);
		setEditable(false);
		
		StyleSheet stylesheet = ((HTMLEditorKit) getEditorKit()).getStyleSheet();
		stylesheet.addRule("td, th {margin:0px;padding:0px}");
		stylesheet.addRule(".description th {padding-left:0px;padding-right:2px;margin-top:0px;font-weight:plain;text-align:right;font-size:9px;color:gray}");
		stylesheet.addRule(".description td {padding-left:0px;margin-top:0px;font-weight:plain;text-align:left; font-size:9px}");		
		setBackground(new Color(0,0,0,0));
		BiosampleActions.attachPopup(this);		
		addHyperlinkListener(new SpiritHyperlinkListener());
	}

	public void setDisplay(int display) {
		this.display = display;
		refresh();
	}
	
	public int getDisplay() {
		return display;
	}
	
	@Override
	public Collection<Biosample> getBiosamples() {
		return biosamples;
	}

	public void setBiosample(Biosample biosample) {
		setBiosamples(biosample==null? null: Collections.singletonList(biosample));
	}
		
	@Override
	public void setBiosamples(Collection<Biosample> biosamples) {
		this.biosamples = biosamples==null/* || biosamples.size()!=1*/? new ArrayList<Biosample>(): biosamples;
		refresh();
	}
	
	private void refresh() {
		StringBuilder txt;
		txt = new StringBuilder();
		txt.append("<html><body>");
		
		int caret = 0;
		if(biosamples!=null) {
			for(Biosample b: biosamples) { //display biosamples
		
				Document consentForm =  b.getInheritedStudy()==null? null: b.getInheritedStudy().getConsentForm();
				txt.append("<div style='white-space:nowrap;padding-top:1px;padding-left:1px;padding-bottom:1px;background:#" + (consentForm==null?"666666": "FF0000") + "'>");
				txt.append("<div width=100% class=description style='white-space:nowrap;font-size:9px;background:#DDDDDD'>");					
	
				//Display Sample Hierarchy
				List<Biosample> hierarchy = b.getParentHierarchy();
				for (int i = 0; i < hierarchy.size() ; i++) {
					
					if(caret<=0 && i==hierarchy.size()-1) {
						//Display Container
						if((b.getContainerType()!=null && b.getContainerType()!=ContainerType.UNKNOWN) || (b.getContainerId()!=null  && b.getContainerId().length()>0) || b.getLocation()!=null) {
							txt.append("<div style='font-size:11px; white-space:wrap; background:#CCCCAA; width:100%; padding:1px; border-left:solid 1px black; border-top:solid 1px black'>");
							if(b.getContainerType()!=null || b.getLocation()!=null) {
								if(b.getScannedPosition()!=null) {
									txt.append("<span style='font-size:12px'><b>" + b.getScannedPosition() + "</b></span><br>");
								}
								//ContainerType
								if((b.getContainerType()!=null && b.getContainerType()!=ContainerType.UNKNOWN) || (b.getContainerId()!=null && b.getContainerId().length()>0)) {
									if(b.getContainerType()!=null && b.getContainerType()!=ContainerType.UNKNOWN) {
										txt.append("<span color:#666666'>" + b.getContainerType().getName() + "</span>");								
									}
									//ContainerId
									if(b.getContainerId()!=null && b.getContainerId().length()>0) {
										txt.append(" <span style='color:#000044'><b>" + b.getContainerId() + "</b></span>");
									} 
									txt.append("<br>");
								}
								
								//Location
								if(b.getLocation()!=null) {
									Privacy privacy = b.getLocation().getInheritedPrivacy();
									if(SpiritRights.canRead(b.getLocation(), Spirit.getUser())) {
										txt.append("<a style='font-size:8px' href='loc:" + b.getLocation().getId() + ":" + b.getPos() + "'>");
										txt.append("<b>" + b.getLocationString(LocationFormat.FULL_POS, Spirit.getUser()).replace("/", "</b>/<b>")+"</b>");		
										txt.append("</a>");
									} else if(privacy==Privacy.PROTECTED) {
										txt.append("<span color='#CC8800'>" + privacy.getName() + (b.getLocation().getEmployeeGroup()!=null? " (" + b.getLocation().getEmployeeGroup().getName() + ")": "") + "</span>") ;
									} else if(privacy==Privacy.PRIVATE) {
										txt.append("<span color='#CC0000'>" + privacy.getName() + (b.getLocation().getEmployeeGroup()!=null? " (" + b.getLocation().getEmployeeGroup().getName() + ")": "") + "</span>") ;
									} else {
										System.err.println("Invalid privacy?? " + privacy);
										txt.append("Invalid privacy?? " + privacy);
									}
								}
							}
							txt.append("</div>");
						}
					}
					
					Biosample b2 = hierarchy.get(i);
					if(b2==b) {
						txt.append("<div style='border-top:solid 1px black;border-left:solid 1px black;padding: 2px 0px 0px 3px;color:black;background:#FFFFEE'>");						
						txt.append("<a name='sample'>");
					} else {
						txt.append("<div style='border-top:solid 1px #999999;border-left:solid 1px #999999;padding: 2px 0px 0px 3px;color:#666666;background:#DDDDDD'>");
					}
					appendBiosample(txt, b2);				
				}
				txt.append("</a>");
				for (int i = 0; i < hierarchy.size() ; i++) {
					txt.append("</div>");
				}
				
				//ConsentForm
				if(consentForm!=null) {
					txt.append("<table style='width:100%; background:#FFAAAA'><tr><td>Consent: <a href='doc:" + consentForm.getId() + "'>" + consentForm.getFileName() + "</a></td></tr></table>");
				}
			}
			
			txt.append("</div>");
			txt.append("</div>");
			
		}
		txt.append("</body></html>");
		
		setText(txt.toString());
		setCaretPosition(getDocument().getLength()-1);		
		scrollToReference("sample");
		
	}	
	
	private void appendBiosample(StringBuilder txt, Biosample b) {
		txt.append("<table width=100% style='padding:0px;margin:0px;white-space:nowrap;font-size:9px'>");					
		
		//Main fields
		{
			
			txt.append("<tr><td width=60px style='width:60px;white-space:nowrap; font-size:8px;vertical-align:bottom' valign=bottom>");
			String imgKey = ImageFactory.getImageKey(b);			
			if(imgKey!=null) {
				BufferedImage img = ImageFactory.getImage(b, 28);
				if(img!=null) {
					String url = "type_"+imgKey;
					if(imageCache.get(url)==null) imageCache.put(url, img);
					txt.append("<img align=left src='" + url + "'>");
				}
			}
			txt.append("<br>" + (b.getBiotype()==null?"": b.getBiotype().getName().length()>12? b.getBiotype().getName().substring(0, 12)+".":b.getBiotype().getName()));			
			txt.append("</td>");
			txt.append("<td width="+(getWidth()-60)+"px valign=top style='white-space:nowrap; vertical-align:bottom' >");
			
			//study
			if(b.getParent()==null || b.getAttachedStudy()!=null || (b.getInheritedPhase()!=null && !b.getInheritedPhase().equals(b.getParent().getInheritedPhase()))) {
				Color groupColor = b.getInheritedGroup()==null? null: b.getInheritedGroup().getBlindedColor(Spirit.getUsername());
				Color fgColor = UIUtils.getForeground(groupColor);
				if(b.getInheritedStudy()!=null /*b.getAttachedStudy()!=null || (first && b.getStudy()!=null)*/) {
					txt.append("<div style='background:"+ UIUtils.getHtmlColor(groupColor) + ";color:" + UIUtils.getHtmlColor(fgColor) + "'>");
					txt.append("<b><a href='stu:"+b.getInheritedStudy().getId()+ "'>" + b.getInheritedStudy().getStudyId() + "</a></b><br>" );
					if(b.getInheritedGroup()!=null) {
						txt.append("<b>" + b.getInheritedGroupString(Spirit.getUsername()));
					}
					if(b.getInheritedPhase()!=null) {
						txt.append(" / <b> " + b.getInheritedPhase().getShortName() + "</b>");
					}
					txt.append("</div>");
				}
			}
			
			//SampleId
			txt.append("<span" + (b.getBiotype()!=null && b.getBiotype().isHideSampleId()?" style='color:gray'":"") + ">" + b.getSampleId() + "</span> ");

			//Status
			Pair<Status, Phase> s = b.getLastActionStatus();
			if(s.getFirst()!=null) { 
				txt.append("<i style='font-size:8px; background:" + UIUtils.getHtmlColor(s.getFirst().getBackground()) + "; color:#000000'>" + s.getFirst() + (s.getSecond()!=null? (s.getFirst().isAvailable()? " until ": " at ") + s.getSecond().getShortName(): "") + " </i><br>");
			}

			//Quality
			if(b.getQuality()!=null && b.getQuality()!=Quality.VALID) {
				txt.append("<i style='font-size:8px; background:" + UIUtils.getHtmlColor(b.getQuality().getBackground()) + "'>" + b.getQuality().getName() + "</i><br>");
			}
			
			//Expiry
			if(b.getExpiryDate()!=null) {
				txt.append("<i style='font-weight:bold;font-size:8px; foreground:" + (b.getExpiryDate().before(now)?"red":"black") + "'>Expiry Date: " + FormatterUtils.formatDate(b.getExpiryDate()) + "</i><br>");
			}
			
			txt.append("</td></tr>");
		}
		

		//SampleName
		if(b.getBiotype()!=null && b.getBiotype().getSampleNameLabel()!=null) {
			txt.append("<tr><td width=60px style='width:60px;font-size:8px'><b>" + b.getBiotype().getSampleNameLabel()+":</b></td><td><b>" + (b.getSampleName()==null || b.getSampleName().length()==0?"":b.getSampleName()) + "</b></td></tr>");
		}
		
		//ELB
		if(b.getElb()!=null && b.getElb().length()>0) {
			txt.append("<tr><td width=60px style='width:60px;font-size:8px'>ELB:</td><td>" + b.getElb() + "</td></tr>");
		}
		//Metadata
		if(b.getBiotype()!=null) {						
			for (BiotypeMetadata metadataType : new TreeSet<>(b.getBiotype().getMetadata())) { //Use TreeSet because Envers may not sort it
				String value = b.getMetadataValue(metadataType);
				if(value!=null && value.length()>0) {
					txt.append("<tr>");
					String name = metadataType.getName();
					txt.append("<td width=60px style='width:60px;font-size:8px;white-space:nowrap' valign=top>" + name  + ":</td>");
					if(metadataType.getDataType()==DataType.D_FILE) {
						if(b.getMetadataDocument(metadataType)!=null) {
							txt.append("<td><a href='doc:" + b.getMetadataDocument(metadataType).getId() + "'>" + value + "</a></td>");
						} else {
							txt.append("<td style='color:red'>" + value + "</td>");
						}
					} else if(metadataType.getDataType()==DataType.LARGE) {
						txt.append("<td><div style='font-size:8px; width:100%' disabled>" + MiscUtils.convert2Html(value) + "</textarea></td>");
					} else if(metadataType.getDataType()==DataType.BIOSAMPLE && b.getMetadataBiosample(metadataType)!=null) {
						Biosample linked = b.getMetadataBiosample(metadataType);						
						txt.append("<td><a href='bio:" + linked.getId() + "'>" + linked.getSampleId() + "</a><span style='font-size:8px;white-space:nowrap;color:gray'><br>" + linked.getInfos(EnumSet.of(InfoFormat.SAMPLENAME, InfoFormat.METATADATA), InfoSize.ONELINE) + "</span></td>");									
					} else if(metadataType.getDataType()==DataType.MULTI) {
						txt.append("<td>" + value.replace(";", "<br>") + "</td>");
					} else if((value.startsWith("http://") || value.startsWith("https://")) && value.indexOf("://", 7)<0){
						txt.append("<td><a href='"+value + "'>" + value + "</a></td>");
					} else {
						txt.append("<td>" + value + "</td>");
					}
				}
				txt.append("</tr>");
			}

			//Amount
			if(b.getAmount()!=null && b.getBiotype()!=null && b.getBiotype().getAmountUnit()!=null) {
				txt.append("<tr><td width=60px style='width:60px;font-size:8px'>" + b.getBiotype().getAmountUnit().getNameUnit() + ": </td><td>");
				if( b.getAmount()!=null) {
					txt.append("<b>" + b.getAmount() + "</b>");
				}
				txt.append("</td></tr>");
			}
			
			//Comments
			if(b.getComments()!=null && b.getComments().length()>0) { 
				txt.append("<tr><td width=60px style='width:60px;font-size:8px'>Comments: </td><td>");
				txt.append(b.getComments());
				txt.append("</td></tr>");
			}
			//LastAction
			ActionBiosample action = b.getLastAction();
			if(action!=null) { 
				txt.append("<tr><td width=60px style='width:60px;font-size:8px'>Last action: </td><td>");
				txt.append(action.getDetails());
				txt.append("</td></tr>");
			}
			txt.append("</table>");
			
		}
		txt.append("</td></tr></table>");
				
	}

	
}
