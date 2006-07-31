/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * Copyright 2006 International Business Machines Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.axis2.jaxws.description;

import java.net.URL;

import javax.xml.namespace.QName;

/**
 * Creates the JAX-WS metadata descritpion hierachy from some combinations of
 * WSDL, Java classes with annotations, and (in the future) deployment descriptors.
 */
public class DescriptionFactory {
    /**
     * A DescrptionFactory can not be instantiated; all methods are static.
     */
    private DescriptionFactory() {
    }
    
    public static ServiceDescription createServiceDescription(URL wsdlURL, QName serviceQName, Class serviceClass) {
        return new ServiceDescription(wsdlURL, serviceQName, serviceClass);
    }
    
    public static ServiceDescription updateEndpointInterface(ServiceDescription serviceDescription, Class seiClass) {
        // TODO: Implement this method
        return serviceDescription;
    }

}
