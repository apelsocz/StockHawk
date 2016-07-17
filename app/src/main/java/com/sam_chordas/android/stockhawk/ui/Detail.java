package com.sam_chordas.android.stockhawk.ui;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.sam_chordas.android.stockhawk.MyStocksActivity;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.model.Stock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class Detail extends Fragment {

    public static final String NAME = Detail.class.getSimpleName();

    private static final String LOG_TAG = Detail.class.getSimpleName();

    private Stock mStock;

    private CandleStickChart mChart;
    private TextView symbol;
    private TextView bid;
    private TextView percentChange;
    private TextView change;
    private TextView dayLow;
    private TextView dayHigh;
    private TextView yearLow;
    private TextView yearHigh;
    private TextView volume;

    public Detail() {}

    public static Detail newInstance(Stock stock) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Stock.TAG, stock);

        Detail fragment = new Detail();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_detail, container, false);

        mChart = (CandleStickChart) view.findViewById(R.id.detail_chart_candlestick);
        symbol = (TextView) view.findViewById(R.id.detail_symbol);
        bid = (TextView) view.findViewById(R.id.detail_bid);
        percentChange = (TextView) view.findViewById(R.id.detail_percent_change);
        change = (TextView) view.findViewById(R.id.detail_change);
        dayLow = (TextView) view.findViewById(R.id.detail_days_low);
        dayHigh = (TextView) view.findViewById(R.id.detail_days_high);
        yearLow = (TextView) view.findViewById(R.id.detail_years_low);
        yearHigh = (TextView) view.findViewById(R.id.detail_years_high);
        volume = (TextView) view.findViewById(R.id.detail_volume);

        mChart.setDescription("");
        mChart.setPinchZoom(false);
        mChart.setDrawGridBackground(false);
        mChart.getLegend().setEnabled(false);
        mChart.getAxisLeft().setEnabled(true);
        mChart.getAxisLeft().setTextColor(Color.parseColor("#70FFFFFF"));
        mChart.getAxisLeft().setAxisLineColor(Color.TRANSPARENT);
        mChart.getAxisLeft().setGridColor(Color.TRANSPARENT);
        mChart.getAxisRight().setEnabled(false);
        mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        mChart.getXAxis().setTextColor(Color.parseColor("#70FFFFFF"));
        mChart.getXAxis().setAxisLineColor(Color.parseColor("#90A4AE"));
        mChart.getXAxis().setGridColor(Color.TRANSPARENT);
        mChart.getXAxis().setEnabled(false);
        mChart.setTouchEnabled(false);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            mStock = getArguments().getParcelable(Stock.TAG);
        }
        else {
            mStock = savedInstanceState.getParcelable(Stock.TAG);
        }

        symbol.setText(mStock.getSymbol());
        bid.setText(mStock.getBid());
        percentChange.setText(mStock.getPercentChange());
        change.setText(mStock.getChange());
        dayLow.setText(mStock.getDayLow());
        dayHigh.setText(mStock.getDayHigh());
        yearLow.setText(mStock.getYearLow());
        yearHigh.setText(mStock.getYearHigh());
        volume.setText(mStock.getVolume());

        ArrayList<CandleEntry> values = new ArrayList<CandleEntry>();

        JSONObject jsonObject = null;
        JSONArray jsonArray = null;

        try {
            jsonObject = new JSONObject(mStock.getHistoricalData());

            if (jsonObject != null && jsonObject.length() != 0) {
                jsonArray = jsonObject.getJSONArray("quote");

                for (int i = 0; i < jsonArray.length(); i++) {
                    float high = Float.valueOf(jsonArray.getJSONObject(i).getString("High"));
                    float low = Float.valueOf(jsonArray.getJSONObject(i).getString("Low"));
                    float open = Float.valueOf(jsonArray.getJSONObject(i).getString("Open"));
                    float close = Float.valueOf(jsonArray.getJSONObject(i).getString("Close"));
                    values.add(new CandleEntry(i, high, low, open, close));
                }

                CandleDataSet set = new CandleDataSet(values, "Data Set");
                set.setAxisDependency(YAxis.AxisDependency.LEFT);
                set.setShadowColor(Color.parseColor("#607D8B"));
                set.setShadowWidth(1f);
                set.setDecreasingColor(Color.parseColor("#455A64"));
                set.setDecreasingPaintStyle(Paint.Style.FILL);
                set.setIncreasingColor(Color.parseColor("#90A4AE"));
                set.setIncreasingPaintStyle(Paint.Style.STROKE);
                set.setNeutralColor(Color.DKGRAY);
                set.setDrawValues(false);

                CandleData data = new CandleData(set);
                data.setDrawValues(false);
                data.setHighlightEnabled(false);

                mChart.setData(data);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mChart.animateY(500);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MyStocksActivity)getActivity()).setToolbarTitle(mStock.getName());
        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        final MenuItem units = menu.findItem(R.id.action_change_units);
        if (units != null) {
            units.setVisible(false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(Stock.TAG, mStock);
        super.onSaveInstanceState(outState);
    }
}