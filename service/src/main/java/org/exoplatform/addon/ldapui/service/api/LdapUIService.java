package org.exoplatform.addon.ldapui.service.api;

import java.util.Collection;
import java.util.List;

import javax.inject.Singleton;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.PropertyConfigurator;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.SessionManagerImpl;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.configuration.ConfigurationManagerImpl;
import org.exoplatform.container.util.ContainerUtil;
import org.exoplatform.container.xml.Component;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.organization.idm.PicketLinkIDMServiceImpl;
import org.exoplatform.test.MockServletContext;

@Singleton
public class LdapUIService {

  public ExoContainer getContainerWithPLIDMMapping(String mappingFilePath) throws Exception {
    System.setProperty("ldapui.picketlink.configuration.path", mappingFilePath);

    // container = new ExoContainer(new MX4JComponentAdapterFactory(), null);
    // FIXME We should use ExoContainer, but some components assume that this is
    // a PortalContainer
    // see
    // https://github.com/exoplatform/gatein-portal/blob/stable/3.5.x-PLF/component/identity/src/main/java/org/exoplatform/services/organization/idm/IDMUserListAccess.java#L141
    String containerName = "ldapui" + ((int) (Math.random() * 1000));
    ExoContainer container = new PortalContainer(RootContainer.getInstance(), new MockServletContext());

    System.setProperty("ldapui.container.name", containerName);

    ConfigurationManager configurationManager = new ConfigurationManagerImpl(this.getClass().getClassLoader(), ExoContainer.getProfiles());
    configurationManager.addConfiguration(this.getClass().getResource("inherited-components-configuration.xml"));
    configurationManager.addConfiguration(this.getClass().getResource("common-configuration.xml"));
    container.registerComponentInstance(ConfigurationManager.class, configurationManager);
    container.registerComponentImplementation(SessionManagerImpl.class);
    new PropertyConfigurator(configurationManager);

    ContainerUtil.addComponents(container, configurationManager);
    container.start();

    ExoContainerContext.setCurrentContainer(container);

    // FIXME Workaround, some IDM components uses PortalContainer.getInstance()
    // instead of ExoContainerContext.getCurrentContainer()
    // see
    // https://github.com/exoplatform/gatein-portal/blob/stable/3.5.x-PLF/component/identity/src/main/java/org/exoplatform/services/organization/idm/IDMUserListAccess.java#L141
    PortalContainer.setInstance((PortalContainer) container);

    return container;
  }

  public void replaceOrgService() throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PicketLinkIDMServiceWrapper idmServiceWrapper = (PicketLinkIDMServiceWrapper) container.getComponentInstanceOfType(PicketLinkIDMService.class);
    OrganizationServiceWrapper organizationServiceWrapper = (OrganizationServiceWrapper) container.getComponentInstanceOfType(OrganizationService.class);

    ConfigurationManager configurationManager = new ConfigurationManagerImpl(this.getClass().getClassLoader(), ExoContainer.getProfiles());
    System.setProperty("ldapui.picketlink.configuration.path", "jar:/org/exoplatform/addon/ldapui/service/api/picketlink-idm-step4.xml");

    configurationManager.addConfiguration(this.getClass().getResource("common-configuration.xml"));

    Collection<?> components = configurationManager.getComponents();
    for (Object object : components) {
      Component component = (Component) object;
      Object service = container.createComponent(Class.forName(component.getType()), component.getInitParams());
      if (service instanceof OrganizationService) {
        organizationServiceWrapper.setOriginalOragSrv((PicketLinkIDMOrganizationServiceImpl) service);
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
  }

}