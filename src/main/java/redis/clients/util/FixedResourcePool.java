package redis.clients.util;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract resource pool of type T.
 * 
 * Needs implementation for creation, validation and destruction of the
 * resources.
 * 
 * Keeps a fixed amount of resources
 * 
 * @author Luis Dario Simonassi
 * 
 * @param <T>
 *            The type of the resource to be managed.
 */
public abstract class FixedResourcePool<T> {
    private static Logger logger = Logger.getLogger(FixedResourcePool.class
	    .getName());

    /*
     * Generic Inner Control Classes ------- ----- ------- ------- * Wrapper *
     * RepairThread
     */

    /**
     * Generic Resource Wrapper
     */
    private static class Wrapper<T> {
	long timestamp;
	T wrapped;

	public Wrapper(T wrapped) {
	    this.wrapped = wrapped;
	    mark();
	}

	public void mark() {
	    timestamp = System.currentTimeMillis();
	}

	public long getLastMark() {
	    return timestamp;
	}
    }

    /**
     * Generic Repair Thread
     */
    protected class RepairThread extends Thread {
	public void run() {

	    // Contribute to the repairing and validation effort until the pool
	    // is destroyed (finishig=true)
	    while (!finishing) {
		Wrapper<T> wrapper;
		try {
		    // Remove the oldest element from the repair queue.
		    wrapper = repairQueue.poll(timeBetweenValidation,
			    TimeUnit.MILLISECONDS);
		    if (wrapper == null) {
			// If I've been waiting too much, i'll check the idle
			// pool if connections need
			// validation and move them to the repair queue
			checkIdles();
			continue;
		    }
		} catch (InterruptedException e) {
		    if (logger.isLoggable(Level.SEVERE)) {
			logger.severe(e.getMessage());
		    }
		    continue;
		}

		// Now, I have something to repair!
		T resource = wrapper.wrapped;
		boolean valid = false;

		// Resources are null right after initialization, it means the
		// same as being an invalid resource
		if (resource != null) {
		    valid = isResourceValid(resource); // Validate the resource.
		    if (!valid)
			metrics.fails++;
		}

		// If resource is invalid or null, create a new resource and
		// destroy the invalid one.
		if (!valid) {
		    T replace = createResource();
		    metrics.resourcesCreated++;
		    wrapper.wrapped = replace;
		    if (resource != null)
			destroyResource(resource);
		}

		// Mark the resource as fresh!
		wrapper.mark();

		// Offer the resource to the available resources pool.
		if (!availableQueue.offer(wrapper)) {
		    if (logger.isLoggable(Level.SEVERE)) {
			logger
				.severe("This shouldn't happen, offering to available was rejected.");
		    }
		}
	    }

	    if (logger.isLoggable(Level.INFO)) {
		logger.info("Ending thread ["
			+ Thread.currentThread().getName() + "]");
	    }
	}

	/**
	 * Check if resources in the idle queue needs to be repaired
	 */
	private void checkIdles() {
	    // Get a sample without removing it
	    Wrapper<T> wrapper = availableQueue.peek();

	    // If no available items, nothing to repair.
	    if (wrapper == null)
		return;

	    // Check if the sampled resource needs to be repaired
	    boolean repairNeeded = isValidationNeeded(wrapper);
	    if (!repairNeeded)
		return;

	    // Move available resources from the available queue to the repair
	    // queue until no repair is needed.
	    while (repairNeeded) {

		// Get the connection from the available queue and check again.
		wrapper = availableQueue.poll();

		// No resources in the available queue, nothing to do
		if (wrapper == null) {
		    repairNeeded = false;
		    return;
		}

		// Add the resource to the corresponding queue, depending on
		// weather the resource needs to be repaired or not.
		repairNeeded = isValidationNeeded(wrapper);

		if (repairNeeded) {
		    if (!repairQueue.offer(wrapper)) {
			if (logger.isLoggable(Level.SEVERE)) {
			    logger
				    .severe("FATAL: This shouldn't happen, offering to repairing was rejected.");
			}
		    }
		} else {
		    if (!availableQueue.offer(wrapper)) {
			if (logger.isLoggable(Level.SEVERE)) {
			    logger
				    .severe("FATAL: This shouldn't happen, offering to available was rejected.");
			}
		    }
		}
	    }
	}
    }

