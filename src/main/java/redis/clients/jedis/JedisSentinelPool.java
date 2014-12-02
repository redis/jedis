package redis.clients.jedis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Pair;
import redis.clients.util.Pool;

public class JedisSentinelPool extends Pool<Jedis> {

	protected GenericObjectPoolConfig poolConfig;

	protected int timeout = Protocol.DEFAULT_TIMEOUT;

	protected String password;

	protected int database = Protocol.DEFAULT_DATABASE;

	protected Set<SentinelListener> masterListeners = new HashSet<SentinelListener>();

	protected Logger log = Logger.getLogger(getClass().getName());

	public JedisSentinelPool(String masterName, Set<String> sentinels,
			final GenericObjectPoolConfig poolConfig) {
		this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, null,
				Protocol.DEFAULT_DATABASE);
	}

	public JedisSentinelPool(String masterName, Set<String> sentinels) {
		this(masterName, sentinels, new GenericObjectPoolConfig(),
				Protocol.DEFAULT_TIMEOUT, null, Protocol.DEFAULT_DATABASE);
	}

	public JedisSentinelPool(String masterName, Set<String> sentinels,
			String password) {
		this(masterName, sentinels, new GenericObjectPoolConfig(),
				Protocol.DEFAULT_TIMEOUT, password);
	}

	public JedisSentinelPool(String masterName, Set<String> sentinels,
			final GenericObjectPoolConfig poolConfig, int timeout,
			final String password) {
		this(masterName, sentinels, poolConfig, timeout, password,
				Protocol.DEFAULT_DATABASE);
	}

	public JedisSentinelPool(String masterName, Set<String> sentinels,
			final GenericObjectPoolConfig poolConfig, final int timeout) {
		this(masterName, sentinels, poolConfig, timeout, null,
				Protocol.DEFAULT_DATABASE);
	}

	public JedisSentinelPool(String masterName, Set<String> sentinels,
			final GenericObjectPoolConfig poolConfig, final String password) {
		this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT,
				password);
	}

	public JedisSentinelPool(String masterName, Set<String> sentinels,
			final GenericObjectPoolConfig poolConfig, int timeout,
			final String password, final int database) {

		this.poolConfig = poolConfig;
		this.timeout = timeout;
		this.password = password;
		this.database = database;

		HostAndPort master = initSentinels(sentinels, masterName);
		initPool(master);
	}

	private volatile JedisFactory factory;
	private volatile HostAndPort currentHostMaster;

	public void destroy() {
		for (final SentinelListener m : masterListeners) {
			m.shutdown();
		}

		super.destroy();
	}

	public HostAndPort getCurrentHostMaster() {
		return currentHostMaster;
	}

	private void initPool(HostAndPort master) {
		if (!master.equals(currentHostMaster)) {
			currentHostMaster = master;
			if (factory == null) {
				factory = new JedisFactory(master.getHost(), master.getPort(),
						timeout, password, database);
				initPool(poolConfig, factory);
			} else {
				factory.setHostAndPort(currentHostMaster);
				// although we clear the pool, we still have to check the
				// returned object
				// in getResource, this call only clears idle instances, not
				// borrowed instances
				internalPool.clear();
			}

			log.log(Level.INFO, "Created JedisPool to master at {0}", master);
		}
	}

	private HostAndPort initSentinels(final Set<String> sentinels,
			final String masterName) {

		HostAndPort master = null;
		boolean sentinelAvailable = false;

		log.info("Trying to find master from available Sentinels...");

		for (final String sentinel : sentinels) {
			final HostAndPort hap = toHostAndPort(Arrays.asList(sentinel
					.split(":")));

			log.log(Level.FINE, "Connecting to Sentinel {0}", hap);

			Jedis jedis = null;
			try {
				jedis = new Jedis(hap.getHost(), hap.getPort());

				List<String> masterAddr = jedis
						.sentinelGetMasterAddrByName(masterName);

				// connected to sentinel...
				sentinelAvailable = true;

				if (masterAddr == null || masterAddr.size() != 2) {
					log.log(Level.WARNING,
							"Can not get master addr, master name: {0}. Sentinel: {1}.",
							new Object[] { masterName, hap });
					continue;
				}

				master = toHostAndPort(masterAddr);
				log.log(Level.FINE, "Found Redis master at {0}", master);
				break;
			} catch (JedisConnectionException e) {
				log.log(Level.WARNING,
						"Cannot connect to sentinel running @ {0}. Trying next one.",
						hap);
			} finally {
				if (jedis != null) {
					jedis.close();
				}
			}
		}

		if (master == null) {
			if (sentinelAvailable) {
				// can connect to sentinel, but master name seems to not
				// monitored
				throw new JedisException("Can connect to sentinel, but "
						+ masterName + " seems to be not monitored...");
			} else {
				throw new JedisConnectionException(
						"All sentinels down, cannot determine where is "
								+ masterName + " master is running...");
			}
		}

		log.log(Level.INFO,
				"Redis master running at {0}, starting Sentinel listeners...",
				master);

		initSentinelListeners(sentinels, masterName,
				new Pair<JedisSentinelPubSubAdapter, String[]>(
						new JedisSentinelPubSubAdapter() {
							@Override
							public boolean isValidMessage(final String channel,
									final String[] messageParts) {
								return null != messageParts
										&& messageParts.length > 3;
							}

							@Override
							public String getMasterName(
									final String[] messageParts) {
								return messageParts[0];
							}

							@Override
							public void handleMessage(final String channel,
									final String[] switchMasterMsg) {
								initPool(toHostAndPort(Arrays.asList(
										switchMasterMsg[3], switchMasterMsg[4])));
							}
						}, new String[] { "+switch-master" }));
		return master;
	}

	protected void initSentinelListeners(Set<String> sentinels,
			final String masterName,
			final Pair<JedisSentinelPubSubAdapter, String[]>... subscriptions) {
		for (String sentinel : sentinels) {
			final HostAndPort hap = toHostAndPort(Arrays.asList(sentinel
					.split(":")));
			final SentinelListener masterListener = new SentinelListener(
					masterName, hap.getHost(), hap.getPort(), subscriptions);
			masterListeners.add(masterListener);
			masterListener.start();
		}
	}

	protected HostAndPort toHostAndPort(final List<String> hostPort) {
		if (null != hostPort && hostPort.size() > 1) {
			String host = hostPort.get(0);
			int port = Integer.parseInt(hostPort.get(1));
			return new HostAndPort(host, port);
		}
		return null;
	}

	@Override
	public Jedis getResource() {
		while (true) {
			Jedis jedis = super.getResource();
			jedis.setDataSource(this);

			// get a reference because it can change concurrently
			final HostAndPort master = currentHostMaster;
			final HostAndPort connection = new HostAndPort(jedis.getClient()
					.getHost(), jedis.getClient().getPort());

			if (master.equals(connection)) {
				// connected to the correct master
				return jedis;
			} else {
				returnBrokenResource(jedis);
			}
		}
	}

	@Override
	public void returnBrokenResource(final Jedis resource) {
		if (resource != null) {
			returnBrokenResourceObject(resource);
		}
	}

	@Override
	public void returnResource(final Jedis resource) {
		if (resource != null) {
			resource.resetState();
			returnResourceObject(resource);
		}
	}

	protected abstract class JedisSentinelPubSubAdapter extends JedisPubSub {

		protected String masterName;
		protected String sentinelHost;
		protected int sentinelPort;

		public void listenOn(final String masterName, final String host,
				final int port) {
			this.masterName = masterName;
			this.sentinelHost = host;
			this.sentinelPort = port;
		}

		@Override
		public void onMessage(final String channel, final String message) {
			log.log(Level.FINE, "Sentinel {0}:{1} published: {2}.",
					new Object[] { sentinelHost, sentinelPort, message });
			final String[] messageParts = message.split(" ");
			if (!isValidMessage(channel, messageParts)) {
				log.log(Level.SEVERE,
						"Invalid message received on Sentinel {0}:{1} on channel {2} : {3}",
						new Object[] { sentinelHost, sentinelPort, channel,
								message });
			} else if (!this.masterName
					.equalsIgnoreCase(getMasterName(messageParts))) {
				log.log(Level.FINE,
						"Ignoring message on {0} for master name {1}, our master name is {2}",
						new Object[] { masterName, message, masterName });
			} else {
				handleMessage(channel, messageParts);
			}
		}

		public abstract boolean isValidMessage(final String channel,
				final String[] messageParts);

		public abstract String getMasterName(final String[] messageParts);

		public abstract void handleMessage(final String channel,
				final String[] messageParts);
	}

	protected class SentinelListener extends Thread {

		public final static long DEFAULT_RETRY_WAIT_TIME = 5000;

		protected String masterName;
		protected String sentinelHost;
		protected int sentinelPort;
		protected long subscribeRetryWaitTimeMillis;
		protected Jedis j;
		protected AtomicBoolean running = new AtomicBoolean(false);
		private final Pair<JedisSentinelPubSubAdapter, String[]>[] subscriptions;

		public SentinelListener(
				final String masterName,
				final String host,
				final int port,
				final Pair<JedisSentinelPubSubAdapter, String[]>... subscriptions) {
			this(masterName, host, port, DEFAULT_RETRY_WAIT_TIME, subscriptions);
		}

		public SentinelListener(
				final String masterName,
				final String host,
				final int port,
				final long subscribeRetryWaitTimeMillis,
				final Pair<JedisSentinelPubSubAdapter, String[]>... subscriptions) {
			if (null == subscriptions || subscriptions.length == 0) {
				throw new JedisException(
						"There are no subscriptions to listen on");
			}
			this.masterName = masterName;
			this.sentinelHost = host;
			this.sentinelPort = port;
			this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
			this.subscriptions = subscriptions;
		}

		@Override
		public void run() {

			running.set(true);

			while (running.get()) {
				// Connect to the Sentinel
				j = new Jedis(sentinelHost, sentinelPort);
				try {
					for (final Pair<JedisSentinelPubSubAdapter, String[]> subscription : subscriptions) {
						// Set the Sentinel host and port to subscribe to
						subscription.getLeft().listenOn(masterName,
								sentinelHost, sentinelPort);
						j.subscribe(subscription.getLeft(),
								subscription.getRight());
					}
				} catch (JedisConnectionException e) {
					if (running.get()) {
						log.log(Level.SEVERE,
								"Lost connection to Sentinel at {0}:{1}. "
										+ "Sleeping {2}ms and retrying.",
								new Object[] { sentinelHost, sentinelPort,
										subscribeRetryWaitTimeMillis });
						try {
							Thread.sleep(subscribeRetryWaitTimeMillis);
						} catch (InterruptedException e1) {
							log.log(Level.SEVERE,
									"Interruped while sleeping to retry connecting "
											+ "to Sentinel: {0}",
									e1.getMessage());
						}
					} else {
						log.log(Level.FINE,
								"Unsubscribing from Sentinel at {0}:{1}",
								new Object[] { sentinelHost, sentinelPort });
					}
				}
			}
		}

		public void shutdown() {
			try {
				log.log(Level.FINE, "Shutting down listener on {0}:{1}",
						new Object[] { sentinelHost, sentinelPort });
				running.set(false);
				// This isn't good, the Jedis object is not thread safe
				j.disconnect();
			} catch (Exception e) {
				log.log(Level.SEVERE,
						"Caught exception while shutting down: {0}",
						e.getMessage());
			}
		}
	}

}
