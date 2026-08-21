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
 * 🚀 [MHSBA010U] 품목 등록 (UI 최적화 버전)
 * - 사용자 요청에 따른 Row by Row 배치
 * - 품목코드 자동 생성 대응 및 단위 콤보박스 연동
 */
public class MHSBA010U extends BaseActivity {

    private EditText etItemCd, etItemNm, etItSize, etInQty, etOutQty, etMaker, etRemark;
    private Spinner spUnit, spInUnit, spOutUnit;
    private Switch swUseYn;
    private ApiService apiService;
    private String cmpycd, userid;
    private String currentActKind = "A0";
    private final List<CodeDto> unitOptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsba010u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");

        apiService = RetrofitClient.getApiService();

        etItemCd = findViewById(R.id.etItemCd);
        etItemNm = findViewById(R.id.etItemNm);
        etItSize = findViewById(R.id.etItSize);
        etInQty = findViewById(R.id.etInQty);
        etOutQty = findViewById(R.id.etOutQty);
        etMaker = findViewById(R.id.etMaker);
        etRemark = findViewById(R.id.etRemark);

        spUnit = findViewById(R.id.spUnit);
        spInUnit = findViewById(R.id.spInUnit);
        spOutUnit = findViewById(R.id.spOutUnit);
        
        swUseYn = findViewById(R.id.swUseYn);

        findViewById(R.id.btnSearch).setOnClickListener(v -> openItemSearchPopup());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        loadUnitOptions();
        initialize();
    }

    private void initialize() {
        currentActKind = "A0";
        etItemCd.setText("");
        etItemCd.setHint("자동생성");
        etItemNm.setText("");
        etItSize.setText("");
        etInQty.setText("1");
        etOutQty.setText("1");
        etMaker.setText("");
        etRemark.setText("");
        swUseYn.setChecked(true);
    }

    private void loadUnitOptions() {
        Map<String, Object> p = new HashMap<>();
        p.put("gubun", "U0"); p.put("cmpycd", cmpycd);
        apiService.executeHs00Procedure("HS00_000S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    unitOptions.clear();
                    List<String> names = new ArrayList<>();
                    for (Map<String, Object> m : response.body()) {
                        CodeDto dto = new CodeDto();
                        dto.codecd = getStringVal(m, "unit");
                        dto.codenm = getStringVal(m, "unitnm");
                        unitOptions.add(dto);
                        names.add(dto.codenm);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MHSBA010U.this, android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spUnit.setAdapter(adapter);
                    spInUnit.setAdapter(adapter);
                    spOutUnit.setAdapter(adapter);
                    
                    // EA(기본값) 선택 시도
                    for(int i=0; i<unitOptions.size(); i++) {
                        if("EA".equalsIgnoreCase(unitOptions.get(i).codecd)) {
                            spUnit.setSelection(i); spInUnit.setSelection(i); spOutUnit.setSelection(i);
                            break;
                        }
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void openItemSearchPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null); // 품목 검색용으로 재활용 가능
        builder.setTitle("품목 검색").setView(dialogView);
        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();
        PopupAdapter popupAdapter = new PopupAdapter(list, "ITEM", item -> {
            loadDetail(item);
            dialog.dismiss();
        });
        rv.setAdapter(popupAdapter);
        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("actkind", "S0"); p.put("cmpycd", cmpycd); 
            p.put("astkind", "120"); // 기본 상품 조회
            p.put("itemnm", etSearch.getText().toString().trim());
            p.put("icqty", 0); p.put("ocqty", 0); p.put("imprice", 0); p.put("omprice", 0); p.put("stock", 0); p.put("qtypnt", 0);
            
            apiService.executeHsbaProcedure("HSBA_010U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
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
        etItemCd.setText(getStringVal(item, "itemcd"));
        etItemNm.setText(getStringVal(item, "itemnm"));
        etItSize.setText(getStringVal(item, "itsize"));
        etInQty.setText(getStringVal(item, "inqty"));
        etOutQty.setText(getStringVal(item, "outqty"));
        etMaker.setText(getStringVal(item, "maker"));
        etRemark.setText(getStringVal(item, "remark"));
        swUseYn.setChecked("Y".equals(getStringVal(item, "useyn")));

        setSpinnerSelection(spUnit, getStringVal(item, "unit"));
        setSpinnerSelection(spInUnit, getStringVal(item, "inunit"));
        setSpinnerSelection(spOutUnit, getStringVal(item, "outunit"));
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        for (int i = 0; i < unitOptions.size(); i++) {
            if (unitOptions.get(i).codecd.equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void save() {
        if (etItemNm.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "품목명을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> p = new HashMap<>();
        p.put("actkind", currentActKind);
        p.put("cmpycd", cmpycd);
        p.put("astkind", "120"); // 기본 상품
        p.put("itemcd", etItemCd.getText().toString().trim());
        p.put("itemnm", etItemNm.getText().toString().trim());
        p.put("itsize", etItSize.getText().toString().trim());
        
        if (spUnit.getSelectedItemPosition() >= 0) p.put("unit", unitOptions.get(spUnit.getSelectedItemPosition()).codecd);
        if (spInUnit.getSelectedItemPosition() >= 0) p.put("inunit", unitOptions.get(spInUnit.getSelectedItemPosition()).codecd);
        if (spOutUnit.getSelectedItemPosition() >= 0) p.put("outunit", unitOptions.get(spOutUnit.getSelectedItemPosition()).codecd);
        
        p.put("inqty", etInQty.getText().toString().trim());
        p.put("outqty", etOutQty.getText().toString().trim());
        p.put("maker", etMaker.getText().toString().trim());
        p.put("useyn", swUseYn.isChecked() ? "Y" : "N");
        p.put("remark", etRemark.getText().toString().trim());
        
        p.put("userid", userid);
        p.put("updemp", userid);
        p.put("stock", 0); p.put("qtypnt", 0); // 필수 파라미터 보정

        apiService.executeHsbaProcedure("HSBA_010U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSBA010U.this, "정상 처리되었습니다.", Toast.LENGTH_SHORT).show();
                    initialize();
                } else {
                    Toast.makeText(MHSBA010U.this, "저장 실패", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                Toast.makeText(MHSBA010U.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    @Override protected String getProgramTitle() { return "품목 등록"; }
    @Override protected String getProgramId() { return "MHSBA010U"; }
}
