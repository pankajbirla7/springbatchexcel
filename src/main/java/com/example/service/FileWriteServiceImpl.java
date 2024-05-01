package com.example.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.operator.jcajce.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.Constants;
import com.example.demo.FileDetails;
import com.example.dto.StdClaim;
import com.example.dto.VohDetails;
import com.example.repository.SafeNetClaimDao;
import com.example.repository.StdClaimDao;
import com.example.utility.Utility;

import org.bouncycastle.openpgp.*;
import com.jcraft.jsch.*;
import java.io.*;
import java.security.SecureRandom;
import java.security.Security;

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

//	@Value("${public_key_path}")
//	private String publicKeyPath;
//	
//	@Value("${private_key_path}")
//	private String privateKeyPath;

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

		//	encryptFileAndUploadToSftp(filePath);
			String encryptedFile = getOriginalFileEncrypted(filePath);

			uploadFileToSftpServer(encryptedFile);
		}
	}

	private void uploadFileToSftpServer(String encryptedFile) {
		try {
			String remoteSftpFolderPath = "sftp folder path"; //Please update
			String userName = "userName";
			String password = "password";
			String host = "host";

			File file = new File(encryptedFile);
			// Establishing the session
			JSch jsch = new JSch();
			Session session = jsch.getSession(userName, host, 22);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			// Opening the SFTP channel
			ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
			channelSftp.connect();

			// Uploading the encrypted file
			FileInputStream encryptedFileInputStream = new FileInputStream(file);
			channelSftp.put(encryptedFileInputStream, remoteSftpFolderPath+file.getName());

			// Disconnecting the channel and session
			channelSftp.disconnect();
			session.disconnect();

			// Closing the file input stream
			encryptedFileInputStream.close();

			System.out.println("File uploaded successfully.");
		} catch (JSchException | SftpException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getOriginalFileEncrypted(String filePath){

		String encryptedFile = "C:\\Users\\acer\\Documents\\Ronak\\encrypted.txt";
		String passpharse = "passpharse";
		String publicKey = "C:\\Users\\acer\\Documents\\Ronak\\public_key.asc";
		FileInputStream in = null;
		try {
			in = new FileInputStream(new File(filePath));

			FileOutputStream out = new FileOutputStream(new File(encryptedFile));

			// Load the recipient's public key
			InputStream keyIn = new FileInputStream(new File(publicKey));
			 Security.addProvider(new BouncyCastleProvider());
			PGPPublicKey publicKeysLoaded = readPublicKey(keyIn);

			// Write encrypted data to the output stream
			encryptFile(out, "recipient@example.com", publicKeysLoaded, in, passpharse.toCharArray(), true, true);

			out.close();
			keyIn.close();
			in.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return encryptedFile;
	}

	private static PGPPublicKey readPublicKey(InputStream in) throws IOException, PGPException {
		PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(in);

		// We just loop through the collection till we find a key suitable for encryption.
		Iterator<PGPPublicKeyRing> keyRingIter = pgpPub.getKeyRings();
		while (keyRingIter.hasNext()) {
			PGPPublicKeyRing keyRing = keyRingIter.next();
			Iterator<PGPPublicKey> keyIter = keyRing.getPublicKeys();
			while (keyIter.hasNext()) {
				PGPPublicKey key = keyIter.next();
				if (key.isEncryptionKey()) {
					return key;
				}
			}
		}
		throw new IllegalArgumentException("Can't find encryption key in key ring.");
	}

	private static void encryptFile(OutputStream out, String fileName, PGPPublicKey publicKey, InputStream dataIn, char[] passPhrase, boolean armor, boolean withIntegrityCheck) throws Exception {
		PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(new JcePGPDataEncryptorBuilder(PGPEncryptedDataGenerator.CAST5).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(new SecureRandom()).setProvider("BC"));
		encryptedDataGenerator.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(publicKey));

		OutputStream encryptedOut = encryptedDataGenerator.open(out, new byte[1 << 16]);

		PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
		OutputStream literalOut = literalDataGenerator.open(encryptedOut, PGPLiteralData.BINARY, fileName, new Date(), new byte[1 << 16]);

		byte[] buf = new byte[1 << 16];
		int len;
		while ((len = dataIn.read(buf)) > 0) {
			literalOut.write(buf, 0, len);
			encryptedOut.write(buf, 0, len);
		}

		literalDataGenerator.close();
		encryptedDataGenerator.close();
	}






	private void encryptFileAndUploadToSftp(String filePath) {
		String inputFilePath = filePath;
		File f = new File(filePath);
        String outputFilePath = f.getName()+".asc";
        String publicKeyPath = "recipient_public_key.asc";
        String passphrase = "your_passphrase";
        String host = "your_sftp_host";
        int port = 22; // Default SFTP port
        String username = "your_username";
        String password = "your password";
        String privateKeyPath = "/path/to/private_key";
        String remoteDirectory = "/path/to/remote/directory";
        
        Utility.moveFileToSFTP(inputFilePath, outputFilePath, publicKeyPath, passphrase, 
        		host, port, username, password, privateKeyPath, remoteDirectory);

	}

//	private void moveFileToSFTP(String filePath) {
//		String publicKeyPath = "path/to/publicKey.asc"; // Path to your public key file
//		String privateKeyPath = "path/to/privateKey.asc"; // Path to your private key file
//		String inputFile = filePath; // Path to your file to encrypt and upload
//		String username = "yourSftpUsername";
//		String password = "yourSftpPassword";
//		String sftpHost = "yourSftpHost";
//		String sftpDestinationFolder = "foldername in sftp server path";
//		int sftpPort = 22; // Default SFTP port
//
//		try {
//			// Load keys
//			PGPPublicKey publicKey = loadPublicKey(publicKeyPath);
//			PGPSecretKey secretKey = loadSecretKey(privateKeyPath);
//
//			// Encrypt file
//			encryptFile(inputFile, publicKey);
//
//			// Decrypt file (optional)
//			// decryptFile(inputFile + ".pgp", secretKey);
//
//			// Upload encrypted file to SFTP server
//			uploadToSftp(inputFile + ".pgp", username, password, sftpHost, sftpPort, sftpDestinationFolder);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	private static PGPPublicKey loadPublicKey(String publicKeyPath) throws IOException, PGPException {
//		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//		try (InputStream keyInputStream = new FileInputStream(publicKeyPath)) {
//			PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(keyInputStream);
//			Iterator<PGPPublicKeyRing> keyRingIterator = pgpPub.getKeyRings();
//			while (keyRingIterator.hasNext()) {
//				PGPPublicKeyRing keyRing = keyRingIterator.next();
//				Iterator<PGPPublicKey> keyIterator = keyRing.getPublicKeys();
//				while (keyIterator.hasNext()) {
//					PGPPublicKey key = keyIterator.next();
//					if (key.isEncryptionKey()) {
//						return key;
//					}
//				}
//			}
//		} catch (IOException | PGPException e) {
//			// Print or log the error message
//			e.printStackTrace();
//			throw e; // Rethrow the exception to the caller
//		}
//		throw new IllegalArgumentException("No encryption key found in the provided public key file.");
//	}
//
//	private static PGPSecretKey loadSecretKey(String privateKeyPath) throws IOException, PGPException {
//		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//		InputStream keyInputStream = new FileInputStream(privateKeyPath);
//		PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(keyInputStream);
//		Iterator<PGPSecretKeyRing> keyRingIterator = pgpSec.getKeyRings();
//		while (keyRingIterator.hasNext()) {
//			PGPSecretKeyRing keyRing = keyRingIterator.next();
//			Iterator<PGPSecretKey> keyIterator = keyRing.getSecretKeys();
//			while (keyIterator.hasNext()) {
//				PGPSecretKey key = keyIterator.next();
//				if (key.isSigningKey()) {
//					return key;
//				}
//			}
//		}
//		throw new IllegalArgumentException("No signing key found in the provided private key file.");
//	}
//
//	private static void encryptFile(String inputFile, PGPPublicKey publicKey) throws IOException, PGPException {
//		InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
//		OutputStream out = new BufferedOutputStream(new FileOutputStream(inputFile + ".pgp"));
//		PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
//				new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5).setWithIntegrityPacket(true)
//						.setSecureRandom(new SecureRandom()).setProvider("BC"));
//		encryptedDataGenerator.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(publicKey).setProvider("BC"));
//		OutputStream encryptedOut = encryptedDataGenerator.open(out, new byte[1024]);
//		byte[] buffer = new byte[1024];
//		int bytesRead;
//		while ((bytesRead = in.read(buffer)) != -1) {
//			encryptedOut.write(buffer, 0, bytesRead);
//		}
//		encryptedOut.close();
//		out.close();
//		in.close();
//	}

//    private static void decryptFile(String inputFile, PGPSecretKey secretKey) throws IOException, PGPException {
//        InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
//        InputStream privateKeyStream = secretKey.extractPrivateKey("your_private_key_password".toCharArray()).getKeyID();
//        PGPObjectFactory pgpF = new PGPObjectFactory(PGPUtil.getDecoderStream(in), null);
//        PGPEncryptedDataList enc;
//        Object o = pgpF.nextObject();
//        if (o instanceof PGPEncryptedDataList) {
//            enc = (PGPEncryptedDataList) o;
//        } else {
//            enc = (PGPEncryptedDataList) pgpF.nextObject();
//        }
//        Iterator<?> it = enc.getEncryptedDataObjects();
//        PGPPrivateKey privateKey = null;
//        PGPPublicKeyEncryptedData pbe = null;
//        while (privateKey == null && it.hasNext()) {
//            pbe = (PGPPublicKeyEncryptedData) it.next();
//            privateKey = findSecretKey(secretKey, pbe.getKeyID());
//        }
//        if (privateKey == null) {
//            throw new IllegalArgumentException("Private key for message not found.");
//        }
//        InputStream clear = pbe.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(privateKey));
//        PGPObjectFactory plainFact = new PGPObjectFactory(clear, null);
//        Object message = plainFact.nextObject();
//        if (message instanceof PGPCompressedData) {
//            PGPCompressedData cData = (PGPCompressedData) message;
//            PGPObjectFactory pgpFact = new PGPObjectFactory(cData.getDataStream(), null);
//            message = pgpFact.nextObject();
//        }
//        if (message instanceof PGPLiteralData) {
//            PGPLiteralData ld = (PGPLiteralData) message;
//            InputStream unc = ld.getInputStream();
//            OutputStream fOut = new BufferedOutputStream(new FileOutputStream(inputFile + ".decrypted"));
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = unc.read(buffer)) != -1) {
//                fOut.write(buffer, 0, bytesRead);
//            }
//            fOut.close();
//            unc.close();
//        } else {
//            throw new IllegalArgumentException("Message is not a simple encrypted file - type unknown.");
//        }
//        in.close();
//        privateKeyStream.close();
//    }

	private static PGPPrivateKey findSecretKey(PGPSecretKey secretKey, long keyID) throws PGPException {
		return secretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC")
				.build("your_private_key_password".toCharArray()));
	}

	private static void uploadToSftp(String inputFile, String username, String password, String sftpHost, int sftpPort,
			String sftpDestinationFolder) throws JSchException, SftpException {
		JSch jsch = new JSch();
		Session session = jsch.getSession(username, sftpHost, sftpPort);
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect();
		ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
		channelSftp.connect();
		channelSftp.cd(sftpDestinationFolder);
		channelSftp.put(inputFile, inputFile.substring(inputFile.lastIndexOf("/") + 1)); // Destination file name on the
																							// server
		channelSftp.disconnect();
		session.disconnect();
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

		String filePath = stgFileDirectory + "\\" + fileDetails.getAgencyFin() + ".txt"; // Change this to your desired
																							// file path

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
				int dcCount = 0;

				// Build the doc header DOH record.
				dohTransCtrlNo++; // Increment dohTransCtrlNo

				String secondRow = String.format("%5s", ssid) + "H" + String.format("%-9s", ctlNum)
						+ String.format("%05d", 0) + // This ensures leading zeros for the integer
						String.format("%-10s", currClaimId) + String.format("%3s", "VOU")
						+ String.format("%05d", docCtrlCount + 1) + // This ensures leading zeros for the integer
						String.format("%010d", dohTransCtrlNo) + // This ensures leading zeros for the integer
						String.format("%05d", 0) + // This ensures leading zeros for the integer
						String.format("%18s", " ") + "DOH" + String.format("%2s", " ") + String.format("%1$-924s", " ");

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
				String recvDate = vohDetails.getDateRecieved();
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
				}

				// Build the VOH record.
				String thirdRow = String.format("%-5s", ssid) + "X" + String.format("%-9s", ctlNum)
						+ String.format("%05d", 0) + String.format("%-10s", currClaimId) + String.format("%-3s", "VOU")
						+ String.format("%05d", 0) + String.format("%010d", transCtrlNo)
						+ String.format("%05d", dcCount) + String.format("%-18s", " ") + "VOH"
						+ String.format("%-2s", " ") + String.format("%-8s", "1S" + claimIDStr) + "ADD" + "DOH01" + "+"
						+ String.format("%-26.3f", claimTotal) + String.format("%-30s", invoiceNumber)
						+ invoiceDateUpdated + // Invoice date
						rcvdDateUpdated + // Recieved date
						String.format("%010d", Integer.parseInt(sfsVendorId)) + String.format("%-310s", " ")
						+ String.format("%-1s", intElig) + paymentDateUpdated + // Payment date
						obligDateUpdated + // Obliged date
						String.format("%-254s", " ") + String.format("%-143s", " ") + acctDateUpdated
						+ String.format("%-93s", " ");

				writer.newLine();
				writer.write(thirdRow);
				System.out.println("Third Row VOH written successfully.");

				int voucherLineNo = 0;
				transCtrlNo++;
				voucherLineNo++;
				// Build the VOL record
				String fourthRow = String.format("%5s", ssid) + "X" + String.format("%-9s", ctlNum)
						+ String.format("%05d", 0) + String.format("%-10s", currClaimId) + String.format("%3s", "VOU")
						+ String.format("%05d", 0) + String.format("%010d", transCtrlNo) + String.format("%05d", 1)
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
						+ String.format("%05d", 0) + String.format("%-10s", currClaimId) + String.format("%3s", "VOU")
						+ String.format("%05d", 0) + String.format("%010d", transCtrlNo) + String.format("%05d", 0)
						+ String.format("%18s", " ") + "VOD" + String.format("%2s", " ")
						+ String.format("%-8s", "2S" + claimIDStr) + String.format("%05d", voucherLineNo) + "DOH01"
						+ "+" + String.format("%-26.3f", claimTotal) + String.format("%5s", " ")
						+ String.format("%10s", " ") + String.format("%05d", 0) + String.format("%03d", 0)
						+ String.format("%05d", 0) + String.format("%1s", " ") + String.format("%-10s", deptCode)
						+ String.format("%-5s", "10000") + String.format("%15s", " ") + String.format("%15s", " ")
						+ String.format("%5s", pgmCode) + String.format("%7s", budRef) + String.format("%-10s", "60301")
						+ String.format("%-10s", "11850") + String.format("%773s", " ");

				writer.newLine();
				writer.write(fifthRow);
				System.out.println("Fifth Row VOD written successfully.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return filePath;
	}

	private VohDetails getVohRecordDetails(int currClaimId) {
		String sql = "SELECT DISTINCT REPLACE(DATE_FORMAT(CURDATE(), '%m/%d/%Y'), '/', '') AS acctDate , \r\n"
				+ "c.InvoiceNo, \r\n" + "IFNULL(Rate,0) as claimTotal, \r\n" + "c.NDC, \r\n" + "a.SFSVendorId, \r\n"
				+ "REPLACE(DATE_FORMAT(f.SubmitDate, '%m/%d/%Y'), '/', '') AS date_received, \r\n"
				+ "REPLACE(DATE_FORMAT(c.dfilled, '%m/%d/%Y'), '/', '') AS dfilled, \r\n"
				+ "(case when mv.payee_type in ('B','C','G','M') then 'N' else 'Y' end) as interest_eligible, \r\n"
				+ "REPLACE(DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '%m/%d/%Y'), '/', '') AS paymentDate \r\n"
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
			vohDetails.setDateRecieved(date_received);
			vohDetails.setDfilled(dfilled);
			vohDetails.setInterestEligible(interest_eligible);
			vohDetails.setPaymentDate(paymentDate);

		}
		return vohDetails;
	}
}

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
import com.example.repository.SafeNetClaimDao;
import com.example.repository.StdClaimDao;
import com.example.utility.Utility;

