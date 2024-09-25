package heronarts.lx.quantize;

import heronarts.lx.LX;
import heronarts.lx.parameter.BooleanParameter;

/**
 * A helper utility to delay method calls until a quantize event.
 * When disabled, input events will result in immediate output.
 *
 * Method callback will only occur after primed by run().
 */
public class MethodQuantizer implements Runnable, Quantizer.Listener {

  public final Quantizer quantizer;

  private int numPending;
  public final BooleanParameter pending =
    new BooleanParameter("Pending", false)
      .setDescription("Whether target method will be called at the next quantize event");

  private Runnable callback;

  /**
   * What to do if run() was called multiple times between quantize events
   */
  public static enum CallbackMode {
    /**
     * callback is called a maximum of once per quantize event, even if run() was called multiple times
     */
    SINGLE,
    /**
     * callback is called once for each call to run()
     */
    MULTIPLE
  }

  private CallbackMode mode = CallbackMode.SINGLE;

  public MethodQuantizer(LX lx, Runnable callback) {
    this(callback, new Quantizer(lx));
  }

  public MethodQuantizer(Runnable callback, Quantizer quantizer) {
    this.quantizer = quantizer;
    this.quantizer.addListener(this);
    setCallback(callback);
  }

  /**
   * Set the method to be run when a quantize event occurs, if preceded by a call to run().
   * @param callback A runnable method to call
   * @return this
   */
  public MethodQuantizer setCallback(Runnable callback) {
    if (this.callback != null) {
      throw new IllegalStateException("Callback already set for MethodQuantizer");
    }
    this.callback = callback;
    return this;
  }

  /**
   * Specify behavior if queue() were to be called multiple times between quantize events
   */
  public MethodQuantizer setMode(CallbackMode mode) {
    this.mode = mode;
    return this;
  }

  /**
   * Checks whether Quantizer will run on the next quantize event.
   */
  public boolean isPending() {
    return this.numPending > 0;
  }

  /**
   * Input side of Quantizer. Callback will fire after next event, or,
   * if quantize is disabled, callback will fire immediately.
   */
  @Override
  public void run() {
    if (this.quantizer.enabled.isOn()) {
      this.numPending++;
      this.pending.setValue(true);
    } else {
      // If quantize is disabled, call back immediately.
      fire();
    }
  }

  @Override
  public void onEnabled(Quantizer quantizer, boolean enabled) {
    if (!enabled && isPending()) {
      fire();
    }
  }

  /**
   * A quantize event occurred
   */
  @Override
  public void onEvent(Quantizer quantizer) {
    if (isPending()) {
      fire();
    }
  }

  /**
   * Go time
   */
  private void fire() {
    int pends = this.numPending;
    // Reset before callbacks in case of immediate re-queue
    _reset();

    if (this.callback != null) {
      if (this.mode == CallbackMode.MULTIPLE) {
        while (pends > 0) {
          this.callback.run();
          pends--;
        }
      } else {
        this.callback.run();
      }
    }
  }

  /**
   * Clear the queue count. The next quantize event will result in no callbacks.
   */
  public MethodQuantizer reset() {
    _reset();
    return this;
  }

  private void _reset() {
    if (this.numPending > 0) {
      this.numPending = 0;
      this.pending.setValue(false);
    }
  }

  public void dispose() {
    this.quantizer.removeListener(this);
  }
}
