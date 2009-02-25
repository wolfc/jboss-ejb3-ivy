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
package org.jboss.ejb3.ivy.plugins.resolver;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.regex.Pattern;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;
import org.junit.Test;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class MavenSnapshotResolverTestCase
{
   @Test
   public void test0()
   {
      Pattern VERSION_PATTERN = Pattern.compile("\\d{6}");
      System.err.println(VERSION_PATTERN);
      assertTrue(VERSION_PATTERN.matcher("123456").matches());
//      Pattern VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+-\\d{6}");
//      System.err.println(VERSION_PATTERN);
//      assertTrue(VERSION_PATTERN.matcher("1.0.0-20090202").matches());
//      Pattern VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+-\\d{6}+\\.\\d{6}+-\\d+");
//      System.err.println(VERSION_PATTERN);
//      assertTrue(VERSION_PATTERN.matcher("1.0.0-20090202-101010-10").matches());
   }
   
   @Test
   public void test1() throws ParseException, IOException
   {
      Ivy ivy = Ivy.newInstance();
      ivy.configure(new File("src/test/resources/ivysettings.xml"));
      ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_DEBUG));
      
      ModuleRevisionId mrid = new ModuleRevisionId(new ModuleId("org.jboss.ejb3", "jboss-ejb3-cache"), "latest.integration");
      ResolveOptions options = new ResolveOptions();
      options.setTransitive(false);
      String confs[] = { "default" };
      options.setConfs(confs);
      ResolveReport report = ivy.resolve(mrid, options, true);
      System.out.println(report.getAllArtifactsReports()[0].getArtifact().getModuleRevisionId().getRevision());
   }
}
