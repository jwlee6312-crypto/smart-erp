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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHSIO580U] 유통상품이동출고
 * 웹 HSIO580U.vue 로직 및 MHSIO052U 프로토타입 기반 완성 버전
 */
public class MHSIO580U extends BaseActivity {

    private TextView tvMoveDate, tvOutDept, tvInDept;
    private EditText etRemarks;
    private Spinner spOutWarehouse, spInWarehouse;
    private DetailAdapter adapter;
    private final List<Map<String, Object>> detailList = new ArrayList<>();
    private final List<CodeDto> whcdList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd, userid;
    private String outDeptCd = "", inDeptCd = "";
    private String ioym = "", iono = "", ino = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio580u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");
        outDeptCd = prefs.getString("deptcd", "");
        String outDeptNm = prefs.getString("deptnm", "");

        apiService = RetrofitClient.getApiService();

        tvMoveDate = findViewById(R.id.tvMoveDate);
        tvOutDept = findViewById(R.id.tvOutDept);
        tvInDept = findViewById(R.id.tvInDept);
        etRemarks = findViewById(R.id.etRemarks);
        spOutWarehouse = findViewById(R.id.spOutWarehouse);
        spInWarehouse = findViewById(R.id.spInWarehouse);
        ListView lvDetailList = findViewById(R.id.lvDetailList);

        adapter = new DetailAdapter();
        lvDetailList.setAdapter(adapter);

