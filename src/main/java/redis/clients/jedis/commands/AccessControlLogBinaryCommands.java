package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.resps.AccessControlUser;

/**
 * This class provides the interfaces necessary to interact with Access Control Lists (ACLs) within
 * redis. These are the interfaces for binary (i.e. non-decoded) interactions.
 * @see <a href="https://redis.io/topics/acl">Redis ACL Guide</a>
 */
public interface AccessControlLogBinaryCommands {

  /**
   * Returns the username used to authenticate the current connection.
   * @see <a href="https://redis.io/commands/acl-whoami">ACL WHOAMI</a>
   * @return The username used for the current connection
   */
  byte[] aclWhoAmIBinary();

  /**
   * Generate a random password
   * @see <a href="https://redis.io/commands/acl-genpass">ACL GENPASS</a>
   * @return A random password
   */
  byte[] aclGenPassBinary();

  /**
   * Generate a random password
   * @param bits the number of output bits
   * @return A random password
   */
  byte[] aclGenPassBinary(int bits);

  /**
   * Returns the currently active ACL rules on the Redis Server
   * @see <a href="https://redis.io/commands/acl-list">ACL LIST</a>
   * @return An array of ACL rules
   */
  List<byte[]> aclListBinary();

  /**
   * Shows a list of all usernames currently configured with access control lists (ACL).
   * @see <a href="https://redis.io/commands/acl-users">ACL USERS</a>
   * @return list of users
   */
  List<byte[]> aclUsersBinary();

  /**
   * The command returns all the rules defined for an existing ACL user.
   * @param name username
   * @return a list of ACL rule definitions for the user.
   */
  AccessControlUser aclGetUser(byte[] name);

  /**
   * Create an ACL for the specified user with the default rules.
   * @param name user who receives an acl
   * @see <a href="https://redis.io/commands/acl-setuser">ACL SETUSER</a>
   * @return A string containing OK on success
   */
  String aclSetUser(byte[] name);

  /**
   * Create an ACL for the specified user, while specifying the rules.
   * @param name user who receives an acl
   * @param rules the acl rules for the specified user
   * @see <a href="https://redis.io/commands/acl-setuser">ACL SETUSER</a>
   * @return A string containing OK on success
   */
  String aclSetUser(byte[] name, byte[]... rules);

  /**
   * Delete the specified user, from the ACL.
   * @param names The username to delete
   * @see <a href="https://redis.io/commands/acl-deluser">ACL DELUSER</a>
   * @return The number of users delete
   */
  long aclDelUser(byte[]... names);

  /**
   * Show the available ACL categories.
   * @see <a href="https://redis.io/commands/acl-cat">ACL CAT</a>
   * @return the available ACL categories
   */
  List<byte[]> aclCatBinary();

  /**
   * Show the available ACLs for a given category.
   * @param category The category for which to list available ACLs
   * @see <a href="https://redis.io/commands/acl-cat">ACL CAT</a>
   * @return the available ACL categories
   */
  List<byte[]> aclCat(byte[] category);

  /**
   * Shows the recent ACL security events.
   * @see <a href="https://redis.io/commands/acl-log">ACL LOG</a>
   * @return The list of recent security events
   */
  List<byte[]> aclLogBinary();

  /**
   * Shows the recent limit ACL security events.
   * @param limit The number of results to return
   * @see <a href="https://redis.io/commands/acl-log">ACL LOG</a>
   * @return The list of recent security events
   */
  List<byte[]> aclLogBinary(int limit);

  /**
   * Reset the script event log
   * @see <a href="https://redis.io/commands/acl-log">ACL LOG</a>
   * @return The OK string
   */
  String aclLogReset();

  /**
   * This function tells Redis to reload its external ACL rules, when Redis is configured with an
   * external ACL file
   * @see <a href="https://redis.io/commands/acl-load">ACL LOAD</a>
   * @return OK or error text
   */
  String aclLoad();

  /**
   * Save the currently defined in-memory ACL to disk.
   * @see <a href="https://redis.io/commands/acl-save">ACL SAVE</a>
   * @return OK on success
   */
  String aclSave();

  byte[] aclDryRunBinary(byte[] username, byte[] command, byte[]... args);

  byte[] aclDryRunBinary(byte[] username, CommandArguments commandArgs);
}
