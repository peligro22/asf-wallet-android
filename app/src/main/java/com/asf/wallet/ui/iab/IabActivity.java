package com.asf.wallet.ui.iab;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.asf.wallet.R;
import com.asf.wallet.entity.TransactionBuilder;
import com.asf.wallet.repository.TransactionService;
import com.asf.wallet.ui.BaseActivity;
import com.jakewharton.rxbinding2.view.RxView;
import dagger.android.AndroidInjection;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Formatter;
import java.util.Locale;
import javax.inject.Inject;

/**
 * Created by trinkes on 13/03/2018.
 */

public class IabActivity extends BaseActivity implements IabView {

  public static final String APP_PACKAGE = "app_package";
  public static final String PRODUCT_NAME = "product_name";
  public static final String TRANSACTION_HASH = "transaction_hash";
  @Inject TransactionService transactionService;
  private Button buyButton;
  private IabPresenter presenter;
  private View loadingView;
  private TextView appName;
  private TextView itemDescription;
  private TextView itemPrice;
  private ImageView appIcon;
  private View contentView;

  public static Intent newIntent(Activity activity, Intent previousIntent) {
    Intent intent = new Intent(previousIntent);
    intent.setClass(activity, IabActivity.class);
    intent.putExtra(APP_PACKAGE, activity.getCallingPackage());
    return intent;
  }

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    AndroidInjection.inject(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.iab_activity);
    buyButton = findViewById(R.id.buy_button);
    loadingView = findViewById(R.id.loading);
    appName = findViewById(R.id.iab_activity_app_name);
    appIcon = findViewById(R.id.iab_activity_item_icon);
    itemDescription = findViewById(R.id.iab_activity_item_description);
    itemPrice = findViewById(R.id.iab_activity_item_price);
    contentView = findViewById(android.R.id.content);
    presenter = new IabPresenter(this, transactionService, AndroidSchedulers.mainThread(),
        new CompositeDisposable());
    Observable.just(getAppPackage())
        .observeOn(Schedulers.io())
        .map(packageName -> new Pair<>(getApplicationName(packageName),
            getPackageManager().getApplicationIcon(packageName)))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(pair -> {
          appName.setText(pair.first);
          appIcon.setImageDrawable(pair.second);
        });
  }

  @Override protected void onStart() {
    super.onStart();
    presenter.present(getIntent().getData()
        .toString());
  }

  @Override protected void onStop() {
    presenter.stop();
    super.onStop();
  }

  @Override public Observable<String> getBuyClick() {
    return RxView.clicks(buyButton)
        .map(click -> getIntent().getData()
            .toString());
  }

  @Override public Observable<Object> getCancelClick() {
    return RxView.clicks(findViewById(R.id.cancel_button));
  }

  @Override public void finish(String hash) {
    Intent intent = new Intent();
    intent.putExtra(TRANSACTION_HASH, hash);
    setResult(Activity.RESULT_OK, intent);
    finish();
  }

  @Override public void showLoading() {
    loadingView.setVisibility(View.VISIBLE);
    loadingView.requestFocus();
    loadingView.setOnTouchListener((v, event) -> true);
  }

  @Override public void showError() {
    loadingView.setVisibility(View.GONE);
    Snackbar.make(contentView, "Error", Snackbar.LENGTH_LONG)
        .show();
    buyButton.setText(R.string.iab_activity_retry_button_text);
  }

  @Override public void lockOrientation() {
    int currentOrientation = getResources().getConfiguration().orientation;
    if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    } else {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }
  }

  @Override public void unlockOrientation() {
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
  }

  @Override public void setup(TransactionBuilder transactionBuilder) {
    Formatter formatter = new Formatter();
    itemPrice.setText(formatter.format(Locale.getDefault(), "%(,.2f", transactionBuilder.amount()
        .doubleValue())
        .toString());
    if (getIntent().hasExtra(PRODUCT_NAME)) {
      itemDescription.setText(getIntent().getExtras()
          .getString(PRODUCT_NAME));
    }
  }

  @Override public void close() {
    setResult(Activity.RESULT_CANCELED, null);
    finish();
  }

  private CharSequence getApplicationName(String appPackage)
      throws PackageManager.NameNotFoundException {
    PackageManager packageManager = getPackageManager();
    ApplicationInfo packageInfo = packageManager.getApplicationInfo(appPackage, 0);
    return packageManager.getApplicationLabel(packageInfo);
  }

  public String getAppPackage() {
    if (getIntent().hasExtra(APP_PACKAGE)) {
      return getIntent().getStringExtra(APP_PACKAGE);
    }
    return null;
  }
}
