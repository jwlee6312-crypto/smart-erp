package com.crmbank.erp.mobile.hsst;

import android.app.DatePickerDialog;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.RetrofitClient;
import com.crmbank.erp.mobile.ApiResponse;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MHSST010S extends BaseActivity {

    private TextView tvDateFrom, tvDateTo;
    private EditText etCustNm;
    private UnsettledAdapter adapter;
    private List<UnsettledItem> dataList;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsst010s);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        apiService = RetrofitClient.getApiService();

        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        etCustNm = findViewById(R.id.etCustNm);
        ListView lvUnsettledList = findViewById(R.id.lvUnsettledList);
        Button btnSearch = findViewById(R.id.btnSearch);

        setupDatePickers();

        dataList = new ArrayList<>();
        adapter = new UnsettledAdapter();
        lvUnsettledList.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> searchUnsettledData());
        
        searchUnsettledData();
    }

    private void setupDatePickers() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String firstDay = sdf.format(cal.getTime());

        tvDateFrom.setText(firstDay);
        tvDateTo.setText(today);

        View.OnClickListener listener = v -> {
            TextView tv = (TextView) v;
            Calendar pickerCal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> tv.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day)), pickerCal.get(Calendar.YEAR), pickerCal.get(Calendar.MONTH), pickerCal.get(Calendar.DAY_OF_MONTH)).show();
        };

        tvDateFrom.setOnClickListener(listener);
        tvDateTo.setOnClickListener(listener);
    }

    private void searchUnsettledData() {
        String fromDt = tvDateFrom.getText().toString().replace("-", "");
        String toDt = tvDateTo.getText().toString().replace("-", "");
        String custNm = etCustNm.getText().toString();

        Map<String, String> params = new HashMap<>();
        params.put("fromdt", fromDt);
        params.put("todt", toDt);
        params.put("custnm", custNm);

        // ApiService defines getPurchaseUnsettledStatus as Call<ApiResponse<List<Map<String, Object>>>>
        apiService.getPurchaseUnsettledStatus(params).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> list = response.body().getData();
                    dataList.clear();
                    if (list == null || list.isEmpty()) {
                        Toast.makeText(MHSST010S.this, "?대떦 ?먮즺媛 議댁옱?섏? ?딆뒿?덈떎.", Toast.LENGTH_SHORT).show();
                    } else {
                        for (Map<String, Object> map : list) {
                            dataList.add(new UnsettledItem(
                                    getStringValue(map, "custnm"),
                                    parseToDouble(map.get("jan_qty")),
                                    parseToDouble(map.get("jan_amt")),
                                    parseToDouble(map.get("jan_vat"))
                            ));
                        }
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MHSST010S.this, "議고쉶 ?ㅽ뙣", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {
                Toast.makeText(MHSST010S.this, "?ㅽ듃?뚰겕 ?ㅻ쪟: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val) : "";
    }

    private double parseToDouble(Object value) {
        if (value == null) return 0.0;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static class UnsettledItem {
        String custNm;
        double qty, supply, vat;

        public UnsettledItem(String custNm, double qty, double supply, double vat) {
            this.custNm = custNm;
            this.qty = qty;
            this.supply = supply;
            this.vat = vat;
        }
    }

    private class UnsettledAdapter extends BaseAdapter {
        @Override public int getCount() { return dataList.size(); }
        @Override public Object getItem(int pos) { return dataList.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MHSST010S.this).inflate(R.layout.item_mhsst010s, parent, false);
            }
            UnsettledItem item = dataList.get(position);
            
            ((TextView) convertView.findViewById(R.id.tvCustNm)).setText(item.custNm);
            ((TextView) convertView.findViewById(R.id.tvQty)).setText(String.format(Locale.getDefault(), "%,d", (int)item.qty));
            ((TextView) convertView.findViewById(R.id.tvSupply)).setText(String.format(Locale.getDefault(), "%,d", (int)item.supply));
            ((TextView) convertView.findViewById(R.id.tvVat)).setText(String.format(Locale.getDefault(), "%,d", (int)item.vat));

            return convertView;
        }
    }

    @Override protected String getProgramTitle() { return "매입처원장"; }
    @Override protected String getProgramId() { return "MHSST010S"; }
}
