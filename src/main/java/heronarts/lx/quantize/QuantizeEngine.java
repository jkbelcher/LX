package heronarts.lx.quantize;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.parameter.ObjectParameter;
import heronarts.lx.utils.ListenerCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuantizeEngine extends LXComponent {

  public interface Listener {
    public void quantizeSourceAdded(QuantizeEngine engine, QuantizeSource source);
    public void quantizeSourceRemoved(QuantizeEngine engine, QuantizeSource source);
  }

  private final ListenerCollection<Listener> listeners = new ListenerCollection<>();

  private final List<QuantizeSource> mutableSources = new ArrayList<QuantizeSource>();
  public final List<QuantizeSource> sources = Collections.unmodifiableList(this.mutableSources);

  public QuantizeEngine(LX lx) {
    super(lx);
  }

  public QuantizeEngine addListener(Listener listener) {
    listeners.add(listener);
    return this;
  }

  public QuantizeEngine removeListener(Listener listener) {
    listeners.remove(listener);
    return this;
  }

  public QuantizeEngine addSource(QuantizeSource source) {
    return addSource(source, -1);
  }

  public QuantizeEngine addSource(QuantizeSource source, int index) {
    if (index < 0) {
      this.mutableSources.add(source);
    } else {
      this.mutableSources.add(index, source);
    }
    // _reindexSources();
    for (Listener listener : this.listeners) {
      listener.quantizeSourceAdded(this, source);
    }
    updateSelectors();
    return this;
  }

  public QuantizeEngine removeSource(QuantizeSource source) {
    if (!this.sources.contains(source)) {
      throw new IllegalStateException("Cannot remove unknown source from QuantizeEngine: " + source);
    }
    this.mutableSources.remove(source);
    // _reindexSources();
    for (Listener listener : this.listeners) {
      listener.quantizeSourceRemoved(this, source);
    }
    updateSelectors();
    // LX.dispose(source);
    return this;
  }

/*  private void _reindexSources() {
    int i = 0;
    for (QuantizeSource source : this.sources) {
      source.setIndex(i++);
    }
  }*/

  private QuantizeSource[] selectorObjects = { };
  private String[] selectorOptions = { };

  private final List<Selector> selectors = new ArrayList<Selector>();

  private void updateSelectors() {
    int numOptions = this.sources.size();
    this.selectorObjects = new QuantizeSource[numOptions];
    this.selectorOptions = new String[numOptions];

    int i = 0;
    for (QuantizeSource source : this.sources) {
      this.selectorObjects[i] = source;
      this.selectorOptions[i] = source.getLabel();
      ++i;
    }

    // Update all selectors to have new range/options
    for (Selector selector : this.selectors) {
      final QuantizeSource selected = selector.getObject();
      selector.setObjects(this.selectorObjects, this.selectorOptions);
      if ((selected != selector.getObject()) && this.sources.contains(selected)) {
        selector.setValue(selected);
      }
    }
  }

  public Selector newSourceSelector(String label, String description) {
    return (Selector) new Selector(label).setDescription(description);
  }

  public class Selector extends ObjectParameter<QuantizeSource> {
    private Selector(String label) {
      this(label, selectorObjects, selectorOptions);
    }
    private Selector(String label, QuantizeSource[] objects, String[] options) {
      super(label, objects, options);
      selectors.add(this);
    }

    @Override
    public void dispose() {
      selectors.remove(this);
      super.dispose();
    }
  }

}
