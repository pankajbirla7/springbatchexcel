package com.example.demo;

import java.sql.Date;

public class MyDataObject {

	private Long id;

	private String fein;

	private String cin;

	private String ndc;

	private Date dateEntered;

	private String dfilled;

	private int fileId;

	private int statusCode;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFein() {
		return fein;
	}

	public void setFein(String fein) {
		this.fein = fein;
	}

	public String getCin() {
		return cin;
	}

	public void setCin(String cin) {
		this.cin = cin;
	}

	public String getNdc() {
		return ndc;
	}

	public void setNdc(String ndc) {
		this.ndc = ndc;
	}

	public Date getDateEntered() {
		return dateEntered;
	}

	public void setDateEntered(Date dateEntered) {
		this.dateEntered = dateEntered;
	}

	public String getDfilled() {
		return dfilled;
	}

	public void setDfilled(String dfilled) {
		this.dfilled = dfilled;
	}

	public int getFileId() {
		return fileId;
	}

	public void setFileId(int fileId) {
		this.fileId = fileId;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	@Override
	public String toString() {
		return "MyDataObject [id=" + id + ", fein=" + fein + ", cin=" + cin + ", ndc=" + ndc + ", dateEntered="
				+ dateEntered + ", dfilled=" + dfilled + ", fileId=" + fileId + ", statusCode=" + statusCode + "]";
	}

	
}
