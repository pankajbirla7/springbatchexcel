package com.example.demo;

import java.util.Date;

public class FileDetails {

	private Long id;

	private String agencyFin;
	
	private String fileName;
	
	private String submittedByEmail;
	
	private Date submitDate;
	
	private Date rawLoadDate;
	
	private Date sfsSentDate;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAgencyFin() {
		return agencyFin;
	}

	public void setAgencyFin(String agencyFin) {
		this.agencyFin = agencyFin;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getSubmittedByEmail() {
		return submittedByEmail;
	}

	public void setSubmittedByEmail(String submittedByEmail) {
		this.submittedByEmail = submittedByEmail;
	}

	public Date getSubmitDate() {
		return submitDate;
	}

	public void setSubmitDate(Date submitDate) {
		this.submitDate = submitDate;
	}

	public Date getRawLoadDate() {
		return rawLoadDate;
	}

	public void setRawLoadDate(Date rawLoadDate) {
		this.rawLoadDate = rawLoadDate;
	}

	public Date getSfsSentDate() {
		return sfsSentDate;
	}

	public void setSfsSentDate(Date sfsSentDate) {
		this.sfsSentDate = sfsSentDate;
	}
}