import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import com.jcraft.jsch.*;
import java.io.*;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Iterator;

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

//	@Value("${public_key_path}")
//	private String publicKeyPath;
//	
//	@Value("${private_key_path}")
//	private String privateKeyPath;

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

		}
	}

	private void encryptFileAndUploadToSftp(String filePath) {
		String inputFilePath = filePath;
		File f = new File(filePath);
        String outputFilePath = f.getName()+".asc";
        String publicKeyPath = "recipient_public_key.asc";
        String passphrase = "your_passphrase";
        String host = "your_sftp_host";
        int port = 22; // Default SFTP port
        String username = "your_username";
        String password = "your password";
        String privateKeyPath = "/path/to/private_key";
        String remoteDirectory = "/path/to/remote/directory";
        
        Utility.moveFileToSFTP(inputFilePath, outputFilePath, publicKeyPath, passphrase, 
        		host, port, username, password, privateKeyPath, remoteDirectory);

	}

//	private void moveFileToSFTP(String filePath) {
//		String publicKeyPath = "path/to/publicKey.asc"; // Path to your public key file
//		String privateKeyPath = "path/to/privateKey.asc"; // Path to your private key file
//		String inputFile = filePath; // Path to your file to encrypt and upload
//		String username = "yourSftpUsername";
//		String password = "yourSftpPassword";
//		String sftpHost = "yourSftpHost";
//		String sftpDestinationFolder = "foldername in sftp server path";
//		int sftpPort = 22; // Default SFTP port
//
//		try {
//			// Load keys
//			PGPPublicKey publicKey = loadPublicKey(publicKeyPath);
//			PGPSecretKey secretKey = loadSecretKey(privateKeyPath);
//
//			// Encrypt file
//			encryptFile(inputFile, publicKey);
//
//			// Decrypt file (optional)
//			// decryptFile(inputFile + ".pgp", secretKey);
//
//			// Upload encrypted file to SFTP server
//			uploadToSftp(inputFile + ".pgp", username, password, sftpHost, sftpPort, sftpDestinationFolder);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	private static PGPPublicKey loadPublicKey(String publicKeyPath) throws IOException, PGPException {
//		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//		try (InputStream keyInputStream = new FileInputStream(publicKeyPath)) {
//			PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(keyInputStream);
//			Iterator<PGPPublicKeyRing> keyRingIterator = pgpPub.getKeyRings();
//			while (keyRingIterator.hasNext()) {
//				PGPPublicKeyRing keyRing = keyRingIterator.next();
//				Iterator<PGPPublicKey> keyIterator = keyRing.getPublicKeys();
//				while (keyIterator.hasNext()) {
//					PGPPublicKey key = keyIterator.next();
//					if (key.isEncryptionKey()) {
//						return key;
//					}
//				}
//			}
//		} catch (IOException | PGPException e) {
//			// Print or log the error message
//			e.printStackTrace();
//			throw e; // Rethrow the exception to the caller
//		}
//		throw new IllegalArgumentException("No encryption key found in the provided public key file.");
//	}
//
//	private static PGPSecretKey loadSecretKey(String privateKeyPath) throws IOException, PGPException {
//		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//		InputStream keyInputStream = new FileInputStream(privateKeyPath);
//		PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(keyInputStream);
//		Iterator<PGPSecretKeyRing> keyRingIterator = pgpSec.getKeyRings();
//		while (keyRingIterator.hasNext()) {
//			PGPSecretKeyRing keyRing = keyRingIterator.next();
//			Iterator<PGPSecretKey> keyIterator = keyRing.getSecretKeys();
//			while (keyIterator.hasNext()) {
//				PGPSecretKey key = keyIterator.next();
//				if (key.isSigningKey()) {
//					return key;
//				}
//			}
//		}
//		throw new IllegalArgumentException("No signing key found in the provided private key file.");
//	}
//
//	private static void encryptFile(String inputFile, PGPPublicKey publicKey) throws IOException, PGPException {
//		InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
//		OutputStream out = new BufferedOutputStream(new FileOutputStream(inputFile + ".pgp"));
//		PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
//				new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5).setWithIntegrityPacket(true)
//						.setSecureRandom(new SecureRandom()).setProvider("BC"));
//		encryptedDataGenerator.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(publicKey).setProvider("BC"));
//		OutputStream encryptedOut = encryptedDataGenerator.open(out, new byte[1024]);
//		byte[] buffer = new byte[1024];
//		int bytesRead;
//		while ((bytesRead = in.read(buffer)) != -1) {
//			encryptedOut.write(buffer, 0, bytesRead);
//		}
//		encryptedOut.close();
//		out.close();
//		in.close();
//	}

