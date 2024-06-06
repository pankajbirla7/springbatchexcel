package com.example.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.Constants;
import com.example.demo.FileDetails;
import com.example.dto.StdClaim;
import com.example.dto.VohDetails;
import com.example.repository.StdClaimDao;
import com.example.utility.Utility;

@Service
public class FileWriteServiceImpl implements FileWriteService {

	@Autowired
	public JdbcTemplate jdbcTemplate;

	@Autowired
	StdClaimDao stdClaimDao;

	@Autowired
	FileDao fileDao;

	@Autowired
	Utility utility;

	@Autowired
	StdClaimService stdClaimService;

	@Value("${stg.file.path}")
	private String stgFileDirectory;

	@Value("${encrypted.file.path}")
	private String encryptedFileDirectory;

	@Value("${decrypted.file.path}")
	private String decryptedFileDirectory;

	@Value("${download.sftp.file.path}")
	private String downloadSftpFilePath;

	@Value("${public_key_path}")
	private String publicKeyPath;

	@Value("${private_key_path}")
	private String privateKeyPath;

	@Value("${sftp.host}")
	private String sftpHost;

	@Value("${sftp.username}")
	private String sftpUserName;

	@Value("${sftp.password}")
	private String sftpPassword;

	@Value("${passphrase}")
	private String passphrase;

	private int port = 22;

	@Value("${sftp.remotedirectory.upload}")
	private String sftpRemoteUploadDirectory;

	@Value("${sftp.remotedirectory.download}")
	private String sftpRemoteDownloadDirectory;

	@Value("${sftp.remotedirectory.archive}")
	private String sftpRemoteArchiveDirectory;
	

	@Value("${download.sftp.processed.file.path}")
	private String downloadSftpProcessedFilePath;
	
	@Value("${decrypted.processed.file.path}")
	private String decryptedProcessedFileDirectory;

	
	@Value("${sftp.processed.remotedirectory.download}")
	private String sftpProcessedRemoteDownloadDirectory;

	@Value("${sftp.processed.remotedirectory.archive}")
	private String sftpProcessedRemoteArchiveDirectory;
	
	@Value("${archive.download.sftp.file.path}")
	private String archiveDownloadDirectory;
	
	@Value("${archive.download.sftp.processed.file.path}")
	private String archiveProcessedDownloadDirectory;

	@Override
	public void generateFile() {

		List<StdClaim> stdClaims = stdClaimDao.getStdClaimDetails(Constants.NEW);
		System.out.println("total stdClaimList " + stdClaims.size());

		for (StdClaim stdClaim : stdClaims) {
			FileDetails fileDetails = fileDao.getFileDetailsByFileID(stdClaim.getFileid());
			int claimCount = stdClaimDao.getClaimCountByDateEntered(fileDetails.getSubmitDate(), Constants.NEW);
			System.out.println("Total Cliam count for stadClaim date value greater than : " + " for file Id : "
					+ stdClaim.getFileid());

			List<StdClaim> stdClaims2 = stdClaimDao.getClaimIds(fileDetails.getSubmitDate(), Constants.NEW);
			String filePath = writeFirstClaimCountRowInFile(claimCount, stdClaim, stdClaims2, fileDetails);

			encryptFileAndUploadToSftp(filePath);
			stdClaimDao.updateStandardDetailSfsDate(stdClaims2);
			break;
		}
	}
///////////////////////////////Encrypt and Upload File/////////////////////////////////////

	private void encryptFileAndUploadToSftp(String filePath) {
		String inputFilePath = filePath;
		LocalDate today = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMddyyyy");
		String formattedDate = today.format(formatter);

		String encryptedFileName = "SN12969_" + formattedDate;
		String outputFilePath = encryptedFileDirectory + "\\" + encryptedFileName + ".gpg";

		utility.moveFileToSFTP(inputFilePath, outputFilePath, publicKeyPath, passphrase, sftpHost, port, sftpUserName,
				sftpPassword, privateKeyPath, sftpRemoteUploadDirectory);
	}

