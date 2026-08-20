package com.crmbank.erp.mobile.hsio;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.RetrofitClient;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.CodeDto;
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
 * 🚀 [MHSIO200S] 입고현황
 * 웹 HSIO200S.vue 로직 기반 모바일 최적화 버전
 */
public class MHSIO200S extends BaseActivity {

    private TextView tvDateFrom, tvDateTo, tvCount, tvTotalAmt;
    private EditText etCustNm;
    private Spinner spWarehouse;
    private StatusAdapter adapter;
    private final List<Map<String, Object>> dataList = new ArrayList<>();
    private final List<CodeDto> whcdList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd;
    private final DecimalFormat df = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio200s);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();

        apiService = RetrofitClient.getApiService();

        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        tvCount = findViewById(R.id.tvCount);
        tvTotalAmt = findViewById(R.id.tvTotalAmt);
        etCustNm = findViewById(R.id.etCustNm);
        spWarehouse = findViewById(R.id.spWarehouse);
        ListView lvStatus = findViewById(R.id.lvStatus);

        adapter = new StatusAdapter();
        lvStatus.setAdapter(adapter);

        tvDateFrom.setOnClickListener(v -> showDatePicker(tvDateFrom));
        tvDateTo.setOnClickListener(v -> showDatePicker(tvDateTo));
        findViewById(R.id.btnSearch).setOnClickListener(v -> search());

        loadWarehouses();
        initialize();
    }

    private void initialize() {
        dataList.clear();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(cal.getTime());
        tvDateTo.setText(today);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        tvDateFrom.setText(sdf.format(cal.getTime()));
        
        etCustNm.setText("");
        tvCount.setText("0건");
        tvTotalAmt.setText("0");

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadWarehouses() {
        Map<String, Object> p = new HashMap<>();
        p.put("gubun", "W0"); p.put("cmpycd", cmpycd);
        apiService.executeHs00Procedure("HS00_000S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    whcdList.clear();
                    CodeDto all = new CodeDto(); all.codecd = "000"; all.codenm = "전체";
                    whcdList.add(all);
                    List<String> names = new ArrayList<>();
                    names.add("전체");
                    for (Map<String, Object> m : response.body()) {
                        CodeDto dto = new CodeDto();
                        dto.codecd = getStringVal(m, "whcd");
                        dto.codenm = getStringVal(m, "whnm");
                        whcdList.add(dto);
                        names.add(dto.codenm);
                    }
                    spWarehouse.setAdapter(new ArrayAdapter<>(MHSIO200S.this, android.R.layout.simple_spinner_item, names));
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> 
            tv.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d)), 
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void search() {
        Map<String, Object> p = new HashMap<>();
        p.put("cmpycd", cmpycd);
        p.put("whcd", whcdList.get(spWarehouse.getSelectedItemPosition()).codecd);
        p.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
        p.put("todt", tvDateTo.getText().toString().replace("-", ""));
        p.put("custcd", "");
        p.put("custnm", etCustNm.getText().toString().trim());
        p.put("iotype", "000");

        apiService.executeHsioProcedure("HSIO_200S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    dataList.clear();
                    dataList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    updateTotals();
                    if (dataList.isEmpty()) Toast.makeText(MHSIO200S.this, "조회된 내역이 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void updateTotals() {
        double sum = 0;
        for (Map<String, Object> item : dataList) {
            sum += getDoubleVal(item, "jsanamt") + getDoubleVal(item, "jsanvat");
        }
        tvCount.setText(String.format(Locale.getDefault(), "%d건", dataList.size()));
        tvTotalAmt.setText(df.format(sum));
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    private double getDoubleVal(Map<String, Object> map, String key) {
        try { return Double.parseDouble(getStringVal(map, key).replace(",", "")); } catch (Exception e) { return 0.0; }
    }

    @Override protected String getProgramTitle() { return "입고현황"; }
    @Override protected String getProgramId() { return "MHSIO200S"; }

    private class StatusAdapter extends BaseAdapter {
        @Override public int getCount() { return dataList.size(); }
        @Override public Object getItem(int p) { return dataList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSIO200S.this).inflate(R.layout.item_mhsio200s, pr, false);
            Map<String, Object> item = dataList.get(p);

            String date = getStringVal(item, "ioymd");
            if (date.length() == 8) date = date.substring(4, 6) + "-" + date.substring(6, 8);
            ((TextView) v.findViewById(R.id.tvDate)).setText(date);
            ((TextView) v.findViewById(R.id.tvCustNm)).setText(getStringVal(item, "custnm"));
            ((TextView) v.findViewById(R.id.tvItemNm)).setText(getStringVal(item, "itemnm"));
            ((TextView) v.findViewById(R.id.tvSize)).setText(getStringVal(item, "itsize"));

            double total = getDoubleVal(item, "jsanamt") + getDoubleVal(item, "jsanvat");
            ((TextView) v.findViewById(R.id.tvAmt)).setText(df.format(total));

            return v;
        }
    }
}
