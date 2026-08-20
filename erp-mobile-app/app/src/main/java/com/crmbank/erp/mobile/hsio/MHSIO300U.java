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
 * 🚀 [MHSIO300U] 입금입력
 * 웹 HSIO300U.vue 로직 및 MHSIO052U 프로토타입 기반 완성 버전
 */
public class MHSIO300U extends BaseActivity {

    private TextView tvPaymentDate;
    private EditText etCustNm, etRemarks, etPaymentNo;
    private PaymentAdapter adapter;
    private final List<Map<String, Object>> paymentItems = new ArrayList<>();
    private final List<CodeDto> paymentTypeCodes = new ArrayList<>();
    private final Map<String, Object> masterData = new HashMap<>();
    private ApiService apiService;
    private String cmpycd, userid, deptcd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsio300u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");
        deptcd = prefs.getString("deptcd", "");

        apiService = RetrofitClient.getApiService();

        tvPaymentDate = findViewById(R.id.tvPaymentDate);
        etCustNm = findViewById(R.id.etCustNm);
        etRemarks = findViewById(R.id.etRemarks);
        etPaymentNo = findViewById(R.id.etPaymentNo);
        ListView lvPaymentList = findViewById(R.id.lvPaymentList);
        
        adapter = new PaymentAdapter();
        lvPaymentList.setAdapter(adapter);

        tvPaymentDate.setOnClickListener(v -> showDatePicker(tvPaymentDate));
        etCustNm.setOnClickListener(v -> openHelp("CUST"));

        findViewById(R.id.btnAddRow).setOnClickListener(v -> {
            Map<String, Object> item = new HashMap<>();
            item.put("imtype", "01"); // 기본: 현금
            item.put("imamt", 0.0);
            item.put("_status", "입력");
            paymentItems.add(item);
            adapter.notifyDataSetChanged();
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnReset).setOnClickListener(v -> initialize());
        findViewById(R.id.btnDelete).setOnClickListener(v -> handleFullDelete());
        findViewById(R.id.btnSearchPayment).setOnClickListener(v -> showPaymentSearchPopup());

        loadPaymentTypeCodes();
        initialize();
    }

