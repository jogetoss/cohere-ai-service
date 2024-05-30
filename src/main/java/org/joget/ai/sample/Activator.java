package org.joget.ai.sample;

import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    public final static String AI_MESSAGE_PATH = "message/ai/aiAppGenerator" ;
    public final static String VERSION = "8.0.0";
    
    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        this.registrationList.add(context.registerService(SampleCohereGeneratorService.class.getName(), new SampleCohereGeneratorService(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}