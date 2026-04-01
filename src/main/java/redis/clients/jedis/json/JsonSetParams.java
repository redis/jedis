package redis.clients.jedis.json;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.params.IParams;

public class JsonSetParams implements IParams {

  private boolean nx = false;
  private boolean xx = false;
  private FphaType fpha;

  /**
   * Floating-point high accuracy types for JSON numeric values.
   * @since 7.5
   */
  public enum FphaType {
    FP16, BF16, FP32, FP64
  }

  public JsonSetParams() {
  }

  public static JsonSetParams jsonSetParams() {
    return new JsonSetParams();
  }

  public JsonSetParams nx() {
    this.nx = true;
    this.xx = false;
    return this;
  }

  public JsonSetParams xx() {
    this.nx = false;
    this.xx = true;
    return this;
  }

  /**
   * Set the floating-point high accuracy type to FP16.
   * @return JsonSetParams
   * @since 7.5
   */
  public JsonSetParams fp16() {
    this.fpha = FphaType.FP16;
    return this;
  }

  /**
   * Set the floating-point high accuracy type to BF16.
   * @return JsonSetParams
   * @since 7.5
   */
  public JsonSetParams bf16() {
    this.fpha = FphaType.BF16;
    return this;
  }

  /**
   * Set the floating-point high accuracy type to FP32.
   * @return JsonSetParams
   * @since 7.5
   */
  public JsonSetParams fp32() {
    this.fpha = FphaType.FP32;
    return this;
  }

  /**
   * Set the floating-point high accuracy type to FP64.
   * @return JsonSetParams
   * @since 7.5
   */
  public JsonSetParams fp64() {
    this.fpha = FphaType.FP64;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (nx) {
      args.add(Keyword.NX);
    }
    if (xx) {
      args.add(Keyword.XX);
    }
    if (fpha != null) {
      args.add("FPHA");
      args.add(fpha);
    }
  }
}
