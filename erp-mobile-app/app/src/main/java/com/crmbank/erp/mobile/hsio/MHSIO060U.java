package com.crmbank.erp.mobile.hsio;

import android.app.AlertDialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.RetrofitClient;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.ApiResponse;
import com.crmbank.erp.mobile.CodeDto;
import com.crmbank.erp.mobile.PopupAdapter;
import com.crmbank.erp.mobile.R;

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
 * 🚀 [MHSIO060U] 입고처리
 * 웹 HSIO060U.vue 로직 및 MHSIO052U 프로토타입 기반 완성 버전
 */
public class MHSIO060U extends BaseActivity {

    private TextView tvDateFrom, tvDateTo, tvInboundDate;
    private EditText etCustNm, etRemarks;
    private Spinner spWarehouse;
    private DetailAdapter adapter;
    private final List<Map<String, Object>> detailList = new ArrayList<>();
    private final Map<String, Object> masterData = new HashMap<>();
    private final List<CodeDto> whcdList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd, userid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio060u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");

        apiService = RetrofitClient.getApiService();

        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        tvInboundDate = findViewById(R.id.tvInboundDate);
        etCustNm = findViewById(R.id.etCustNm);
        etRemarks = findViewById(R.id.etRemarks);
        spWarehouse = findViewById(R.id.spWarehouse);
        ListView lvDetailList = findViewById(R.id.lvDetailList);

        adapter = new DetailAdapter();
        lvDetailList.setAdapter(adapter);

        tvDateFrom.setOnClickListener(v -> showDatePicker(tvDateFrom));
        tvDateTo.setOnClickListener(v -> showDatePicker(tvDateTo));
        tvInboundDate.setOnClickListener(v -> showDatePicker(tvInboundDate));
        etCustNm.setOnClickListener(v -> fetchCustList());

        findViewById(R.id.btnSearch).setOnClickListener(v -> fetchCustList());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnReset).setOnClickListener(v -> initialize());

        loadWarehouses();
        initialize();
    }

    private void initialize() {
        detailList.clear();
        masterData.clear();
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(cal.getTime());
        tvDateTo.setText(today);
        tvInboundDate.setText(today);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        tvDateFrom.setText(sdf.format(cal.getTime()));
        
        etCustNm.setText("");
        etRemarks.setText("");

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadWarehouses() {
        apiService.executeHs00Procedure("HS00_000S_STR", new HashMap<String, Object>() {{
            put("gubun", "W0"); put("cmpycd", cmpycd);
        }}).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    whcdList.clear();
                    List<String> names = new ArrayList<>();
                    for (Map<String, Object> m : response.body()) {
                        CodeDto dto = new CodeDto();
                        dto.codecd = getStringVal(m, "whcd");
                        dto.codenm = getStringVal(m, "whnm");
                        whcdList.add(dto);
                        names.add(dto.codenm);
                    }
                    spWarehouse.setAdapter(new ArrayAdapter<>(MHSIO060U.this, android.R.layout.simple_spinner_item, names));
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

    private void fetchCustList() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setTitle("미입고 거래처 조회").setView(dialogView);

        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<Map<String, Object>> popList = new ArrayList<>();
        AlertDialog dialog = builder.create();

        PopupAdapter popupAdapter = new PopupAdapter(popList, "CUST", item -> {
            masterData.putAll(item);
            etCustNm.setText(getStringVal(item, "custnm"));
            dialog.dismiss();
            fetchDetails();
        });
        rv.setAdapter(popupAdapter);

        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("actkind", "S1");
            p.put("cmpycd", cmpycd);
            p.put("iogbn", "100");
            p.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
            p.put("todt", tvDateTo.getText().toString().replace("-", ""));
            p.put("custnm", etSearch.getText().toString().trim());

            apiService.executeHsioProcedure("HSIO_060U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                    if (r.isSuccessful() && r.body() != null) {
                        popList.clear(); popList.addAll(r.body()); popupAdapter.notifyDataSetChanged();
                    }
                }
                @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
            });
        });

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        dialogView.findViewById(R.id.btnSearch).performClick();
    }

    private void fetchDetails() {
        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S0");
        p.put("cmpycd", cmpycd);
        p.put("iogbn", "100");
        p.put("custcd", getStringVal(masterData, "custcd"));
        p.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
        p.put("todt", tvDateTo.getText().toString().replace("-", ""));

        apiService.executeHsioProcedure("HSIO_060U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    detailList.clear();
                    for (Map<String, Object> item : response.body()) {
                        item.put("ioqty", getStringVal(item, "janqty"));
                        detailList.add(item);
                    }
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void save() {
        if (detailList.isEmpty()) return;

        Map<String, Object> mst = new HashMap<>();
        mst.put("cmpycd", cmpycd);
        mst.put("custcd", getStringVal(masterData, "custcd"));
        mst.put("whcd", getSelectedWhcd());
        mst.put("ioymd", tvInboundDate.getText().toString().replace("-", ""));
        mst.put("remark", etRemarks.getText().toString());
        mst.put("userid", userid);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> item : detailList) {
            Map<String, Object> d = new HashMap<>(item);
            d.put("ioqty", getStringVal(item, "ioqty"));
            items.add(d);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("mst", mst);
        payload.put("items", items);

        apiService.saveHsio060U(payload).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSIO060U.this, "입고 처리가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    initialize();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private String getSelectedWhcd() {
        if (spWarehouse.getSelectedItemPosition() < 0) return "";
        return whcdList.get(spWarehouse.getSelectedItemPosition()).codecd;
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    @Override protected String getProgramTitle() { return "입고 처리"; }
    @Override protected String getProgramId() { return "MHSIO060U"; }

    private class DetailAdapter extends BaseAdapter {
        @Override public int getCount() { return detailList.size(); }
        @Override public Object getItem(int p) { return detailList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSIO060U.this).inflate(R.layout.item_outbound_register, pr, false); // Reuse outbound layout for similar structure
            Map<String, Object> item = detailList.get(p);
            
            ((TextView) v.findViewById(R.id.tvItemName)).setText(getStringVal(item, "itemnm"));
            ((TextView) v.findViewById(R.id.tvOrderQty)).setText(getStringVal(item, "janqty"));
            
            EditText etIo = v.findViewById(R.id.etOutboundQty);
            etIo.setText(getStringVal(item, "ioqty"));
            etIo.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) { item.put("ioqty", s.toString()); }
            });
            
            return v;
        }
    }
}
