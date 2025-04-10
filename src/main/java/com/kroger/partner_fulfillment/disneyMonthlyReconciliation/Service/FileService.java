package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.kroger.partner_fulfillment.disneyMonthlyReconciliation.DTO.CSVDataObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface FileService {

    public void writeFileToSFTPLocation(String epochTime);
    public void writeFileToSFTPLocalLocation(String epochTime);
    public void deleteFileAfterUse();
    public void copyFileToSFTPLocal(String filename) throws JSchException, IOException, SftpException;
    public void copyResponderFileToSFTPLocal(String filename) throws JSchException, IOException, SftpException;
    public Map<String, String > getListOfFiles();
    public Map<String, String > getListOfFilesInResponderDirectory();
    public List<CSVDataObject> processFile(String filename) throws IOException;
    public List<String> getFileStructure() throws Exception;
}
