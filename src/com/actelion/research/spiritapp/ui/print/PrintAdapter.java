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

package com.actelion.research.spiritapp.ui.print;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.print.DirectPrinter;
import com.actelion.research.spiritapp.print.PrintTemplate;
import com.actelion.research.spiritapp.print.PrintableOfContainers;
import com.actelion.research.spiritapp.print.PrintableOfLines;
import com.actelion.research.spiritcore.business.biosample.Container;

public abstract class PrintAdapter {

	public static final int PREVIEW_WIDTH = 1000;
	public static final int PREVIEW_HEIGHT = 200;
	private PrintingTab observer;

	public abstract JComponent getConfigPanel();

	/**One of those 2 function should be overidden*/
	public JComponent getPreviewPanelForList(List<Container> containers) {return null;}
	public JComponent getPreviewPanel(Container container) {return null;}

	public PrintAdapter(PrintingTab observer) {
		this.observer = observer;
	}

	public void fireConfigChanged() {
		observer.fireConfigChanged();
	}

	public abstract void print(List<Container> containers) throws Exception;


	public void eventSetRows(List<Container> containers) {}

	public static void print(List<Container> containers, PrintService ps, Media media, PrintTemplate model) throws Exception {

		if (containers == null || containers.size() == 0)
			throw new Exception("You must select some containers to print");

		List<Container> toPrint = new ArrayList<>();
		for (Container b : containers) {
			if (b != null) {
				toPrint.add(b);
			}
		}


		Printable printable;
		if(model.isPerLine()){
			List<String[]> items = new ArrayList<>();
			for (Container c : containers) {
				items.add(BrotherPrinterAdapter.getPrintableLines(c, model));
			}
			PrintableOfLines p = new PrintableOfLines(items);
			printable = p;
		} else {
			PrintableOfContainers p = new PrintableOfContainers(containers);
			p.setPrintTemplate(model);
			printable = p;

		}

		DirectPrinter printer = new DirectPrinter();
		printer.print(ps, media, printable);

	}

	public static JComponent getPreviewPanel(Container container, PrintService ps, Media media, PrintTemplate model) {

		if (container == null)
			return new JPanel();

		Printable printable;
		if(model.isPerLine()){
			PrintableOfLines p = new PrintableOfLines(Collections.singletonList(BrotherPrinterAdapter.getPrintableLines(container, model)));
			printable = p;
		} else {
			PrintableOfContainers p = new PrintableOfContainers(Collections.singletonList(container));
			p.setPrintTemplate(model);
			printable = p;
		}
		try {

			if (ps == null) throw new Exception("You must select a 'Brother' printer");
			PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
			if (media != null)
				attr.add(media);

			PrinterJob job = PrinterJob.getPrinterJob();
			job.setPrintable(printable);
			job.setPrintService(ps);
			PageFormat pf = job.getPageFormat(attr);

			BufferedImage img = new BufferedImage((int) pf.getWidth() * 3 + 10, (int) pf.getHeight() * 3 + 10, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = (Graphics2D) img.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, img.getWidth(), img.getHeight());
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, img.getWidth() - 1, img.getHeight() - 1);
			g.translate(3, 3); // border
			g.scale(3, 3);
			printable.print(g, pf, 0);
			g.dispose();

			int w = PREVIEW_WIDTH;
			int h = img.getHeight() * w / img.getWidth();
			if (h > PREVIEW_HEIGHT) {
				h = PREVIEW_HEIGHT;
				w = h * img.getWidth() / img.getHeight();
			}

			JLabel lbl = new JLabel();
			lbl.setMinimumSize(new Dimension(w, h));
			lbl.setPreferredSize(new Dimension(w, h));

			BufferedImage scale = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			g = (Graphics2D) scale.getGraphics();
			g.drawImage(img, 0, 0, scale.getWidth(), scale.getHeight(), lbl);
			g.dispose();

			lbl.setIcon(new ImageIcon(scale));
			return lbl;
		} catch (Exception e) {
			e.printStackTrace();
			return new JLabel(e.getMessage());
		}

	}
}
