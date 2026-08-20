package com.crmbank.erp.mobile.hsod;

import android.app.AlertDialog;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.RetrofitClient;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.ApiResponse;
import com.crmbank.erp.mobile.R;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHSOD300U] 입고확정
 * MHSIO052U 프로토타입 기반, HSOD300U.vue 비즈니스 로직 연동 완료 버전
 */
public class MHSOD300U extends BaseActivity {

    private TextView tvOrderDate;
    private EditText etOrderNo, etCustomerName, etTotalSum;
    private Switch swConfirm;
    private DetailAdapter adapter;
    private final List<Map<String, Object>> detailList = new ArrayList<>();
    private final Map<String, Object> masterData = new HashMap<>();
    private ApiService apiService;
    private String cmpycd, userid;
    private String searchStatus = "N"; // 초기 조회 조건: 미확정

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsod300u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");

        apiService = RetrofitClient.getApiService();

        tvOrderDate = findViewById(R.id.tvOrderDate);
        etOrderNo = findViewById(R.id.etOrderNo);
        etCustomerName = findViewById(R.id.etCustomerName);
        etTotalSum = findViewById(R.id.etTotalSum);
        swConfirm = findViewById(R.id.swConfirm);
        ListView lvDetailList = findViewById(R.id.lvDetailList);

