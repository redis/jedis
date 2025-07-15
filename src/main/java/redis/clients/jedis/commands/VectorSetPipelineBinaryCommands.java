package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.Response;
import redis.clients.jedis.params.VAddParams;
import redis.clients.jedis.params.VSimParams;
import redis.clients.jedis.resps.RawVector;
import redis.clients.jedis.resps.VSimResult;

/**
 * Interface for Redis Vector Set binary pipeline commands.
 * Vector sets are a new data type introduced in Redis 8.0 for vector similarity operations.
 */
public interface VectorSetPipelineBinaryCommands {

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vadd/">VADD Command</a></b>
   * Add a new element into the vector set specified by key.
   * <p>
   * Time complexity: O(log(N)) for each element added, where N is the number of elements in the vector set.
   * @param key the name of the key that will hold the vector set data
   * @param vector the vector as floating point numbers
   * @param element the name of the element that is being added to the vector set
   * @return Response wrapping 1 if key was added; 0 if key was not added
   */
  Response<Boolean> vadd(byte[] key, float[] vector, byte[] element);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vadd/">VADD Command</a></b>
   * Add a new element into the vector set specified by key with additional parameters.
   * <p>
   * Time complexity: O(log(N)) for each element added, where N is the number of elements in the vector set.
   * @param key the name of the key that will hold the vector set data
   * @param vector the vector as floating point numbers
   * @param element the name of the element that is being added to the vector set
   * @param params additional parameters for the VADD command
   * @return Response wrapping 1 if key was added; 0 if key was not added
   */
  Response<Boolean> vadd(byte[] key, float[] vector, byte[] element, VAddParams params);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vadd/">VADD Command</a></b>
   * Add a new element into the vector set specified by key using FP32 binary format.
   * <p>
   * Time complexity: O(log(N)) for each element added, where N is the number of elements in the vector set.
   * @param key the name of the key that will hold the vector set data
   * @param vectorBlob the vector as FP32 binary blob
   * @param element the name of the element that is being added to the vector set
   * @return Response wrapping 1 if key was added; 0 if key was not added
   */
  Response<Boolean> vaddFP32(byte[] key, byte[] vectorBlob, byte[] element);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vadd/">VADD Command</a></b>
   * Add a new element into the vector set specified by key using FP32 binary format with additional parameters.
   * <p>
   * Time complexity: O(log(N)) for each element added, where N is the number of elements in the vector set.
   * @param key the name of the key that will hold the vector set data
   * @param vectorBlob the vector as FP32 binary blob
   * @param element the name of the element that is being added to the vector set
   * @param params additional parameters for the VADD command
   * @return Response wrapping 1 if key was added; 0 if key was not added
   */
  Response<Boolean> vaddFP32(byte[] key, byte[] vectorBlob, byte[] element, VAddParams params);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vadd/">VADD Command</a></b>
   * Add a new element into the vector set specified by key with dimension reduction and additional parameters.
   * <p>
   * Time complexity: O(log(N)) for each element added, where N is the number of elements in the vector set.
   * @param key the name of the key that will hold the vector set data
   * @param vector the vector as floating point numbers
   * @param element the name of the element that is being added to the vector set
   * @param reduceDim the target dimension after reduction using random projection
   * @param params additional parameters for the VADD command
   * @return Response wrapping 1 if key was added; 0 if key was not added
   */
  Response<Boolean> vadd(byte[] key, float[] vector, byte[] element, int reduceDim, VAddParams params);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vadd/">VADD Command</a></b>
   * Add a new element into the vector set specified by key using FP32 binary format with dimension reduction and additional parameters.
   * <p>
   * Time complexity: O(log(N)) for each element added, where N is the number of elements in the vector set.
   * @param key the name of the key that will hold the vector set data
   * @param vectorBlob the vector as FP32 binary blob
   * @param element the name of the element that is being added to the vector set
   * @param reduceDim the target dimension after reduction using random projection
   * @param params additional parameters for the VADD command
   * @return Response wrapping 1 if key was added; 0 if key was not added
   */
  Response<Boolean> vaddFP32(byte[] key, byte[] vectorBlob, byte[] element, int reduceDim, VAddParams params);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vsim/">VSIM Command</a></b>
   * Return elements similar to a given vector.
   * <p>
   * Time complexity: O(log(N)) where N is the number of elements in the vector set.
   * @param key the name of the key that holds the vector set data
   * @param vector the vector to use as similarity reference
   * @return Response wrapping list of similar elements
   */
  Response<List<byte[]>> vsim(byte[] key, float[] vector);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vsim/">VSIM Command</a></b>
   * Return elements similar to a given vector with additional parameters.
   * <p>
   * Time complexity: O(log(N)) where N is the number of elements in the vector set.
   * @param key the name of the key that holds the vector set data
   * @param vector the vector to use as similarity reference
   * @param params additional parameters for the VSIM command
   * @return Response wrapping VSimResult containing similar elements and optionally their scores
   */
  Response<VSimResult> vsim(byte[] key, float[] vector, VSimParams params);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vsim/">VSIM Command</a></b>
   * Return elements similar to a given element in the vector set.
   * <p>
   * Time complexity: O(log(N)) where N is the number of elements in the vector set.
   * @param key the name of the key that holds the vector set data
   * @param element the name of the element to use as similarity reference
   * @return Response wrapping list of similar elements
   */
  Response<List<byte[]>> vsimByElement(byte[] key, byte[] element);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vsim/">VSIM Command</a></b>
   * Return elements similar to a given element in the vector set with additional parameters.
   * <p>
   * Time complexity: O(log(N)) where N is the number of elements in the vector set.
   * @param key the name of the key that holds the vector set data
   * @param element the name of the element to use as similarity reference
   * @param params additional parameters for the VSIM command
   * @return Response wrapping VSimResult containing similar elements and optionally their scores
   */
  Response<VSimResult> vsimByElement(byte[] key, byte[] element, VSimParams params);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vsim/">VSIM Command</a></b>
   * Return elements similar to a given vector using FP32 binary format.
   * <p>
   * Time complexity: O(log(N)) where N is the number of elements in the vector set.
   * @param key the name of the key that holds the vector set data
   * @param vectorBlob the vector as FP32 binary blob to use as similarity reference
   * @return Response wrapping list of similar elements
   */
  Response<List<byte[]>> vsimFP32(byte[] key, byte[] vectorBlob);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vsim/">VSIM Command</a></b>
   * Return elements similar to a given vector using FP32 binary format with additional parameters.
   * <p>
   * Time complexity: O(log(N)) where N is the number of elements in the vector set.
   * @param key the name of the key that holds the vector set data
   * @param vectorBlob the vector as FP32 binary blob to use as similarity reference
   * @param params additional parameters for the VSIM command
   * @return Response wrapping VSimResult containing similar elements and optionally their scores
   */
  Response<VSimResult> vsimFP32(byte[] key, byte[] vectorBlob, VSimParams params);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vdim/">VDIM Command</a></b>
   * Return the number of dimensions of the vectors in the specified vector set.
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the vector set
   * @return Response wrapping the number of vector set elements
   */
  Response<Long> vdim(byte[] key);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vcard/">VCARD Command</a></b>
   * Return the number of elements in the specified vector set.
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the vector set
   * @return Response wrapping the number of elements in the vector set
   */
  Response<Long> vcard(byte[] key);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vemb/">VEMB Command</a></b>
   * Return the approximate vector associated with a given element in the vector set.
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the vector set
   * @param element the name of the element whose vector you want to retrieve
   * @return Response wrapping list of real numbers representing the vector
   */
  Response<List<Double>> vemb(byte[] key, byte[] element);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vemb/">VEMB Command</a></b>
   * Return the raw vector data associated with a given element in the vector set.
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the vector set
   * @param element the name of the element whose vector you want to retrieve
   * @return Response wrapping RawVector containing raw vector data, quantization type, and metadata
   */
  Response<RawVector> vembRaw(byte[] key, byte[] element);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vrem/">VREM Command</a></b>
   * Remove an element from a vector set.
   * <p>
   * Time complexity: O(log(N)) for each element removed, where N is the number of elements in the vector set
   * @param key the name of the key that holds the vector set
   * @param element the name of the element to remove from the vector set
   * @return Response wrapping true if the element was removed, false if either element or key do not exist
   */
  Response<Boolean> vrem(byte[] key, byte[] element);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vlinks/">VLINKS Command</a></b>
   * Return the neighbors of a specified element in a vector set.
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the vector set
   * @param element the name of the element whose HNSW neighbors you want to inspect
   * @return Response wrapping list of neighbor element names
   */
  Response<List<byte[]>> vlinks(byte[] key, byte[] element);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vlinks/">VLINKS Command</a></b>
   * Return the neighbors of a specified element in a vector set with similarity scores.
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the vector set
   * @param element the name of the element whose HNSW neighbors you want to inspect
   * @return Response wrapping map of neighbor element names to similarity scores
   */
  Response<Map<byte[], Double>> vlinksWithScores(byte[] key, byte[] element);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vrandmember/">VRANDMEMBER Command</a></b>
   * Return a random element from a vector set.
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the vector set
   * @return Response wrapping a random element name, or null if the key does not exist
   */
  Response<byte[]> vrandmember(byte[] key);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vrandmember/">VRANDMEMBER Command</a></b>
   * Return random elements from a vector set.
   * <p>
   * Time complexity: O(N) where N is the absolute value of the count argument
   * @param key the name of the key that holds the vector set
   * @param count the number of elements to return. Positive values return distinct elements; negative values allow duplicates
   * @return Response wrapping list of random element names
   */
  Response<List<byte[]>> vrandmember(byte[] key, int count);

  /**
   * <b><a href="https://redis.io/docs/latest/commands/vgetattr/">VGETATTR Command</a></b>
   * Get the attributes of an element in a vector set.
   * <p>
   * Time complexity: O(1)
   * @param key the name of the key that holds the vector set
   * @param element the name of the element whose attributes to retrieve
   * @return Response wrapping the attributes of the element as a JSON string, or null if the element doesn't exist or has no attributes
   */
  Response<byte[]> vgetattr(byte[] key, byte[] element);
}
