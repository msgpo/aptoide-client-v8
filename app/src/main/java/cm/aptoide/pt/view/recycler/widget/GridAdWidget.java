package cm.aptoide.pt.view.recycler.widget;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.database.realm.MinimalAd;
import cm.aptoide.pt.navigator.FragmentNavigator;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.search.model.SearchAdResult;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.view.BaseActivity;
import cm.aptoide.pt.view.recycler.displayable.GridAdDisplayable;
import com.jakewharton.rxbinding.view.RxView;
import javax.inject.Inject;

/**
 * Created by neuro on 20-06-2016.
 */
public class GridAdWidget extends Widget<GridAdDisplayable> {

  @Inject FragmentNavigator fragmentNavigator;
  private TextView name;
  private ImageView icon;
  private TextView downloadsNumber;
  private RatingBar rating;

  public GridAdWidget(View itemView) {
    super(itemView);
    ((BaseActivity) getContext()).getActivityComponent()
        .inject(this);
  }

  @Override protected void assignViews(View itemView) {
    name = (TextView) itemView.findViewById(R.id.name);
    icon = (ImageView) itemView.findViewById(R.id.icon);
    downloadsNumber = (TextView) itemView.findViewById(R.id.downloads);
    rating = (RatingBar) itemView.findViewById(R.id.ratingbar);
  }

  @Override public void bindView(GridAdDisplayable displayable) {
    MinimalAd pojo = displayable.getPojo();
    name.setText(pojo.getName());

    final FragmentActivity context = getContext();
    ImageLoader.with(context)
        .load(pojo.getIconPath(), icon);

    compositeSubscription.add(RxView.clicks(itemView)
        .subscribe(v -> {
          fragmentNavigator.navigateTo(AptoideApplication.getFragmentProvider()
              .newAppViewFragment(new SearchAdResult(pojo), displayable.getTag()), true);
        }, throwable -> CrashReport.getInstance()
            .log(throwable)));
    downloadsNumber.setText(
        AptoideUtils.StringU.withSuffix(pojo.getDownloads()) + context.getString(
            R.string._downloads));
    rating.setRating(pojo.getStars());
  }
}
