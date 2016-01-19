package org.exoplatform.addon.ldapui.service.api;

import java.util.Collection;
import java.util.List;

import javax.inject.Singleton;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.PropertyConfigurator;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.configuration.ConfigurationManagerImpl;
import org.exoplatform.container.util.ContainerUtil;
import org.exoplatform.container.xml.Component;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.idm.PicketLinkIDMCacheService;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.organization.idm.PicketLinkIDMServiceImpl;

@Singleton
public class LdapUIService {

  public ExoContainer getContainerWithPLIDMMapping(String mappingFilePath) throws Exception {
    System.setProperty("ldapui.picketlink.configuration.path", mappingFilePath);

    String containerName = "ldapui" + ((int) (Math.random() * 1000));
    ExoContainer container = new ExoContainer();

    System.setProperty("ldapui.container.name", containerName);

    ConfigurationManager configurationManager = new ConfigurationManagerImpl(this.getClass().getClassLoader(), ExoContainer.getProfiles());
    configurationManager.addConfiguration(this.getClass().getResource("inherited-components-configuration.xml"));
    configurationManager.addConfiguration(this.getClass().getResource("common-configuration.xml"));
    container.registerComponentInstance(ConfigurationManager.class, configurationManager);
    new PropertyConfigurator(configurationManager);

    ContainerUtil.addComponents(container, configurationManager);
    container.start();

    PicketLinkIDMCacheService idmCacheService = (PicketLinkIDMCacheService) container.getComponentInstanceOfType(PicketLinkIDMCacheService.class);
    idmCacheService.invalidateAll();

    ExoContainerContext.setCurrentContainer(container);
    return container;
  }

  public void replaceOrgService() throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PicketLinkIDMServiceWrapper idmServiceWrapper = (PicketLinkIDMServiceWrapper) container.getComponentInstanceOfType(PicketLinkIDMService.class);
    OrganizationServiceWrapper organizationServiceWrapper = (OrganizationServiceWrapper) container.getComponentInstanceOfType(OrganizationService.class);

    ConfigurationManager configurationManager = new ConfigurationManagerImpl(this.getClass().getClassLoader(), ExoContainer.getProfiles());
    System.setProperty("ldapui.picketlink.configuration.path", "jar:/org/exoplatform/addon/ldapui/service/api/picketlink-idm-step4.xml");

    configurationManager.addConfiguration(this.getClass().getResource("common-configuration.xml"));
    configurationManager.addConfiguration("war:/conf/platform/organization-integration-configuration.xml");

    Collection<?> components = configurationManager.getComponents();
    for (Object object : components) {
      Component component = (Component) object;
      Object service = container.createComponent(Class.forName(component.getType()), component.getInitParams());
      if (service instanceof OrganizationService) {
        organizationServiceWrapper.setOriginalOrgSrv((PicketLinkIDMOrganizationServiceImpl) service);
        List<org.exoplatform.container.xml.ComponentPlugin> plugins = component.getComponentPlugins();
        if (plugins != null) {
          for (org.exoplatform.container.xml.ComponentPlugin plugin : plugins) {
            Class<?> pluginClass = Class.forName(plugin.getType());
            ComponentPlugin cplugin = (ComponentPlugin) container.createComponent(pluginClass, plugin.getInitParams());
            cplugin.setName(plugin.getName());
            cplugin.setDescription(plugin.getDescription());
            ((OrganizationService) service).addListenerPlugin(cplugin);
          }
        }
        organizationServiceWrapper.start();
      } else if (service instanceof PicketLinkIDMService) {
        idmServiceWrapper.setOriginalPLIDMService((PicketLinkIDMServiceImpl) service);
        idmServiceWrapper.start();
      }
    }

    PicketLinkIDMCacheService idmCacheService = (PicketLinkIDMCacheService) container.getComponentInstanceOfType(PicketLinkIDMCacheService.class);
    idmCacheService.invalidateAll();
  }

  public void synchronizeProfiles() throws Exception {
    OrganizationService organizationService = (OrganizationService) PortalContainer.getInstance().getComponentInstanceOfType(OrganizationService.class);
    ListAccess<User> usersListAccess = organizationService.getUserHandler().findAllUsers(UserStatus.ANY);
    int size = usersListAccess.getSize();
    int index = 0;
    while (index < size) {
      User[] users = usersListAccess.load(index++, 1);
      organizationService.getUserHandler().saveUser(users[0], true);
    }
  }

}