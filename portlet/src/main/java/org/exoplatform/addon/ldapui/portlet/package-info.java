@Portlet
@Application(name = "LdapUI")
@Bindings(
  {
    @Binding(value = LdapUIService.class)
  }
)
@Assets(
  scripts = {
    @Script(id = "ldapui", src = "js/ldapui.js", location = AssetLocation.APPLICATION)
  },
  stylesheets = {
    @Stylesheet(src = "style/ldapui.css", location = AssetLocation.APPLICATION)
  }
)
package org.exoplatform.addon.ldapui.portlet;

import juzu.Application;
import juzu.asset.AssetLocation;
import juzu.plugin.asset.Assets;
import juzu.plugin.asset.Script;
import juzu.plugin.asset.Stylesheet;
import juzu.plugin.binding.Binding;
import juzu.plugin.binding.Bindings;
import juzu.plugin.portlet.Portlet;

import org.exoplatform.addon.ldapui.service.api.LdapUIService;

