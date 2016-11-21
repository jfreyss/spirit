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

package com.actelion.research.spiritapp.spirit.ui.admin;

import java.awt.Dimension;
import java.awt.Image;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JEditorPane;

import com.actelion.research.spiritapp.spirit.ui.icons.ImageFactory;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.UIUtils;

public class BioTypeDocumentPane extends JEditorPane {
	private static Hashtable<URL, Image> imageCache = new Hashtable<URL, Image>();
	
	public BioTypeDocumentPane() {
		super("text/html", "");		
		getDocument().putProperty("imageCache", imageCache);
		setEditable(false);
//		showAllBiotypes();
	}
	
	
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(700, 400);
	}
	
	public void showAllBiotypes() {
		StringBuilder sb = new StringBuilder();

		sb.append("<html><body>");
		List<Biotype> list = DAOBiotype.getBiotypes();
		Biotype prev = null;
		for(Biotype type: list) {
			sb.append(getHelp(type));
			
			if(prev!=null && !prev.getCategory().equals(type.getCategory())) sb.append("<hr>");
			prev = type;
		}			
		sb.append("</body></html>");	
		setText(sb.toString());		
		setCaretPosition(0);
	}
	
	public void setBiotype(Biotype t) {	
		StringBuilder sb = new StringBuilder();
		if(t!=null) {
			sb.append("<html><body style='margin:0px;padding:0px;background:" + UIUtils.getHtmlColor(t.getCategory().getBackground()) + "'>");
			sb.append(getHelp(t));							
			sb.append("</body></html>");		
		} 
		setText(sb.toString());		
		setCaretPosition(0);
	}

	private StringBuilder getHelp(Biotype type) {
		StringBuilder sb = new StringBuilder();
		sb.append("<table width=100% style='margin:0px 0px 2px 0px ;background:" + UIUtils.getHtmlColor(type.getCategory().getBackground()) + "'>");
		sb.append("<tr><td style='width:110px' valign=top>");
		sb.append("<b style='font-size:16px'>" + type.getName() + "</b><br>");
		if(type.isHideSampleId()) {
			sb.append("No SampleId<br>");			
		} else {
			sb.append("SampleId: "+(type.getPrefix()==null?"user defined": type.getPrefix())+"<br>");
		}
		sb.append((type.isAbstract()?"<b>Abstract</b>":"")+"<br><br>");
		if(type.getAmountUnit()!=null) sb.append("Amount: "+type.getAmountUnit()+"<br>");
		if(type.getContainerType()!=null) sb.append("Container: "+type.getContainerType()+"<br>");
		
		
		try {
			URL url = new URL("file://localhost/type_"+type.getName());
			if(imageCache.get(url)==null) imageCache.put(url, ImageFactory.getImage(type));
			sb.append("<img height=80 width=100 border=0 src='" + url + "'><br>");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		sb.append("</td>");
		sb.append("<td style='font-size:9px' valign=top><table>");
		
		if(type.getSampleNameLabel()!=null) {
			sb.append("<tr><td style='white-space:nowrap'><b><u>" + type.getSampleNameLabel() + ":</u></b></td><td>MainField " + (type.isNameAutocomplete()?"Autocomplete":"") + (type.isNameRequired()?"<span style='color:red'> [Req]</span>":"") + "</td></tr>");

		}
		for (BiotypeMetadata m : type.getMetadata()) {
			sb.append("<tr><td style='white-space:nowrap'><b>" + m.getName() + ":</b></td><td>" + m.getDataType() + (m.isRequired()?"<span style='color:red'> [Req]</span>":"") + (m.isSecundary()?"<span style='color:blue'> [Sec]</span>":"") + "</td></tr>");
		}				
//		if(type.getDescription()!=null) sb.append("<tr><td colspan=2><i>" + type.getDescription() + "</i></td></tr>");
		sb.append("</table></td></tr>");
		sb.append("</table");
		if(type.getCreDate()!=null) {
			sb.append("<i>Created by " + (type.getCreUser()==null?"N/A":type.getCreUser()) + " - "+ FormatterUtils.formatDateTime(type.getCreDate())+"</i><br>");
			if(type.getUpdDate()!=null && type.getUpdDate().after(type.getCreDate())) {
				sb.append("<i>Updated by " + (type.getUpdUser()==null?"N/A":type.getUpdUser()) + " - "+ FormatterUtils.formatDateTime(type.getUpdDate())+"</i><br>");
			}
		}
		return sb;
	}
	
	
	
}

