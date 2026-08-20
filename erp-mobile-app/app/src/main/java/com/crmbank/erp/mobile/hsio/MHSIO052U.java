package com.crmbank.erp.mobile.hsio;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Button;
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
 * 🚀 [MHSIO052U] 일반발주등록
 * 4,700건+ 대량 데이터 고성능 네비처리(RecyclerView) 및 어댑터 동기화 완결 버전
 */
public class MHSIO052U extends BaseActivity {

    private static final String TAG = "MHSIO052U";
    private TextView tvOrderDate;
    private EditText etOrderNo, etCustomerName, etEmail, etRemarks;
    private OrderAdapter adapter;
    private final List<Map<String, Object>> orderItems = new ArrayList<>();
    private final Map<String, Object> masterData = new HashMap<>();
    private ApiService apiService;
    private String cmpycd, deptcd, userid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio052u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        deptcd = prefs.getString("deptcd", "");
        deptcd = prefs.getString("deptcd", "");
        userid = prefs.getString("userId", "");

        apiService = RetrofitClient.getApiService();

        tvOrderDate = findViewById(R.id.tvOrderDate);
        etOrderNo = findViewById(R.id.etOrderNo);
        etCustomerName = findViewById(R.id.etCustomerName);
        etEmail = findViewById(R.id.etEmail);
        etRemarks = findViewById(R.id.etRemarks);
        ListView lvOrderList = findViewById(R.id.lvOrderList);

        adapter = new OrderAdapter();
        lvOrderList.setAdapter(adapter);

        initialize();

        tvOrderDate.setOnClickListener(v -> showDatePicker(tvOrderDate));
        etCustomerName.setOnClickListener(v -> openHelp("CUST"));

