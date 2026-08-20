package com.crmbank.erp.mobile.hsio;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
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
 * 🚀 [MHSIO190U] 입고반품등록
 * MHSIO052U 프로토타입 기반, HSIO190U.vue 비즈니스 로직 적용 완료 버전
 */
public class MHSIO190U extends BaseActivity {

    private TextView tvOrderDate;
    private EditText etOrderNo, etCustomerName, etRemarks, etTotalSum;
    private Spinner spWhcd;
    private ReturnAdapter adapter;
    private final List<Map<String, Object>> returnItems = new ArrayList<>();
    private final Map<String, Object> masterData = new HashMap<>();
    private final List<Map<String, Object>> whcdList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd, userid, deptcd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio190u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");
        deptcd = prefs.getString("deptcd", "");

        apiService = RetrofitClient.getApiService();

        tvOrderDate = findViewById(R.id.tvOrderDate);
        etOrderNo = findViewById(R.id.etOrderNo);
        etCustomerName = findViewById(R.id.etCustomerName);
        etRemarks = findViewById(R.id.etRemarks);
        etTotalSum = findViewById(R.id.etTotalSum);
        spWhcd = findViewById(R.id.spWhcd);
        ListView lvOrderList = findViewById(R.id.lvOrderList);

        adapter = new ReturnAdapter();
        lvOrderList.setAdapter(adapter);

        tvOrderDate.setOnClickListener(v -> showDatePicker(tvOrderDate));
        etCustomerName.setOnClickListener(v -> openHelp("CUST"));