    /*
     * Pool metrics
     */
    Metrics metrics;

    public static class Metrics {
	private volatile long failsReported = 0;
	private volatile long fails = 0;
	private volatile long resourcesCreated = 0;
	private volatile long resourcesProvided = 0;
	private volatile long resourcesReturned = 0;

	/*
	 * Pool metrics accessing methods.
	 */

	public long getFailsReported() {
	    return failsReported;
	}

	public long getFails() {
	    return fails;
	}

	public long getResourcesCreated() {
	    return resourcesCreated;
	}

	public long getResourcesProvided() {
	    return resourcesProvided;
	}

	public long getResourcesReturned() {
	    return resourcesReturned;
	}
    }

    /*
     * Pool status structures
     */
    private LinkedBlockingQueue<Wrapper<T>> availableQueue;
    private LinkedBlockingQueue<Wrapper<T>> repairQueue;
    private HashMap<T, Wrapper<T>> inUse = new HashMap<T, Wrapper<T>>();
    private RepairThread[] repairThreads;
    private Timer t;
    private boolean initializated = false;
    private boolean finishing = false;

    /*
     * Pool configuration parameters
     */
    private String name;
    private long defaultPoolWait = 50;
    private int resourcesNumber = 10;
    private int repairThreadsNumber = 3;
    private long timeBetweenValidation = 150000;
    private long destructionWait = 10000;

    /*
     * Bean pool configuration
     */

    public int getResourcesNumber() {
	return resourcesNumber;
    }

    public void setResourcesNumber(int resourcesNumber) {
	this.resourcesNumber = resourcesNumber;
    }

    public int getRepairThreadsNumber() {
	return repairThreadsNumber;
    }

    public void setRepairThreadsNumber(int repairThreadsNumber) {
	if (initializated)
	    throw new IllegalStateException(
		    "Repair threads should be setted up before init()");
	this.repairThreadsNumber = repairThreadsNumber;
    }

    public long getTimeBetweenValidation() {
	return timeBetweenValidation;
    }

    public void setTimeBetweenValidation(long timeBetweenValidation) {
	this.timeBetweenValidation = timeBetweenValidation;
    }

    public long getDestructionWait() {
	return destructionWait;
    }

    public void setDestructionWait(long destructionWait) {
	this.destructionWait = destructionWait;
    }

    public void setName(String name) {
	if (initializated)
	    throw new IllegalStateException(
		    "Name should be setted up before init()");
	this.name = name;
    }

    public String getName() {
	if (name == null)
	    name = getClass().getName();
	return name;
    }

    public void setDefaultPoolWait(long defaultPoolWait) {
	this.defaultPoolWait = defaultPoolWait;
    }

    public long getDefaultPoolWait() {
	return defaultPoolWait;
    }

    /**
     * Pool initialization & destruction
     */

    /**
     * Initialize the pool
     */
    @SuppressWarnings("unchecked")
    public void init() {
	if (initializated == true) {
	    if (logger.isLoggable(Level.WARNING)) {
		logger.severe("Warning, double initialization of [" + this
			+ "]");
	    }
	    return;
	}

	initializated = true;
	metrics = new Metrics();

	// Create queues with maximum possible capacity
	availableQueue = new LinkedBlockingQueue<Wrapper<T>>(resourcesNumber);
	repairQueue = new LinkedBlockingQueue<Wrapper<T>>(resourcesNumber);

	// Create and start the repair threads.
	repairThreads = new FixedResourcePool.RepairThread[repairThreadsNumber];
	for (int i = 0; i < repairThreads.length; i++) {
	    repairThreads[i] = new RepairThread();
	    repairThreads[i].setName("REPAIR[" + i + "]:" + name);
	    repairThreads[i].start();
	}

	// Create resource wrappers with null content.
	for (int i = 0; i < resourcesNumber; i++) {
	    if (!repairQueue.offer(new Wrapper<T>(null)))
		throw new IllegalStateException(
			"What!? not enough space in the repairQueue to offer the element. This shouldn't happen!");
	}

	// Schedule a status report every 10 seconds.
	t = new Timer();
	t.schedule(new TimerTask() {
	    @Override
	    public void run() {
		if (logger.isLoggable(Level.FINE)) {
		    StringBuilder sb = new StringBuilder();
		    sb.append("**********************************");
		    sb.append("* Pool name:[" + name + "]");
		    sb.append("* resourcesCreated....:"
			    + metrics.getResourcesCreated());
		    sb.append("* failsReported.......:"
			    + metrics.getFailsReported());
		    sb.append("* fails...............:" + metrics.getFails());
		    sb.append("* resourcesCreated....:"
			    + metrics.getResourcesCreated());
		    sb.append("* resourcesProvided...:"
			    + metrics.getResourcesProvided());
		    sb.append("* resourcesReturned...:"
			    + metrics.getResourcesReturned());
		    sb
			    .append("* available size......:"
				    + availableQueue.size());
		    sb.append("* repair size.........:" + repairQueue.size());
		    sb.append("**********************************");
		    logger.fine(sb.toString());
		}

	    }
	}, 10000, 10000);

	if (logger.isLoggable(Level.INFO)) {
	    logger.info("Initialized [" + name + "]");
	}
    }

