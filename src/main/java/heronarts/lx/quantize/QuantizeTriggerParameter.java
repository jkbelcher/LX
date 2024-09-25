package heronarts.lx.quantize;

import heronarts.lx.LX;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameterListener;

/**
 * This is a copy/paste of TriggerParameter, except with a different base class.
 * J: Guessing we'll want to remove this code duplication once everything else is figured out.
 */
public class QuantizeTriggerParameter extends QuantizeBooleanParameter {

  private Runnable onTrigger = null;

  public QuantizeTriggerParameter(LX lx, String label) {
    this(lx, label, null);
  }

  public QuantizeTriggerParameter(LX lx, String label, Runnable onTrigger) {
    super(lx, label, false);
    setMode(Mode.MOMENTARY);
    addListener(this.listener);
    onTrigger(onTrigger);
  }

  @Override
  public QuantizeTriggerParameter setDescription(String description) {
    return (QuantizeTriggerParameter) super.setDescription(description);
  }

  private final LXParameterListener listener = p -> {
    if (isOn()) {
      if (this.onTrigger != null) {
        this.onTrigger.run();
      }
      setValue(false);
    }
  };

  public QuantizeTriggerParameter onTrigger(Runnable onTrigger) {
    if (this.onTrigger != null) {
      LX.error(new Exception(), "WARNING / SHOULDFIX: Overwriting previous onTrigger on QuantizeTriggerParameter: " + getCanonicalPath());
    }
    this.onTrigger = onTrigger;
    return this;
  }

  public QuantizeTriggerParameter trigger() {
    setValue(true);
    return this;
  }

  @Override
  public QuantizeTriggerParameter setMode(Mode mode) {
    if (mode != Mode.MOMENTARY) {
      throw new IllegalArgumentException("QuantizeTriggerParameter may only have MOMENTARY mode");
    }
    super.setMode(mode);
    return this;
  }

  @Override
  public void dispose() {
    removeListener(this.listener);
    this.onTrigger = null;
    super.dispose();
  }

}
