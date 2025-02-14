package heronarts.lx.clip;

import com.google.gson.JsonObject;
import heronarts.lx.LX;
import heronarts.lx.utils.LXUtils;

public class OscClipEvent extends LXClipEvent<OscClipEvent> {

  public enum OscType {
    INT, FLOAT, STRING;
  }

  private OscType oscType = OscType.STRING;
  private String path = "";
  private int valueInt = 0;
  private float valueFloat = 0f;
  private String valueString = "";

  private final LX lx;

  public OscClipEvent(LX lx, OscClipLane lane) {
    super(lane);
    this.lx = lx;
  }

  public OscClipEvent setPath(String path) {
    this.path = path;
    return this;
  }

  public OscClipEvent setType(OscType oscType) {
    this.oscType = oscType;
    return this;
  }

  public OscClipEvent setValue(int value) {
    this.valueInt = value;
    return this;
  }

  public OscClipEvent setValue(float value) {
    this.valueFloat = value;
    return this;
  }

  public OscClipEvent setValue(String value) {
    this.valueString = value;
    return this;
  }

  public String getPath() {
    return this.path;
  }

  public OscType getType() {
    return this.oscType;
  }

  public int getValueInt() {
    return this.valueInt;
  }

  public float getValueFloat() {
    return this.valueFloat;
  }

  public String getValueString() {
    return this.valueString;
  }

  @Override
  public void execute() {
    // TODO: other checks for valid path?
    if (LXUtils.isEmpty(this.path)) {
      return;
    }

    switch (this.oscType) {
      case INT:
        this.lx.engine.osc.sendMessage(this.path, this.valueInt);
        break;
      case FLOAT:
        this.lx.engine.osc.sendMessage(this.path, this.valueFloat);
        break;
      case STRING:
        this.lx.engine.osc.sendMessage(this.path, this.valueString);
        break;
    }
  }

  protected static final String KEY_PATH = "path";
  protected static final String KEY_TYPE = "type";
  protected static final String KEY_VALUE_INT = "valueInt";
  protected static final String KEY_VALUE_FLOAT = "valueFloat";
  protected static final String KEY_VALUE_STRING = "valueString";

  @Override
  public void save(LX lx, JsonObject obj) {
    super.save(lx, obj);
    obj.addProperty(KEY_PATH, this.path);
    obj.addProperty(KEY_TYPE, this.oscType.name());
    obj.addProperty(KEY_VALUE_INT, this.valueInt);
    obj.addProperty(KEY_VALUE_FLOAT, this.valueFloat);
    obj.addProperty(KEY_VALUE_STRING, this.valueString);
  }
}
