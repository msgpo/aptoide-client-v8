/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 06/07/2016.
 */

package cm.aptoide.pt.store.view.featured;

import android.view.View;
import android.widget.ImageView;
import androidx.fragment.app.FragmentActivity;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.view.recycler.widget.Widget;
import com.jakewharton.rxbinding.view.RxView;

/**
 * Created by neuro on 09-05-2016.
 */
public class AppBrickWidget extends Widget<AppBrickDisplayable> {

  private ImageView graphic;

  public AppBrickWidget(View itemView) {
    super(itemView);
  }

  @Override protected void assignViews(View itemView) {
    graphic = (ImageView) itemView.findViewById(R.id.featured_graphic);
  }

  @Override public void bindView(AppBrickDisplayable displayable, int position) {
    final FragmentActivity context = getContext();
    ImageLoader.with(context)
        .load(displayable.getPojo()
            .getGraphic(), R.attr.placeholder_brick, graphic);

    compositeSubscription.add(RxView.clicks(itemView)
        .subscribe(v -> {
          getFragmentNavigator().navigateTo(AptoideApplication.getFragmentProvider()
              .newAppViewFragment(displayable.getPojo()
                  .getId(), displayable.getPojo()
                  .getPackageName(), displayable.getPojo()
                  .getStore()
                  .getAppearance()
                  .getTheme(), displayable.getPojo()
                  .getStore()
                  .getName(), displayable.getTag(), String.valueOf(getAdapterPosition())), true);
        }, throwable -> CrashReport.getInstance()
            .log(throwable)));
  }
}
