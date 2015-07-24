package rice.elec529.dias.interfaces;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rice.elec529.dias.examples.LogFixture;

public class NameResolver
{
	static private NameResolver m_resolver = null;
	static private LogInterface m_logger = null;
	static private P2PInterface m_p2p = null;
	
	static private HashMap<String,nametime> h = new HashMap();
	
	private class nametime
	{
		public nametime(String ip, long ms) throws UnknownHostException
		{
			m_addy = InetAddress.getByName(ip);
			m_ms = ms;
		}
		InetAddress m_addy;
		long m_ms;
	}
	
	static public void setP2P(P2PInterface p)
	{
		m_p2p = p;
	}
	static public void addName(String name, String ip)
	{
		if (m_logger != null)
		{
			m_logger.log("nameresolver.addname " + name + " " + ip);
		}
		try
		{
			name = name.toLowerCase().trim();
			h.put(name, getNameResolver().new nametime(ip, System.currentTimeMillis()));
			if (m_logger != null)
				m_logger.log("added to ip table " + name);
		}
		catch (Exception e)
		{
			if (m_logger != null)
				m_logger.log("nameresolver " + e.getMessage());
		}
	}
	static public InetAddress getFromCache(String name)
	{
		if (name == null)
			return null;
		name = name.toLowerCase().trim();
		nametime i = h.get(name);
		if (i == null)
			return null;
		long l = System.currentTimeMillis();
		if (i.m_ms < (l - 5000))
		{
			m_logger.log("found but expired");
			return null;
		}
		return i.m_addy;
	}

	private NameResolver(LogInterface logger)
	{
		m_logger = logger;
		/*
		 * networkaddress.cache.ttl
		 * 
		 * Indicates the caching policy for successful name lookups from the name service. The value is 
		 * specified as as integer to indicate the number of seconds to cache the successful lookup. 
		 * The default setting is to cache for an implementation specific period of time.  
		 * A value of -1 indicates "cache forever".
		 */
		java.security.Security.setProperty("networkaddress.cache.ttl" , "0");

		/*
		 * networkaddress.cache.negative.ttl (default: 10)
		 * 
		 * Indicates the caching policy for un-successful name lookups from the name service. The value is 
		 * specified as as integer to indicate the number of seconds to cache the failure for un-successful lookups.
		 * A value of 0 indicates "never cache". A value of -1 indicates "cache forever".
		 */
		java.security.Security.setProperty("networkaddress.cache.negative.ttl" , "0");

		/*
		 * sun.net.inetaddr.ttl
		 * 
		 * This is a sun private system property which corresponds to networkaddress.cache.ttl.
		 * It takes the same value and has the same meaning, but can be set as a command-line 
		 * option. However, the preferred way is to use the security property mentioned above. 
		 * 
		 * sun.net.inetaddr.negative.ttl
		 * This is a sun private system property which corresponds to networkaddress.cache.negative.ttl. 
		 * It takes the same value and has the same meaning, but can be set as a command-line option. 
		 * However, the preferred way is to use the security property mentioned above.
		 */
	}

	static public NameResolver getNameResolver()
	{
		if (m_resolver == null)
		{
			m_resolver = new NameResolver(new LogFixture());
		}
		return m_resolver;
	}
	static public void setNameResolver(NameResolver resolver)
	{
		m_resolver = resolver;
	}
	public InetAddress getByName(String name) throws UnknownHostException
	{
		m_logger.log("NameResolver: Looking up " + name);
		
		InetAddress cached = getFromCache(name);
		
		if (cached != null)
			return cached;
		
		//  Try P2P.
		if (m_p2p!= null)
		{
			m_logger.log("sending ip lookup");
			m_p2p.sendMessage(name, "getIP".getBytes());
			InetAddress addy = null;
			try
			{
				m_logger.log("getting by name");
				addy = InetAddress.getByName(name);
			}
			catch (Exception e)
			{
				m_logger.log("no inet name " + name + " " + e.getMessage());
				addy = null;
			}
			m_logger.log("waiting for p2p");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				m_logger.log("thread sleep error " + e.getMessage());
				e.printStackTrace();
			}
			m_logger.log("timeout");
			cached = getFromCache(name);
			if (cached == null)
			{
				if (addy == null)
				{
					throw new UnknownHostException(name);
				}
				else
					return addy;
			}
			return cached;
		}
		
		InetAddress addy = InetAddress.getByName(name);
		m_logger.log(" is: " + addy.getHostAddress());

		try
		{
			String addressCache = "addressCache";
			m_logger.log(addressCache);
			printDNSCache(addressCache);
			String negativeCache = "negativeCache";
			m_logger.log(negativeCache);
			printDNSCache(negativeCache);
		}
		catch (Exception e)
		{
			m_logger.log(e.getMessage());
		}
		return addy;
	}

	private static void printDNSCache(String cacheName) throws Exception {
		Class<InetAddress> klass = InetAddress.class;
		Field acf = klass.getDeclaredField(cacheName);
		acf.setAccessible(true);
		Object addressCache = acf.get(null);
		Class cacheKlass = addressCache.getClass();
		Field cf = cacheKlass.getDeclaredField("cache");
		cf.setAccessible(true);
		Map<String, Object> cache = (Map<String, Object>) cf.get(addressCache);
		for (Map.Entry<String, Object> hi : cache.entrySet()) {
			Object cacheEntry = hi.getValue();
			Class cacheEntryKlass = cacheEntry.getClass();
			Field expf = cacheEntryKlass.getDeclaredField("expiration");
			expf.setAccessible(true);
			long expires = (Long) expf.get(cacheEntry);

			Field af = cacheEntryKlass.getDeclaredField("address");
			af.setAccessible(true);
			InetAddress[] addresses = (InetAddress[]) af.get(cacheEntry);
			List<String> ads = new ArrayList<String>(addresses.length);
			for (InetAddress address : addresses) {
				ads.add(address.getHostAddress());
			}

			m_logger.log(hi.getKey() + " "+new Date(expires) +" " +ads);
		}
	}
}
