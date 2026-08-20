package com.crmbank.erp.mobile.hsio;

import android.app.DatePickerDialog;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
import com.crmbank.erp.mobile.ApiResponse;
import com.crmbank.erp.mobile.RetrofitClient;

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

public class MHSIO600S extends BaseActivity {

    private TextView tvFromDate, tvToDate;
    private EditText etCustName;
    private ListView lvStatus;
    private StatusAdapter adapter;
    private List<Map<String, Object>> statusList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio600s);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("異쒓퀬 ?꾪솴");
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

        btnSearch.setOnClickListener(v -> fetchOutboundStatus());
        
        // ?붾㈃ 濡쒕뱶 ??利됱떆 議고쉶
        fetchOutboundStatus();
    }

    private void initDates() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(cal.getTime());
        
        // ?대떦 ?붿쓽 01?쇰줈 ?ㅼ젙
        cal.set(Calendar.DAY_OF_MONTH, 1);
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

    private void fetchOutboundStatus() {
        Map<String, String> params = new HashMap<>();
        params.put("fromdt", tvFromDate.getText().toString().replace("-", ""));
        params.put("todt", tvToDate.getText().toString().replace("-", ""));
        params.put("custnm", etCustName.getText().toString().trim());
        params.put("whcd", "");
        params.put("iotype", ""); // 湲곕낯媛?怨듬갚

        RetrofitClient.getApiService().getOutboundStatus(params).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
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
                        if (statusList.isEmpty()) Toast.makeText(MHSIO600S.this, "議고쉶 寃곌낵媛 ?놁뒿?덈떎.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MHSIO600S.this, "?ㅻ쪟: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Map<String, Object>>>> call, Throwable t) {
                Toast.makeText(MHSIO600S.this, "?듭떊 ?ㅽ뙣", Toast.LENGTH_SHORT).show();
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
                convertView = LayoutInflater.from(MHSIO600S.this).inflate(R.layout.item_mhsio600s, parent, false);
            }

            Map<String, Object> item = statusList.get(position);

            TextView tvCustNm = convertView.findViewById(R.id.tvCustNm);
            TextView tvInYmd = convertView.findViewById(R.id.tvInYmd);
            TextView tvItemNm = convertView.findViewById(R.id.tvItemNm);
            TextView tvInQty = convertView.findViewById(R.id.tvInQty);
            TextView tvInAmt = convertView.findViewById(R.id.tvInAmt);
            TextView tvInVat = convertView.findViewById(R.id.tvInVat);

            // 異쒓퀬 ?곗씠??留ㅽ븨 (荑쇰━ 而щ읆紐?湲곗?)
            tvCustNm.setText(getValue(item, "custnm", "CUSTNM"));
            tvInYmd.setText(getValue(item, "ioymd", "IOYMD")); 
            tvItemNm.setText(getValue(item, "itemnm", "ITEMNM"));
            
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

    @Override protected String getProgramTitle() { return "기간별 출고현황"; }
    @Override protected String getProgramId() { return "MHSIO_600S"; }
}
