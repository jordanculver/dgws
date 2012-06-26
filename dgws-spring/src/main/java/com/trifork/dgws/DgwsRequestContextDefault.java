package com.trifork.dgws;

import oasis.names.tc.saml._2_0.assertion.Attribute;
import oasis.names.tc.saml._2_0.assertion.AttributeStatement;
import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;
import org.apache.log4j.Logger;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.Security;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.SoapMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.springframework.util.CollectionUtils.findValueOfType;

public class DgwsRequestContextDefault implements DgwsRequestContext, EndpointInterceptor {
    private static Logger logger = Logger.getLogger(DgwsRequestContextDefault.class);
    ThreadLocal<Security> securityThreadLocal = new ThreadLocal<Security>();
    @Autowired
    Unmarshaller unmarshaller;

    public String getIdCardCpr() {
        final AttributeStatement userLog = CollectionUtils.find(securityThreadLocal.get().getAssertion().getAttributeStatement(), new Predicate<AttributeStatement>() {
            public boolean evaluate(AttributeStatement attributeStatement) {
                return attributeStatement.getId().equals("UserLog");
            }
        });
        final Attribute cprAttribute = CollectionUtils.find(userLog.getAttribute(), new Predicate<Attribute>() {
            public boolean evaluate(Attribute attribute) {
                return attribute.getName().equals("medcom:UserCivilRegistrationNumber");
            }
        });
        logger.debug("Found CPR=" + cprAttribute.getAttributeValue() + " in header");
        return cprAttribute.getAttributeValue();
    }

    public boolean handleRequest(MessageContext messageContext, Object o) throws Exception {
        if (messageContext.getRequest() instanceof SoapMessage) {
            List headerElements = unmarshalHeaderElements(((SoapMessage) messageContext.getRequest()).getSoapHeader());
            Security securityHeader = findValueOfType(headerElements, Security.class);
            securityThreadLocal.set(securityHeader);
        }
        return true;
    }

    public boolean handleResponse(MessageContext messageContext, Object o) throws Exception {
        return true;
    }

    public boolean handleFault(MessageContext messageContext, Object o) throws Exception {
        return true;
    }

    public void afterCompletion(MessageContext messageContext, Object o, Exception e) throws Exception {
        securityThreadLocal.remove();
    }

    private List unmarshalHeaderElements(SoapHeader soapHeader) throws Exception {
        List result = new ArrayList();
        final Iterator<SoapHeaderElement> it = soapHeader.examineAllHeaderElements();
        while (it.hasNext()) {
            SoapHeaderElement e = it.next();
            result.add(unmarshaller.unmarshal(e.getSource()));
        }
        return result;
    }

}