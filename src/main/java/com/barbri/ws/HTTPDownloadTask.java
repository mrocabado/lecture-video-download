package com.barbri.ws;

import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Logger;

public class HTTPDownloadTask implements Runnable {


    private CloudBlockBlob fileBlob;
    private String targetFile;
    private Logger logger;

    public HTTPDownloadTask(CloudBlockBlob fileBlob, String targetFile, Logger logger) {

        this.fileBlob = fileBlob;
        this.targetFile = targetFile;
        this.logger = logger;
    }



    @Override
    public void run() {
        logger.info( String.format("Starting download for '%s'", fileBlob.getUri() ));
        try  {
            fileBlob.downloadToFile(targetFile);
            logger.info( String.format("Successful download to '%s'", targetFile ));
        } catch (Exception e) {
            logger.info( String.format("Failed download for '%s'. Message '%s", fileBlob.getUri(), e.getMessage() + "@" + e.getClass().getSimpleName() ));
        }

    }
}
