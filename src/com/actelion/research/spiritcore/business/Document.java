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

package com.actelion.research.spiritcore.business;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.actelion.research.util.IOUtils;

/**
 * Document represents either:
 * - one single document (Datatype == File)
 * - a zip containing several documents (Datatype == Files && DocumentType == Zip)
 *
 * @author Joel Freyss
 */
@Entity
@Table(name="document")
@Audited
@SequenceGenerator(name="document_sequence", sequenceName="document_sequence", allocationSize=1)
public class Document {

	public enum DocumentType {
		CONSENT_FORM,
		DESIGN,
		PRESENTATION,
		OTHER,
		ZIP
	}


	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="document_sequence")
	private int id;

	private String fileName;

	@Column(length=20)
	@Enumerated(EnumType.STRING)
	private DocumentType type;

	@ManyToOne(fetch=FetchType.LAZY, optional=false, cascade=CascadeType.ALL)
	@JoinColumn(name="document_bytes_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.AUDITED)
	private DocumentBytes bytes = new DocumentBytes();

	private String creUser;

	@Temporal(TemporalType.TIMESTAMP)
	private Date creDate = new Date();

	public Document() {
	}

	public Document(DocumentType type) {
		this.type = type;
	}

	public Document(String title, byte[] bytes) {
		setFileName(title);
		setBytes(bytes);
	}

	public Document(File file) throws IOException {
		setFileName(file.getName());
		setBytes(IOUtils.getBytes(file));
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public byte[] getBytes() {
		return bytes.getBytes();
	}

	public void setBytes(byte[] bytes) {
		this.bytes.setBytes(bytes);
	}

	public String getCreUser() {
		return creUser;
	}

	public void setCreUser(String creUser) {
		this.creUser = creUser;
	}

	public Date getCreDate() {
		return creDate;
	}

	public void setCreDate(Date creDate) {
		this.creDate = creDate;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;
		if(!(obj instanceof Document)) return false;
		if(getId()>0 && ((Document)obj).getId()>0) {
			return getId()==((Document)obj).getId();
		} else {
			return getType() == ((Document)obj).getType() && getFileName().equals(((Document)obj).getFileName());
		}
	}

	@Override
	public String toString() {
		return fileName;
	}

	/**
	 * @return the type
	 */
	public DocumentType getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(DocumentType type) {
		this.type = type;
	}

	public static Map<DocumentType, List<Document>> mapDocumentTypes(Collection<Document> documents) {
		Map<DocumentType, List<Document>> res = new HashMap<>();
		for (Document d : documents) {
			List<Document> docs = res.get(d.getType());
			if(docs==null) {
				res.put(d.getType(), docs = new ArrayList<>());
			}
			docs.add(d);
		}
		return res;
	}

	public static Map<String, Document> mapFilenames(Collection<Document> documents) {
		Map<String, Document> res = new HashMap<>();
		for (Document d : documents) {
			res.put(d.getFileName(), d);
		}
		return res;
	}

	/**
	 * Used to add 1 file if the document represents multiple files
	 * @param f
	 * @throws Exception
	 */
	public void addZipEntry(File f) throws Exception {
		addZipEntry(new Document(f));
	}

	public void addZipEntry(Document doc) throws Exception {
		if(getType()!=DocumentType.ZIP) {
			//Convert the existing document to a zip containing this doc
			//This case may happen if the datatype has been changed to ZIP
			Document toAdd = new Document(getFileName(), getBytes());
			setType(DocumentType.ZIP);
			setFileName(null);
			setBytes(null);
			addZipEntry(toAdd);
			addZipEntry(doc);
			return;
		}

		try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			int n = 0;
			ZipOutputStream os = new ZipOutputStream(baos);
			//Add existing entries
			if(getBytes()!=null && getBytes().length>0) {
				try(ZipInputStream is = new ZipInputStream(new ByteArrayInputStream(getBytes()))) {
					ZipEntry entry;
					while((entry = is.getNextEntry())!=null) {
						os.putNextEntry(entry);
						IOUtils.redirect(is, os);
						os.closeEntry();
						is.closeEntry();
						n++;
					}
				} catch(Exception e2) {
					e2.printStackTrace();
				}
			}
			//Add new entry
			os.putNextEntry(new ZipEntry(doc.getFileName()));
			IOUtils.redirect(doc.getBytes(), os);
			os.closeEntry();

			//Close Zip stream
			os.close();
			n++;
			this.fileName = n + "_docs.zip";
			this.setBytes(baos.toByteArray());
		}
	}

	/**
	 * Used to remove 1 file if the document represents multiple files
	 * @param f
	 * @throws Exception
	 */
	public void removeZipEntry(int index) throws Exception {
		if(getType()!=DocumentType.ZIP) throw new Exception("Not a ZIP");
		//Recreate zip
		int n = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try(ZipOutputStream os = new ZipOutputStream(baos)) {
			//Add existing entries
			try(ZipInputStream is = new ZipInputStream(new ByteArrayInputStream(getBytes()))) {
				ZipEntry entry;
				while((entry = is.getNextEntry())!=null) {
					if(n++!=index) {
						os.putNextEntry(entry);
						IOUtils.redirect(is, os);
						os.closeEntry();
					}
					is.closeEntry();
				}
			}

			fileName = (n-1) + "_docs.zip";
			os.close();
			setBytes(baos.toByteArray());
		}
	}

	/**
	 * Used to remove 1 file if the document represents multiple files
	 * @param f
	 * @throws Exception
	 */
	public Document getZipEntry(int index) throws Exception {
		if(getType()!=DocumentType.ZIP) {
			//Not a ZIP entry, returns the document itself
			//This can happen, if a single document is converted to a multiple
			if(index>0) return null;
			return this;
		}
		int n = 0;
		try(ZipInputStream is = new ZipInputStream(new ByteArrayInputStream(getBytes()))) {
			ZipEntry entry;
			while((entry = is.getNextEntry())!=null) {
				if(n++==index) {
					return new Document(entry.getName(), IOUtils.getBytes(is));
				}
			}
		}
		return null;
	}



}

