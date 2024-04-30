package com.example.utility;

import com.jcraft.jsch.*;

import org.bouncycastle.openpgp.PGPException;

import java.io.*;

public class Utility {

	public static void encryptAndUpload(String inputFilePath, String outputFilePath, String publicKeyPath, String passphrase, String host, int port, String username, String privateKeyPath, String remoteDirectory) throws IOException, PGPException, JSchException {
        // Encrypt the file
        PublicKeyEncryption.encryptFile(inputFilePath, outputFilePath, publicKeyPath, passphrase);

        // Upload the encrypted file to SFTP server
        try {
			uploadFile(outputFilePath, host, port, username, privateKeyPath, remoteDirectory);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    // Upload file to SFTP server
    public static void uploadFile(String localFilePath, String host, int port, String username, String privateKeyPath, String remoteDirectory) throws JSchException, SftpException, FileNotFoundException {
        JSch jsch = new JSch();
        jsch.addIdentity(privateKeyPath);

        Session session = jsch.getSession(username, host, port);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        // Change directory to the remote directory
        channelSftp.cd(remoteDirectory);

        // Upload the file
        channelSftp.put(new FileInputStream(localFilePath), new File(localFilePath).getName());

        // Disconnect from the server
        channelSftp.disconnect();
        session.disconnect();

        System.out.println("File uploaded successfully.");
    }

    public static void moveFileToSFTP(String inputFilePath, String outputFilePath, String publicKeyPath, 
    		String passphrase, String host, int port, String username, String privateKeyPath, String remoteDirectory) {

        try {
			encryptAndUpload(inputFilePath, outputFilePath, publicKeyPath, passphrase, host, port, username, privateKeyPath, remoteDirectory);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}
