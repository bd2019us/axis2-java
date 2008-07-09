/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
package org.apache.axis2.osgi.deployment;

import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.modules.Module;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.DeploymentEngine;
import org.apache.axis2.deployment.ModuleBuilder;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisOperation;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.apache.axis2.osgi.deployment.OSGiAxis2Constants.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @see org.osgi.framework.BundleListener
 */
public class ModuleRegistry extends AbstractRegistry<AxisModule> {

    private static Log log = LogFactory.getLog(ModuleRegistry.class);

    private Registry serviceRegistry;

    public ModuleRegistry(BundleContext context, ConfigurationContext configCtx,
                          Registry serviceRegistry) {
        super(context, configCtx);
        this.serviceRegistry = serviceRegistry;
    }

    public void register(Bundle bundle) throws AxisFault {
        lock.lock();
        try {
            addModules(bundle);
            serviceRegistry.resolve();
        } finally {
            lock.unlock();
        }

    }

    public void unRegister(Bundle bundle) {
        lock.lock();
        try {
            List<Long> stopBundleList = new ArrayList<Long>();
            List<AxisModule> moduleList = resolvedBundles.get(bundle);
            for (AxisModule module : moduleList) {
                AxisConfiguration axisConfig = configCtx.getAxisConfiguration();
                for (Iterator iterator = axisConfig.getServiceGroups(); iterator.hasNext();) {
                    AxisServiceGroup axisServiceGroup = (AxisServiceGroup) iterator.next();
                    if (axisServiceGroup.isEngaged(module)) {
                        Long value = (Long) axisServiceGroup.getParameterValue(OSGi_BUNDLE_ID);
                        if (value != null) {
                            stopBundleList.add(value);
                        }
                    }
                }
                //
                HashMap serviceMap = axisConfig.getServices();
                Collection values = serviceMap.values();
                for (Object value1 : values) {
                    AxisService axisService = (AxisService) value1;
                    if (axisService.isEngaged(module)) {
                        Long value = (Long) axisService.getParameterValue(OSGi_BUNDLE_ID);
                        if (value != null && !stopBundleList.contains(value)) {
                            stopBundleList.add(value);
                        }
                    }
                    for (Iterator iterator1 = axisService.getOperations(); iterator1.hasNext();) {
                        AxisOperation axisOperation = (AxisOperation) iterator1.next();
                        if (axisOperation.isEngaged(module)) {
                            Long value = (Long) axisOperation.getParameterValue(OSGi_BUNDLE_ID);
                            if (value != null && !stopBundleList.contains(value)) {
                                stopBundleList.add(value);
                            }
                        }
                    }
                }
                axisConfig.removeModule(module.getName(), module.getVersion());
                if (resolvedBundles.containsKey(bundle)) {
                    resolvedBundles.remove(bundle);
                }
                log.info("[Axis2/OSGi] Stopping" + module.getName() + ":" +
                         module.getVersion() + " moduel in Bundle - " +
                         bundle.getSymbolicName());
            }
            for (Long bundleId : stopBundleList) {
                Bundle stopBundle = context.getBundle(bundleId);
                if (stopBundle != null) {
                    try {
                        serviceRegistry.unRegister(stopBundle);
                        stopBundle.stop();
                    } catch (BundleException e) {
                        String msg = "Error while stopping the bundle";
                        log.error(msg, e);
                    } catch (AxisFault e) {
                        String msg = "Erro while stopping the bundle";
                        log.error(msg, e);

                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * When the bundle is activated, this method will look for xml files that ends with "module.xml".
     * Thus, a given bundle can have n number of Axis2 modules with differen names suffixed with module.xml.
     * Ex: rampart_module.xml; rahas_module.xml addressingmodule.xml
     * <p/>
     * <p/>
     * If there are n number of *module.xml and out of which failed modules will be ignored and and all the
     * successful *module.xml files will use to crate the proper AxisModule. It is utmost important that
     * that if n number of *module.xml files are present, module should be give a proper name.
     *
     * @param bundle started bundle
     */
    private void addModules(Bundle bundle) {
        if (!resolvedBundles.containsKey(bundle)) {
            Enumeration enumeration = bundle.findEntries("META-INF", "*module.xml", false);
            List<AxisModule> moduleList = null;
            if (enumeration != null) {
                moduleList = new ArrayList<AxisModule>();
            }
            while (enumeration != null && enumeration.hasMoreElements()) {
                try {
                    URL url = (URL) enumeration.nextElement();
                    AxisModule axismodule = new AxisModule();
                    ClassLoader loader =
                            new BundleClassLoader(bundle, Registry.class.getClassLoader());
                    axismodule.setModuleClassLoader(loader);
                    ModuleBuilder builder =
                            new ModuleBuilder(url.openStream(), axismodule,
                                              configCtx.getAxisConfiguration());
                    Dictionary headers = bundle.getHeaders();
                    String bundleSymbolicName = (String) headers.get("Bundle-SymbolicName");
                    if (bundleSymbolicName != null && bundleSymbolicName.length() != 0) {
                        axismodule.setName(bundleSymbolicName);
                    }
//                                    axismodule.setVersion(org.apache.axis2.util.Utils.getModuleVersion(shortFileName));
                    builder.populateModule();
                    axismodule.setParent(configCtx.getAxisConfiguration());
                    //                axismodule.setFileName(new URL(bundle.getLocation()));
                    AxisModule module =
                            configCtx.getAxisConfiguration().getModule(axismodule.getName());
                    if (module == null) {
                        DeploymentEngine.addNewModule(axismodule, configCtx.getAxisConfiguration());
                        //initialze the module if the module contains Module interface.
                        Module moduleObj = axismodule.getModule();
                        if (moduleObj != null) {
                            moduleObj.init(configCtx, axismodule);
                        }
                        moduleList.add(axismodule);
                        log.info("[Axis2/OSGi] Starting any modules in Bundle - " +
                                 bundle.getSymbolicName());
                    } else {
                        log.info("[ModuleRegistry] Module : " + axismodule.getName() +
                                 " is already available.");
                    }
                } catch (IOException e) {
                    String msg = "Error while reading module.xml";
                    log.error(msg, e);
                }
            }
            if (moduleList != null && moduleList.size() > 0) {
                resolvedBundles.put(bundle, moduleList);
            }

        }

    }
}
