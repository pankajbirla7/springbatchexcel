package com.example.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.Constants;
import com.example.demo.EmailUtility;
import com.example.demo.FileDetails;
import com.example.dto.StdClaim;
import com.example.dto.VohDetails;
import com.example.repository.StdClaimDao;
import com.example.utility.Utility;

@Service
public class FileWriteServiceImpl implements FileWriteService {

	static final Logger logger = LoggerFactory.getLogger(FileWriteServiceImpl.class);

	@Autowired
	public JdbcTemplate jdbcTemplate;
	
	private TransactionTemplate transactionTemplate;

	@Autowired
	StdClaimDao stdClaimDao;

	@Autowired
	FileDao fileDao;

	@Autowired
	Utility utility;

	@Autowired
	EmailUtility emailUtility;
	
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

	@Value("${html.file.path}")
	private String htmlFilePath;
	
	@Value("${csv.file.path}")
	private String csvFilePath;

	@Value("${pdf.file.path}")
	private String pdfFilePath;
	
	@Value("${load_M171_data.storeprocedure.name}")
	private String loadM171Sp;

	@Value("${load_paymentsummary.storeprocedure.name}")
	private String loadPaymentSummarySp;
	
	@Value("${generate_voucher_summary.storeprocedure.name}")
	private String generateVoucherSummarySp;

//	#################################################################################

	@Override
	public void generateFile() {
		try {
			logger.info("Fetching total std claims for status new and datetosfs status is null status value ::  "
					+ Constants.NEW);
			List<StdClaim> stdClaims = stdClaimDao.getStdClaimDetails(Constants.NEW);
			logger.info("total stdClaimList " + stdClaims.size());

			if (stdClaims != null && stdClaims.size() > 0) {
				for (StdClaim stdClaim : stdClaims) {
					FileDetails fileDetails = fileDao.getFileDetailsByFileID(stdClaim.getFileid());
					int claimCount = stdClaimDao.getClaimCountByDateEntered(fileDetails.getSubmitDate(), Constants.NEW);
					logger.info("Total Cliam count for stadClaim date value greater than : " + " for file Id : "
							+ stdClaim.getFileid());

					List<StdClaim> stdClaims2 = stdClaimDao.getClaimIds(fileDetails.getSubmitDate(), Constants.NEW);
					String filePath = writeFirstClaimCountRowInFile(claimCount, stdClaim, stdClaims2, fileDetails);

					logger.info("While generating and encrypting file process the filepath is : " + filePath);
					if (filePath != null) {
						encryptFileAndUploadToSftp(filePath, stdClaim.getFein());
						stdClaimDao.updateStandardDetailSfsDate(stdClaims2);
						emailUtility.sendEmail(
								"JOB 2 : Generate file and encrypt file and upload to sftp job is success at time "
										+ System.currentTimeMillis(),
								Constants.SUCCESSS);
					}
					
					break;
				}
			} else {
				logger.info(
						"total stdClaimList for claim status new and datetosfs is null records not found " + stdClaims);
			}
		} catch (Exception e) {
			logger.error(
					"JOB 2 : Generate file and encrypt file and upload to sftp job is failed due to "
							+ Utility.getStackTrace(e));
			emailUtility.sendEmail(
					"JOB 2 : Generate file and encrypt file and upload to sftp job is failed at time "
							+ System.currentTimeMillis(),
					Constants.FAILED);
		}
	}
///////////////////////////////Encrypt and Upload File/////////////////////////////////////

	private void encryptFileAndUploadToSftp(String filePath, int fein) throws Exception {
		String inputFilePath = filePath;
		LocalDate today = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMddyyyy");
		String formattedDate = today.format(formatter);

		String encryptedFileName = "SN12969_"+fein+"_" + formattedDate;
		String outputFilePath = encryptedFileDirectory + "\\" + encryptedFileName + ".gpg";

		utility.moveFileToSFTP(inputFilePath, outputFilePath, publicKeyPath, passphrase, sftpHost, port, sftpUserName,
				sftpPassword, privateKeyPath, sftpRemoteUploadDirectory);
	}

