package com.crmbank.erp.mobile.hpio;

import android.app.DatePickerDialog;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
import com.crmbank.erp.mobile.RetrofitClient;
import com.crmbank.erp.mobile.ApiResponse;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

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

public class MHPIO420S extends BaseActivity {

    private TextView tvFromDate, tvToDate;
    private EditText etCustName;
    private ListView lvStatus;
    private StatusAdapter adapter;
    private List<Map<String, Object>> statusList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhpio420s);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("?낃퀬 ?꾪솴");
        }

        tvFromDate = findViewById(R.id.tvFromDate);
        tvToDate = findViewById(R.id.tvToDate);
        etCustName = findViewById(R.id.etCustName);
        lvStatus = findViewById(R.id.lvStatus);
        Button btnSearch = findViewById(R.id.btnSearch);

        initDates();

        tvFromDate.setOnClickListener(v -> showDatePicker(tvFromDate));
        tvToDate.setOnClickListener(v -> showDatePicker(tvToDate));

        adapter = new StatusAdapter();
        lvStatus.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> fetchInboundStatus());
    }

    private void initDates() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, -7);
        tvFromDate.setText(sdf.format(cal.getTime()));
        tvToDate.setText(today);
    }

    private void showDatePicker(TextView textView) {
        String current = textView.getText().toString();
        int year, month, day;
        try {
            String[] parts = current.split("-");
            year = Integer.parseInt(parts[0]);
            month = Integer.parseInt(parts[1]) - 1;
            day = Integer.parseInt(parts[2]);
        } catch (Exception e) {
            Calendar cal = Calendar.getInstance();
            year = cal.get(Calendar.YEAR);
            month = cal.get(Calendar.MONTH);
            day = cal.get(Calendar.DAY_OF_MONTH);
        }

        new DatePickerDialog(this, (view, y, m, d) -> {
            textView.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d));
        }, year, month, day).show();
    }

    private void fetchInboundStatus() {
        Map<String, String> params = new HashMap<>();
        params.put("fromdt", tvFromDate.getText().toString().replace("-", ""));
        params.put("todt", tvToDate.getText().toString().replace("-", ""));
        params.put("custnm", etCustName.getText().toString().trim());
        params.put("whcd", "");

        RetrofitClient.getApiService().getInboundStatus(params).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Map<String, Object>>>> call, Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Map<String, Object>>> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        statusList.clear();
                        if (apiResponse.getData() != null) {
                            statusList.addAll(apiResponse.getData());
                        }
                        adapter.notifyDataSetChanged();
                        if (statusList.isEmpty()) Toast.makeText(MHPIO420S.this, "議고쉶 寃곌낵媛 ?놁뒿?덈떎.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MHPIO420S.this, "?ㅻ쪟: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {
                Toast.makeText(MHPIO420S.this, "?듭떊 ?ㅽ뙣", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private class StatusAdapter extends BaseAdapter {
        private DecimalFormat df = new DecimalFormat("#,###");

        @Override public int getCount() { return statusList.size(); }
        @Override public Object getItem(int position) { return statusList.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MHPIO420S.this).inflate(R.layout.item_mhpio420s, parent, false);
            }

            Map<String, Object> item = statusList.get(position);

            TextView tvCustNm = convertView.findViewById(R.id.tvCustNm);
            TextView tvInYmd = convertView.findViewById(R.id.tvInYmd);
            TextView tvItemNm = convertView.findViewById(R.id.tvItemNm);
            TextView tvInQty = convertView.findViewById(R.id.tvInQty);
            TextView tvInAmt = convertView.findViewById(R.id.tvInAmt);
            TextView tvInVat = convertView.findViewById(R.id.tvInVat);

            // 理쒖떊 SQL 荑쇰━ 而щ읆紐??곸슜
            tvCustNm.setText(getValue(item, "custnm", "CUSTNM"));
            tvInYmd.setText(getValue(item, "ioymd", "IOYMD")); // ioymd 濡??섏젙
            tvItemNm.setText(getValue(item, "itemnm", "ITEMNM"));
            
            // ?섎웾(ioqty), 湲덉븸(ioamt), 遺媛??iovat)
            tvInQty.setText(formatNumber(item, "ioqty", "IOQTY"));
            tvInAmt.setText(formatNumber(item, "ioamt", "IOAMT"));
            tvInVat.setText(formatNumber(item, "iovat", "IOVAT"));

            return convertView;
        }

        private String getValue(Map<String, Object> map, String... keys) {
            for (String key : keys) {
                if (map.containsKey(key) && map.get(key) != null) return map.get(key).toString();
            }
            return "";
        }

        private String formatNumber(Map<String, Object> map, String... keys) {
            for (String key : keys) {
                if (map.containsKey(key)) {
                    Object val = map.get(key);
                    if (val != null && !val.toString().isEmpty()) {
                        try {
                            double d = Double.parseDouble(val.toString());
                            return df.format(d);
                        } catch (Exception e) { return val.toString(); }
                    }
                }
            }
            return "0";
        }
    }

    @Override protected String getProgramTitle() { return "제품입고현황"; }
    @Override protected String getProgramId() { return "MHPIO_420S"; }
}
