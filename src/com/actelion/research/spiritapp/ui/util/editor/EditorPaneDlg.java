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

package com.actelion.research.spiritapp.ui.util.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.ui.util.lf.LF;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.UIUtils;

/**
 * EditorPane dlg handling some special features:
 * <img src="histo://123456"> to create an histogram
 * 
 * @author freyssj
 *
 */
public class EditorPaneDlg extends JEscapeDialog {

	private ImageEditorPane editorPane = new ImageEditorPane();

	public EditorPaneDlg(Frame frame, String title) {
		super(frame, title, false);
		init();
	}
	public EditorPaneDlg(Frame frame, String title, String html) {
		super(frame, title, false);
		setHtml(html);
		init();		
	}
	
	public EditorPaneDlg(Dialog dlg, String title, String html) {
		super(dlg, title, false);
		setHtml(html);
		init();		
	}
	
	public ImageEditorPane getEditorPane() {
		return editorPane;
	}
	
	private void init() {
		LF.initComp(editorPane);
		JButton closeButton = new  JButton("Close");
		closeButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		add(BorderLayout.CENTER, new JScrollPane(editorPane));
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), closeButton));
		setSize(1050, 740);
		setLocationRelativeTo(UIUtils.getMainFrame());		
	}
	
	
	public void setHtml(String html) {
		createImages(html);
		editorPane.setText(html);
		editorPane.setCaretPosition(0);
	}
	

	private void createImages(String html) {
		Pattern pattern = Pattern.compile("<img src=['\"](.*?)['\"].*?>");
		Matcher matcher = pattern.matcher(html);
		while(matcher.find()) {
			final String url = matcher.group(1);
			if(url.startsWith("histo://") && editorPane.getImageCache().get(url)==null) {
				BufferedImage image = createHisto(url.substring("histo://".length()));
				editorPane.getImageCache().put(url, image);
			}
		}
	}
	
	private BufferedImage createHisto(String code) {
		int max = 5;
		
		BufferedImage img = new BufferedImage(4*code.length(), max*3, BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		g.setColor(UIUtils.getColor(240,240,240));
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		g.setColor(Color.DARK_GRAY);


		for (int i = 0; i < code.length(); i++) {
			char c = code.charAt(i);
			int n = c-'0';
			if(n<0 || n>=10) continue;
			
			g.fillRect(i*4, img.getHeight()-3*n, 4, 3*n);
			
		}
		g.dispose();
		return img;
	}
	
	public static void main(String[] args) {
		new EditorPaneDlg((JFrame) null, "TEST", "MY TEST <br> <h1>IMG</h1> "
				+ "<ul>"
				+ "<li><b>HISTO 123</b><img src='histo://123'>"
				+ "<li><b>HISTO 0123210</b><img src='histo://0123210'>"
				+ "<li><b>HISTO 123456</b><img src='histo://123456'>"
				+ "<li><b>HISTO 123456789</b><img src='histo://123456789'>"
				+ "</ul><hr>"
				+ "<h1>done</h1>").setVisible(true);
	}
}


