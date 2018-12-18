package com.barbri.ws;

import com.barbri.util.AppSettings;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import org.mockito.internal.util.MockUtil;


/**
 * Unit test for Function class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {AppSettings.class, LectureVideoDownloadFunction.class} )
public class LectureVideoDownloadFunctionTest {


    @Test
    public void whenBlobDoesNotExists_NotFoundIsReturned() throws Exception {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("file", "test_lecture");
        doReturn(queryParams).when(req).getQueryParameters();

        final Optional<String> queryBody = Optional.empty();
        doReturn(queryBody).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        mockStatic(AppSettings.class);
        Mockito.when(AppSettings.get( "AZURE_VIDEO_STORAGE_CONNECTION_STRING") ).thenReturn("test_conn_string");
        Mockito.when(AppSettings.get( "AZURE_VIDEO_STORAGE_BLOB_CONTAINER") ).thenReturn("mediabackup");


        final CloudBlockBlob nonExistingBlob = mock(CloudBlockBlob.class);
        doReturn(false).when(nonExistingBlob).exists();


        final CloudBlobContainer containerRef = mock(CloudBlobContainer.class);
        doReturn(nonExistingBlob).when(containerRef).getBlockBlobReference("test_lecture");


        final LectureVideoDownloadFunction azureFunction = PowerMockito.spy(new LectureVideoDownloadFunction());
        PowerMockito.doReturn(containerRef).when(azureFunction, "buildBlobContainerReference");

        // Invoke
        final HttpResponseMessage ret = azureFunction.run(req, context);

        // Verify
        assertEquals(ret.getStatus(), HttpStatus.NOT_FOUND);
    }


    @Test
    public void whenBlobDoesExists_OkIsReturned() throws Exception {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("file", "test_lecture");
        doReturn(queryParams).when(req).getQueryParameters();

        final Optional<String> queryBody = Optional.empty();
        doReturn(queryBody).when(req).getBody();

        doAnswer(new Answer<HttpResponseMessage.Builder>() {
            @Override
            public HttpResponseMessage.Builder answer(InvocationOnMock invocation) {
                HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
            }
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        mockStatic(AppSettings.class);
        Mockito.when(AppSettings.get( "AZURE_VIDEO_STORAGE_CONNECTION_STRING") ).thenReturn("test_conn_string");
        Mockito.when(AppSettings.get( "AZURE_VIDEO_STORAGE_BLOB_CONTAINER") ).thenReturn("mediabackup");


        final CloudBlockBlob existingBlob = mock(CloudBlockBlob.class);
        doReturn(true).when(existingBlob).exists();


        final CloudBlobContainer containerRef = mock(CloudBlobContainer.class);
        doReturn(existingBlob).when(containerRef).getBlockBlobReference("test_lecture");


        final LectureVideoDownloadFunction azureFunction = PowerMockito.spy(new LectureVideoDownloadFunction());
        PowerMockito.doReturn(containerRef).when(azureFunction, "buildBlobContainerReference");
        PowerMockito.doReturn(Optional.empty()).when(azureFunction, "buildDownloadTask", existingBlob, Logger.getGlobal());

        // Invoke
        final HttpResponseMessage ret = azureFunction.run(req, context);

        // Verify
        PowerMockito.verifyPrivate(azureFunction, times(1)).invoke(existingBlob, Logger.getGlobal());
        assertEquals(ret.getStatus(), HttpStatus.OK);
    }
}
