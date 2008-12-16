/*
 * Copyright 2008 Toni Menzel
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.exam.rbc.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * @author Toni Menzel (tonit)
 * @since Jun 10, 2008
 */
public class RemoteBundleContextImpl
    implements RemoteBundleContext, Serializable
{

    /**
     * JCL Logger.
     */
    private static final Log LOG = LogFactory.getLog( RemoteBundleContextImpl.class );

    private transient BundleContext m_bundleContext;

    /**
     * Constructor.
     *
     * @param bundleContext bundle context
     */
    public RemoteBundleContextImpl( final BundleContext bundleContext )
    {
        m_bundleContext = bundleContext;
    }

    /**
     * {@inheritDoc}
     */
    public Object remoteCall( final Class<?> serviceType,
                              final String methodName,
                              final Class<?>[] methodParams,
                              final int timeoutInMillis,
                              final Object... actualParams )
        throws NoSuchServiceException, NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        LOG.info( "Remote call of [" + serviceType.getName() + "." + methodName + "]" );
        return serviceType.getMethod( methodName, methodParams ).invoke(
            getService( serviceType, timeoutInMillis ),
            actualParams
        );
    }

    /**
     * {@inheritDoc}
     */
    public long installBundle( final String bundleUrl )
        throws BundleException
    {
        LOG.info( "Install bundle from URL [" + bundleUrl + "]" );
        return m_bundleContext.installBundle( bundleUrl ).getBundleId();
    }

    /**
     * {@inheritDoc}
     */
    public long installBundle( final String bundleLocation,
                               final byte[] bundle )
        throws BundleException
    {
        LOG.info( "Install bundle [" + bundleLocation + "] from byte array" );
        final ByteArrayInputStream inp = new ByteArrayInputStream( bundle );
        try
        {
            return m_bundleContext.installBundle( bundleLocation, inp ).getBundleId();
        }
        finally
        {
            try
            {
                inp.close();
            }
            catch( IOException e )
            {
                // ignore.
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void startBundle( long bundleId )
        throws BundleException
    {
        startBundle( m_bundleContext.getBundle( bundleId ) );
    }

    /**
     * Lookup a service in the service registry.
     *
     * @param serviceType     service class
     * @param timeoutInMillis number of milliseconds to wait for service before failing
     *
     * @return a service published under the required service type
     *
     * @throws NoSuchServiceException - If service cannot be found in the service registry
     */
    private Object getService( final Class<?> serviceType,
                               final int timeoutInMillis )
        throws NoSuchServiceException
    {
        LOG.info( "Look up service [" + serviceType.getName() + "], timeout in " + timeoutInMillis + " millis" );
        final ServiceReference ref = m_bundleContext.getServiceReference( serviceType.getName() );
        if( ref != null )
        {
            // TODO shall we check also that the service cannot be found by the calls bellow?
            return m_bundleContext.getService( ref );
        }
        else
        {
            throw new NoSuchServiceException( serviceType );
        }
    }

    // TODO obsolete?
    private void startAllBundles()
        throws BundleException
    {
        // first make sure all installed bundles are up
        Bundle[] bundles = m_bundleContext.getBundles();
        for( Bundle bundle : bundles )
        {
            startBundle( bundle );
        }
    }

    // TODO Add JavaDoc
    private void startBundle( final Bundle bundle )
        throws BundleException
    {
        // Don't start if bundle already active
        int bundleState = bundle.getState();
        if( bundleState == Bundle.ACTIVE )
        {
            return;
        }

        // Don't start if bundle is a fragment bundle
        Dictionary bundleHeaders = bundle.getHeaders();
        if( bundleHeaders.get( Constants.FRAGMENT_HOST ) != null )
        {
            return;
        }

        // Start bundle
        bundle.start();

        bundleState = bundle.getState();
        if( bundleState != Bundle.ACTIVE )
        {
            long bundleId = bundle.getBundleId();
            String bundleName = bundle.getSymbolicName();
            String bundleStateStr = bundleStateToString( bundleState );
            throw new BundleException(
                "Bundle (" + bundleId + ", " + bundleName + ") not started (still " + bundleStateStr + ")"
            );
        }
    }

    private String bundleStateToString( int bundleState )
    {
        switch( bundleState )
        {
            case Bundle.ACTIVE:
                return "active";
            case Bundle.INSTALLED:
                return "installed";
            case Bundle.RESOLVED:
                return "resolved";
            case Bundle.STARTING:
                return "starting";
            case Bundle.STOPPING:
                return "stopping";
            case Bundle.UNINSTALLED:
                return "uninstalled";
            default:
                return "unknown (" + bundleState + ")";
        }
    }


}