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

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.apache.tools.ant.Project;
import org.junit.Test;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class XPathTaskTestCase
{
   @Test
   public void test1() throws Exception
   {
      File file = new File(System.getProperty("user.home") + "/.m2/settings.xml");
      
      String expression = "/settings/servers/server[id='snapshots.jboss.org']/username";
      
      Project project = new Project();
      
      XPathTask task = new XPathTask();
      task.setFile(file);
      task.setExpression(expression);
      task.setProperty("test");
      task.setProject(project);
      task.execute();
      
      String value = project.getProperty("test");
      assertNotNull(value);
   }
}
