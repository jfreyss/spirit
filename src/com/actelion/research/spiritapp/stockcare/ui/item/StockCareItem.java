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

package com.actelion.research.spiritapp.stockcare.ui.item;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import com.actelion.research.spiritapp.spirit.ui.util.icons.ImageFactory;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.pivot.ColumnPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotItemFactory;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOTest;

public class StockCareItem {

	private String name;
	private final Biotype[] biotypes;
	private final AbstractAction[] actions;

	public static List<StockCareItem> getStockCareItems() {
		if(items==null) {

			synchronized (StockCareItem.class) {
				if(items==null) {
					items = new ArrayList<StockCareItem>();
					if(DBAdapter.getInstance().isInActelionDomain()) {
						//Items configured by Actelion
						add("Cells", "Cell Aliquot", null);
						add("Antibodies", "Antibody", new AbstractAction[] {new AntibodySelectAction()});
						add("Primers", "Primer", null);
						add("Bacteria", "Bacteria", null);
						items.add(null);
					}

					//Items from other components: biotypes which are leaves, component, not hidden
					Set<Biotype> referenced = new HashSet<>();

					//Find biotypes references in other types
					for(Biotype biotype: DAOBiotype.getBiotypes()) {
						if(biotype.getParent()!=null) referenced.add(biotype.getParent());
						for (BiotypeMetadata mt : biotype.getMetadata()) {
							if(mt.getDataType()==DataType.BIOSAMPLE && mt.getParameters()!=null && mt.getParameters().length()>0) {
								Biotype biotype2 = DAOBiotype.getBiotype(mt.getParameters());
								if(biotype2!=null) referenced.add(biotype2);
							}
						}
					}
					System.out.println("StockCareItem.getStockCareItems() "+referenced);
					//Add all other purifified/library biotypes not referenced elsewhere
					for(Biotype biotype: DAOBiotype.getBiotypes()) {
						if(biotype.getCategory()==BiotypeCategory.LIVING || biotype.getCategory()==BiotypeCategory.SOLID || biotype.getCategory()==BiotypeCategory.LIQUID) continue;
						if(biotype.isHidden()) continue;
						if(referenced.contains(biotype)) continue;
						add(biotype.getName(), biotype, null);
					}
				}
			}
		}
		return items;
	}


	private static List<StockCareItem> items = null;

	private static void add(String name, String aliquotType, AbstractAction[] actions) {
		add(name, DAOBiotype.getBiotype(aliquotType), actions);
	}

	private static void add(String name, Biotype type, AbstractAction[] actions) {
		try {
			if(type==null) throw new Exception("no type for "+name);

			for (StockCareItem item : items) {
				if(item!=null && item.getBiotypes()[item.getBiotypes().length-1].equals(type)) return; //already added
			}

			List<Biotype> hierarchy = new ArrayList<>();
			hierarchy.add(type);
			while(type.getParent()!=null) {
				type = type.getParent();
				hierarchy.add(0, type);
			}

			//Ignore type with a too comple hierarchy
			if(hierarchy.size()>3) return;
			items.add(new StockCareItem(name, hierarchy.toArray(new Biotype[hierarchy.size()]), actions==null? new AbstractAction[0]: actions));

		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public StockCareItem(String name, Biotype[] biotypes, AbstractAction[] actions) throws Exception {
		this.name = name;
		this.biotypes = biotypes;
		this.actions = actions;

		if(biotypes.length<1 || biotypes.length>3)  throw new Exception("The biotypes for "+name + " must between [1,3]");
		for (Biotype biotype : biotypes) {
			if(biotype==null) throw new Exception("The biotype for "+name + " cannot be null");
		}
	}

	public String getName() {
		return name;
	}

	public Biotype[] getBiotypes() {
		return biotypes;
	}

	public Biotype getMainBiotype() {
		return biotypes[biotypes.length-1];
	}

	public Set<Biotype> getAggregatedBiotypes() {
		Set<Biotype> res = new HashSet<Biotype>();
		Set<Biotype> already = new HashSet<Biotype>(Arrays.asList(biotypes));
		for(Biotype b: biotypes) {
			for(BiotypeMetadata mt: b.getMetadata()) {
				if(mt.getDataType()==DataType.BIOSAMPLE && mt.getParameters()!=null) {
					Biotype biotype = DAOBiotype.getBiotype(mt.getParameters());
					if(biotype!=null && !already.contains(biotype)) {
						res.add(biotype);
					}
				}
			}
		}

		return res;
	}


	public AbstractAction[] getActions() {
		return actions;
	}

	public ImageIcon getIcon(boolean fullSize) {
		Biotype biotype = biotypes[biotypes.length-1];
		Image img;
		if(fullSize) {
			img = ImageFactory.getImage(biotype);
		} else {
			img = ImageFactory.getImageThumbnail(biotype);
		}
		return new ImageIcon(img);
	}


	public ImageIcon getIcon() {
		return getIcon(false);
	}

	public boolean hasResults() {
		return name.equals("Bacteria");
	}

	public Test getDefaultTest() {
		if(name.equals("Bacteria")) {
			return DAOTest.getTest("MIC");
		} else{
			return null;
		}
	}

	public PivotTemplate[] getDefaultTemplates() {
		if(name.equals("Bacteria")) {
			PivotTemplate tpl1 = new ColumnPivotTemplate("Bacteria") {
				@Override
				public void init(List<Result> results) {
					super.init(results);
					setWhere(new PivotItemFactory.PivotItemBiosampleLinker(new BiosampleLinker(getMainBiotype(), getMainBiotype().getMetadata("Genus"))), Where.ASROW);
					setWhere(new PivotItemFactory.PivotItemBiosampleLinker(new BiosampleLinker(getMainBiotype(), getMainBiotype().getMetadata("Species"))), Where.ASROW);
				}
			};
			PivotTemplate tpl2 = new ColumnPivotTemplate("Range") {
				@Override
				public void init(List<Result> results) {
					super.init(results);
					setWhere(PivotItemFactory.BIOSAMPLE_TOPID, Where.MERGE);
					setWhere(PivotItemFactory.BIOSAMPLE_TOPNAME, Where.MERGE);
					setWhere(new PivotItemFactory.PivotItemBiosampleLinker(new BiosampleLinker(getMainBiotype(), getMainBiotype().getMetadata("Genus"))), Where.ASROW);
					setWhere(new PivotItemFactory.PivotItemBiosampleLinker(new BiosampleLinker(getMainBiotype(), getMainBiotype().getMetadata("Species"))), Where.ASROW);
					setAggregation(Aggregation.RANGE);
				}
			};
			return new PivotTemplate[]{tpl1, tpl2};


		} else {
			return null;
		}
	}



}
