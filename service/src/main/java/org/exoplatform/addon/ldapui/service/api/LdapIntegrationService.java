package org.exoplatform.addon.ldapui.service.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.Component;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.Impact;
import org.exoplatform.management.annotations.ImpactType;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionManager;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionMode;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipEventListener;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileEventListener;

/**
 * This Service create Organization Model profiles, for User & Groups not
 * created via eXo OrganizationService.
 * 
 * @author Boubaker KHANFIR
 */
@Managed
@ManagedDescription("LDAP UI Addon - Integration Service")
@NameTemplate({ @Property(
  key = "name",
  value = "LDAPIntegrationService"), @Property(
  key = "service",
  value = "extensions"), @Property(
  key = "type",
  value = "ldapui") })
@RESTEndpoint(
  path = "ldapintegration")
public class LdapIntegrationService {

  private static final Log LOG = ExoLogger.getLogger(LdapIntegrationService.class);
  private static final String GROUPS_PATH = "groupsPath";
  private static final String USERS_PATH = "usersPath";
  private static final Comparator<org.exoplatform.container.xml.ComponentPlugin> COMPONENT_PLUGIN_COMPARATOR = new Comparator<org.exoplatform.container.xml.ComponentPlugin>() {
    public int compare(org.exoplatform.container.xml.ComponentPlugin o1, org.exoplatform.container.xml.ComponentPlugin o2) {
      return o1.getPriority() - o2.getPriority();
    }
  };
  private static final Comparator<Group> GROUP_COMPARATOR = new Comparator<Group>() {
    public int compare(Group o1, Group o2) {
      if (o1.getId().contains(o2.getId())) {
        return 1;
      }
      if (o2.getId().contains(o1.getId())) {
        return -1;
      }
      return o2.getId().compareTo(o1.getId());
    }
  };

  private Map<String, UserEventListener> userDAOListeners_;
  private Map<String, GroupEventListener> groupDAOListeners_;
  private Map<String, MembershipEventListener> membershipDAOListeners_;
  private Map<String, UserProfileEventListener> userProfileListeners_;

  private OrganizationService organizationService;
  private RepositoryService repositoryService;
  private DataDistributionManager dataDistributionManager;
  private NodeHierarchyCreator nodeHierarchyCreatorService;

  private boolean enableSocial = true;
  private boolean enableCalendar = true;
  private boolean enableForum = true;

