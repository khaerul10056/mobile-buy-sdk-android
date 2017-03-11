package com.shopify.sample;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.cache.http.DiskLruCacheStore;
import com.apollographql.android.cache.http.TimeoutEvictionStrategy;
import com.apollographql.android.impl.ApolloClient;
import com.shopify.sample.repository.type.CustomType;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

public class SampleApplication extends BaseApplication {
  private static final String SHOP_PROPERTIES_INSTRUCTION =
    "\n\tAdd your shop credentials to a shop.properties file in the main app folder (e.g. 'app/shop.properties')."
      + "Include these keys:\n" + "\t\tSHOP_DOMAIN=<myshop>.myshopify.com\n"
      + "\t\tAPI_KEY=0123456789abcdefghijklmnopqrstuvw\n";

  private static ApolloClient apolloClient;

  public static ApolloClient apolloClient() {
    return apolloClient;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    initializeGraphClient();
  }

  private void initializeGraphClient() {
    String shopUrl = BuildConfig.SHOP_DOMAIN;
    if (TextUtils.isEmpty(shopUrl)) {
      throw new IllegalArgumentException(SHOP_PROPERTIES_INSTRUCTION + "You must add 'SHOP_DOMAIN' entry in "
        + "app/shop.properties, in the form '<myshop>.myshopify.com'");
    }

    String shopifyApiKey = BuildConfig.API_KEY;
    if (TextUtils.isEmpty(shopifyApiKey)) {
      throw new IllegalArgumentException(SHOP_PROPERTIES_INSTRUCTION + "You must populate the 'API_KEY' entry in "
        + "app/shop.properties");
    }

    String authHeader = String.format("Basic %s", Base64.encodeToString(shopifyApiKey.getBytes(Charset.forName("UTF-8")), Base64.NO_WRAP));

    OkHttpClient httpClient = new OkHttpClient.Builder()
      .addNetworkInterceptor(new HttpLoggingInterceptor().setLevel(BuildConfig.OKHTTP_LOG_LEVEL))
      .addInterceptor(chain -> {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder().method(original.method(), original.body());
        builder.header("Authorization", authHeader);
        return chain.proceed(builder.build());
      })
      .addInterceptor(chain -> {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder().method(original.method(), original.body());
        builder.header("User-Agent", "Android Apollo Client");
        return chain.proceed(builder.build());
      })
      .build();

    apolloClient = ApolloClient.builder()
      .okHttpClient(httpClient)
      .serverUrl(HttpUrl.parse("https://" + shopUrl + "/api/graphql"))
//      .httpCache(new DiskLruCacheStore(getCacheDir(), 1000 * 1024), new TimeoutEvictionStrategy(10, TimeUnit.MINUTES))
      .build();
  }
}
