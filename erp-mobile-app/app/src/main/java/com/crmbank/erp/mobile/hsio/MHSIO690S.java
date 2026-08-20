package com.crmbank.erp.mobile.hsio;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.RetrofitClient;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.PopupAdapter;
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
 * 🚀 [MHSIO690S] 매입미정산현황
 * 웹 HSIO690S.vue 로직 기반 모바일 최적화 버전
 */
public class MHSIO690S extends BaseActivity {

    private TextView tvDateFrom, tvDateTo, tvDeptNm, tvCount, tvTotalBalance;
    private ListView lvUnsettledList;
    private UnsettledAdapter adapter;
    private final List<Map<String, Object>> dataList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd, deptcd;
    private final DecimalFormat df = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio690s);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        deptcd = prefs.getString("deptcd", "");
        String deptnm = prefs.getString("deptnm", "");

        apiService = RetrofitClient.getApiService();

        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        tvDeptNm = findViewById(R.id.tvDeptNm);
        tvCount = findViewById(R.id.tvCount);
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        lvUnsettledList = findViewById(R.id.lvUnsettledList);

        adapter = new UnsettledAdapter();
        lvUnsettledList.setAdapter(adapter);

        tvDateFrom.setOnClickListener(v -> showDatePicker(tvDateFrom));
        tvDateTo.setOnClickListener(v -> showDatePicker(tvDateTo));
        tvDeptNm.setOnClickListener(v -> openDeptHelp());
        findViewById(R.id.btnSearch).setOnClickListener(v -> search());

        tvDeptNm.setText(deptnm);
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
        
        tvCount.setText("0건");
        tvTotalBalance.setText("0");

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> 
            tv.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d)), 
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openDeptHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setTitle("입고부서 선택").setView(dialogView);
        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();
        PopupAdapter popupAdapter = new PopupAdapter(list, "DEPT", item -> {
            deptcd = getStringVal(item, "deptcd");
            tvDeptNm.setText(getStringVal(item, "deptnm"));
            dialog.dismiss();
        });
        rv.setAdapter(popupAdapter);
        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("gubun", "D0"); p.put("cmpycd", cmpycd); p.put("remark", etSearch.getText().toString().trim());
            apiService.executeHa00Procedure("HA00_00P_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                    if (r.isSuccessful() && r.body() != null) {
                        list.clear(); list.addAll(r.body()); popupAdapter.notifyDataSetChanged();
                    }
                }
                @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
            });
        });
        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        dialogView.findViewById(R.id.btnSearch).performClick();
    }

    private void search() {
        if (deptcd.isEmpty()) { Toast.makeText(this, "입고부서를 선택하세요.", Toast.LENGTH_SHORT).show(); return; }

        Map<String, Object> p = new HashMap<>();
        p.put("cmpycd", cmpycd);
        p.put("deptcd", deptcd);
        p.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
        p.put("todt", tvDateTo.getText().toString().replace("-", ""));

        apiService.executeHsioProcedure("HSIO_690S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    dataList.clear();
                    dataList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    updateTotals();
                    if (dataList.isEmpty()) Toast.makeText(MHSIO690S.this, "조회된 내역이 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void updateTotals() {
        double sum = 0;
        for (Map<String, Object> item : dataList) {
            sum += getDoubleVal(item, "namt") + getDoubleVal(item, "nvat");
        }
        tvCount.setText(String.format(Locale.getDefault(), "%d건", dataList.size()));
        tvTotalBalance.setText(df.format(sum));
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

    @Override protected String getProgramTitle() { return "매입미정산현황"; }
    @Override protected String getProgramId() { return "MHSIO690S"; }

    private class UnsettledAdapter extends BaseAdapter {
        @Override public int getCount() { return dataList.size(); }
        @Override public Object getItem(int p) { return dataList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSIO690S.this).inflate(R.layout.item_mhsio690s, pr, false);
            Map<String, Object> item = dataList.get(p);

            ((TextView) v.findViewById(R.id.tvCustNm)).setText(getStringVal(item, "custnm"));
            
            double inAmt = getDoubleVal(item, "oamt") + getDoubleVal(item, "ovat");
            ((TextView) v.findViewById(R.id.tvInboundAmt)).setText(df.format(inAmt));
            
            double balAmt = getDoubleVal(item, "namt") + getDoubleVal(item, "nvat");
            ((TextView) v.findViewById(R.id.tvBalanceAmt)).setText(df.format(balAmt));

            return v;
        }
    }
}
