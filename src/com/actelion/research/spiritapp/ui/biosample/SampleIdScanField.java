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

package com.actelion.research.spiritapp.ui.biosample;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collection;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.util.ui.JCustomTextField;

public class SampleIdScanField extends JCustomTextField {

	/**
	 * The model
	 */
	private Biosample biosample;
	
	/**
	 * Used as a cache to avoid too many round-trips to the db, or recrating samples with the same sampleId
	 */
	private Collection<Biosample> extraPool;
	
	public SampleIdScanField() {
		this(13, "");
	}
	
	public SampleIdScanField(int cols, String defaultText) {
		super(cols, "", defaultText);
		setToolTipText("Click here and scan an ID or paste a sampleId");
		
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				loadBiosample();
			}
		});
		
	}
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(130,26);
	}

	
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
//		SampleIdLabel.paint(this, g, biosample, true, false, false, true, null, 0);
	}
	
	private void loadBiosample() {
		String sampleId = getText();
		boolean ok;
		//infoField.setText("");
		if(sampleId.length()==0) {
			//Empty -> OK
			ok = true;
			biosample = null;
		} else if(biosample!=null && biosample.getSampleId().equals(sampleId)){
			//Same as before -> OK
			ok = true;
		} else {
			//1. Check in extraPool
			ok = false;
			if(extraPool!=null) {
				for (Biosample b : extraPool) {
					if(b.getSampleId().equals(sampleId)) {
						biosample = b;
						ok = true;
					}
				}
			}
			
			//2. Check in DB
			if(!ok) {
				Biosample b = DAOBiosample.getBiosample(sampleId);
				if(b!=null) {
					biosample = b;
					ok = true;
				}
			} 
			
			//3.Create a fake sample
			if(!ok) {
				biosample = new Biosample(sampleId);
				ok = false;
			}
		}
		
	}
	
	public void setSampleId(String txt) {
		setText(txt);
		loadBiosample();
	}
	public String getSampleId() {
		return getText();
	}

	public void setBiosample(Biosample biosample) {
		this.biosample = biosample;
		setSampleId(biosample==null?"": biosample.getSampleId());
	}

	public Biosample getBiosample() {
		if(hasFocus()) loadBiosample();
		return biosample;
	}
	
	
	public void setExtraBiosamplesPool(Collection<Biosample> extraPool) {
		this.extraPool = extraPool;
	}
	

}