//    private static void decryptFile(String inputFile, PGPSecretKey secretKey) throws IOException, PGPException {
//        InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
//        InputStream privateKeyStream = secretKey.extractPrivateKey("your_private_key_password".toCharArray()).getKeyID();
//        PGPObjectFactory pgpF = new PGPObjectFactory(PGPUtil.getDecoderStream(in), null);
//        PGPEncryptedDataList enc;
//        Object o = pgpF.nextObject();
//        if (o instanceof PGPEncryptedDataList) {
//            enc = (PGPEncryptedDataList) o;
//        } else {
//            enc = (PGPEncryptedDataList) pgpF.nextObject();
//        }
//        Iterator<?> it = enc.getEncryptedDataObjects();
//        PGPPrivateKey privateKey = null;
//        PGPPublicKeyEncryptedData pbe = null;
//        while (privateKey == null && it.hasNext()) {
//            pbe = (PGPPublicKeyEncryptedData) it.next();
//            privateKey = findSecretKey(secretKey, pbe.getKeyID());
//        }
//        if (privateKey == null) {
//            throw new IllegalArgumentException("Private key for message not found.");
//        }
//        InputStream clear = pbe.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(privateKey));
//        PGPObjectFactory plainFact = new PGPObjectFactory(clear, null);
//        Object message = plainFact.nextObject();
//        if (message instanceof PGPCompressedData) {
//            PGPCompressedData cData = (PGPCompressedData) message;
//            PGPObjectFactory pgpFact = new PGPObjectFactory(cData.getDataStream(), null);
//            message = pgpFact.nextObject();
//        }
//        if (message instanceof PGPLiteralData) {
//            PGPLiteralData ld = (PGPLiteralData) message;
//            InputStream unc = ld.getInputStream();
//            OutputStream fOut = new BufferedOutputStream(new FileOutputStream(inputFile + ".decrypted"));
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = unc.read(buffer)) != -1) {
//                fOut.write(buffer, 0, bytesRead);
//            }
//            fOut.close();
//            unc.close();
//        } else {
//            throw new IllegalArgumentException("Message is not a simple encrypted file - type unknown.");
//        }
//        in.close();
//        privateKeyStream.close();
//    }

	private static PGPPrivateKey findSecretKey(PGPSecretKey secretKey, long keyID) throws PGPException {
		return secretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC")
				.build("your_private_key_password".toCharArray()));
	}

	private static void uploadToSftp(String inputFile, String username, String password, String sftpHost, int sftpPort,
			String sftpDestinationFolder) throws JSchException, SftpException {
		JSch jsch = new JSch();
		Session session = jsch.getSession(username, sftpHost, sftpPort);
		session.setPassword(password);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect();
		ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
		channelSftp.connect();
		channelSftp.cd(sftpDestinationFolder);
		channelSftp.put(inputFile, inputFile.substring(inputFile.lastIndexOf("/") + 1)); // Destination file name on the
																							// server
		channelSftp.disconnect();
		session.disconnect();
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

		String filePath = stgFileDirectory + "\\" + fileDetails.getAgencyFin() + ".txt"; // Change this to your desired
																							// file path

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
				int dcCount = 0;

				// Build the doc header DOH record.
				dohTransCtrlNo++; // Increment dohTransCtrlNo

				String secondRow = String.format("%5s", ssid) + "H" + String.format("%-9s", ctlNum)
						+ String.format("%05d", 0) + // This ensures leading zeros for the integer
						String.format("%-10s", currClaimId) + String.format("%3s", "VOU")
						+ String.format("%05d", docCtrlCount + 1) + // This ensures leading zeros for the integer
						String.format("%010d", dohTransCtrlNo) + // This ensures leading zeros for the integer
						String.format("%05d", 0) + // This ensures leading zeros for the integer
						String.format("%18s", " ") + "DOH" + String.format("%2s", " ") + String.format("%1$-924s", " ");

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
				String recvDate = vohDetails.getDateRecieved();
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
				}

				// Build the VOH record.
				String thirdRow = String.format("%-5s", ssid) + "X" + String.format("%-9s", ctlNum)
						+ String.format("%05d", 0) + String.format("%-10s", currClaimId) + String.format("%-3s", "VOU")
						+ String.format("%05d", 0) + String.format("%010d", transCtrlNo)
						+ String.format("%05d", dcCount) + String.format("%-18s", " ") + "VOH"
						+ String.format("%-2s", " ") + String.format("%-8s", "1S" + claimIDStr) + "ADD" + "DOH01" + "+"
						+ String.format("%-26.3f", claimTotal) + String.format("%-30s", invoiceNumber)
						+ invoiceDateUpdated + // Invoice date
						rcvdDateUpdated + // Recieved date
						String.format("%010d", Integer.parseInt(sfsVendorId)) + String.format("%-310s", " ")
						+ String.format("%-1s", intElig) + paymentDateUpdated + // Payment date
						obligDateUpdated + // Obliged date
						String.format("%-254s", " ") + String.format("%-143s", " ") + acctDateUpdated
						+ String.format("%-93s", " ");

				writer.newLine();
				writer.write(thirdRow);
				System.out.println("Third Row VOH written successfully.");

				int voucherLineNo = 0;
				transCtrlNo++;
				voucherLineNo++;
				// Build the VOL record
				String fourthRow = String.format("%5s", ssid) + "X" + String.format("%-9s", ctlNum)
						+ String.format("%05d", 0) + String.format("%-10s", currClaimId) + String.format("%3s", "VOU")
						+ String.format("%05d", 0) + String.format("%010d", transCtrlNo) + String.format("%05d", 1)
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
						+ String.format("%05d", 0) + String.format("%-10s", currClaimId) + String.format("%3s", "VOU")
						+ String.format("%05d", 0) + String.format("%010d", transCtrlNo) + String.format("%05d", 0)
						+ String.format("%18s", " ") + "VOD" + String.format("%2s", " ")
						+ String.format("%-8s", "2S" + claimIDStr) + String.format("%05d", voucherLineNo) + "DOH01"
						+ "+" + String.format("%-26.3f", claimTotal) + String.format("%5s", " ")
						+ String.format("%10s", " ") + String.format("%05d", 0) + String.format("%03d", 0)
						+ String.format("%05d", 0) + String.format("%1s", " ") + String.format("%-10s", deptCode)
						+ String.format("%-5s", "10000") + String.format("%15s", " ") + String.format("%15s", " ")
						+ String.format("%5s", pgmCode) + String.format("%7s", budRef) + String.format("%-10s", "60301")
						+ String.format("%-10s", "11850") + String.format("%773s", " ");

				writer.newLine();
				writer.write(fifthRow);
				System.out.println("Fifth Row VOD written successfully.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return filePath;
	}

	private VohDetails getVohRecordDetails(int currClaimId) {
		String sql = "SELECT DISTINCT REPLACE(DATE_FORMAT(CURDATE(), '%m/%d/%Y'), '/', '') AS acctDate , \r\n"
				+ "c.InvoiceNo, \r\n" + "IFNULL(Rate,0) as claimTotal, \r\n" + "c.NDC, \r\n" + "a.SFSVendorId, \r\n"
				+ "REPLACE(DATE_FORMAT(f.SubmitDate, '%m/%d/%Y'), '/', '') AS date_received, \r\n"
				+ "REPLACE(DATE_FORMAT(c.dfilled, '%m/%d/%Y'), '/', '') AS dfilled, \r\n"
				+ "(case when mv.payee_type in ('B','C','G','M') then 'N' else 'Y' end) as interest_eligible, \r\n"
				+ "REPLACE(DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '%m/%d/%Y'), '/', '') AS paymentDate \r\n"
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
			vohDetails.setDateRecieved(date_received);
			vohDetails.setDfilled(dfilled);
			vohDetails.setInterestEligible(interest_eligible);
			vohDetails.setPaymentDate(paymentDate);

		}
		return vohDetails;
	}
}
