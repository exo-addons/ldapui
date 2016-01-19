require( ["SHARED/jquery", "SHARED/bootstrap", "SHARED/bts_tab", "SHARED/juzu-ajax" ], function ( $ )
{

	$( document ).ready(function() {
	  if(!$('#ldapUI .nav li.active a[data-toggle="tab"]').length) {
	    $('#ldapUI .nav li a[href="#step1"]').tab('show');
	  } else {
	    var selectedHref = $('#ldapUI li.active a[data-toggle="tab"]').attr('href');
	    selectedTab = parseInt(selectedHref.replace("#step", ""));
	  }
	  $('#ldapUI .pager .next a').on('click', function (e) {
	    var selectedHref = $('#ldapUI .nav li.active a[data-toggle="tab"]').attr('href');
	    selectedTab = parseInt(selectedHref.replace("#step", ""));
	    
	    validateStep2();
	
	    $('#ldapUI .nav li a[href="#step'+(selectedTab+1)+'"]').tab('show');
	  });
	  
	  $('#formstep2 #testLDAPConnection').on('click', function (e) {
	  	validateStep2();
	  });
	
	  $('#formstep3 #testUsersMapping').on('click', function (e) {
	  	validateStep3();
	  });
	
	  $('#formstep3 #testUsersCredentials').on('click', function (e) {
	  	testUserCredentials();
	  });
	
	  $('#formstep4 #testGroupsMapping').on('click', function (e) {
	  	validateStep4();
	  });
	  $('#formstep4 #replaceOrgSrv').on('click', function (e) {
	  	replaceOrgSrv();
	  });
	
	  $('#formstep4 #testUserMemberships').on('click', function (e) {
	  	testUserMemberships();
	  });

	  $('#formstep5 #initializeProfiles').on('click', function (e) {
		  synchronizeProfiles();
	  });
	
	  function validateStep2() {
	  	$("#formStatusStep2").html("");
	    var providerURL = $("#providerURLInput").val();
	    if(!providerURL || providerURL == '') {
	      $("#formStatusStep2").html("Enter a valid URL");
	      return;
	    }
	
	    var adminDN = $("#adminDNInput").val();
	    if(!adminDN || adminDN == '') {
	      $("#formStatusStep3").html("Enter a valid adminDN");
	      return;
	    }
	
	    var adminPassword = $("#adminPasswordInput").val();
	    if(!adminPassword || adminPassword == '') {
	      $("#formStatusStep3").html("Enter a valid password");
	      return;
	    }
	
	    $('#formStatusStep2').jzLoad("LdapUIController.testConnection()", {
	        "providerURL": providerURL,
	        "adminDN": adminDN,
	        "adminPassword": adminPassword
	    });
	  }
	
	
	  function validateStep3() {
	  	$("#formStatusStep3").html("");
	
	    var ctxDNs = $("#UserCtxDNsInput").val();
	    if(!ctxDNs || ctxDNs == '') {
	      $("#formStatusStep3").html("Enter a valid context DN");
	      return;
	    }
	
	    var idAttributeName = $("#UserIdAttributeNameInput").val();
	    if(!idAttributeName || idAttributeName == '') {
	      $("#formStatusStep3").html("Enter a valid id Attribute Name");
	      return;
	    }
	
	    var passwordAttribute = $("#UserPasswordAttributeNameInput").val();
	    if(!passwordAttribute || passwordAttribute == '') {
	      $("#formStatusStep3").html("Enter a valid password attribute");
	      return;
	    }
	
	    var entrySearchFilter = $("#UserEntrySearchFilterInput").val();
	    if(!entrySearchFilter || entrySearchFilter == '') {
	      $("#formStatusStep3").html("Enter a valid search filter");
	      return;
	    }
	
	    var firstName = $("#UserFirstNameInput").val();
	    if(!firstName || firstName == '') {
	      $("#formStatusStep3").html("Enter a valid firstName attribute");
	      return;
	    }
	
	    var lastName = $("#UserLastNameInput").val();
	    if(!lastName || lastName == '') {
	      $("#formStatusStep3").html("Enter a valid lastname attribute");
	      return;
	    }
	
	    var email = $("#UserEmailInput").val();
	    if(!email || email == '') {
	      $("#formStatusStep3").html("Enter a valid email attribute");
	      return;
	    }
	
	    $('#formStatusStep3').jzLoad("LdapUIController.testUserMapping()", {
	        "ctxDNs": ctxDNs,
	        "idAttributeName": idAttributeName,
	        "passwordAttributeName": passwordAttribute,
	        "entrySearchFilter": entrySearchFilter,
	        "firstName": firstName,
	        "lastName": lastName,
	        "email": email
	    });
	  }
	
	  function replaceOrgSrv() {
	  	$("#replaceOrgSrvStatus").html("");
	    $('#replaceOrgSrvStatus').jzLoad("LdapUIController.replaceOrgSrv()");
	  }
	
	  function validateStep4() {
	  	$("#formStatusStep4").html("");
	
	    var ctxDNs = $("#GroupCtxDNsInput").val();
	    if(!ctxDNs || ctxDNs == '') {
	      $("#formStatusStep4").html("Enter a valid context DN");
	      return;
	    }
	
	    var idAttributeName = $("#GroupIdAttributeNameInput").val();
	    if(!idAttributeName || idAttributeName == '') {
	      $("#formStatusStep4").html("Enter a valid id Attribute Name");
	      return;
	    }
	
	    var entrySearchFilter = $("#GroupEntrySearchFilterInput").val();
	    if(!entrySearchFilter || entrySearchFilter == '') {
	      $("#formStatusStep4").html("Enter a valid search filter");
	      return;
	    }
	
	    var groupParentName = $("#GroupParentNameInput").val();
	    if(!groupParentName || groupParentName == '') {
	      $("#formStatusStep4").html("Enter a valid search filter");
	      return;
	    }
	
	    $('#formStatusStep4').jzLoad("LdapUIController.testGroupMapping()", {
	        "ctxDNs": ctxDNs,
	        "idAttributeName": idAttributeName,
	        "entrySearchFilter": entrySearchFilter,
	        "groupParentName": groupParentName
	    });
	  }
	
	  function validateStep5() {
	  	$("#formStatusStep5").html("");
	
	    var enableSocial = $("#EnableSocialInput").val();
	    var enableCalendar = $("#EnableCalendarInput").val();
	    var enableForum = $("#EnableForumInput").val();
	
	    $('#formStatusStep5').jzLoad("LdapUIController.saveEnabledProfiles()", {
	        "enableSocial": enableSocial,
	        "enableCalendar": enableCalendar,
	        "enableForum": enableForum
	    });
	  }
	
	
	  function testUserCredentials() {
	  	$("#testUsersCredentialsStatus").html("");
	
	    var username = $("#TestCredentialsUserNameInput").val();
	    if(!username || username == '') {
	      $("#testUsersCredentialsStatus").html("Enter a username");
	      return;
	    }
	
	    var password = $("#TestCredentialsPasswordInput").val();
	    if(!password || password == '') {
	      $("#testUsersCredentialsStatus").html("Enter a password");
	      return;
	    }
	
	    $('#testUsersCredentialsStatus').jzLoad("LdapUIController.testUserCredentials()", {
	        "username": username,
	        "password": password
	    });
	  }

	  function synchronizeProfiles() {
		  	$("#formStatusStep5").html("");
		    $('#formStatusStep5').jzLoad("LdapUIController.synchronizeProfiles()");
		  }

	  function testUserMemberships() {
	  	$("#UserMembershipsStatus").html("");
	
	    var username = $("#UserMembershipsInput").val();
	    if(!username || username == '') {
	      $("#UserMembershipsStatus").html("Enter a username");
	      return;
	    }
	
	    $('#UserMembershipsStatus').jzLoad("LdapUIController.testUserMemberships()", {
	        "username": username
	    });
	  }
	});
});