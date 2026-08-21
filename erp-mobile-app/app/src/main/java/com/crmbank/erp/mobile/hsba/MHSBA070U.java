package com.crmbank.erp.mobile.hsba;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHSBA070U] 거래처 정보 등록 (UI 최적화 버전)
 * - 사용자 요청에 따른 Row by Row 배치
 * - 메일주소 추가 및 전자계산서 항목 제거
 */
public class MHSBA070U extends BaseActivity {

    private EditText etCustCd, etCustNm, etCustNo, etBossNm, etTelNo, etEmail, etAddress, etDAddress, etCustType, etCustKind;
    private Spinner spStatus;
    private Switch swUseYn;
    private ApiService apiService;
    private String cmpycd, userid;
    private String currentActKind = "I0";
    private final List<CodeDto> statusOptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsba070u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");

        apiService = RetrofitClient.getApiService();

        etCustCd = findViewById(R.id.etCustCd);
        etCustNm = findViewById(R.id.etCustNm);
        etCustNo = findViewById(R.id.etCustNo);
        etBossNm = findViewById(R.id.etBossNm);
        etTelNo = findViewById(R.id.etTelNo);
        etEmail = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);
        etDAddress = findViewById(R.id.etDAddress);
        etCustType = findViewById(R.id.etCustType);
        etCustKind = findViewById(R.id.etCustKind);
        spStatus = findViewById(R.id.spStatus);
        swUseYn = findViewById(R.id.swUseYn);

        findViewById(R.id.btnSearch).setOnClickListener(v -> openCustSearchPopup());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        loadStatusOptions();
        initialize();
    }

    private void initialize() {
        currentActKind = "I0";
        etCustCd.setText("");
        etCustCd.setEnabled(true);
        etCustNm.setText("");
        etCustNo.setText("");
        etBossNm.setText("");
        etTelNo.setText("");
        etEmail.setText("");
        etAddress.setText("");
        etDAddress.setText("");
        etCustType.setText("");
        etCustKind.setText("");
        swUseYn.setChecked(true);
    }

    private void loadStatusOptions() {
        Map<String, Object> p = new HashMap<>();
        p.put("gubun", "E0"); p.put("cmpycd", cmpycd); p.put("gbncd", "280");
        apiService.executeHs00Procedure("HS00_000S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    statusOptions.clear();
                    List<String> names = new ArrayList<>();
                    for (Map<String, Object> m : response.body()) {
                        CodeDto dto = new CodeDto();
                        dto.codecd = getStringVal(m, "code");
                        dto.codenm = getStringVal(m, "cdnm");
                        statusOptions.add(dto);
                        names.add(dto.codenm);
                    }
                    spStatus.setAdapter(new ArrayAdapter<>(MHSBA070U.this, android.R.layout.simple_spinner_item, names));
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void openCustSearchPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setTitle("거래처 검색").setView(dialogView);
        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();
        PopupAdapter popupAdapter = new PopupAdapter(list, "CUST", item -> {
            loadDetail(item);
            dialog.dismiss();
        });
        rv.setAdapter(popupAdapter);
        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("actkind", "S0"); p.put("cmpycd", cmpycd); p.put("custnm", etSearch.getText().toString().trim());
            apiService.executeHsbaProcedure("HSBA_070U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
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

    private void loadDetail(Map<String, Object> item) {
        currentActKind = "U0";
        etCustCd.setText(getStringVal(item, "custcd"));
        etCustCd.setEnabled(false);
        etCustNm.setText(getStringVal(item, "custnm"));
        etCustNo.setText(getStringVal(item, "custno"));
        etBossNm.setText(getStringVal(item, "bossnm"));
        etTelNo.setText(getStringVal(item, "telno"));
        etEmail.setText(getStringVal(item, "email"));
        etAddress.setText(getStringVal(item, "address"));
        etDAddress.setText(getStringVal(item, "d_address"));
        etCustType.setText(getStringVal(item, "custtype"));
        etCustKind.setText(getStringVal(item, "custkind"));
        swUseYn.setChecked("Y".equals(getStringVal(item, "useyn")));

        String status = getStringVal(item, "status");
        for (int i = 0; i < statusOptions.size(); i++) {
            if (statusOptions.get(i).codecd.equals(status)) { spStatus.setSelection(i); break; }
        }
    }

    private void save() {
        if (etCustNm.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "거래처명(상호)을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> p = new HashMap<>();
        p.put("actkind", currentActKind);
        p.put("cmpycd", cmpycd);
        p.put("custcd", etCustCd.getText().toString().trim());
        p.put("custnm", etCustNm.getText().toString().trim());
        p.put("custno", etCustNo.getText().toString().trim());
        p.put("bossnm", etBossNm.getText().toString().trim());
        p.put("telno", etTelNo.getText().toString().trim());
        p.put("email", etEmail.getText().toString().trim());
        p.put("address", etAddress.getText().toString().trim());
        p.put("d_address", etDAddress.getText().toString().trim());
        p.put("custtype", etCustType.getText().toString().trim());
        p.put("custkind", etCustKind.getText().toString().trim());
        p.put("status", statusOptions.get(spStatus.getSelectedItemPosition()).codecd);
        p.put("useyn", swUseYn.isChecked() ? "Y" : "N");
        p.put("updemp", userid);

        apiService.executeHsbaProcedure("HSBA_070U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSBA070U.this, "정상 처리되었습니다.", Toast.LENGTH_SHORT).show();
                    initialize();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    @Override protected String getProgramTitle() { return "거래처 등록"; }
    @Override protected String getProgramId() { return "MHSBA070U"; }
}
