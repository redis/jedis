package redis.clients.util;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Abstract resource pool of type T.
 * 
 * Needs implementation for creation, validation and destruction of the
 * resources.
 * 
 * @author Luis Darío Simonassi
 * 
 * @param <T>
 *            The type of the resource to be managed.
 */
public abstract class FixedResourcePool<T> {

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
    public class RepairThread extends Thread {
	public void run() {
	    while (true) {
		Wrapper<T> wrapper;
		try {
		    wrapper = repairQueue.poll(timeBetweenCheck,
			    TimeUnit.MILLISECONDS);
		    if (wrapper == null) {
			System.err
				.println("Warning!, maybe there are too many repair threads. Check configuration.["
					+ FixedResourcePool.this + "]");
			continue;
		    }
		} catch (InterruptedException e) {
		    e.printStackTrace();
		    continue;
		}
		T wrapped = wrapper.wrapped;
		boolean valid = false;
		if (wrapped != null) {
		    valid = isResourceValid(wrapped);
		    if (!valid)
			fails++;
		}
		if (!valid) {
		    T replace = createResource();
		    resourcesCreated++;
		    wrapper.wrapped = replace;
		    if (wrapped != null)
			destroyResource(wrapped);
		}
		wrapper.mark();
		if (!availableQueue.offer(wrapper)) {
		    System.err
			    .println("This shouldn't happen, offering to available was rejected.");
		}
	    }
	}
    }

    /*
     * Pool statistics
     */

    volatile long failsReported = 0;
    volatile long fails = 0;
    volatile long resourcesCreated = 0;
    volatile long resourcesProvided = 0;
    volatile long resourcesReturned = 0;

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

    /*
     * Pool status structures
     */
    private LinkedBlockingQueue<Wrapper<T>> availableQueue;
    private LinkedBlockingQueue<Wrapper<T>> repairQueue;
    private HashMap<T, Wrapper<T>> inUse = new HashMap<T, Wrapper<T>>();
    private RepairThread[] repairThreads;

    /*
     * Pool parameters
     */
    int resourcesNumber = 10;
    int repairThreadsNumber = 3;
    long timeBetweenCheck = 150000;
    private boolean init = false;

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
	this.repairThreadsNumber = repairThreadsNumber;
    }

    public long getTimeBetweenCheck() {
	return timeBetweenCheck;
    }

    public void setTimeBetweenCheck(long timeBetweenCheck) {
	this.timeBetweenCheck = timeBetweenCheck;
    }

    /**
     * Initialize the pool
     */
    @SuppressWarnings("unchecked")
    public void init() {
	if (init == true) {
	    System.err.println("Warning, double initialization of [" + this
		    + "]");
	    return;
	}
	init = true;
	// Create queues with maximum possible capacity
	availableQueue = new LinkedBlockingQueue<Wrapper<T>>(resourcesNumber);
	repairQueue = new LinkedBlockingQueue<Wrapper<T>>(resourcesNumber);

	// Create and start the repair threads.
	repairThreads = new FixedResourcePool.RepairThread[repairThreadsNumber];
	for (int i = 0; i < repairThreads.length; i++) {
	    repairThreads[i] = new RepairThread();
	    repairThreads[i].setName("REPAIR[" + i + "]:" + this.toString());
	    repairThreads[i].start();
	}

	// Create resource wrappers with null content.
	for (int i = 0; i < resourcesNumber; i++) {
	    if (!repairQueue.offer(new Wrapper<T>(null)))
		throw new IllegalStateException(
			"What!? not enough space in the repairQueue to offer the element. This shouldn't happen!");
	}
    }

    /**
     * Return a resource to the pool. When no longer needed.
     * 
     * @param resource
     */
    public void returnResource(T resource) {
	if (!init)
	    throw new IllegalStateException("Call the init() method first!");

	Wrapper<T> wrapper;

	if (resource == null)
	    throw new IllegalArgumentException(
		    "The resource shouldn't be null.");

	// Delete the resource from the inUse list.
	synchronized (inUse) {
	    wrapper = inUse.remove(resource);
	}

	if (wrapper == null)
	    throw new IllegalArgumentException("El recurso [" + resource
		    + "] no est� en la lista de recursos en uso de este pool.");

	// Add noise to the check times to avoid simultaneous resource checking.
	long noisyTimeBetweenCheck = (timeBetweenCheck - (long) ((Math.random() - 0.5) * (timeBetweenCheck / 10)));

	// Check if the resource need to be checked.
	if (wrapper.getLastMark() + noisyTimeBetweenCheck < System
		.currentTimeMillis()) {
	    if (!repairQueue.offer(wrapper))
		throw new IllegalStateException(
			"This shouldn't happen. Offering to repair queue rejected.");
	} else {
	    if (!availableQueue.offer(wrapper))
		throw new IllegalStateException(
			"This shouldn't happen. Offering to available queue rejected.");
	}
	resourcesReturned++;
    }

    /**
     * Return a broken resource to the pool. If the application detects a
     * malfunction of the resource. This resources will go directly to the
     * repair queue.
     * 
     * @param resource
     */
    public void returnBrokenResource(T resource) {
	if (!init)
	    throw new IllegalStateException("Call the init() method first!");
	Wrapper<T> wrapper;

	// Delete the resource from the inUse list.
	synchronized (inUse) {
	    wrapper = inUse.remove(resource);
	}

	if (wrapper == null)
	    throw new IllegalArgumentException("El recurso [" + resource
		    + "] no est� en la lista de recursos en uso de este pool.");

	if (!repairQueue.offer(wrapper))
	    throw new IllegalStateException(
		    "This shouldn't happen. Offering to repair queue rejected.");
	resourcesReturned++;
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
	if (!init)
	    throw new IllegalStateException("Call the init() method first!");
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
		    resourcesProvided++;
		    return ret.wrapped;
		}
	    } catch (InterruptedException e1) {
		e1.printStackTrace();
	    } // If the wait gets interrupted, doesn't matter but print it (just
	      // in case).
	} while (true);
    }

    /*
     * Implementation dependent methods. To be implemented.
     */

    /**
     * Create a resource for the pool
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

    /**
     * Coming features: TODO Pool destruction. Down resources/threads and wait.
     * TODO Busy time check. Cron to check when a resource is being taken for a
     * long time. TODO Validation of long time idle objects
     */

}