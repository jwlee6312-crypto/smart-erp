package com.crmbank.erp.mobile.hsio;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHSIO120U] 입고정산취소
 * 웹 HSIO120U.vue 로직 및 MHSIO052U 프로토타입 기반 완성 버전
 */
public class MHSIO120U extends BaseActivity {

    private TextView tvDateFrom, tvDateTo, tvSearchDept;
    private EditText etCustNm;
    private ListView lvSettleList;
    private SettleAdapter adapter;
    private final List<Map<String, Object>> settleList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd, userid, searchDeptCd, searchDeptNm;
    private String selectedCustCd = "";
    private final DecimalFormat df = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio120u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");
        searchDeptCd = prefs.getString("deptcd", "");
        searchDeptNm = prefs.getString("deptnm", "");

        apiService = RetrofitClient.getApiService();

        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        tvSearchDept = findViewById(R.id.tvSearchDept);
        etCustNm = findViewById(R.id.etCustNm);
        lvSettleList = findViewById(R.id.lvSettleList);

        adapter = new SettleAdapter();
        lvSettleList.setAdapter(adapter);

        tvDateFrom.setOnClickListener(v -> showDatePicker(tvDateFrom));
        tvDateTo.setOnClickListener(v -> showDatePicker(tvDateTo));
        tvSearchDept.setOnClickListener(v -> openHelp("S_DEPT"));
        etCustNm.setOnClickListener(v -> fetchCustomerList());

        findViewById(R.id.btnSearch).setOnClickListener(v -> fetchCustomerList());
        findViewById(R.id.btnCancelSettle).setOnClickListener(v -> cancelSettlement());

        initialize();
    }

    private void initialize() {
        settleList.clear();
        selectedCustCd = "";
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(cal.getTime());
        tvDateTo.setText(today);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        tvDateFrom.setText(sdf.format(cal.getTime()));
        
        tvSearchDept.setText(searchDeptNm);
        etCustNm.setText("");

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> 
            tv.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d)), 
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void fetchCustomerList() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setTitle("매입 정산 취소 대상 조회").setView(dialogView);

        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<Map<String, Object>> popList = new ArrayList<>();
        AlertDialog dialog = builder.create();

        PopupAdapter popupAdapter = new PopupAdapter(popList, "CUST", item -> {
            selectedCustCd = getStringVal(item, "custcd");
            etCustNm.setText(getStringVal(item, "custnm"));
            dialog.dismiss();
            fetchSettlementList();
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

            apiService.executeHsioProcedure("HSIO_120U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
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

    private void fetchSettlementList() {
        if (selectedCustCd.isEmpty()) return;

        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S0");
        p.put("cmpycd", cmpycd);
        p.put("iogbn", "100");
        p.put("custcd", selectedCustCd);
        p.put("deptcd", searchDeptCd);
        p.put("fromdt", tvDateFrom.getText().toString().replace("-", ""));
        p.put("todt", tvDateTo.getText().toString().replace("-", ""));

        apiService.executeHsioProcedure("HSIO_120U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    settleList.clear();
                    for (Map<String, Object> item : response.body()) {
                        item.put("_checked", false);
                        settleList.add(item);
                    }
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void cancelSettlement() {
        List<Map<String, Object>> selected = new ArrayList<>();
        for (Map<String, Object> item : settleList) {
            if (Boolean.TRUE.equals(item.get("_checked"))) selected.add(item);
        }
        if (selected.isEmpty()) return;

        new AlertDialog.Builder(this).setTitle("정산 취소").setMessage("선택한 자료를 취소하시겠습니까?")
            .setPositiveButton("예", (d, w) -> {
                for (int i = 0; i < selected.size(); i++) {
                    Map<String, Object> item = selected.get(i);
                    Map<String, Object> p = new HashMap<>();
                    p.put("actkind", "D0");
                    p.put("cmpycd", cmpycd);
                    p.put("iogbn", "100");
                    p.put("deptcd", searchDeptCd);
                    p.put("jsanym", getStringVal(item, "jsanym"));
                    p.put("jsanno", getStringVal(item, "jsanno"));
                    p.put("userid", userid);

                    final boolean isLast = (i == selected.size() - 1);
                    apiService.executeHsioProcedure("HSIO_120U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                            if (isLast) {
                                Toast.makeText(MHSIO120U.this, "취소되었습니다.", Toast.LENGTH_SHORT).show();
                                initialize();
                            }
                        }
                        @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
                    });
                }
            }).setNegativeButton("아니오", null).show();
    }

    private void openHelp(String type) {
        if (type.equals("S_DEPT")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
            builder.setTitle("부서 선택").setView(dialogView);
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
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    @Override protected String getProgramTitle() { return "매입 정산 취소"; }
    @Override protected String getProgramId() { return "MHSIO120U"; }

    private class SettleAdapter extends BaseAdapter {
        @Override public int getCount() { return settleList.size(); }
        @Override public Object getItem(int p) { return settleList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSIO120U.this).inflate(R.layout.item_purchase_settle, pr, false);
            Map<String, Object> item = settleList.get(p);
            
            CheckBox cb = v.findViewById(R.id.cbSelect);
            cb.setOnCheckedChangeListener(null);
            cb.setChecked(Boolean.TRUE.equals(item.get("_checked")));
            cb.setOnCheckedChangeListener((btn, isChecked) -> item.put("_checked", isChecked));

            ((TextView) v.findViewById(R.id.tvSalsYmd)).setText(getStringVal(item, "jsanymd"));
            ((TextView) v.findViewById(R.id.tvItemName)).setText(getStringVal(item, "deptnm"));
            
            double sum = Double.parseDouble(getStringVal(item, "spyamt")) + Double.parseDouble(getStringVal(item, "vatamt"));
            ((TextView) v.findViewById(R.id.tvSumAmt)).setText(df.format(sum));

            return v;
        }
    }
}
