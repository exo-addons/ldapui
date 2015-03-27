package org.exoplatform.addon.ldapui.service.api;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.database.HibernateService;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.organization.idm.IntegrationCache;
import org.exoplatform.services.organization.idm.PicketLinkIDMCacheService;
import org.exoplatform.services.organization.idm.PicketLinkIDMServiceImpl;
import org.picketlink.idm.api.IdentitySession;
import org.picketlink.idm.api.IdentitySessionFactory;
import org.picocontainer.Startable;

public class PicketLinkIDMServiceWrapper extends PicketLinkIDMServiceImpl {

  PicketLinkIDMServiceImpl originalPLIDMService;

  public PicketLinkIDMServiceWrapper(ExoContainerContext exoContainerContext, InitParams initParams, HibernateService hibernateService, ConfigurationManager confManager,
      PicketLinkIDMCacheService picketLinkIDMCache, InitialContextInitializer dependency) throws Exception {
    super(exoContainerContext, initParams, hibernateService, confManager, picketLinkIDMCache, dependency);
    originalPLIDMService = new PicketLinkIDMServiceImpl(exoContainerContext, initParams, hibernateService, confManager, picketLinkIDMCache, dependency);
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
    ((Startable) originalPLIDMService).start();
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
}