        findViewById(R.id.btnAddItem).setOnClickListener(v -> openHelp("ITEM"));
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnReset).setOnClickListener(v -> initialize());
        findViewById(R.id.btnDelete).setOnClickListener(v -> handleFullDelete());
        findViewById(R.id.btnOrderSearch).setOnClickListener(v -> showReturnSearchPopup());

        loadWarehouses();
        initialize();
    }

    private void initialize() {
        masterData.clear();
        String todayYmd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        masterData.put("cmpycd", cmpycd);
        masterData.put("ioym", todayYmd.replace("-", "").substring(0, 6));
        masterData.put("iono", "");
        masterData.put("ioymd", todayYmd);
        masterData.put("deptcd", deptcd);
        masterData.put("whcd", "100");
        masterData.put("userid", userid);
        masterData.put("remark", "");
        masterData.put("totsum", "0");

        etOrderNo.setText("");
        etOrderNo.setHint("(자동 생성)");
        etCustomerName.setText("");
        etRemarks.setText("");
        etTotalSum.setText("0");
        tvOrderDate.setText(todayYmd);
        
        returnItems.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        
        selectSpinnerItemByCode(spWhcd, "100");
    }

    private void loadWarehouses() {
        apiService.executeHs00Procedure("HS00_000S_STR", new HashMap<String, Object>() {{
            put("gubun", "W0");
        }}).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    whcdList.clear();
                    whcdList.addAll(response.body());
                    List<String> names = new ArrayList<>();
                    for (Map<String, Object> w : whcdList) names.add(getStringVal(w, "whnm"));
                    ArrayAdapter<String> aa = new ArrayAdapter<>(MHSIO190U.this, android.R.layout.simple_spinner_item, names);
                    aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spWhcd.setAdapter(aa);
                    selectSpinnerItemByCode(spWhcd, "100");
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void selectSpinnerItemByCode(Spinner spinner, String code) {
        for (int i = 0; i < whcdList.size(); i++) {
            if (getStringVal(whcdList.get(i), "whcd").equals(code)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            String date = String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d);
            tv.setText(date);
            if (tv.getId() == R.id.tvOrderDate) {
                masterData.put("ioymd", date.replace("-", ""));
                masterData.put("ioym", date.replace("-", "").substring(0, 6));
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showReturnSearchPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_purch_order_search, null);
        builder.setTitle("반품 목록 조회").setView(dialogView);

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
                if (v == null) v = LayoutInflater.from(pr.getContext()).inflate(R.layout.item_mhsio190u_pop, pr, false);
                Map<String, Object> item = popList.get(p);
                ((TextView) v.findViewById(R.id.tvPopCustNm)).setText(getStringVal(item, "custnm"));
                ((TextView) v.findViewById(R.id.tvPopBalYmd)).setText(getStringVal(item, "ioymd"));
                String bNo = getStringVal(item, "ioym") + getStringVal(item, "iono");
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
            p.put("iogbn", "100");
            p.put("custnm", etPopCust.getText().toString().trim());

            apiService.executeHsioProcedure("HSIO_190U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
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
        String ioym = getStringVal(row, "ioym");
        String iono = getStringVal(row, "iono");
        
        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S");
        p.put("cmpycd", cmpycd);
        p.put("iogbn", "100");
        p.put("ioym", ioym);
        p.put("iono", iono);

        apiService.executeHsioProcedure("HSIO_190U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Map<String, Object> mst = response.body().get(0);
                    masterData.clear();
                    masterData.putAll(mst);
                    
                    etOrderNo.setText(String.format("%s%s", getStringVal(mst, "ioym"), getStringVal(mst, "iono")));
                    etCustomerName.setText(getStringVal(mst, "custnm"));
                    etRemarks.setText(getStringVal(mst, "remark"));
                    tvOrderDate.setText(formatDate(getStringVal(mst, "ioymd")));
                    selectSpinnerItemByCode(spWhcd, getStringVal(mst, "whcd"));
                    
                    fetchItems(ioym, iono);
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchItems(String ioym, String iono) {
        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S");
        p.put("cmpycd", cmpycd);
        p.put("iogbn", "100");
        p.put("ioym", ioym);
        p.put("iono", iono);
        p.put("ioqty", 0); p.put("ioamt", 0); p.put("iovat", 0);

        apiService.executeHsioProcedure("HSIO_191U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    returnItems.clear();
                    for (Map<String, Object> item : response.body()) {
                        item.put("_status", ""); 
                        returnItems.add(item);
                    }
                    adapter.notifyDataSetChanged();
                    updateTotalSum();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void save() {
        if (getStringVal(masterData, "custcd").isEmpty() || returnItems.isEmpty()) {
            Toast.makeText(this, "필수 정보를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> mst = new HashMap<>(masterData);
        String iono = getStringVal(mst, "iono");
        mst.put("actkind", iono.isEmpty() ? "A" : "U");
        mst.put("iogbn", "100");
        mst.put("iotype", "100");
        mst.put("cfmyn", "Y");
        mst.put("gubun", "1");
        mst.put("whcd", getStringVal(whcdList.get(spWhcd.getSelectedItemPosition()), "whcd"));
        mst.put("ioymd", tvOrderDate.getText().toString().replace("-", ""));
        mst.put("totsum", etTotalSum.getText().toString().replace(",", ""));
        mst.put("updemp", userid);

        List<Map<String, Object>> dtl = new ArrayList<>();
        for (Map<String, Object> item : returnItems) {
            String status = getStringVal(item, "_status");
            Map<String, Object> d = new HashMap<>(item);
            d.put("cmpycd", cmpycd);
            d.put("iogbn", "100");
            d.put("iotype", "100");
            d.put("cfmyn", "Y");
            d.put("actkind", status.equals("입력") ? "A" : (status.equals("삭제") ? "D" : "U"));
            d.put("updemp", userid);
            dtl.add(d);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("mst", mst);
        payload.put("dtl", dtl);

        apiService.saveHsio190U(payload).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(MHSIO190U.this, "저장 완료", Toast.LENGTH_SHORT).show();
                    fetchDetail(response.body().getData());
                } else if (response.code() == 401) { handleSessionExpired(); }
                else { Toast.makeText(MHSIO190U.this, "저장 실패: " + (response.body() != null ? response.body().getMessage() : ""), Toast.LENGTH_SHORT).show(); }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void handleFullDelete() {
        if (getStringVal(masterData, "iono").isEmpty()) return;
        new AlertDialog.Builder(this).setTitle("전체 삭제").setMessage("반품 정보를 모두 삭제하시겠습니까?")
            .setPositiveButton("예", (d, w) -> {
                Map<String, Object> p = new HashMap<>();
                p.put("actkind", "D");
                p.put("cmpycd", cmpycd);
                p.put("iogbn", "100");
                p.put("ioym", getStringVal(masterData, "ioym"));
                p.put("iono", getStringVal(masterData, "iono"));
                p.put("cfmyn", "Y");

                apiService.executeHsioProcedure("HSIO_190U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                        if (r.isSuccessful()) { initialize(); Toast.makeText(MHSIO190U.this, "삭제 완료", Toast.LENGTH_SHORT).show(); }
                    }
                    @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
                });
            }).setNegativeButton("아니오", null).show();
    }

    private void openHelp(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setView(dialogView);
        
        final String titleText = type.equals("CUST") ? "매입거래처 검색" : "품목 검색";
        builder.setTitle(titleText);
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText(titleText);

        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();

        PopupAdapter popupAdapter = new PopupAdapter(list, type, item -> {
            if (type.equals("CUST")) {
                masterData.put("custcd", getStringVal(item, "custcd"));
                masterData.put("custnm", getStringVal(item, "custnm"));
                etCustomerName.setText(getStringVal(item, "custnm"));
            } else {
                Map<String, Object> newItem = new HashMap<>();
                newItem.put("itemcd", getStringVal(item, "itemcd"));
                newItem.put("itemnm", getStringVal(item, "itemnm"));
                newItem.put("itsize", getStringVal(item, "itsize"));
                newItem.put("unit", getStringVal(item, "unit"));
                double price = parseToDouble(item.get("inprice"));
                newItem.put("price", price);
                newItem.put("ioqty", 1.0);
                newItem.put("ioamt", price);
                newItem.put("iovat", Math.floor(price * 0.1));
                newItem.put("_status", "입력");
                returnItems.add(newItem);
                adapter.notifyDataSetChanged();
                updateTotalSum();
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
                p.put("gubun", "I1"); p.put("gbncd", "1"); p.put("code", ""); p.put("codenm", keyword); p.put("etcval", "");
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

    private void updateTotalSum() {
        double total = 0;
        for (Map<String, Object> item : returnItems) {
            if (!getStringVal(item, "_status").equals("삭제")) {
                total += parseToDouble(item.get("ioamt")) + parseToDouble(item.get("iovat"));
            }
        }
        DecimalFormat df = new DecimalFormat("#,###");
        etTotalSum.setText(df.format(total));
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

    @Override protected String getProgramTitle() { return "입고 반품 등록"; }
    @Override protected String getProgramId() { return "MHSIO190U"; }

    private class ReturnAdapter extends BaseAdapter {
        private final DecimalFormat df = new DecimalFormat("#,###");
        @Override public int getCount() { return returnItems.size(); }
        @Override public Object getItem(int p) { return returnItems.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(pr.getContext()).inflate(R.layout.item_mhsio190u, pr, false);
            Map<String, Object> item = returnItems.get(p);
            
            ((TextView) v.findViewById(R.id.tvItemName)).setText(getStringVal(item, "itemnm"));
            EditText etQty = v.findViewById(R.id.etQuantity);
            EditText etPrice = v.findViewById(R.id.etPrice);
            TextView tvAmt = v.findViewById(R.id.etAmount);
            TextView tvVat = v.findViewById(R.id.etVat);

            double qty = parseToDouble(item.get("ioqty"));
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
                        item.put("ioqty", nQty);
                        item.put("price", nPrice);
                        item.put("ioamt", Math.floor(nQty * nPrice));
                        item.put("iovat", Math.floor(nQty * nPrice * 0.1));
                        if (getStringVal(item, "_status").isEmpty()) item.put("_status", "수정");
                        notifyDataSetChanged();
                        updateTotalSum();
                    } catch (Exception ignored) {}
                }
            };
            etQty.setOnFocusChangeListener(listener);
            etPrice.setOnFocusChangeListener(listener);

            v.findViewById(R.id.btnDelete).setOnClickListener(view -> {
                if (getStringVal(item, "_status").equals("입력")) returnItems.remove(p);
                else item.put("_status", getStringVal(item, "_status").equals("삭제") ? "" : "삭제");
                notifyDataSetChanged();
                updateTotalSum();
            });
            
            return v;
        }
    }
}
