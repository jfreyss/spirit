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

package com.actelion.research.spiritapp.ui.location.depictor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Set;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.icons.ImageFactory;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoSize;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;

public class DefaultRackDepictorRenderer implements RackDepictorRenderer {

	private static final Color EMPTY_COLOR = new Color(230, 230, 230);

	@Override
	public Color getWellBackground(Location location, int pos, Container c) {
		Color bgColor;
		if(c==null) {
			bgColor = EMPTY_COLOR;
		} else {
			if(c.getStatus()!=null && !c.getStatus().isAvailable()) {
				bgColor = UIUtils.getDilutedColor(Color.WHITE, c.getStatus().getBackground(), .7);
			}  else if(c.getStudy()!=null) {
				Group gr = c.getGroup();
				bgColor = gr==null? Color.WHITE: UIUtils.getDilutedColor(gr.getBlindedColor(SpiritFrame.getUsername()), Color.WHITE, .2);
			} else {
				Biosample b = c.getBiosamples().isEmpty()? null: c.getBiosamples().iterator().next();
				if(b==null) {
					bgColor = Color.WHITE;
				} else {
					b = b.getTopParent();
					if(b!=null && b.getBiotype()!=null && b.getBiotype().getSampleNameLabel()!=null && b.getSampleName()!=null) {
						bgColor = UIUtils.getDilutedColor(UIUtils.getRandomBackground(b.getSampleName().hashCode()), Color.WHITE, .2);
					} else {
						bgColor = Color.WHITE;
					}
				}
			}
		}
		return bgColor;
	}

	@Override
	public void paintWellPre(RackDepictor depictor, Graphics2D g, Location location, int pos, Container c, Rectangle r) {

	}

	@Override
	public void paintWellPost(RackDepictor depictor, Graphics2D g, Location location, int pos, Container c, Rectangle r) {

	}

	@Override
	public void paintWell(RackDepictor depictor, Graphics2D g, Location location, int pos, Container c, Rectangle r) {

		if(c!=null) {
			Shape shape = g.getClip();
			g.setClip(r.intersection(g.getClipBounds()));
			//Draw Image
			boolean small = r.height < FastFont.getDefaultFontSize()*3;
			int iconSize = Math.max(FastFont.getDefaultFontSize(), Math.min(r.height/4, FastFont.getDefaultFontSize()*2));
			if(!small) {
				if(c.getBiosamples().size()==1) {
					Biosample b = c.getBiosamples().iterator().next();
					//Paint status
					if(b.getStatus()!=null) {
						g.setColor(b.getStatus().getBackground());
						g.fillRect(r.x+1, r.y+1, iconSize, iconSize);
					}

					//Paint icon
					Image img = ImageFactory.getImage(b, iconSize);
					if(img!=null) g.drawImage(img, r.x+1, r.y+1, depictor);
				} else if(c.getContainerType()!=null) {
					g.drawImage(c.getContainerType().getImage(FastFont.getDefaultFontSize()*2), r.x, r.y, depictor);
				}
			}

			//Draw biosample or containerid
			g.setColor(Color.DARK_GRAY);
			g.setFont(FastFont.MEDIUM);
			int x = r.x + (small? 2: Math.max(iconSize+2, (r.width-iconSize-4)/5));
			int y = r.y+FastFont.MEDIUM.getSize()+2;
			if(c.getContainerOrBiosampleId().length()>0) {
				g.drawString(c.getContainerOrBiosampleId(), x, y);
			}
			y += FastFont.SMALL.getSize()+1;
			
			//Print Study info from container
			g.setColor(Color.BLACK);
			String info = c.getPrintStudyLabel(SpiritFrame.getUsername());
			if(info.length()>0) {
				g.setFont(FastFont.SMALL);
				for (String line: info.split("\n")) {
					g.drawString(line, x, y);
					y+=g.getFont().getSize();
				}
			}
						
			//Print Metadata info from container
			String infos = c.getPrintMetadataLabel(InfoSize.EXPANDED);
			int index = (infos+'\n').indexOf('\n');
			String name = infos.substring(0, index);
			infos = infos.substring(index);
			g.setColor(Color.BLACK);
			g.setFont(FastFont.SMALL);
			y = UIUtils.drawString(g, name, x, y, r.width-(x-r.x)*3/2, r.height-(y-r.y)-2) + g.getFont().getSize()-1;
			g.setFont(FastFont.SMALL);
			UIUtils.drawString(g, infos, x, y, r.width-(x-r.x)*3/2, r.height-(y-r.y)-2);
			g.setClip(shape);
		}
	}

}
