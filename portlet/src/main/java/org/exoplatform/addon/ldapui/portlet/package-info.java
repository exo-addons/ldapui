@Portlet
@Application(name = "LdapUI")
@Scripts({ @Script(id = "ldapui", value = "js/ldapui.js", location = juzu.asset.AssetLocation.APPLICATION) })
@Stylesheets({ @Stylesheet(id = "ldapuiSkin", value = "style/ldapui.css") })
@Assets("*")
package org.exoplatform.addon.ldapui.portlet;

import juzu.Application;
import juzu.plugin.asset.Assets;
import juzu.plugin.asset.Script;
import juzu.plugin.asset.Scripts;
import juzu.plugin.asset.Stylesheet;
import juzu.plugin.asset.Stylesheets;
import juzu.plugin.portlet.Portlet;