    /**
     * Destroy the pool. After being destroyed the pool can be initialized
     * again.
     */
    public void destroy() {
	checkInit();

	if (logger.isLoggable(Level.INFO)) {
	    logger.info("Destroying [" + getName() + "]...");
	}

	// Signal al threads to end
	finishing = true;

	if (logger.isLoggable(Level.INFO)) {
	    logger.info("Destroying [" + getName() + "] threads");
	}
	// Wait for the Repair Threas
	for (int i = 0; i < repairThreads.length; i++) {
	    boolean joined = false;
	    do {
		try {
		    repairThreads[i].interrupt();
		    repairThreads[i].join();
		    joined = true;
		} catch (InterruptedException e) {
		    if (logger.isLoggable(Level.SEVERE)) {
			logger.severe(e.getMessage());
		    }
		}
	    } while (!joined);
	}

	if (logger.isLoggable(Level.INFO)) {
	    logger.info("Waiting at most [" + getDestructionWait()
		    + "]ms for [" + getName() + "] resources to be returned.");
	}
	// Wait for all resources to be returned to the pool
	long t1 = System.currentTimeMillis();
	synchronized (this) {
	    while (!inUse.isEmpty()) {
		try {
		    this.wait(getDestructionWait());
		    long t2 = System.currentTimeMillis();
		    if ((t2 - t1 > getDestructionWait()) & !inUse.isEmpty()) {
			if (logger.isLoggable(Level.INFO)) {
			    logger
				    .info("Resource wait timeout. Forcing inUse resources destruction.");
			}
			for (T used : inUse.keySet()) {
			    destroyResource(used);
			}
		    }
		} catch (InterruptedException e) {
		    if (logger.isLoggable(Level.SEVERE)) {
			logger.severe(e.getMessage());
		    }
		}
	    }
	}

	if (logger.isLoggable(Level.INFO)) {
	    logger.info("Destroying [" + getName() + "] resources.");
	}
	// Destroy resources
	for (Wrapper<T> resource : availableQueue) {
	    destroyResource(resource.wrapped);
	}

	availableQueue.clear();
	availableQueue = null;

	for (Wrapper<T> resource : repairQueue) {
	    destroyResource(resource.wrapped);
	}

	repairQueue.clear();
	repairQueue = null;

	// Destroy metrics timer
	if (logger.isLoggable(Level.INFO)) {
	    logger.info("Shuting metrics timer for [" + getName() + "] down.");
	}
	t.cancel();
	t = null;

	// Reset metrics
	metrics = null;

	// Set states to initial values
	initializated = false;
	finishing = false;

	if (logger.isLoggable(Level.INFO)) {
	    logger.info("Pool [" + getName() + "] successfully destroyed.");
	}
    }

    protected void checkInit() {
	if (!initializated)
	    throw new IllegalStateException("Call the init() method first!");
    }

    /**
     * Returns true if wrapped resource needs validation
     * 
     * @param wrapper
     * @return
     */
    private boolean isValidationNeeded(Wrapper<T> wrapper) {
	// Add noise to the check times to avoid simultaneous resource checking.
	long noisyTimeBetweenCheck = (timeBetweenValidation - (long) ((Math
		.random() - 0.5) * (timeBetweenValidation / 10)));

	// Check if the resource need to be checked.
	return wrapper.getLastMark() + noisyTimeBetweenCheck < System
		.currentTimeMillis();
    }

