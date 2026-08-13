package org.entermediadb.authenticate;

import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.HttpSharedConnection;

public class AutoLoginOAuth extends AutoLoginWithCookie implements AutoLoginProvider
{
	static final Log log = LogFactory.getLog(AutoLoginOAuth.class);
	CacheManager fieldCacheManager;

	public CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

	@Override
	public AutoLoginResult autoLogin(WebPageRequest inReq)
	{
		// log.info("Auto Login check");

		User ok = null;

		if (ok == null && inReq.getRequest() != null)
		{
			String auth = inReq.getRequest().getHeader("Authorization");
			if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7))
			{
				ok = autoLoginFromMd5Value(inReq, auth.substring(7).trim());

				if (ok == null)
				{
					// Go check the upstream server
					ok = autoLoginRemoteFromMd5Value(inReq, auth.substring(7).trim());
				}
			}
		}
		if (ok != null)
		{
			saveCookieForUser(inReq, ok); // For next time
			AutoLoginResult result = new AutoLoginResult();
			result.setUser(ok);
			return result;
		}

		return null;
	}

	protected UserManager getUserManager()
	{
		return (UserManager) getModuleManager().getBean("system", "userManager");
	}

	protected org.openedit.util.HttpSharedConnection getSharedHttpClient()
	{
		HttpSharedConnection connection = (HttpSharedConnection) getCacheManager().get("httpconnection", "oAuthConnection");
		if (connection == null)
		{
			connection = (HttpSharedConnection) getModuleManager().getBean("system", "httpSharedConnection", false);
			getCacheManager().put("httpconnection", "oAuthConnection", connection);
		}
		return connection;
	}

	protected synchronized User autoLoginRemoteFromMd5Value(WebPageRequest inReq, String uandpass)
	{
		// Rest API call?
		Data remote = getSearcherManager().getCachedData("system", "systemsettings", "upstream_authentication_mediadb");
		if (remote == null || remote.get("value") == null)
		{
			return null;
		}
		String url = remote.get("value");
		getSharedHttpClient().putSharedHeader("Authorization", "Bearer " + uandpass);
		String api = url + "/services/authentication/user.json";
		CloseableHttpResponse response = getSharedHttpClient().sharedGet(api);
		Map responsedata = (Map<String, Object>) getSharedHttpClient().parseMap(response);
		Map userdata = (Map<String, Object>) responsedata.get("user");
		if (userdata == null)
		{
			log.error("User not found: " + uandpass);
			return null;
		}

		String userid = (String) userdata.get("username");
		String firstname = (String) userdata.get("firstname");
		String lastname = (String) userdata.get("lastname");
		String email = (String) userdata.get("email");
		User user = getUserManager().getUser(userid);

		if (user == null)
		{
			user = getUserManager().createUser(userid, null);
			user.setFirstName(firstname);
			user.setLastName(lastname);
			user.setEmail(email);
			getUserManager().saveUser(user); // Will have a unique entermediadkey based on random passowrd
		}
		return user;

	}

}
