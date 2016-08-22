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

package com.actelion.research.spiritapp.spirit.ui.help;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;

import com.actelion.research.spiritapp.animalcare.AnimalCare;
import com.actelion.research.spiritapp.animalcare.ui.monitor.MonitoringDlg;
import com.actelion.research.spiritapp.animalcare.ui.monitor.MonitoringOverviewDlg;
import com.actelion.research.spiritapp.animalcare.ui.randomize.RandomizationDlg;
import com.actelion.research.spiritapp.animalcare.ui.sampleweighing.SampleWeighingDlg;
import com.actelion.research.spiritapp.bioviewer.BioViewer;
import com.actelion.research.spiritapp.slidecare.SlideCare;
import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.admin.BiotypeEditDlg;
import com.actelion.research.spiritapp.spirit.ui.admin.TestEditDlg;
import com.actelion.research.spiritapp.spirit.ui.admin.user.EmployeeEditDlg;
import com.actelion.research.spiritapp.spirit.ui.admin.user.EmployeeGroupEditDlg;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTab;
import com.actelion.research.spiritapp.spirit.ui.biosample.edit.CreateChildrenDlg;
import com.actelion.research.spiritapp.spirit.ui.biosample.edit.EditBiosampleDlg;
import com.actelion.research.spiritapp.spirit.ui.biosample.form.BiosampleFormDlg;
import com.actelion.research.spiritapp.spirit.ui.config.ConfigDlg;
import com.actelion.research.spiritapp.spirit.ui.container.CheckinDlg;
import com.actelion.research.spiritapp.spirit.ui.container.CheckoutDlg;
import com.actelion.research.spiritapp.spirit.ui.location.LocationTab;
import com.actelion.research.spiritapp.spirit.ui.location.edit.LocationBatchEditDlg;
import com.actelion.research.spiritapp.spirit.ui.pivot.PivotAnalyzerDlg;
import com.actelion.research.spiritapp.spirit.ui.result.ResultTab;
import com.actelion.research.spiritapp.spirit.ui.result.edit.EditResultDlg;
import com.actelion.research.spiritapp.spirit.ui.result.edit.PivotDlg;
import com.actelion.research.spiritapp.spirit.ui.study.ManageSamplesDlg;
import com.actelion.research.spiritapp.spirit.ui.study.SetLivingStatusDlg;
import com.actelion.research.spiritapp.spirit.ui.study.StudyHistoryDlg;
import com.actelion.research.spiritapp.spirit.ui.study.StudyTab;
import com.actelion.research.spiritapp.spirit.ui.study.edit.AttachAnimalsManuallyDlg;
import com.actelion.research.spiritapp.spirit.ui.study.sampling.NamedSamplingDlg;
import com.actelion.research.spiritapp.spirit.ui.study.wizard.StudyInfoDlg;
import com.actelion.research.spiritapp.spirit.ui.study.wizard.StudyWizardDlg;
import com.actelion.research.spiritapp.spirit.ui.study.wizard.group.StudyGroupDlg;
import com.actelion.research.spiritapp.spirit.ui.study.wizard.phase.PhaseDlg;
import com.actelion.research.spiritapp.spirit.ui.study.wizard.treatment.StudyTreatmentDlg;
import com.actelion.research.spiritapp.stockcare.StockCare;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class HelpBinder {

	private static Map<Class<?>, String> class2Ref = null;
	
	
	static {
		if(class2Ref==null) {
			class2Ref = new HashMap<>();
			class2Ref.put(Spirit.class, null);
			class2Ref.put(StudyTab.class, "Studies Visualization");
			class2Ref.put(BiosampleTab.class, "Biosamples Visualization");
			class2Ref.put(LocationTab.class, "Locations Visualization");
			class2Ref.put(ResultTab.class, "Results Visualization");
			
			class2Ref.put(StudyWizardDlg.class, "Edit Study Design");
			class2Ref.put(StudyInfoDlg.class, "Study Infos");
			class2Ref.put(PhaseDlg.class, "Phases");
			class2Ref.put(StudyGroupDlg.class, "Groups");
			class2Ref.put(StudyTreatmentDlg.class, "Treatments");
			class2Ref.put(NamedSamplingDlg.class, "Sampling Templates");
			class2Ref.put(RandomizationDlg.class, "Assignment Wizard");
			class2Ref.put(AttachAnimalsManuallyDlg.class, "Manual Assigment");			
			class2Ref.put(MonitoringOverviewDlg.class, "Monitoring Overview");
			class2Ref.put(MonitoringDlg.class, "Monitoring Dialog");
			class2Ref.put(SetLivingStatusDlg.class, "Replace / Set Living Status");
			class2Ref.put(ManageSamplesDlg.class, "Manage Samples");
			class2Ref.put(SampleWeighingDlg.class, "Sample Weighing");
			class2Ref.put(StudyHistoryDlg.class, "Study History");
			
			class2Ref.put(EditBiosampleDlg.class, "Editing in Batch Mode");
			class2Ref.put(BiosampleFormDlg.class, "Editing in Form Mode");
			class2Ref.put(CreateChildrenDlg.class, "Create Aliquots");
			class2Ref.put(CheckinDlg.class, "Checkin / Relocate");
			class2Ref.put(CheckoutDlg.class, "Checkout");

			//there buttons need to be added
			class2Ref.put(LocationBatchEditDlg.class, "Editing Locations");
			class2Ref.put(EditResultDlg.class, "Edit Result Dialog");
			class2Ref.put(PivotDlg.class, "Pivot per Phase");

			
			class2Ref.put(PivotAnalyzerDlg.class, "Analysis");
			
			class2Ref.put(EmployeeGroupEditDlg.class, "Edit Groups");
			class2Ref.put(EmployeeEditDlg.class, "Edit Users");
			class2Ref.put(BiotypeEditDlg.class, "Edit Biotypes");
			class2Ref.put(TestEditDlg.class, "Edit Tests");
			class2Ref.put(ConfigDlg.class, "Balance Configuration");

			class2Ref.put(AnimalCare.class, "AnimalCare");
			class2Ref.put(StockCare.class, "StockCare");
			class2Ref.put(BioViewer.class, "BioViewer");
			class2Ref.put(SlideCare.class, "SlideCare");

		}
		
	}
	
	public static JButton createHelpButton() {
		final JButton res = new JIconButton(IconType.HELP);
		res.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				showHelp(res);
			}
		});
		return res;
	}
	
	public static void bindHelp() {
		
		Toolkit.getDefaultToolkit ().addAWTEventListener (new AWTEventListener() {
			
			@Override
			public void eventDispatched(AWTEvent event) {
				if(event instanceof KeyEvent && ((KeyEvent)event).getKeyCode()==112 && ((KeyEvent)event).getID()==KeyEvent.KEY_PRESSED) {					
					if(event.getSource()!=null && event.getSource() instanceof Component)  {
						showHelp((Component) event.getSource());
					}
				}
			}
		}, AWTEvent.KEY_EVENT_MASK); 


	}
	
	public static void showHelp(Component source) {
		while(source!=null && source.getParent()!=null && !class2Ref.containsKey(source.getClass())) {
			source = source.getParent();
		}
		String ref = class2Ref.get(source==null? null: source.getClass());
		showHelp(ref);

	}
	
	public static void showHelp(String ref) {
		try { 
			String url = "https://jfreyss.github.io/spirit/doc/index.html#"+URLEncoder.encode(ref, "UTF-8");
			Desktop.getDesktop().browse(new URI(url));
		} catch(Exception e) {
			JExceptionDialog.showError(e);
		}
		
	}

}
