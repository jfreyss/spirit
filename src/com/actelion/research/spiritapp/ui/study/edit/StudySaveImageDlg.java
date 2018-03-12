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

package com.actelion.research.spiritapp.ui.study.edit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.ui.study.depictor.StudyDepictor;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

public class StudySaveImageDlg {
	
	private Study study ;
	private JPanel imagePanel = new JPanel(new BorderLayout());
	private BufferedImage img;
	private JLabel imgSizeLabel = new JLabel();

	public static void showSaveImageDlg(StudyDepictor depictor) {
		new StudySaveImageDlg(depictor.getStudy(), depictor.getSizeFactor());
	}
	
	public static void showSaveImageDlg(Study study) {
		new StudySaveImageDlg(study, 1);
	}
	
	
	private StudySaveImageDlg(Study study, int zoomFactor) {
		this.study = study;
		final JGenericComboBox<Integer> zoomField = new JGenericComboBox<Integer>(new Integer[] {0, 1, 2, 3}, false);
		
		zoomField.setSelection(zoomFactor);
		zoomField.setPreferredWidth(50);
		imgSizeLabel.setPreferredSize(new Dimension(120, 22));
		
		JButton clipboardButton = new JButton("Export to Clipboard");
		clipboardButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(img!=null) {
					ImageSelection.copyImageToClipboard(img);
					JOptionPane.showMessageDialog(null, "Image exported to clipboard.\nYou can paste it now in Word or other applications.", "Export Study Image", JOptionPane.INFORMATION_MESSAGE);
				}
			}
		});
		JPanel line1 = UIUtils.createHorizontalBox(
				new JLabel("Zoom:"), zoomField,
				Box.createHorizontalStrut(15),
				clipboardButton,
				Box.createHorizontalStrut(30),
				imgSizeLabel,
				Box.createHorizontalGlue());
		
		JPanel box = new JPanel(new BorderLayout());
		box.add(BorderLayout.NORTH, line1);
		box.add(BorderLayout.CENTER, new JScrollPane(imagePanel));
		box.setPreferredSize(new Dimension(750, 500));
		
		zoomField.addItemListener(new ItemListener() {			
			@Override
			public void itemStateChanged(ItemEvent e) {
				makeImageOn(imagePanel, zoomField.getSelection());
			}
		});
		makeImageOn(imagePanel, zoomField.getSelection());
		
		JFileChooser chooser = new JFileChooser();
		chooser.setAccessory(box);
		chooser.setSelectedFile(new File(study.getStudyId()+".png"));
		int res = chooser.showSaveDialog(UIUtils.getMainFrame());
		if(res!=JOptionPane.YES_OPTION) return;
		try {
			ImageIO.write(img, "PNG", chooser.getSelectedFile());
		} catch(Exception e) {
			JExceptionDialog.showError(e);
		}
		
		
	}
	
	private void makeImageOn(JPanel imagePanel, int zoom) {
		StudyDepictor sd = new StudyDepictor();
		sd.setSizeFactor(zoom);
		sd.setStudy(study);
		img = sd.getImage();
		imagePanel.removeAll();
		imagePanel.add(BorderLayout.CENTER, new JLabel(new ImageIcon(img)));
		imagePanel.updateUI();
		imgSizeLabel.setText("Image size: " + img.getWidth()+"x"+img.getHeight());

	}
	
	public static class ImageSelection implements Transferable {
	    private Image image;
	   
	    public static void copyImageToClipboard(Image image) {
	        ImageSelection imageSelection = new ImageSelection(image);
	        java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
	        toolkit.getSystemClipboard().setContents(imageSelection, null);
	    }
	   
	    public ImageSelection(Image image) {
	        this.image = image;
	    }
	   
	    @Override
	    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
	        if (flavor.equals(DataFlavor.imageFlavor) == false) {
	            throw new UnsupportedFlavorException(flavor);
	        }
	        return image;
	    }
	    @Override
	    public boolean isDataFlavorSupported(DataFlavor flavor) {
	        return flavor.equals(DataFlavor.imageFlavor);
	    }
	    @Override
	    public DataFlavor[] getTransferDataFlavors() {
	        return new DataFlavor[] {
	            DataFlavor.imageFlavor
	        };
	    }
	}
	


}
