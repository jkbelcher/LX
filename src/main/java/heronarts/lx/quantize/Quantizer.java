package heronarts.lx.quantize;

import heronarts.lx.LX;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.utils.ListenerCollection;

/**
 * Abstracts the common functions for a quantizable component, including
 * an Enabled flag and a selectable QuantizeSource.
 *
 * Listeners will be notified for every quantize event.
 *
 * Multiple consumers/parameters could reference the same quantizer.
 * Thinking this would be the case for Clip launching.
 */
public class Quantizer implements QuantizeSource.Listener {

  public interface Listener {
    /**
     * State of Quantizer.enabled was changed
     */
    default public void onEnabled(Quantizer quantizer, boolean enabled) { }

    /**
     * A quantize event occured.
     */
    public void onEvent(Quantizer quantizer);
  }
  private final ListenerCollection<Listener> listeners = new ListenerCollection<>();

  public final BooleanParameter enabled =
    new BooleanParameter("Enabled", false)
      .setDescription("Whether inputs will be delayed until the next quantize event. If False, inputs will result in immediate output.");

  public final QuantizeEngine.Selector source;
  private QuantizeSource registeredSource;

  private final LXParameterListener enabledListener = (p) -> {
    final boolean enabled = this.enabled.isOn();
    for (Listener listener : listeners) {
      listener.onEnabled(this, enabled);
    }
  };
  private final LXParameterListener sourceListener;

  public Quantizer(LX lx) {
    this(lx, true);
  }

  public Quantizer(LX lx, boolean enabled) {
    this.source = lx.quantize.newSourceSelector("Source", "Quantize Event Source");
    // sourceListener references this.source, so cannot be created until after this.source is initialized
    this.source.addListener(this.sourceListener = (p) -> {
        QuantizeSource source = this.source.getObject();
        if (this.registeredSource != null) {
          this.registeredSource.removeListener(this);
        }
        this.registeredSource = source;
        if (this.registeredSource != null) {
          this.registeredSource.addListener(this);
        }
      }, true);
    this.enabled.addListener(this.enabledListener);
    this.enabled.setValue(enabled);
  }

  /**
   * Set enabled state of Quantizer. This is a chainable method for setup convenience.
   * @param on Whether Quantizer is enabled
   * @return this
   */
  public Quantizer setEnabled(boolean on) {
    this.enabled.setValue(on);
    return this;
  }

  /**
   * A quantize event occurred
   */
  @Override
  public void onEvent(QuantizeSource source) {
    if (this.enabled.isOn()) {
      // Notify listeners of every event
      for (Listener listener : this.listeners) {
        listener.onEvent(this);
      }
    }
  }

  public Quantizer addListener(Listener listener) {
    this.listeners.add(listener);
    return this;
  }

  public Quantizer removeListener(Listener listener) {
    this.listeners.remove(listener);
    return this;
  }

  public void dispose() {
    this.source.removeListener(this.sourceListener);
    this.enabled.removeListener(this.enabledListener);
    if (this.registeredSource != null) {
      this.registeredSource.removeListener(this);
      this.registeredSource = null;
    }
  }

}
