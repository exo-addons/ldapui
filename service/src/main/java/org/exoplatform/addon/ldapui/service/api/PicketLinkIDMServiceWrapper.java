package org.exoplatform.addon.ldapui.service.api;

import java.io.File;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.database.HibernateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.organization.idm.IntegrationCache;
import org.exoplatform.services.organization.idm.PicketLinkIDMCacheService;
import org.exoplatform.services.organization.idm.PicketLinkIDMServiceImpl;
import org.picketlink.idm.api.IdentitySession;
import org.picketlink.idm.api.IdentitySessionFactory;
import org.picocontainer.Startable;

public class PicketLinkIDMServiceWrapper extends PicketLinkIDMServiceImpl {

  private static Log log = ExoLogger.getLogger(PicketLinkIDMServiceWrapper.class);

  private PicketLinkIDMServiceImpl originalPLIDMService;

  private SessionProviderService sessionProviderService;
  private RepositoryService repositoryService;
  private ExoContainerContext exoContainerContext;
  private InitParams initParams;
  private HibernateService hibernateService;
  private ConfigurationManager confManager;
  private PicketLinkIDMCacheService picketLinkIDMCache;
  private InitialContextInitializer dependency;

  public PicketLinkIDMServiceWrapper(SessionProviderService sessionProviderService, RepositoryService repositoryService, ExoContainerContext exoContainerContext, InitParams initParams,
      HibernateService hibernateService, ConfigurationManager confManager, PicketLinkIDMCacheService picketLinkIDMCache, InitialContextInitializer dependency) throws Exception {
    // This is useless, but keep it to be able to compile
    super(exoContainerContext, initParams, hibernateService, confManager, picketLinkIDMCache, dependency);

    this.sessionProviderService = sessionProviderService;
    this.repositoryService = repositoryService;
    this.exoContainerContext = exoContainerContext;
    this.initParams = initParams;
    this.hibernateService = hibernateService;
    this.confManager = confManager;
    this.picketLinkIDMCache = picketLinkIDMCache;
    this.dependency = dependency;
  }

  @Override
  public IdentitySessionFactory getIdentitySessionFactory() {
    return originalPLIDMService.getIdentitySessionFactory();
  }

  @Override
  public IdentitySession getIdentitySession() throws Exception {
    return originalPLIDMService.getIdentitySession();
  }

  @Override
  public IdentitySession getIdentitySession(String realm) throws Exception {
    return originalPLIDMService.getIdentitySession(realm);
  }

  @Override
  public void start() {
    try {
      String plidmConfigPath = System.getProperty("ldapui.plidm.config.path");
      if (plidmConfigPath != null && !plidmConfigPath.isEmpty() && plidmConfigPath.startsWith("jcr:/")) {
        File file = getLocalFile(sessionProviderService, repositoryService, "system", plidmConfigPath);
        String url = "file:///" + file.getAbsoluteFile();
        ValueParam param = new ValueParam();
        param.setName(PARAM_CONFIG_OPTION);
        param.setValue(url);
        initParams.addParameter(param);
      }

      originalPLIDMService = new PicketLinkIDMServiceImpl(exoContainerContext, initParams, hibernateService, confManager, picketLinkIDMCache, dependency);
      ((Startable) originalPLIDMService).start();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void stop() {
    ((Startable) originalPLIDMService).stop();
  }

  public IntegrationCache getIntegrationCache() {
    return originalPLIDMService.getIntegrationCache();
  }

  public String getRealmName() {
    return originalPLIDMService.getRealmName();
  }

  public HibernateService getHibernateService() {
    return originalPLIDMService.getHibernateService();
  }

  public void setOriginalPLIDMService(PicketLinkIDMServiceImpl originalPLIDMService) {
    this.originalPLIDMService = originalPLIDMService;
  }

  public PicketLinkIDMServiceImpl getOriginalPLIDMService() {
    return originalPLIDMService;
  }

  public File getLocalFile(SessionProviderService provider, RepositoryService repositoryService, String workspace, String jcrURL) throws Exception {
    ManageableRepository manageableRepository = null;
    try {
      manageableRepository = repositoryService.getCurrentRepository();
    } catch (Exception e) {
      manageableRepository = repositoryService.getDefaultRepository();
    }
    if (manageableRepository == null) {
      throw new IllegalStateException("Cannot find repository");
    }

    Session session = provider.getSystemSessionProvider(null).getSession(workspace, manageableRepository);
    File file = null;
    try {
      Node template = (Node) session.getItem(jcrURL.substring("jcr:".length()));
      Node resourceNode = template.getNode("jcr:content");
      String content = resourceNode.getProperty("jcr:data").getString();
      file = File.createTempFile("picketlink-idm-", ".xml");
      FileUtils.write(file, content);
    } catch (Exception e) {
      log.error("Unexpected problem happen when try to process with url: " + jcrURL, e);
    } finally {
      session.logout();
    }
    return file;
  }

}