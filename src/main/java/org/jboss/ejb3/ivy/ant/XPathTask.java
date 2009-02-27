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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.xml.sax.InputSource;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class XPathTask extends Task
{
   private String expression;
   private File file;
   private String property;
   
   @Override
   public void execute() throws BuildException
   {
      XPath xpath = XPathFactory.newInstance().newXPath();
      try
      {
         BufferedReader reader = new BufferedReader(new FileReader(file));
         try
         {
            InputSource source = new InputSource(reader);
            String value = xpath.evaluate(expression, source);
            getProject().setNewProperty(property, value);
         }
         finally
         {
            reader.close();
         }
      }
      catch(IOException e)
      {
         throw new BuildException(e);
      }
      catch (XPathExpressionException e)
      {
         throw new BuildException(e);
      }
   }
   
   public void setExpression(String expression)
   {
      this.expression = expression;
   }
   
   public void setFile(File file)
   {
      this.file = file;
   }
   
   public void setProperty(String property)
   {
      this.property = property;
   }
}
