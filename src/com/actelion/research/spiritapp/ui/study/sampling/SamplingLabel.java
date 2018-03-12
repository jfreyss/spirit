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

package com.actelion.research.spiritapp.ui.study.sampling;

import java.awt.Image;

import javax.swing.ImageIcon;

import com.actelion.research.spiritapp.ui.util.icons.ImageFactory;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class SamplingLabel extends JLabelNoRepaint{

	private Sampling sampling;
	
	public SamplingLabel() {
		setCondenseText(false);
		setWrappingWidth(200);
	}
	
	public SamplingLabel(Sampling sampling) {
		this();
		setSampling(sampling);
	}
	
	public void setSampling(Sampling sampling) {
		this.sampling = sampling;
		
		if(sampling==null) {
			setText(null);
			setIcon(null);
		} else {
			setText("<B>"+sampling.getBiotype().getName()+": " + 
					(sampling.getSampleName()==null?"": sampling.getSampleName()+"\n") +
					(sampling.getMetadataValues().length()==0?"": sampling.getMetadataValues() +" ") +
					(sampling.getComments()==null?"":sampling.getComments()));
			Image img = ImageFactory.getImage(sampling.createCompatibleBiosample(), 22);
			setIcon(img==null? null: new ImageIcon(img));
		}
	}	
	
	public Sampling getSampling() {
		return sampling;
	}
}
