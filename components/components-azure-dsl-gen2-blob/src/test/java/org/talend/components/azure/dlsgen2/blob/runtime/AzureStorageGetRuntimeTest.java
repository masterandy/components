// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.azure.dlsgen2.blob.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;

import org.assertj.core.util.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.azure.dlsgen2.FileUtils;
import org.talend.components.azure.dlsgen2.RuntimeContainerMock;
import org.talend.components.azure.dlsgen2.blob.AzureDlsGen2BlobService;
import org.talend.components.azure.dlsgen2.blob.helpers.RemoteBlobsGetTable;
import org.talend.components.azure.dlsgen2.blob.tazurestorageget.TAzureDlsGen2GetProperties;
import org.talend.components.azure.dlsgen2.tazurestorageconnection.TAzureDlsGen2ConnectionProperties;
import org.talend.components.azure.dlsgen2.tazurestorageconnection.TAzureDlsGen2ConnectionProperties.Protocol;
import org.talend.daikon.i18n.GlobalI18N;
import org.talend.daikon.i18n.I18nMessages;
import org.talend.daikon.properties.ValidationResult;

public class AzureStorageGetRuntimeTest {

    public static final String PROP_ = "PROP_";

    private static final I18nMessages messages = GlobalI18N.getI18nMessageProvider()
            .getI18nMessages(AzureStorageGetRuntimeTest.class);

    private RuntimeContainer runtimeContainer;

    private TAzureDlsGen2GetProperties properties;

    private AzureDlsGen2GetRuntime storageGet;

    private File localFolder;

    @Mock
    private AzureDlsGen2BlobService blobService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setup() throws IOException {
        properties = new TAzureDlsGen2GetProperties(PROP_ + "Get");
        properties.setupProperties();
        // valid connection
        properties.connection = new TAzureDlsGen2ConnectionProperties(PROP_ + "Connection");
        properties.connection.protocol.setValue(Protocol.HTTP);
        properties.connection.accountName.setValue("fakeAccountName");
        properties.connection.accountKey.setValue("fakeAccountKey=ANBHFYRJJFHRIKKJFU");
        properties.container.setValue("goog-container-name-1");

        runtimeContainer = new RuntimeContainerMock();
        this.storageGet = new AzureDlsGen2GetRuntime();

        localFolder = FileUtils.createTempDirectory();
    }

    @After
    public void dispose() {
        localFolder.delete();
    }

    @Test
    public void testInitializeEmptyBlobs() {
        properties.remoteBlobsGet = new RemoteBlobsGetTable("RemoteBlobsGetTable");
        properties.remoteBlobsGet.prefix.setValue(new ArrayList<String>());
        ValidationResult validationResult = storageGet.initialize(runtimeContainer, properties);
        assertEquals(ValidationResult.Result.ERROR, validationResult.getStatus());
        assertEquals(messages.getMessage("error.EmptyBlobs"), validationResult.getMessage());
    }

    @Test
    public void testInitializeValidProperties() {
        properties.remoteBlobsGet = new RemoteBlobsGetTable("RemoteBlobsGetTable");
        properties.remoteBlobsGet.prefix.setValue(new ArrayList<String>());
        properties.remoteBlobsGet.prefix.getValue().add("");

        properties.localFolder.setValue(localFolder.getAbsolutePath());
        ValidationResult validationResult = storageGet.initialize(runtimeContainer, properties);
        assertNull(validationResult.getMessage());
        assertEquals(ValidationResult.OK.getStatus(), validationResult.getStatus());
    }

    @Test
    public void testRunAtDriverValid() {
        String localFolderPath = null;
        try {

            localFolderPath = FileUtils.createTempDirectory().getAbsolutePath();

            properties.remoteBlobsGet = new RemoteBlobsGetTable("RemoteBlobsGetTable");
            properties.remoteBlobsGet.include.setValue(Arrays.asList(true));
            properties.remoteBlobsGet.prefix.setValue(Arrays.asList("block1"));
            properties.remoteBlobsGet.create.setValue(Arrays.asList(false));
            properties.localFolder.setValue(localFolderPath);

            ValidationResult validationResult = storageGet.initialize(runtimeContainer, properties);
            assertEquals(ValidationResult.OK.getStatus(), validationResult.getStatus());

            final List<BlobItem> list = new ArrayList<>();
            final BlobItem b = new BlobItem();
            b.setName("blob-1");
            list.add(b);
            when(blobService.listBlobs(anyString(), anyString(), anyBoolean()))
                    .thenReturn(new PagedIterable<BlobItem>(new PagedFlux<BlobItem>(() -> {
                        return null;
                    })) {

                        @Override
                        public Iterator<BlobItem> iterator() {
                            return new DummyListBlobItemIterator(list);
                        }
                    });

            doAnswer(new Answer<Void>() {

                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    return null;
                }
            }).when(blobService).download(any(String.class), any(String.class), any(OutputStream.class));
            storageGet.azureDlsGen2BlobService = blobService;
            storageGet.runAtDriver(runtimeContainer);

        } catch (BlobStorageException | IOException e) {
            fail("should not throw " + e.getMessage());
        } finally {
            if (localFolderPath != null) {
                Files.delete(new File(localFolderPath));
            }
        }

    }

    @Test
    public void testRunAtDriverNotKeepRemoteDirStructure() {
        String localFolderPath = null;
        try {

            localFolderPath = FileUtils.createTempDirectory().getAbsolutePath();

            properties.keepRemoteDirStructure.setValue(false);
            properties.remoteBlobsGet = new RemoteBlobsGetTable("RemoteBlobsGetTable");
            properties.remoteBlobsGet.include.setValue(Arrays.asList(true));
            properties.remoteBlobsGet.prefix.setValue(Arrays.asList("someDir/"));
            properties.remoteBlobsGet.create.setValue(Arrays.asList(false));
            properties.localFolder.setValue(localFolderPath);

            ValidationResult validationResult = storageGet.initialize(runtimeContainer, properties);
            assertEquals(ValidationResult.OK.getStatus(), validationResult.getStatus());

            final List<BlobItem> list = new ArrayList<>();
            final BlobItem b = new BlobItem();
            b.setName("blob-1");
            list.add(b);
            when(blobService.listBlobs(anyString(), anyString(), anyBoolean()))
                    .thenReturn(new PagedIterable<BlobItem>(new PagedFlux<BlobItem>(() -> {
                        return null;
                    })) {

                        @Override
                        public Iterator<BlobItem> iterator() {
                            return new DummyListBlobItemIterator(list);
                        }
                    });

            doAnswer(new Answer<Void>() {

                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    return null;
                }
            }).when(blobService).download(any(String.class), any(String.class), any(OutputStream.class));
            storageGet.azureDlsGen2BlobService = blobService;
            storageGet.runAtDriver(runtimeContainer);


            Mockito.verify(blobService, Mockito.times(1)).download(any(), any(), any());
        } catch (BlobStorageException | IOException e) {
            fail("should not throw " + e.getMessage());
        } finally {
            if (localFolderPath != null) {
                Files.delete(new File(localFolderPath));
            }
        }

    }

}