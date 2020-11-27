//==============================================================================
//
// Copyright (C) 2006-2020 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
//==============================================================================

package org.talend.components.azure.dlsgen2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class AzureDlsGen2ConnectionWithTokenTest {


    @Test
    public void testCreateConnectionWithToken() throws Exception {
        String testAccountName = "someAccountName";
        AzureDlsGen2ConnectionWithToken instance = AzureDlsGen2ConnectionWithToken.builder()
                .withAccountName(testAccountName)
                .withTenantId("tenantId")
                .withClientId("clientId")
                .withClientSecret("clientSecret")
                .build();
        assertNotNull(instance);
        assertNotNull(instance.getClientSecretCredential());
        assertEquals(testAccountName, instance.getAccountName());
        assertNotNull(instance.getBlobServiceClient());
    }
}