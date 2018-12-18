package com.barbri.ws;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.util.logging.Logger;
import java.util.*;

import com.barbri.util.AppSettings;
import com.barbri.util.ThreadPoolUtil;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.commons.lang3.StringUtils;
import org.omg.CORBA.Environment;

/**
 * Azure Functions with HTTP Trigger.
 */
public class LectureVideoDownloadFunction {

    private static final String FUNCTION_NAME = "lecture-video-download-fn";
    private static final String WINDOWS_SHARE_PATH_SEPARATOR = "\\";

    /**
     * This function listens at endpoint "/api/lecture-video-download-fn". How to invoke it using "curl" command in bash:
     * 1. curl "{your host}/api/HttpTrigger-Java?file={file name}&code={function key}"
     * Function Key is not needed when running locally, to invoke HttpTrigger deployed to Azure,
     * see here(https://docs.microsoft.com/en-us/azure/azure-functions/functions-bindings-http-webhook#authorization-keys) on how to get function key for your app.
     */
    @FunctionName(FUNCTION_NAME)
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request
            , final ExecutionContext context) {

        try {

            CloudBlockBlob fileBlob = retrieveBlob( request.getQueryParameters().get("file") );

            Optional<HTTPDownloadTask> task = buildDownloadTask(fileBlob, context.getLogger());
            task.ifPresent(HTTPDownloadTask::run);

        } catch (IllegalArgumentException e ) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND).body(e.getMessage()).build();

        } catch (Exception e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(e.getMessage()).build();

        }


        return request.createResponseBuilder(HttpStatus.OK).body("File download started").build();
    }

    //private helper
    private CloudBlockBlob retrieveBlob(String fileName) throws InvalidKeyException, StorageException, URISyntaxException {
        if ( StringUtils.isEmpty(fileName) ) {
            throw  new NullPointerException("Missing file name");
        }

        CloudBlobContainer containerRef = buildBlobContainerReference();

        CloudBlockBlob blockBlob = containerRef.getBlockBlobReference(fileName);
        if (!blockBlob.exists()) {
            throw new IllegalArgumentException (String.format("'%s' not found @ '%s'", fileName, containerRef.getStorageUri()));
        }

        return blockBlob;
    }

    private CloudBlobContainer buildBlobContainerReference() throws URISyntaxException, InvalidKeyException, StorageException {
        String blobStorageConnString = AppSettings.get("AZURE_VIDEO_STORAGE_CONNECTION_STRING");
        String containerName = AppSettings.get("AZURE_VIDEO_STORAGE_BLOB_CONTAINER");

        if ( StringUtils.isEmpty(blobStorageConnString) ) {
            throw  new NullPointerException("Missing blob store connection string app setting");
        }

        if ( StringUtils.isEmpty(containerName) ) {
            throw  new NullPointerException("Missing blob container app settings");
        }

        CloudStorageAccount storageAccount = CloudStorageAccount.parse(blobStorageConnString);
        CloudBlobClient client = storageAccount.createCloudBlobClient();

        return client.getContainerReference(containerName);
    }

    private Optional<HTTPDownloadTask> buildDownloadTask(CloudBlockBlob fileBlob, Logger logger) {
        HTTPDownloadTask task = null;

        String videoDownloadFolder  = System.getenv("VIDEO_DOWNLOAD_FOLDER");

        if ( !StringUtils.isEmpty(videoDownloadFolder) ) {


            String targetFile = videoDownloadFolder + fileBlob.getName();
            if (!videoDownloadFolder.endsWith(WINDOWS_SHARE_PATH_SEPARATOR) ) {
                targetFile = videoDownloadFolder +  WINDOWS_SHARE_PATH_SEPARATOR + fileBlob.getName();
            }

            Path path = Paths.get(targetFile);
            if (Files.exists(path)) {
                logger.info( String.format("Skipping download as '%s' exists", path ));
            } else {
                logger.info( String.format("Creating the download task for '%s'", fileBlob.getUri() ));
                task =  new HTTPDownloadTask(fileBlob, targetFile, logger);
            }
        } else {
            logger.info( "Skipping download as 'VIDEO_DOWNLOAD_FOLDER' setting is missing");
        }


        return Optional.ofNullable(task);
    }

}
