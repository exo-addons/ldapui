package org.exoplatform.addon.ldapui.portlet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.directory.InitialDirContext;

import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.SessionScoped;
import juzu.View;
import juzu.template.Template;

import org.exoplatform.addon.ldapui.service.api.LdapUIService;
import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;

@SessionScoped
public class LdapUIController {
  private static final Log LOG = ExoLogger.getLogger(LdapUIController.class);

  private static Map<String, Object> parameters = new HashMap<String, Object>();

  @Inject
  LdapUIService ldapUIService;

  @Inject
  @Path("index.gtmpl")
  Template index;

  @Inject
  @Path("step1.gtmpl")
  Template step1;

  @Inject
  @Path("step2.gtmpl")
  Template step2;

  @Inject
  @Path("step3.gtmpl")
  Template step3;

  @Inject
  @Path("step4.gtmpl")
  Template step4;

  @Inject
  @Path("step5.gtmpl")
  Template step5;

  @Inject
  @Path("step6.gtmpl")
  Template step6;

  boolean enableSocial;
  boolean enableCalendar;
  boolean enableForum;

  @View
  public Response.Render index() {
    System.setProperty("ldap.groups.groupParentName", "/*");
    System.setProperty("ldap.groups.groupNameToInit", "platform");
    return index.ok(parameters);
  }