  public LdapIntegrationService(OrganizationService organizationService, NodeHierarchyCreator nodeHierarchyCreatorService, RepositoryService repositoryService, ConfigurationManager manager,
      InitParams initParams) {
    this.organizationService = organizationService;
    this.repositoryService = repositoryService;
    this.nodeHierarchyCreatorService = nodeHierarchyCreatorService;

    userDAOListeners_ = new LinkedHashMap<String, UserEventListener>();
    groupDAOListeners_ = new LinkedHashMap<String, GroupEventListener>();
    membershipDAOListeners_ = new LinkedHashMap<String, MembershipEventListener>();
    userProfileListeners_ = new LinkedHashMap<String, UserProfileEventListener>();

    if (initParams != null) {
      if (initParams.containsKey("enableSocial")) {
        String enableSocialString = initParams.getValueParam("enableSocial").getValue();
        this.enableSocial = (enableSocialString != null && enableSocialString.trim().equalsIgnoreCase("true")) ? true : false;
      }
      if (initParams.containsKey("enableForum")) {
        String enableForumString = initParams.getValueParam("enableForum").getValue();
        this.enableForum = (enableForumString != null && enableForumString.trim().equalsIgnoreCase("true")) ? true : false;
      }
      if (initParams.containsKey("enableCalendar")) {
        String enableCalendarString = initParams.getValueParam("enableCalendar").getValue();
        this.enableCalendar = (enableCalendarString != null && enableCalendarString.trim().equalsIgnoreCase("true")) ? true : false;
      }
    }

    boolean hasExternalComponentPlugins = false;
    int nbExternalComponentPlugins = 0;
    try {
      ExternalComponentPlugins organizationServiceExternalComponentPlugins = manager.getConfiguration().getExternalComponentPlugins(LdapIntegrationService.class.getName());

      if (organizationServiceExternalComponentPlugins != null && organizationServiceExternalComponentPlugins.getComponentPlugins() != null) {
        nbExternalComponentPlugins = organizationServiceExternalComponentPlugins.getComponentPlugins().size();
      }

      Component organizationServiceComponent = manager.getComponent(LdapIntegrationService.class);

      if (organizationServiceComponent != null && organizationServiceComponent.getComponentPlugins() != null) {
        nbExternalComponentPlugins += organizationServiceComponent.getComponentPlugins().size();
      }
      hasExternalComponentPlugins = (nbExternalComponentPlugins > 0);
    } catch (Exception e) {
      LOG.error("Test if this component has ExternalComponentPlugins generated an exception", e);
    }

    if (!hasExternalComponentPlugins) {
      try {
        ExternalComponentPlugins organizationServiceExternalComponentPlugins = manager.getConfiguration().getExternalComponentPlugins(OrganizationService.class.getName());
        addComponentPlugin(organizationServiceExternalComponentPlugins.getComponentPlugins());

        Component organizationServiceComponent = manager.getComponent(OrganizationService.class);
        List<org.exoplatform.container.xml.ComponentPlugin> organizationServicePlugins = organizationServiceComponent.getComponentPlugins();
        if (organizationServicePlugins != null) {
          addComponentPlugin(organizationServicePlugins);
        }
      } catch (Exception e) {
        LOG.error("Failed to add OrganizationService plugins", e);
      }
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("This component has already " + nbExternalComponentPlugins + " ExternalComponentPlugins");
      }
    }
  }

  /**
   * Add a list of OrganizationService listeners into
   * OrganizationIntegrationService
   * 
   * @param plugins
   *          List of OrganizationService ComponentPlugins
   */
  public void addComponentPlugin(List<org.exoplatform.container.xml.ComponentPlugin> plugins) {
    if (plugins == null)
      return;
    Collections.sort(plugins, COMPONENT_PLUGIN_COMPARATOR);
    for (org.exoplatform.container.xml.ComponentPlugin plugin : plugins) {
      try {
        Class<?> pluginClass = Class.forName(plugin.getType());
        if (!enableSocial && pluginClass.getName().contains(".social.")) {
          LOG.info("Ignore Social plugin: " + pluginClass.getName());
          continue;
        }
        if (!enableCalendar && pluginClass.getName().contains(".calendar.")) {
          LOG.info("Ignore Calendar plugin: " + pluginClass.getName());
          continue;
        }
        if (!enableForum && pluginClass.getName().contains(".forum.")) {
          LOG.info("Ignore Forum plugin: " + pluginClass.getName());
          continue;
        }
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        ComponentPlugin cplugin = (ComponentPlugin) container.createComponent(pluginClass, plugin.getInitParams());
        cplugin.setName(plugin.getName());
        cplugin.setDescription(plugin.getDescription());

        this.addListenerPlugin(cplugin);
      } catch (Exception e) {
        LOG.error("Failed to instanciate component plugin " + plugin.getName() + ", type=" + plugin.getClass(), e);
      }
    }
  }

  /**
   * Add a listener instance to dedicated list of one organization element.
   * 
   * @param listener
   *          have to extends UserEventListener, GroupEventListener,
   *          MembershipEventListener or UserProfileEventListener.
   */
  public void addListenerPlugin(ComponentPlugin listener) {
    if (listener instanceof UserEventListener) {
      userDAOListeners_.put(listener.getName(), (UserEventListener) listener);
    } else if (listener instanceof GroupEventListener) {
      groupDAOListeners_.put(listener.getName(), (GroupEventListener) listener);
    } else if (listener instanceof MembershipEventListener) {
      membershipDAOListeners_.put(listener.getName(), (MembershipEventListener) listener);
    } else if (listener instanceof UserProfileEventListener) {
      userProfileListeners_.put(listener.getName(), (UserProfileEventListener) listener);
    } else {
      LOG.debug("Ignore listener type : " + listener.getClass());
    }
  }

  /**
   * Apply OrganizationService listeners on all Groups
   */
  @Managed
  @ManagedDescription("invoke all organization model listeners. Becarefull, this could takes a lot of time.")
  @Impact(ImpactType.READ)
  public void syncAll() {
    startRequest();
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug(" Search for non integrated Groups.");
      }
      syncAllGroups();

      if (LOG.isDebugEnabled()) {
        LOG.debug(" Search for non integrated Users.");
      }
      syncAllUsers();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    endRequest();
  }

  /**
   * Invoke Groups listeners to all Organization Model Elements
   * 
   * @throws Exception
   *           JCR or IDM operation failure
   */
  @SuppressWarnings("unchecked")
  @Managed
  @ManagedDescription("invoke all groups listeners")
  @Impact(ImpactType.READ)
  public void syncAllGroups() throws Exception {
    List<Group> groups = null;

    startRequest();
    try {
      groups = new ArrayList<Group>(organizationService.getGroupHandler().getAllGroups());

      // Invoke listeners on groups, starting from parent groups to children
      Collections.sort(groups, GROUP_COMPARATOR);
    } finally {
      endRequest();
    }

    for (Group group : groups) {
      syncGroup(group.getId());
    }
  }

  /**
   * Apply OrganizationService listeners on a selected group.
   * 
   * @param groupId
   *          The group Identifier
   */
  @Managed
  @ManagedDescription("invoke a group listeners")
  @Impact(ImpactType.WRITE)
  public void syncGroup(@ManagedDescription("Group Id") @ManagedName("groupId") String groupId) throws Exception {

    if (LOG.isDebugEnabled()) {
      LOG.debug("\tGroup listeners invocation for group= " + groupId);
    }

    boolean groupIsSynchronized = isGroupSync(groupId);

    if (!groupIsSynchronized) {
      startRequest();
      try {
        Group group = organizationService.getGroupHandler().findGroupById(groupId);
        if (group == null) {
          LOG.warn("\t\t" + groupId + " group wasn't found.");
          return;
        }
        invokeGroupListeners(group);
      } catch (Exception e) {
        LOG.error("\t\t" + "Error occured while invoking listeners of group: " + groupId, e);
      } finally {
        endRequest();
      }
    }
  }

  /**
   * Apply all users OrganizationService listeners
   */
  @Managed
  @ManagedDescription("invoke all users listeners")
  @Impact(ImpactType.READ)
  public void syncAllUsers() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("All users listeners invocation");
    }
    startRequest();
    try {
      ListAccess<User> usersListAccess = organizationService.getUserHandler().findAllUsers();
      int i = 0;
      while (i <= usersListAccess.getSize()) {
        int length = i + 10 <= usersListAccess.getSize() ? 10 : usersListAccess.getSize() - i;
        User[] users = usersListAccess.load(i, length);
        for (User user : users) {
          syncUser(user.getUserName());
        }
        i += 10;
        endRequest();

        // start new request
        startRequest();
      }
    } catch (Exception e) {
      LOG.error("\tUnknown error was occured while preparing to proceed users update", e);
    } finally {
      endRequest();
    }
  }

  /**
   * Apply OrganizationService listeners on selected User
   * 
   * @param username
   *          The user name
   * @param eventType
   *          ADDED/UPDATED/DELETED
   */
  @SuppressWarnings("deprecation")
  @Managed
  @ManagedDescription("invoke a user listeners")
  @Impact(ImpactType.READ)
  public void syncUser(@ManagedDescription("User name") @ManagedName("username") String username) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("\tUser listeners invocation for user= " + username);
    }

    boolean isUserSynchronized = isUserSync(username);
    if (!isUserSynchronized) {
      startRequest();
      try {
        User user = organizationService.getUserHandler().findUserByName(username);
        if (user == null) {
          LOG.info("\t\tFailed to synchronize " + username + " : Doesn't exist ");
          return;
        }
        if (user.getCreatedDate() == null) {
          user.setCreatedDate(new Date());
        }
        LOG.info("Invoke " + username + " user synchronization ");
        Collection<UserEventListener> userDAOListeners = userDAOListeners_.values();
        for (UserEventListener userEventListener : userDAOListeners) {
          startRequest();
          try {
            userEventListener.preSave(user, true);
          } catch (Exception e) {
            LOG.warn("\t\tFailed to call preSave for " + username + " User with listener : " + userEventListener.getClass(), e);
          } finally {
            endRequest();
          }
        }
        for (UserEventListener userEventListener : userDAOListeners) {
          try {
            startRequest();
            userEventListener.postSave(user, true);
          } catch (Exception e) {
            LOG.warn("\t\tFailed to call postSave for " + username + " User with listener : " + userEventListener.getClass(), e);
          } finally {
            endRequest();
          }
        }
      } catch (Exception e) {
        LOG.warn("\t\tFailed to call listeners for " + username + " User", e);
      } finally {
        endRequest();
      }
      invokeUserProfileListeners(username);
      invokeUserMembershipsListeners(username);
    }
  }

  /**
   * Apply OrganizationService listeners on selected User
   * 
   * @param username
   *          The user name
   */
  @Managed
  @ManagedDescription("Test if User is synhronized")
  @Impact(ImpactType.READ)
  public boolean isUserSync(@ManagedDescription("User name") @ManagedName("username") String username) throws Exception {
    boolean isUserSynchronized = false;
    startRequest();
    try {
      DataDistributionType dataDistributionType = dataDistributionManager.getDataDistributionType(DataDistributionMode.READABLE);
      String usersPath = nodeHierarchyCreatorService.getJcrPath(USERS_PATH);

      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();

      String workspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
      Session session = manageableRepository.getSystemSession(workspace);

      Node usersHome = (Node) session.getItem(usersPath);
      try {
        Node userNode = dataDistributionType.getDataNode(usersHome, username);
        isUserSynchronized = userNode != null;
      } catch (Exception e1) {
        // node not found
      }
    } finally {
      endRequest();
    }
    return isUserSynchronized;
  }

  @Managed
  @ManagedDescription("Test if group is synhronized")
  @Impact(ImpactType.READ)
  public boolean isGroupSync(String groupId) throws Exception {
    boolean groupIsSynchronized = false;
    startRequest();
    try {
      DataDistributionType dataDistributionType = dataDistributionManager.getDataDistributionType(DataDistributionMode.NONE);
      String groupsPath = nodeHierarchyCreatorService.getJcrPath(GROUPS_PATH);

      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();

      String workspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
      Session session = manageableRepository.getSystemSession(workspace);

      Node groupsHome = (Node) session.getItem(groupsPath);
      try {
        Node groupNode = dataDistributionType.getDataNode(groupsHome, groupId);
        groupIsSynchronized = groupNode != null;
      } catch (Exception e1) {
        // node not found
      }
    } finally {
      endRequest();
    }
    return groupIsSynchronized;
  }

  @SuppressWarnings("unchecked")
  private void invokeUserMembershipsListeners(String username) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("\t\tMemberships listeners invocation for user= " + username);
    }
    boolean isNew = true;

    Collection<Membership> memberships = null;
    startRequest();
    try {
      memberships = organizationService.getMembershipHandler().findMembershipsByUser(username);
      if (memberships == null || memberships.isEmpty()) {
        return;
      }
    } finally {
      endRequest();
    }

    for (Membership membership : memberships) {
      try {
        LOG.info("Invoke " + membership.getId() + " Membership synchronization.");

        if (!isGroupSync(membership.getGroupId())) {
          syncGroup(membership.getGroupId());
        }

        Collection<MembershipEventListener> membershipDAOListeners = membershipDAOListeners_.values();
        for (MembershipEventListener membershipEventListener : membershipDAOListeners) {
          startRequest();
          try {
            membershipEventListener.preSave(membership, isNew);
          } catch (Exception e) {
            LOG.error("\t\tFailed to call preSave on Membership (" + membership.getId() + ",isNew = " + isNew + ") listener = " + membershipEventListener.getClass(), e);
          } finally {
            endRequest();
          }
          startRequest();
          try {
            membershipEventListener.postSave(membership, isNew);
          } catch (Exception e) {
            LOG.error("\t\tFailed to call postSave on Membership (" + membership.getId() + ") listener = " + membershipEventListener.getClass(), e);
          } finally {
            endRequest();
          }
        }
      } catch (Exception e) {
        LOG.error("\t\tFailed to call listeners on " + username + " Memberships listeners", e);
      } finally {
        endRequest();
      }
    }
  }

  private void invokeUserProfileListeners(String username) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("\t\tProfile listeners invocation for user= " + username);
    }
    startRequest();
    try {
      boolean isNew = true;

      UserProfile userProfile = organizationService.getUserProfileHandler().findUserProfileByName(username);
      if (userProfile == null) {
        userProfile = organizationService.getUserProfileHandler().createUserProfileInstance(username);
        organizationService.getUserProfileHandler().saveUserProfile(userProfile, isNew);
        userProfile = organizationService.getUserProfileHandler().findUserProfileByName(username);
      } else if (isNew) {
        LOG.info("Invoke " + username + " user profile synchronization.");
        Collection<UserProfileEventListener> userProfileListeners = userProfileListeners_.values();
        for (UserProfileEventListener userProfileEventListener : userProfileListeners) {
          if (userProfile.getUserInfoMap() == null) {
            userProfile.setUserInfoMap(new HashMap<String, String>());
          }
          startRequest();
          try {
            userProfileEventListener.preSave(userProfile, isNew);
          } catch (Exception e) {
            LOG.warn("\t\t\tFailed to call preSave on " + username + " User profile with listener : " + userProfileEventListener.getClass(), e);
          } finally {
            endRequest();
          }
          startRequest();
          try {
            userProfileEventListener.postSave(userProfile, isNew);
          } catch (Exception e) {
            LOG.warn("\t\t\tFailed to call postSave on " + username + " User profile with listener : " + userProfileEventListener.getClass(), e);
          } finally {
            endRequest();
          }
        }
      }
    } finally {
      endRequest();
    }
  }

  private void invokeGroupListeners(Group group) {
    boolean isNew = true;
    if (group.getParentId() != null && !group.getParentId().isEmpty()) {
      startRequest();
      try {
        Group parentGroup = organizationService.getGroupHandler().findGroupById(group.getParentId());
        invokeGroupListeners(parentGroup);
      } catch (Exception e) {
        LOG.warn("\t\tError occured while attempting to get parent of " + group.getId() + " Group. Listeners will not be applied on parent " + group.getParentId(), e);
      } finally {
        endRequest();
      }
    }
    LOG.info("Invoke " + group.getId() + " Group save listeners.");
    Collection<GroupEventListener> groupDAOListeners = groupDAOListeners_.values();
    for (GroupEventListener groupEventListener : groupDAOListeners) {
      startRequest();
      try {
        groupEventListener.preSave(group, isNew);
      } catch (Exception e) {
        LOG.warn("\t\t\tFailed to call preSave on " + group.getId() + " Group, listener = " + groupEventListener.getClass(), e);
      } finally {
        endRequest();
      }
      startRequest();
      try {
        groupEventListener.postSave(group, isNew);
      } catch (Exception e) {
        LOG.warn("\t\t\tFailed to call postSave on " + group.getId() + " Group, listener = " + groupEventListener.getClass(), e);
      } finally {
        endRequest();
      }
    }
  }

  private void endRequest() {
    if (organizationService instanceof ComponentRequestLifecycle) {
      try {
        ((ComponentRequestLifecycle) organizationService).endRequest(ExoContainerContext.getCurrentContainer());
      } catch (Exception e) {
        LOG.warn("Error while committing and rollbacking transaction, see below for root cause", e);
      }
    }
  }

  private void startRequest() {
    if (organizationService instanceof ComponentRequestLifecycle) {
      ((ComponentRequestLifecycle) organizationService).startRequest(ExoContainerContext.getCurrentContainer());
    }
  }

}