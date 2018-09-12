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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * <p>Title: Client HTTP Request class</p>
 * <p>Description: this class helps to send POST HTTP requests with various form data,
 * including files. Cookies can be added to be included in the request.</p>
 *
 * @author Vlad Patryshev
 * @version 1.0
 */
public class JSonHttpRequest {
	URLConnection connection;
	OutputStream os = null;

	protected void connect() throws IOException {
		if (os == null) os = connection.getOutputStream();
	}
	/**
	 * Creates a new multipart POST HTTP request on a freshly opened URLConnection
	 *
	 * @param connection an already open URL connection
	 * @throws IOException
	 */
	public JSonHttpRequest(URLConnection connection) throws IOException {
		this.connection = connection;
		((HttpURLConnection)connection).setUseCaches(false);
		((HttpURLConnection)connection).setRequestMethod("POST");
		((HttpURLConnection)connection).setDoInput(true);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/json");
	}

	/**
	 * Creates a new multipart POST HTTP request for a specified URL
	 *
	 * @param url the URL to send request to
	 * @throws IOException
	 */
	public JSonHttpRequest(URL url) throws IOException {
		this(url.openConnection());
	}

	/**
	 * Creates a new multipart POST HTTP request for a specified URL string
	 *
	 * @param urlString the string representation of the URL to send request to
	 * @throws IOException
	 */
	public JSonHttpRequest(String urlString) throws IOException {
		this(new URL(urlString));
	}


	/**
	 * posts the requests to the server, with all the cookies and parameters that were added
	 * @return input stream with the server response
	 * @throws IOException
	 */
	public InputStream post(String text) throws IOException {
		connect();
		os.write(text.getBytes());
		os.flush();
		os.close();

		InputStream c = connection.getInputStream();
		return c;
	}

}
