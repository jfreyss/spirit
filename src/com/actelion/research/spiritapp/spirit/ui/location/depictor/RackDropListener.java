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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.location.Direction;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.util.ui.JExceptionDialog;

/**
 * The LocationDropListener is responsible for the handling moving containers in location.
 * It takes care of DnD but also dbl-click and predicting where containers will be moved.
 * @author freyssj
 *
 */
public class RackDropListener implements DropTargetListener, DragSourceListener, DragGestureListener {

	private final RackDepictor rackPanel;
	private final Map<Integer, Color> pos2Color = new HashMap<Integer, Color>();
	private final List<Integer> droppedPoses = new ArrayList<Integer>();

	private static Direction direction = Direction.LEFT_RIGHT;
	private int mouseOffsetPosition = -1;

	/**
	 * @param locationPanel
	 */
	protected RackDropListener(RackDepictor rackPanel) {
		this.rackPanel = rackPanel;
	}


	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		pos2Color.clear();
		Set<Container> sel = this.rackPanel.getSelectedContainers();
		if(sel.size()<0) return;

		boolean acceptDrag = false;
		if(rackPanel.getDepictor()!=null && this.rackPanel.getDepictor().getRackDepictorListeners()!=null) {
			for (RackDepictorListener listener : this.rackPanel.getDepictor().getRackDepictorListeners()) {
				if(listener==null || !listener.acceptDrag()) continue;
				acceptDrag = true;
			}
		}

		if(acceptDrag) {
			Transferable transferable = new ContainerTransferable(sel);
			dge.startDrag(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR), transferable, this);

			if(dge.getComponent() instanceof RackDepictor) {
				int currentPos = ((RackDepictor) dge.getComponent()).getPosAt(dge.getDragOrigin().x, dge.getDragOrigin().y);
				mouseOffsetPosition = currentPos;
			} else {
				mouseOffsetPosition = -1;
			}

		}


	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
	}

	@SuppressWarnings("unchecked")
	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		DropTarget target = (DropTarget) dtde.getSource();
		try {
			List<Container> containers = (List<Container>) dtde.getTransferable().getTransferData(ContainerTransferable.DATA_FLAVOR);
			if(containers.size()==0 || !(target.getComponent() instanceof RackDepictor)) return;

			RackDepictor rp = (RackDepictor) target.getComponent();
			boolean ok = computeDroppedPoses(rp.getPosAt(dtde.getLocation().x, dtde.getLocation().y), mouseOffsetPosition, containers);

			if(!ok) dtde.rejectDrag();
			else dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);

			this.rackPanel.repaint();
		} catch (Exception e) {
			dtde.rejectDrag();
			e.printStackTrace();
		}
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
		pos2Color.clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void drop(DropTargetDropEvent dtde) {

		pos2Color.clear();

		try {
			List<Container> containers = (List<Container>) dtde.getTransferable().getTransferData(ContainerTransferable.DATA_FLAVOR);
			if(containers.size()>0) {
				for (RackDepictorListener listener : this.rackPanel.getDepictor().getRackDepictorListeners()) {
					if(listener==null || !listener.acceptDrag()) continue;
					listener.containerDropped(containers, droppedPoses);
				}
			}
			dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
			this.rackPanel.setSelectedPoses(Container.getPoses(containers));
			this.rackPanel.doLayout();
			this.rackPanel.repaint();
		} catch (Exception e) {
			dtde.rejectDrop();
			JExceptionDialog.showError(e);
		}
		dtde.dropComplete(true);

	}


	@Override
	public void dragEnter(DragSourceDragEvent dsde) {
	}

	@Override
	public void dragOver(DragSourceDragEvent dsde) {
	}

	@Override
	public void dropActionChanged(DragSourceDragEvent dsde) {
	}

	@Override
	public void dragExit(DragSourceEvent dse) {
	}

	@SuppressWarnings("unchecked")
	@Override
	public void dragDropEnd(DragSourceDropEvent dsde) {
		pos2Color.clear();
		mouseOffsetPosition = -1;
		try {
			List<Container> containers = (List<Container>) dsde.getDragSourceContext().getTransferable().getTransferData(ContainerTransferable.DATA_FLAVOR);
			this.rackPanel.setSelectedPoses(Container.getPoses(containers));
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.rackPanel.repaint();

	}

	/**
	 * Computes and highlights the new positions where the container could be dropped
	 * @param startPos
	 * @param mouseOffsetPosition (-1 to place the containers one by one starting from newpos and following the direction, >=0 to place them as they are correctly but with the given offset)
	 * @param containers
	 * @return true if this is possible
	 */
	public boolean computeDroppedPoses(int startPos, int mouseOffsetPosition, Collection<Container> containers) {

		droppedPoses.clear();
		pos2Color.clear();

		Location location = rackPanel.getBioLocation();
		if(location==null) return false;
		if(startPos<0) return false;
		if(containers.size()==0) return false;

		int index = 0;

		int smallestPos = Integer.MAX_VALUE;
		boolean allWithScannedPositions = Container.isAllWithScannedPositions(containers);
		for (Container c : containers) {
			int oriPos;
			if(allWithScannedPositions) {
				try {
					oriPos = location.getLabeling().getPos(location, c.getScannedPosition());
				} catch(Exception e) {
					e.printStackTrace();
					return false;
				}
			} else {
				oriPos = c.getPos();
			}
			smallestPos = Math.min(smallestPos, oriPos);
		}

		Map<Integer, Container> pos2containers = location.getContainersMap();
		for (Container c : containers) {
			int pos;
			if(mouseOffsetPosition>=0) {
				//Dragging from depictor to depictor
				//Translate the existing position
				pos = startPos + c.getPos() - mouseOffsetPosition;
			} else {
				int oriPos;
				if(allWithScannedPositions) {
					try {
						oriPos = location.getLabeling().getPos(location, c.getScannedPosition());
					} catch(Exception e) {
						e.printStackTrace();
						return false;
					}
				} else  {
					oriPos = c.getPos();
				}
				Direction d = direction==Direction.DEFAULT? location.getLocationType().getPositionType().getDefaultDirection(): direction;
				if(d==Direction.PATTERN) {
					pos = location.getLabeling().getNextForPattern(location, startPos, oriPos-smallestPos);
				} else {
					pos = location.getLabeling().getNext(location, startPos, d, index);
				}
				index++;
			}
			boolean occupied = pos2containers.get(pos)!=null && !containers.contains(pos2containers.get(pos));

			boolean ok;
			if(pos==c.getPos() && location.equals(c.getLocation()) ) {
				//Nothing
				ok = true;
			} else if(location.getSize()>0 && pos>=location.getSize()) {
				pos2Color.put(pos, Color.RED);
				ok = false;
			} else if(!occupied) {
				pos2Color.put(pos, Color.GREEN);
				ok = true;
			}  else {
				pos2Color.put(pos, Color.RED);
				ok = false;
			}


			//Ok position, add it in the preview
			if(ok && pos>=0) {

				if(droppedPoses.contains(pos)) {
					pos2Color.put(pos, Color.RED);
					ok = false;
				} else {
					droppedPoses.add(pos);
				}
			}
		}
		return droppedPoses.size()==containers.size();
	}

	public List<Integer> getDroppedPoses() {
		return droppedPoses;
	}

	public Map<Integer, Color> getPos2Color() {
		return pos2Color;
	}
	public static Direction getDirection() {
		return direction;
	}
	public static void setDirection(Direction d) {
		direction = d;
	}

}