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

package com.actelion.research.util.ui;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import com.actelion.research.util.WikiNewsFeed;

/**
 * SplashScreen utility class. To use this class, the best is to extend it and define:
 * <li> the url of the image (optional)
 * <li> the path of the executable (recommended)
 * <li> the contact person (optional)
 * 
 * 
 * In the application: 
 * <pre>
 * SplashConfig congig = new SplashConfig(...)
 * SplashScreen2.show(config)
 * </pre>
 * 
 * To create an about menu:
 * <pre>
 * JMenu helpMenu = new JMenu("Help");
 * helpMenu(SplashScreen.createAboutAction(config));
 * </pre>
 * 
 * @author freyssj
 *
 */
public class SplashScreen2 extends JFrame {
	
	private int minimumTimeSeconds = 5;
	
	public static class SplashConfig {
		URL urlImage;
		String title; 
		String signature; 
		String wikiNewsFeed;

		public SplashConfig(URL urlImage, String title, String signature) {
			this(urlImage, title, signature, null);
		}
		public SplashConfig(URL urlImage, String title, String signature, String wikiNewsFeed) {
			this.urlImage = urlImage;
			this.title = title;
			this.signature = signature;
			this.wikiNewsFeed = wikiNewsFeed;
		}
	}

	private final static SimpleDateFormat df  = new SimpleDateFormat("d MMM yyyy");

	private final JEditorPane editor = new JEditorPane("text/html", "");
	private final JLabel lastVersionLabel = new JLabel();
	private final JButton closeButton = new JButton(new Action_Close());
	private final List<News> news = new ArrayList<News>();
	
	private boolean about;
	private String wikiPage = null;
    

	private static class News {
		public Date date;
		public String txt;
		public String link;
		public String body;
		
		public News(Date date, String text) {
			this.date = date;
			this.txt = text;
		}
		public News(Date date, String text, String link, String body) {
			this.date = date;
			this.txt = text;
			this.link = link;
			this.body = body;
		}
		@Override
		public String toString() {
			return "[News:"+df.format(date)+"]";
		}
	}
	
	private class Action_Close extends AbstractAction {
		public Action_Close() {
			super("Close");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	}
	
	private static class AboutAction extends AbstractAction {	
		private SplashConfig config;
		private SplashScreen2 splashScreen;
		public AboutAction(SplashConfig config) {
			super("About");
			this.config = config;
		}
		public AboutAction(SplashScreen2 splashScreen) {
			super("About");
			this.splashScreen = splashScreen;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(config!=null) {
				SplashScreen2.showAbout(config);
			} else {
				splashScreen.setAbout(true);
				splashScreen.setVisible(true);
			}
		}
	}

//	
//	@Override
//	public void setVisible(final boolean b) {
//		SwingUtilities.invokeLater(new Runnable() {			
//			@Override
//			public void run() {
//				SplashScreen2.super.setVisible(b);
//			}
//		});
//		
//		new Thread() {
//			@Override
//			public void run() {
//				try {Thread.sleep(5000);} catch(Exception e) {}
//				SplashScreen2.super.dispose();
//			}
//		}.start();
//	}
	
	
	@Deprecated
	public static AbstractAction createAboutAction(SplashScreen2 splashScreen) {
		return new AboutAction(splashScreen);
	}

	public static AbstractAction createAboutAction(SplashConfig config) {
		return new AboutAction(config);
	}
	
	@Deprecated
	public SplashScreen2(URL urlImage, String title, String signature) {
		this(urlImage, title, signature, null);
	}
	
	@Deprecated
	public SplashScreen2(URL urlImage, String title, String signature, String wikiNewsFeed) {
		this(new SplashConfig(urlImage, title, signature, wikiNewsFeed));
	}
	
