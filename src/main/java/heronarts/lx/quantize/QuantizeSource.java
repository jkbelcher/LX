package heronarts.lx.quantize;

/**
 * Classes implementing QuantizeSource throw a quantizable event.
 */
public interface QuantizeSource {

  static public interface Listener {
    public void onEvent(QuantizeSource source);
  }

  public QuantizeSource addListener(Listener listener);
  public QuantizeSource removeListener(Listener listener);

  public String getLabel();

}
