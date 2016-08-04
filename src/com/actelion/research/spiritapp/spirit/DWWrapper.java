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

package com.actelion.research.spiritapp.spirit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.jnlp.IntegrationService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

public class DWWrapper {

	
	public static void main(String[] args) throws Exception {
		try {
			IntegrationService is = (IntegrationService) ServiceManager.lookup("javax.jnlp.IntegrationService");

			is.requestAssociation("application-x/datawarrior", new String[] { "dwar" });
			
			if (!is.hasMenuShortcut()) {
				is.requestShortcut(true, true, "Actelion");
			}
			
		} catch (UnavailableServiceException use) {
			use.printStackTrace();
		}
		

		Class<?> claz = Class.forName("com.actelion.research.datawarrior.DataWarriorActelionLinux", true, DWWrapper.class.getClassLoader());		
		Method method = claz.getDeclaredMethod("main", String[].class);

		List<String> files = new ArrayList<String>();
		for (String s : args) {
			if(!s.startsWith("-")) files.add(s);
		}

		method.invoke(null, (Object) files.toArray(new String[files.size()]));


	}
}