  @Ajax
  @Resource
  public Response.Content<?> testUserMapping(String ctxDNs, String idAttributeName, String passwordAttributeName, String entrySearchFilter, String firstName, String lastName, String email) {
    PortalContainer portalContainer = PortalContainer.getInstance();
    ExoContainer container = null;
    try {
      System.setProperty("ldap.users.ctxDNs", ctxDNs);
      System.setProperty("ldap.users.idAttributeName", idAttributeName);
      System.setProperty("ldap.users.passwordAttributeName", passwordAttributeName);
      System.setProperty("ldap.users.entrySearchFilter", entrySearchFilter);
      System.setProperty("ldap.users.firstName", firstName);
      System.setProperty("ldap.users.lastName", lastName);
      System.setProperty("ldap.users.email", email);

      container = ldapUIService.getContainerWithPLIDMMapping("picketlink-idm-step3.xml");
      OrganizationService organizationService = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);

      ListAccess<User> usersListAccess = organizationService.getUserHandler().findAllUsers();
      return Response.ok("'" + usersListAccess.getSize() + "' users found.");
    } catch (Exception e) {
      LOG.error("LDAP Connection error", e);
      return Response.ok("LDAP Connection error: " + (e == null ? e : e.getMessage()));
    } finally {
      if (container != null) {
        container.stop();
      }
      PortalContainer.setInstance(portalContainer);
      ExoContainerContext.setCurrentContainer(portalContainer);
    }
  }

  @Ajax
  @Resource
  public Response.Content<?> replaceOrgSrv() throws Exception {
    try {
      ldapUIService.replaceOrgService();
      return Response.ok("Sucess");
    } catch (Exception e) {
      return Response.ok("Failed");
    }
  }

  @Ajax
  @Resource
  public Response.Content<?> saveEnabledProfiles(String enableSocial, String enableCalendar, String enableForum) {
    this.enableCalendar = (enableCalendar != null && enableCalendar.trim().equalsIgnoreCase("true")) ? true : false;
    this.enableForum = (enableForum != null && enableForum.trim().equalsIgnoreCase("true")) ? true : false;
    this.enableSocial = (enableSocial != null && enableSocial.trim().equalsIgnoreCase("true")) ? true : false;

    System.setProperty("ldapui.enableCalendar", "" + this.enableCalendar);
    System.setProperty("ldapui.enableForum", "" + this.enableForum);
    System.setProperty("ldapui.enableSocial", "" + this.enableSocial);

    return Response.ok("Sucess");
  }

  @Ajax
  @Resource
  public Response.Content<?> synchronizeProfiles() {
    return Response.ok("Sucess");
  }

  @Ajax
  @Resource
  public Response.Content<?> testUserCredentials(String username, String password) {
    PortalContainer portalContainer = PortalContainer.getInstance();
    ExoContainer container = null;
    try {
      container = ldapUIService.getContainerWithPLIDMMapping("picketlink-idm-step3.xml");
      OrganizationService organizationService = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);

      return Response.ok(organizationService.getUserHandler().authenticate(username, password) ? "Sucess" : "Failed");
    } catch (Exception e) {
      LOG.error("LDAP Connection error", e);
      return Response.ok("LDAP Connection error: " + (e == null ? e : e.getMessage()));
    } finally {
      if (container != null) {
        container.stop();
      }
      PortalContainer.setInstance(portalContainer);
      ExoContainerContext.setCurrentContainer(portalContainer);
    }
  }

  @Ajax
  @Resource
  public Response.Content<?> testGroupMapping(String ctxDNs, String idAttributeName, String entrySearchFilter, String groupParentName) {
    PortalContainer portalContainer = PortalContainer.getInstance();
    ExoContainer container = null;
    try {
      System.setProperty("ldap.groups.ctxDNs", ctxDNs);
      System.setProperty("ldap.groups.idAttributeName", idAttributeName);
      System.setProperty("ldap.groups.entrySearchFilter", entrySearchFilter);
      if (groupParentName != null && groupParentName.startsWith("/")) {
        groupParentName = groupParentName.substring(1);
      }
      if (groupParentName == null || groupParentName.trim().isEmpty()) {
        System.setProperty("ldap.groups.groupParentName", "/*");
      } else {
        System.setProperty("ldap.groups.groupParentName", "/" + groupParentName + "/*");
      }

      if (groupParentName == null || groupParentName.trim().isEmpty()) {
        groupParentName = "platform";
      }
      System.setProperty("ldap.groups.groupNameToInit", groupParentName);

      container = ldapUIService.getContainerWithPLIDMMapping("picketlink-idm-step4.xml");
      OrganizationService organizationService = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);

      @SuppressWarnings("unchecked")
      Collection<Group> groups = organizationService.getGroupHandler().getAllGroups();

      return Response.ok("'" + groups.size() + "' groups found.");
    } catch (Exception e) {
      LOG.error("LDAP Connection error", e);
      return Response.ok("LDAP Connection error: " + (e == null ? e : e.getMessage()));
    } finally {
      if (container != null) {
        container.stop();
      }
      PortalContainer.setInstance(portalContainer);
      ExoContainerContext.setCurrentContainer(portalContainer);
    }
  }

  @Ajax
  @Resource
  public Response.Content<?> testUserMemberships(String username) {
    PortalContainer portalContainer = PortalContainer.getInstance();
    ExoContainer container = null;
    try {
      container = ldapUIService.getContainerWithPLIDMMapping("picketlink-idm-step4.xml");
      OrganizationService organizationService = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);

      @SuppressWarnings("unchecked")
      Collection<Membership> memberships = organizationService.getMembershipHandler().findMembershipsByUser(username);
      for (Membership membership : memberships) {
        System.out.println(membership.getId());
      }

      return Response.ok("'" + memberships.size() + "' memberships for user: '" + username + "'");
    } catch (Exception e) {
      LOG.error("LDAP Connection error", e);
      return Response.ok("LDAP Connection error: " + (e == null ? e : e.getMessage()));
    } finally {
      if (container != null) {
        container.stop();
      }
      PortalContainer.setInstance(portalContainer);
      ExoContainerContext.setCurrentContainer(portalContainer);
    }
  }

  @Ajax
  @Resource
  public Response.Content<?> testConnection(String providerURL, String adminDN, String adminPassword) {
    try {
      Hashtable<String, String> envs = new Hashtable<String, String>();
      envs.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      envs.put(Context.PROVIDER_URL, providerURL);
      envs.put(Context.SECURITY_AUTHENTICATION, "simple");
      envs.put(Context.SECURITY_PRINCIPAL, adminDN);
      envs.put(Context.SECURITY_CREDENTIALS, adminPassword);

      new InitialDirContext(envs);

      System.setProperty("ldap.providerURL", providerURL);
      System.setProperty("ldap.adminDN", adminDN);
      System.setProperty("ldap.adminPassword", adminPassword);
      return Response.ok("Connected successfully!");
    } catch (Exception e) {
      LOG.error("LDAP Connection error", e);
      return Response.ok("LDAP Connection error: " + (e == null ? e : e.getMessage()));
    }
  }
}