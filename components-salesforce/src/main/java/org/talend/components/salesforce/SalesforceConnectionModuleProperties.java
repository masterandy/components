// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.salesforce;

import org.apache.avro.Schema;
import org.talend.components.api.component.Connector;
import org.talend.components.api.component.PropertyPathConnector;
import org.talend.components.api.properties.ComponentPropertyFactory;
import org.talend.components.common.FixedConnectorsComponentProperties;
import org.talend.daikon.properties.Property;
import org.talend.daikon.properties.presentation.Form;

/**
 * Properties common to input and output Salesforce components.
 */
public abstract class SalesforceConnectionModuleProperties extends FixedConnectorsComponentProperties implements
        SalesforceProvideConnectionProperties {

    // Collections
    //
    public static final String NB_LINE = "NB_LINE";

    public SalesforceConnectionProperties connection = new SalesforceConnectionProperties("connection"); //$NON-NLS-1$

    public SalesforceModuleProperties module;

    protected transient PropertyPathConnector MAIN_CONNECTOR = new PropertyPathConnector(Connector.MAIN_NAME, "module.main");

    public SalesforceConnectionModuleProperties(String name) {
        super(name);
    }

    @Override
    public void setupProperties() {
        super.setupProperties();
        returns = ComponentPropertyFactory.newReturnsProperty();
        ComponentPropertyFactory.newReturnProperty(returns, Property.Type.STRING, "ERROR_MESSAGE"); //$NON-NLS-1$ 
        ComponentPropertyFactory.newReturnProperty(returns, Property.Type.INT, NB_LINE);
        // Allow for subclassing
        module = new SalesforceModuleProperties("module");
        module.connection = connection;
    }

    public Schema getSchema() {
        return (Schema) module.main.schema.getValue();
    }

    @Override
    public void setupLayout() {
        super.setupLayout();
        Form mainForm = Form.createFromAndRegisterProperties(this, Form.MAIN);
        mainForm.addRow(connection.getForm(Form.REFERENCE));
        mainForm.addRow(module.getForm(Form.REFERENCE));

        Form advancedForm = Form.createFromAndRegisterProperties(this, Form.ADVANCED);
        advancedForm.addRow(connection.getForm(Form.ADVANCED));
    }

    @Override
    public SalesforceConnectionProperties getConnectionProperties() {
        return connection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.daikon.properties.Properties#refreshLayout(org.talend.daikon.properties.presentation.Form)
     */
    @Override
    public void refreshLayout(Form form) {
        super.refreshLayout(form);
        for (Form childForm: connection.getForms()) {
            connection.refreshLayout(childForm);
        }
    }

}
