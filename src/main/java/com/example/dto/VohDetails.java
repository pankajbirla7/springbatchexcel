package com.example.dto;

import java.math.BigDecimal;

public class VohDetails {
	String acctDate;
	String invoiceNo;
	BigDecimal claimTotal;
	String NDC;
	String SFSVendorId;
	String dateReceived;
	String dfilled;
	String interestEligible;
	String paymentDate;

	public String getAcctDate() {
		return acctDate;
	}

	public void setAcctDate(String acctDate) {
		this.acctDate = acctDate;
	}

	public String getInvoiceNo() {
		return invoiceNo;
	}

	public void setInvoiceNo(String invoiceNo) {
		this.invoiceNo = invoiceNo;
	}

	public BigDecimal getClaimTotal() {
		return claimTotal;
	}

	public void setClaimTotal(BigDecimal claimTotal) {
		this.claimTotal = claimTotal;
	}

	public String getNDC() {
		return NDC;
	}

	public void setNDC(String nDC) {
		NDC = nDC;
	}

	public String getSFSVendorId() {
		return SFSVendorId;
	}

	public void setSFSVendorId(String sFSVendorId) {
		SFSVendorId = sFSVendorId;
	}

	

	public String getDateReceived() {
		return dateReceived;
	}

	public void setDateReceived(String dateReceived) {
		this.dateReceived = dateReceived;
	}

	public String getDfilled() {
		return dfilled;
	}

	public void setDfilled(String dfilled) {
		this.dfilled = dfilled;
	}

	public String getInterestEligible() {
		return interestEligible;
	}

	public void setInterestEligible(String interestEligible) {
		this.interestEligible = interestEligible;
	}

	public String getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(String paymentDate) {
		this.paymentDate = paymentDate;
	}

}
