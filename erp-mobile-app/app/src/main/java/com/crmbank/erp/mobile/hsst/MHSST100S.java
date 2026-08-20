package com.crmbank.erp.mobile.hsst;

import android.app.DatePickerDialog;
import android.content.Intent;
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
import com.crmbank.erp.mobile.CommonWebViewActivity;
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
 * 🚀 [MHSST100S] 매출처원장 (웹 HSST100S.vue 로직 100% 동기화)
 */
public class MHSST100S extends BaseActivity {

    private TextView tvDateFrom, tvDateTo;
    private EditText etCustNm;
    private LedgerAdapter adapter;
    private final List<Map<String, Object>> dataList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd, deptcd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsst100s);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("매출처원장");
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "");
        deptcd = prefs.getString("deptcd", "");

        apiService = RetrofitClient.getApiService();

        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        etCustNm = findViewById(R.id.etCustNm);
        ListView lvLedger = findViewById(R.id.lvLedger);
        Button btnSearch = findViewById(R.id.btnSearch);

        setupDatePickers();

        adapter = new LedgerAdapter();
        lvLedger.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> searchData());

        // 🚀 거래처 클릭 시 상세 내역으로 이동 (네이티브 미구현 시 웹뷰로 Fallback)
        lvLedger.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> selected = dataList.get(position);
            String custcd = getStringVal(selected, "custcd");
            String custnm = getStringVal(selected, "custnm");
            
            // 상세 원장은 웹뷰로 먼저 연결 (HSST110S 패턴 가정)
            Intent intent = new Intent(this, CommonWebViewActivity.class);
            intent.putExtra("PGMID", "HSST110S"); // 💡 매출처상세원장 웹 ID
            intent.putExtra("TITLE", custnm + " 상세원장");
            startActivity(intent);
        });

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
        // 🚀 웹 HSST100S.vue 로직과 100% 동일한 파라미터 구성
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        params.put("selgbn", "1");
        params.put("deptcd", deptcd);
        params.put("custfr", ""); 
        params.put("custto", "");
        params.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
        params.put("todt", tvDateTo.getText().toString().replace("-", ""));

        apiService.executeHsstProcedure("HSST_100S_STR", params).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    dataList.clear();
                    dataList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    if (dataList.isEmpty()) Toast.makeText(MHSST100S.this, "조회된 자료가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                Toast.makeText(MHSST100S.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
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

    @Override protected String getProgramTitle() { return "매출처원장"; }
    @Override protected String getProgramId() { return "MHSST100S"; }

    private class LedgerAdapter extends BaseAdapter {
        private final DecimalFormat df = new DecimalFormat("#,###");
        @Override public int getCount() { return dataList.size(); }
        @Override public Object getItem(int pos) { return dataList.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MHSST100S.this).inflate(R.layout.item_mhsst100s, parent, false);
            }
            Map<String, Object> item = dataList.get(position);

            // 🚀 웹 필드명(baseamt, spyamt, vatamt, cashamt, bankamt 등)에 맞춰 합계 계산 표시
            ((TextView) convertView.findViewById(R.id.tvCustNm)).setText(getStringVal(item, "custnm"));
            ((TextView) convertView.findViewById(R.id.tvPrevAmt)).setText(df.format(getDoubleVal(item, "baseamt")));
            
            double salesTotal = getDoubleVal(item, "spyamt") + getDoubleVal(item, "vatamt");
            double depositTotal = getDoubleVal(item, "cashamt") + getDoubleVal(item, "bankamt") + getDoubleVal(item, "billamt") + getDoubleVal(item, "etcamt");
            double balance = getDoubleVal(item, "baseamt") + salesTotal - depositTotal;

            ((TextView) convertView.findViewById(R.id.tvSalesAmt)).setText(df.format(salesTotal));
            ((TextView) convertView.findViewById(R.id.tvDepositAmt)).setText(df.format(depositTotal));
            ((TextView) convertView.findViewById(R.id.tvBalanceAmt)).setText(df.format(balance));

            return convertView;
        }
    }
}
