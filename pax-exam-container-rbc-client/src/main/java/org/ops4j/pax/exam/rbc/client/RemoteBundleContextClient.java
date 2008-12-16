/*
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
package org.ops4j.pax.exam.rbc.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.osgi.framework.BundleException;
import org.ops4j.pax.exam.rbc.internal.RemoteBundleContext;
import org.ops4j.pax.exam.spi.container.TestContainer;
import org.ops4j.pax.exam.spi.container.TestContainerException;

/**
 * TODO Add JavaDoc.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.3.0, December 15, 2008
 */
public class RemoteBundleContextClient
    implements TestContainer
{

    /**
     * RMI communication port.
     */
    private final Integer m_rmiPort;
    /**
     * Timeout for looking up the remote bundle context via RMI.
     */
    private final Integer m_rmiLookupTimeout;
    private RemoteBundleContext m_remoteBundleContext;

    /**
     * Constructor.
     *
     * @param rmiPort          RMI communication port (cannot be null)
     * @param rmiLookupTimeout timeout for looking up the remote bundle context via RMI (cannot be null)
     */
    public RemoteBundleContextClient( final Integer rmiPort,
                                      final Integer rmiLookupTimeout )
    {
        m_rmiPort = rmiPort;
        m_rmiLookupTimeout = rmiLookupTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public <T> T getService( final Class<T> serviceType )
    {
        return getService( serviceType, NO_WAIT );
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings( "unchecked" )
    public <T> T getService( final Class<T> serviceType,
                             final int timeoutInMillis )
    {
        return (T) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{ serviceType },
            new InvocationHandler()
            {
                /**
                 * {@inheritDoc}
                 * Delegates the call to remote bundle context.
                 */
                public Object invoke( final Object proxy,
                                      final Method method,
                                      final Object[] params )
                    throws Throwable
                {
                    try
                    {
                        return getRemoteBundleContext().remoteCall(
                            method.getDeclaringClass(),
                            method.getName(),
                            method.getParameterTypes(),
                            timeoutInMillis,
                            params
                        );
                    }
                    catch( InvocationTargetException e )
                    {
                        throw e.getCause();
                    }
                    catch( RemoteException e )
                    {
                        throw new TestContainerException( "Remote exception", e );
                    }
                    catch( Exception e )
                    {
                        throw new TestContainerException( "Invocation exception", e );
                    }
                }
            }
        );
    }

    /**
     * {@inheritDoc}
     */
    public long installBundle( final String bundleUrl )
    {
        try
        {
            return getRemoteBundleContext().installBundle( bundleUrl );
        }
        catch( RemoteException e )
        {
            throw new TestContainerException( "Remote exception", e );
        }
        catch( BundleException e )
        {
            throw new TestContainerException( "Bundle cannot be installed", e );
        }
    }

    /**
     * {@inheritDoc}
     */
    public long installBundle( final String bundleLocation,
                               final byte[] bundle )
        throws TestContainerException
    {
        try
        {
            return getRemoteBundleContext().installBundle( bundleLocation, bundle );
        }
        catch( RemoteException e )
        {
            throw new TestContainerException( "Remote exception", e );
        }
        catch( BundleException e )
        {
            throw new TestContainerException( "Bundle cannot be installed", e );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void startBundle( final long bundleId )
        throws TestContainerException
    {
        try
        {
            getRemoteBundleContext().startBundle( bundleId );
        }
        catch( RemoteException e )
        {
            throw new TestContainerException( "Remote exception", e );
        }
        catch( BundleException e )
        {
            throw new TestContainerException( "Bundle cannot be installed", e );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop()
    {
        // TODO will be better to stop the framework via this method
        throw new UnsupportedOperationException( "Stop via the remote bundle context is not supported");
    }

    private RemoteBundleContext getRemoteBundleContext()
    {
        if( m_remoteBundleContext == null )
        {
            long startedTrying = System.currentTimeMillis();
            //!! Absolutely necesary for RMI class loading to work
            // TODO maybe use ContextClassLoaderUtils.doWithClassLoader
            Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );
            Throwable reason = null;
            try
            {
                final Registry registry = LocateRegistry.getRegistry( m_rmiPort );
                do
                {
                    try
                    {
                        m_remoteBundleContext =
                            (RemoteBundleContext) registry.lookup( RemoteBundleContext.class.getName() );
                    }
                    catch( Exception e )
                    {
                        reason = e;
                    }
                }
                while( m_rmiLookupTimeout == 0 || System.currentTimeMillis() < startedTrying + m_rmiLookupTimeout );
            }
            catch( RemoteException e )
            {
                reason = e;
            }
            if( m_remoteBundleContext == null )
            {
                throw new TestContainerException( "Cannot get the remote bundle context", reason );
            }
        }
        return m_remoteBundleContext;
    }

    /**
     * Getter.
     *
     * @return rmi port
     */
    public Integer getRmiPort()
    {
        return m_rmiPort;
    }

}