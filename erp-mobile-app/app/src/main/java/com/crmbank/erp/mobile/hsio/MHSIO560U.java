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
import android.widget.CheckBox;
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
import com.crmbank.erp.mobile.CodeDto;
import com.crmbank.erp.mobile.PopupAdapter;
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
 * 🚀 [MHSIO560U] 주문출고취소
 * 웹 HSIO560U.vue 로직 및 MHSIO052U 프로토타입 기반 완성 버전
 */
public class MHSIO560U extends BaseActivity {

    private TextView tvDateFrom, tvDateTo;
    private EditText etCustNm;
    private Spinner spWarehouse;
    private ListView lvDetailList;
    private DetailAdapter adapter;
    private final List<Map<String, Object>> detailList = new ArrayList<>();
    private final Map<String, Object> masterData = new HashMap<>();
    private final List<CodeDto> whcdList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd, userid;
    private String selectedCustCd = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio560u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");

        apiService = RetrofitClient.getApiService();

        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        etCustNm = findViewById(R.id.etCustNm);
        spWarehouse = findViewById(R.id.spWarehouse);
        lvDetailList = findViewById(R.id.lvDetailList);

        adapter = new DetailAdapter();
        lvDetailList.setAdapter(adapter);

        tvDateFrom.setOnClickListener(v -> showDatePicker(tvDateFrom));
        tvDateTo.setOnClickListener(v -> showDatePicker(tvDateTo));
        etCustNm.setOnClickListener(v -> fetchCustList());

        findViewById(R.id.btnSearch).setOnClickListener(v -> fetchCustList());
        findViewById(R.id.btnCancelSave).setOnClickListener(v -> saveCancel());

        loadWarehouses();
        initialize();
    }

    private void initialize() {
        detailList.clear();
        masterData.clear();
        selectedCustCd = "";
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(cal.getTime());
        tvDateTo.setText(today);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        tvDateFrom.setText(sdf.format(cal.getTime()));
        
        etCustNm.setText("");

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
                    spWarehouse.setAdapter(new ArrayAdapter<>(MHSIO560U.this, android.R.layout.simple_spinner_item, names));
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
        builder.setTitle("출고 완료 거래처 조회").setView(dialogView);

        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<Map<String, Object>> popList = new ArrayList<>();
        AlertDialog dialog = builder.create();

        PopupAdapter popupAdapter = new PopupAdapter(popList, "CUST", item -> {
            selectedCustCd = getStringVal(item, "custcd");
            etCustNm.setText(getStringVal(item, "custnm"));
            dialog.dismiss();
            fetchDetails();
        });
        rv.setAdapter(popupAdapter);

        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("actkind", "S1");
            p.put("cmpycd", cmpycd);
            p.put("iogbn", "200");
            p.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
            p.put("todt", tvDateTo.getText().toString().replace("-", ""));
            p.put("whcd", getSelectedWhcd());
            p.put("custnm", etSearch.getText().toString().trim());

            apiService.executeHsioProcedure("HSIO_560U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
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
        p.put("iogbn", "200");
        p.put("custcd", selectedCustCd);
        p.put("whcd", getSelectedWhcd());
        p.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
        p.put("todt", tvDateTo.getText().toString().replace("-", ""));

        apiService.executeHsioProcedure("HSIO_560U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    detailList.clear();
                    for (Map<String, Object> item : response.body()) {
                        item.put("_checked", false);
                        detailList.add(item);
                    }
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void saveCancel() {
        List<Map<String, Object>> selected = new ArrayList<>();
        for (Map<String, Object> item : detailList) {
            if (Boolean.TRUE.equals(item.get("_checked"))) selected.add(item);
        }

        if (selected.isEmpty()) {
            Toast.makeText(this, "취소할 품목을 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 정산 여부 체크
        for (Map<String, Object> item : selected) {
            if ("Y".equals(getStringVal(item, "jyn"))) {
                Toast.makeText(this, "정산 완료된 자료는 취소할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        new AlertDialog.Builder(this).setTitle("출고 취소").setMessage("선택한 품목을 모두 취소하시겠습니까?")
            .setPositiveButton("예", (dialog, which) -> {
                for (int i = 0; i < selected.size(); i++) {
                    Map<String, Object> item = selected.get(i);
                    Map<String, Object> p = new HashMap<>();
                    p.put("actkind", "D0");
                    p.put("cmpycd", cmpycd);
                    p.put("iogbn", "200");
                    p.put("ioym", getStringVal(item, "ioym"));
                    p.put("iono", getStringVal(item, "iono"));
                    p.put("iorowno", getStringVal(item, "iorowno"));
                    p.put("whcd", getSelectedWhcd());
                    p.put("updemp", userid);

                    final boolean isLast = (i == selected.size() - 1);
                    apiService.executeHsioProcedure("HSIO_560U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                        @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                            if (isLast) {
                                Toast.makeText(MHSIO560U.this, "취소 처리가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                fetchDetails();
                            }
                        }
                        @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
                    });
                }
            }).setNegativeButton("아니오", null).show();
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

    @Override protected String getProgramTitle() { return "주문 출고 취소"; }
    @Override protected String getProgramId() { return "MHSIO560U"; }

    private class DetailAdapter extends BaseAdapter {
        private final DecimalFormat df = new DecimalFormat("#,###");
        @Override public int getCount() { return detailList.size(); }
        @Override public Object getItem(int p) { return detailList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSIO560U.this).inflate(R.layout.item_inbound_cancel, pr, false); // Reuse cancel layout
            Map<String, Object> item = detailList.get(p);
            
            CheckBox cb = v.findViewById(R.id.cbSelect);
            cb.setOnCheckedChangeListener(null);
            cb.setChecked(Boolean.TRUE.equals(item.get("_checked")));
            cb.setOnCheckedChangeListener((btn, isChecked) -> item.put("_checked", isChecked));

            ((TextView) v.findViewById(R.id.tvIoYmd)).setText(getStringVal(item, "ioymd"));
            ((TextView) v.findViewById(R.id.tvCustNm)).setText(getStringVal(item, "itemnm")); // 품명 표시
            ((TextView) v.findViewById(R.id.tvIoNo)).setText(getStringVal(item, "ioqty")); // 수량 표시
            
            double amt = 0;
            try { amt = Double.parseDouble(getStringVal(item, "jsanamt")) + Double.parseDouble(getStringVal(item, "jsanvat")); } catch (Exception ignored) {}
            ((TextView) v.findViewById(R.id.tvSumAmt)).setText(df.format(amt));

            return v;
        }
    }
}
