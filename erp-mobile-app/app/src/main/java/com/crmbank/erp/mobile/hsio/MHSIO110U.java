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
import com.crmbank.erp.mobile.ApiResponse;
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
 * 🚀 [MHSIO110U] 매입정산
 * MHSIO052U 프로토타입 및 HSIO110U.vue 로직 적용 완료 버전
 */
public class MHSIO110U extends BaseActivity {

    private TextView tvDateFrom, tvDateTo, tvIssueDate, tvSearchDept;
    private EditText etCustNm, etTotalAmt;
    private Spinner spTaxUnit, spVatType;
    private SettleAdapter adapter;
    private final List<Map<String, Object>> detailList = new ArrayList<>();
    private final List<Map<String, Object>> taxUnitList = new ArrayList<>();
    private final List<Map<String, Object>> vatTypeList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd, userid, searchDeptCd, searchDeptNm;
    private String selectedCustCd = "", selectedDeptCd = "";
    private final DecimalFormat df = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio110u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");
        searchDeptCd = prefs.getString("deptcd", "");
        searchDeptNm = prefs.getString("deptnm", "");

        apiService = RetrofitClient.getApiService();

        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        tvIssueDate = findViewById(R.id.tvIssueDate);
        tvSearchDept = findViewById(R.id.tvSearchDept);
        etCustNm = findViewById(R.id.etCustomerName);
        etTotalAmt = findViewById(R.id.etTotalAmt);
        spTaxUnit = findViewById(R.id.spTaxUnit);
        spVatType = findViewById(R.id.spVatType);
        ListView lvSettleList = findViewById(R.id.lvSettleList);

        adapter = new SettleAdapter();
        lvSettleList.setAdapter(adapter);

        tvDateFrom.setOnClickListener(v -> showDatePicker(tvDateFrom));
        tvDateTo.setOnClickListener(v -> showDatePicker(tvDateTo));
        tvIssueDate.setOnClickListener(v -> showDatePicker(tvIssueDate));
        tvSearchDept.setOnClickListener(v -> openHelp("S_DEPT"));
        etCustNm.setOnClickListener(v -> openHelp("CUST_LIST"));