    /**
     * Return a resource to the pool. When no longer needed.
     * 
     * @param resource
     */
    public void returnResource(T resource) {
	checkInit();

	Wrapper<T> wrapper;

	if (resource == null)
	    throw new IllegalArgumentException(
		    "The resource shouldn't be null.");

	// Delete the resource from the inUse list.
	synchronized (inUse) {
	    wrapper = inUse.remove(resource);
	}

	if (wrapper == null)
	    throw new IllegalArgumentException("The resource [" + resource
		    + "] isn't in the busy resources list.");

	if (isValidationNeeded(wrapper)) {
	    if (!repairQueue.offer(wrapper))
		throw new IllegalStateException(
			"This shouldn't happen. Offering to repair queue rejected.");
	} else {
	    if (!availableQueue.offer(wrapper))
		throw new IllegalStateException(
			"This shouldn't happen. Offering to available queue rejected.");
	}
	metrics.resourcesReturned++;

	if (finishing) {
	    synchronized (this) {
		this.notify();
	    }
	}
    }

    /**
     * Return a broken resource to the pool. If the application detects a
     * malfunction of the resource. This resources will go directly to the
     * repair queue.
     * 
     * @param resource
     */
    public void returnBrokenResource(T resource) {
	checkInit();
	Wrapper<T> wrapper;

	// Delete the resource from the inUse list.
	synchronized (inUse) {
	    wrapper = inUse.remove(resource);
	}

	if (wrapper == null)
	    throw new IllegalArgumentException("The resource [" + resource
		    + "] isn't in the busy resources list.");

	if (!repairQueue.offer(wrapper))
	    throw new IllegalStateException(
		    "This shouldn't happen. Offering to repair queue rejected.");
	metrics.resourcesReturned++;

	if (finishing) {
	    synchronized (this) {
		this.notify();
	    }
	}
    }

    /**
     * Get a resource from the pool waiting the default time.
     * {@link #setDefaultPoolWait(long)}
     * 
     * @return the resource of type T
     * @throws TimeoutException
     */
    public T getResource() throws TimeoutException {
	return getResource(defaultPoolWait);
    }

    /**
     * Get a resource from the pool.
     * 
     * @param maxTime
     *            Max time you would like to wait for the resource
     * @return
     * @throws TimeoutException
     */
    public T getResource(long maxTime) throws TimeoutException {
	if (finishing)
	    throw new IllegalStateException("Pool [" + getName()
		    + "] is currently being destroyed.");
	checkInit();

	final long tInit = System.currentTimeMillis();
	do {
	    try {
		long timeSpent = System.currentTimeMillis() - tInit;
		long timeToSleep = maxTime - timeSpent;
		timeToSleep = timeToSleep > 0 ? timeToSleep : 0;
		if (timeToSleep == 0)
		    throw new TimeoutException("" + timeSpent + ">" + maxTime);
		Wrapper<T> ret = availableQueue.poll(timeToSleep,
			TimeUnit.MILLISECONDS);
		if (ret != null) {
		    synchronized (inUse) {
			inUse.put(ret.wrapped, ret);
		    }
		    metrics.resourcesProvided++;
		    return ret.wrapped;
		}
	    } catch (InterruptedException e1) {
		if (logger.isLoggable(Level.SEVERE)) {
		    logger.severe(e1.getMessage());
		}
	    } // If the wait gets interrupted, doesn't matter but print it (just
	    // in case).
	} while (true);
    }

    /*
     * Implementation dependent methods. To be implemented.
     */

    /**
     * Create a resource for the pool. This method is always called from the
     * REPAIR threads. If it's impossible to create the resource, this method
     * should wait or hold until resource creation becomes possible.
     */
    protected abstract T createResource();

    /**
     * Check if the resource is still valid.
     * 
     * @param resource
     * @return
     */
    protected abstract boolean isResourceValid(T resource);

    /**
     * Destroy a resource.
     * 
     * @param resource
     */
    protected abstract void destroyResource(T resource);

    @Override
    public String toString() {
	return getName() + "[" + super.toString() + "]";
    }

    /**
     * Coming features: TODO Busy time check. Cron to check when a resource is
     * being taken for a long time.
     */

}