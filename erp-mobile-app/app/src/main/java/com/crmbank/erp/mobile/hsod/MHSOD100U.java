package com.crmbank.erp.mobile.hsod;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
import com.crmbank.erp.mobile.PopupAdapter;
import com.crmbank.erp.mobile.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHSOD100U] 주문등록
 * 고성능 네비처리(RecyclerView) 적용 및 어댑터 동기화 완결 버전
 */
public class MHSOD100U extends BaseActivity {

    private TextView tvOrderDate;
    private EditText etOrderNo, etCustomerName;
    private final List<Map<String, Object>> orderItems = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsod100u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();

        apiService = RetrofitClient.getApiService();

        tvOrderDate = findViewById(R.id.tvOrderDate);
        etOrderNo = findViewById(R.id.etOrderNo);
        etCustomerName = findViewById(R.id.etCustomerName);

        etCustomerName.setOnClickListener(v -> openHelp("CUST"));
        findViewById(R.id.btnAddItem).setOnClickListener(v -> openHelp("ITEM"));

        initialize();
    }

    private void initialize() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvOrderDate.setText(today);
        etOrderNo.setText("");
        etCustomerName.setText("");
        orderItems.clear();
    }

    private void openHelp(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setView(dialogView);
        
        String titleText = type.equals("CUST") ? "거래처 검색" : "품목 검색";
        builder.setTitle(titleText);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText(titleText);

        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        
        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();

        // 🚀 단일 어댑터 고정
        PopupAdapter popupAdapter = new PopupAdapter(list, type, item -> {
            if (type.equals("CUST")) {
                etCustomerName.setText(getStringVal(item, "custnm"));
            } else {
                Map<String, Object> newItem = new HashMap<>(item);
                newItem.put("_status", "입력");
                orderItems.add(newItem);
            }
            dialog.dismiss();
        });
        rv.setAdapter(popupAdapter);

        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("cmpycd", cmpycd);
            String keyword = etSearch != null ? etSearch.getText().toString().trim() : "";
            
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
                p.put("gubun", "I1"); p.put("gbncd", "2"); p.put("code", ""); p.put("codenm", keyword); p.put("etcval", "");
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

    @Override protected String getProgramTitle() { return "주문등록"; }
    @Override protected String getProgramId() { return "MHSOD100U"; }
}
