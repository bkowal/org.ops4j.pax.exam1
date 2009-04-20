/*
 * Copyright 2009 Toni Menzel.
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
package org.ops4j.pax.tinybundles.core.targets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import org.ops4j.io.StreamUtils;
import org.ops4j.pax.tinybundles.core.BundleAs;

/**
 * @author Toni Menzel (tonit)
 * @since Apr 9, 2009
 */
public class BundleAsURLImpl implements BundleAs<URL>
{

    public URL make( InputStream inp )
    {
        try
        {
            // TODO use url handler instead
            File fout = File.createTempFile( "tinybundle_", ".jar" );
            fout.deleteOnExit();
            StreamUtils.copyStream( inp, new FileOutputStream( fout ), true );
            return fout.toURI().toURL();
        }
        catch( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
}
