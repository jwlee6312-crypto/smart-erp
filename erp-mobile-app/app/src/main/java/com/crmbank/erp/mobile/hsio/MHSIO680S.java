package com.crmbank.erp.mobile.hsio;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.RetrofitClient;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.R;

import java.text.DecimalFormat;
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

/**
 * 🚀 [MHSIO680S] 매출미정산현황 (웹 HSIO680S.vue 로직 100% 동기화)
 */
public class MHSIO680S extends BaseActivity {

    private TextView tvDateFrom, tvDateTo;
    private EditText etCustNm;
    private UnsettledAdapter adapter;
    private final List<Map<String, Object>> dataList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd, deptcd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio680s);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("매출 미정산 현황");
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "");
        deptcd = prefs.getString("deptcd", "");

        apiService = RetrofitClient.getApiService();

        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        etCustNm = findViewById(R.id.etCustNm);
        ListView lvUnsettledList = findViewById(R.id.lvUnsettledList);
        Button btnSearch = findViewById(R.id.btnSearch);

        setupDatePickers();

        adapter = new UnsettledAdapter();
        lvUnsettledList.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> searchData());
        
        searchData();
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
            String current = tv.getText().toString();
            int y, m, d;
            try {
                String[] p = current.split("-");
                y = Integer.parseInt(p[0]); m = Integer.parseInt(p[1]) - 1; d = Integer.parseInt(p[2]);
            } catch (Exception e) {
                Calendar c = Calendar.getInstance();
                y = c.get(Calendar.YEAR); m = c.get(Calendar.MONTH); d = c.get(Calendar.DAY_OF_MONTH);
            }
            new DatePickerDialog(this, (view, year, month, day) -> 
                tv.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day)), y, m, d).show();
        };

        tvDateFrom.setOnClickListener(listener);
        tvDateTo.setOnClickListener(listener);
    }

    private void searchData() {
        // 🚀 웹 HSIO680S.vue 로직과 100% 동일한 파라미터 구성
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        params.put("deptcd", deptcd);
        params.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
        params.put("todt", tvDateTo.getText().toString().replace("-", ""));

        apiService.executeHsioProcedure("HSIO_680S_STR", params).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    dataList.clear();
                    dataList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    if (dataList.isEmpty()) Toast.makeText(MHSIO680S.this, "조회된 자료가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                Toast.makeText(MHSIO680S.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringVal(Map<String, Object> map, String key) {
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    private double getDoubleVal(Map<String, Object> map, String key) {
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        try { return Double.parseDouble(String.valueOf(val != null ? val : "0")); } catch (Exception e) { return 0.0; }
    }

    @Override protected String getProgramTitle() { return "매출 미정산 현황"; }
    @Override protected String getProgramId() { return "MHSIO680S"; }

    private class UnsettledAdapter extends BaseAdapter {
        private final DecimalFormat df = new DecimalFormat("#,###");
        @Override public int getCount() { return dataList.size(); }
        @Override public Object getItem(int pos) { return dataList.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MHSIO680S.this).inflate(R.layout.item_mhsio680s, parent, false);
            }
            Map<String, Object> item = dataList.get(position);
            
            ((TextView) convertView.findViewById(R.id.tvCustNm)).setText(getStringVal(item, "custnm"));
            ((TextView) convertView.findViewById(R.id.tvQty)).setText(df.format(getDoubleVal(item, "nqty"))); // 미정산 수량
            
            double supply = getDoubleVal(item, "namt");
            double vat = getDoubleVal(item, "nvat");
            ((TextView) convertView.findViewById(R.id.tvSupply)).setText(df.format(supply));
            ((TextView) convertView.findViewById(R.id.tvVat)).setText(df.format(vat));

            return convertView;
        }
    }
}
