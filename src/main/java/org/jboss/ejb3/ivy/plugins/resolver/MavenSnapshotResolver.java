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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.resolver.ResolverSettings;
import org.apache.ivy.plugins.resolver.URLResolver;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;
import org.apache.ivy.util.ContextualSAXHandler;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.XMLHelper;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class MavenSnapshotResolver extends URLResolver
{
   private final static Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)-\\d{8}\\.\\d{6}-\\d+");
   
   private static final String M2_PER_MODULE_PATTERN = "[revision]/[artifact]-[revision](-[classifier]).[ext]";

   private static final String M2_PATTERN = "[organisation]/[module]/" + M2_PER_MODULE_PATTERN;

   public static final String DEFAULT_PATTERN = "[module]/[type]s/[artifact]-[revision].[ext]";

   public static final String DEFAULT_ROOT = "http://www.ibiblio.org/maven/";

   public static final String DEFAULT_M2_ROOT = "http://repo1.maven.org/maven2/";

   private String root = null;

   private String pattern = null;

   // use poms if m2 compatible is true
   private boolean usepoms = true;

   // use maven-metadata.xml is exists to list revisions
   private boolean useMavenMetadata = true;

   public MavenSnapshotResolver()
   {
      // SNAPSHOT revisions are changing revisions
      setChangingMatcher(PatternMatcher.REGEXP);
      setChangingPattern(".*-SNAPSHOT");
   }

   public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data)
   {
      if (isM2compatible() && isUsepoms())
      {
         ModuleRevisionId mrid = dd.getDependencyRevisionId();
         mrid = convertM2IdForResourceSearch(mrid);

         ResolvedResource rres = null;
         if (dd.getDependencyRevisionId().getRevision().endsWith("SNAPSHOT"))
         {
            rres = findSnapshotDescriptor(dd, data, mrid);
            if (rres != null)
            {
               return rres;
            }
         }

         rres = findResourceUsingPatterns(mrid, getIvyPatterns(), DefaultArtifact.newPomArtifact(mrid, data.getDate()),
               getRMDParser(dd, data), data.getDate());
         return rres;
      }
      else
      {
         return null;
      }
   }

   protected ResolvedResource findArtifactRef(Artifact artifact, Date date)
   {
      ensureConfigured(getSettings());
      ModuleRevisionId mrid = artifact.getModuleRevisionId();
      if (isM2compatible())
      {
         mrid = convertM2IdForResourceSearch(mrid);
      }
      ResolvedResource rres = null;
      if (artifact.getId().getRevision().endsWith("SNAPSHOT"))
      {
         rres = findSnapshotArtifact(artifact, date, mrid);
         if (rres != null)
         {
            return rres;
         }
      }
      String rev = artifact.getId().getRevision();
      // TODO: we assume here that nobody will version releases according to this scheme
      Matcher matcher = VERSION_PATTERN.matcher(rev);
      if(matcher.matches())
      {
         // TODO: blazing through on shear luck, there could be less version digits
         String originalRev = matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3) + "-SNAPSHOT";
         String pattern = (String) getArtifactPatterns().get(0);
         pattern = pattern.replaceFirst("\\[revision\\]", originalRev);
         return findResourceUsingPattern(mrid, pattern, artifact, getDefaultRMDParser(artifact
               .getModuleRevisionId().getModuleId()), date);
      }
      return findResourceUsingPatterns(mrid, getArtifactPatterns(), artifact, getDefaultRMDParser(artifact
            .getModuleRevisionId().getModuleId()), date);
   }

   private ResolvedResource findSnapshotArtifact(Artifact artifact, Date date, ModuleRevisionId mrid)
   {
      String rev = findSnapshotVersion(mrid);
      if (rev != null)
      {
         // replace the revision token in file name with the resolved revision
         String pattern = (String) getArtifactPatterns().get(0);
         pattern = pattern.replaceFirst("\\-\\[revision\\]", "-" + rev);
         return findResourceUsingPattern(mrid, pattern, artifact, getDefaultRMDParser(artifact.getModuleRevisionId()
               .getModuleId()), date);
      }
      return null;
   }

   private ResolvedResource findSnapshotDescriptor(DependencyDescriptor dd, ResolveData data, ModuleRevisionId mrid)
   {
      String rev = findSnapshotVersion(mrid);
      if (rev != null)
      {
         // here it would be nice to be able to store the resolved snapshot version, to avoid
         // having to follow the same process to download artifacts

         Message.verbose("[" + rev + "] " + mrid);

         // replace the revision token in file name with the resolved revision
         String pattern = (String) getIvyPatterns().get(0);
         pattern = pattern.replaceFirst("\\-\\[revision\\]", "-" + rev);
         return findResourceUsingPattern(mrid, pattern, DefaultArtifact.newPomArtifact(mrid, data.getDate()),
               getRMDParser(dd, data), data.getDate());
      }
      return null;
   }

   private String findSnapshotVersion(ModuleRevisionId mrid)
   {
      String pattern = (String) getIvyPatterns().get(0);
      if (shouldUseMavenMetadata(pattern))
      {
         InputStream metadataStream = null;
         try
         {
            String metadataLocation = IvyPatternHelper.substitute(root
                  + "[organisation]/[module]/[revision]/maven-metadata.xml", mrid);
            Resource metadata = getRepository().getResource(metadataLocation);
            if (metadata.exists())
            {
               metadataStream = metadata.openStream();
               final StringBuffer timestamp = new StringBuffer();
               final StringBuffer buildNumer = new StringBuffer();
               XMLHelper.parse(metadataStream, null, new ContextualSAXHandler()
               {
                  public void endElement(String uri, String localName, String qName) throws SAXException
                  {
                     if ("metadata/versioning/snapshot/timestamp".equals(getContext()))
                     {
                        timestamp.append(getText());
                     }
                     if ("metadata/versioning/snapshot/buildNumber".equals(getContext()))
                     {
                        buildNumer.append(getText());
                     }
                     super.endElement(uri, localName, qName);
                  }
               }, null);
               if (timestamp.length() > 0)
               {
                  // we have found a timestamp, so this is a snapshot unique version
                  String rev = mrid.getRevision();
                  rev = rev.substring(0, rev.length() - "SNAPSHOT".length());
                  rev = rev + timestamp.toString() + "-" + buildNumer.toString();

                  return rev;
               }
            }
            else
            {
               Message.verbose("\tmaven-metadata not available: " + metadata);
            }
         }
         catch (IOException e)
         {
            Message.verbose("impossible to access maven metadata file, ignored: " + e.getMessage());
         }
         catch (SAXException e)
         {
            Message.verbose("impossible to parse maven metadata file, ignored: " + e.getMessage());
         }
         catch (ParserConfigurationException e)
         {
            Message.verbose("impossible to parse maven metadata file, ignored: " + e.getMessage());
         }
         finally
         {
            if (metadataStream != null)
            {
               try
               {
                  metadataStream.close();
               }
               catch (IOException e)
               {
                  // ignored
               }
            }
         }
      }
      return null;
   }

   public void setM2compatible(boolean m2compatible)
   {
      super.setM2compatible(m2compatible);
      if (m2compatible)
      {
         if (root == null)
         {
            root = DEFAULT_M2_ROOT;
         }
         if (pattern == null)
         {
            pattern = M2_PATTERN;
         }
         updateWholePattern();
      }
   }

   public void ensureConfigured(ResolverSettings settings)
   {
      if (settings != null && (root == null || pattern == null))
      {
         if (root == null)
         {
            String root = settings.getVariable("ivy.ibiblio.default.artifact.root");
            if (root != null)
            {
               this.root = root;
            }
            else
            {
               settings.configureRepositories(true);
               this.root = settings.getVariable("ivy.ibiblio.default.artifact.root");
            }
         }
         if (pattern == null)
         {
            String pattern = settings.getVariable("ivy.ibiblio.default.artifact.pattern");
            if (pattern != null)
            {
               this.pattern = pattern;
            }
            else
            {
               settings.configureRepositories(false);
               this.pattern = settings.getVariable("ivy.ibiblio.default.artifact.pattern");
            }
         }
         updateWholePattern();
      }
   }

   private String getWholePattern()
   {
      return root + pattern;
   }

   public String getPattern()
   {
      return pattern;
   }

   public void setPattern(String pattern)
   {
      if (pattern == null)
      {
         throw new NullPointerException("pattern must not be null");
      }
      this.pattern = pattern;
      ensureConfigured(getSettings());
      updateWholePattern();
   }

   public String getRoot()
   {
      return root;
   }

   /**
   * Sets the root of the maven like repository. The maven like repository is necessarily an http
   * repository.
   * 
   * @param root
   *            the root of the maven like repository
   * @throws IllegalArgumentException
   *             if root does not start with "http://"
   */
   public void setRoot(String root)
   {
      if (root == null)
      {
         throw new NullPointerException("root must not be null");
      }
      if (!root.endsWith("/"))
      {
         this.root = root + "/";
      }
      else
      {
         this.root = root;
      }
      ensureConfigured(getSettings());
      updateWholePattern();
   }

   private void updateWholePattern()
   {
      if (isM2compatible() && isUsepoms())
      {
         setIvyPatterns(Collections.singletonList(getWholePattern()));
      }
      setArtifactPatterns(Collections.singletonList(getWholePattern()));
   }

   public void publish(Artifact artifact, File src)
   {
      throw new UnsupportedOperationException("publish not supported by IBiblioResolver");
   }

   // we do not allow to list organisations on ibiblio, nor modules in ibiblio 1
   public String[] listTokenValues(String token, Map otherTokenValues)
   {
      if (IvyPatternHelper.ORGANISATION_KEY.equals(token))
      {
         return new String[0];
      }
      if (IvyPatternHelper.MODULE_KEY.equals(token) && !isM2compatible())
      {
         return new String[0];
      }
      ensureConfigured(getSettings());
      return super.listTokenValues(token, otherTokenValues);
   }

   protected String[] listTokenValues(String pattern, String token)
   {
      if (IvyPatternHelper.ORGANISATION_KEY.equals(token))
      {
         return new String[0];
      }
      if (IvyPatternHelper.MODULE_KEY.equals(token) && !isM2compatible())
      {
         return new String[0];
      }
      ensureConfigured(getSettings());

      // let's see if we should use maven metadata for this listing...
      if (IvyPatternHelper.REVISION_KEY.equals(token) && isM2compatible() && isUseMavenMetadata())
      {
         if (((String) getIvyPatterns().get(0)).endsWith(M2_PER_MODULE_PATTERN))
         {
            // now we must use metadata if available
            /*
            * we substitute tokens with ext token only in the m2 per module pattern, to match
            * has has been done in the given pattern
            */
            String partiallyResolvedM2PerModulePattern = IvyPatternHelper.substituteTokens(M2_PER_MODULE_PATTERN,
                  Collections.singletonMap(IvyPatternHelper.EXT_KEY, "xml"));
            if (pattern.endsWith(partiallyResolvedM2PerModulePattern))
            {
               /*
               * the given pattern already contain resolved org and module, we just have to
               * replace the per module pattern at the end by 'maven-metadata.xml' to have the
               * maven metadata file location
               */
               String metadataLocation = pattern.substring(0, pattern.lastIndexOf(partiallyResolvedM2PerModulePattern))
                     + "maven-metadata.xml";
               List revs = listRevisionsWithMavenMetadata(getRepository(), metadataLocation);
               if (revs != null)
               {
                  return (String[]) revs.toArray(new String[revs.size()]);
               }
            }
            else
            {
               /*
               * this is probably because the given pattern has been substituted with jar ext,
               * if this resolver has optional module descriptors. But since we have to use
               * maven metadata, we don't care about this case, maven metadata has already
               * been used when looking for revisions with the pattern substituted with
               * ext=xml for the "ivy" pattern.
               */
               return new String[0];
            }
         }
      }
      return super.listTokenValues(pattern, token);
   }

   public OrganisationEntry[] listOrganisations()
   {
      return new OrganisationEntry[0];
   }

   public ModuleEntry[] listModules(OrganisationEntry org)
   {
      if (isM2compatible())
      {
         ensureConfigured(getSettings());
         return super.listModules(org);
      }
      return new ModuleEntry[0];
   }

   public RevisionEntry[] listRevisions(ModuleEntry mod)
   {
      ensureConfigured(getSettings());
      return super.listRevisions(mod);
   }

   protected ResolvedResource[] listResources(Repository repository, ModuleRevisionId mrid, String pattern,
         Artifact artifact)
   {
      if (shouldUseMavenMetadata(pattern))
      {
         List revs = listRevisionsWithMavenMetadata(repository, mrid.getModuleId().getAttributes());
         if (revs != null)
         {
            Message.debug("\tfound revs: " + revs);
            List rres = new ArrayList();
            for (Iterator iter = revs.iterator(); iter.hasNext();)
            {
               String rev = (String) iter.next();
               ModuleRevisionId snapshotMrid = ModuleRevisionId.newInstance(mrid, rev);
               String realRev = findSnapshotVersion(snapshotMrid);
               String realPattern = pattern.replaceFirst("\\[revision\\]", "/" + rev);
               String resolvedPattern = IvyPatternHelper.substitute(realPattern, ModuleRevisionId.newInstance(snapshotMrid, realRev),
                     artifact);
               try
               {
                  Resource res = repository.getResource(resolvedPattern);
                  if ((res != null) && res.exists())
                  {
                     rres.add(new ResolvedResource(res, realRev));
                  }
               }
               catch (IOException e)
               {
                  Message.warn("impossible to get resource from name listed by maven-metadata.xml:" + rres + ": "
                        + e.getMessage());
               }
            }
            return (ResolvedResource[]) rres.toArray(new ResolvedResource[rres.size()]);
         }
         else
         {
            // maven metadata not available or something went wrong, 
            // use default listing capability
            return super.listResources(repository, mrid, pattern, artifact);
         }
      }
      else
      {
         return super.listResources(repository, mrid, pattern, artifact);
      }
   }

   private List listRevisionsWithMavenMetadata(Repository repository, Map tokenValues)
   {
      String metadataLocation = IvyPatternHelper.substituteTokens(root + "[organisation]/[module]/maven-metadata.xml",
            tokenValues);
      return listRevisionsWithMavenMetadata(repository, metadataLocation);
   }

   private List listRevisionsWithMavenMetadata(Repository repository, String metadataLocation)
   {
      List revs = null;
      InputStream metadataStream = null;
      try
      {
         Resource metadata = repository.getResource(metadataLocation);
         if (metadata.exists())
         {
            Message.verbose("\tlisting revisions from maven-metadata: " + metadata);
            final List metadataRevs = new ArrayList();
            metadataStream = metadata.openStream();
            XMLHelper.parse(metadataStream, null, new ContextualSAXHandler()
            {
               public void endElement(String uri, String localName, String qName) throws SAXException
               {
                  if ("metadata/versioning/versions/version".equals(getContext()))
                  {
                     metadataRevs.add(getText().trim());
                  }
                  super.endElement(uri, localName, qName);
               }
            }, null);
            revs = metadataRevs;
         }
         else
         {
            Message.verbose("\tmaven-metadata not available: " + metadata);
         }
      }
      catch (IOException e)
      {
         Message.verbose("impossible to access maven metadata file, ignored: " + e.getMessage());
      }
      catch (SAXException e)
      {
         Message.verbose("impossible to parse maven metadata file, ignored: " + e.getMessage());
      }
      catch (ParserConfigurationException e)
      {
         Message.verbose("impossible to parse maven metadata file, ignored: " + e.getMessage());
      }
      finally
      {
         if (metadataStream != null)
         {
            try
            {
               metadataStream.close();
            }
            catch (IOException e)
            {
               // ignored
            }
         }
      }
      return revs;
   }

   protected void findTokenValues(Collection names, List patterns, Map tokenValues, String token)
   {
      if (IvyPatternHelper.REVISION_KEY.equals(token))
      {
         String pattern = (String) patterns.get(0);
         if (shouldUseMavenMetadata(pattern))
         {
            List revs = listRevisionsWithMavenMetadata(getRepository(), tokenValues);
            if (revs != null)
            {
               names.addAll(filterNames(revs));
               return;
            }
         }
      }
      super.findTokenValues(names, patterns, tokenValues, token);
   }

   private boolean shouldUseMavenMetadata(String pattern)
   {
      return isUseMavenMetadata() && isM2compatible() && pattern.endsWith(M2_PATTERN);
   }

   public String getTypeName()
   {
      return "maven-snapshot-resolver";
   }

   // override some methods to ensure configuration
   public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data) throws ParseException
   {
      ensureConfigured(data.getSettings());
      return super.getDependency(dd, data);
   }

   public DownloadReport download(Artifact[] artifacts, DownloadOptions options)
   {
      ensureConfigured(getSettings());
      return super.download(artifacts, options);
   }

   public boolean exists(Artifact artifact)
   {
      ensureConfigured(getSettings());
      return super.exists(artifact);
   }

   public ArtifactOrigin locate(Artifact artifact)
   {
      ensureConfigured(getSettings());
      return super.locate(artifact);
   }

   public List getArtifactPatterns()
   {
      ensureConfigured(getSettings());
      return super.getArtifactPatterns();
   }

   public boolean isUsepoms()
   {
      return usepoms;
   }

   @Override
   public ResolvedModuleRevision parse(ResolvedResource mdRef, DependencyDescriptor dd, ResolveData data)
      throws ParseException
   {
      ResolvedModuleRevision rmr = super.parse(mdRef, dd, data);
      // patch in the resolved snapshot version
      ModuleRevisionId mrid = rmr.getId();
      DefaultModuleDescriptor descriptor = (DefaultModuleDescriptor) rmr.getDescriptor();
      //descriptor.addExtraInfo(MavenSnapshotResolver.class.getName() + ".originalRev", mrid.getRevision());
      ModuleRevisionId revId = ModuleRevisionId.newInstance(mrid, mdRef.getRevision());
      descriptor.setModuleRevisionId(revId);
      rmr.getDescriptor().setResolvedModuleRevisionId(revId);
      return rmr;
   }
   
   public void setUsepoms(boolean usepoms)
   {
      this.usepoms = usepoms;
      updateWholePattern();
   }

   public boolean isUseMavenMetadata()
   {
      return useMavenMetadata;
   }

   public void setUseMavenMetadata(boolean useMavenMetadata)
   {
      this.useMavenMetadata = useMavenMetadata;
   }

   public void dumpSettings()
   {
      ensureConfigured(getSettings());
      super.dumpSettings();
      Message.debug("\t\troot: " + getRoot());
      Message.debug("\t\tpattern: " + getPattern());
      Message.debug("\t\tusepoms: " + usepoms);
      Message.debug("\t\tuseMavenMetadata: " + useMavenMetadata);
   }
}
