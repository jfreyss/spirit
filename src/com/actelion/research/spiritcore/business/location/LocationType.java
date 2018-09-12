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

package com.actelion.research.spiritcore.business.location;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.persistence.Transient;

public enum LocationType {
	BUILDING("Building",LocationCategory.ADMIN),
	LAB("Lab", 			LocationCategory.ADMIN),
	FREEZER("Freezer", 	LocationCategory.CONTAINER),
	SHELF("Shelf", 		LocationCategory.CONTAINER),
	DRAWER("Drawer", 	LocationCategory.CONTAINER),
	TANK("Tank", 		LocationCategory.CONTAINER),
	TOWER("Tower", 		LocationCategory.CONTAINER),
	BENCH("Bench", 		LocationCategory.CONTAINER),
	BOX("Box", 			LocationCategory.MOVEABLE, LocationLabeling.ALPHA, 8, 12),
	RACK("Rack",		LocationCategory.MOVEABLE, LocationLabeling.ALPHA, 8, 12),
	//BAG("Bag", 			LocationCategory.MOVEABLE, LocationLabeling.NONE, -1, -1),
	//SLIDEBOX("SlideBox",LocationCategory.MOVEABLE, LocationLabeling.NUM_I, 50, 2),
	;



	public static enum LocationCategory {ADMIN, CONTAINER, MOVEABLE}

	private final String name;
	private final LocationCategory category;
	private final LocationLabeling positionType;
	private final int defaultRows;
	private final int defaultCols;

	private LocationType(String name, LocationCategory category) {
		this(name, category, null, -1, -1);

	}

	private LocationType(String name, LocationCategory category, LocationLabeling positionType, int defaultRows, int defaultCols) {
		this.name = name;
		this.category = category;
		this.positionType = positionType;
		this.defaultCols = defaultCols;
		this.defaultRows = defaultRows;
	}

	public LocationType getPreferredChild() {
		switch (this) {
		case BUILDING: return LocationType.LAB;
		case LAB: return LocationType.FREEZER;
		case FREEZER: return LocationType.DRAWER;
		case DRAWER: return LocationType.BOX;
		case SHELF: return LocationType.DRAWER;
		case TANK: return LocationType.TOWER;
		case TOWER: return LocationType.BOX;
		default: return null;
		}

	}


	@Override
	public String toString() {
		return name;
	}

	public String getName() {
		return name;
	}

	public static List<LocationType> getValues() {
		List<LocationType> res = new ArrayList<>();
		for (LocationType cat : values()) {
			res.add(cat);
		}
		return res;
	}

	@Transient
	public static final Map<String, Image> images = new HashMap<>();

	@Transient
	public static final Map<String, Image> smallImages = new HashMap<>();


	public Image getImage() {
		Image image = images.get(name);
		if(image==null) {
			synchronized (images) {
				String n = getName().toLowerCase();
				if(n.indexOf(' ')>0) n = n.substring(0, n.indexOf(' '));
				URL url = getClass().getResource(n+".png");
				if(url!=null) {
					try {
						image = ImageIO.read(url);
					} catch (Exception e) {
						System.err.println("no image for "+name.toLowerCase()+".png  "+e);
					}
				}
				if(image==null) {
					image = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = (Graphics2D) image.getGraphics();
					g.setColor(new Color(255,255,255,255));
					g.dispose();
				}
				images.put(name, image);
			}
		}
		Image img = images.get(name);
		return img;
	}

	public Image getImageThumbnail() {
		if(!smallImages.containsKey(name)) {
			Image img = getImage();
			Image img2 = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
			if(img!=null) {
				img2 = img.getScaledInstance(18, 18, Image.SCALE_SMOOTH);
			}
			smallImages.put(name, img2);
		}
		return smallImages.get(name);
	}
	/**
	 * @return the labeling
	 */
	public LocationLabeling getPositionType() {
		return positionType==null?LocationLabeling.NONE: positionType;
	}

	public LocationCategory getCategory() {
		return category;
	}

	public int getDefaultCols() {
		return defaultCols;
	}

	public int getDefaultRows() {
		return defaultRows;
	}

	public static List<LocationType> getPossibleRoots() {
		List<LocationType> roots = new ArrayList<>();
		for (LocationType t : values()) {
			if(t.getCategory()!=LocationCategory.MOVEABLE) roots.add(t);
		}
		return roots;
	}

}
