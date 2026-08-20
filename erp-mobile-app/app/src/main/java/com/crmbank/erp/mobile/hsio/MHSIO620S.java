package com.crmbank.erp.mobile.hsio;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
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

import com.crmbank.erp.mobile.Config;
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
 * 🚀 [MHSIO620S] 거래명세표
 * 웹 HSIO620S.vue 로직 기반 마스터-상세 하이브리드 버전
 */
public class MHSIO620S extends BaseActivity {

    private TextView tvDateFrom, tvDateTo;
    private EditText etCustNm;
    private ListView lvMasterList, lvDetailList;
    private MasterAdapter masterAdapter;
    private DetailAdapter detailAdapter;
    private final List<Map<String, Object>> masterList = new ArrayList<>();
    private final List<Map<String, Object>> detailList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd;
    private Map<String, Object> selectedMaster = null;
    private final DecimalFormat df = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio620s);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();

        apiService = RetrofitClient.getApiService();

        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        etCustNm = findViewById(R.id.etCustNm);
        lvMasterList = findViewById(R.id.lvMasterList);
        lvDetailList = findViewById(R.id.lvDetailList);

        masterAdapter = new MasterAdapter();
        lvMasterList.setAdapter(masterAdapter);
        
        detailAdapter = new DetailAdapter();
        lvDetailList.setAdapter(detailAdapter);

        tvDateFrom.setOnClickListener(v -> showDatePicker(tvDateFrom));
        tvDateTo.setOnClickListener(v -> showDatePicker(tvDateTo));
        etCustNm.setOnClickListener(v -> openCustHelp());
        findViewById(R.id.btnSearch).setOnClickListener(v -> searchMaster());
        findViewById(R.id.btnPreview).setOnClickListener(v -> previewSlip());

        lvMasterList.setOnItemClickListener((parent, view, position, id) -> {
            selectedMaster = masterList.get(position);
            masterAdapter.notifyDataSetChanged();
            fetchDetails();
        });

        initialize();
    }

    private void initialize() {
        masterList.clear();
        detailList.clear();
        selectedMaster = null;
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(cal.getTime());
        tvDateTo.setText(today);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        tvDateFrom.setText(sdf.format(cal.getTime()));
        
        etCustNm.setText("");

        if (masterAdapter != null) masterAdapter.notifyDataSetChanged();
        if (detailAdapter != null) detailAdapter.notifyDataSetChanged();
    }

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> 
            tv.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d)), 
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openCustHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setTitle("거래처 선택").setView(dialogView);
        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();
        PopupAdapter popupAdapter = new PopupAdapter(list, "CUST", item -> {
            etCustNm.setText(getStringVal(item, "custnm"));
            dialog.dismiss();
            searchMaster();
        });
        rv.setAdapter(popupAdapter);
        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("gubun", "C4"); p.put("cmpycd", cmpycd); p.put("remark", etSearch.getText().toString().trim());
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

    private void searchMaster() {
        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S1");
        p.put("cmpycd", cmpycd);
        p.put("iogbn", "200");
        p.put("whcd", "000");
        p.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
        p.put("todt", tvDateTo.getText().toString().replace("-", ""));
        p.put("custnm", etCustNm.getText().toString().trim());
        p.put("slipyn", "Y");

        apiService.executeHsioProcedure("HSIO_620S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    masterList.clear();
                    masterList.addAll(response.body());
                    masterAdapter.notifyDataSetChanged();
                    detailList.clear();
                    detailAdapter.notifyDataSetChanged();
                    selectedMaster = null;
                    if (masterList.isEmpty()) Toast.makeText(MHSIO620S.this, "조회된 내역이 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchDetails() {
        if (selectedMaster == null) return;

        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S0");
        p.put("cmpycd", cmpycd);
        p.put("iogbn", "200");
        p.put("ioym", getStringVal(selectedMaster, "ioym"));
        p.put("iono", getStringVal(selectedMaster, "iono"));

        apiService.executeHsioProcedure("HSIO_620S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    detailList.clear();
                    detailList.addAll(response.body());
                    detailAdapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void previewSlip() {
        if (selectedMaster == null) { Toast.makeText(this, "먼저 출고 건을 선택하세요.", Toast.LENGTH_SHORT).show(); return; }
        
        String url = String.format("%sreport/HSIO_TRANS_PRINT?PRTGU=Print&ioym=%s&iono=%s", 
                Config.BASE_URL, getStringVal(selectedMaster, "ioym"), getStringVal(selectedMaster, "iono"));
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
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

    @Override protected String getProgramTitle() { return "거래명세표"; }
    @Override protected String getProgramId() { return "MHSIO620S"; }

    private class MasterAdapter extends BaseAdapter {
        @Override public int getCount() { return masterList.size(); }
        @Override public Object getItem(int p) { return masterList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSIO620S.this).inflate(R.layout.item_mhsio620s_master, pr, false);
            Map<String, Object> item = masterList.get(p);

            v.setBackgroundColor(item == selectedMaster ? Color.parseColor("#E3F2FD") : Color.WHITE);

            String date = getStringVal(item, "ioymd");
            if (date.length() == 8) date = date.substring(4, 6) + "-" + date.substring(6, 8);
            ((TextView) v.findViewById(R.id.tvDate)).setText(date);
            ((TextView) v.findViewById(R.id.tvCustNm)).setText(getStringVal(item, "custnm"));
            ((TextView) v.findViewById(R.id.tvIoNo)).setText(getStringVal(item, "ioym") + "-" + getStringVal(item, "iono"));

            return v;
        }
    }

    private class DetailAdapter extends BaseAdapter {
        @Override public int getCount() { return detailList.size(); }
        @Override public Object getItem(int p) { return detailList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSIO620S.this).inflate(R.layout.item_mhsio620s_detail, pr, false);
            Map<String, Object> item = detailList.get(p);

            ((TextView) v.findViewById(R.id.tvItemNm)).setText(getStringVal(item, "itemnm"));
            ((TextView) v.findViewById(R.id.tvSize)).setText(getStringVal(item, "itsize"));
            ((TextView) v.findViewById(R.id.tvQty)).setText(df.format(getDoubleVal(item, "ioqty")));
            
            double amt = getDoubleVal(item, "jsanamt") + getDoubleVal(item, "jsanvat");
            ((TextView) v.findViewById(R.id.tvAmt)).setText(df.format(amt));

            return v;
        }
    }
}
