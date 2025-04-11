package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.gson.Gson;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.CSVDataObject;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.DataDogLoggingObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.jdbc.DataSourcePoolMetricsAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLOutput;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

@Service
public class FileServiceImpl implements FileService {

    @Value("${sftp.host}")
    private String sftpHost;

    @Value("${sftp.username}")
    private String sftpUsername;

    @Value("${sftp.keyPath}")
    private String sftpKeyPath;

    @Value("${sftp.remotePath}")
    private String sftpRemotePath;

    @Value("${domain.file.name}")
    private String sftpLocalPath;

    @Value("${sftp.localPath.reconciliation}")
    private String localPath;

    @Value("${sftp.remotePath.reconciliation}")
    private String remotePath;

    @Value("${sftp.remotePath.responder}")
    private String remotePathResponder;

    @Value("${sftp.username.local.reconciliation}")
    private String localUsername;

    @Value("${sftp.username.remote.reconciliation}")
    private String remoteUsername;

    @Value("${sftp.host.local.reconciliation}")
    private String localHost;

    @Value("${sftp.host.remote.reconciliation}")
    private String remoteHost;

    @Value("${sftp.key.local.reconciliation}")
    private String localKeyPath;

    @Value("${sftp.key.remote.reconciliation}")
    private String remoteKeyPath;

    @Value("${sftp.responder.host}")
    private String sftpResponderHost;

    @Value("${sftp.responder.username}")
    private String sftpResponderUsername;

    @Value("${sftp.backup.path}")
    private String backupPath;

    @Autowired
    private Environment environment;

    private static final Gson GSON = new Gson();


