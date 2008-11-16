package org.ops4j.pax.drone.samples.simple;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.ops4j.pax.exam.api.DroneConnector;
import static org.ops4j.pax.drone.connector.paxrunner.GenericConnector.*;
import org.ops4j.pax.drone.connector.paxrunner.Platforms;
import org.ops4j.pax.drone.spi.junit.MultiConnectorDroneTestCase;

/**
 * This test compares felix and equinox directly in terms of compatibilty with pax logging support.
 *
 * @author Toni Menzel (tonit)
 * @since Nov 11, 2008
 */
public class FelixVsEquinoxPaxLoggingTest extends MultiConnectorDroneTestCase
{

    protected DroneConnector[] configure()
    {
        return new DroneConnector[]{
            create( createBundleProvision()
                .addBundle( "mvn:org.ops4j.pax.logging/pax-logging-api" )
                .addBundle( "mvn:org.ops4j.pax.logging/pax-logging-service" )
            ).setPlatform( Platforms.FELIX ),
            create( createBundleProvision()
                .addBundle( "mvn:org.ops4j.pax.logging/pax-logging-api" )
                .addBundle( "mvn:org.ops4j.pax.logging/pax-logging-service" )
            ).setPlatform( Platforms.EQUINOX )
        };
    }

    public void testServiceExposed()
    {
        ServiceReference ref = bundleContext.getServiceReference( LogService.class.getName() );
        assertNotNull( "LogService should be exposed", ref );

    }

}
