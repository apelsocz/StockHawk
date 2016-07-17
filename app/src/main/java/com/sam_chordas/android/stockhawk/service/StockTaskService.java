package com.sam_chordas.android.stockhawk.service;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.util.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {

    public static final String ACTION_STOCK_UPDATED = "stockHawkUpdated";

    private String LOG_TAG = StockTaskService.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    public StockTaskService() {}

    public StockTaskService(Context context) {
        mContext = context;
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        // Base URL for the Yahoo query
        urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
        if (params.getTag().equals("periodic")) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
//                // Init task. Populates DB with quotes for the symbols seen below
//                try {
//                    urlStringBuilder.append(
//                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
                // Force result = GcmNetworkManager.RESULT_FAILURE
                urlStringBuilder = null;
            } else if (initQueryCursor != null) {
                try {
                    urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol ",
                            "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"" +
                            initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) + "\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode("in (" + mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals("add")) {
            try {
                urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol ",
                        "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("in (" + "\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        else if (params.getTag().equals("detail")) {
            try {
                urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata" +
                        " where symbol ", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            isUpdate = false;
            String stockInput = params.getExtras().getString("symbol");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String endDate = formatter.format(calendar.getTime());
            calendar.add(Calendar.DAY_OF_YEAR, -30);
            String startDate = formatter.format(calendar.getTime());
            try {
                urlStringBuilder.append(URLEncoder.encode("= \"" + stockInput + "\" and startDate = \"" +
                        startDate + "\" and endDate = \"" + endDate + "\"", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        if (urlStringBuilder != null) {
            // finalize the URL for the API query.
            urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys&callback=");
        }

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            Log.i(LOG_TAG, urlString);
            try {
                getResponse = fetchData(urlString);
                if (Utils.isValidQuote(getResponse)) {
                    result = GcmNetworkManager.RESULT_SUCCESS;
                    try {
                        ContentValues contentValues = new ContentValues();
                        // update ISCURRENT to 0 (false) so new data is current
                        if (isUpdate) {
                            contentValues.put(QuoteColumns.ISCURRENT, 0);
                            mContext.getContentResolver().update(
                                    QuoteProvider.Quotes.CONTENT_URI,
                                    contentValues,
                                    null,
                                    null
                            );
                        }
                        mContext.getContentResolver().applyBatch(
                                QuoteProvider.AUTHORITY,
                                Utils.quoteJsonToContentVals(getResponse)
                        );
                        Intent updateWidgetIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                        updateWidgetIntent.setAction(ACTION_STOCK_UPDATED);
                        mContext.sendBroadcast(updateWidgetIntent);
                    } catch (RemoteException | OperationApplicationException e) {
                        Log.e(LOG_TAG, "Error applying batch insert", e);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }
}