        adapter = new DetailAdapter();
        lvDetailList.setAdapter(adapter);

        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnReset).setOnClickListener(v -> initialize());
        findViewById(R.id.btnOrderSearch).setOnClickListener(v -> showSearchPopup());

        initialize();
    }

    private void initialize() {
        masterData.clear();
        detailList.clear();
        
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvOrderDate.setText(today);
        etOrderNo.setText("");
        etCustomerName.setText("");
        etTotalSum.setText("0");
        swConfirm.setChecked(true);
        swConfirm.setText("확정함");

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void showSearchPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_purch_order_search, null);
        builder.setTitle("입고 내역 조회").setView(dialogView);

        TextView tvPopStart = dialogView.findViewById(R.id.tvPopStartDate);
        TextView tvPopEnd = dialogView.findViewById(R.id.tvPopEndDate);
        EditText etPopCust = dialogView.findViewById(R.id.etPopCustNm);
        Button btnPopSearch = dialogView.findViewById(R.id.btnPopSearch);
        ListView lv = dialogView.findViewById(R.id.lvPopOrderList);

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvPopEnd.setText(sdf.format(cal.getTime()));
        cal.add(Calendar.MONTH, -1);
        tvPopStart.setText(sdf.format(cal.getTime()));

        tvPopStart.setOnClickListener(v -> showDatePicker(tvPopStart));
        tvPopEnd.setOnClickListener(v -> showDatePicker(tvPopEnd));

        List<Map<String, Object>> popList = new ArrayList<>();
        BaseAdapter popAdapter = new BaseAdapter() {
            @Override public int getCount() { return popList.size(); }
            @Override public Object getItem(int p) { return popList.get(p); }
            @Override public long getItemId(int p) { return p; }
            @Override public View getView(int p, View v, ViewGroup pr) {
                if (v == null) v = LayoutInflater.from(pr.getContext()).inflate(R.layout.item_mhsod300u_pop, pr, false);
                Map<String, Object> item = popList.get(p);
                ((TextView) v.findViewById(R.id.tvPopCustNm)).setText(getStringVal(item, "custnm"));
                ((TextView) v.findViewById(R.id.tvPopBalYmd)).setText(getStringVal(item, "ioymd"));
                String bNo = getStringVal(item, "ioym") + "-" + getStringVal(item, "iono");
                ((TextView) v.findViewById(R.id.tvPopBalno)).setText(bNo);
                return v;
            }
        };
        lv.setAdapter(popAdapter);

        AlertDialog dialog = builder.create();
        btnPopSearch.setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("actkind", "S0");
            p.put("cmpycd", cmpycd);
            p.put("fromdt", tvPopStart.getText().toString().replace("-", ""));
            p.put("todt", tvPopEnd.getText().toString().replace("-", ""));
            p.put("iogbn", "100");
            p.put("whcd", "000");
            p.put("slipyn", searchStatus);
            p.put("custnm", etPopCust.getText().toString().trim());

            apiService.executeHsodProcedure("HSOD_300U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        popList.clear();
                        popList.addAll(response.body());
                        popAdapter.notifyDataSetChanged();
                    }
                }
                @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
            });
        });

        lv.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> selected = popList.get(position);
            dialog.dismiss();
            fetchDetail(selected);
        });

        dialogView.findViewById(R.id.btnPopClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        btnPopSearch.performClick();
    }

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            tv.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void fetchDetail(Map<String, Object> row) {
        masterData.clear();
        masterData.putAll(row);
        
        etOrderNo.setText(String.format("%s-%s", getStringVal(row, "ioym"), getStringVal(row, "iono")));
        etCustomerName.setText(getStringVal(row, "custnm"));
        tvOrderDate.setText(formatDate(getStringVal(row, "ioymd")));
        
        double ioamt = 0;
        try { ioamt = Double.parseDouble(getStringVal(row, "ioamt")); } catch (Exception ignored) {}
        etTotalSum.setText(new DecimalFormat("#,###").format(ioamt));

        boolean isConfirmed = "Y".equals(getStringVal(row, "cfmyn"));
        swConfirm.setChecked(isConfirmed);
        swConfirm.setText(isConfirmed ? "확정함" : "미확정");

        fetchItems(getStringVal(row, "ioym"), getStringVal(row, "iono"));
    }

    private void fetchItems(String ioym, String iono) {
        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S1");
        p.put("cmpycd", cmpycd);
        p.put("ioym", ioym);
        p.put("iono", iono);
        p.put("userid", userid);

        apiService.executeHsodProcedure("HSOD_300U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    detailList.clear();
                    detailList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void save() {
        if (getStringVal(masterData, "iono").isEmpty()) {
            Toast.makeText(this, "대상 항목을 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> p = new HashMap<>(masterData);
        // actkind: 'A0' (신규확정), 'U0' (수정/취소)
        p.put("actkind", "N".equals(searchStatus) ? "A0" : "U0");
        p.put("cfmyn", swConfirm.isChecked() ? "Y" : "N");
        p.put("ioymd", getStringVal(masterData, "ioymd").replace("-", ""));
        p.put("iogbn", "100");
        p.put("whcd", "000");
        p.put("updemp", userid);

        apiService.executeHsodProcedure("HSOD_300U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSOD300U.this, "처리가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    initialize();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    private String formatDate(String d) {
        return d != null && d.length() == 8 ? String.format("%s-%s-%s", d.substring(0,4), d.substring(4,6), d.substring(6,8)) : d;
    }

    @Override protected String getProgramTitle() { return "입고 확정"; }
    @Override protected String getProgramId() { return "MHSOD300U"; }

    private class DetailAdapter extends BaseAdapter {
        private final DecimalFormat df = new DecimalFormat("#,###");
        @Override public int getCount() { return detailList.size(); }
        @Override public Object getItem(int p) { return detailList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(pr.getContext()).inflate(R.layout.item_mhsod300u, pr, false);
            Map<String, Object> item = detailList.get(p);
            
            ((TextView) v.findViewById(R.id.tvItemName)).setText(getStringVal(item, "itemnm"));
            
            TextView tvQty = v.findViewById(R.id.etQuantity);
            TextView tvPrice = v.findViewById(R.id.etPrice);
            TextView tvAmt = v.findViewById(R.id.etAmount);
            TextView tvTot = v.findViewById(R.id.etVat); // Layout reused: etVat acts as Total here

            double qty = 0, price = 0, amt = 0, tot = 0;
            try {
                qty = Double.parseDouble(getStringVal(item, "ioqty"));
                amt = Double.parseDouble(getStringVal(item, "ioamt"));
                tot = Double.parseDouble(getStringVal(item, "totamt"));
                price = qty != 0 ? Math.round(amt / qty) : 0;
            } catch (Exception ignored) {}

            tvQty.setText(String.valueOf(qty));
            tvPrice.setText(df.format(price));
            tvAmt.setText(df.format(amt));
            tvTot.setText(df.format(tot));

            v.findViewById(R.id.btnDelete).setVisibility(View.GONE); // Delete button not needed in confirm view
            
            return v;
        }
    }
}
