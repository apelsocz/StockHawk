package com.sam_chordas.android.stockhawk;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.model.Stock;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.ui.Detail;
import com.sam_chordas.android.stockhawk.ui.Stocks;
import com.sam_chordas.android.stockhawk.util.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MyStocksActivity extends AppCompatActivity {

    private Intent mServiceIntent;
    private Context mContext;
    private Toolbar mToolbar;
    private FloatingActionButton mFAB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        setContentView(R.layout.activity_my_stocks);
        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new Stocks(), Stocks.NAME)
                    .commit();
        }

        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        mFAB = (FloatingActionButton) findViewById(R.id.fab);

        assert mFAB != null;
        mFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MyApplication.getInstance().isConnected()) {
                    showDialog();
                } else {
                    Snackbar.make(getCurrentFocus(), getString(R.string.network_snackbar),
                            Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showDialog() {
        new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                .content(R.string.content_test)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .inputRange(1, 4)
                .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        String symbol = input.toString().toUpperCase();
                        // On FAB click, receive user input. Make sure the stock doesn't already exist
                        // in the DB and proceed accordingly
                        Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                new String[]{symbol}, null);
                        if (c.getCount() != 0) {
                            Toast toast =
                                    Toast.makeText(MyStocksActivity.this,
                                            getString(R.string.message_exist), Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                            toast.show();
                            return;
                        } else {
                            if (symbol.matches("[^A-Z]")) {
                                Toast toast =
                                        Toast.makeText(MyStocksActivity.this,
                                                getString(R.string.message_invalid), Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                toast.show();
                                return;
                            }
                            else {
                                // Add the stock to DB
                                mServiceIntent.putExtra("tag", "add");
                                mServiceIntent.putExtra("symbol", symbol);
                                startService(mServiceIntent);
                            }
                        }
                    }
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    public void onStockClick(String symbol) {
        Log.i(MyStocksActivity.class.getSimpleName(), symbol);
        new DownloadStockTask().execute(symbol);
        mFAB.hide();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setToolbarTitle(getString(R.string.app_name));
        // animate the fab back into view
        mFAB.show();
    }

    public void setToolbarTitle(String string) {
        mToolbar.setTitle(string);
    }

    private class DownloadStockTask extends AsyncTask<String, Void, Stock> {

        private final String LOG_TAG = DownloadStockTask.class.getSimpleName();

        private OkHttpClient mOKHttpClient = new OkHttpClient();

        private String fetch(String url) {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = null;
            String retVal = null;
            try {
                response = mOKHttpClient.newCall(request).execute();
                retVal = response.body().string();
                response.body().close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Request could not be executed: " + e.getMessage());
            }

            return retVal;
        }

        @Override
        protected Stock doInBackground(String... params) {
            final String symbol = params[0];

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            final String endDate = formatter.format(calendar.getTime());
            calendar.add(Calendar.DAY_OF_YEAR, -30);
            final String startDate = formatter.format(calendar.getTime());

            final String ENCODING = "UTF-8";
            final String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
            final String YQL_QUOTE_SELECT = "select * from yahoo.finance.quotes where symbol ";
            final String YQL_HISTORY_SELECT = "select * from yahoo.finance.historicaldata where symbol ";
            final String YQL_QUOTE_PARAM = "in (" + "\"" + symbol + "\")";
            final String YQL_HISTORY_PARAM = "= \"" + symbol + "\" and startDate = \"" +
                    startDate + "\" and endDate = \"" + endDate + "\"";
            final String PARAM_END = "&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys&callback=";

            StringBuilder quoteBuilder = new StringBuilder();

            try {
                quoteBuilder.append(BASE_URL)
                        .append(URLEncoder.encode(YQL_QUOTE_SELECT, ENCODING))
                        .append(URLEncoder.encode(YQL_QUOTE_PARAM, ENCODING))
                        .append(PARAM_END);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            StringBuilder historyBuilder = new StringBuilder();
            try {
                historyBuilder.append(BASE_URL)
                        .append(URLEncoder.encode(YQL_HISTORY_SELECT, ENCODING))
                        .append(URLEncoder.encode(YQL_HISTORY_PARAM, ENCODING))
                        .append(PARAM_END);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            Stock stock = new Stock();

            String quoteJSON = fetch(quoteBuilder.toString());
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(quoteJSON);
                if (jsonObject != null && jsonObject.length() != 0) {
                    jsonObject = jsonObject.getJSONObject("query")
                            .getJSONObject("results")
                            .getJSONObject("quote");
//                    stock.setName(jsonObject.getString("Name"));
                    stock.setName(jsonObject.getString("Name"));
                    stock.setSymbol(jsonObject.getString("symbol"));
                    stock.setBid(Utils.truncateBidPrice(jsonObject.getString("Bid")));
                    stock.setPercentChange(Utils.truncateChange(
                            jsonObject.getString("ChangeinPercent"), true));
                    stock.setChange(Utils.truncateChange(jsonObject.getString("Change"), false));
                    stock.setDayLow(Utils.truncateBidPrice(jsonObject.getString("DaysLow")));
                    stock.setDayHigh(Utils.truncateBidPrice(jsonObject.getString("DaysHigh")));
                    stock.setYearLow(Utils.truncateBidPrice(jsonObject.getString("YearLow")));
                    stock.setYearHigh(Utils.truncateBidPrice(jsonObject.getString("YearHigh")));
                    stock.setVolume(Utils.formatVolume(jsonObject.getString("Volume")));
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "String to JSON failed: " + e);
            }

            String historyJSON = fetch(historyBuilder.toString());
            jsonObject = null;
            try {
                jsonObject = new JSONObject(historyJSON);

                if (jsonObject != null && jsonObject.length() != 0) {
                    jsonObject = jsonObject.getJSONObject("query").getJSONObject("results");
                    stock.setHistoricalData(jsonObject.toString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return stock;
        }

        @Override
        protected void onPostExecute(Stock stock) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStackImmediate(Detail.NAME, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getSupportFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.fragment_container, Detail.newInstance(stock), Detail.NAME)
                    .addToBackStack(Detail.NAME)
                    .commit();

            super.onPostExecute(stock);
        }
    }
}