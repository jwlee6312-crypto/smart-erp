package com.crmbank.erp.mobile.hsio;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
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
 * 🚀 [MHSIO010U] 입고요청등록
 * 웹 HSIO010U.vue 로직 및 MHSIO052U 디자인 패턴 통합 버전
 */
public class MHSIO010U extends BaseActivity {

    private TextView tvReqDate, tvReqDept;
    private EditText etReqNo, etRemarks;
    private ItemAdapter adapter;
    private final List<Map<String, Object>> orderItems = new ArrayList<>();
    private final Map<String, Object> masterData = new HashMap<>();
    private ApiService apiService;
    private String cmpycd, deptcd, userid;
    private String reqym = "", reqno = "0000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio010u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        deptcd = prefs.getString("deptcd", "");
        userid = prefs.getString("userId", "");

        apiService = RetrofitClient.getApiService();

        tvReqDate = findViewById(R.id.tvReqDate);
        tvReqDept = findViewById(R.id.tvReqDept);
        etReqNo = findViewById(R.id.etReqNo);
        etRemarks = findViewById(R.id.etRemarks);
        ListView lvItemList = findViewById(R.id.lvItemList);

        adapter = new ItemAdapter();
        lvItemList.setAdapter(adapter);

        tvReqDate.setOnClickListener(v -> showDatePicker(tvReqDate));
        tvReqDept.setOnClickListener(v -> openHelp("DEPT"));
        findViewById(R.id.btnAddItem).setOnClickListener(v -> openHelp("ITEM"));
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnReset).setOnClickListener(v -> initialize());
        findViewById(R.id.btnDelete).setOnClickListener(v -> handleFullDelete());
        findViewById(R.id.btnOrderSearch).setOnClickListener(v -> showRequestSearchPopup());

        tvReqDept.setText(prefs.getString("deptnm", ""));
        initialize();
    }

    private void initialize() {
        masterData.clear();
        orderItems.clear();
        reqym = ""; reqno = "0000";
        
        String todayYmd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvReqDate.setText(todayYmd);
        etReqNo.setText("");
        etRemarks.setText("");
        
        masterData.put("cmpycd", cmpycd);
        masterData.put("deptcd", deptcd);
        masterData.put("reqymd", todayYmd.replace("-", ""));
        masterData.put("sts", "N");

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            String date = String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d);
            tv.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openHelp(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setTitle(type.equals("DEPT") ? "부서 선택" : "품목 선택").setView(dialogView);
        
        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();

        PopupAdapter popupAdapter = new PopupAdapter(list, type, item -> {
            if (type.equals("DEPT")) {
                deptcd = getStringVal(item, "deptcd");
                tvReqDept.setText(getStringVal(item, "deptnm"));
                masterData.put("deptcd", deptcd);
            } else {
                Map<String, Object> row = new HashMap<>(item);
                row.put("reqqty", 1.0);
                row.put("imprice", getDoubleVal(item, "incost"));
                row.put("reqamt", getDoubleVal(item, "incost"));
                row.put("_status", "입력");
                orderItems.add(row);
                adapter.notifyDataSetChanged();
            }
            dialog.dismiss();
        });
        rv.setAdapter(popupAdapter);

        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("cmpycd", cmpycd);
            if (type.equals("DEPT")) {
                p.put("gubun", "D0"); p.put("remark", etSearch.getText().toString().trim());
                apiService.executeHa00Procedure("HA00_00P_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                        if (r.isSuccessful() && r.body() != null) { list.clear(); list.addAll(r.body()); popupAdapter.notifyDataSetChanged(); }
                    }
                    @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
                });
            } else {
                p.put("gubun", "I1"); p.put("gbncd", "3"); p.put("codenm", etSearch.getText().toString().trim());
                apiService.executeHs00Procedure("HS00_000S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                        if (r.isSuccessful() && r.body() != null) { list.clear(); list.addAll(r.body()); popupAdapter.notifyDataSetChanged(); }
                    }
                    @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
                });
            }
        });
        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        dialogView.findViewById(R.id.btnSearch).performClick();
    }

    private void showRequestSearchPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_purch_order_search, null);
        builder.setTitle("입고 요청 목록").setView(dialogView);

        TextView tvStart = dialogView.findViewById(R.id.tvPopStartDate);
        TextView tvEnd = dialogView.findViewById(R.id.tvPopEndDate);
        EditText etDept = dialogView.findViewById(R.id.etPopCustNm);
        etDept.setHint("부서명 입력");
        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvEnd.setText(sdf.format(cal.getTime()));
        cal.add(Calendar.MONTH, -1);
        tvStart.setText(sdf.format(cal.getTime()));

        tvStart.setOnClickListener(v -> showDatePicker(tvStart));
        tvEnd.setOnClickListener(v -> showDatePicker(tvEnd));

        List<Map<String, Object>> popList = new ArrayList<>();
        BaseAdapter popAdapter = new BaseAdapter() {
            @Override public int getCount() { return popList.size(); }
            @Override public Object getItem(int p) { return popList.get(p); }
            @Override public long getItemId(int p) { return p; }
            @Override public View getView(int p, View v, ViewGroup pr) {
                if (v == null) v = LayoutInflater.from(pr.getContext()).inflate(R.layout.item_request_search, pr, false);
                Map<String, Object> item = popList.get(p);
                ((TextView) v.findViewById(R.id.tvPopDeptNm)).setText(getStringVal(item, "deptnm"));
                ((TextView) v.findViewById(R.id.tvPopReqYmd)).setText(getStringVal(item, "reqymd"));
                String bNo = getStringVal(item, "reqym") + "-" + getStringVal(item, "reqno");
                ((TextView) v.findViewById(R.id.tvPopReqNo)).setText(bNo);
                return v;
            }
        };
        ListView lv = dialogView.findViewById(R.id.lvPopOrderList);
        lv.setAdapter(popAdapter);

        AlertDialog dialog = builder.create();
        dialogView.findViewById(R.id.btnPopSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("actkind", "L");
            p.put("cmpycd", cmpycd);
            p.put("fromdt", tvStart.getText().toString().replace("-", ""));
            p.put("todt", tvEnd.getText().toString().replace("-", ""));
            p.put("deptnm", etDept.getText().toString().trim());

            apiService.executeHsioProcedure("HSIO_010U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        popList.clear(); popList.addAll(response.body()); popAdapter.notifyDataSetChanged();
                    }
                }
                @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
            });
        });

        lv.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            fetchDetail(popList.get(position));
        });

        dialogView.findViewById(R.id.btnPopClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void fetchDetail(Map<String, Object> row) {
        reqym = getStringVal(row, "reqym");
        reqno = getStringVal(row, "reqno");
        
        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S"); p.put("cmpycd", cmpycd); p.put("reqym", reqym); p.put("reqno", reqno);

        apiService.executeHsioProcedure("HSIO_010U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Map<String, Object> mst = response.body().get(0);
                    masterData.clear(); masterData.putAll(mst);
                    etReqNo.setText(String.format("%s-%s", reqym, reqno));
                    tvReqDept.setText(getStringVal(mst, "deptnm"));
                    etRemarks.setText(getStringVal(mst, "remark"));
                    
                    String ymd = getStringVal(mst, "reqymd");
                    if (ymd.length() == 8) tvReqDate.setText(String.format("%s-%s-%s", ymd.substring(0,4), ymd.substring(4,6), ymd.substring(6,8)));
                    
                    fetchItems();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchItems() {
        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S"); p.put("cmpycd", cmpycd); p.put("reqym", reqym); p.put("reqno", reqno);
        apiService.executeHsioProcedure("HSIO_011U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    orderItems.clear();
                    for (Map<String, Object> item : response.body()) {
                        item.put("_status", ""); orderItems.add(item);
                    }
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void save() {
        if (orderItems.isEmpty()) return;

        Map<String, Object> mst = new HashMap<>(masterData);
        mst.put("actkind", reqno.equals("0000") ? "A0" : "U0");
        mst.put("cmpycd", cmpycd);
        mst.put("reqym", reqym.isEmpty() ? tvReqDate.getText().toString().replace("-", "").substring(0, 6) : reqym);
        mst.put("reqno", reqno);
        mst.put("reqymd", tvReqDate.getText().toString().replace("-", ""));
        mst.put("deptcd", deptcd);
        mst.put("remark", etRemarks.getText().toString());
        mst.put("asgbn", "N");
        mst.put("updemp", userid);

        List<Map<String, Object>> dtl = new ArrayList<>();
        for (Map<String, Object> item : orderItems) {
            String status = getStringVal(item, "_status");
            Map<String, Object> d = new HashMap<>(item);
            d.put("actkind", status.equals("입력") ? "A1" : (status.equals("삭제") ? "D1" : "U1"));
            d.put("updemp", userid);
            dtl.add(d);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("mst", mst);
        payload.put("dtl", dtl);

        apiService.saveHsio010U(payload).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(MHSIO010U.this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
                    fetchDetail(response.body().getData());
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void handleFullDelete() {
        if (reqno.equals("0000")) return;
        new AlertDialog.Builder(this).setTitle("전체 삭제").setMessage("요청 내역을 모두 삭제하시겠습니까?")
            .setPositiveButton("예", (d, w) -> {
                Map<String, Object> p = new HashMap<>();
                p.put("actkind", "D0"); p.put("cmpycd", cmpycd); p.put("reqym", reqym); p.put("reqno", reqno);
                apiService.executeHsioProcedure("HSIO_010U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                        initialize(); Toast.makeText(MHSIO010U.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
                });
            }).setNegativeButton("아니오", null).show();
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

    @Override protected String getProgramTitle() { return "입고요청등록"; }
    @Override protected String getProgramId() { return "MHSIO010U"; }

    private class ItemAdapter extends BaseAdapter {
        private final DecimalFormat df = new DecimalFormat("#,###");
        @Override public int getCount() { return orderItems.size(); }
        @Override public Object getItem(int p) { return orderItems.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSIO010U.this).inflate(R.layout.item_mhsio010u, pr, false);
            Map<String, Object> item = orderItems.get(p);
            
            ((TextView) v.findViewById(R.id.tvItemName)).setText(getStringVal(item, "itemnm"));
            EditText etQty = v.findViewById(R.id.etQuantity);
            EditText etPrice = v.findViewById(R.id.etPrice);
            TextView tvAmt = v.findViewById(R.id.etAmount);

            double qty = getDoubleVal(item, "reqqty");
            double price = getDoubleVal(item, "imprice");
            double amt = qty * price;

            etQty.setText(String.valueOf(qty));
            etPrice.setText(String.valueOf(price));
            tvAmt.setText(df.format(amt));

            if (getStringVal(item, "_status").equals("삭제")) v.setBackgroundColor(Color.LTGRAY);
            else v.setBackgroundColor(Color.WHITE);

            View.OnFocusChangeListener listener = (view, focus) -> {
                if (!focus) {
                    try {
                        double nQty = Double.parseDouble(etQty.getText().toString());
                        double nPrice = Double.parseDouble(etPrice.getText().toString());
                        item.put("reqqty", nQty);
                        item.put("imprice", nPrice);
                        item.put("reqamt", nQty * nPrice);
                        if (getStringVal(item, "_status").isEmpty()) item.put("_status", "수정");
                        notifyDataSetChanged();
                    } catch (Exception ignored) {}
                }
            };
            etQty.setOnFocusChangeListener(listener);
            etPrice.setOnFocusChangeListener(listener);

            v.findViewById(R.id.btnDelete).setOnClickListener(view -> {
                if (getStringVal(item, "_status").equals("입력")) orderItems.remove(p);
                else item.put("_status", getStringVal(item, "_status").equals("삭제") ? "" : "삭제");
                notifyDataSetChanged();
            });
            return v;
        }
    }
}
