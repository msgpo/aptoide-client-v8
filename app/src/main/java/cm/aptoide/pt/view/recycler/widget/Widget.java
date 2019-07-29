/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 31/05/2016.
 */

package cm.aptoide.pt.view.recycler.widget;

import android.view.View;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.navigator.ActivityNavigator;
import cm.aptoide.pt.navigator.ActivityResultNavigator;
import cm.aptoide.pt.navigator.FragmentNavigator;
import cm.aptoide.pt.view.recycler.displayable.Displayable;
import rx.subscriptions.CompositeSubscription;

/**
 * Class that represents a generic Widget. All widgets should extend this class.
 */
public abstract class Widget<T extends Displayable> extends RecyclerView.ViewHolder {

  private final FragmentNavigator fragmentNavigator;
  protected CompositeSubscription compositeSubscription;
  private ActivityNavigator activityNavigator;

  public Widget(@NonNull View itemView) {
    super(itemView);
    fragmentNavigator = ((ActivityResultNavigator) getContext()).getFragmentNavigator();
    activityNavigator = ((ActivityResultNavigator) getContext()).getActivityNavigator();

    try {
      assignViews(itemView);
    } catch (Exception e) {
      CrashReport.getInstance()
          .log(e);
    }
  }

  public FragmentActivity getContext() {
    return (FragmentActivity) itemView.getContext();
  }

  protected abstract void assignViews(View itemView);

  @CallSuper public void unbindView() {
    if (compositeSubscription != null && !compositeSubscription.isUnsubscribed()) {
      compositeSubscription.clear();
    }
  }

  public void internalBindView(T displayable, int position) {
    if (compositeSubscription == null) {
      compositeSubscription = new CompositeSubscription();
    }
    displayable.setVisible(true);
    bindView(displayable, position);
  }

  public abstract void bindView(T displayable, int position);

  public View getRootView() {
    return getFragmentNavigator().peekLast()
        .getView();
  }

  protected FragmentNavigator getFragmentNavigator() {
    return fragmentNavigator;
  }

  protected ActivityNavigator getActivityNavigator() {
    return activityNavigator;
  }
}
