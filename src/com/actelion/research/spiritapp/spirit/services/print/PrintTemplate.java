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

package com.actelion.research.spiritapp.spirit.services.print;

public class PrintTemplate {

	/**
	 * overlapPosition: 1 for left, -1 for right, 0 for none
	 */
	private int overlapPosition = 1;
	/**
	 * barcodePosition: 1 for left, -1 for right, 0 for none
	 */
	private int barcodePosition = 1;
	private boolean showInternalIdFirst = false;
	private boolean showParent = true;
	private boolean showBlocNo = true;
	private boolean showMetadata = true;
	private boolean showComments = true;
	private boolean perLine = false;

	public boolean isShowInternalIdFirst() {
		return showInternalIdFirst;
	}
	public void setShowInternalIdFirst(boolean showInternalIdFirst) {
		this.showInternalIdFirst = showInternalIdFirst;
	}
	public int getOverlapPosition() {
		return overlapPosition;
	}
	public void setOverlapPosition(int overlapPosition) {
		this.overlapPosition = overlapPosition;
	}
	public int getBarcodePosition() {
		return barcodePosition;
	}
	public void setBarcodePosition(int barcodePosition) {
		this.barcodePosition = barcodePosition;
	}
	public boolean isShowParent() {
		return showParent;
	}
	public void setShowParent(boolean showParent) {
		this.showParent = showParent;
	}
	public boolean isShowBlocNo() {
		return showBlocNo;
	}
	public void setShowBlocNo(boolean showBlocNo) {
		this.showBlocNo = showBlocNo;
	}
	public boolean isShowMetadata() {
		return showMetadata;
	}
	public void setShowMetadata(boolean showMetadata) {
		this.showMetadata = showMetadata;
	}
	public boolean isShowComments() {
		return showComments;
	}
	public void setShowComments(boolean showComments) {
		this.showComments = showComments;
	}
	public boolean isPerLine() {
		return perLine;
	}
	public void setPerLine(boolean perLine) {
		this.perLine = perLine;
	}


}