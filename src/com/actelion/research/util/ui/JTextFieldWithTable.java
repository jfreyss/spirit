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

package com.actelion.research.util.ui;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 *
 * A textfield that provides suggestion in a table (works like jcombobox)
 *
 * @author freyssj
 * @since  29.08.2012
 *
 */
public abstract class JTextFieldWithTable extends JCustomTextField {

	private String[] headers;

	/**
	 *
	 */
	public JTextFieldWithTable(String[] headers) {
		super(CustomFieldType.ALPHANUMERIC, 14);
		if(headers==null || headers.length==0) throw new IllegalArgumentException("You must give the table headers");
		this.headers = headers;
		initTextFieldWithTable();
	}

	private void initTextFieldWithTable() {
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				showPopup();
			}
		});
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				showPopup();
			}
		});

		setEditable(true);
	}

	public int getValueColumn() {return 0;}
	public abstract List<String[]> getSuggestions(String txt);

	private JDialog lastDialog = null;
	public void showPopup() {
		if(!isShowing()) return;
		String txt = getText();
		if(txt.length()<=0) return;

		Component top = JTextFieldWithTable.this.getTopLevelAncestor();
		final JDialog f = (top instanceof JDialog)? new JDialog( (JDialog) top, false):  new JDialog( (JFrame) top, false);
		final Point p = JTextFieldWithTable.this.getLocationOnScreen();


		int selRow = -1;
		final List<String[]> items = getSuggestions(getText());

		final String[][] data = new String[items.size()][headers.length];
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				data[i][j] = j<items.get(i).length? items.get(i)[j]: "";
			}
			if(selRow<0 && txt.equals(data[i][getValueColumn()])) selRow = i;
		}
		DefaultTableModel model = new DefaultTableModel(data, headers) {
			@Override
			public boolean isCellEditable(int row, int column) {return false;}
		};
		final JTable table = new JTable(model);
		if(selRow>=0) table.getSelectionModel().addSelectionInterval(selRow, selRow);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setColumnSelectionAllowed(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setFont(FastFont.SMALL);
		final JScrollPane panel = new JScrollPane(table);

		//Resize columns to fit all data
		FontMetrics fm = getFontMetrics(table.getFont());
		int totalWidth = 0;
		for(int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
			int maxWidth = 30;
			for (int rowIndex = 0; rowIndex < data.length && rowIndex<10; rowIndex++) {
				Object value = table.getModel().getValueAt(rowIndex, columnIndex);
				if(value==null) continue;
				maxWidth = Math.min(450, Math.max(maxWidth, fm.stringWidth(value.toString())+4));
			}
			totalWidth+=maxWidth+3;
			table.getColumnModel().getColumn(columnIndex).setPreferredWidth(maxWidth);
		}
		table.setFocusable(false);



		//Set renderer
		TableCellRenderer renderer = new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if(value==null) {

				} else {
					String match = JTextFieldWithTable.this.getText();
					String text = getText();
					text = highlightMatch(text, match);
					if(column==getValueColumn()) text = "<b>" + text + "</b>";
					setText("<html>" + text + "</html>");
				}
				return this;
			}
		};
		table.setDefaultRenderer(Object.class, renderer);


		//Table Listeners
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int r = table.getSelectedRow();
				if(r<0) return;
				Object o = data[r][getValueColumn()];
				if(o!=null && o.toString().length()>0) {
					setText(o.toString());
				}
			}
		});
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount()>=2) f.dispose();
			}
		});

		f.addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
			}
			@Override
			public void windowLostFocus(WindowEvent e) {
				f.dispose();
			}
		});
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				f.dispose();
			}
		});

		panel.setOpaque(false);
		f.setFocusableWindowState(false);
		f.setFocusable(false);
		f.setUndecorated(true);
		f.setContentPane(panel);
		f.setAlwaysOnTop(true);
		f.setSize(Math.min(700, totalWidth+8), Math.min(500, table.getPreferredSize().height+40));

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				int x = p.x;
				int y = p.y+getBounds().height;
				if(y+f.getHeight()>Toolkit.getDefaultToolkit().getScreenSize().height) {
					x = p.x+getBounds().width;
					y = Toolkit.getDefaultToolkit().getScreenSize().height - f.getHeight();
				}
				f.setLocation(x, y);
				if(lastDialog!=null) {lastDialog.dispose();}
				lastDialog=f;
				f.setVisible(true);
				JTextFieldWithTable.this.requestFocusInWindow();

			}
		});

	}

	private static String highlightMatch(String text, String matchString) {
		//		text = text.replaceAll("(?i)"+match, "<span style='background:#FFFFCC;color:black'>" + match + "</span>");
		for(String match: matchString.split("\\s+")) {
			StringBuilder sb = new StringBuilder();
			boolean inBrace = false;
			int i = 0;
			while(i < text.length()) {
				if(text.charAt(i)=='<') {
					inBrace = true;
				} else if(text.charAt(i)=='>') {
					inBrace = false;
				}
				if(!inBrace && i<=text.length()-match.length() && text.substring(i, i+match.length()).equalsIgnoreCase(match)) {
					sb.append("<span style='background:#FFFFAA;color:black'>" + text.substring(i, i+match.length()) + "</span>");
					i+=match.length();
				} else {
					sb.append(text.charAt(i++));
				}
			}
			text = sb.toString();
		}
		return text;
	}

	private String extractKey(String v) {
		if(v.indexOf(" - ")>0) v = v.substring(0, v.indexOf(" - "));
		return v.trim();
	}
	public String[] getCheckedItems() {
		String sel = getText();
		String[] res = sel.split(";");
		for (int i = 0; i < res.length; i++) {
			res[i] = res[i].trim();
		}
		Arrays.sort(res);
		return res;
	}
	public boolean isChecked(String v) {
		v = extractKey(v);
		for(String s: getCheckedItems()) {
			if(s.equals(v)) return true;
		}
		return false;
	}

}