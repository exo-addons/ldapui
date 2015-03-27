package org.exoplatform.addon.ldapui.service.api;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.services.organization.idm.Config;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.gatein.common.transaction.JTAUserTransactionLifecycleService;
import org.picocontainer.Startable;

public class OrganizationServiceWrapper extends PicketLinkIDMOrganizationServiceImpl {

  private PicketLinkIDMOrganizationServiceImpl originalOragSrv;
  private List<ComponentPlugin> listeners = new ArrayList<ComponentPlugin>();

  public OrganizationServiceWrapper(InitParams params, PicketLinkIDMService idmService, JTAUserTransactionLifecycleService jtaTransactionLifecycleService) throws Exception {
    super(params, idmService, jtaTransactionLifecycleService);
    originalOragSrv = new PicketLinkIDMOrganizationServiceImpl(params, idmService, jtaTransactionLifecycleService);
  }

  @Override
  public UserHandler getUserHandler() {
    return originalOragSrv.getUserHandler();
  }

  @Override
  public UserProfileHandler getUserProfileHandler() {
    return originalOragSrv.getUserProfileHandler();
  }

  @Override
  public GroupHandler getGroupHandler() {
    return originalOragSrv.getGroupHandler();
  }

  @Override
  public MembershipTypeHandler getMembershipTypeHandler() {
    return originalOragSrv.getMembershipTypeHandler();
  }

  @Override
  public MembershipHandler getMembershipHandler() {
    return originalOragSrv.getMembershipHandler();
  }

  @Override
  public void addListenerPlugin(ComponentPlugin listener) throws Exception {
    listeners.add(listener);
    originalOragSrv.addListenerPlugin(listener);
  }

  public void setOriginalOragSrv(PicketLinkIDMOrganizationServiceImpl originalOragSrv) throws Exception {
    if (originalOragSrv != this.originalOragSrv) {
      for (ComponentPlugin componentPlugin : listeners) {
        originalOragSrv.addListenerPlugin(componentPlugin);
      }
    }
    this.originalOragSrv = originalOragSrv;
  }

  public OrganizationService getOriginalOragSrv() {
    return originalOragSrv;
  }

  @Override
  public void startRequest(ExoContainer container) {
    ((ComponentRequestLifecycle) originalOragSrv).startRequest(container);
  }

  @Override
  public void endRequest(ExoContainer container) {
    ((ComponentRequestLifecycle) originalOragSrv).endRequest(container);
  }

  @Override
  public void start() {
    ((Startable) originalOragSrv).start();
  }

  @Override
  public void stop() {
    ((Startable) originalOragSrv).stop();
  }
  
  @Override
  public void flush() {
    originalOragSrv.flush();
  }
  
  @Override
  public Config getConfiguration() {
    return originalOragSrv.getConfiguration();
  }
  
  @Override
  public void recoverFromIDMError(Exception e) {
    originalOragSrv.recoverFromIDMError(e);
  }
  
  @Override
  public void setConfiguration(Config configuration) {
    originalOragSrv.setConfiguration(configuration);
  }
}