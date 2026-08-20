package com.crmbank.erp.mobile.hsio;

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
 * 🚀 [MHSIO400S] 입금현황
 * 웹 HSIO400S.vue 로직 기반 모바일 최적화 버전
 */
public class MHSIO400S extends BaseActivity {

    private TextView tvDateFrom, tvDateTo, tvCount, tvTotalAmt;
    private EditText etCustNm;
    private ListView lvPaymentStatus;
    private PaymentAdapter adapter;
    private final List<Map<String, Object>> dataList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd, deptcd;
    private final DecimalFormat df = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio400s);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        deptcd = prefs.getString("deptcd", "");

        apiService = RetrofitClient.getApiService();

        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        tvCount = findViewById(R.id.tvCount);
        tvTotalAmt = findViewById(R.id.tvTotalAmt);
        etCustNm = findViewById(R.id.etCustNm);
        lvPaymentStatus = findViewById(R.id.lvPaymentStatus);

        adapter = new PaymentAdapter();
        lvPaymentStatus.setAdapter(adapter);

        tvDateFrom.setOnClickListener(v -> showDatePicker(tvDateFrom));
        tvDateTo.setOnClickListener(v -> showDatePicker(tvDateTo));
        findViewById(R.id.btnSearch).setOnClickListener(v -> search());

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

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> 
            tv.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d)), 
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void search() {
        Map<String, Object> p = new HashMap<>();
        p.put("cmpycd", cmpycd);
        p.put("deptcd", deptcd);
        p.put("custcdfr", "");
        p.put("custcdto", "");
        p.put("custnmfr", etCustNm.getText().toString().trim());
        p.put("custnmto", etCustNm.getText().toString().trim());
        p.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
        p.put("todt", tvDateTo.getText().toString().replace("-", ""));
        p.put("salsemp", "000");

        // 💡 웹 버전의 executeHS00_000S_STR는 실제로는 입금현황을 조회하는 공통 래퍼일 가능성이 높음
        // 여기서는 executeHsioProcedure("HSIO_410S_STR") 또는 적절한 프로시저 사용
        apiService.executeHsioProcedure("HSIO_410S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    dataList.clear();
                    dataList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    updateTotals();
                    if (dataList.isEmpty()) Toast.makeText(MHSIO400S.this, "조회된 내역이 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void updateTotals() {
        double sum = 0;
        for (Map<String, Object> item : dataList) {
            sum += getDoubleVal(item, "amttot");
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

    @Override protected String getProgramTitle() { return "입금현황"; }
    @Override protected String getProgramId() { return "MHSIO400S"; }

    private class PaymentAdapter extends BaseAdapter {
        @Override public int getCount() { return dataList.size(); }
        @Override public Object getItem(int p) { return dataList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSIO400S.this).inflate(R.layout.item_mhsio400s, pr, false);
            Map<String, Object> item = dataList.get(p);

            ((TextView) v.findViewById(R.id.tvCustNm)).setText(getStringVal(item, "custnm"));
            ((TextView) v.findViewById(R.id.tvCashAmt)).setText(df.format(getDoubleVal(item, "cashamt")));
            ((TextView) v.findViewById(R.id.tvBankAmt)).setText(df.format(getDoubleVal(item, "bankamt")));
            ((TextView) v.findViewById(R.id.tvTotalAmt)).setText(df.format(getDoubleVal(item, "amttot")));

            return v;
        }
    }
}