    @Override
    public void writeFileToSFTPLocation(String epochTime) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(sftpUsername, sftpHost, 22);
            jsch.addIdentity(sftpKeyPath);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            String path = sftpRemotePath.replace("AsOf", epochTime);
            sftpChannel.put(sftpLocalPath, path);
            sftpChannel.exit();
            session.disconnect();
            logResponse("File Written Successfully to Remote Location", null);
        } catch (JSchException | SftpException e) {
            logResponse("Error Writing File to Remote Location", getStackTraceAsString(e));
        }

    }

    @Override
    public void writeFileToSFTPLocalLocation(String epochTime) {
        if(!sftpHost.equalsIgnoreCase("10.32.10.252")) {
            try {
                JSch jsch = new JSch();
                Session session = jsch.getSession("ecadmin", "10.32.10.252", 22);
                jsch.addIdentity("Key/id_rsa");
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();
                Channel channel = session.openChannel("sftp");
                channel.connect();
                ChannelSftp sftpChannel = (ChannelSftp) channel;
                String remoteFilePath = backupPath;
                String path = remoteFilePath.replace("AsOf", epochTime);
                sftpChannel.put(sftpLocalPath, path);
                sftpChannel.exit();
                session.disconnect();
                logResponse("Backup File Written Successfully to ec sftp location", null);
            } catch (JSchException | SftpException e) {
                logResponse("Error Writing Backup File to ec sftp Location", getStackTraceAsString(e));
            }
        }
    }

    @Override
    public void copyFileToSFTPLocal(String fileName) throws JSchException, SftpException {

        JSch jschRead = new JSch();
        JSch jschWrite = new JSch();
        Session sessionRead = null;
        Session sessionWrite = null;
        System.out.println("Inside copy method");

        try {
            System.out.println("Attempting to connect to " + remoteHost + " with username " + remoteUsername + " and key path " + remoteKeyPath);
            sessionRead = jschRead.getSession(remoteUsername, remoteHost, 22);
            jschRead.addIdentity(remoteKeyPath);
            sessionRead.setConfig("StrictHostKeyChecking", "no");
            sessionRead.connect();

        } catch (Exception e) {
            System.out.println("Unable to connect to the remote sftp directory: " + e.getMessage());
        }

        try {
            System.out.println("Attempting to connect to " + localHost + " with username " + localUsername + " and key path " + localKeyPath);
            sessionWrite = jschWrite.getSession(localUsername, localHost, 22);
            jschWrite.addIdentity(localKeyPath);
            sessionWrite.setConfig("StrictHostKeyChecking", "no");
            sessionWrite.connect();
        } catch (Exception e) {
            System.out.println("Unable to connect to the local sftp directory: " + e.getMessage());
        }

        System.out.println("Connections Successful");

        if(sessionWrite != null && sessionRead != null) {
            ChannelSftp channelRead = null;
            ChannelSftp channelWrite = null;

            try {
                System.out.println("Attempting to open read channel");
                channelRead = (ChannelSftp)sessionRead.openChannel("sftp");
                channelRead.connect();

                System.out.println("Attempting to open write channel");
                channelWrite = (ChannelSftp)sessionWrite.openChannel("sftp");
                channelWrite.connect();

//                PipedInputStream pin = new PipedInputStream();
//                PipedOutputStream pout = new PipedOutputStream(pin);

                String remotePathWithFileName = remotePath.replace("{fileName}", fileName);
                String localPathWithFileName = localPath.replace("{fileName}", fileName);

                System.out.println("Writing from " + remotePathWithFileName + " to " + localPathWithFileName);
//                channelRead.get(remotePathWithFileName, pout);
                InputStream srcInputStream = channelRead.get(remotePathWithFileName);
                System.out.println("Reading complete!!!!");
//                channelWrite.put(pin, localPathWithFileName);
                channelWrite.put(srcInputStream, localPathWithFileName);
                System.out.println("Writing complete!!!!");
            } catch (Exception e) {
                System.out.println("Unknown Error: " + e.getMessage());
            } finally {
                if(channelRead != null) {
                    channelRead.disconnect();
                }
                if(channelWrite != null) {
                    channelWrite.disconnect();
                }
            }
        }

        sessionRead.disconnect();
        sessionWrite.disconnect();

    }

    @Override
    public void copyResponderFileToSFTPLocal(String fileName) throws JSchException, SftpException {

//         JSch jschRead = new JSch();
//         JSch jschWrite = new JSch();
//         Session sessionRead = null;
//         Session sessionWrite = null;
//         System.out.println("Inside copy method");

//         try {
//             System.out.println("Attempting to connect to " + remoteHost + " with username " + remoteUsername + " and key path " + remoteKeyPath);
//             sessionRead = jschRead.getSession(remoteUsername, remoteHost, 22);
//             jschRead.addIdentity(remoteKeyPath);
//             sessionRead.setConfig("StrictHostKeyChecking", "no");
//             sessionRead.connect();

//         } catch (Exception e) {
//             System.out.println("Unable to connect to the remote sftp directory: " + e.getMessage());
//         }

//         try {
//             System.out.println("Attempting to connect to " + localHost + " with username " + localUsername + " and key path " + localKeyPath);
//             sessionWrite = jschWrite.getSession(localUsername, localHost, 22);
//             jschWrite.addIdentity(localKeyPath);
//             sessionWrite.setConfig("StrictHostKeyChecking", "no");
//             sessionWrite.connect();
//         } catch (Exception e) {
//             System.out.println("Unable to connect to the local sftp directory: " + e.getMessage());
//         }

//         System.out.println("Connections Successful");

//         if(sessionWrite != null && sessionRead != null) {
//             ChannelSftp channelRead = null;
//             ChannelSftp channelWrite = null;

//             try {
//                 System.out.println("Attempting to open read channel");
//                 channelRead = (ChannelSftp)sessionRead.openChannel("sftp");
//                 channelRead.connect();

//                 System.out.println("Attempting to open write channel");
//                 channelWrite = (ChannelSftp)sessionWrite.openChannel("sftp");
//                 channelWrite.connect();

// //                PipedInputStream pin = new PipedInputStream();
// //                PipedOutputStream pout = new PipedOutputStream(pin);

//                 String remotePathWithFileName = remotePathResponder.replace("{fileName}", fileName);
//                 String localPathWithFileName = localPath.replace("{fileName}", fileName);

//                 System.out.println("Writing from " + remotePathWithFileName + " to " + localPathWithFileName);
// //                channelRead.get(remotePathWithFileName, pout);
//                 InputStream srcInputStream = channelRead.get(remotePathWithFileName);
//                 System.out.println("Reading complete!!!!");
// //                channelWrite.put(pin, localPathWithFileName);
//                 channelWrite.put(srcInputStream, localPathWithFileName);
//                 System.out.println("Writing complete!!!!");
//             } catch (Exception e) {
//                 System.out.println("Unknown Error: " + e.getMessage());
//             } finally {
//                 if(channelRead != null) {
//                     channelRead.disconnect();
//                 }
//                 if(channelWrite != null) {
//                     channelWrite.disconnect();
//                 }
//             }
//         }

//         sessionRead.disconnect();
//         sessionWrite.disconnect();

    }

    @Override
    public Map<String, String> getListOfFiles() {
        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        try {
            String path = remotePath.substring(0,  remotePath.lastIndexOf('/')+1);
            JSch jsch = new JSch();
            jsch.addIdentity(remoteKeyPath);
            session = jsch.getSession(remoteUsername, remoteHost, 22);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            System.out.println(path);
            channelSftp.cd(path);
            Vector filelist = channelSftp.ls(path);
            Map<String, String> files = new HashMap<>();
            for (int i = 0; i < filelist.size(); i++) {
                ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) filelist.get(i);
                files.put(entry.getFilename(), entry.getAttrs().getAtimeString());
            }
            return files;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (session != null) session.disconnect();
            if (channel != null) channel.disconnect();
        }
        return null;
    }

    @Override
    public Map<String, String> getListOfFilesInResponderDirectory() {
        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        try {
            String path = remotePathResponder.substring(0,  remotePathResponder.lastIndexOf('/')+1);
            JSch jsch = new JSch();
            jsch.addIdentity(remoteKeyPath);
            session = jsch.getSession(remoteUsername, remoteHost, 22);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            System.out.println(path);
            channelSftp.cd(path);
            Vector filelist = channelSftp.ls(path);
            Map<String, String> files = new HashMap<>();
            for (int i = 0; i < filelist.size(); i++) {
                ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) filelist.get(i);
                files.put(entry.getFilename(), entry.getAttrs().getAtimeString());
            }
            return files;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (session != null) session.disconnect();
            if (channel != null) channel.disconnect();
        }
        return null;
    }

    @Override
    public List<CSVDataObject> processFile(String filename) throws IOException {
        // Create SFTP into directory and retrieve file
        JSch jsch = new JSch();
        Session session = null;
        String sftpRemotePath = environment.getProperty("sftp.responder.remotePath");
        long startTime = System.currentTimeMillis();
        try {
            jsch.addIdentity(Paths.get(sftpKeyPath).toString());
            session = jsch.getSession(sftpResponderUsername, sftpResponderHost, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            LocalDate yesterday = LocalDate.now().minusDays(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String date = yesterday.format(formatter);
            sftpRemotePath = sftpRemotePath.replace("DATE", date);
            if(filename != null) {
                sftpRemotePath = sftpRemotePath.substring(0,sftpRemotePath.lastIndexOf("/")+1);
                sftpRemotePath = sftpRemotePath + filename;
            }
            sftpChannel.get(sftpRemotePath, sftpLocalPath);
            sftpChannel.exit();
            long elapsed = System.currentTimeMillis() - startTime;

            // Read file content and remove all instances of a character
            File file = new File(sftpLocalPath);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(file.toPath()));
                content = content.replaceAll("\u0000", ""); // Replace CHARACTER_TO_REMOVE with the actual character

                // Write the modified content back to the file
                Files.write(file.toPath(), content.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            }

            logSuccess("File Retrieval complete", startTime);
        } catch (JSchException e) {
            logError("Error During File Retrieval: ", e, startTime);
            return null;
        } catch (SftpException e) {
            logError("Error During File Retrieval: ", e, startTime);
            return null;
        } finally {
            session.disconnect();
        }

        //Process file into hashmap
        List<CSVDataObject> output = new LinkedList<>();
        try {
            CsvMapper mapper = new CsvMapper();
            CsvSchema schema = CsvSchema.builder()
                    .addColumn("promotionCode")
                    .addColumn("program")
                    .addColumn("redeemedAt")
                    .setReorderColumns(true)
                    .setUseHeader(true)
                    .build();
            File file = new File(sftpLocalPath);
            MappingIterator<CSVDataObject> iterator = mapper.reader(CSVDataObject.class)
                    .with(schema)
                    .readValues(file);
            while (iterator.hasNext()) {
                output.add(iterator.next());
            }
            logSuccess("File mapping complete: " + output.size() + " records ready for processing.", startTime);
        } catch (Exception e) {
            logError("Error During File Retrieval: ", e, startTime);
            return null;
        }
        //return map of file contents
        return output;
    }

    @Override
    public List<String> getFileStructure() throws Exception {
        Session session = null;
        Channel channel = null;
        List<String> output = new ArrayList<>();
        JSch jsch = new JSch();
        jsch.addIdentity(remoteKeyPath);
        session = jsch.getSession(remoteUsername, remoteHost, 22);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        channel = session.openChannel("sftp");
        channel.connect();
        ChannelSftp sftpChannel = (ChannelSftp) channel;
        listDirectory(sftpChannel, "/", output);
        return output;
    }

    public void deleteFileAfterUse() {
        logFileDeletionStatus(new File(sftpLocalPath).delete());
    }

    private void logFileDeletionStatus(boolean isDeleted) {
        String message = isDeleted ? "File Deleted Successfully" : "File NOT Deleted Successfully";
        System.out.println(GSON.toJson(new DataDogLoggingObject( message, null, null, null)));
    }

    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    public static void listDirectory(
            ChannelSftp channelSftp, String path, List<String> list) throws Exception
    {
        Vector<ChannelSftp.LsEntry> files = channelSftp.ls(path);
        for (ChannelSftp.LsEntry entry : files)
        {
            if (!entry.getAttrs().isDir())
            {
                list.add(path + "/" + entry.getFilename());
            }
            else
            {
                if (!entry.getFilename().equals(".") &&
                        !entry.getFilename().equals(".."))
                {
                    listDirectory(channelSftp, path + "/" + entry.getFilename(), list);
                }
            }
        }
    }

    private void logError(String message, Exception e, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        DataDogLoggingObject logObject = new DataDogLoggingObject(message + e.getMessage(), String.valueOf(elapsed), "500", 
            e.getCause() != null ? getStackTraceAsString(e.getCause()) : null);
        System.out.println(GSON.toJson(logObject));
    }

    private void logResponse(String message, String error) {
        DataDogLoggingObject logObject = new DataDogLoggingObject( message, null, null, error, null);
        System.out.println( GSON.toJson(logObject));
    }

    private void logSuccess(String message, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        DataDogLoggingObject logObject = new DataDogLoggingObject(message, String.valueOf(elapsed), "200", null);
        System.out.println(GSON.toJson(logObject));
    }
}