        tvMoveDate.setOnClickListener(v -> showDatePicker(tvMoveDate));
        tvOutDept.setOnClickListener(v -> openDeptHelp("OUT"));
        tvInDept.setOnClickListener(v -> openDeptHelp("IN"));
        findViewById(R.id.btnAddItem).setOnClickListener(v -> openItemHelp());
        findViewById(R.id.btnSearch).setOnClickListener(v -> showSearchPopup());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnReset).setOnClickListener(v -> initialize());

        tvOutDept.setText(outDeptNm);
        loadWarehouses();
        initialize();
    }

    private void initialize() {
        detailList.clear();
        ioym = ""; iono = ""; ino = "";
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvMoveDate.setText(sdf.format(cal.getTime()));
        
        etRemarks.setText("");
        tvInDept.setText("부서 선택");
        inDeptCd = "";

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadWarehouses() {
        Map<String, Object> p = new HashMap<>();
        p.put("gubun", "W0"); p.put("cmpycd", cmpycd);
        apiService.executeHs00Procedure("HS00_000S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
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
                    ArrayAdapter<String> whAdapter = new ArrayAdapter<>(MHSIO580U.this, android.R.layout.simple_spinner_item, names);
                    spOutWarehouse.setAdapter(whAdapter);
                    spInWarehouse.setAdapter(whAdapter);
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

    private void openDeptHelp(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setTitle("부서 선택").setView(dialogView);
        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();
        PopupAdapter popupAdapter = new PopupAdapter(list, "DEPT", item -> {
            if (type.equals("OUT")) {
                outDeptCd = getStringVal(item, "deptcd");
                tvOutDept.setText(getStringVal(item, "deptnm"));
            } else {
                inDeptCd = getStringVal(item, "deptcd");
                tvInDept.setText(getStringVal(item, "deptnm"));
            }
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

    private void openItemHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setTitle("품목 선택").setView(dialogView);
        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();
        PopupAdapter popupAdapter = new PopupAdapter(list, "ITEM", item -> {
            Map<String, Object> row = new HashMap<>(item);
            row.put("ioqty", "1");
            row.put("_status", "입력");
            detailList.add(row);
            adapter.notifyDataSetChanged();
            dialog.dismiss();
        });
        rv.setAdapter(popupAdapter);
        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("gubun", "I1"); p.put("cmpycd", cmpycd); p.put("gbncd", "2"); 
            p.put("codenm", etSearch.getText().toString().trim());
            apiService.executeHs00Procedure("HS00_000S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
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
    }

    private void showSearchPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setTitle("이동 목록 조회").setView(dialogView);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();
        PopupAdapter popupAdapter = new PopupAdapter(list, "MOVE", item -> {
            fetchDetail(item);
            dialog.dismiss();
        });
        rv.setAdapter(popupAdapter);
        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("actkind", "L"); p.put("cmpycd", cmpycd); p.put("iogbn", "200");
            p.put("fromdt", tvMoveDate.getText().toString().replace("-", ""));
            p.put("todt", tvMoveDate.getText().toString().replace("-", ""));
            apiService.executeHsioProcedure("HSIO_580U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
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

    private void fetchDetail(Map<String, Object> master) {
        ioym = getStringVal(master, "ioym");
        iono = getStringVal(master, "iono");
        ino = getStringVal(master, "ino");
        outDeptCd = getStringVal(master, "odeptcd");
        inDeptCd = getStringVal(master, "ideptcd");
        tvOutDept.setText(getStringVal(master, "odeptnm"));
        tvInDept.setText(getStringVal(master, "ideptnm"));
        etRemarks.setText(getStringVal(master, "remark"));
        
        String ymd = getStringVal(master, "ioymd");
        if (ymd.length() == 8) tvMoveDate.setText(String.format("%s-%s-%s", ymd.substring(0,4), ymd.substring(4,6), ymd.substring(6,8)));

        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S"); p.put("cmpycd", cmpycd); p.put("iogbn", "200");
        p.put("ioym", ioym); p.put("iono", iono);
        apiService.executeHsioProcedure("HSIO_581U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    detailList.clear();
                    for (Map<String, Object> item : response.body()) {
                        item.put("_status", "");
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
        if (inDeptCd.isEmpty()) { Toast.makeText(this, "입고부서를 선택하세요.", Toast.LENGTH_SHORT).show(); return; }

        Map<String, Object> mst = new HashMap<>();
        mst.put("cmpycd", cmpycd);
        mst.put("ioym", ioym.isEmpty() ? tvMoveDate.getText().toString().replace("-", "").substring(0, 6) : ioym);
        mst.put("iono", iono);
        mst.put("ioymd", tvMoveDate.getText().toString().replace("-", ""));
        mst.put("iogbn", "200");
        mst.put("odeptcd", outDeptCd);
        mst.put("owhcd", whcdList.get(spOutWarehouse.getSelectedItemPosition()).codecd);
        mst.put("ideptcd", inDeptCd);
        mst.put("iwhcd", whcdList.get(spInWarehouse.getSelectedItemPosition()).codecd);
        mst.put("remark", etRemarks.getText().toString());
        mst.put("userid", userid);
        mst.put("actkind", iono.isEmpty() ? "A" : "U");

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> item : detailList) {
            Map<String, Object> d = new HashMap<>(item);
            String status = (String) item.get("_status");
            d.put("actkind", status.equals("입력") ? "A" : (status.equals("삭제") ? "D" : "U"));
            items.add(d);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("mst", mst);
        payload.put("dtl", items);

        apiService.saveHsio580U(payload).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSIO580U.this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
                    initialize();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    @Override protected String getProgramTitle() { return "유통상품이동출고"; }
    @Override protected String getProgramId() { return "MHSIO580U"; }

    private class DetailAdapter extends BaseAdapter {
        @Override public int getCount() { return detailList.size(); }
        @Override public Object getItem(int p) { return detailList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSIO580U.this).inflate(R.layout.item_mhsio580u, pr, false);
            Map<String, Object> item = detailList.get(p);
            ((TextView) v.findViewById(R.id.tvItemNm)).setText(getStringVal(item, "itemnm"));
            ((TextView) v.findViewById(R.id.tvSize)).setText(getStringVal(item, "itsize"));
            
            EditText etQty = v.findViewById(R.id.etQty);
            etQty.setText(getStringVal(item, "ioqty"));
            etQty.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) { 
                    item.put("ioqty", s.toString()); 
                    if (getStringVal(item, "_status").isEmpty()) item.put("_status", "수정");
                }
            });

            v.findViewById(R.id.btnDelete).setOnClickListener(v1 -> {
                if (getStringVal(item, "_status").equals("입력")) detailList.remove(p);
                else item.put("_status", "삭제");
                notifyDataSetChanged();
            });

            v.setVisibility(getStringVal(item, "_status").equals("삭제") ? View.GONE : View.VISIBLE);
            return v;
        }
    }
}
