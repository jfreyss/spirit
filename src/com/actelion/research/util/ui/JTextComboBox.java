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

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Component used to work like comboboxes but made editable.
 * If the user is not allowed to enter custom text, it works like the swingX autocomplete combobox.
 * If the user is allowed to enter his own text, it works like a textfield, with a popup coming suggesting the possible input.
 *
 * @author freyssj
 *
 */
@SuppressWarnings("rawtypes")
public class JTextComboBox extends JCustomTextField {

	private TreeSet<String> choices = new TreeSet<>(new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			if(o1==null && o2==null) return 0;
			if(o1==null) return 1;
			if(o2==null) return -1;
			return o1.compareToIgnoreCase(o2);
		}
	});

	private JDialog popup;
	private final DefaultListModel<String> model = new DefaultListModel<>();
	private final JList<String> list = new JList<>(model);
	private ListCellRenderer renderer = new DefaultListCellRenderer();
	private boolean allowTyping = true;
	private boolean progressiveFiltering = true;
	private int push = 0;

	private boolean preferMultipleChoices = false; //set by the user
	private static final String DEFAULT_SEPARATORS = ",; ";
	private String separators = DEFAULT_SEPARATORS;

	public JTextComboBox() {
		this(true);

	}


	public JTextComboBox(List<String> choices) {
		this();
		setChoices(choices);
	}

	@SuppressWarnings("unchecked")
	public JTextComboBox(boolean allowTyping) {
		super(JCustomTextField.ALPHANUMERIC);
		setAllowTyping(allowTyping);


		list.setCellRenderer(getListCellRenderer());
		list.setSelectionMode(preferMultipleChoices && separators.length()>0? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION: ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				if(push>0) return;
				push++;
				try {
					setTextFromList();
					if(!ctrlDown) {
						hidePopup();
						fireTextChanged();
						fireActionPerformed();
					}
				} finally{
					push--;
				}
			}
		});

		list.setAutoscrolls(true);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(!isEnabled())return;
				showPopup();
			}
		});

		addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				selectAll();
			}
			@Override
			public void focusLost(FocusEvent e) {
				hidePopup();
				fireTextChanged();
			}
		});

		final Document doc = new MyCustomDocument() {
			private String olderSel = null;
			@Override
			public void remove(int offs, int len) throws BadLocationException {
				if(isAllowTyping() || offs==0) {
					super.remove(offs, len);
				}
				selectAndScroll();
			}


			@Override
			public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
				if(str==null) return;
				if(push>0) {
					super.insertString(offs, str, a);
				} else if (getMaxChars()<=0 || (getLength() + str.length()) <= getMaxChars()) {
					if(isAllowTyping() && getCaretPosition()<getLength() && getSelectionStart()>=getSelectionEnd()) {
						super.insertString(offs, str, a);
					} else {
						String prefix = JTextComboBox.this.getText().substring(0, offs) + str;
						selectWithPrefix(offs, prefix);
					}
				}
				selectAndScroll();
			}

			/**
			 * Update the selection
			 * @param offs
			 * @param prefix
			 * @throws BadLocationException
			 */
			private void selectWithPrefix(int offs, String prefix) throws BadLocationException {
				if(prefix.length()>0) {
					String selectedChoice = null;
					for(String s: getChoices()) {
						if(s.startsWith(prefix)) {
							selectedChoice = s;
							break;
						} else if(s.toUpperCase().startsWith(prefix.toUpperCase())) {
							selectedChoice = s;
						}
					}
					if(selectedChoice!=null) {
						//Found a match
						super.remove(0, super.getLength());
						super.insertString(0, selectedChoice, null);
						setCaretPosition(selectedChoice.length());
						moveCaretPosition(offs+1);
						olderSel = selectedChoice;
						return;
					}
				}
				if(isAllowTyping()) {
					super.remove(0, super.getLength());
					super.insertString(0, prefix, null);
					setCaretPosition(getLength());
				} else if(olderSel!=null){
					//No match, reset
					super.remove(0, super.getLength());
					super.insertString(0, olderSel, null);
					int horizVisibility = getHorizontalVisibility().getValue();
					setCaretPosition(getLength());
					moveCaretPosition(Math.min(offs, getLength()));
					getHorizontalVisibility().setValue(horizVisibility);
					showPopup(); //reset to previous value and show popup because the updated string does not match anything
				} else {
					super.insertString(0, prefix, null);
					olderSel = prefix;
					showPopup(); //show popup because the initial string does not match anything
				}

			}
		};
		setDocument(doc);

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int dir = 0;
				if(e.getKeyCode()==38) { //down key
					dir = -1;
				} else if(e.getKeyCode()==40) { //up key
					dir = 1;
				} else if(e.getKeyCode()==33) { //down key
					dir = -10;
				} else if(e.getKeyCode()==34) { //down key
					dir = 10;
				}

				if(dir!=0) {
					push++;
					try {
						if(isVisible() && (popup==null || !popup.isVisible())) {
							showPopup();
						} else {
							int index = Math.min(list.getModel().getSize()-1, Math.max(0, list.getSelectedIndex()+dir));
							list.setSelectedIndex(index);
							list.ensureIndexIsVisible(index);
						}
					} finally {
						push--;
					}

				}
			}
			@Override
			public void keyTyped(KeyEvent e) {
				if(e.getKeyChar()==10) {
					if(popup!=null && popup.isVisible()) {
						setTextFromList();
						hidePopup();
					}
					fireTextChanged();
					fireActionPerformed();
					e.consume();
				} else if(e.getKeyChar()==27) {
					if(popup!=null && popup.isVisible()) {
						hidePopup();
						e.consume();
					}
				} else if(e.getKeyChar()==8) {
					if(!isAllowTyping()) {
						//move the caret
						int caret = Math.min(getSelectionStart(), getCaretPosition())-1;
						if(caret<=0) {
							setText("");
							selectAll();
							hidePopup();
						} else {
							setText(getText().substring(0, caret));
							setCaretPosition(getText().length());
							moveCaretPosition(caret);
						}
					}
				} else if(Character.isLetterOrDigit(e.getKeyChar())){
					//OK
					showPopup();
				} else if(getText().length()==0) {
					hidePopup();
					e.consume();
				}
			}
		});

		addAncestorListener(new AncestorListener(){
			@Override
			public void ancestorAdded(AncestorEvent event){ hidePopup();}
			@Override
			public void ancestorRemoved(AncestorEvent event){ hidePopup();}
			@Override
			public void ancestorMoved(AncestorEvent event){
				hidePopup();
			}
		});

		final AWTEventListener listener = new AWTEventListener() {
			@Override
			public void eventDispatched(AWTEvent event) {
				if((event instanceof MouseEvent ) && ((MouseEvent)event).getID()==MouseEvent.MOUSE_CLICKED) {
					if(popup!=null && (event.getSource() instanceof Component) && SwingUtilities.getWindowAncestor((Component) event.getSource())!=popup &&  event.getSource()!=JTextComboBox.this) {
						hidePopup();
					}
				}
				if((event instanceof KeyEvent)) {
					//is ctrl down?
					ctrlDown = ((KeyEvent) event).isControlDown() ||  ((KeyEvent) event).isShiftDown();

					//consume enter, to avoid closing
					if(((KeyEvent) event).getKeyCode()==10 && (popup!=null && popup.isVisible())) ((KeyEvent) event).consume();
				}
			}
		};


		addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
			}
			@Override
			public void focusLost(FocusEvent e) {
				Toolkit.getDefaultToolkit().removeAWTEventListener(listener);
			}
		});

		setMargin(new Insets(0, 0, 0, 12));
	}

	public void setMultipleChoices(boolean multipleMode) {
		this.preferMultipleChoices = multipleMode;
	}

	public void testAllowMultipleMode() {
		Collection<String> choices = getChoices();
		if(preferMultipleChoices && choices!=null && choices.size()>0) {
			separators = DEFAULT_SEPARATORS;
			for (String string : choices) {
				for (int i = 0; i < separators.length(); i++) {
					if(string.indexOf(separators.charAt(i))>=0) {
						separators = separators.substring(0, i) + separators.substring(i+1);
						i--;
						continue;
					}
				}
			}
		}
	}

	@Override
	public void setText(String t) {
		push++;
		try {
			if(t==null) t = "";
			if(getText().equals(t)) return;
			super.setText(t);
			fireTextChanged();
		} finally{
			push--;
		}
	}

	/**
	 * Is the user allow to enter a text, which is not in the list of choices
	 * @param allowTyping
	 */
	public void setAllowTyping(boolean allowTyping) {
		this.allowTyping = allowTyping;
		setBackground(allowTyping?Color.WHITE: UIUtils.WHITESMOKE);
	}

	public boolean isAllowTyping() {
		return allowTyping;
	}


	/**
	 * Draw with a Nimbus style
	 * @param graphics
	 */
	@Override
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		UIUtils.applyDesktopProperties(g);
		super.paint(g);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Color fg = Color.BLACK;
		if(UIManager.getLookAndFeel().getName().contains("!!Nimbus")) {
			Color bg0 = UIUtils.getColor(246, 248, 250);
			Color bg1 = UIUtils.getColor(170, 190, 207);
			Color bg2 = UIUtils.getColor(187, 208, 227);
			if(!isEnabled()) {
				bg0 = bg0.darker();
				bg1 = bg1.darker();
				bg2 = bg2.darker();
			} else if(popup!=null && popup.isShowing()) {
				bg0 = UIUtils.getColor(143, 169, 192);
				bg1 = UIUtils.getColor(63, 108, 147);
				bg2 = UIUtils.getColor(90, 139, 182);
				fg = Color.WHITE;
			}
			Insets insets = getInsets();
			g.setPaint(new GradientPaint(0, 0, bg0, 0, getHeight()/2, bg1));
			g.fillRect(getWidth()-16, insets.top-3,  13, getHeight()/2-(insets.top-3));
			g.setPaint(new GradientPaint(0, getHeight()/2, bg1, 0, getHeight()-2, bg2));
			g.fillRect(getWidth()-16, getHeight()/2, 13, getHeight()/2-(insets.bottom-4));
			g.setColor(UIUtils.getColor(139,160,179));
			g.drawLine(getWidth()-16, insets.top-3, getWidth()-16, getHeight()-(insets.bottom-3)-1);
		}
		g.setColor(fg);
		g.fillPolygon(new int[] {getWidth()-12, getWidth()-6, getWidth()-9}, new int[] {getHeight()/2-3, getHeight()/2-3, getHeight()/2+3}, 3 );

	}

	/**
	 * Can be overriden by classes to get choices on popup opening
	 * @return
	 */
	public Collection<String> getChoices() {
		return choices;
	}

	/**
	 * Set the choices.
	 * Disable multiple choices if some have spaces
	 * @param choices
	 */
	public void setChoices(Collection<String> choices) {
		if(choices!=this.choices) {
			this.choices.clear();
			if(choices!=null) this.choices.addAll(choices);
		}

		//Check if we can allow multiplechoices
		testAllowMultipleMode();
	}

	public ListCellRenderer getListCellRenderer() {
		return renderer;
	}

	public void setListCellRenderer(ListCellRenderer renderer) {
		this.renderer = renderer;
	}

	public void hidePopup() {
		if(popup!=null) {
			popup.dispose();
			repaint();
		}
	}

	/**
	 * Scroll to the current selected token
	 */
	private void selectAndScroll() {

		if(progressiveFiltering) {
			String[] typed = splitText(getText().substring(0, getCaretPosition()));
			String last = typed.length>0? typed[typed.length-1]: null;
			Collection<String> choices = getChoices();
			model.clear();
			model.addElement(" ");
			for (String s : choices) {
				if(getCaretPosition()==getText().length() || last==null || s.startsWith(last)) {
					model.addElement(s);
				}
			}
		} else {
			model.clear();
			model.addElement(" ");
			for (String s : choices) {
				model.addElement(s);
			}
		}

		if(popup==null || !popup.isVisible()) return;

		try {
			push++;

			String text = getText();
			TreeSet<String> tokens = new TreeSet<>(Arrays.asList(splitText(text)));
			list.clearSelection();


			int firstIndex = -1;
			int lastIndex = -1;
			for (int i = 0; i < list.getModel().getSize(); i++) {
				String s = list.getModel().getElementAt(i);
				if(s!=null && tokens.contains(s)) {
					list.getSelectionModel().addSelectionInterval(i, i);
					if(firstIndex<0) {
						firstIndex = i; lastIndex = i;
					} else {
						firstIndex = Math.min(i, firstIndex); lastIndex = Math.max(i, lastIndex);
					}
				}
			}

			if(firstIndex>=0) {
				int v1 = list.getFirstVisibleIndex();
				int v2 = list.getLastVisibleIndex();
				if(v2<firstIndex || v1>lastIndex) {
					list.ensureIndexIsVisible(firstIndex);
					list.ensureIndexIsVisible(Math.max(0, firstIndex-5));
					list.ensureIndexIsVisible(Math.min(list.getModel().getSize()-1, firstIndex+5));
				}
			}
		} finally {
			push--;
		}

	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}


	private boolean ctrlDown = false;
	public void showPopup() {
		final Point p = getLocationOnScreen();
		testAllowMultipleMode();
		selectAndScroll();

		if(!isVisible() || (popup!=null && popup.isVisible())) {
			return;
		}


		final JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(Math.max(100, Math.max(list.getPreferredSize().width+30, getWidth())), Math.min(list.getPreferredSize().height+15, 330) ));
		scrollPane.setBorder(null);

		JCustomLabel infoPanel = new JCustomLabel("Use Ctrl for multiple-selection", FastFont.SMALL);
		infoPanel.setVisible(list.getSelectionMode()==ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);


		final JPanel contentPane = UIUtils.createBox(scrollPane, null, infoPanel, null, null);
		contentPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				final WindowFocusListener wl = new WindowFocusListener() {
					@Override public void windowLostFocus(WindowEvent e) {hidePopup();}
					@Override public void windowGainedFocus(WindowEvent e) {}
				};

				if(popup!=null) popup.dispose();
				if(getTopLevelAncestor() instanceof JDialog) {
					if(popup==null) ((JDialog)getTopLevelAncestor()).addWindowFocusListener(wl);
					popup = new JDialog((JDialog)getTopLevelAncestor(), false);
				} else if(getTopLevelAncestor() instanceof JFrame) {
					if(popup==null) ((JFrame)getTopLevelAncestor()).addWindowFocusListener(wl);
					popup = new JDialog((JFrame)getTopLevelAncestor(), false);
				} else {
					return;
				}


				popup.setUndecorated(true);
				popup.setContentPane(contentPane);
				popup.setAlwaysOnTop(true);
				popup.pack();




				int x = p.x;
				int y = p.y+getBounds().height;
				if(y+popup.getHeight()>Toolkit.getDefaultToolkit().getScreenSize().height) {
					x = p.x+getBounds().width;
					y = Toolkit.getDefaultToolkit().getScreenSize().height - popup.getHeight();
				}
				if(x+popup.getWidth()>Toolkit.getDefaultToolkit().getScreenSize().width) {
					x = Toolkit.getDefaultToolkit().getScreenSize().width - popup.getWidth();
				}
				popup.setFocusableWindowState(false);
				popup.setLocation(x, y);
				popup.setVisible(true);

				selectAndScroll();
			}
		});
		repaint();
	}



	private void setTextFromList() {
		String text = getText();
		if(list==null || popup==null || !popup.isVisible()) return;
		if(preferMultipleChoices && separators.length()>0) {

			//Find current entered tokens
			String[] enteredText = splitText(text);
			Set<String> tokens = new TreeSet<>();
			if(ctrlDown || list.getSelectedIndices().length==0) {
				for(String t: enteredText) tokens.add(t);
			}


			//If an existing token is no more selected -> remove it
			//If a non existing token is selected -> add it
			int[] indices = list.getSelectedIndices();
			for (int i = 0; i < list.getModel().getSize(); i++) {
				String s = list.getModel().getElementAt(i).trim();
				if(s.length()==0) continue;

				boolean selected = Arrays.binarySearch(indices, i)>=0;
				if(!selected && tokens.contains(s)) {
					tokens.remove(s);
				} else if(selected && !tokens.contains(s)) {
					tokens.add(s);
				}
			}

			StringBuilder sb = new StringBuilder();
			for (String s : tokens) {
				sb.append((sb.length()>0? separators.charAt(0)+" ": "")+s);
			}
			setText(sb.toString());
		} else {
			if(list.getSelectedValue()!=null) {
				setText(list.getSelectedValue());
			}
		}

	}

	public static void main(String[] args) {

		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			UIManager.put("nimbusSelectionBackground", new Color(173,207,231));
		} catch (Exception e) {
			e.printStackTrace();
		}


		List<String> choices = new ArrayList<String>();
		choices.add("0.OK");
		choices.add("1.NOK");
		choices.add("2a.maybe");
		choices.add("2b.canbe");
		choices.add("3.unknown");
		choices.add("Sol1");
		choices.add("Sol2");

		List<String> choices2 = new ArrayList<String>();
		choices2.add("IL-1");
		choices2.add("IL-1 srf");
		choices2.add("IL-11");
		choices2.add("Igh 1");
		choices2.add("Igh2");
		choices2.add("Srf");

		List<String> choices3 = new ArrayList<String>();
		for (int i = 0; i < 200; i++) {
			String s = "";
			for (int j = 0; j < 10; j++) {
				s  += (char) ('A' + (int) (Math.random()*26));
				if(Math.random()<.1) break;
			}
			choices3.add(s);
		}

		JTextComboBox cb1 = new JTextComboBox(false);
		JTextComboBox cb2 = new JTextComboBox(false);
		JTextComboBox cb3 = new JTextComboBox(false);
		JTextComboBox cb4 = new JTextComboBox(true);
		JTextComboBox cb5 = new JTextComboBox(true);
		JTextComboBox cb6 = new JTextComboBox(true);
		cb1.setChoices(choices);
		cb2.setChoices(choices2);
		cb3.setChoices(choices3);

		cb4.setChoices(choices);
		cb5.setChoices(choices2);
		cb6.setChoices(choices3);

		JFrame testFrame = new JFrame();
		JPanel contentPane = new JPanel(new GridLayout(2, 3, 5, 5));
		contentPane.add(cb1);
		contentPane.add(cb2);
		contentPane.add(cb3);
		contentPane.add(cb4);
		contentPane.add(cb5);
		contentPane.add(cb6);

		if(true) {
			cb1.setMultipleChoices(true);
			cb2.setMultipleChoices(true);
			cb3.setMultipleChoices(true);
			cb4.setMultipleChoices(true);
			cb5.setMultipleChoices(true);
			cb6.setMultipleChoices(true);
		}

		testFrame.setContentPane(UIUtils.createBox(contentPane, UIUtils.createHorizontalTitlePanel("Test of Comboboxes")));
		testFrame.pack();
		testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		testFrame.setVisible(true);

	}


	/**
	 * Split the text for multiplechoices, based on the used separator
	 * Otherwise return the text
	 * @param text
	 * @return
	 */
	public String[] splitText(String text) {
		if(preferMultipleChoices && separators.length()>0) {
			String[] res = split(text, separators);
			for (int i = 0; i < res.length; i++) {
				res[i] = res[i].trim();
			}
			return res;
		} else {
			return text.length()>0? new String[] {text}: new String[0];
		}
	}


	/**
	 * To be used when escapeSpaceWithQuotes=true
	 */
	public String flattenTokens(String[] tokens) {
		if(preferMultipleChoices && separators.length()>0) {
			StringBuilder sb = new StringBuilder();
			for(String tok: tokens) {
				tok = tok.trim();
				if(sb.length()>0) sb.append(separators.charAt(0));
				else sb.append(tok);
			}
			return sb.toString();
		} else {
			return tokens.length>0? tokens[0]: "";
		}
	}

	public String[] getSelectionArray() {
		return splitText(getText());
	}


	public void setSelectionArray(String[] tokens) {
		setText(flattenTokens(tokens));
	}




	///Util function copied from dd_spirit
	private static String[] split(String s, String separators) {
		if(s==null) return new String[0];
		StringTokenizer st = new StringTokenizer(s, "\"" + separators, true);
		List<String> res = new ArrayList<String>();
		boolean inQuote = false;
		StringBuilder sb = new StringBuilder();
		while(st.hasMoreTokens()) {
			String token = st.nextToken();
			if(token.equals("\"")) {
				if(inQuote) {
					if(sb.toString().trim().length()>0) res.add(sb.toString().trim());
					sb.setLength(0);
					inQuote = false;
				} else {
					inQuote = true;
				}
			} else if(!inQuote && separators.indexOf(token)>=0) {
				if(sb.toString().trim().length()>0) res.add(sb.toString().trim());
				sb.setLength(0);
			} else {
				sb.append(token);
			}
		}
		if(sb.toString().trim().length()>0) res.add(sb.toString().trim());
		return res.toArray(new String[res.size()]);
	}
}
