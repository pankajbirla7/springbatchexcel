package com.example.dto;

import java.util.Date;

public class SafeNetClaim {

	int id;
	int claimId;
	Date dateEntered;
	Date dateToSfs;
	String statuscd;

	public int getId() {
		return id;
	}

	public int getClaimId() {
		return claimId;
	}

	public void setClaimId(int claimId) {
		this.claimId = claimId;
	}

	public String getStatuscd() {
		return statuscd;
	}

	public void setStatuscd(String statuscd) {
		this.statuscd = statuscd;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getDateEntered() {
		return dateEntered;
	}

	public void setDateEntered(Date dateEntered) {
		this.dateEntered = dateEntered;
	}

	public Date getDateToSfs() {
		return dateToSfs;
	}

	public void setDateToSfs(Date dateToSfs) {
		this.dateToSfs = dateToSfs;
	}

}
