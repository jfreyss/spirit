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

package com.actelion.research.spiritapp.spirit.services.report;


import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.study.depictor.StudyDepictor;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.util.MiscUtils;

public class StudyDesignReport extends AbstractReport {
	
	public StudyDesignReport() {
		super(ReportCategory.STUDY, "Design", "<ul><li>Study design,<li>Sampling templates<li>Treatment descriptions");
	}
	
	@Override
	protected void populateWorkBook() throws Exception {
		CreationHelper helper = wb.getCreationHelper();
		
		//create sheet
	    Sheet sheet = createSheet(wb, "Design");
	    createHeadersWithTitle(sheet, study, "Study Design");
	    
	    // Create the drawing patriarch.  This is the top level container for all shapes. 
	    Drawing drawing = sheet.createDrawingPatriarch();
	    
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    BufferedImage img = StudyDepictor.getImage(study, 5000, 5000, 2);
	    ImageIO.write(img, "PNG", os);
	    
	    //add a picture shape
	    int pictureIdx = wb.addPicture(os.toByteArray(), Workbook.PICTURE_TYPE_PNG);
	    
	    ClientAnchor anchor = helper.createClientAnchor();
	    //set top-left corner of the picture,
	    //subsequent call of Picture#resize() will operate relative to it
	    anchor.setAnchorType(ClientAnchor.MOVE_AND_RESIZE);
	    anchor.setCol1(0);
	    anchor.setRow1(3);
	    Picture pict = drawing.createPicture(anchor, pictureIdx);
	    

	    //Samplings
	    int y = img.getHeight()/20 + 5;
	    int nSamplings = 0;
	    for(NamedSampling ns: study.getNamedSamplings()) {
	    	nSamplings = Math.max(nSamplings, ns.getAllSamplings().size());
	    }
	    if(study.getNamedSamplings().size()>0) {
	    	set(sheet, y++, 0, "Sampling Templates", Style.S_TITLE14BLUE);	    	
		    int count = 0;
		    for(NamedSampling ns: study.getNamedSamplings()) {
		    	set(sheet, y, count, ns.getName(), Style.S_TH_LEFT);
		    	int line = 0;
		    	for(Sampling s: ns.getAllSamplings()) {
			    	set(sheet, y+(++line), count, MiscUtils.removeHtml(s.getDetailsWithMeasurements()), Style.S_TD_LEFT);	    		
		    	}
		    	while(line<nSamplings) {
		    		set(sheet, y+(++line), count, "", Style.S_TD_LEFT);
		    	}
		    	count++;
		    }
	    }
	    
	    //Treatments
	    y+= nSamplings+3;
	    if(study.getNamedTreatments().size()>0) {
	    	set(sheet, y++, 0, "Treatments", Style.S_TITLE14BLUE);	    	
		    int count = 0;
		    for(NamedTreatment nt: study.getNamedTreatments()) {
		    	set(sheet, y, count, nt.getName(), Style.S_TH_LEFT);
	    		set(sheet, y+1, count, nt.getCompoundAndUnit1(), Style.S_TD_LEFT);
	    		set(sheet, y+2, count, nt.getCompoundAndUnit2(), Style.S_TD_LEFT);
		    	count++;
		    }
	    }
	    


	    POIUtils.autoSizeColumns(sheet,-1, true);
	    pict.resize();


	}
	
	
	public static void main(String[] args)  {
		try {
			Spirit.setUser(DAOSpiritUser.loadUser("freyssj"));
			StudyDesignReport wg = new StudyDesignReport();
			wg.populateReport(DAOStudy.getStudyByStudyId("S-00414"));
			wg.export(null);
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.exit(1);
	}


}
