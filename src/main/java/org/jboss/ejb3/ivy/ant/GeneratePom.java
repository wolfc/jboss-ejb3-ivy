/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ejb3.ivy.ant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.ivy.ant.IvyPostResolveTask;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorWriter;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorWriter.ConfigurationScopeMapping;
import org.apache.ivy.util.FileUtil;
import org.apache.tools.ant.BuildException;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class GeneratePom extends IvyPostResolveTask
{
   public static class Mapping
   {
      private String conf;

      private String scope;

      public String getConf()
      {
         return conf;
      }

      public void setConf(String conf)
      {
         this.conf = conf;
      }

      public String getScope()
      {
         return scope;
      }

      public void setScope(String scope)
      {
         this.scope = scope;
      }
   }
   
   private File headerFile;
   
   private Collection<Mapping> mappings = new ArrayList<Mapping>();
   
   private File pomFile;
   
   public Mapping createMapping()
   {
      Mapping mapping = new Mapping();
      this.mappings.add(mapping);
      return mapping;
   }
   
   /* (non-Javadoc)
    * @see org.apache.ivy.ant.IvyMakePom#doExecute()
    */
   @Override
   public void doExecute() throws BuildException
   {
      prepareAndCheck();
      
      // getResolveId() == null
      ModuleId mid = getResolvedModuleId();
      ModuleDescriptor md = (ModuleDescriptor) getResolvedDescriptor(mid.getOrganisation(), mid.getName());
      try
      {
         PomModuleDescriptorWriter.write(md,
               headerFile == null ? null : FileUtil.readEntirely(headerFile),
               mappings.isEmpty() 
                   ? PomModuleDescriptorWriter.DEFAULT_MAPPING
                   : new ConfigurationScopeMapping(getMappingsMap()), pomFile);
      }
      catch(IOException e)
      {
         throw new BuildException(e);
      }
   }
   
   protected Map<String, String> getMappingsMap() {
       Map<String, String> mappingsMap = new HashMap<String, String>();
       for (Iterator<Mapping> iter = mappings.iterator(); iter.hasNext();) {
           Mapping mapping = iter.next();
           mappingsMap.put(mapping.getConf(), mapping.getScope());
       }
       return mappingsMap;
   }
   
   public void setHeaderFile(File headerFile)
   {
      this.headerFile = headerFile;
   }
   
   public void setPomFile(File pomFile)
   {
      this.pomFile = pomFile;
   }
}