	private String writeFirstClaimCountRowInFile(int claimCount, StdClaim stdClaim1, List<StdClaim> stdClaims2,
			FileDetails fileDetails) throws Exception {
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
			logger.info("First Row Data written to file successfully. " + firstRow);
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

				logger.info("Second Row DOH written successfully.");

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
					logger.info("Third Row VOH written successfully.");

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
					logger.info("Fourth Row VOL written successfully.");

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
					logger.info("Fifth Row VOD written successfully.");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error occured while generating file and content in method writeFirstClaimCountRowInFile at time error is :  "+Utility.getStackTrace(e));
			throw e;
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

	/////////////////////////////// Download ANd Decrypt
	/////////////////////////////// File/////////////////////////////////
	@Override
	public void downloadAndDecrptFile() {
		int count = 0;
		try {
			count = utility.downloadFilesFromSftpAndDecrypt(downloadSftpFilePath, decryptedFileDirectory, passphrase, sftpHost,
					port, sftpUserName, sftpPassword, privateKeyPath, sftpRemoteDownloadDirectory,
					sftpRemoteArchiveDirectory, true, archiveDownloadDirectory, null);
			
			if(count > 0) {
				emailUtility.sendEmail(
						"JOB 3 : Download files from sftp and Decrypt files and process voucher details job is success at time "
								+ System.currentTimeMillis(),
						Constants.SUCCESSS);
			}
		} catch (Exception e) {
			logger.error(
					"JOB 3 : downloadAndDecrptFile method, Download files from sftp and Decrypt files and process vpucher details job is failed due to "
							+ Utility.getStackTrace(e));
			emailUtility.sendEmail(
					"JOB 3 : Download files from sftp and Decrypt files and process vpucher details job is failed at time "
							+ System.currentTimeMillis(),
					Constants.FAILED);
		}
	}

	@Override
	public void downloadAndDecryptProcessedFiles(PlatformTransactionManager transactionManager) {
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		int count = 0;
		try {
			emailUtility.sendEmail(
					"JOB 4 : Download processed files from sftp and Decrypt processed files and process voucher details and CSV to PDF Conversion job is started at time "
							+ System.currentTimeMillis(),
					Constants.SUCCESSS);
			
			count = utility.downloadFilesFromSftpAndDecrypt(downloadSftpProcessedFilePath, decryptedProcessedFileDirectory,
					passphrase, sftpHost, port, sftpUserName, sftpPassword, privateKeyPath,
					sftpProcessedRemoteDownloadDirectory, sftpProcessedRemoteArchiveDirectory, false,
					archiveProcessedDownloadDirectory, Constants.FILE_PATTERN);
			final Integer[] spResponses = new Integer[1];
			Integer spResponse = 1;

			transactionTemplate.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					try {
						spResponses[0] = callStoredProcedure(loadM171Sp);
						logger.info("Executing Stored procedure :"+loadM171Sp+" :: And Resposne is : " + spResponse + " :: Completed at time : "
								+ System.currentTimeMillis());
					} catch (Exception e) {
						logger.error("JOB 4 : Stored Procedure :"+loadM171Sp + "  is failed due to "
								+ Utility.getStackTrace(e));
						emailUtility.sendEmail(
								"JOB 4 : Stored Procedure :"+loadM171Sp + "  is failed due to  "+Utility.getStackTrace(e),
								Constants.FAILED);
					}
				}
			});

			boolean isJobResume = spResponses[0] == 0 ? true : false;

