package com.example.utility;

import com.jcraft.jsch.*;

import org.bouncycastle.openpgp.PGPException;

import java.io.*;
import java.util.Vector;

public class Utility {

	///////////////////////////// Encrypt File and Upload File To SFTP server ///////////////////////////////
	
	public static void encryptAndUpload(String inputFilePath, String outputFilePath, String publicKeyPath, String passphrase, 
			String host, int port, String username, String password, String privateKeyPath, String remoteDirectory) throws IOException, PGPException, JSchException {
        // Encrypt the file
        PublicKeyEncryption.encryptFile(inputFilePath, outputFilePath, publicKeyPath, passphrase);

        // Upload the encrypted file to SFTP server
        try {
			uploadFile(outputFilePath, host, port, username, password, passphrase, privateKeyPath, remoteDirectory);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    // Upload file to SFTP server
    public static void uploadFile(String localFilePath, String host, int port, String username, String password, String passphrase, 
    		String privateKeyPath, String remoteDirectory) throws JSchException, SftpException, FileNotFoundException {
        JSch jsch = new JSch();
        jsch.addIdentity(privateKeyPath, passphrase);

        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        channelSftp.cd(remoteDirectory);

        channelSftp.put(new FileInputStream(localFilePath), new File(localFilePath).getName());

        channelSftp.disconnect();
        session.disconnect();

        System.out.println("File uploaded successfully.");
    }

    public static void moveFileToSFTP(String inputFilePath, String outputFilePath, String publicKeyPath, 
    		String passphrase, String host, int port, String username, String password, String privateKeyPath, String remoteDirectory) {
        try {
			encryptAndUpload(inputFilePath, outputFilePath, publicKeyPath, passphrase, host, port, username, password, privateKeyPath, remoteDirectory);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    /////////////////////////////// Download SFTP File And Decryopt ///////////////////////////////

	public static void downloadFilesFromSftpAndDecrypt(String downloadFilePath, String decryptFilePath, 
			String passphrase, String host, int port, String username, String password, String privateKeyPath, String remoteDirectory) {
		try {
			downloadFiles(host, port, username, password, passphrase, privateKeyPath, downloadFilePath, remoteDirectory);
			decryptFile(passphrase, privateKeyPath, downloadFilePath, decryptFilePath);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void decryptFile(String passphrase, String privateKeyPath, String downloadFilePath, String decryptFilePath) {
		
        // Decrypting files
        try {
			PublicKeyEncryption.decryptFiles(downloadFilePath, decryptFilePath, privateKeyPath, passphrase);
        } catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void downloadFiles(String host, int port, String username, String password, String passphrase,
			String privateKeyPath, String downloadFilePath, String remoteDirectory) {
		try {
			JSch jsch = new JSch();
			Session session = jsch.getSession(username, host, port);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
			channelSftp.connect();

			channelSftp.cd(remoteDirectory);

			@SuppressWarnings("unchecked")
			Vector<ChannelSftp.LsEntry> fileList = channelSftp.ls(".");
			for (ChannelSftp.LsEntry entry : fileList) {
				if (!entry.getAttrs().isDir()) {
					String remoteFileName = entry.getFilename();
					String localFilePath = downloadFilePath + File.separator + remoteFileName;
					channelSftp.get(remoteFileName, localFilePath);

					System.out.println("File downloaded: " + remoteFileName);
				}
			}

			channelSftp.disconnect();
			session.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("All files downloaded successfully.");
	}
}