        findViewById(R.id.btnAddItem).setOnClickListener(v -> openHelp("ITEM"));
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnReset).setOnClickListener(v -> initialize());
        findViewById(R.id.btnDelete).setOnClickListener(v -> handleFullDelete());
        findViewById(R.id.btnOrderSearch).setOnClickListener(v -> showOrderSearchPopup());
    }

    private void initialize() {
        masterData.clear();
        String todayYmd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        masterData.put("balym", todayYmd.replace("-", "").substring(0, 6));
        masterData.put("balno", "0000");
        masterData.put("balymd", todayYmd);
        masterData.put("reqymd", todayYmd);
        masterData.put("deptcd", deptcd);
        masterData.put("deptcd", deptcd);
        masterData.put("custcd", "");
        masterData.put("custnm", "");
        masterData.put("remark", "");
        masterData.put("email", "");
        masterData.put("remark", "");
        masterData.put("email", "");
        masterData.put("balgb", "2");

        etOrderNo.setText("");
        etOrderNo.setHint("(자동 생성)");
        etOrderNo.setHint("(자동 생성)");
        etCustomerName.setText("");
        etEmail.setText("");
        etRemarks.setText("");
        tvOrderDate.setText(todayYmd);
        
        orderItems.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            String date = String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d);
            tv.setText(date);
            if (tv.getId() == R.id.tvOrderDate) {
                masterData.put("balymd", date.replace("-", ""));
                masterData.put("balym", date.replace("-", "").substring(0, 6));
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showOrderSearchPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_purch_order_search, null);
        builder.setTitle("발주 목록 조회").setView(dialogView);

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
                if (v == null) v = LayoutInflater.from(pr.getContext()).inflate(R.layout.item_mhsio052u_pop, pr, false);
                Map<String, Object> item = popList.get(p);
                ((TextView) v.findViewById(R.id.tvPopCustNm)).setText(getStringVal(item, "custnm"));
                ((TextView) v.findViewById(R.id.tvPopBalYmd)).setText(getStringVal(item, "balymd"));
                String bNo = getStringVal(item, "balym") + "-" + getStringVal(item, "balno");
                ((TextView) v.findViewById(R.id.tvPopBalno)).setText(bNo);
                return v;
            }
        };
        lv.setAdapter(popAdapter);

        AlertDialog dialog = builder.create();
        btnPopSearch.setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("actkind", "L");
            p.put("cmpycd", cmpycd);
            p.put("fromdt", tvPopStart.getText().toString().replace("-", ""));
            p.put("todt", tvPopEnd.getText().toString().replace("-", ""));
            p.put("gubun", "2");
            p.put("custnm", etPopCust.getText().toString().trim());

            apiService.executeHsioProcedure("HSIO_052U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        popList.clear();
                        popList.addAll(response.body());
                        popAdapter.notifyDataSetChanged();
                    } else if (response.code() == 401) { handleSessionExpired(); }
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

    private void fetchDetail(Map<String, Object> row) {
        String balym = getStringVal(row, "balym");
        String balno = getStringVal(row, "balno");
        
        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S");
        p.put("cmpycd", cmpycd);
        p.put("balym", balym);
        p.put("balno", balno);

        apiService.executeHsioProcedure("HSIO_052U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Map<String, Object> mst = response.body().get(0);
                    masterData.clear();
                    masterData.putAll(mst);
                    
                    etOrderNo.setText(String.format("%s-%s", getStringVal(mst, "balym"), getStringVal(mst, "balno")));
                    etCustomerName.setText(getStringVal(mst, "custnm"));
                    etEmail.setText(getStringVal(mst, "email"));
                    etRemarks.setText(getStringVal(mst, "remark"));
                    tvOrderDate.setText(formatDate(getStringVal(mst, "balymd")));
                    
                    fetchItems(balym, balno);
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchItems(String balym, String balno) {
        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S");
        p.put("cmpycd", cmpycd);
        p.put("balym", balym);
        p.put("balno", balno);

        apiService.executeHsioProcedure("HSIO_051U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    orderItems.clear();
                    for (Map<String, Object> item : response.body()) {
                        item.put("_status", ""); 
                        orderItems.add(item);
                    }
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void save() {
        if (getStringVal(masterData, "custcd").isEmpty() || orderItems.isEmpty()) {
            Toast.makeText(this, "필수 정보를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> mst = new HashMap<>(masterData);
        String balno = getStringVal(mst, "balno");
        mst.put("actkind", (balno.isEmpty() || "0000".equals(balno)) ? "A" : "U");
        mst.put("cmpycd", cmpycd);
        mst.put("balymd", tvOrderDate.getText().toString().replace("-", ""));
        mst.put("reqymd", tvOrderDate.getText().toString().replace("-", ""));
        mst.put("updemp", userid);

        List<Map<String, Object>> dtl = new ArrayList<>();
        for (Map<String, Object> item : orderItems) {
            String status = getStringVal(item, "_status");
            Map<String, Object> d = new HashMap<>(item);
            d.put("cmpycd", cmpycd);
            d.put("actkind", status.equals("입력") ? "A" : (status.equals("삭제") ? "D" : "U"));
            d.put("updemp", userid);
            dtl.add(d);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("mst", mst);
        payload.put("dtl", dtl);

        apiService.saveHsio052U(payload).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(MHSIO052U.this, "저장 완료", Toast.LENGTH_SHORT).show();
                    fetchDetail(response.body().getData());
                } else if (response.code() == 401) { handleSessionExpired(); }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void handleFullDelete() {
        if ("0000".equals(getStringVal(masterData, "balno"))) return;
        new AlertDialog.Builder(this).setTitle("전체 삭제").setMessage("발주 정보를 모두 삭제하시겠습니까?")
            .setPositiveButton("예", (d, w) -> {
                Map<String, Object> mst = new HashMap<>();
                mst.put("actkind", "D");
                mst.put("cmpycd", cmpycd);
                mst.put("balym", getStringVal(masterData, "balym"));
                mst.put("balno", getStringVal(masterData, "balno"));
                mst.put("updemp", userid);

                Map<String, Object> payload = new HashMap<>();
                payload.put("mst", mst);
                payload.put("dtl", new ArrayList<>());

                apiService.saveHsio052U(payload).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                    @Override public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> c, @NonNull Response<ApiResponse<Map<String, Object>>> r) {
                        if (r.isSuccessful()) { initialize(); Toast.makeText(MHSIO052U.this, "삭제 완료", Toast.LENGTH_SHORT).show(); }
                    }
                    @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> c, @NonNull Throwable t) {}
                });
            }).setNegativeButton("아니오", null).show();
    }

    private void openHelp(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setView(dialogView);
        
        final String titleText = type.equals("CUST") ? "거래처 검색" : "품목 검색";
        final String hintText = type.equals("CUST") ? "거래처명을 입력하세요" : "품목명을 입력하세요";
        
        builder.setTitle(titleText);
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText(titleText);

        if (tvTitle != null) tvTitle.setText(titleText);

        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        if (etSearch != null) etSearch.setHint(hintText);

        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();

        // 🚀 단일 어댑터 생성 및 고정
        PopupAdapter popupAdapter = new PopupAdapter(list, type, item -> {
            if (type.equals("CUST")) {
                masterData.put("custcd", getStringVal(item, "custcd"));
                masterData.put("custnm", getStringVal(item, "custnm"));
                etCustomerName.setText(getStringVal(item, "custnm"));
                etEmail.setText(getStringVal(item, "email"));
            } else {
                Map<String, Object> newItem = new HashMap<>();
                newItem.put("itemcd", getStringVal(item, "itemcd"));
                newItem.put("itemnm", getStringVal(item, "itemnm"));
                newItem.put("itsize", getStringVal(item, "itsize"));
                newItem.put("unit", getStringVal(item, "unit"));
                double price = parseToDouble(item.get("incost"));
                newItem.put("price", price);
                newItem.put("balqty", 1.0);
                newItem.put("balqty", 1.0);
                newItem.put("balamt", price);
                newItem.put("balvat", Math.floor(price * 0.1));
                newItem.put("sumamt", price + Math.floor(price * 0.1));
                newItem.put("_status", "입력");
                newItem.put("_status", "입력");
                orderItems.add(newItem);
                adapter.notifyDataSetChanged();
            }
            dialog.dismiss();
        });
        rv.setAdapter(popupAdapter);

        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            String keyword = etSearch != null ? etSearch.getText().toString().trim() : "";
            p.put("cmpycd", cmpycd);
            
            if (type.equals("CUST")) {
                p.put("gubun", "C4"); p.put("gbncd", ""); p.put("code", ""); p.put("remark", keyword);
                apiService.executeHa00Procedure("HA00_00P_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            list.clear(); list.addAll(r.body()); popupAdapter.notifyDataSetChanged();
                        } else if (r.code() == 401) { handleSessionExpired(); dialog.dismiss(); }
                    }
                    @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
                });
            } else {
                p.put("gubun", "I1"); p.put("gbncd", "3"); p.put("code", ""); p.put("codenm", keyword); p.put("etcval", "");
                apiService.executeHs00Procedure("HS00_000S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            list.clear(); list.addAll(r.body()); popupAdapter.notifyDataSetChanged();
                        } else if (r.code() == 401) { handleSessionExpired(); dialog.dismiss(); }
                    }
                    @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
                });
            }
        });

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void handleSessionExpired() {
        Toast.makeText(this, "세션이 만료되었습니다. 다시 로그인하세요.", Toast.LENGTH_LONG).show();
        finish();
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    private double parseToDouble(Object val) {
        if (val == null) return 0.0;
        try { return Double.parseDouble(String.valueOf(val).replace(",", "")); } catch (Exception e) { return 0.0; }
    }

    private String formatDate(String d) {
        return d != null && d.length() == 8 ? String.format("%s-%s-%s", d.substring(0,4), d.substring(4,6), d.substring(6,8)) : d;
    }

    @Override protected String getProgramTitle() { return "일반발주등록"; }
    @Override protected String getProgramId() { return "MHSIO052U"; }

    private class OrderAdapter extends BaseAdapter {
        private final DecimalFormat df = new DecimalFormat("#,###");
        @Override public int getCount() { return orderItems.size(); }
        @Override public Object getItem(int p) { return orderItems.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(pr.getContext()).inflate(R.layout.item_mhsio052u, pr, false);
            Map<String, Object> item = orderItems.get(p);
            
            ((TextView) v.findViewById(R.id.tvItemName)).setText(getStringVal(item, "itemnm"));
            EditText etQty = v.findViewById(R.id.etQuantity);
            EditText etPrice = v.findViewById(R.id.etPrice);
            TextView tvAmt = v.findViewById(R.id.etAmount);
            TextView tvVat = v.findViewById(R.id.etVat);

            double qty = parseToDouble(item.get("balqty"));
            double price = parseToDouble(item.get("price"));
            double amt = Math.floor(qty * price);
            double vat = Math.floor(amt * 0.1);

            etQty.setText(String.valueOf(qty));
            etPrice.setText(String.valueOf(price));
            tvAmt.setText(df.format(amt));
            tvVat.setText(df.format(vat));

            if (getStringVal(item, "_status").equals("삭제")) v.setBackgroundColor(Color.LTGRAY);
            else v.setBackgroundColor(Color.WHITE);

            View.OnFocusChangeListener listener = (view, focus) -> {
                if (!focus) {
                    try {
                        double nQty = parseToDouble(etQty.getText().toString());
                        double nPrice = parseToDouble(etPrice.getText().toString());
                        item.put("balqty", nQty);
                        item.put("price", nPrice);
                        item.put("balamt", Math.floor(nQty * nPrice));
                        item.put("balvat", Math.floor(nQty * nPrice * 0.1));
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
