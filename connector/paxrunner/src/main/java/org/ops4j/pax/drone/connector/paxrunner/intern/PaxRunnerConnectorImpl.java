/*
 * Copyright 2008 Toni Menzel.
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.drone.connector.paxrunner.intern;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.exam.api.DroneProvider;
import org.ops4j.pax.exam.api.DroneSummary;
import org.ops4j.pax.exam.api.*;
import org.ops4j.pax.exam.api.RunnerContext;
import org.ops4j.pax.drone.connector.paxrunner.PaxRunnerConnector;
import org.ops4j.pax.drone.connector.paxrunner.Platforms;
import org.ops4j.pax.drone.connector.paxrunner.SubProcess;
import org.ops4j.pax.drone.spi.SummaryImpl;
import org.ops4j.pax.drone.zombie.RemoteDroneClient;

/**
 * Construct a PaxRunner setup in PaxDrone.
 *
 * @author Toni Menzel
 * @author Alin Dreghiciu
 * @since 0.1.0, June 17, 2008
 */
public class PaxRunnerConnectorImpl
    implements PaxRunnerConnector
{

    private static final Log LOG = LogFactory.getLog( PaxRunnerConnectorImpl.class );

    /**
     * The context (configuration & information) for this connector.
     */
    private RunnerContext m_context;

    /**
     * Pax Runner OSGi Framework to load. (felix, equinnox, knopflerfish or concierge)
     */
    private Platforms m_selectedPlatform;

    /**
     * Allows to set the boot delegation packages property.
     * Will be delegated 1:1 to pax runners equivalent option.
     */
    private String m_bootDelegationPkgs;

    /**
     * Allows to set the system packages property.
     * Will be delegated 1:1 to pax runners equivalent option.
     */
    private String m_systemPkgs;

    /**
     * Allows to set the virtual machine options.
     * Will be delegated 1:1 to pax runners equivalent option + drone related options.
     */
    private StringBuilder m_vmOptions;

    /**
     * Allows setting of raw pax runner command line options.
     * Will be delegated as is to pax runner.
     */
    private List<String> m_rawOptions;

    /**
     * Framework version to be used.
     */
    private String m_selectedPlatformVersion;

    private BundleProvision m_provision;

    public PaxRunnerConnectorImpl( RunnerContext context, BundleProvision bundleProvision )
    {
        NullArgumentException.validateNotNull( context, "context" );
        NullArgumentException.validateNotNull( bundleProvision, "bundleProvision" );

        m_context = context;
        m_provision = bundleProvision;
    }

    /**
     * {@inheritDoc}
     */
    public PaxRunnerConnector setPlatform( Platforms platform )
    {
        NullArgumentException.validateNotNull( platform, "platform" );
        m_selectedPlatform = platform;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public PaxRunnerConnector setVersion( final String version )
    {
        NullArgumentException.validateNotNull( version, "version" );
        m_selectedPlatformVersion = version;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public PaxRunnerConnector addBootDelegationFor( String pkg )
    {
        NullArgumentException.validateNotEmpty( pkg, true, "package" );
        if( m_bootDelegationPkgs == null )
        {
            m_bootDelegationPkgs = "";
        }
        else
        {
            m_bootDelegationPkgs += ",";
        }
        m_bootDelegationPkgs += pkg;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public PaxRunnerConnector addSystemPackage( String pkg )
    {
        NullArgumentException.validateNotEmpty( pkg, true, "package" );
        if( m_systemPkgs == null )
        {
            m_systemPkgs = "";
        }
        else
        {
            m_systemPkgs += ",";
        }
        m_systemPkgs += pkg;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public PaxRunnerConnector addVMOption( final String option )
    {
        NullArgumentException.validateNotEmpty( option, true, "virtual machine option" );
        if( m_vmOptions == null )
        {
            m_vmOptions = new StringBuilder();
        }
        m_vmOptions.append( " " ).append( option );
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public PaxRunnerConnector addRawOptions( final String options )
    {
        NullArgumentException.validateNotEmpty( options, true, "raw options" );
        // TODO maybe validate that the raw option is not one of the ones that can be set via api
        if( m_rawOptions == null )
        {
            m_rawOptions = new ArrayList<String>();
        }
        m_rawOptions.add( options );
        return this;
    }

    /**
     * Constructs options for PaxRunner out of *this*.
     * Everything you *add* to this instance will (probably) end up in those options.
     *
     * @return a PaxRunner compatible option list
     */
    private String[] calculateOptions()
    {
        NullArgumentException.validateNotNull( m_selectedPlatform, "m_selectedPlatform" );

        List<String> full = new ArrayList<String>();
        // Add dependencies from paxdrone itself:
        String version = Info.getPaxDroneVersion();
        // if drone is in snapshot version we should always use the latest. Otherwise we can
        // keep it without update so that subsequent dronecalls are much faster.
        if (version != null && version.endsWith( "SNAPSHOT" )) {
            version += "@update";
        }
        full.add( "scan-bundle:mvn:org.ops4j.pax.exam/pax-exam-zombie/" + version );
        full.add( "scan-bundle:mvn:org.ops4j.pax.exam/pax-exam-runtime/" + version );

        // Add custom dependency bundles:
        String[] bundles = m_provision.getBundles();
        for( String bundle : bundles )
        {
            full.add( bundle );
        }
        

        full.add( "--platform=" + m_selectedPlatform.toString().toLowerCase() );
        if( m_selectedPlatformVersion != null )
        {
            full.add( "--version=" + m_selectedPlatformVersion );
        }
        full.add( "--noConsole" );
        full.add( "--log=debug" );

        final StringBuilder vmOptions = new StringBuilder( "--vmOptions=" );
        vmOptions.append( "-Ddrone.communication.port=" ).append( m_context.getCommunicationPort() ).append( " " );
        if( m_vmOptions != null && m_vmOptions.length() > 0 )
        {
            vmOptions.append( m_vmOptions );
        }
        full.add( vmOptions.toString() );

        full.add( "--workingDirectory=" + m_context.getWorkingDirectory().getAbsolutePath() );

        if( m_bootDelegationPkgs != null )
        {
            full.add( "--bootDelegation=" + m_bootDelegationPkgs );
        }

        if( m_systemPkgs != null )
        {
            full.add( "--systemPackages=" + m_systemPkgs );
        }

        if( m_rawOptions != null && m_rawOptions.size() > 0 )
        {
            full.addAll( m_rawOptions );
        }
        full.add( "--noDownloadFeedback" );
        full.add( "--noArgs" );
        return full.toArray( new String[full.size()] );
    }

    public void execute( SubProcess instance, RemoteDroneClient client, PrintStream out, DroneProvider provider )
    {
        NullArgumentException.validateNotNull( instance, "process" );
        NullArgumentException.validateNotNull( client, "remote drone client handle" );
        NullArgumentException.validateNotNull( provider, "provider" );

        try
        {
            // start the instance
            LOG.info( "LOADING paxrunner.." );
            instance.startup();
            LOG.info( "BUILDING and INSTALLING drone bundle.." );
            client.install( provider.build() );
            // use remoting client to call the tests

            LOG.info( "EXECUTING DRONE NOW" );
            out.println( client.execute() );
        }
        finally
        {
            if( instance != null )
            {
                LOG.info( "PAX DRONE HAS FINISHED ITS JOB." );

                instance.shutdown();
            }
        }
    }

    public DroneSummary execute( DroneProvider provider )
    {
        Info.showLogo();
        DroneSummary summary = new SummaryImpl();

        String[] options = calculateOptions();
        File workingDirectory = m_context.getWorkingDirectory();
        System.out.println( "Using working directory: " + workingDirectory );
        int port = m_context.getCommunicationPort();
        try
        {
            SubProcess instance = new PaxRunnerInstanceImpl( options, workingDirectory, port );
            RemoteDroneClient c = new RemoteDroneClient( port );

            execute( instance, c, System.out, provider );
        }
        catch( Exception e )
        {
            summary.setException( e );
        }

        return summary;
    }

}
