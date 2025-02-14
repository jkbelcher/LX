package heronarts.lx.clip;

import com.google.gson.JsonObject;
import heronarts.lx.LX;

public class OscClipLane extends LXClipLane<OscClipEvent> {

  protected OscClipLane(LXClip clip) {
    super(clip);
  }

  @Override
  public String getPath() {
    return "OSC";
  }

  @Override
  public String getLabel() {
    return "OSC";
  }

  public OscClipEvent insertEvent(Cursor cursor) {
    OscClipEvent event = new OscClipEvent(this.lx, this);
    event.setCursor(cursor);
    super.insertEvent(event);
    return event;
  }

  @Override
  protected OscClipEvent loadEvent(LX lx, JsonObject eventObj) {
    String path = eventObj.get(OscClipEvent.KEY_PATH).getAsString();
    OscClipEvent.OscType oscType = OscClipEvent.OscType.valueOf(eventObj.get(OscClipEvent.KEY_TYPE).getAsString());
    int valueInt = eventObj.get(OscClipEvent.KEY_VALUE_INT).getAsInt();
    float valueFloat = eventObj.get(OscClipEvent.KEY_VALUE_FLOAT).getAsFloat();
    String valueString = eventObj.get(OscClipEvent.KEY_VALUE_STRING).getAsString();

    OscClipEvent event = new OscClipEvent(this.lx, this);
    event.setPath(path);
    event.setType(oscType);
    event.setValue(valueInt);
    event.setValue(valueFloat);
    event.setValue(valueString);
    return event;
  }

}
