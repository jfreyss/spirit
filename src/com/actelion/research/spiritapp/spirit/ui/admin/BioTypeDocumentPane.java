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

import com.actelion.research.spiritapp.spirit.ui.icons.ImageFactory;
import com.actelion.research.spiritapp.spirit.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.UIUtils;

public class BioTypeDocumentPane extends ImageEditorPane {

	private Biotype selection = null;
	private boolean displayDatatypes = false;

	public BioTypeDocumentPane() {
		super();
		setEditable(false);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(700, 400);
	}

	public void setSelection(Biotype t) {
		this.selection = t;
		setText(getHelp().toString());
		setCaretPosition(0);
	}

	private StringBuilder getHelp() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table style='white-space:nowrap' cellpadding=1 cellspacing=0>");
		sb.append("<tr style='font-weight:bold;background:#CCCCCC;padding:3px'>");
		sb.append("<td>Prefix</td>");
		sb.append("<td>Amount</td>");
		sb.append("<td>Container</td>");
		sb.append("<td>Category</td>");
		sb.append("<td>Name</td>");
		sb.append("<td>Metadata</td>");
		sb.append("<td>Creation</td>");
		sb.append("<td>Update</td>");
		sb.append("</tr>");
		for(Biotype type: DAOBiotype.getBiotypes()) {

			sb.append("<tr style='background:" + (type.equals(selection)?"yellow": UIUtils.getHtmlColor(type.getCategory().getBackground())) + "'>");


			//SampleId
			sb.append("<td style='color:" + (type.isHideSampleId()?"gray":"black") + "'>");
			sb.append(MiscUtils.convert2Html((type.getPrefix()==null?"custom": type.getPrefix() + "###")));
			sb.append("</td>");

			//Amount
			sb.append("<td>");
			if(type.getAmountUnit()!=null) sb.append(type.getAmountUnit().getNameUnit());
			sb.append("</td>");

			//Container
			sb.append("<td>");
			if(type.isAbstract()) {
				sb.append("<i>Abstract</i>");
			} else {
				if(type.getContainerType()!=null) sb.append(type.getContainerType());
			}
			sb.append("</td>");

			//Category
			sb.append("<td>");
			sb.append(type.getCategory().getShortName());
			sb.append("</td>");

			//Name
			sb.append("<td style='white-space:nowrap'>");
			sb.append(MiscUtils.repeat("&nbsp;", type.getDepth()*4));
			String url = "file://localhost/type_"+type.getName();
			if(getImageCache().get(url)==null) getImageCache().put(url, ImageFactory.getImageThumbnail(type));
			sb.append("<img height=10 border=0 src='" + url + "' align=left>");
			sb.append("<a href='type:" + MiscUtils.convert2Html(type.getName()) + "'><b>" + MiscUtils.convert2Html(type.getName()) + "</b></a>");
			sb.append("</td>");

			//Metadata
			sb.append("<td>");
			int n = 0;
			if(type.getSampleNameLabel()!=null) {
				sb.append("<b>" + MiscUtils.convert2Html(type.getSampleNameLabel()) + "</b>");
				if(displayDatatypes) sb.append("(" + (type.isNameAutocomplete()?"Autocomplete":"") + (type.isNameRequired()?"<span style='color:red'> [Req]</span>":"") + ")");
				n++;
			}
			for (BiotypeMetadata m : type.getMetadata()) {
				if(n>0) sb.append(", ");
				if(n>5) {sb.append("..."); break;}
				sb.append(MiscUtils.convert2Html(m.getName()));
				if(displayDatatypes) sb.append("(" + m.getDataType() + (m.isRequired()?"<span style='color:red'> [Req]</span>":"") + (m.isSecundary()?"<span style='color:blue'> [Sec]</span>":"") + ")");
				n++;
			}
			sb.append("</td>");

			//Creation
			sb.append("<td>");
			if(type.getCreUser()!=null && type.getCreUser().length()>0) {
				sb.append(FormatterUtils.formatDate(type.getCreDate()) + " [" + type.getCreUser() + "]");
			}
			sb.append("</td>");
			//Update
			sb.append("<td>");
			if(type.getUpdUser()!=null && type.getUpdUser().length()>0) {
				sb.append(FormatterUtils.formatDate(type.getUpdDate()) + " [" + type.getUpdUser() + "]");
			}
			sb.append("</td>");

			sb.append("</tr>");
		}
		sb.append("</table>");
		return sb;
	}



}