	public SplashScreen2(SplashConfig config) {		
		super("SplashScreen");
		assert config!=null;
		
		setBackground(Color.WHITE);
		setForeground(Color.BLACK);
		setAbout(false);	
		if(config.title!=null) {
			setTitle(config.title);
		}
		if(config.urlImage!=null) {
			try {
				setIconImage(ImageIO.read(config.urlImage));
			} catch (Exception e) {
			}
		}


        //Background Image
        JLabel splashIcon;
        int width = 400;
        if(config.urlImage!=null) {
        	ImageIcon ico = new ImageIcon(config.urlImage);
        	splashIcon = new JLabel(ico);
        	width = Math.max(width, ico.getIconWidth());
        } else {
        	splashIcon = new JLabel("Application News");
        }
        

		JPanel buttons = new JPanel(new GridBagLayout());
		buttons.setPreferredSize(new Dimension(width, 40));
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(5, 1, 5, 0);
        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 0; buttons.add(lastVersionLabel, c);
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(1, 0, 1, 1);
        c.gridx = 2; c.gridy = 0; c.weightx = 0; c.weighty = 0; buttons.add(closeButton, c);	        
		buttons.setBackground(getBackground());
		
		
		closeButton.setVisible(false);
		

		//ContentPane
        JPanel contentPane = new JPanel(new GridBagLayout());
//        contentPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE), BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 2, 2, Color.LIGHT_GRAY), BorderFactory.createMatteBorder(2, 2, 2, 2, Color.DARK_GRAY))));
        contentPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE), BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK)));
        contentPane.setBackground(getBackground());
        
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH; 
        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 0; contentPane.add(splashIcon, c);
        
        
        if(config.wikiNewsFeed!=null) {
	        JScrollPane sp = new JScrollPane(editor);
	        sp.setPreferredSize(new Dimension(width, 180));
			sp.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.DARK_GRAY));
			sp.setViewportBorder(null);
			editor.setBorder(null);
			editor.setEditable(true);
			editor.setEditable(false);
	        c.gridx = 0; c.gridy = 1; c.weightx = 1; c.weighty = 1; contentPane.add(sp, c);
			setWikiNewsFeed(config.wikiNewsFeed);
        }
		
        c.gridx = 0; c.gridy = 2; c.weightx = 1; c.weighty = 0; contentPane.add(buttons, c);
        
        
        Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        Dimension labelSize = getPreferredSize();
        setLocation(screenSize.width/2 - (labelSize.width/2),
                    screenSize.height/2 - (labelSize.height/2));
        screenSize = null;
        labelSize = null;
        
        
		editor.addHyperlinkListener(new HyperlinkListener() {
			
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType()==EventType.ACTIVATED) {
					URL url = e.getURL();
					if(url!=null) {
						try {
							Desktop.getDesktop().browse(new URI(url.toString()));
						} catch(Exception ex) {
							JExceptionDialog.showError(ex);
						}
					}
				}
			}
		});

		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		rootPane.registerKeyboardAction(new Action_Close(), stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);


		news.clear();
        
        if(config.wikiNewsFeed!=null) {
			new SwingWorker<Object, Object>(){
				@Override
				protected Object doInBackground() throws Exception {
			        try {
			        	if(wikiPage!=null) {
					        WikiNewsFeed feed = new WikiNewsFeed(wikiPage);	  
					        for(com.actelion.research.util.WikiNewsFeed.News news: feed.getNews()) {
					        	addNews(news.getDate(), news.getTitle(), news.getLink(), news.getContent());
					        }
			        	}
			        } catch (Throwable e) {
			        	addNews(new Date(), "Error", null, wikiPage + " not found"); 
						e.printStackTrace();
					}				
					return null;
				}
				@Override
				protected void done() {
					refreshText();
				}
			}.execute();
        }
		lastVersionLabel.setText("<html><body>" + "<div style='color:#8888FF;font-size:8px'><i>"+config.signature+"</i></div>" + "</body></html>");

		if(!about) {
			addWindowFocusListener(new WindowFocusListener() {			
				@Override
				public void windowLostFocus(WindowEvent e) {
					dispose();
				}			
				@Override
				public void windowGainedFocus(WindowEvent e) {}
			});
		}

		setUndecorated(true);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setContentPane(contentPane);
        pack();
		setLocationRelativeTo(null);
        
	}
	
	/**
	 * Show the dialog until it loses its focus, no need to dispose or close
	 * @param config
	 */
	public static void show(final SplashConfig config) {
		if(SwingUtilities.isEventDispatchThread()) {
			SplashScreen2 splash = new SplashScreen2(config);
			splash.setVisible(true);			
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						SplashScreen2 splash = new SplashScreen2(config);
						splash.setVisible(true);
					}
				});			
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void showAbout(final SplashConfig config) {
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {
				SplashScreen2 splash = new SplashScreen2(config);
				splash.setAbout(true);
				splash.setVisible(true);
			}
		});
	}	
	private static String getHexColor(Color color) {
		String r = Integer.toHexString(color.getRed());
		String g = Integer.toHexString(color.getGreen());
		String b = Integer.toHexString(color.getBlue());
		if(r.length()<2) r = "0" + r;
		if(g.length()<2) g = "0" + b;
		if(b.length()<2) b = "0" + b;
		return ("#" + r + g + b).toUpperCase();
	}
	private static String getComplimentaryColor(Color color) {
		String r = Integer.toHexString(255-color.getRed());
		String g = Integer.toHexString(255-color.getGreen());
		String b = Integer.toHexString(255-color.getBlue());
		if(r.length()<2) r = "0" + r;
		if(g.length()<2) g = "0" + b;
		if(b.length()<2) b = "0" + b;
		return ("#" + r + g + b).toUpperCase();
	}
	
	private void refreshText() {
		final StringBuilder sb = new StringBuilder();
		
		sb.append("<html><body bgcolor='" + getHexColor(getBackground()) + "' color='" + getHexColor(getForeground()) + "' style='font-family:Arial'>\n");
		for (News n : news) {
			
			sb.append("<div style='padding-left:8px; font-size:9px;color:" + getComplimentaryColor(getBackground()) + "'>");
			sb.append("<b color='" + getComplimentaryColor(getBackground()) + "' style='font-size:10px'>");
			if(n.link!=null) {
				sb.append("<a href='" + n.link + "'>");
			}
			if(n.date!=null)  {
				sb.append(df.format(n.date) + ": ");
			}
			sb.append(n.txt);
			if(n.link!=null) {
				sb.append("</a>");
			}
			sb.append("</b>");
			sb.append("<br>");
			if(n.body!=null && n.body.length()>0) {
				sb.append(n.body);				
				sb.append("<br>");
			} else {
				sb.append("<br>");				
			}
			sb.append("<br>");
			sb.append("</div>\n");
			
		}
		sb.append("</body></html>");
		
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {
				editor.setText(sb.toString());
				editor.setCaretPosition(0);
			}
		});
		
		
	}
	
	public void addNews(String ddMMyyyy_HHmm, String txt) {
		try {
			addNews(new SimpleDateFormat("dd.MM.yyyy HH:mm").parse(ddMMyyyy_HHmm), txt);
		} catch (ParseException e) {
			try {
				addNews(new SimpleDateFormat("dd.MM.yyyy").parse(ddMMyyyy_HHmm), txt);
			} catch (ParseException e2) {
				throw new IllegalArgumentException("Invalid date " + ddMMyyyy_HHmm+" (required format dd.MM.yyyy HH:mm)");
			}
		}
	}
	public void addNews(Date date, String txt) {		
		news.add(new News(date, txt));
	}
	public void addNews(Date date, String txt, String link, String body) {		
		news.add(new News(date, txt, link,  body));
	}
	
	/**
	 * @param about the about to set
	 */
	public void setAbout(boolean about) {
		this.about = about;
		closeButton.setVisible(about);
	}

	/**
	 * @return the about
	 */
	public boolean isAbout() {
		return about;
	}
	
	
	
	public void setWikiNewsFeed(final String wikiPage) {
		this.wikiPage = wikiPage;
	}
	
	private long started = System.currentTimeMillis();
	/**
	 * Close the dialog with a minimum of 5s
	 */
	public void close() {
		close(minimumTimeSeconds*1000);
	}
	public void close(final int minimumTimeSeconds) {		
		if(about) return;
		new SwingWorkerExtended() {			
			@Override
			protected void doInBackground() throws Exception {
				long sleep = started - System.currentTimeMillis() + minimumTimeSeconds*1000;

				//Wait some time to let the user read the splash screen
				if(sleep>0) {
					try {Thread.sleep(sleep);}catch (Exception e) {}
				}
			}

			@Override
			protected void done() {
				SplashScreen2.super.setVisible(false);
			}
			
		};
	}
	
	
	public void setMinimumTimeSeconds(int minimumTimeSeconds) {
		this.minimumTimeSeconds = minimumTimeSeconds;
	}
	

	/**
	 * Example of use
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
	
		SplashConfig config = new SplashConfig(new URL("http://www.vbforfree.com/images/splashScreenArticle.jpg"), "\\\\actelch02\\PGM\\ActelionResearch\\Spirit\\bin\\Spirit.exe", "J.Freyss (Ext: 926334)", "Documentation.Test.News");
		SplashScreen2.show(config);
		
	}

}