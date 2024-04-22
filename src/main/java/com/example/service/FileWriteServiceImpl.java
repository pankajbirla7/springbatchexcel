package com.example.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.Constants;
import com.example.demo.FileDetails;
import com.example.dto.SafeNetClaim;
import com.example.dto.StdClaim;
import com.example.repository.SafeNetClaimDao;
import com.example.repository.StdClaimDao;

@Service
public class FileWriteServiceImpl implements FileWriteService {

	@Autowired
	public JdbcTemplate jdbcTemplate;

	@Autowired
	StdClaimDao stdClaimDao;

	@Autowired
	SafeNetClaimDao safeNetClaimDao;
	
	@Autowired
	FileDao fileDao;

	@Value("${stg.file.path}")
	private String stgFileDirectory;

	@Override
	public void generateFile() {

		List<StdClaim> stdClaims = stdClaimDao.getStdClaimDetails(Constants.NEW);
		System.out.println("total stdClaimList " + stdClaims.size());

		for (StdClaim stdClaim : stdClaims) {
			FileDetails fileDetails = fileDao.getFileDetailsByFileID(stdClaim.getFileid());
			int claimCount = stdClaimDao.getClaimCountByDateEntered(fileDetails.getSubmitDate(), Constants.NEW);
			System.out.println("Total Cliam count for stadClaim date value greater than : "
					+ " for file Id : " + stdClaim.getFileid());

			List<StdClaim> stdClaims2 = stdClaimDao.getClaimIds(fileDetails.getSubmitDate(), Constants.NEW);
			writeFirstClaimCountRowInFile(claimCount, stdClaim, stdClaims2);

		}
	}

	private void writeFirstClaimCountRowInFile(int claimCount, StdClaim stdClaim1, List<StdClaim> stdClaims2) {
		String ssid = "12969";
		String ctlNum = "SFNET";
		
		String firstRow = String.format("%5s", ssid) +
			    "C" +
			    String.format("%-9s", ctlNum) +
			    String.format("%05d", claimCount) +
			    String.format("%-10s", " ") +
			    String.format("%3s", " ") +
			    String.format("%05d", 0) +
			    String.format("%010d", 0) +
			    String.format("%05d", 0) +
			    String.format("%18s", " ") +
			    "CTL" +
			    String.format("%2s", " ") +
			    String.format("%1$-924s", " ");
		
		String filePath = stgFileDirectory+"\\"+"output.txt"; // Change this to your desired file path
        
        // Write data to the file
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writer.write(firstRow);
            System.out.println("FIrst Row Data written to file successfully.");
        
			int dohTransCtrlNo = 0;
			for (StdClaim stdClaim : stdClaims2) {
	
					int currClaimId = stdClaim.getId();
		
					List<String> claimIds = new ArrayList<>();
		
					String claimIDStr = Integer.toString(currClaimId);
					claimIds.add(claimIDStr); // Add currClaimId to claimIds list
		
					// Pad claimIDStr with leading zeros until it has a length of 6
					while (claimIDStr.length() < 6) {
						claimIDStr = '0' + claimIDStr;
					}
		
					// each claim has 2 lines in payment file
					int docCtrlCount = 2;
					int transCtrlNo = 0;
		
					// Build the doc header DOH record.
					dohTransCtrlNo++; // Increment dohTransCtrlNo
		
					String secondRow = String.format("%5s", ssid) +
						    "H" +
						    String.format("%-9s", ctlNum) +
						    String.format("%05d", 0) + // This ensures leading zeros for the integer
						    String.format("%-10s", currClaimId) +
						    String.format("%3s", "VOU") +
						    String.format("%05d", docCtrlCount + 1) + // This ensures leading zeros for the integer
						    String.format("%010d", dohTransCtrlNo) + // This ensures leading zeros for the integer
						    String.format("%05d", 0) + // This ensures leading zeros for the integer
						    String.format("%18s", " ") +
						    "DOH" +
						    String.format("%2s", " ") +
						    String.format("%1$-924s", " ");
					
					writer.newLine();
					writer.write(secondRow);
				//	writer.newLine();
				//	writer.write("Line 3");
					
					transCtrlNo++;

					// Build the VOH record.
//					String sTextLine =
//					    String.format("%5s", ssid) +                // start transaction prefix
//					    "X" +
//					    String.format("%-9s", ctlNum) +
//					    String.format("%5d", 0) +
//					    String.format("%-10s", currClaimId) +
//					    String.format("%3s", "VOU") +
//					    String.format("%5d", 0) +
//					    String.format("%10d", transCtrlNo) +
//					    String.format("%5d", dcCount) +
//					    String.format("%18s", " ") +
//					    "VOH" +
//					    String.format("%2s", " ") +                // end transaction prefix
//					    String.format("%-8s", "2S" + claimIDStr) +
//					    "ADD" +
//					    "DOH01" +
//					    "+" +
//					    String.format("%-26.3f", claimTotal) +
//					    String.format("%-30s", invoiceNumber) +
//					    String.format("%8s", invoiceDate) +
//					    String.format("%8s", recvDate) +
//					    String.format("%10s", sfsVendorId) +
//					    String.format("%310s", " ") +
//					    String.format("%1s", intElig) +
//					    String.format("%8s", paymentDate) +
//					    String.format("%8s", obligDate) +
//					    String.format("%-254s", " ") +
//					    String.format("%143s", " ") +
//					    String.format("%8s", acctDate) +
//					    String.format("%93s", " ");

		            System.out.println("File written successfully.");
		          //  break;
				}
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
		}
	}


