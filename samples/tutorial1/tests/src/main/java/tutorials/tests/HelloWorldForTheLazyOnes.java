package tutorials.tests;

import org.ops4j.pax.drone.api.ConnectorConfiguration;
import org.ops4j.pax.drone.connector.paxrunner.ConnectorConfigurationFactory;
import org.ops4j.pax.drone.connector.paxrunner.Platforms;
import org.ops4j.pax.drone.spi.junit.DroneTestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Toni Menzel (tonit)
 * @since Jul 13, 2008
 */
public class HelloWorldForTheLazyOnes extends DroneTestCase {
   public static final Log logger = LogFactory.getLog(HelloWorldForTheLazyOnes.class);

    public ConnectorConfiguration configure() {
        return ConnectorConfigurationFactory.create(this)
                .setPlatform(Platforms.FELIX)
                .addBundle("mvn:org.ops4j.pax.logging/pax-logging-api")
                // watch out: here is the mvnlive handler
                .addBundle("mvnlive:org.ops4j.pax.drone/tutorial-helloworld");
    }

    public void testCheckFramework() {
        // do nothing. 
    }

   
}