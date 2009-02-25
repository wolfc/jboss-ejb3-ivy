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
package org.jboss.ejb3.ivy.plugins.version;

import java.util.regex.Pattern;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.version.AbstractVersionMatcher;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class MavenVersionMatcher extends AbstractVersionMatcher
{
   private final static Pattern VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+-\\d{8}\\.\\d{6}-\\d+");
   
   public MavenVersionMatcher()
   {
      super("maven-version-matcher");
   }
   
   @Override
   public boolean accept(ModuleRevisionId askedMrid, ModuleDescriptor foundMD)
   {
      System.err.println("NYI: accept md " + askedMrid + " : " + foundMD);
      return foundMD.getRevision().endsWith("-SNAPSHOT");
   }
   
   public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid)
   {
      //return askedMrid.getRevision().endsWith("-SNAPSHOT");
      //throw new RuntimeException("NYI: accept " + askedMrid + " : " + foundMrid);
      return true;
   }

   public boolean isDynamic(ModuleRevisionId askedMrid)
   {
      boolean isDynamic = VERSION_PATTERN.matcher(askedMrid.getRevision()).matches();
      System.err.println("isDynamic: " + askedMrid + " " + isDynamic);
      return isDynamic;
      //return askedMrid.getRevision().endsWith("-SNAPSHOT");
      //return true;
   }
}