    private void initialize() {
        masterData.clear();
        String todayYmd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        masterData.put("imym", todayYmd.replace("-", "").substring(0, 6));
        masterData.put("imno", "");
        masterData.put("imymd", todayYmd);
        masterData.put("deptcd", deptcd);
        masterData.put("custcd", "");
        masterData.put("remark", "");

        etPaymentNo.setText("");
        etPaymentNo.setHint("(자동 생성)");
        etCustNm.setText("");
        etRemarks.setText("");
        tvPaymentDate.setText(todayYmd);
        
        paymentItems.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadPaymentTypeCodes() {
        apiService.getCommonCode(cmpycd, "KOR", "130").enqueue(new Callback<List<CodeDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<CodeDto>> call, @NonNull Response<List<CodeDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    paymentTypeCodes.clear();
                    paymentTypeCodes.addAll(response.body());
                    if (adapter != null) adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<CodeDto>> call, @NonNull Throwable t) {}
        });
    }

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            String date = String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d);
            tv.setText(date);
            if (tv.getId() == R.id.tvPaymentDate) {
                masterData.put("imymd", date.replace("-", ""));
                masterData.put("imym", date.replace("-", "").substring(0, 6));
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openHelp(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setView(dialogView);
        builder.setTitle("거래처 검색");

        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();

        PopupAdapter popupAdapter = new PopupAdapter(list, "CUST", item -> {
            masterData.put("custcd", getStringVal(item, "custcd"));
            masterData.put("custnm", getStringVal(item, "custnm"));
            etCustNm.setText(getStringVal(item, "custnm"));
            dialog.dismiss();
        });
        rv.setAdapter(popupAdapter);

        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("cmpycd", cmpycd);
            p.put("gubun", "C4"); p.put("gbncd", ""); p.put("code", ""); 
            p.put("remark", etSearch.getText().toString().trim());

            apiService.executeHa00Procedure("HA00_00P_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
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

    private void showPaymentSearchPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_purch_order_search, null);
        builder.setTitle("입금 내역 조회").setView(dialogView);

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
                if (v == null) v = LayoutInflater.from(pr.getContext()).inflate(R.layout.item_mhsio300u_pop, pr, false);
                Map<String, Object> item = popList.get(p);
                ((TextView) v.findViewById(R.id.tvPopCustNm)).setText(getStringVal(item, "custnm"));
                ((TextView) v.findViewById(R.id.tvPopBalYmd)).setText(getStringVal(item, "imymd"));
                String imNo = getStringVal(item, "imym") + "-" + getStringVal(item, "imno");
                ((TextView) v.findViewById(R.id.tvPopBalno)).setText(imNo);
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
            p.put("custnm", etPopCust.getText().toString().trim());

            apiService.executeHsioProcedure("HSIO_300U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
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

    private void fetchDetail(Map<String, Object> row) {
        String imym = getStringVal(row, "imym");
        String imno = getStringVal(row, "imno");
        
        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S1");
        p.put("cmpycd", cmpycd);
        p.put("imym", imym);
        p.put("imno", imno);

        apiService.executeHsioProcedure("HSIO_300U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Map<String, Object> mst = response.body().get(0);
                    masterData.clear();
                    masterData.putAll(mst);
                    
                    etPaymentNo.setText(String.format("%s-%s", getStringVal(mst, "imym"), getStringVal(mst, "imno")));
                    etCustNm.setText(getStringVal(mst, "custnm"));
                    etRemarks.setText(getStringVal(mst, "remark"));
                    tvPaymentDate.setText(formatDate(getStringVal(mst, "imymd")));
                    
                    fetchItems(imym, imno);
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchItems(String imym, String imno) {
        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S1");
        p.put("cmpycd", cmpycd);
        p.put("imym", imym);
        p.put("imno", imno);

        apiService.executeHsioProcedure("HSIO_300U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    paymentItems.clear();
                    for (Map<String, Object> item : response.body()) {
                        item.put("_status", ""); 
                        paymentItems.add(item);
                    }
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void save() {
        if (getStringVal(masterData, "custcd").isEmpty() || paymentItems.isEmpty()) {
            Toast.makeText(this, "필수 정보를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> mst = new HashMap<>(masterData);
        String imno = getStringVal(mst, "imno");
        mst.put("actkind", imno.isEmpty() ? "A0" : "U0");
        mst.put("cmpycd", cmpycd);
        mst.put("imymd", tvPaymentDate.getText().toString().replace("-", ""));
        mst.put("updemp", userid);

        List<Map<String, Object>> dtl = new ArrayList<>();
        for (Map<String, Object> item : paymentItems) {
            String status = getStringVal(item, "_status");
            Map<String, Object> d = new HashMap<>(item);
            d.put("actkind", status.equals("입력") ? "A1" : (status.equals("삭제") ? "D1" : "U1"));
            dtl.add(d);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("mst", mst);
        payload.put("dtl", dtl);

        apiService.saveHsio300U(payload).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(MHSIO300U.this, "저장 완료", Toast.LENGTH_SHORT).show();
                    fetchDetail(response.body().getData());
                } else {
                    String msg = response.body() != null ? response.body().getMessage() : "Unknown Error";
                    Toast.makeText(MHSIO300U.this, "저장 실패: " + msg, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void handleFullDelete() {
        if (getStringVal(masterData, "imno").isEmpty()) return;
        new AlertDialog.Builder(this).setTitle("전체 삭제").setMessage("입금 정보를 모두 삭제하시겠습니까?")
            .setPositiveButton("예", (d, w) -> {
                Map<String, Object> mst = new HashMap<>(masterData);
                mst.put("actkind", "D0");
                mst.put("updemp", userid);

                apiService.executeHsioProcedure("HSIO_300U_STR", mst).enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                        if (r.isSuccessful()) { initialize(); Toast.makeText(MHSIO300U.this, "삭제 완료", Toast.LENGTH_SHORT).show(); }
                    }
                    @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
                });
            }).setNegativeButton("아니오", null).show();
    }

    private String formatDate(String d) {
        return d != null && d.length() == 8 ? String.format("%s-%s-%s", d.substring(0,4), d.substring(4,6), d.substring(6,8)) : d;
    }

    @Override protected String getProgramTitle() { return "입금 입력"; }
    @Override protected String getProgramId() { return "MHSIO300U"; }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    private class PaymentAdapter extends BaseAdapter {
        private final DecimalFormat df = new DecimalFormat("#,###");
        @Override public int getCount() { return paymentItems.size(); }
        @Override public Object getItem(int p) { return paymentItems.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSIO300U.this).inflate(R.layout.item_mhsio300u, pr, false);
            Map<String, Object> item = paymentItems.get(p);
            
            Spinner spType = v.findViewById(R.id.spPaymentType);
            EditText etAmt = v.findViewById(R.id.etAmount);
            EditText etRef = v.findViewById(R.id.etRefNo);

            ArrayAdapter<CodeDto> aa = new ArrayAdapter<>(MHSIO300U.this, android.R.layout.simple_spinner_item, paymentTypeCodes);
            aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spType.setAdapter(aa);
            
            // 타입 설정
            String type = getStringVal(item, "imtype");
            for (int i = 0; i < paymentTypeCodes.size(); i++) {
                if (paymentTypeCodes.get(i).codecd.equals(type)) {
                    spType.setSelection(i); break;
                }
            }

            etAmt.setText(getStringVal(item, "imamt"));
            etRef.setText(getStringVal(item, "manage_no"));

            v.findViewById(R.id.btnDelete).setOnClickListener(view -> {
                if (getStringVal(item, "_status").equals("입력")) paymentItems.remove(p);
                else item.put("_status", getStringVal(item, "_status").equals("삭제") ? "" : "삭제");
                notifyDataSetChanged();
            });
            
            return v;
        }
    }
}
