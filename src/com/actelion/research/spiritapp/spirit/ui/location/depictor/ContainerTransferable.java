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

package com.actelion.research.spiritapp.spirit.ui.location.depictor;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import com.actelion.research.spiritcore.business.biosample.Container;

public class ContainerTransferable implements Transferable {

	public static final DataFlavor DATA_FLAVOR = new DataFlavor(List.class, "Container");

	private List<Container> containers;
	
	public ContainerTransferable(Collection<Container> containers) {
		this.containers = new ArrayList<Container>(new LinkedHashSet<Container>(containers));
	}
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] {DATA_FLAVOR};
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(DATA_FLAVOR);
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if(!DATA_FLAVOR.equals(flavor)) throw new UnsupportedFlavorException(flavor);
		return containers;
	}
	
	public List<Container> getContainers() {
		return containers;
	}
	
}