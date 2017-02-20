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

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import com.actelion.research.spiritapp.spirit.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritcore.util.IOUtils;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class HelpDlg extends JEscapeDialog {

	private JButton exportButton = new JIconButton(IconType.HTML, "Export");
	
	
	private String html;
	private String ver = getClass().getPackage().getImplementationVersion();
	private String HEADER = "<br><img src=\"spirit.jpg\" style='width:700;height:500'><br><div style='font-size:90%'>version "+(ver==null? FormatterUtils.formatDateTime(new Date()):ver)+"</div><br>";
	
	private final Map<String, Image> imageCache = new Hashtable<String, Image>();

	private final ImageEditorPane editorPane = new ImageEditorPane(imageCache) {
		@Override
		public void setText(String t) {
			if(t==null) {
				super.setText(null);
				return;
			}
			t = t.replaceAll("<h0><div>(.*)</div></h0>", "");
			
			//Parse images and add them to cache
			Pattern p = Pattern.compile("<img src=['\"](.*?)['\"].*?>");
			Matcher m = p.matcher(t);
			int MAXWIDTH = scrollPane==null? 900: scrollPane.getWidth()-30;
			while(m.find()) {
				String img = m.group(1);
				try {
					if(!imageCache.containsKey(img)) {
						Image image = ImageIO.read(HelpDlg.class.getResource(img));
						if(image.getWidth(editorPane)>MAXWIDTH) {
							image = image.getScaledInstance(MAXWIDTH, MAXWIDTH * image.getHeight(editorPane) / image.getWidth(editorPane), Image.SCALE_FAST);
						}
						imageCache.put(img, image);
					}
				} catch(Exception e) {
					System.err.println("Could not load "+img);
					e.printStackTrace();
				}
			}
			
			super.setText(t);
			
		}
	};
	private JScrollPane scrollPane = new JScrollPane(editorPane);
	private JTree indexTree;

	
	public HelpDlg() {
		this(null);
	}
	public HelpDlg(final String ref) {
		super(UIUtils.getMainFrame(), "Spirit Help");
		//Load HTML
		InputStream is = null;
		try {
			DefaultMutableTreeNode root = new DefaultMutableTreeNode("Documentation");

			is = HelpDlg.class.getResourceAsStream("help.html");
			html = IOUtils.streamToString(is);
			html = parseHeadersAndAddNumbering(html, root);
			
			indexTree = new JTree(root);
			indexTree.setCellRenderer(new DefaultTreeCellRenderer() {
				@Override
				public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
					super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
					if(((DefaultMutableTreeNode)value).getLevel()<=1) setFont(FastFont.BOLD);
					else if(((DefaultMutableTreeNode)value).getLevel()==2) setFont(FastFont.REGULAR);
					else if(((DefaultMutableTreeNode)value).getLevel()==3) setFont(FastFont.SMALL);
					setIcon(null);
					return this;
				}
			});
			for (int i = 0; i < indexTree.getRowCount(); i++) {
				if(((DefaultMutableTreeNode)indexTree.getPathForRow(i).getLastPathComponent()).getLevel()<=1) {
					indexTree.expandRow(i);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			html = e.getMessage();
		} finally {
			if(is!=null) try{ is.close();}catch(Exception e) {}
		}
		
		scrollPane.addComponentListener(new ComponentAdapter() {
			int lastWidth = 0;
			@Override
			public void componentResized(ComponentEvent e) {
				if(Math.abs(lastWidth - scrollPane.getWidth())<20) return;
				lastWidth = scrollPane.getWidth();
				imageCache.clear();
			}
		});
		editorPane.addHyperlinkListener(new HyperlinkListener() {
			
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType()!=EventType.ACTIVATED) return;
				String desc = e.getDescription();
				if(desc.startsWith("#")) {
					
					String ref = desc.substring(1);
					goRoReference(ref);
				}
			}
		});

		indexTree.addTreeSelectionListener(new TreeSelectionListener() {			
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				selectionChanged();
			}
		});

		editorPane.getDocument().putProperty("imageCache", imageCache);
		editorPane.setEditable(false);
		

		exportButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				new SwingWorkerExtended("Preparing Documentation", editorPane, false) {
					File f;
					@Override
					protected void doInBackground() throws Exception {
						f = exportHtml();						
					}

					@Override
					protected void done() {
						try {
							Desktop.getDesktop().browse(f.toURI());
						} catch(Exception ex) {
							JExceptionDialog.showError(ex);
						}
					}
					
				};
				
			}
		});
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
				UIUtils.createBox(new JScrollPane(indexTree), 
						UIUtils.createHorizontalBox(
								Box.createHorizontalGlue(), exportButton), 
						null, null, null), 
				scrollPane);
		splitPane.setDividerLocation(230);
		
		
		new SwingWorkerExtended("Loading", splitPane, false) {
			@Override
			protected void doInBackground() throws Exception {
				try {Thread.sleep(50);}catch(Exception e) {}
			}			
			@Override
			protected void done() {
				goRoReference(ref);
				toFront();
			}
			
		};
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setContentPane(splitPane);
		setSize(Math.min(dim.width-100, 1600), Math.max(dim.height-100, 780));
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
		
	}
	
	public void goRoReference(String ref) {
		if(title2ref.containsKey(ref)) ref = title2ref.get(ref);
		if(ref==null) {
			indexTree.setSelectionRow(0);
			return;
		}
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) indexTree.getModel().getRoot();
		TreePath path = new TreePath(node);
		loop: while(true) {
			for(int i=0; i<node.getChildCount(); i++) {
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
				String text = getReference(child);
				if(text!=null && (ref.startsWith(text) )) {
					path = path.pathByAddingChild(child);
					node = child; continue loop;
				}
			}
			break;
		} 
		indexTree.setSelectionPath(path);
	}
	
	public void selectionChanged() {
		TreePath path = indexTree.getSelectionPath();
		String last = (String) ((DefaultMutableTreeNode) path.getLastPathComponent()).toString();
		String ref = last.indexOf(". ")<0? "": last.substring(0, last.indexOf(". "));
		
		if(ref.length()==0) {
			//Add the subHeader from the selection						
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) indexTree.getModel().getRoot();
			//node is now the corresponding reference node, add the parent and the children
			StringBuilder sb = new StringBuilder();
			if(node!=null) {
				sb.append(HEADER + "<div style='background:#DDDDDD;padding:5px'>");
				for(int i = 0; i < node.getChildCount(); i++) {
					DefaultMutableTreeNode n = (DefaultMutableTreeNode) node.getChildAt(i);
					sb.append("<a href='#"+getReference(n)+"'>" + n + "</a><br>");
				}
				sb.append("</div>");
			}
			
			//Set the text
			editorPane.setText(getStyleSheet(html) + sb.toString());
			editorPane.setCaretPosition(0);
			editorPane.scrollToReference(ref);

		} else {
			//Extract the text from the selected header 
			int index = html.indexOf("name=\""+ref+"\"");
			if(index<0) {editorPane.setText("Section "+ ref+" not found");return;}
			int index1 = html.lastIndexOf("<h", index);
			if(index1<0)  {editorPane.setText("Section "+ ref+" not found");return;}
			int index2 = index1;
			while(true) {
				index2 = html.indexOf("<h", index2+1);
				if(index2<=0) break; 
				if(html.charAt(index2+2)=='1' || html.charAt(index2+2)=='2' || html.charAt(index2+2)=='3') break;
			} 
			String extract = index2<0? html.substring(index1): html.substring(index1, index2);
			
			//Add the subHeader from the selection						
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) indexTree.getModel().getRoot();
			loop: while(true) {
				for(int i = 0; i < node.getChildCount(); i++) {
					DefaultMutableTreeNode n = (DefaultMutableTreeNode) node.getChildAt(i);
					String nRef = n.toString();
					nRef = nRef.substring(0, nRef.indexOf(". "));
					if((ref+".").equals((nRef+"."))) {
						node = n;
						break loop;
					} else if((ref+".").startsWith((nRef+"."))) {
						node = n;
						continue loop;
					}
				}
				node = null;
				break;
			}
			
			//node is now the corresponding reference node, add the parent and the children
			StringBuilder sbBack = new StringBuilder();
			StringBuilder sb = new StringBuilder();
			if(node!=null) {
				DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
				if(parent!=null) sbBack.append("<div style='background:#DDDDDD;padding:5px'>Back to <a href='#"+getReference(parent)+"'>" + parent + "</a></div><br>");

				sb.append("<br><br><div style='background:#DDDDDD;padding:5px'>");
				for(int i = 0; i < node.getChildCount(); i++) {
					DefaultMutableTreeNode n = (DefaultMutableTreeNode) node.getChildAt(i);
					sb.append("<a href='#"+getReference(n)+"'>" + n + "</a><br>");
				}
				sb.append("</div>");
			}
			
			
			//Set the text
			editorPane.setText(getStyleSheet(html) + sbBack + extract + sb );
			editorPane.setCaretPosition(0);
			

			
		}
	}
	
	private static String getStyleSheet(String html) {
		int index = html.indexOf("<body>");
		if(index<0) return "";
		return html.substring(0, index+6);
	}
	
	public static String getReference(DefaultMutableTreeNode n) {
		String nRef = n.toString();
		if(nRef.indexOf(". ")<0) return "";
		return nRef.substring(0, nRef.indexOf(". "));
	}
	
	private Map<String, String> title2ref = new HashMap<String, String>();
	
	public String parseHeadersAndAddNumbering(String text, DefaultMutableTreeNode root) {
		title2ref.clear();
		
		String regexp = "(<h[123]>)(.*?)(</h[123]>)";
		StringBuilder sb = new StringBuilder(); 
		Pattern pattern = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(text);
		DefaultMutableTreeNode lastH1 = null;
		DefaultMutableTreeNode lastH2 = null;
		int index = 0;
		int count1 = 0;
		int count2 = 0;
		int count3 = 0;
		while(matcher.find()) {
			String ref;
			String title = matcher.group(2);
			if(matcher.group(1).equalsIgnoreCase("<h1>") || lastH1==null) {
				++count1;
				count2 = count3 = 0;
				ref = ""+count1;
				title = ref + ". " + title;
				lastH1 = new DefaultMutableTreeNode(title);
				lastH2 = null;
				root.add(lastH1);
			} else if(matcher.group(1).equalsIgnoreCase("<h2>") || lastH2==null) {
				++count2;
				count3 = 0;
				ref = count1 + "." +  count2;
				title = ref + ". " + title;
				lastH2 = new DefaultMutableTreeNode(title);
				lastH1.add(lastH2);
			} else {
				++count3;
				ref = count1 + "." + count2 + "." + count3;
				title = ref +  ". " + title;
				DefaultMutableTreeNode lastH3 = new DefaultMutableTreeNode(title);
				lastH2.add(lastH3);
			}
			title2ref.put(matcher.group(2), title);
			sb.append(text.substring(index, matcher.start()));
			sb.append(matcher.group(1));	
			sb.append("<a name=\"" + title.substring(0, title.indexOf(". ")) + "\">");
//			sb.append(title.replace("/", "&frasl;"));
			sb.append(title);
			sb.append(matcher.group(3));
			sb.append("</a>");
			index = matcher.end();

		}
		sb.append(text.substring(index));
		return sb.toString();
	}
	
	private String getFullHelp() {
		StringBuilder sb = new StringBuilder();
		int index = html.indexOf("<body>");
		if(index<0) return "Invalid help: no <body> tag";
		sb.append(html.substring(0, index));

		sb.append("<body>");
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) indexTree.getModel().getRoot();
		//Extract the menu
		sb.append(HEADER + "<h1 style='page-break-before: always'>Table Of Content</h1>" + "<div style='background:#DDDDDD;padding:5px;border:1px solid #CCCCCC'>");
		for(int i = 0; i < root.getChildCount(); i++) {
			sb.append("<ul>");
			DefaultMutableTreeNode n1 = (DefaultMutableTreeNode) root.getChildAt(i);
			sb.append("<li><a href='#"+getReference(n1)+"'>" + n1 + "</a></li>");				
			for(int i2 = 0; i2 < n1.getChildCount(); i2++) {
				sb.append("<ul>");
				DefaultMutableTreeNode n2 = (DefaultMutableTreeNode) n1.getChildAt(i2);
				sb.append("<li><a href='#"+getReference(n2)+"'>" + n2 + "</a></li>");
				for(int i3 = 0; i3 < n2.getChildCount(); i3++) {
					sb.append("<ul>");
					DefaultMutableTreeNode n3 = (DefaultMutableTreeNode) n2.getChildAt(i3);
					sb.append("<li><a href='#"+getReference(n3)+"'>" + n3 + "</a></li>");
					
					sb.append("</ul>");
				}
				
				sb.append("</ul>");
			}
			sb.append("</ul>");
		}
		sb.append("</div>");
		sb.append(html.substring(index+6));
		return sb.toString();
	}
	
	private File exportHtml() throws Exception {
		String help = getFullHelp();
		//Change body style
		help = help.replace("body {font-size:12px", "body {font-size:1em; width:700px;margin-left:auto; margin-right:auto}\r\n table {margin-left:auto; margin-right:auto");
		File dir = createTempDirectory(); 
		
		
		//Write Images and set up a maximum size
		StringBuilder sb2 = new StringBuilder();
		Pattern p = Pattern.compile("<img src=['\"](.*?)['\"].*?>");
		Matcher m = p.matcher(help);
		int MAXWIDTH = 650;
		int start = 0;
		while(m.find()) {
			String img = m.group(1);
			int width=0;
			int height=0;
			URL url = HelpDlg.class.getResource(img);
			if(url==null) throw new IllegalArgumentException("Invalid img: "+img);
			Image image = ImageIO.read(url);
			height = image.getHeight(this);
			width = image.getWidth(this);
			if(width>MAXWIDTH) {
				height = (int)(1.0*MAXWIDTH/width*height);
				width = MAXWIDTH; 
			}
			ImageIO.write((RenderedImage) image, "png", new File(dir, img));
			
			sb2.append(help.substring(start, m.start()));
			sb2.append("<img src=\"" + img + "\" width=" + width + "  height="+height+">");
			
			start = m.end();
		}
		sb2.append(help.substring(start));
		
		//Write file
		File helpFile = new File(dir, "spirit_documentation" + (ver==null?"":"v"+ver) + ".html");
		FileWriter w = new FileWriter(helpFile);
		w.write("<html>"+sb2.toString()+"<html>");
		w.flush();
		w.close();
		
		
		return helpFile;
		
	}
	
	public static File createTempDirectory() throws IOException {
	    final File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

	    if(!temp.delete()) {
	        throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
	    }

	    if(!temp.mkdir()) {
	        throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
	    }

	    return temp;
	}

}
