package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.utils;

import com.jcraft.jsch.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.InputStream;


@Component
public class SFTPClient {
    @Value("${sftp.host}")
    private String sftpHost;

    @Value("${sftp.username}")
    private String sftpUsername;

    @Value("${sftp.keyPath}")
    private String sftpKeyPath;

    public ChannelSftp setupSftpConnection() throws JSchException {
        JSch jsch = new JSch();
        jsch.addIdentity(sftpKeyPath);
        Session session = jsch.getSession(sftpUsername, sftpHost, 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        Channel channel = session.openChannel("sftp");
        channel.connect();
        return (ChannelSftp) channel;
    }

    public void uploadFile(String localFilePath, String remoteFilePath) throws JSchException, SftpException {
        ChannelSftp sftpChannel = setupSftpConnection();
        try {
            sftpChannel.put(localFilePath, remoteFilePath);
        } finally {
            sftpChannel.exit();
            sftpChannel.getSession().disconnect();
        }
    }

    public InputStream downloadFile(String remoteFilePath) throws JSchException, SftpException {
        ChannelSftp sftpChannel = setupSftpConnection();
        return sftpChannel.get(remoteFilePath);
    }
}
