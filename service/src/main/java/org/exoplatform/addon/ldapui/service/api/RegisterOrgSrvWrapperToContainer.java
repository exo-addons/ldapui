package org.exoplatform.addon.ldapui.service.api;

import org.exoplatform.container.BaseContainerLifecyclePlugin;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.Component;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;

/**
 * @author <a href="mailto:boubaker@exoplatform.com">Boubaker Khanfir</a>
 */
public class RegisterOrgSrvWrapperToContainer extends BaseContainerLifecyclePlugin {

  private static final Log LOG = ExoLogger.getLogger("RegisterOrgSrvWrapperToContainer");

  public void initContainer(ExoContainer container) {
    ConfigurationManager cm = (ConfigurationManager) container.getComponentInstanceOfType(ConfigurationManager.class);
    try {
      replaceImplementation(container, cm, PicketLinkIDMService.class, PicketLinkIDMServiceWrapper.class);
      replaceImplementation(container, cm, OrganizationService.class, OrganizationServiceWrapper.class);
    } catch (Exception e) {
      LOG.error("Error while registring OrganizationService wrapper", e);
    }
  }

  private void replaceImplementation(ExoContainer container, ConfigurationManager cm, Class<?> key, Class<?> newType) throws Exception {
    Component component = cm.getComponent(key);
    component.setType(newType.getName());

    container.unregisterComponent(key);
    container.registerComponentImplementation(key, newType);
  }
}