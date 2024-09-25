package heronarts.lx.quantize;

import heronarts.lx.LX;
import heronarts.lx.command.LXCommand;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.TriggerParameter;

import java.util.LinkedList;
import java.util.Objects;

/**
 * A BooleanParameter with quantizable user inputs
 */
public class QuantizeBooleanParameter extends BooleanParameter implements Quantizer.Listener {

  public final Quantizer quantizer;

  public final BooleanParameter control =
    new BooleanParameter("Control", false)
      .setDescription("State of the user control button. Indicates immediate user input.");

  public final BooleanParameter pending =
    new BooleanParameter("Pending", false)
      .setDescription("Whether a parameter state change is pending");

  // FIFO event queue
  private final LinkedList<Boolean> queue = new LinkedList<>();

  public QuantizeBooleanParameter(LX lx, String label) {
    this(lx, label, false);
  }

  public QuantizeBooleanParameter(LX lx, String label, boolean on) {
    this(label, on, new Quantizer(lx));
  }

  public QuantizeBooleanParameter(String label, Quantizer quantizer) {
    this(label, false, quantizer);
  }

  public QuantizeBooleanParameter(String label, boolean on, Quantizer quantizer) {
    super(label, on);
    this.control.setValue(on);
    this.quantizer = Objects.requireNonNull(quantizer);
    this.quantizer.addListener(this);
  }

  @Override
  public QuantizeBooleanParameter setValue(boolean value) {
    this.control.setValue(value);
    if (this.quantizer.enabled.isOn()) {
      this.queue.add(value);
      setPending(true);
    } else {
      super.setValue(value);
    }
    return this;
  }

  private void setPending(boolean value) {
    this.pending.setValue(value);
  }

  /**
   * State of Quantizer.enabled was changed
   */
  @Override
  public void onEnabled(Quantizer quantizer, boolean enabled) {
    // If we stopped delaying input events, run the queue immediately
    if (!this.quantizer.enabled.isOn()) {
      runQueue();
    }
  }

  /**
   * A quantize event occured.
   */
  @Override
  public void onEvent(Quantizer quantizer) {
    // If input events are delayed, run them.
    if (this.quantizer.enabled.isOn()) {
      runQueue();
    }
  }

  private void runQueue() {
    while (hasQueue()) {
      boolean value = this.queue.poll();
      setPending(hasQueue());
      super.setValue(value);

      // If MOMENTARY: latch for at least one cycle, but allow immediate re-latch.
      // TODO: avoid import of QuantizeTriggerParameter
      if (isMomentary() && super.getValueb() && !(this instanceof QuantizeTriggerParameter)) {
        break;
      }
    }
  }

  protected boolean hasQueue() {
    return !this.queue.isEmpty();
  }

  protected boolean isMomentary() {
    return this.getMode() == Mode.MOMENTARY;
  }

  @Override
  public QuantizeBooleanParameter setDescription(String description) {
    return (QuantizeBooleanParameter) super.setDescription(description);
  }

  @Override
  public QuantizeBooleanParameter setMode(Mode mode) {
    return (QuantizeBooleanParameter) super.setMode(mode);
  }

  @Override
  public void dispose() {
    this.quantizer.removeListener(this);
    // TODO: Don't dispose it if we didn't create it! (if an existing Quantizer was passed to constructor)
    this.quantizer.dispose();
    super.dispose();
  }
}
