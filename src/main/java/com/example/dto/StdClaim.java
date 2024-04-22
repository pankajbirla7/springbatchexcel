package com.example.dto;

import java.util.Date;

public class StdClaim {

	int id;
	Date dateEntered;
	int fileid;
	Date dateToSfs;

	public int getId() {
		return id;
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

	public int getFileid() {
		return fileid;
	}

	public void setFileid(int fileid) {
		this.fileid = fileid;
	}

	public Date getDateToSfs() {
		return dateToSfs;
	}

	public void setDateToSfs(Date dateToSfs) {
		this.dateToSfs = dateToSfs;
	}

}