        findViewById(R.id.btnSearch).setOnClickListener(v -> fetchCustList());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnReset).setOnClickListener(v -> initialize());

        loadOptions();
        initialize();
    }

    private void initialize() {
        detailList.clear();
        selectedCustCd = "";
        selectedDeptCd = "";
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(cal.getTime());
        tvDateTo.setText(today);
        tvIssueDate.setText(today);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        tvDateFrom.setText(sdf.format(cal.getTime()));
        
        tvSearchDept.setText(searchDeptNm);
        etCustNm.setText("");
        etTotalAmt.setText("0");

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadOptions() {
        // 사업장 (SA)
        apiService.executeHa00Procedure("HA00_00P_STR", new HashMap<String, Object>() {{
            put("gubun", "SA"); put("cmpycd", cmpycd);
        }}).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    taxUnitList.clear(); taxUnitList.addAll(response.body());
                    List<String> names = new ArrayList<>();
                    for (Map<String, Object> m : taxUnitList) names.add(getStringVal(m, "unitnm"));
                    spTaxUnit.setAdapter(new ArrayAdapter<>(MHSIO110U.this, android.R.layout.simple_spinner_item, names));
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });

        // 과세유형 (120)
        apiService.executeHa00Procedure("HA00_00P_STR", new HashMap<String, Object>() {{
            put("gubun", "E0"); put("cmpycd", cmpycd); put("gbncd", "120");
        }}).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    vatTypeList.clear(); vatTypeList.addAll(response.body());
                    List<String> names = new ArrayList<>();
                    for (Map<String, Object> m : vatTypeList) names.add(getStringVal(m, "codenm"));
                    spVatType.setAdapter(new ArrayAdapter<>(MHSIO110U.this, android.R.layout.simple_spinner_item, names));
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
        builder.setTitle("매입 정산 대상 조회").setView(dialogView);

        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<Map<String, Object>> popList = new ArrayList<>();
        AlertDialog dialog = builder.create();

        PopupAdapter popupAdapter = new PopupAdapter(popList, "CUST", item -> {
            selectedCustCd = getStringVal(item, "custcd");
            selectedDeptCd = getStringVal(item, "deptcd");
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
            p.put("deptcd", searchDeptCd);
            p.put("custnm", etSearch.getText().toString().trim());

            apiService.executeHsioProcedure("HSIO_110U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
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
        if (selectedCustCd.isEmpty()) return;

        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S0");
        p.put("cmpycd", cmpycd);
        p.put("iogbn", "100");
        p.put("custcd", selectedCustCd);
        p.put("deptcd", searchDeptCd);
        p.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
        p.put("todt", tvDateTo.getText().toString().replace("-", ""));

        apiService.executeHsioProcedure("HSIO_110U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    detailList.clear();
                    for (Map<String, Object> item : response.body()) {
                        item.put("_checked", true); // Default select all
                        detailList.add(item);
                    }
                    adapter.notifyDataSetChanged();
                    updateTotalAmt();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void updateTotalAmt() {
        double total = 0;
        for (Map<String, Object> item : detailList) {
            if (Boolean.TRUE.equals(item.get("_checked"))) {
                total += getDoubleVal(item, "jsanamt") + getDoubleVal(item, "jsanvat");
            }
        }
        etTotalAmt.setText(df.format(total));
    }

    private void save() {
        List<Map<String, Object>> selected = new ArrayList<>();
        double totalSpl = 0, totalVat = 0;
        for (Map<String, Object> item : detailList) {
            if (Boolean.TRUE.equals(item.get("_checked"))) {
                selected.add(item);
                totalSpl += getDoubleVal(item, "jsanamt");
                totalVat += getDoubleVal(item, "jsanvat");
            }
        }

        if (selected.isEmpty()) {
            Toast.makeText(this, "정산 처리할 품목을 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String taxUnit = getStringVal(taxUnitList.get(spTaxUnit.getSelectedItemPosition()), "taxunit");
        String vatType = getStringVal(vatTypeList.get(spVatType.getSelectedItemPosition()), "codecd");

        // 1. Create Master (A0)
        Map<String, Object> mst = new HashMap<>();
        mst.put("actkind", "A0");
        mst.put("cmpycd", cmpycd);
        mst.put("iogbn", "100");
        mst.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
        mst.put("todt", tvDateTo.getText().toString().replace("-", ""));
        mst.put("custcd", selectedCustCd);
        mst.put("deptcd", selectedDeptCd);
        mst.put("taxunit", taxUnit);
        mst.put("vattype", vatType);
        mst.put("jsanymd", tvIssueDate.getText().toString().replace("-", ""));
        mst.put("jsanamt", totalSpl);
        mst.put("jsanvat", totalVat);
        mst.put("updemp", userid);

        apiService.executeHsioProcedure("HSIO_110U_STR", mst).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Map<String, Object> res = response.body().get(0);
                    String jsanym = getStringVal(res, "jsanym");
                    String jsanno = getStringVal(res, "jsanno");

                    if (jsanym.equals("000000")) {
                        Toast.makeText(MHSIO110U.this, "정산 오류: " + jsanno, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // 2. Link Items (U0)
                    for (int i = 0; i < selected.size(); i++) {
                        Map<String, Object> item = selected.get(i);
                        Map<String, Object> d = new HashMap<>();
                        d.put("actkind", "U0");
                        d.put("cmpycd", cmpycd);
                        d.put("iogbn", "100");
                        d.put("fromdt", mst.get("fromdt"));
                        d.put("todt", mst.get("todt"));
                        d.put("custcd", selectedCustCd);
                        d.put("deptcd", selectedDeptCd);
                        d.put("ioym", item.get("ioym"));
                        d.put("iono", item.get("iono"));
                        d.put("iorowno", item.get("iorowno"));
                        d.put("taxunit", taxUnit);
                        d.put("vattype", vatType);
                        d.put("jsanym", jsanym);
                        d.put("jsanno", jsanno);
                        d.put("jsanymd", mst.get("jsanymd"));
                        d.put("jsanamt", getDoubleVal(item, "jsanamt"));
                        d.put("jsanvat", getDoubleVal(item, "jsanvat"));
                        d.put("updemp", userid);

                        final boolean isLast = (i == selected.size() - 1);
                        apiService.executeHsioProcedure("HSIO_110U_STR", d).enqueue(new Callback<List<Map<String, Object>>>() {
                            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                                if (isLast) {
                                    Toast.makeText(MHSIO110U.this, "정산 처리가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                    initialize();
                                }
                            }
                            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
                        });
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void openHelp(String type) {
        if (type.equals("S_DEPT")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
            builder.setTitle("부서 선택").setView(dialogView);
            TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
            if (tvTitle != null) tvTitle.setText("부서 선택");

            EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
            RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
            rv.setLayoutManager(new LinearLayoutManager(this));

            List<Map<String, Object>> list = new ArrayList<>();
            AlertDialog dialog = builder.create();

            PopupAdapter popupAdapter = new PopupAdapter(list, "DEPT", item -> {
                searchDeptCd = getStringVal(item, "deptcd");
                searchDeptNm = getStringVal(item, "deptnm");
                tvSearchDept.setText(searchDeptNm);
                dialog.dismiss();
            });
            rv.setAdapter(popupAdapter);

            dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
                Map<String, Object> p = new HashMap<>();
                p.put("gubun", "D0"); p.put("cmpycd", cmpycd); p.put("gbncd", ""); p.put("code", ""); 
                p.put("remark", etSearch.getText().toString().trim());
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
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    private double getDoubleVal(Map<String, Object> map, String key) {
        try { return Double.parseDouble(getStringVal(map, key).replace(",", "")); } catch (Exception e) { return 0; }
    }

    @Override protected String getProgramTitle() { return "매입 정산"; }
    @Override protected String getProgramId() { return "MHSIO110U"; }

    private class SettleAdapter extends BaseAdapter {
        @Override public int getCount() { return detailList.size(); }
        @Override public Object getItem(int p) { return detailList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSIO110U.this).inflate(R.layout.item_purchase_settle, pr, false);
            Map<String, Object> item = detailList.get(p);
            
            CheckBox cb = v.findViewById(R.id.cbSelect);
            cb.setOnCheckedChangeListener(null);
            cb.setChecked(Boolean.TRUE.equals(item.get("_checked")));
            cb.setOnCheckedChangeListener((btn, isChecked) -> {
                item.put("_checked", isChecked);
                updateTotalAmt();
            });

            ((TextView) v.findViewById(R.id.tvSalsYmd)).setText(getStringVal(item, "salsymd"));
            ((TextView) v.findViewById(R.id.tvItemName)).setText(getStringVal(item, "itemnm"));
            
            double amt = getDoubleVal(item, "jsanamt") + getDoubleVal(item, "jsanvat");
            ((TextView) v.findViewById(R.id.tvSumAmt)).setText(df.format(amt));
            
            return v;
        }
    }
}