			if (isJobResume) {
				
				transactionTemplate.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						try {
							spResponses[0] = callStoredProcedure(loadPaymentSummarySp);
							logger.info("Executing Stored procedure :"+loadPaymentSummarySp+" :: And Resposne is : " + spResponse + " :: Completed at time : "
									+ System.currentTimeMillis());
						} catch (Exception e) {
							logger.error("JOB 4 : Stored Procedure :"+loadPaymentSummarySp + "  is failed due to "
									+ Utility.getStackTrace(e));
							emailUtility.sendEmail(
									"JOB 4 : Stored Procedure :"+loadPaymentSummarySp + "  is failed due to  "+Utility.getStackTrace(e),
									Constants.FAILED);
						}
					}
				});
				
				isJobResume = spResponses[0] == 0 ? true : false;
				
				if (isJobResume) {
					transactionTemplate.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) {
							try {
								spResponses[0] = callStoredProcedure(generateVoucherSummarySp);
								logger.info("Executing Stored procedure :"+generateVoucherSummarySp+" :: And Resposne is : " + spResponse + " :: Completed at time : "
										+ System.currentTimeMillis());
							} catch (Exception e) {
								logger.error("JOB 4 : Stored Procedure :"+generateVoucherSummarySp + "  is failed due to "
										+ Utility.getStackTrace(e));
								emailUtility.sendEmail(
										"JOB 4 : Stored Procedure :"+generateVoucherSummarySp + "  is failed due to  "+Utility.getStackTrace(e),
										Constants.FAILED);
							}
						}
					});
					
					isJobResume = spResponses[0] == 0 ? true : false;
					
					if (isJobResume) {
						try {
							//migrateCsvToPdfFiles();
							migrateHtmlToPdfFiles();
						}catch(Exception e) {
							logger.error("Error occured during migaretCsvToPdfFiles due to :: "+Utility.getStackTrace(e));
							emailUtility.sendEmail(
									"JOB 4 : migrateHtmlToPdfFiles is failed due to  "+Utility.getStackTrace(e),
									Constants.FAILED);
						}
					} else {
						emailUtility.sendEmail("Calling stored procedure "+generateVoucherSummarySp+" -- response is not 0 response is : " + spResponse
								+ " - at time " + System.currentTimeMillis(), Constants.SUCCESSS);
					}
					
				} else {
					emailUtility.sendEmail("Calling stored procedure "+loadPaymentSummarySp+" -- response is not 0 response is : " + spResponse
							+ " - at time " + System.currentTimeMillis(), Constants.SUCCESSS);
				}
				
			} else {
				emailUtility.sendEmail("Calling stored procedure "+loadM171Sp+" -- response is not 0 response is : " + spResponse
						+ " - at time " + System.currentTimeMillis(), Constants.SUCCESSS);
			}
			
			if(count > 0) {
				emailUtility.sendEmail(
						"JOB 4 : Download processed files from sftp and Decrypt processed files and process voucher details and CSV to PDF Conversion job is ended at time "
								+ System.currentTimeMillis(),
						Constants.SUCCESSS);
			}
		} catch (Exception e) {
			logger.error(
					"JOB 4 : downloadAndDecryptProcessedFiles method, Download files from sftp and Decrypt files and process voucher details and CSV to PDF Conversion job is failed due to "
							+ Utility.getStackTrace(e));
			emailUtility.sendEmail(
					"JOB 4 : Download processed files from sftp and Decrypt processed files and process voucher details and CSV to PDF Conversion job is failed at time "
							+ System.currentTimeMillis(),
					Constants.FAILED);
		}
	}

	@Override
	public void migrateCsvToPdfFiles() throws Exception {

		utility.migaretCsvToPdfFiles(csvFilePath, pdfFilePath);
	}
	
	@Override
	public void migrateHtmlToPdfFiles() throws Exception {

		utility.migrateHtmlToPdfFiles(htmlFilePath, pdfFilePath);
	}
	
	public int callStoredProcedure(String storedProcedureName) {
		try {
			List<Integer> resultList = jdbcTemplate.query("CALL " + storedProcedureName, new RowMapper<Integer>() {
				@Override
				public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
					return rs.getInt("outputValue");
				}
			});
			return resultList.get(0);
		} catch (Exception e) {
			logger.error("Exception occurred while calling stored procedure due to " + Utility.getStackTrace(e));
			return 1;
		}
	}


}