	private String writeFirstClaimCountRowInFile(int claimCount, StdClaim stdClaim1, List<StdClaim> stdClaims2,
			FileDetails fileDetails) {
		String ssid = "12969";
		String ctlNum = "SFNET";
		String pgmCode = "28638";
		String budRef = "2023-24";
		String deptCode = "3450340";

		String firstRow = String.format("%5s", ssid) + "C" + String.format("%-9s", ctlNum)
				+ String.format("%05d", claimCount) + String.format("%-10s", " ") + String.format("%3s", " ")
				+ String.format("%05d", 0) + String.format("%010d", 0) + String.format("%05d", 0)
				+ String.format("%18s", " ") + "CTL" + String.format("%2s", " ") + String.format("%1$-924s", " ");

		String filePath = stgFileDirectory + "\\" + fileDetails.getAgencyFein() + ".txt"; // Change this to your desired
																							// file path
		// Write data to the file
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writer.write(firstRow);
			System.out.println("First Row Data written to file successfully.");
			int count = 0;

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
				int dcCount = 0;

				// Build the doc header DOH record.
				dohTransCtrlNo++; // Increment dohTransCtrlNo

				String secondRow = String.format("%5s", ssid) + "H" + String.format("%-9s", ctlNum)
						+ String.format("%05d", 0) // This ensures leading zeros for the integer
						+ String.format("%-10s", currClaimId) + String.format("%3s", "VOU")
						+ String.format("%05d", docCtrlCount + 1) // This ensures leading zeros for the integer
						+ String.format("%010d", dohTransCtrlNo) // This ensures leading zeros for the integer
						+ String.format("%05d", 0) // This ensures leading zeros for the integer
						+ String.format("%18s", " ") + "DOH" + String.format("%2s", " ")
						+ String.format("%1$-924s", " ");

				writer.newLine();
				writer.write(secondRow);

				System.out.println("Second Row DOH written successfully.");

				transCtrlNo++;

				VohDetails vohDetails = getVohRecordDetails(currClaimId);

				String acctDate = vohDetails.getAcctDate();
				String invoiceDate = vohDetails.getDfilled();
				String NDC = vohDetails.getNDC();
				String invoiceNumber = vohDetails.getInvoiceNo();
				float claimTotal = vohDetails.getClaimTotal().floatValue();
				String sfsVendorId = vohDetails.getSFSVendorId();
				String recvDate = vohDetails.getDateReceived();
				String obligDate = vohDetails.getDfilled();
				String intElig = vohDetails.getInterestEligible();
				String paymentDate = vohDetails.getPaymentDate();
				String invoiceDateStr = invoiceDate != null ? invoiceDate.replaceAll("[^\\d]", "") : "";
				String invoiceDateUpdated = "";

				if (!invoiceDateStr.isEmpty()) {
					invoiceDateUpdated = String.format("%08d", Integer.parseInt(invoiceDateStr));
				}

				String recDatetStr = recvDate != null ? recvDate.replaceAll("[^\\d]", "") : "";
				String rcvdDateUpdated = "";
				if (!recDatetStr.isEmpty()) {
					rcvdDateUpdated = String.format("%08d", Integer.parseInt(recDatetStr));
				}

				String obligDateStr = obligDate.replaceAll("[^\\d]", "");
				String obligDateUpdated = "";
				if (!obligDateStr.isEmpty()) {
					obligDateUpdated = String.format("%08d", Integer.parseInt(obligDateStr));
				}

				String paymentDateStr = paymentDate.replaceAll("[^\\d]", "");
				String paymentDateUpdated = "";
				if (!paymentDateStr.isEmpty()) {
					paymentDateUpdated = String.format("%08d", Integer.parseInt(paymentDateStr));
				}

				String acctDateStr = acctDate.replaceAll("[^\\d]", "");
				String acctDateUpdated = "";
				if (!acctDateStr.isEmpty()) {
					acctDateUpdated = String.format("%08d", Integer.parseInt(acctDateStr));

					String thirdRow = String.format("%-5s", ssid) + "X" + String.format("%-9s", ctlNum)
							+ String.format("%05d", 0) + String.format("%-10s", currClaimId)
							+ String.format("%-3s", "VOU") + String.format("%05d", 0)
							+ String.format("%010d", transCtrlNo) + String.format("%05d", dcCount)
							+ String.format("%-18s", " ") + "VOH" + String.format("%-2s", " ")
							+ String.format("%-8s", "1S" + claimIDStr) + "ADD" + "DOH01" + "+"
							+ String.format("%-26.3f", claimTotal) + String.format("%-30s", invoiceNumber)
							+ invoiceDateUpdated // Assuming invoiceDate is an integer, if not, adjust accordingly
							+ rcvdDateUpdated // Assuming recvDate is an integer, if not, adjust accordingly
							+ String.format("%010d", Integer.parseInt(sfsVendorId)) // Assuming sfsVendorId is an
																					// integer, if not, adjust
																					// accordingly
							+ String.format("%-310s", " ") + String.format("%-1s", intElig) + paymentDateUpdated // Assuming
																													// paymentDate
																													// is
																													// an
																													// integer,
																													// if
																													// not,
																													// adjust
																													// accordingly
							+ obligDateUpdated // Assuming obligDate is an integer, if not, adjust accordingly
							+ String.format("%-254s", " ") + String.format("%-143s", " ") + acctDateUpdated // Assuming
																											// acctDate
																											// is an
																											// integer,
																											// if
							+ String.format("%-93s", " ");

					writer.newLine();
					writer.write(thirdRow);
					System.out.println("Third Row VOH written successfully.");

					int voucherLineNo = 0;
					transCtrlNo++;
					voucherLineNo++;
					// Build the VOL record
					String fourthRow = String.format("%5s", ssid) + "X" + String.format("%-9s", ctlNum)
							+ String.format("%05d", 0) + String.format("%-10s", currClaimId)
							+ String.format("%3s", "VOU") + String.format("%05d", 0)
							+ String.format("%010d", transCtrlNo) + String.format("%05d", 1)
							+ String.format("%18s", " ") + "VOL" + String.format("%2s", " ")
							+ String.format("%-8s", "2S" + claimIDStr) + String.format("%05d", voucherLineNo) + "DOH01"
							+ "+" + String.format("%-26.3f", claimTotal) + String.format("%56s", " ")
							+ String.format("%-30s", NDC) + String.format("%-70s", "340B Supplemental Reimbursement")
							+ String.format("%723s", " ");

					writer.newLine();
					writer.write(fourthRow);
					System.out.println("Fourth Row VOL written successfully.");

					// Write VOD record
					transCtrlNo++;

					String fifthRow = String.format("%5s", ssid) + "X" + String.format("%-9s", ctlNum)
							+ String.format("%05d", 0) + String.format("%-10s", currClaimId)
							+ String.format("%3s", "VOU") + String.format("%05d", 0)
							+ String.format("%010d", transCtrlNo) + String.format("%05d", 0)
							+ String.format("%18s", " ") + "VOD" + String.format("%2s", " ")
							+ String.format("%-8s", "2S" + claimIDStr) + String.format("%05d", voucherLineNo) + "DOH01"
							+ "+" + String.format("%-26.3f", claimTotal) + String.format("%5s", " ")
							+ String.format("%10s", " ") + String.format("%05d", 0) + String.format("%03d", 0)
							+ String.format("%05d", 0) + String.format("%1s", " ") + String.format("%-10s", deptCode)
							+ String.format("%-5s", "10000") + String.format("%15s", " ") + String.format("%15s", " ")
							+ String.format("%5s", pgmCode) + String.format("%7s", budRef)
							+ String.format("%-10s", "60301") + String.format("%-10s", "11850")
							+ String.format("%773s", " ");

					writer.newLine();
					writer.write(fifthRow);
					System.out.println("Fifth Row VOD written successfully.");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return filePath;
	}

	private VohDetails getVohRecordDetails(int currClaimId) {
		String sql = "SELECT DISTINCT REPLACE(CONVERT(varchar, GETDATE(), 101), '/', '') AS acctDate , \r\n"
				+ "c.InvoiceNo, \r\n" + "ISNULL(Rate,0) as claimTotal, \r\n" + "c.NDC, \r\n" + "a.SFSVendorId, \r\n"
				+ "REPLACE(CONVERT(varchar, f.SubmitDate, 101), '/', '') AS date_received, \r\n"
				+ "REPLACE(CONVERT(varchar, c.dfilled, 101), '/', '') AS dfilled, \r\n"
				+ "(case when mv.payee_type in ('B','C','G','M') then 'N' else 'Y' end) as interest_eligible, \r\n"
				+ "REPLACE(CONVERT(varchar, DATEADD(day, 2, GETDATE()), 101), '/', '') AS paymentDate \r\n"
				+ "from std_claims c, \r\n" + "files f, \r\n" + "Agency a, \r\n" + "agencyreimbrate ar, \r\n"
				+ "m131_vendor mv \r\n" + "where c.FIle_id=f.id \r\n" + "and f.AgencyFEIN = a.FEIN \r\n"
				+ "and a.SFSVendorId=mv.VENDOR_ID \r\n" + "and ar.AgencyID = a.FEIN \r\n" + "and c.id = " + currClaimId;

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
		VohDetails vohDetails = new VohDetails();
		for (Map<String, Object> row : rows) {

			String acctDate = (String) row.get("acctDate");
			String invoiceNumber = (String) row.get("InvoiceNo");
			BigDecimal claimTotal = (BigDecimal) row.get("claimTotal");
			String NDC = (String) row.get("NDC");
			String SFSVendorId = (String) row.get("SFSVendorId");
			String date_received = (String) row.get("date_received");
			String dfilled = (String) row.get("dfilled");
			String interest_eligible = (String) row.get("interest_eligible");
			String paymentDate = (String) row.get("paymentDate");

			vohDetails.setAcctDate(acctDate);
			vohDetails.setInvoiceNo(invoiceNumber);
			vohDetails.setClaimTotal(claimTotal);
			vohDetails.setNDC(NDC);
			vohDetails.setSFSVendorId(SFSVendorId);
			vohDetails.setDateReceived(date_received);
			vohDetails.setDfilled(dfilled);
			vohDetails.setInterestEligible(interest_eligible);
			vohDetails.setPaymentDate(paymentDate);

		}
		return vohDetails;
	}

	///////////////////////////////Download ANd Decrypt File/////////////////////////////////
	@Override
	public void downloadAndDecrptFile() {

		utility.downloadFilesFromSftpAndDecrypt(downloadSftpFilePath, decryptedFileDirectory, passphrase, sftpHost,
				port, sftpUserName, sftpPassword, privateKeyPath, sftpRemoteDownloadDirectory,
				sftpRemoteArchiveDirectory, true, archiveDownloadDirectory, null);
	}
	
	@Override
	public void downloadAndDecryptProcessedFiles() {

		utility.downloadFilesFromSftpAndDecrypt(downloadSftpProcessedFilePath, decryptedProcessedFileDirectory, passphrase, sftpHost,
				port, sftpUserName, sftpPassword, privateKeyPath, sftpProcessedRemoteDownloadDirectory,
				sftpProcessedRemoteArchiveDirectory, false, archiveProcessedDownloadDirectory, Constants.FILE_PATTERN);
	}

}