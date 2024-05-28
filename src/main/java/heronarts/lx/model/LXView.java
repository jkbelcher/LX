/**
 * Copyright 2021- Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.lx.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import heronarts.lx.LX;

public class LXView extends LXModel {

  public enum Normalization {
    RELATIVE("Normalize to View"),
    ABSOLUTE("Preserve Absolute");

    public final String description;

    private Normalization(String description) {
      this.description = description;
    }

    @Override
    public String toString() {
      return this.description;
    }
  }

  private static final String GROUP_SEPARATOR = "\\s*;\\s*";
  private static final String SELECTOR_SEPARATOR = "\\s*,\\s*";
  private static final char GROUP_OPERATOR = '*';

  private static class ParseState {

    private final LXModel model;
    private final List<List<LXModel>> groups = new ArrayList<List<LXModel>>();
    private final List<LXModel> uniqueSubmodels = new ArrayList<LXModel>();

    private ParseState(LXModel model) {
      this.model = model;
    }
  }

  /**
   * Constructs a view of the given model object
   *
   * @param model Model Parent model to create view of
   * @param viewSelector View selection string
   * @param normalization What normalization mode to use for this view
   * @return A view of the model that selects the elements in the selector string
   */
  public static LXView create(final LXModel model, String viewSelector, Normalization normalization) {
    ParseState state = new ParseState(model);

    // Split at top-level by groups, separated by ;
    for (String groupSelector : viewSelector.trim().split(GROUP_SEPARATOR)) {
      parseGroup(state, groupSelector);
    }

    // We now have a set of unique submodels, organized by group. Each submodel
    // belongs strictly to one group.
    //
    // Construct a new list of a copy of all the points from all the models
    // in the view. We need a copy because these will all be re-normalized
    // with xn/yn/zn values relative to this view
    final Map<Integer, LXPoint> clonedPoints = new HashMap<Integer, LXPoint>();
    final LXView[] views = new LXView[state.groups.size()];
    final List<LXPoint> allPoints = new ArrayList<LXPoint>();
    int g = 0;
    for (List<LXModel> group : state.groups) {
      List<LXPoint> groupPoints = new ArrayList<LXPoint>();
      LXModel[] groupChildren = new LXModel[group.size()];
      int c = 0;
      for (LXModel sub : group) {
        // Replicate all the points from each group submodel
        for (LXPoint p : sub.points) {
          if (!clonedPoints.containsKey(p.index)) {
            LXPoint copy = new LXPoint(p);
            clonedPoints.put(p.index, copy);
            groupPoints.add(copy);
            allPoints.add(copy);
          }

        }
        // Clone the submodel of this group
        groupChildren[c++] = cloneModel(clonedPoints, sub);
      }
      views[g++] = new LXView(model, normalization, clonedPoints, groupPoints, groupChildren);
    }

    if (views.length == 0) {
      // Empty view!
      return new LXView(model, normalization, clonedPoints, new ArrayList<LXPoint>(), new LXModel[0]);
    } else if (views.length == 1) {
      // Just a single view, that'll do it!
      return views[0];
    } else {
      // Return a container-view with the group views as children, holding all of the points. We set
      // the normalization mode to absolute here no matter what, as this container view shouldn't do any
      // re-normalization
      return new LXView(model, Normalization.ABSOLUTE, clonedPoints, allPoints, views);
    }

  }

  private static void parseGroup(ParseState state, String groupSelector) {
    groupSelector = groupSelector.trim();
    if (groupSelector.isEmpty()) {
      return;
    }

    final int subgroup = groupSelector.lastIndexOf(GROUP_OPERATOR);
    if (subgroup >= 0) {
      final String rootSelector = groupSelector.substring(0, subgroup).replace(GROUP_OPERATOR, ' ').trim();
      final String subSelector = groupSelector.substring(subgroup+1).trim();
      final List<LXModel> groupRoots = new ArrayList<LXModel>();

      // Everything to the left of the * symbol specifies a level of grouping, these are
      // only actually added if there is no sub-selector to the right.
      final boolean terminal = subSelector.isEmpty();
      parseGroupSelector(state, state.model, groupRoots, rootSelector, terminal);

      for (LXModel groupRoot : groupRoots) {
        final List<LXModel> group = new ArrayList<LXModel>();
        if (terminal) {
          // If no subselector, then each group root candidate is itself an actual group
          // that's added directly
          group.add(groupRoot);
          state.groups.add(group);
        } else {
          // When a subselector is present, parse each potential group root candidate, which
          // may or may not find anything. This is now a terminal search.
          parseGroupSelector(state, groupRoot, group, subSelector, true);
          if (!group.isEmpty()) {
            state.groups.add(group);
          }
        }
      }
    } else {
      // There is no subgrouping in this selector, everything selected will be added, so
      // terminal is true.
      final List<LXModel> group = new ArrayList<LXModel>();
      parseGroupSelector(state, state.model, group, groupSelector, true);
      if (!group.isEmpty()) {
        state.groups.add(group);
      }
    }
  }

  private static void parseGroupSelector(ParseState state, LXModel root, List<LXModel> group, String groupSelector, boolean terminal) {
    // Within a group, multiple CSS-esque selectors are separated by ,
    // the union of these selectors forms the group
    for (String selector : groupSelector.split(SELECTOR_SEPARATOR)) {
      parseSubselector(state, root, group, selector, terminal);
    }
  }

  private static void parseSubselector(ParseState state, LXModel root, List<LXModel> group, String selector, boolean terminal) {
    // Is the selector empty? skip it.
    selector = selector.trim();
    if (selector.isEmpty()) {
      return;
    }

    // Set of candidates for addition to this group, by default we have the initial model
    final List<LXModel> candidates = new ArrayList<LXModel>();
    candidates.add(root);

    final List<LXModel> searchSpace = new ArrayList<LXModel>();

    final List<LXModel> intersect = new ArrayList<LXModel>();

    boolean directChildMode = false;
    boolean andMode = false;

    // Selectors are of the form "a b c" - meaning tags "c" contained by "b" contained by "a"
    for (String part : selector.split("\\s+")) {
      String tag = part.trim();

      // Check for special operators, set a flag for next encountered tag
      if (">".equals(tag)) {
        directChildMode = true;
        continue;
      } else if ("&".equals(tag)) {
        andMode = true;
        continue;
      }

      if (andMode) {
        // In andMode, we will keep the same search space as before, now run a new sub-query
        // and intersect it against the previous candidates
        intersect.clear();
        intersect.addAll(candidates);
      } else {
        // Clear the search space, add previously matched candidates to search space - on this
        // pass of the loop we will be searching the descendants of the previous match
        searchSpace.clear();
        searchSpace.addAll(candidates);
      }

      // We're going to select new candidates on this pass
      candidates.clear();

      // If this index selection syntax gets more complex, should clean it up to use regex matching
      int startIndex = 0, endIndex = -1, increment = 1;
      final int rangeStart = tag.indexOf('[');
      final int rangeEnd = tag.indexOf(']');
      if (rangeStart >= 0) {
        if ((rangeEnd < 0) || (rangeEnd <= rangeStart)) {
          LX.error("Poorly formatted view selection range: " + tag);
        } else {
          // Range can be specified as
          // - [even] same as 0:2
          // - [odd] same as 1:2
          // - [n] fixed index
          // - [n-m] (inclusive)
          // - [:i] increment by i
          // - [n:i] starting at n with increment i
          // - [n-m:i] inclusive range with increment i
          String range = tag.substring(rangeStart+1, rangeEnd).trim();
          tag = tag.substring(0, rangeStart);

          if ("even".equals(range)) {
            increment = 2;
          } else if ("odd".equals(range)) {
            startIndex = 1;
            increment = 2;
          } else {
            final int colon = range.indexOf(":");
            final boolean hasIncrement = (colon >= 0);
            if (hasIncrement) {
              // It's a increment specified tag[n:i] or tag[n-m:i]
              try {
                increment = Integer.parseInt(range.substring(colon+1).trim());
                range = range.substring(0, colon).trim();
              } catch (NumberFormatException nfx) {
                LX.error("Bad number in view selection range: " + tag);
              }
            }
            final int dash = range.indexOf('-');
            if (dash >= 0) {
              // It's a range tag[n-m]
              try {
                startIndex = Integer.parseInt(range.substring(0, dash).trim());
                endIndex = Integer.parseInt(range.substring(dash+1).trim());
              } catch (NumberFormatException nfx) {
                LX.error("Bad number in view selection range: " + tag);
              }
            } else {
              // It's a direct index tag[n]
              try {
                if (hasIncrement) {
                  if (!range.isEmpty()) {
                    startIndex = Integer.parseInt(range);
                  }
                } else {
                  startIndex = endIndex = Integer.parseInt(range);
                }
              } catch (NumberFormatException nfx) {
                LX.error("Bad number in view selection range: " + tag);
              }
            }
          }
        }
      }

      // Iterate over all searchSpace parents, to find sub-tags of appropriate type
      for (LXModel search : searchSpace) {
        List<LXModel> subs;
        if (andMode) {
          subs = search.sub(tag);
        } else if (directChildMode) {
          subs = search.children(tag);
        } else {
          subs = search.sub(tag);
        }
        if (increment < 1) {
          increment = 1;
        }
        if (startIndex < 0) {
          startIndex = 0;
        }
        if ((endIndex < 0) || (endIndex >= subs.size())) {
          endIndex = subs.size() - 1;
        }
        for (int i = startIndex; i <= endIndex; i += increment) {
          LXModel sub = subs.get(i);

          // Has this candidate *already* been found? Then there is no point
          // including it or searching below in the tree, we'll only be keeping
          // the ancestor in any case
          if (!state.uniqueSubmodels.contains(sub)) {
            candidates.add(subs.get(i));
          }
        }
      }

      // If this was the and query, filter candidates for presence in
      if (andMode) {
        Iterator<LXModel> iter = candidates.iterator();
        while (iter.hasNext()) {
          LXModel candidate = iter.next();
          if (!intersect.contains(candidate)) {
            iter.remove();
          }
        }
      }

      // Clear special mode flags as we move on
      directChildMode = false;
      andMode = false;
    }

    if (terminal) {
      // Check that candidates are valid/unique, and add to the group only if they are
      // not already part of the view by the fact that they belong to a parent that's already
      // in the view
      addGroupCandidates(state, group, candidates);
    } else {
      // If this is not a terminal selector, then we just add all candidates, they're not going
      // into the view directly, they'll just be used as roots for a further search.
      group.addAll(candidates);
    }
  }

  private static void addGroupCandidates(ParseState state, List<LXModel> group, List<LXModel> candidates) {

    // Now we have all the candidates matched by this selector, but we need to see
    // if they were already matched in this or another group!
    for (LXModel candidate : candidates) {
      // If the submodel is already directly contained, either by this query
      // or by a previous group, skip it and give precedence to first selector
      if (state.uniqueSubmodels.contains(candidate)) {
        continue;
      }

      // Now we need to check for two scenarios... one is that the candidate
      // is an ancestor of one or more already-contained submodels. In which case,
      // those need to be removed. The candidate will be added instead, implicitly
      // containing the submodels.
      //
      // Alternately, if the candidate is a descendant of one of the existing
      // submodels, then we can skip it as it is already contained
      boolean isDescendant = false;
      Iterator<LXModel> iter = state.uniqueSubmodels.iterator();
      while (!isDescendant && iter.hasNext()) {
        LXModel submodel = iter.next();
        if (submodel.contains(candidate)) {
          isDescendant = true;
        } else if (candidate.contains(submodel)) {
          // We're subsuming this thing, remove it!
          iter.remove();

          // Remove this from any group which contained it previously! We now
          // have a broader selection that contains the submodel, this will takes
          // priority
          for (List<LXModel> existingGroup : state.groups) {
            // NOTE(mcslee): Should we push a user-facing warning here explaining
            // that one submodel can't be in two separate groups?
            existingGroup.remove(submodel);
          }
        }
      }
      if (!isDescendant) {
        state.uniqueSubmodels.add(candidate);
        group.add(candidate);
      }
    }
  }

  private static LXModel cloneModel(Map<Integer, LXPoint> clonedPoints, LXModel model) {
    // Re-map points onto new ones
    List<LXPoint> points = new ArrayList<LXPoint>(model.points.length);
    for (LXPoint p : model.points) {
      points.add(clonedPoints.get(p.index));
    }

    // Recursively clone children with new points
    LXModel[] children = new LXModel[model.children.length];
    for (int i = 0; i < children.length; ++i) {
      children[i] = cloneModel(clonedPoints, model.children[i]);
    }

    return new LXModel(points, children, model.getNormalizationBounds(), model.metaData, model.tags);
  }

  private final LXModel model;

  final Normalization normalization;

  final Map<Integer, LXPoint> clonedPoints;

  /**
   * Constructs a view of the given model
   *
   * @param model Parent model that view is of
   * @param normalization Normalization mode
   * @param clonedPoints Map of points cloned from parent model into this view
   * @param points Points in this view
   * @param children Child models
   */
  private LXView(LXModel model, Normalization normalization, Map<Integer, LXPoint> clonedPoints, List<LXPoint> points, LXModel[] children) {
    super(points, children, (normalization == Normalization.ABSOLUTE) ? model.getNormalizationBounds() : null, LXModel.Tag.VIEW);
    this.model = model;
    this.normalization = normalization;

    this.clonedPoints = java.util.Collections.unmodifiableMap(clonedPoints);
    model.derivedViews.add(this);
    if (normalization == Normalization.RELATIVE) {
      normalizePoints();
    }
  }

  @Override
  public LXModel getMainRoot() {
    return this.model.getMainRoot();
  }

  @Override
  public void dispose() {
    this.model.derivedViews.remove(this);
    super.dispose();
  }

}
