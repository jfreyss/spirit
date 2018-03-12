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

package com.actelion.research.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;




/**
 * Class used to parse a WIKI page and extract a feed of news
 *
 * Example of Wiki Formatting
 * <pre>
 * !25.2.2011: It is today
 * An important day is today
 *
 * !1.1.2011: Happy New Year
 * As the title says
 *
 * !10.10.2010: A new feature is developped
 * Now you can use Unity to superimpose molecules
 * </pre>
 * @author freyssj
 */
public class WikiNewsFeed {

	private static final String SERVER = "ares";
	private static String linkPrefix = "http://"+SERVER+":8080/portal/Wiki.jsp?page=";
	private List<News> news;

	public static class News implements Comparable<News> {
		private final Date date;
		private final String title;
		private final String content;
		private final String link;

		News(Date date, String title,  String content){
			this.date = date;
			this.content = content;

			int index1 = title.indexOf("[");
			int index2 = title.indexOf("|", index1);
			if(index2<0) index2 = index1;
			int index3 = title.indexOf("]", index2);
			int index4 = title.lastIndexOf(".", index3);

			if(index1>=0 && index2>=0) {
				this.link = linkPrefix + title.substring(index2+1, index3);
				this.title = title.substring(0, index1) + (index2>index1? title.substring(index1+1, index2): index4>index1 && index4<index3? title.substring(index4+1, index3): title.substring(index1+1, index3)) + title.substring(index3+1);
			} else {
				this.title = title;
				this.link = null;
			}
		}

		public Date getDate() {return date;}
		public String getTitle() {return title;}
		public String getContent() {return content;}
		public String getLink() {return link;}


		@Override
		public String toString() {
			SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
			return (date!=null? dateFormat.format(date)+": ": "") + title + "\n" + getContent()+"\n-------------";
		}
		@Override
		public int compareTo(News o) {
			if(date==null) return -1;
			return date.compareTo(o.date);
		}

	}

	private WikiNewsFeed() {}

	public WikiNewsFeed(String wikiPageName) throws Exception {
		this(SERVER, wikiPageName);
	}

	public WikiNewsFeed(String serverName, String wikiPageName) throws Exception {
		this();
		news = getNews(wikiPageName);
	}

	public List<News> getNews() {
		return news;
	}

	public static String getHtml(String wikiPage) {
		InputStreamReader is = null;
		try {
			URL url = new URL("http://" + SERVER + ":8080/portal/Wiki.jsp?page=" + URLEncoder.encode(wikiPage, "UTF-8") + "&view=simple");

			is = new InputStreamReader(url.openStream());

			String html = IOUtils.readerToString(is);
			int index1 = html.indexOf("<body");
			int index2 = html.indexOf("</body>");
			if(index1>=0 && index2>index1) {
				html = html.substring(html.indexOf('>', index1+1)+1, index2);
			}


			html = html.replace("\"/portal/", "http://" + SERVER + ":8080/portal/");
			html = html.replace("\"attach/", "http://" + SERVER + ":8080/portal/attach/");
			html = html.replace("\">#</a>", "\"></a>");
			html = html.replace("<p />", "<br>");
			html = html.replaceAll("<\\/?p>", "");

			is.close();
			return "<html>"+html+"</html>";
		} catch (Exception e) {
			if(is!=null) try{is.close();} catch (Exception e2) {}
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public static List<News> getNews(String pageName) throws Exception {
		return getNews(pageName, 14);
	}
	public static List<News> getNews(String pageName, int nDays) throws Exception {

		SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy");
		SimpleDateFormat dateFormat2 = new SimpleDateFormat("d/M/yyyy");
		List<News> res = new ArrayList<>();
		//Open the URL
		LineNumberReader is = null;
		try {
			URL url = new URL("http://" + SERVER + ":8080/portal/Wiki.jsp?page=" + URLEncoder.encode(pageName, "UTF-8") + "&view=raw");
			is = new LineNumberReader(new InputStreamReader(url.openStream()));
			//Parse the content
			String line;
			Date date = null;
			String title = null;
			StringBuilder sb = new StringBuilder();
			while((line = is.readLine())!=null) {
				line = line.trim();
				line = line.replace("&gt;", ">");
				line = line.replace("&lt;", "<");
				line = line.replace("&quot;", "\"");
				if(line.startsWith("!")) {
					if(date!=null || title!=null) {
						//Add the news
						if(nDays>0 && (System.currentTimeMillis()-date.getTime())/1000/3600/24<nDays) {
							res.add(new News(date, title, sb.toString().trim()));
						}
						title = null;
						sb.setLength(0);
					}
					int index = line.indexOf(":");
					if(index<0) index = line.length();
					try {
						date = dateFormat.parse(line.substring(1, index).trim());
						title = index+2>=line.length()?"": line.substring(index+1).trim();
					} catch (Exception e) {
						try {
							date = dateFormat2.parse(line.substring(1, index).trim());
							title = index+2>=line.length()?"": line.substring(index+1).trim();
						} catch (Exception e2) {
							e2.printStackTrace();
							date = null;
							title = line.substring(1);
						}
					}
				} else {
					//We have a content
					sb.append(line);
				}
			}
			if(date!=null || title!=null) {
				if(nDays>0 && (System.currentTimeMillis()-date.getTime())/1000/3600/24<nDays) {
					res.add(new News(date, title, sb.toString().trim()));
				}

			}
		} catch (Exception e) {
			System.err.println("Problems reading: "+pageName);
			throw e;
		} finally {
			if(is!=null) is.close();
		}

		return res;
	}

	public static void main(String[] args) throws IOException {
		URL url = new URL("http://freyssj:8080/portal/Wiki.jsp?page=Documentation&view=simple");
		//		new URL("http://ares:8080/portal/Wiki.jsp?page=Documentation&view=simple")
		InputStreamReader is = new InputStreamReader(url.openStream());
		String html = IOUtils.readerToString(is);

		System.out.println(html);
	}

}
